# Phase 9 — Profile Completion

**Status:** Ongoing (blocked on Phase 4)  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — additive only; no existing behavior changes

---

## Objective

Extend the `ActionCapabilityProvider.safeProfile()` pattern from Phase 4 to all remaining execution profile shorthands. After Phase 9, every standard profile (`safely()`, `reliable()`, `fast()`, `debug()`) resolves through capability-declared constants — no central switch for any of them. Adding a new profile type in a future phase requires no changes to existing files.

---

## Context

Phase 4 established `safeProfile()` and `ActionProfiles.DEFAULT_SAFE`. The same mechanism applies to every other profile shorthand. Phase 4 proves the pattern works; Phase 9 completes it.

The naming choice matters: this phase is called "Profile Completion" and not "Reliable Profile Extension" because the scope is all profiles, not just reliable. Future profiles (`flakyProfile()`, `accessibilityProfile()`, etc.) follow the same pattern — no new phase required.

### The Symmetry

| Shorthand | Profile method | Framework constant | Phase added |
|-----------|---------------|-------------------|-------------|
| `safely()` | `safeProfile()` | `ActionProfiles.DEFAULT_SAFE` | Phase 4 |
| `reliable()` | `reliableProfile()` | `ActionProfiles.DEFAULT_RELIABLE` | Phase 9 |
| `fast()` | `fastProfile()` | `ActionProfiles.DEFAULT_FAST` | Phase 9 |
| `debug()` | `debugProfile()` | `ActionProfiles.DEFAULT_DEBUG` | Phase 9 |

---

## Target Design

### `ActionProfiles` additions

```java
public final class ActionProfiles {

    /** Phase 4 — already added */
    public static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .build();

    /** Phase 9 — reliable: longer waits, retry-oriented hooks */
    public static final ActionProfile DEFAULT_RELIABLE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .build();

    /** Phase 9 — fast: minimal hooks, no visual feedback */
    public static final ActionProfile DEFAULT_FAST = ActionProfile.builder()
            .build();   // empty — no before/after hooks

    /** Phase 9 — debug: verbose feedback, highlight everything */
    public static final ActionProfile DEFAULT_DEBUG = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .after(After.HIGHLIGHT_ELEMENT)
            .build();

    private ActionProfiles() {}
}
```

### `ActionCapabilityProvider` additions

```java
default ActionProfile reliableProfile() {
    return ActionProfiles.DEFAULT_RELIABLE;
}

default ActionProfile fastProfile() {
    return ActionProfiles.DEFAULT_FAST;
}

default ActionProfile debugProfile() {
    return ActionProfiles.DEFAULT_DEBUG;
}
```

### `ElementBoundAction` additions

```java
@Override
public Action reliable() {
    return using(capability instanceof ActionCapabilityProvider p
            ? p.reliableProfile()
            : ActionProfiles.DEFAULT_RELIABLE);
}

@Override
public Action fast() {
    return using(capability instanceof ActionCapabilityProvider p
            ? p.fastProfile()
            : ActionProfiles.DEFAULT_FAST);
}

@Override
public Action debug() {
    return using(capability instanceof ActionCapabilityProvider p
            ? p.debugProfile()
            : ActionProfiles.DEFAULT_DEBUG);
}
```

Or capture all four profile references at construction time inside `ElementBoundAction`, the same way `safeProfile` is captured in Phase 4. Prefer capture at construction time to avoid repeated `instanceof` at call time.

### Capability overrides (where behavior differs meaningfully)

Override `reliableProfile()` in `Clickable`, `Typeable`, `Selectable` where the reliable profile has different hooks than the default. Do not override for capabilities where `DEFAULT_RELIABLE` is already correct. Do not add override boilerplate for the sake of completeness — only override when the capability has something different to say.

```java
// Clickable.java — reliable: full element-readiness check with retry posture
ActionProfile CLICKABLE_RELIABLE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile reliableProfile() { return CLICKABLE_RELIABLE_PROFILE; }
```

```java
// Typeable.java — reliable: clear + wait, with highlight for confirmation
ActionProfile TYPEABLE_RELIABLE_PROFILE = ActionProfile.builder()
    .before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile reliableProfile() { return TYPEABLE_RELIABLE_PROFILE; }
```

Exact hook choices for `fastProfile()` and `debugProfile()` overrides are determined by reviewing the existing `Profiles.FAST` and `Profiles.DEBUG` switch arms at implementation time.

