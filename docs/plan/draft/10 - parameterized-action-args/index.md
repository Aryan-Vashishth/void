# Parameterized Action Args

Identified: 2026-08-05, post-hotfix audit of SauceDemoTest.
Branch: `initiative/parameterized-action-args`, cut from `main` after hotfix merge.

---

## Problem statement

Elements whose XPath contains `%1$s` substitution placeholders (e.g. slug-keyed buttons)
cannot be used through the action DSL. Callers must drop below the DSL to resolve the
locator manually:

```java
// Current -- bypasses the action DSL entirely
engine().click(engine().resolve(ADD_TO_CART_BUTTON, ElementRole.TRIGGER, "sauce-labs-backpack"));
```

This is verbose, bypasses any engine-level wait/retry logic the action wraps around the
raw `engine.click()` call, and is stylistically inconsistent with the rest of the test
code. The root cause is that `ElementAction.resolve()` never forwards override args to
`UIEngine.resolve()` -- it always passes no args, so `effectiveArgs()` always falls back
to the element's static `getArgs()`.

**Constraint:** The fix must be type-gated. A static element like `LOGIN_BUTTON` must not
expose the parameterized API. The compiler, not documentation, must enforce this.

**Constraint:** The fix must be engine-agnostic. The resolution path goes through
`UIEngine.resolve()` (the neutral interface); no Selenium-specific code may appear in the
new types. A future `PlaywrightEngine` must work identically.

See also: `docs/audits/backlog/violations/uiengine-getnativedriver-escape-hatch.md` for
the related `getNativeDriver()` escape-hatch vulnerability that the hotfix partially
addressed.

---

## Concern map

| ID | Concern | Layer |
|----|---------|-------|
| C1 | No way to pass locator args through the action DSL | `core.actions` / `domain.automation.web.vocabulary.actions` |
| C2 | Parameterized elements indistinguishable from static elements at type level | `domain.automation.web.vocabulary.capability` |
| C3 | `SortDropdown.Controls.SORT_SELECT` declared `Clickable` instead of `Selectable` | `tests.demo.pages.saucedemo` |

---

## Design

### Naming

`withArgs(Object... args)` -- supplies arguments that are substituted into the locator
template. Alternatives considered and rejected:

| Rejected name | Reason |
|---|---|
| `dynamically(arg)` | Ambiguous -- dynamic execution? dynamic wait? dynamic locator? |
| `forItem(arg)` | Domain-specific, meaningless outside one test suite |
| `using(arg)` | Natural but collides with common existing method names |

### Type hierarchy

```
ElementAction  (existing, unchanged)
  └── ParameterizedAction<T>  (new abstract, CRTP)  ← locatorArgs + withArgs() + resolve()
        └── ParameterizedClickAction                 (new concrete) ← only overrides execute()
        └── ParameterizedHoverAction  (Phase 2)
        └── ParameterizedTypeAction   (Phase 2)

Clickable  (existing, unchanged)
  └── ParameterizedClickable  (new)  ← overrides click() with covariant return type
```

`withArgs()` and the args-forwarding `resolve()` live in `ParameterizedAction` once.
Phase 2 action types inherit both for free -- zero duplication.

### CRTP: self-referential generic for fluent return type

The naive `public <A extends ParameterizedAction> A withArgs(...)` compiles but is not
actually type-safe -- any caller can assign the result to any `ParameterizedAction`
subtype and the cast only fails at runtime. The Self type (CRTP) fixes this:

```java
abstract class ParameterizedAction<T extends ParameterizedAction<T>> extends ElementAction {

    @SuppressWarnings("unchecked")    // contained: cast is safe by the CRTP contract
    protected final T self() { return (T) this; }

    public final T withArgs(Object... args) { ... return self(); }
}

final class ParameterizedClickAction extends ParameterizedAction<ParameterizedClickAction> { ... }
```

`withArgs()` now returns `ParameterizedClickAction` at the call site -- no external cast,
no `@SuppressWarnings` on the caller. The single suppression is isolated inside `self()`.

