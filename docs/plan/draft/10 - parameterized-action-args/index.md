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
| C3 | `SortDropdown.Controls.SORT_SELECT` declared `Clickable` instead of `Selectable` | `examples.pages.saucedemo` |

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
ElementAction  (existing, +locatorArgs() hook)
  └── ClickableElementAction  (existing, unchanged)
        └── ClickAction  (existing, no longer final)
              └── ParameterizedClickAction  (new) ← storedArgs + withArgs() + locatorArgs() override
        └── HoverAction  (future Phase 2 pattern)
              └── ParameterizedHoverAction  (Phase 2)
        └── ...

Clickable  (existing, unchanged)
  └── ParameterizedClickable  (new)  ← overrides click() with covariant return type
```

`ParameterizedClickAction extends ClickAction` (not a separate `ParameterizedAction<T>` base).
This gives it the correct type for the covariant return override in `ParameterizedClickable` --
`ClickAction` is the overridden return type, `ParameterizedClickAction` is the subtype.

`withArgs()` returns `ParameterizedClickAction` (concrete type) directly -- no CRTP needed
since the class is `final`. No generic suppression warnings.

### Why not a shared `ParameterizedAction<T>` base class

The CRTP base was the original plan. Java covariant return requires the override's return
type to be a strict subtype of the overridden method's return type:

```
Clickable.click() → ClickAction
ParameterizedClickable.click() → ParameterizedClickAction  ← must extend ClickAction
```

`ParameterizedAction<T> extends ElementAction` creates a sibling branch alongside
`ClickAction` -- `ParameterizedClickAction extends ParameterizedAction<T>` would NOT be a
subtype of `ClickAction`, so the covariant override would not compile.

The simplest correct solution: `ParameterizedClickAction extends ClickAction` directly.
`ClickAction` was `final`; removing `final` enables this. `ParameterizedClickAction` inherits
`execute()`, the CLICKABLE profiles, and `operationLabel()` from the parent -- no duplication.

Phase 2 (hover, type) follows the same pattern: `ParameterizedHoverAction extends HoverAction`
(if `HoverAction` is made non-final), etc. The duplication is minimal -- only `storedArgs`
field, `withArgs()`, and `locatorArgs()` override (8 lines per class).

### Action lifecycle

VOID's paradigm: elements emit actions, actions compose flows, flows are executed by the
runtime. `withArgs()` fits this model -- it returns the action (still just a description
of intent), which is then added to a `Flow` and consumed by `app.run()`. The caller never
invokes execution directly.

```
Element constant (enum)
    │
    │  .click()
    ▼
ParameterizedClickAction         ← description of intent; locatorArgs = NO_ARGS
    │
    │  .withArgs("sauce-labs-backpack")
    ▼
ParameterizedClickAction         ← locatorArgs = ["sauce-labs-backpack"] (same instance)
    │
    │  added to Flow
    ▼
app.run(Flow.of(...))            ← execution boundary; runtime owns execution
    │
    ▼
FlowExecutor → action.perform()  ← internal lifecycle, not called by user
    │
    ▼
resolve()                        ← engine.resolve(element, TRIGGER, locatorArgs)
    │                               effectiveArgs: locatorArgs non-empty → used as-is
    ▼
