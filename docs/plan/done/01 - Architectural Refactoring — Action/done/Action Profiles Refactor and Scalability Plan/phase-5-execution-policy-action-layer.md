# Phase 5 — Execution Policy Moved to Action Layer (SoC Correction)

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — behavioral output of `safely()` is identical for all existing capabilities; only the location of policy ownership changes

---

## Problem Statement

Phase 4 placed `ActionProfile` constants and `safeProfile()` overrides directly on capability interfaces (`Clickable`, `Typeable`, `Selectable`, `SearchField`, `SearchableDropdown`). This violated Separation of Concerns.

A capability interface answers: "Can I be clicked? Where is my locator? What action do I emit? What capability am I?" It must not answer: "Which hooks run before execution? How many retries?"

The resulting dependency graph was backwards:

```
Capability → ActionProfile → Hooks → Engine
```

This meant `elements.api.capability` depended on `core.actions` (ActionProfile) and `core.interactions.hooks` (Before, After). The abstraction leaked upward — the lower layer (capability structure) knew about the higher layer (execution infrastructure).

The correct dependency direction:

```
Element → Capability → Action (owns execution policy) → ExecutionPipeline → Engine
```

Additionally, Phase 4's `safeProfile` field on `ElementAction` was dead code — `ElementAction.safely()` already called `using(Profiles.SAFE)` directly and never read the stored field.

---

## Root Cause Analysis

Phase 4's design asked the wrong question: "Which capability owns the safe profile?"

The correct question is: "Which object owns execution policy?"

The answer: **the Action** — not the capability.

Consider `DoubleClickAction` vs `ClickAction`: same capability (`CLICKABLE`), different safe execution (double-click needs different timing and angular-loader handling). If the profile lives on `Clickable`, both actions inherit the same policy, even though they should differ. The capability interface does not know which action subclass will be created.

---

## Corrected Design

### Rule

> Capabilities own: locator semantics, emitted actions, capability metadata.  
> Actions own: execution intent, execution profiles, retries, timeout defaults, tracing metadata.

### `ActionProfiles` (extended)

Three capability-specific safe profile constants added as package-private members of `core.actions`. The policy stays in the action layer:

```java
// core.actions.ActionProfiles — package-private constants, action layer only
static final ActionProfile CLICKABLE_SAFE = ActionProfile.builder()
        .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
        .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
        .build();

static final ActionProfile TYPEABLE_SAFE = ActionProfile.builder()
        .before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE)
        .after(After.HIGHLIGHT_ELEMENT)
        .build();

static final ActionProfile SELECTABLE_SAFE = ActionProfile.builder()
        .before(Before.WAIT_FOR_ELEMENT_VISIBLE,
                Before.WAIT_FOR_ELEMENT_CLICKABLE,
                Before.WAIT_FOR_ANGULAR_LOADER)
        .after(After.HIGHLIGHT_ELEMENT)
        .build();

static ActionProfile safeProfileFor(ActionCapability capability) {
    return switch (capability) {
        case CLICKABLE, CHECKABLE -> CLICKABLE_SAFE;
        case TYPEABLE, SEARCH_FIELD, SEARCHABLE -> TYPEABLE_SAFE;
        case SELECTABLE, SEARCHABLE_DROPDOWN, MULTI_SELECTABLE -> SELECTABLE_SAFE;
        default -> DEFAULT_SAFE;
    };
}
```

### `ElementAction.safely()` — Template Method

```java
// Final — subclasses cannot change the lifecycle
public final Action safely() {
    return using(defaultSafeProfile());
}

// Protected — subclasses override only when safe behavior differs
protected ActionProfile defaultSafeProfile() {
    return ActionProfiles.safeProfileFor(capability);
}
```

`DoubleClickAction` overrides `defaultSafeProfile()` without touching any capability interface or framework file. OCP is honored at the action level, not the capability level.

### `ActionCapabilityProvider`

