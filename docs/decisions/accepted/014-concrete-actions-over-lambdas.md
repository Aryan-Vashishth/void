# ADR-014 — Concrete Actions over Anonymous Lambdas

**Date:** 2026-07-07  
**Status:** Accepted

---

## Context

Before Phase 14, every capability method returned an anonymous action via `ElementActions.of()`:

```java
// Pre-Phase 14
default Action click() {
    return ElementActions.of(this, ElementRole.TRIGGER, (engine, d) -> engine.click(d));
}
```

The action was a runtime-constructed anonymous object. Its type was `ElementBoundAction` (an
internal anonymous subclass of `ElementAction`). Its execution policy was looked up at runtime
via `ActionProfiles.safeProfileFor(action.capability())`. Every action of the same capability
type was structurally identical.

The Phase 13–15 refactor introduced 17 concrete `ElementAction` subclasses — one per interaction
type — and changed capability methods to return them directly.

---

## Decision

**Capability methods emit typed concrete action subclasses. Anonymous lambdas are banned from
production capability interfaces.**

```java
// Post-Phase 14
default ClickAction click() {
    return new ClickAction(this);
}
```

Each concrete action type:
- Declares its locator role in the constructor
- Declares its `ActionCapability` in the constructor
- Overrides `execute(UIEngine, LocatorDescriptor)` with the specific engine call
- Inherits `perform()`, `safely()`, `debug()`, `reliable()`, `raw()` from `ElementAction`
- Overrides `defaultSafeProfile()` / `defaultReliableProfile()` only when its policy differs from the default

---

## Why Not Anonymous Lambdas

| Concern | Anonymous lambda | Concrete class |
|---------|-----------------|----------------|
| Type identity | Structural, no name | Named — `ClickAction`, `TypeAction` |
| Debuggability | "anonymous ElementBoundAction" in traces | Full class name in stack traces |
| Policy ownership | Looked up at runtime from capability enum | Declared in the class |
| Covariant return | Impossible — always `Action` | Possible — `Clickable.click()` returns `ClickAction` |
| Extension | Fork `ElementActions.of()` call | Override `defaultSafeProfile()` in subclass |
| Testability | Hard to assert specific type | `assertInstanceOf(ClickAction.class, ...)` |
| Readable intent | `(engine, d) -> engine.click(d)` | `execute(): engine.click(descriptor)` |

---

## Covariant Return Types

Concrete action types enable covariant return, which preserves type information across the fluent
chain:

```java
ClickAction click = LoginPage.SUBMIT.click();    // still a ClickAction reference
Action safe       = click.safely();              // wrapped — loses type, but that's fine
                                                 // caller assigns to Action anyway
```

Test code can assert the specific type:
```java
assertInstanceOf(ClickAction.class, LoginPage.SUBMIT.click());
```

This is not possible with anonymous lambdas.

---

## `ElementActions` Retained as @Internal

`ElementActions.of()` is kept as an `@Internal` factory for two legitimate uses:
1. Test infrastructure — tests that verify hook pipelines need element-bound actions with
   programmable execute behavior that no concrete subclass provides
2. Framework-internal edge cases — one-off element-bound actions without a dedicated subclass

Production capability interfaces must not call `ElementActions.of()`. See ADR-012.

---

## Consequences

- 17 concrete action subclasses: `ClickAction`, `TypeAction`, `ClearAction`, `AppendTypeAction`,
  `TypeAndPressAction`, `ToggleAction`, `CheckAction`, `HoverAction`, `OpenAction`,
  `SelectAction`, `SelectByTextAction`, `SelectByValueAction`, `UploadAction`,
  `TypeSearchAction`, `SubmitSearchAction`, `SearchAndSelectAction`, `ReadTextAction`
- `ElementActions.java` is `@Internal` — no production capability interface calls it
- Stack traces name the action class explicitly
- Capability return types are covariant (e.g., `Clickable.click()` → `ClickAction`)
- New interaction type → new `ElementAction` subclass, no factory change
