# Phase 4 — Capability-Driven Hook Selection

**Status:** Done (hook-selection design superseded by Phase 5 SoC correction)  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — no API removal; `safely()` behavior is identical for all existing capabilities

> **Note:** Part B of this phase (capability-declared safe profiles via `safeProfile()` on
> `ActionCapabilityProvider` and `*_SAFE_PROFILE` constants on capability interfaces) was
> identified as a Separation of Concerns violation and replaced by Phase 5. The capability
> resolution fix (Part A) and `ActionProfiles.DEFAULT_SAFE` constant remain. See
> `phase-5-execution-policy-action-layer.md` for the corrected design.

---

## Objective

Complete Phase 3's self-description promise. Wire `ActionCapabilityProvider` into the action pipeline so that `safely()` delegates to the capability that owns the knowledge of which hooks it needs — not to a central dispatcher. Eliminate the final centralized capability-to-behavior mapping in the framework.

---

## Context

Phase 3 added `ActionCapabilityProvider` so all 14 capability interfaces declare their own `ActionCapability`. Two gaps remain that make Phase 3's work structurally incomplete.

### Gap 1 — `ElementActions.capabilityFor()` bypasses self-description

```java
// Current — ignores ActionCapabilityProvider entirely
private static ActionCapability capabilityFor(Element element, ElementRole role) {
    if (element instanceof Selectable) return ActionCapability.SELECTABLE;
    if (element instanceof Typeable)   return ActionCapability.TYPEABLE;
    if (element instanceof Clickable)  return ActionCapability.CLICKABLE;
    // All other capabilities → UNKNOWN
}
```

A `Hoverable`, `Checkable`, or `Uploadable` element produces an action that reports capability `UNKNOWN`. Trace output, logging, and profile switches all receive incorrect metadata for 11 of 14 capability types. Phase 3's self-description delivers no benefit to the action pipeline until this is fixed.

### Gap 2 — `Profiles.SAFE` centralizes hook selection behind a switch

```java
// Must be modified every time a new capability needs different safe hooks
return switch (action.capability()) {
    case TYPEABLE   -> List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE);
    case SELECTABLE -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, ...);
    case CLICKABLE  -> List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE);
    default         -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE);
};
```

This is a centralized dispatcher. Adding a new capability with distinct safe-execution requirements requires modifying `Profiles.java`. This is the same Open/Closed violation that Phase 3 solved for capability identity — Phase 4 solves it for capability behavior.

### Why Not a `HookStrategyResolver`?

A central resolver mapping `capability()` → strategy class is structurally identical to this switch. The dispatch mechanism changes; the centralization does not. Both require modification per new capability. The name of a dispatcher does not change its architectural role.

### The Correct Principle

Phase 3 established: capabilities self-describe what they are.  
Phase 4 establishes: capabilities declare their default safe execution profile — a framework convention, not an intrinsic property of the capability.

`Clickable` means "this object can be clicked." That is identity. The default safe-click profile is a framework opinion about how clicking is done safely, which could vary across execution contexts (web, mobile, desktop) even though the capability identity is the same. The default is owned by the capability interface because it knows its own interaction model — but the word "default" is important: it is overridable by context.

The knowledge of "which hooks does a Clickable typically need" belongs on `Clickable`. The knowledge of "which hooks does a Typeable typically need" belongs on `Typeable`. Neither belongs in `Profiles.java`.

---

## Target Design

### Part A — Fix `ElementActions.capabilityFor()`

```java
private static ActionCapability capabilityFor(Element element, ElementRole role) {
    // Phase 3 self-description is the primary path
    if (element instanceof ActionCapabilityProvider p) return p.capability();
    // Fallback for elements that predate ActionCapabilityProvider
    if (role == ElementRole.INPUT)   return ActionCapability.TYPEABLE;
    if (role == ElementRole.LIST)    return ActionCapability.SELECTABLE;
    if (role == ElementRole.TRIGGER) return ActionCapability.CLICKABLE;
    return ActionCapability.UNKNOWN;
}
```

One change. All 14 capability types now report accurate metadata through the action pipeline. Trace output, logging, and the SAFE/RELIABLE switches all receive correct capability data.

---

### Part B — `ActionProfiles` constant class and `ActionCapabilityProvider.safeProfile()`

`ActionProfiles` is a new utility class that holds shared, framework-level `ActionProfile` constants. Its purpose is to give capabilities a switch-free, shared immutable object to inherit from — instead of returning a profile enum that resolves hooks through a capability-dispatch switch.

```java
// src/main/java/core/actions/ActionProfiles.java
package core.actions;

import core.interactions.hooks.Before;

public final class ActionProfiles {

    /** Generic safe profile: wait for the element to be visible. No capability dispatch. */
    public static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .build();

    private ActionProfiles() {}
}
```