Reduced to a single-method interface. Execution policy is not a capability concern:

```java
public interface ActionCapabilityProvider {
    ActionCapability capability();
}
```

### Capability interfaces

`Clickable`, `Typeable`, `Selectable`, `SearchField`, `SearchableDropdown` lost all `ActionProfile` imports, `*_SAFE_PROFILE` constants, and `safeProfile()` overrides. They are pure structural contracts: locator semantics, action emission, capability metadata.

---

## Affected Files

Modified:
- `src/main/java/core/actions/ActionProfiles.java` — added `CLICKABLE_SAFE`, `TYPEABLE_SAFE`, `SELECTABLE_SAFE` constants and `safeProfileFor()` method; added `import core.interactions.hooks.After`
- `src/main/java/core/actions/ActionCapabilityProvider.java` — removed `safeProfile()` default method; now single-method interface
- `src/main/java/core/actions/ElementAction.java` — constructor 3-arg (removed `safeProfile` parameter and field); `safely()` calls `defaultSafeProfile()`; `defaultSafeProfile()` dispatches via `ActionProfiles.safeProfileFor(capability)`
- `src/main/java/core/actions/ElementActions.java` — removed `safeProfile` capture; 3-arg `ElementAction` construction
- `src/main/java/elements/api/capability/Clickable.java` — removed `ActionProfile`, `Before`, `After` imports; removed `CLICKABLE_SAFE_PROFILE` constant and `safeProfile()` override
- `src/main/java/elements/api/capability/Typeable.java` — same removals as Clickable
- `src/main/java/elements/api/capability/Selectable.java` — same removals as Clickable
- `src/main/java/elements/api/capability/SearchField.java` — removed `ActionProfile`, `Before`, `After` imports; removed `SEARCH_FIELD_SAFE_PROFILE` constant and `safeProfile()` override
- `src/main/java/elements/api/capability/SearchableDropdown.java` — same removals as SearchField

Updated examples:
- `src/test/java/core/actions/ElementActionsSafeProfileTest.java` — full rewrite: examples now verify `ActionProfiles.safeProfileFor()` hook content, `ElementAction.safely()` dispatch, and backward compat with `Profiles.SAFE`
- `src/test/java/core/actions/ElementActionTest.java` — 3-arg constructor throughout; test `defaultSafeProfile_returnsNonNullProfile` replaces field check; split `safely` examples cover normal dispatch and override pattern

---

## Open/Closed at the Correct Level

| Extension point | Before Phase 5 | After Phase 5 |
|-----------------|---------------|---------------|
| New capability with custom safe hooks | Override `safeProfile()` on the capability interface (violates SoC) | Update `ActionProfiles.safeProfileFor()` switch (correct layer) |
| New action type with custom safe hooks | Impossible — profile lived on capability | Override `defaultSafeProfile()` in the action subclass |

---

## Exit Criteria

- [x] No `ActionProfile` or hook imports in any capability interface
- [x] `ActionCapabilityProvider` contains only `capability()`
- [x] `ElementAction.safely()` is final and uses `defaultSafeProfile()` template method
- [x] `ActionProfiles.safeProfileFor()` is the canonical mapping of capability to safe profile
- [x] Behavioral equivalence: `safely()` hook lists identical for all existing capabilities
- [x] All three test classes pass: `ElementActionsSafeProfileTest`, `ElementActionTest`, `ActionCapabilityProviderTest`

---

## Relationship to Ongoing Plan

Phase 5 partially implements work described in the "Architectural Refactoring — Action Ownership with Layering Principle" ongoing plan:
- **Phase 13** (ElementAction base class) — `ElementAction` abstract class was created as part of this correction; Phase 13 plan items are complete
- **Phase 16** (delete execution policy from capabilities) — fully complete; all profile constants and `safeProfile()` overrides removed from capability interfaces

Remaining phases (14, 15, 17-20) are independent and continue from the current state.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