`withArgs()` is `final` -- no subclass should override locator-arg handling. All
subclasses differ only in `execute()`.

### Action lifecycle (one-shot objects)

```
Element constant (enum)
    │
    │ .click()
    ▼
ParameterizedClickAction   ← created; locatorArgs = NO_ARGS
    │
    │ .withArgs("sauce-labs-backpack")
    ▼
ParameterizedClickAction   ← locatorArgs = ["sauce-labs-backpack"]  (same instance)
    │
    │ .perform(engine)
    ▼
resolve(engine)            ← engine.resolve(element, TRIGGER, locatorArgs)
    │                         effectiveArgs: locatorArgs non-empty → used as-is
    ▼
engine.click(descriptor)   ← SeleniumEngine / PlaywrightEngine -- engine-neutral
```

Actions are one-shot objects. Mutating `locatorArgs` via a second `withArgs()` call after
`perform()` is silent but semantically wrong -- the action has already executed.
Document this at the class level: **do not reuse action instances across calls**.

### Engine agnosticism

`ParameterizedAction.resolve()` calls `UIEngine.resolve(element, role, locatorArgs)`.
`UIEngine` is the engine-neutral interface. `SeleniumEngine` and any future
`PlaywrightEngine` both implement it. No engine-specific code appears in the new types.

### `withArgs()` double-call behavior

Second call silently overwrites first. This is a deliberate simplicity tradeoff --
throwing on a second call adds complexity for a usage pattern that should not occur in
practice. The one-shot lifecycle note above and a dedicated test make this explicit.

---

## Phase 1 -- Core implementation

**Files:**

| File | Change |
|---|---|
| `src/main/java/domain/automation/web/vocabulary/actions/ParameterizedAction.java` | New abstract base: `locatorArgs` field, `withArgs()`, `resolve()` override |
| `src/main/java/domain/automation/web/vocabulary/actions/ParameterizedClickAction.java` | New concrete: extends `ParameterizedAction`; `execute()` calls `engine.click()` |
| `src/main/java/domain/automation/web/vocabulary/capability/ParameterizedClickable.java` | New interface: extends `Clickable`; overrides `click()` with covariant `ParameterizedClickAction` return |
| `src/main/java/tests/demo/pages/saucedemo/ProductsPage.java` | Split `ProductItem.Buttons` → `Buttons` (static) + `DynamicButtons` (`ParameterizedClickable`); `Controls.SORT_SELECT` → `Selectable` |
| `src/main/java/tests/demo/pages/saucedemo/CartPage.java` | Split `CartItem.Buttons` → `Buttons` (static) + `DynamicButtons` (`ParameterizedClickable`) |
| `src/main/resources/tests/demo/pages/SauceDemo/ProductsPage/locators.properties` | Add `SortDropdown.Controls.SORT_SELECT.LIST` XPath if absent; re-sync |
| `src/main/java/tests/demo/SauceDemoTest.java` | Replace `engine().click(resolve(..., slug))` with `click().withArgs(slug)`; `selectByVisibleText()` with `selectByText()` |
| `src/test/java/elements/api/actions/ParameterizedActionTest.java` | New -- 6 unit tests (see below) |

**No changes to:** `ElementAction`, `ClickAction`, `Clickable`, `UIEngine`, `SeleniumEngine`.

### `ParameterizedAction` (new abstract base)

```java
// T is the concrete subtype -- enables withArgs() to return the concrete type directly.
public abstract class ParameterizedAction<T extends ParameterizedAction<T>>
        extends ElementAction {

    private Object[] locatorArgs = Target.NO_ARGS;

    protected ParameterizedAction(UIElement element, ElementRole role, ActionCapability capability) {
        super(element, role, capability);
    }

    // Single suppression point; safe by the CRTP contract (T is the concrete subclass).
    @SuppressWarnings("unchecked")
    protected final T self() { return (T) this; }

    // final: no subclass should change how locator args are stored or returned.
    public final T withArgs(Object... args) {
        // Second call overwrites first -- actions are one-shot; do not reuse.
        this.locatorArgs = (args != null) ? args : Target.NO_ARGS;
        return self();
    }

    @Override
    public final LocatorDescriptor resolve(Executor executor) {
        UIEngine engine = (UIEngine) executor;
        // engine.resolve() is the engine-neutral contract; effectiveArgs(locatorArgs)
        // inside the resolver: non-empty locatorArgs win, empty falls back to element.getArgs().
        return engine.resolve(element, role, locatorArgs);
    }
}
```