With `DEFAULT_SAFE` in place, `ActionCapabilityProvider` declares it as the default:

```java
package core.actions;

public interface ActionCapabilityProvider {
    ActionCapability capability();

    /**
     * The framework default safe execution profile for this capability.
     *
     * <p>Default: {@link ActionProfiles#DEFAULT_SAFE} — wait-for-visible, no capability
     * dispatch, no switch. Override in capability interfaces that require different
     * safe-execution semantics.</p>
     *
     * <p>Implementations should return a {@code public static final} interface field
     * (initialized once at class load time), not construct a new object per call.</p>
     */
    default ActionProfile safeProfile() {
        return ActionProfiles.DEFAULT_SAFE;
    }
}
```

`ActionProfiles.DEFAULT_SAFE` is a shared immutable `ActionProfile` object with a single `WAIT_FOR_ELEMENT_VISIBLE` before-hook. No switch. No capability dispatch. Capabilities that do not override `safeProfile()` inherit this constant directly. Capabilities that do override `safeProfile()` bypass it entirely.

---

### Part C — Capability-Declared Safe Profiles

Three capability interfaces declare their own safe behavior. Two additional interfaces are forced to declare explicitly due to diamond inheritance — Java cannot resolve two conflicting `safeProfile()` defaults automatically.

Interface fields are implicitly `public static final` in Java. Profiles are created once at class load time.

```java
// Clickable.java
ActionProfile CLICKABLE_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile safeProfile() { return CLICKABLE_SAFE_PROFILE; }
```

```java
// Typeable.java
ActionProfile TYPEABLE_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile safeProfile() { return TYPEABLE_SAFE_PROFILE; }
```

```java
// Selectable.java
ActionProfile SELECTABLE_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_VISIBLE,
            Before.WAIT_FOR_ELEMENT_CLICKABLE,
            Before.WAIT_FOR_ANGULAR_LOADER)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile safeProfile() { return SELECTABLE_SAFE_PROFILE; }
```

```java
// SearchField.java — forced override: both Typeable and Clickable declare safeProfile()
// Primary interaction is typing into the search input → use Typeable behavior.
ActionProfile SEARCH_FIELD_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile safeProfile() { return SEARCH_FIELD_SAFE_PROFILE; }
```

```java
// SearchableDropdown.java — forced override: Selectable and Searchable both declare safeProfile()
// Primary interaction is selecting from a dropdown → use Selectable behavior.
ActionProfile SEARCHABLE_DROPDOWN_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_VISIBLE,
            Before.WAIT_FOR_ELEMENT_CLICKABLE,
            Before.WAIT_FOR_ANGULAR_LOADER)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

@Override
default ActionProfile safeProfile() { return SEARCHABLE_DROPDOWN_SAFE_PROFILE; }
```

---

### Inheritance Resolution Table

| Interface | `safeProfile()` source | Notes |
|-----------|----------------------|-------|
| Clickable | Self (declares own) | WAIT_FOR_ELEMENT_CLICKABLE |
| Typeable | Self (declares own) | CLEAR_FIELD + WAIT_FOR_ELEMENT_VISIBLE |
| Selectable | Self (declares own) | VISIBLE + CLICKABLE + ANGULAR_LOADER |
| Checkable | Inherits Clickable | No override needed — same interaction model |
| Hoverable | Inherits `ActionProfiles.DEFAULT_SAFE` | Generic wait, no switch |
| Uploadable | Inherits `ActionProfiles.DEFAULT_SAFE` | Generic wait, no switch |
| ReadOnly | Inherits `ActionProfiles.DEFAULT_SAFE` | Generic wait, no switch |
| Table | Inherits `ActionProfiles.DEFAULT_SAFE` | Generic wait, no switch |
| Listable | Inherits `ActionProfiles.DEFAULT_SAFE` | Generic wait, no switch |
| MultiSelectable | Inherits Selectable (Java resolves) | Listable has no override; Selectable is more specific |
| EditableTable | Inherits `ActionProfiles.DEFAULT_SAFE` via Table | Generic wait, no switch |
| SearchField | Self (forced — diamond) | Typeable behavior |
| Searchable | Inherits SearchField | Typeable behavior |
| SearchableDropdown | Self (forced — diamond) | Selectable behavior |

---

### Part D — Wire `safeProfile` into `ElementBoundAction`

`ElementActions.of()` captures the element's declared safe profile at action-creation time, before the element reference is discarded:

```java
public static Action of(Element element, ElementRole role,
                        BiConsumer<UIEngine, LocatorDescriptor> op) {
    ActionCapability capability = capabilityFor(element, role);
    ActionProfile safeProfile = element instanceof ActionCapabilityProvider p
            ? p.safeProfile()
            : ActionProfiles.DEFAULT_SAFE;
    Action base = new ElementBoundAction(element, role, op, capability, safeProfile);
    return ActionProfiles.applyConfiguredDefault(base);
}
```

