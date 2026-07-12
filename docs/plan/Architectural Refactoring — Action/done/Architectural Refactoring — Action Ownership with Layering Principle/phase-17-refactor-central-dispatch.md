# Phase 17 — Eliminate Capability-Based Profile Dispatch

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — deletes code, doesn't add it  
**Depends on:** Phase 14 (concrete action subclasses), Phase 16 (no capability-owned profiles in interfaces)

---

## Objective

Implement Phase 17 by removing all capability-based profile dispatch from the framework and
making profile resolution fully polymorphic.

This is an architectural refactor, **not** a feature addition.

The goal is to strengthen VOID's object-oriented architecture while preserving its long-term
engine-agnostic design.

---

## Background

Previous phases moved profile ownership into concrete actions.

Today, profile resolution still contains remnants of the old architecture:

```java
switch (action.capability()) {
    ...
}
```

This central dispatch in `Profiles.SAFE` and `Profiles.RELIABLE` must be completely removed.

A concrete action should never need another class to inspect its capability in order to determine
its default behavior. Instead:

```text
ClickAction  →  defaultSafeProfile()
TypeAction   →  defaultSafeProfile()
HoverAction  →  defaultSafeProfile()
```

Every action owns its own defaults.

---

## Architectural Principle

> Behavior belongs to the object performing the behavior.

No registry, dispatcher, or profile manager should infer behavior from an `ActionCapability`.

---

## Engine-Agnostic Reminder

While implementing this phase, continuously verify that the architecture remains engine-agnostic.

VOID expresses **automation intent**, never engine implementation.

Correct dependency direction:

```text
Action
    ↓
ActionProfile
    ↓
Execution Pipeline
    ↓
UIEngine
    ↓
Selenium / Playwright / Appium / Future Engines
```

Avoid introducing any reverse dependency such as:

```text
ActionProfile → ActionCapability
ActionProfile → Selenium
ActionProfile → Playwright
```

Profiles describe **what** should happen. Engines decide **how** to satisfy that intent.

Good hook names:

```
Before.WAIT_FOR_ELEMENT
Before.WAIT_FOR_ELEMENT_CLICKABLE
Before.CLEAR_FIELD
```

Bad hook names (engine-specific):

```
Before.SELENIUM_WAIT
Before.PLAYWRIGHT_AUTO_WAIT
```

The execution engine is responsible for interpreting abstract hooks.

---

## Required Changes

### 1. Add reliable profiles to ActionProfiles

Add `reliableProfileFor(ActionCapability)` and the per-capability reliable profile constants to
`ActionProfiles` (package-private, action layer). This mirrors the existing safe profile pattern:

**File:** `src/main/java/core/actions/ActionProfiles.java`

```java
static final ActionProfile DEFAULT_RELIABLE = ActionProfile.builder()
        .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
        .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
        .build();

static final ActionProfile CLICKABLE_RELIABLE = ActionProfile.builder()
        .before(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_CLICKABLE)
        .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
        .build();

// ... TYPEABLE_RELIABLE, SELECTABLE_RELIABLE (same pattern)

static ActionProfile reliableProfileFor(ActionCapability capability) {
    return switch (capability) {
        case CLICKABLE, CHECKABLE                              -> CLICKABLE_RELIABLE;
        case TYPEABLE, SEARCH_FIELD, SEARCHABLE               -> TYPEABLE_RELIABLE;
        case SELECTABLE, SEARCHABLE_DROPDOWN, MULTI_SELECTABLE -> SELECTABLE_RELIABLE;
        default                                               -> DEFAULT_RELIABLE;
    };
}
```

### 2. Route `reliable()` through `defaultReliableProfile()` in ElementAction

`safely()` already goes through `defaultSafeProfile()` → `ActionProfiles.safeProfileFor()`.
Make `reliable()` follow the same pattern.

**File:** `src/main/java/core/actions/ElementAction.java`

```java
// Before
public final Action reliable() {
    return using(Profiles.RELIABLE);  // ← static dispatch, breaks encapsulation
}

protected ActionProfile defaultReliableProfile() {
    return Profiles.RELIABLE;
}

// After
public final Action reliable() {
    return using(defaultReliableProfile());  // ← polymorphic, action owns its profile
}

protected ActionProfile defaultReliableProfile() {
    return ActionProfiles.reliableProfileFor(capability);
}
```

### 3. Remove `Profiles.SAFE` and `Profiles.RELIABLE`

Delete every occurrence of switch-on-capability from Profiles:

**File:** `src/main/java/core/actions/Profiles.java`

- Remove `Profiles.SAFE` (has `before(Action)` and `after(Action)` capability switches)
- Remove `Profiles.RELIABLE` (has `before(Action)` capability switch)
- Keep `RAW`, `DEBUG`, `FAST`, `VISUAL` — these are action-independent; their hooks are the
  same regardless of what action they're applied to
- Update `fromName()`: remove SAFE/RELIABLE entries (unknown names already fall through to RAW)