### `ParameterizedClickAction` (new concrete)

```java
// The type parameter is resolved here -- withArgs() returns ParameterizedClickAction directly.
public final class ParameterizedClickAction
        extends ParameterizedAction<ParameterizedClickAction> {

    ParameterizedClickAction(ParameterizedClickable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.CLICKABLE);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
```

### `ParameterizedClickable` (new capability interface)

```java
public interface ParameterizedClickable extends Clickable {
    @Override
    default ParameterizedClickAction click() {
        return new ParameterizedClickAction(this);
    }
}
```

Covariant return is valid Java. Elements declaring `implements ParameterizedClickable`
get `click()` → `ParameterizedClickAction` (has `withArgs()`). Elements declaring only
`implements Clickable` get `click()` → `ClickAction` (no `withArgs()`).

### Page object enum split (Java constraint)

Java enums cannot vary implemented interfaces per constant. Constants with different
parameterization requirements must be in separate enums. Convention: parameterized
constants go into `DynamicButtons` within the same conceptual group.

```java
// ProductsPage.ProductItem -- before
enum Buttons implements Clickable {
    ADD_TO_CART_BUTTON, REMOVE_BUTTON, ITEM_TITLE_LINK;
}

// ProductsPage.ProductItem -- after
enum Buttons implements Clickable {
    ITEM_TITLE_LINK;         // static XPath
}
enum DynamicButtons implements ParameterizedClickable {
    ADD_TO_CART_BUTTON,      // //button[@data-test='add-to-cart-%1$s']
    REMOVE_BUTTON;           // //button[@data-test='remove-%1$s']
}
```

Same split applies to `CartPage.CartItem`.

### Tests (no browser required)

Recording stub: override `resolve(UIElement, ElementRole, Object...)` in `UIEngineStub`
to capture the args array for assertion.

| # | Test | Verifies |
|---|---|---|
| 1 | `withArgs_singleArg_passedToEngineResolve` | args forwarded correctly |
| 2 | `withArgs_multipleArgs_allPassedToEngineResolve` | multi-arg forwarding |
| 3 | `withArgs_notCalled_emptyArgsForwarded` | fallback to `element.getArgs()` via `effectiveArgs` |
| 4 | `withArgs_calledWithNull_treatedAsNoOverride` | null → `NO_ARGS` |
| 5 | `withArgs_calledTwice_secondCallOverwritesFirst` | double-call behavior made explicit |
| 6 | `withArgs_returnsItselfForChaining` | fluent return type |

---

## Phase 2 -- Extend to hover, type (future)

When a use case requiring parameterized hover or type operations arises:

```java
// Pattern is identical -- only execute() differs
class ParameterizedHoverAction
        extends ParameterizedAction<ParameterizedHoverAction> {
    protected void execute(UIEngine engine, LocatorDescriptor d) { engine.hover(d); }
}
interface ParameterizedHoverable extends Hoverable {
    default ParameterizedHoverAction hover() { return new ParameterizedHoverAction(this); }
}
```

`withArgs()` and `resolve()` are inherited from `ParameterizedAction`. No duplication.

---

## Versioning (CHANGELOG.md)

Candidate release: minor version bump (new public capability interface + new action type).
Exact version TBD at merge time.

---

## Verification

1. `mvn test-compile` -- no stub breakage (no interface method added to `UIEngine`).
2. IDE check: `LOGIN_BUTTON.click().withArgs("x")` does not compile.
3. `mvn test -Dtest=ParameterizedActionTest,ConcreteActionsTest,ElementActionTest`
4. Run `SauceDemoTest` -- no `engine().resolve(..., slug)` call sites; all slug-specific
   actions use `click().withArgs()`.