`ElementBoundAction` stores the profile and overrides `safely()`:

```java
// In ElementBoundAction:
private final ActionProfile safeProfile;

@Override
public Action safely() {
    return using(this.safeProfile);
}
```

`Action.safely()` default (lambda actions with no element) continues to call `using(Profiles.SAFE)` unchanged.

---

### What `Profiles.SAFE` Becomes

`Profiles.SAFE` is not modified. It remains valid for:
1. `action.using(Profiles.SAFE)` called on any action type (backward compatible)
2. Lambda actions — `Action.safely()` default still calls `using(Profiles.SAFE)`, which applies the switch

The switch inside `Profiles.SAFE.before(action)` continues to exist. It is no longer in the default path for element-bound actions that implement `ActionCapabilityProvider`. Capability interfaces that do not override `safeProfile()` now inherit `ActionProfiles.DEFAULT_SAFE` — a direct immutable profile with no switch — not `Profiles.SAFE`.

The net effect: `Profiles.SAFE`'s switch is reached only when a caller explicitly passes `Profiles.SAFE` to `.using()`. Capability-declared profiles go directly to their static profile constants.

---

## Affected Files

New:
- `src/main/java/core/actions/ActionProfiles.java` — shared `ActionProfile` constants; `DEFAULT_SAFE` is the switch-free generic safe profile

Modified:
- `src/main/java/core/actions/ActionCapabilityProvider.java` — add `safeProfile()` default returning `ActionProfiles.DEFAULT_SAFE`
- `src/main/java/core/actions/ElementActions.java` — fix `capabilityFor()`, add `safeProfile` capture and `ElementBoundAction.safely()` override
- `src/main/java/elements/api/capability/Clickable.java` — add `CLICKABLE_SAFE_PROFILE` constant and `safeProfile()` override
- `src/main/java/elements/api/capability/Typeable.java` — add `TYPEABLE_SAFE_PROFILE` constant and `safeProfile()` override
- `src/main/java/elements/api/capability/Selectable.java` — add `SELECTABLE_SAFE_PROFILE` constant and `safeProfile()` override
- `src/main/java/elements/api/capability/SearchField.java` — add `SEARCH_FIELD_SAFE_PROFILE` constant and `safeProfile()` override (diamond resolution)
- `src/main/java/elements/api/capability/SearchableDropdown.java` — add `SEARCHABLE_DROPDOWN_SAFE_PROFILE` constant and `safeProfile()` override (diamond resolution)

Net change: one new file (`ActionProfiles`), seven modified. Net reduction in framework-maintained central switch coverage.

---

## Migration Strategy

No migration required. All existing code compiles and behaves identically.

| Code path | Before Phase 4 | After Phase 4 |
|-----------|---------------|--------------|
| `clickable.click().safely()` | `Profiles.SAFE.before()` → switch hits CLICKABLE | `ElementBoundAction.safely()` → `CLICKABLE_SAFE_PROFILE` |
| Hook lists for CLICKABLE `safely()` | `[WAIT_FOR_ELEMENT_CLICKABLE]` before | `[WAIT_FOR_ELEMENT_CLICKABLE]` before — **identical** |
| `clickable.click().using(Profiles.SAFE)` | switch hits CLICKABLE | switch still hits CLICKABLE — **identical** |
| `hoverable.hover()` capability reported | UNKNOWN | HOVERABLE (fixed) |
| `hoverable.hover().safely()` hooks | `Profiles.SAFE` default → `[WAIT_FOR_ELEMENT_VISIBLE]` | `ActionProfiles.DEFAULT_SAFE` → `[WAIT_FOR_ELEMENT_VISIBLE]` — **identical** |
| Lambda `action.safely()` | `Profiles.SAFE` | `Profiles.SAFE` — **identical** |

### For new capabilities added after Phase 4

Override `safeProfile()` in the new capability interface — no modification to any existing file required. This is the Open/Closed guarantee Phase 4 establishes.

---

## Checklist

### Part A — Capability resolution fix
- [x] Update `ElementActions.capabilityFor()` — first line delegates to `ActionCapabilityProvider.capability()` if element is a provider.
- [x] Verify fallback chain covers INPUT/LIST/TRIGGER roles and UNKNOWN.
- [x] Confirm `HookChainAction.capability()` still delegates to the wrapped action (no change needed — already correct).