### 4. Update `Action.safely()` default

The default `safely()` in the `Action` interface still calls `using(Profiles.SAFE)`.
After removing `Profiles.SAFE`, update it to use `ActionProfiles.DEFAULT_SAFE`:

**File:** `src/main/java/core/actions/Action.java`

```java
// Before
default Action safely() {
    return using(Profiles.SAFE);
}

// After
default Action safely() {
    return using(ActionProfiles.DEFAULT_SAFE);
}
```

Note: `ElementAction` overrides `safely()` with the full polymorphic path. This default only
affects plain lambda actions (`engine -> {}`). `ActionProfiles.DEFAULT_SAFE` (wait-for-visible)
is a reasonable minimal guard for those.

### 5. Update tests

**Delete** (testing removed dispatch):
- `ActionProfilesTest.safeProfile_clickable_expandsExpectedHooks`
- `ActionProfilesTest.safeProfile_typeable_expandsExpectedHooks`
- `ActionProfilesTest.safeProfile_selectable_expandsExpectedHooks`
- `ActionProfilesTest.configuredDefaultProfile_isAppliedToNewActions` (already broken by
  Phase 15: concrete action methods bypass `ElementActions.of()` and thus `applyConfiguredDefault`)
- `ElementActionsSafeProfileTest` backward-compatibility block: the four tests comparing
  `Profiles.SAFE.before(action)` with `ActionProfiles.safeProfileFor(action.capability())`

**Add**:
- Reliable profile content tests in `ElementActionsSafeProfileTest`
- Configured-default test using `ElementActions.of()` + `DEBUG` profile
- `fromName("SAFE")` and `fromName("RELIABLE")` both fall back to RAW

**Rename**:
- `ElementActionTest.reliable_usesProfilesReliable` → `reliable_preservesCapability` (the
  assertion was always about capability preservation, not about `Profiles.RELIABLE`)

---

## Keep `ActionProfile` as a Value Object

Profiles are passive configuration objects — they describe execution behavior without containing
dispatch logic. The builder continues to work unchanged:

```java
ActionProfile custom = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();
```

No changes to `ActionProfile.java`.

---

## Verify `ActionCapability` Remaining Uses

After removing central dispatch, audit all remaining uses of `ActionCapability`:

| Location | Use | Verdict |
|----------|-----|---------|
| `ElementAction.capability` field | stores capability for `defaultSafeProfile()` / `defaultReliableProfile()` | Keep — action owns its policy |
| `ActionProfiles.safeProfileFor()` | internal dispatch inside action layer | Keep — this IS the polymorphic resolution |
| `ActionProfiles.reliableProfileFor()` | same | Keep |
| `ElementAction.operationLabel()` | trace/logging label | Keep — metadata, not execution policy |
| `HookChainAction.operationLabel()` | fallback trace label for non-labeled delegates | Keep — metadata only; `ElementAction` subclasses never reach this branch |
| `ActionCapabilityProvider.capability()` | self-description on capability interfaces | Keep — pure metadata |

No capability use constitutes execution dispatch after this phase.

---

## Verification Checklist

- [x] No `switch(action.capability())` remains in `Profiles.java`
- [x] No `Profiles.SAFE.before(action)` calls exist in codebase
- [x] No `Profiles.RELIABLE.before(action)` calls exist in codebase
- [x] No `instanceof` replaces the removed switches
- [x] Every action owns its own default profiles
- [x] Profiles are passive configuration objects — no dispatch logic
- [x] The execution pipeline is unchanged
- [x] Demo projects still compile
- [x] All tests pass
- [x] The architecture remains engine-agnostic

---

## Affected Files

**Modify:**
- `src/main/java/core/actions/ActionProfiles.java` — add reliable constants + `reliableProfileFor()`
- `src/main/java/core/actions/ElementAction.java` — route `reliable()` through `defaultReliableProfile()`
- `src/main/java/core/actions/Profiles.java` — remove SAFE, remove RELIABLE, update `fromName()`
- `src/main/java/core/actions/Action.java` — update `safely()` default

**Delete tests / update tests:**
- `src/test/java/core/actions/ActionProfilesTest.java`
- `src/test/java/core/actions/ElementActionsSafeProfileTest.java`
- `src/test/java/core/actions/ElementActionTest.java`

---

## Compilation Checkpoint

```bash
mvn -DskipTests compile
mvn test
```

---

## Exit Criteria

- [x] `ActionProfiles` contains `reliableProfileFor()` and reliable profile constants
- [x] `ElementAction.reliable()` calls `using(defaultReliableProfile())`
- [x] `Profiles.SAFE` and `Profiles.RELIABLE` are deleted
- [x] `Profiles.fromName("SAFE")` and `fromName("RELIABLE")` return RAW
- [x] No `switch(action.capability())` in `Profiles.java`
- [x] All tests pass

---

## Next Phase

Phase 18 — Audit `ElementRole` for Necessity
