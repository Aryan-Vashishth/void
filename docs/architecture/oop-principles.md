# OOP Principles in VOID

Reference document for VOID's SOLID application. Violations are tracked using IDs P1-P11
in `docs/plan/draft/oop-violations-remediation/index.md`. For the protocol on handling
violations found during development, see `docs/contributing/architecture-rules.md`.

---

## Open/Closed Principle (highest priority)

Classes and interfaces must be **open for extension, closed for modification**. Adding a new
engine, capability, or action type should require adding a new class -- not editing an
existing one.

### Violation: instanceof dispatch chain (P1, P2)

`Action.java` checks the runtime type of `this` to decide behavior:

##### Examples

```java
// core/actions/Action.java -- P1 violation (4 sites)
default Action before(@Nullable BeforeActionHandler... hooks) {
    if (this instanceof HookChainAction chain) {          // type check on self
        return chain.withAdditionalHooks(toList(hooks), null);
    }
    return new HookChainAction(this, toList(hooks), null);
}

default Action withProfile(ActionProfile profile) {
    Action profiled = profile.apply(this);
    if (profiled instanceof HookChainAction chain) {      // type check again
        profiled = chain.withProfileName(profile.name());
    }
    return profiled;
}
```

`VoidDSL.java` branches on the runtime type of a method argument:

```java
// dsl/VoidDSL.java -- P2 violation
if (first instanceof MultiSelectable multiDropdown) {
    engine.triggerDropdown(multiDropdown, dropdownIndex);
} else if (first instanceof Selectable singleDropdown) {
    engine.triggerDropdown(singleDropdown);
} else {
    throw new IllegalArgumentException(...);
}
```

Both are OCP violations: adding a new type requires modifying existing methods.

**The fix:** Put the behavior on the type. `HookChainAction` should expose an extension hook
(`mergeHooks`) that `Action.before()` calls directly with no type check. `VoidDSL` should
call `element.triggerAction()` and let each capability interface dispatch.

---

### Violation: switch-on-string type selector (P8)

`UIEngineFactory.java` selects an engine implementation by comparing a string:

##### Examples

```java
// core/engine/UIEngineFactory.java -- P8 violation
UIEngine engine = switch (engineName) {
    case "selenium" -> {
        if (bootstrap instanceof EngineBootstrap.FromProfile fp) {
            yield new SeleniumEngine(fp.profile());
        } else {
            throw new IllegalStateException(...);
        }
    }
    // Adding "playwright" requires editing this method
    default -> throw new IllegalStateException(
            "Unsupported engine: '" + engineName + "'. Supported: selenium");
};
```

Every new engine requires modifying this switch.

**The fix:** An open registration map -- `Map<String, Function<EngineBootstrap, UIEngine>>`.
Registering a new engine adds an entry without modifying the factory body.

---

### Violation: switch on enum value for label (P3)

`HookChainAction.java` derives an operation label by switching on `ActionCapability`:

##### Examples

```java
// core/actions/HookChainAction.java -- P3 violation
@Override
public String operationLabel() {
    if (delegate instanceof ActionLabeled l) return l.operationLabel();
    return switch (capability()) {
        case CLICKABLE  -> "click";
        case TYPEABLE   -> "type";
        case SELECTABLE -> "select";
        default         -> "perform";
    };
}
```

Adding a new `ActionCapability` value requires editing this method.

**The fix:** `operationLabel()` should be a default method on `Action` itself, or each
capability interface should provide it. The switch disappears.

---

### Correct pattern: capability interfaces

##### Example

```java
// elements/api/capability/Clickable.java -- correct OCP pattern
public interface Clickable extends UIElement {
    default ActionCapability capability() { return ActionCapability.CLICKABLE; }
    default ClickAction click() { return new ClickAction(this); }
    // adding a new capability = adding a new interface; no existing code changes
}
```

---

## Liskov Substitution Principle

Subtypes must be substitutable for their base type. Do not rely on a runtime cast that only
works for specific implementors.

### Violation: unguarded (Enum<?>) this cast (P5)

`UIElement.java` default methods cast `this` to `Enum<?>` without verifying the type:

##### Examples

```java
// elements/api/UIElement.java -- P5 violation (4 sites)
default String getExternalFileName() {
    Enum<?> e = (Enum<?>) this;            // throws ClassCastException for any non-enum UIElement
    Class<?> enumClass = e.getDeclaringClass();
    ...
}

default String getPrimaryLocator() {
    ...
    Enum<?> e = (Enum<?>) this;            // same assumption, no guard
    Class<?> enumClass = e.getDeclaringClass();
    ...
}
```

A non-enum implementor of `UIElement` throws `ClassCastException` on any default method call.

**The fix:** `ElementSupport` centralises the cast:
`ElementSupport.nameOf(e)`, `ElementSupport.declaringClassOf(e)`.

---

### Violation: instanceof ActionLabeled fallback (P4)

##### Examples

