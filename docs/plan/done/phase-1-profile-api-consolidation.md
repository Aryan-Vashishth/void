# Phase 1 — Profile API Consolidation

**Status:** Complete  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — additive changes only

---

## Objective

Make profile APIs predictable. Establish one canonical way to apply behavior to actions. Align all demo and documentation examples to the same style so contributors are never confused about which API to use.

---

## Context

The current `Action` interface offers:

```java
void perform(UIEngine engine)
LocatorDescriptor resolve(UIEngine engine)
Action withHooks(List<ActionHandler> before, List<ActionHandler> after)
```

The feature branch adds shorthand profiles:

```java
Action safely()
Action debug()
Action raw()
Action using(ActionProfile profile)
```

The problem is that examples and docs currently mix styles. Some use `withHooks(...)` directly. Some use shorthand. Some reference the old `.before(...)/.after(...)` API that no longer exists. This creates confusion for contributors.

---

## What Each Profile Expands To

| Method | Capability | Before Hooks | After Hooks |
|---|---|---|---|
| `safely()` | Clickable | `WAIT_FOR_ELEMENT_CLICKABLE` | `WAIT_FOR_ANGULAR_LOADER`, `HIGHLIGHT_ELEMENT` |
| `safely()` | Typeable | `CLEAR_FIELD`, `WAIT_FOR_ELEMENT_VISIBLE` | `HIGHLIGHT_ELEMENT` |
| `safely()` | Selectable | `WAIT_FOR_ELEMENT_VISIBLE`, `WAIT_FOR_ELEMENT_CLICKABLE` | `HIGHLIGHT_ELEMENT` |
| `debug()` | Any | `HIGHLIGHT_ELEMENT`, `LOG_INTENT` | `HIGHLIGHT_ELEMENT` |
| `raw()` | Any | _(none)_ | _(none)_ |

---

## Affected Files

- `src/main/java/core/actions/Action.java` — public API surface
- `src/main/java/core/actions/ActionProfiles.java` — profile implementations
- `src/main/java/core/actions/Profiles.java` — preset constants
- `src/main/java/core/actions/Profile.java` — profile interface
- `src/main/java/tests/demo/VoidDemo.java` — reference usage example

---

## Checklist

### API Surface
- [x] Confirm exactly which methods are public on `Action` and write them down.
- [x] Ensure `withHooks(List, List)` is still available as the power-user escape hatch.
- [x] Ensure `safely()`, `debug()`, `raw()` all delegate to `withHooks` internally (no duplicate logic).
- [x] Ensure `using(ActionProfile)` accepts any `ActionProfile` implementation including custom ones.

### Profiles Behavior
- [x] Verify `safely()` on a `Clickable` action applies the correct before/after hook set.
- [x] Verify `safely()` on a `Typeable` action applies the correct before/after hook set.
- [x] Verify `debug()` applies logging and highlight hooks only.
- [x] Verify `raw()` applies no hooks.

### Demo and Examples
- [x] Update `VoidDemo.loginWithHookedActions()` to demonstrate `safely()` or `using(Profiles.SAFE)` as primary example.
- [x] Keep at least one `withHooks(...)` example as the advanced usage reference.
- [x] Remove any remaining references to non-existent `.before(...)/.after(...)` chaining.

### Tests
- [x] Unit test: `safely()` on click action produces the expected hook order.
- [x] Unit test: `debug()` on type action produces the expected hook order.
- [x] Unit test: `raw()` skips all hooks and calls perform directly.
- [x] Unit test: `using(customProfile)` applies hooks from the custom profile.

### Documentation
- [x] Update `src/main/java/core/actions/README.md` with profile API examples.
- [x] Update `docs/architecture/hooks-pipeline.md` to show `safely()` as the primary path.

---

## Exit Criteria

- All four profile entry points compile and have test coverage.
- No mixed API styles in `VoidDemo.java`.
- `mvn test` passes in CI on `feature/action-package-refactor`.

---

## What NOT to Do

- Do not add more methods to `Action` without updating the public method count.
- Do not make `safely()` behavior configurable via system properties in this phase (that is Phase 5).
- Do not rename `withHooks` — it remains the low-level contract.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