engine.click(descriptor)         ← SeleniumEngine / PlaywrightEngine -- engine-neutral
```

Usage in examples:

```java
app.run(Flow.of(
    FIRST_NAME_INPUT.type(first),
    LAST_NAME_INPUT.type(last),
    ADD_TO_CART_BUTTON.click().withArgs(slug),   // ← still just an Action in the flow
    CONTINUE_BUTTON.click()
));
```

The "one-shot" concern effectively disappears: since actions are passed immediately into
`Flow.of()`, there is no window to mutate them after execution. The rule is simply:
**actions are immutable descriptions of work consumed by the runtime**.

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
| `src/main/java/domain/automation/web/vocabulary/actions/ElementAction.java` | Add `protected Object[] locatorArgs()` hook; update `resolve()` to call it |
| `src/main/java/domain/automation/web/vocabulary/actions/ClickAction.java` | Remove `final` modifier to allow subclassing by `ParameterizedClickAction` |
| `src/main/java/domain/automation/web/vocabulary/actions/ParameterizedClickAction.java` | New concrete: extends `ClickAction`; adds `storedArgs`, `withArgs()`, `locatorArgs()` override |
| `src/main/java/domain/automation/web/vocabulary/capability/ParameterizedClickable.java` | New interface: extends `Clickable`; overrides `click()` with covariant `ParameterizedClickAction` return |
| `src/main/java/examples/demo/pages/saucedemo/ProductsPage.java` | Split `ProductItem.Buttons` → `Buttons` (static) + `DynamicButtons` (`ParameterizedClickable`); `Controls.SORT_SELECT` → `Selectable` |
| `src/main/java/examples/demo/pages/saucedemo/CartPage.java` | Split `CartItem.Buttons` → `Buttons` (static) + `DynamicButtons` (`ParameterizedClickable`) |
| `src/main/resources/examples/demo/pages/SauceDemo/ProductsPage/locators.properties` | Add `SortDropdown.Controls.SORT_SELECT.LIST` XPath if absent; re-sync |
| `src/main/java/examples/demo/SauceDemoTest.java` | Replace `engine().click(resolve(..., slug))` with `click().withArgs(slug)`; `selectByVisibleText()` with `selectByText()` |
| `src/test/java/elements/api/actions/ParameterizedActionTest.java` | New -- 6 unit examples (see below) |

**No changes to:** `Clickable`, `UIEngine`, `SeleniumEngine`.
**Minimal change to:** `ElementAction` -- add `protected Object[] locatorArgs()` hook; change `resolve()` to call it.
**Removed `final` from:** `ClickAction` -- required to allow `ParameterizedClickAction extends ClickAction` for covariant return.

### `ElementAction` change (minimal, required)

`ElementAction.resolve()` is `final` -- subclasses cannot override it. The fix is a
protected hook method that `resolve()` delegates to for the args array:

```java
// Added to ElementAction -- hook for subclasses that need to supply locator args.
// Base returns empty array so existing actions are unaffected.
protected Object[] locatorArgs() {
    return new Object[0];
}

// resolve() updated to call the hook:
@Override
public final LocatorDescriptor resolve(Executor executor) {
    UIEngine engine = (UIEngine) executor;
    return engine.resolve(element, role, locatorArgs());  // was: engine.resolve(element, role)
}
```

All existing action subclasses inherit the no-arg default transparently.

### `ParameterizedClickAction` (new concrete)

Extends `ClickAction` directly. Inherits `execute()`, profiles, and `operationLabel()`.
Only adds `storedArgs`, `withArgs()`, and the `locatorArgs()` hook override:

```java
public final class ParameterizedClickAction extends ClickAction {

    private Object[] storedArgs = Target.NO_ARGS;

    public ParameterizedClickAction(ParameterizedClickable element) {
        super(element);
    }

    public ParameterizedClickAction withArgs(Object... args) {
        this.storedArgs = (args != null) ? args : Target.NO_ARGS;
        return this;
    }

    @Override
    protected Object[] locatorArgs() {
        return storedArgs;
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

When a use case requiring parameterized hover or type operations arises, the same pattern
applies: extend the concrete action class, add `storedArgs` + `withArgs()` + `locatorArgs()`.

```java
// HoverAction must be made non-final (same as ClickAction was for Phase 1)
public final class ParameterizedHoverAction extends HoverAction {
    private Object[] storedArgs = Target.NO_ARGS;
    public ParameterizedHoverAction(ParameterizedHoverable element) { super(element); }
    public ParameterizedHoverAction withArgs(Object... args) { this.storedArgs = ...; return this; }
    @Override protected Object[] locatorArgs() { return storedArgs; }
}
interface ParameterizedHoverable extends Hoverable {
    @Override default ParameterizedHoverAction hover() { return new ParameterizedHoverAction(this); }
}
```

The duplication is minimal: 8 lines per class (`storedArgs`, `withArgs()`, `locatorArgs()`).
The heavy lifting is in `ElementAction.locatorArgs()` hook -- shared by all subclasses.

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