```java
// core/actions/HookChainAction.java -- P4 violation
@Override
public String elementLabel() {
    if (delegate instanceof ActionLabeled l) return l.elementLabel();
    return "ACTION";    // silent fallback; behavior differs by concrete type of delegate
}
```

**The fix:** Promote `elementLabel()` and `operationLabel()` to `Action` with defaults.
No secondary interface or runtime check needed.

> **FIXED:** `ActionLabeled` is deleted. `elementLabel()` and `operationLabel()` are now default methods on `Action` directly.

---

## Interface Segregation Principle

A class that implements an interface should use every method it declares.

### Correct pattern: narrow interfaces

##### Example

```java
// core/actions/Action.java -- correct ISP (narrow, focused defaults)
// Note: ActionLabeled was deleted; elementLabel() and operationLabel() are now
// default methods on Action directly.
interface Action {
    default String elementLabel() { ... }    // used by every action
    default String operationLabel() { ... }  // used by every action
}
```

Each capability interface (`Clickable`, `Typeable`, `Selectable`) declares only the methods
relevant to that capability.

---

### Violation: Via grows per capability (P11)

##### Examples

```java
// core/interactions/Via.java -- P11 violation (grows with every new capability)
public static Clickable          clickable(UIElement e)          { ... }
public static Typeable           typeable(UIElement e)           { ... }
public static Selectable         selectable(UIElement e)         { ... }
public static ReadOnly           readOnly(UIElement e)           { ... }
public static Searchable         searchable(UIElement e)         { ... }
public static SearchableDropdown searchableDropdown(UIElement e) { ... }
public static MultiSelectable    multiSelectable(UIElement e)    { ... }
public static Checkable          checkable(UIElement e)          { ... }
public static Hoverable          hoverable(UIElement e)          { ... }
```

**The fix:** One generic cast helper:
`public static <T extends UIElement> T as(UIElement e, Class<T> type)`.

---

## Single Responsibility Principle

Each class has one reason to change.

### Example: FrameworkBootstrap before Phase 4

##### Examples

```java
// FrameworkBootstrap.java (pre-decoupling) -- SRP violation
public static void init() {
    configureLogging();
    suppressSeleniumLogger();   // Selenium-specific; belongs in SeleniumEngine.initialize()
}
```

`FrameworkBootstrap` is engine-agnostic. Selenium logger suppression is a second
responsibility. **Fixed in Phase 4:** moved into `SeleniumEngine.initialize()`.

---

## Dependency Inversion Principle

High-level modules must not depend on low-level modules. Both should depend on abstractions.

### Correct pattern: VOID depends on UIEngine, not SeleniumEngine

##### Example

```java
// core/runtime/VOID.java -- correct DIP
private final UIEngine engine;         // depends on the abstraction
private final SessionContext context;  // engine-typed, not WebDriver-typed

public void shutdown() {
    engine.shutdown();                 // no knowledge of SeleniumEngine
}
```

---

### Violation: Interactions depended on WebDriver (fixed in Phase 3)

##### Examples

```java
// core/interactions/Interactions.java (pre-Phase-3) -- DIP violation
public Interactions(UIEngine engine) {
    this.engine = engine;
    DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver()); // Selenium cast
}
```

**Fixed:** `SeleniumEngine.initialize()` registers the driver. `Interactions` no longer
references `WebDriver` or `DriverContext`.

---

## Violation Map (P1-P11)

| ID | Priority | Principle | Phase | Summary |
|---|---|---|---|---|
| P1 | CRITICAL | DIP, OCP | 1 | `instanceof HookChainAction` in 4 `Action` default methods (FIXED) |
| P2 | CRITICAL | OCP | 3 | Sequential `instanceof` chains in `VoidDSL` dispatch (FIXED) |
| P3 | HIGH | OCP | 1 | `switch (ActionCapability)` in `HookChainAction.operationLabel` (FIXED) |
| P4 | HIGH | LSP, DIP | 1 | `instanceof ActionLabeled` in `HookChainAction` (FIXED) |
| P5 | HIGH | LSP | 2 | `(Enum<?>) this` hard cast in `UIElement` interface defaults (FIXED) |
| P6 | MEDIUM | DRY, LSP | 2 | Duplicated `instanceof Enum<?>` in `ElementAction` + `LocatorResolver` (FIXED) |
| P7 | MEDIUM | ISP, OCP | 2 | `instanceof ActionCapabilityProvider` in `ElementActions.capabilityFor` (FIXED) |
| P8 | MEDIUM | OCP | 4 | `switch` on engine name string in `UIEngineFactory` |
| P9 | LOW | OCP | 4 | O(n) dedup in `SearchableDropdown`/`SearchField.getAllLocatorRoles` (FIXED) |
| P10 | LOW | ISP | 2 | Forced abstract `getIndex()` in `Listable` with no default (FIXED) |
| P11 | LOW | OCP | 4 | Per-capability static helpers in `Via` growing with capability count |

Full phase assignments and remediation plan: `docs/plan/draft/oop-violations-remediation/index.md`.