### Part B — `ActionProfiles` and `ActionCapabilityProvider` extension
- [x] Create `ActionProfiles.java` in `core.actions` with `DEFAULT_SAFE` constant (`ActionProfile.builder().before(Before.WAIT_FOR_ELEMENT_VISIBLE).build()`).
- [x] Add `safeProfile()` default method to `ActionCapabilityProvider` returning `ActionProfiles.DEFAULT_SAFE`.
- [x] Verify no import needed (both are in `core.actions`).
- [x] Add Javadoc noting: implementations should return a `public static final` constant, not construct on each call.

### Part C — Capability overrides
- [x] Add `CLICKABLE_SAFE_PROFILE` field and `safeProfile()` override to `Clickable` (import `Before`, `After`, `ActionProfile`).
- [x] Add `TYPEABLE_SAFE_PROFILE` field and `safeProfile()` override to `Typeable`.
- [x] Add `SELECTABLE_SAFE_PROFILE` field and `safeProfile()` override to `Selectable`.
- [x] Add `SEARCH_FIELD_SAFE_PROFILE` field and `safeProfile()` override to `SearchField` (diamond — forced by compiler).
- [x] Add `SEARCHABLE_DROPDOWN_SAFE_PROFILE` field and `safeProfile()` override to `SearchableDropdown` (diamond — forced by compiler).
- [x] Compile and verify `Checkable` inherits Clickable's `safeProfile()` without an explicit override.
- [x] Compile and verify `MultiSelectable` resolves to Selectable's `safeProfile()` without an explicit override.
- [x] Compile and verify `Searchable` inherits SearchField's `safeProfile()`.

### Part D — `ElementBoundAction` wiring
- [x] Add `safeProfile` field to `ElementBoundAction`.
- [x] Update `ElementActions.of()` to capture `safeProfile` from the element at creation time.
- [x] Add `safely()` override in `ElementBoundAction` returning `using(this.safeProfile)`.

---

## Tests

- [x] `ElementActions.of(clickable, ...)` produces action with `capability() == CLICKABLE` (not `UNKNOWN`).
- [x] `ElementActions.of(hoverable, ...)` produces action with `capability() == HOVERABLE` (not `UNKNOWN`).
- [x] `ElementActions.of(checkable, ...)` produces action with `capability() == CHECKABLE` (not `UNKNOWN`).
- [ ] `ElementActions.of(uploadable, ...)` produces action with `capability() == UPLOADABLE`.
- [x] `clickable.click().safely()` applies `[WAIT_FOR_ELEMENT_CLICKABLE]` before and `[WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]` after.
- [x] `typeable.type("x").safely()` applies `[CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE]` before and `[HIGHLIGHT_ELEMENT]` after.
- [x] `selectable.select("v").safely()` applies `[WAIT_FOR_ELEMENT_VISIBLE, WAIT_FOR_ELEMENT_CLICKABLE, WAIT_FOR_ANGULAR_LOADER]` before.
- [x] `checkable.check().safely()` produces the same hook list as `clickable.click().safely()` without any override in `Checkable`.
- [x] `searchField.search("x").safely()` applies Typeable-equivalent hooks.
- [x] Lambda `action.safely()` falls back to `Profiles.SAFE` behavior.
- [x] `clickable.click().using(Profiles.SAFE)` produces the identical hook list as before this phase (backward compatibility).
- [x] `typeable.type("x").using(Profiles.SAFE)` produces the identical hook list as before this phase.
- [x] All Phase 1, Phase 2, Phase 3 tests pass unchanged.

---

## Exit Criteria

- [x] `ElementActions.capabilityFor()` contains no `instanceof` checks against specific capability types. All capability types report correct metadata through the action pipeline.
- [x] `action.safely()` on any element-bound action uses the capability's own declared safe profile.
- [x] Adding a new capability interface that overrides `safeProfile()` requires zero changes to any existing framework file.
- [x] No `HookStrategyResolver`, `HookStrategy`, or parallel strategy class hierarchy exists.
- [x] All existing tests pass without modification.

---

## What NOT to Do

- Do not create `HookStrategyResolver` or any class whose sole responsibility is mapping a capability to a strategy. This is the centralized dispatcher pattern Phase 4 eliminates.
- Do not create a `HookStrategy` interface — `ActionProfile` already fills this responsibility. A second interface for the same concept creates taxonomy confusion.
- Do not remove the switch from `Profiles.SAFE` — it remains correct for `.using(Profiles.SAFE)` callers and for lambda actions. It is the backward-compatible path, not the primary path.
- Do not add `reliableProfile()` in this phase. `Profiles.RELIABLE` is less frequently used. Add `reliableProfile()` and a `reliable()` shorthand in a future phase once the pattern is proven.
- Do not use a generic field name like `SAFE_PROFILE` in capability interfaces — use the capability-specific name (e.g. `CLICKABLE_SAFE_PROFILE`) to prevent ambiguity in sub-interface contexts.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