---

## Affected Files

Modified:
- `src/main/java/core/actions/ActionProfiles.java` — add `DEFAULT_RELIABLE`, `DEFAULT_FAST`, `DEFAULT_DEBUG`
- `src/main/java/core/actions/ActionCapabilityProvider.java` — add `reliableProfile()`, `fastProfile()`, `debugProfile()` defaults
- `src/main/java/core/actions/ElementActions.java` — capture and wire `reliableProfile`, `fastProfile`, `debugProfile` in `ElementBoundAction`
- `src/main/java/elements/api/capability/Clickable.java` — override profile methods where behavior differs
- `src/main/java/elements/api/capability/Typeable.java` — override profile methods where behavior differs
- `src/main/java/elements/api/capability/Selectable.java` — override profile methods where behavior differs
- `src/main/java/elements/api/capability/SearchField.java` — forced diamond resolution for any newly conflicting profile methods
- `src/main/java/elements/api/capability/SearchableDropdown.java` — forced diamond resolution

---

## Migration Strategy

No migration required. All existing behavior is preserved via fallback to `DEFAULT_*` constants.

---

## Checklist

### `ActionProfiles` additions
- [ ] Add `DEFAULT_RELIABLE` constant to `ActionProfiles`.
- [ ] Add `DEFAULT_FAST` constant to `ActionProfiles`.
- [ ] Add `DEFAULT_DEBUG` constant to `ActionProfiles`.
- [ ] Verify hook lists match the behavior of the corresponding `Profiles.*` enum switch arms.

### `ActionCapabilityProvider` extensions
- [ ] Add `reliableProfile()` default returning `ActionProfiles.DEFAULT_RELIABLE`.
- [ ] Add `fastProfile()` default returning `ActionProfiles.DEFAULT_FAST`.
- [ ] Add `debugProfile()` default returning `ActionProfiles.DEFAULT_DEBUG`.

### `ElementBoundAction` wiring
- [ ] Add `reliableProfile`, `fastProfile`, `debugProfile` fields, captured at construction time.
- [ ] Override `reliable()` to call `using(this.reliableProfile)`.
- [ ] Override `fast()` to call `using(this.fastProfile)`.
- [ ] Override `debug()` to call `using(this.debugProfile)`.

### Capability overrides
- [ ] Review `Profiles.RELIABLE`, `Profiles.FAST`, `Profiles.DEBUG` switch arms for Clickable, Typeable, Selectable.
- [ ] Add overrides only where the capability-specific behavior differs from `DEFAULT_*`.
- [ ] Verify diamond resolution in `SearchField` and `SearchableDropdown` where needed.

---

## Tests

- [ ] `clickable.click().reliable()` applies `CLICKABLE_RELIABLE_PROFILE` hooks.
- [ ] `typeable.type("x").reliable()` applies `TYPEABLE_RELIABLE_PROFILE` hooks.
- [ ] `hoverable.hover().reliable()` applies `DEFAULT_RELIABLE` hooks (no override needed).
- [ ] `hoverable.hover().fast()` applies `DEFAULT_FAST` hooks (empty — no before/after).
- [ ] `clickable.click().debug()` applies debug hooks (highlight visible in output).
- [ ] Lambda `action.reliable()` falls back to `Profiles.RELIABLE` behavior.
- [ ] `clickable.click().using(Profiles.RELIABLE)` produces identical hook list as before this phase.
- [ ] All Phase 4 tests pass unchanged.

---

## Exit Criteria

- All four profile shorthands resolve through capability-declared constants for element-bound actions.
- Adding a new capability with a custom `reliableProfile()` requires zero changes to any existing file.
- `Profiles.*` enum switch coverage continues to work correctly for `.using(Profiles.*)` callers.
- No new central dispatcher or strategy class exists.

---

## What NOT to Do

- Do not override `fastProfile()` or `debugProfile()` in every capability — only where behavior meaningfully differs from the `DEFAULT_*` constant.
- Do not add profile methods for non-existent shorthands — `safeProfile()`, `reliableProfile()`, `fastProfile()`, `debugProfile()` correspond to the four existing shorthands in `Action`. Add others only when a new shorthand is added to `Action`.
- Do not remove the switches from `Profiles.RELIABLE`, `Profiles.FAST`, or `Profiles.DEBUG` — they remain valid for `.using(Profiles.*)` callers.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
