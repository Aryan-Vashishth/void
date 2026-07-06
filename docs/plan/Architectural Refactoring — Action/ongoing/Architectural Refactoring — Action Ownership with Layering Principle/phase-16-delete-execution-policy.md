# Phase 16 — Delete Execution Policy from Capabilities

**Status:** Partially Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Medium — removes methods from public interface  
**Depends on:** Phase 14, Phase 15

> **Partial progress (Phase 5 SoC correction):** `ActionCapabilityProvider.safeProfile()` was
> removed — the interface now contains only `capability()`. The five capability interfaces that
> Phase 4 had polluted (`Clickable`, `Typeable`, `Selectable`, `SearchField`,
> `SearchableDropdown`) were cleaned of `*_SAFE_PROFILE` constants and `safeProfile()`
> overrides.
>
> **Remaining work:** This phase still depends on Phase 14 (concrete action subclasses) and
> Phase 15 (capability interfaces returning concrete action types). Once those phases
> introduce new patterns, all capability interfaces must be re-audited to confirm no
> execution policy has crept back in, and the exit criteria below must be re-verified
> against the updated codebase.

---

## Objective

Remove all execution policy from capability interfaces and `ActionCapabilityProvider`. Profile methods (safeProfile, reliableProfile, etc.) and profile constant fields are deleted. Keep only `ActionCapabilityProvider.capability()` as pure metadata.

---

## Context

Currently, `ActionCapabilityProvider` contains:

```java
ActionCapability capability();
default ActionProfile safeProfile() { ... }
default ActionProfile reliableProfile() { ... }
default ActionProfile fastProfile() { ... }
default ActionProfile debugProfile() { ... }
```

And capability interfaces contain profile constants:

```java
// Clickable.java
ActionProfile CLICKABLE_SAFE_PROFILE = ActionProfile.builder()...;
ActionProfile CLICKABLE_RELIABLE_PROFILE = ActionProfile.builder()...;
```

These are execution concerns. Per the Layering Principle (ADR-013), they belong to actions, not capabilities.

---

## Target Design

### ActionCapabilityProvider — Metadata Only

```java
public interface ActionCapabilityProvider {
    /**
     * Returns the canonical capability identifier for this element capability.
     * 
     * This is metadata-only, used for logging, tracing, metrics, diagnostics.
     * It is never used for behavioral dispatch.
     */
    ActionCapability capability();
}
```

No profile methods.

### Capability Interfaces — No Profile Constants

**Before (Clickable.java):**
```java
ActionProfile CLICKABLE_SAFE_PROFILE = ...;
ActionProfile CLICKABLE_RELIABLE_PROFILE = ...;
ActionProfile CLICKABLE_FAST_PROFILE = ...;
ActionProfile CLICKABLE_DEBUG_PROFILE = ...;

default ActionCapability capability() { ... }
default ActionProfile safeProfile() { return CLICKABLE_SAFE_PROFILE; }
default ActionProfile reliableProfile() { return CLICKABLE_RELIABLE_PROFILE; }
// ... etc
```

**After (Clickable.java):**
```java
default ActionCapability capability() { return ActionCapability.CLICKABLE; }
```

(That's it.)

---

## Implementation

### Step 1: Delete Profile Methods from ActionCapabilityProvider

**File:** `src/main/java/core/actions/ActionCapabilityProvider.java`

Remove:
- `default ActionProfile safeProfile()`
- `default ActionProfile reliableProfile()`
- `default ActionProfile fastProfile()`
- `default ActionProfile debugProfile()`

Keep only:
- `ActionCapability capability()`

### Step 2: Delete Profile Constants and Methods from Capability Interfaces

For each file:
- `src/main/java/elements/api/capability/Clickable.java`
- `src/main/java/elements/api/capability/Checkable.java`
- `src/main/java/elements/api/capability/Hoverable.java`
- `src/main/java/elements/api/capability/Typeable.java`
- `src/main/java/elements/api/capability/Selectable.java`
- ... (all capability interfaces)

Delete:
- `CLICKABLE_SAFE_PROFILE`
- `CLICKABLE_RELIABLE_PROFILE`
- etc.
- `default ActionProfile safeProfile()`
- `default ActionProfile reliableProfile()`
- etc.

### Step 3: Update Tests

Tests that verify capability-owned profiles are removed:

**Remove from tests:**
```java
// DELETE THIS
@Test
public void clickable_safeProfile_hasCorrectHooks() {
    ActionProfile p = Clickable.CLICKABLE_SAFE_PROFILE;
    assertEquals(...);
}
```

Profile behavior is now tested through action subclasses (Phase 14).

### Step 4: Verify No Callers

Search for calls to deleted methods:

```bash
grep -r "\.safeProfile()" src/main/java/
grep -r "\.reliableProfile()" src/main/java/
grep -r "CLICKABLE_SAFE_PROFILE" src/
```

Should find only test code (which was deleted) and metadata access (action subclasses, which own profiles now).

---

## Affected Files

**Modify:**
- `src/main/java/core/actions/ActionCapabilityProvider.java` — delete profile methods
- `src/main/java/elements/api/capability/Clickable.java` — delete profile fields and methods
- `src/main/java/elements/api/capability/Checkable.java`
- `src/main/java/elements/api/capability/Hoverable.java`
- `src/main/java/elements/api/capability/Typeable.java`
- `src/main/java/elements/api/capability/Selectable.java`
- `src/main/java/elements/api/capability/SearchField.java`
- `src/main/java/elements/api/capability/SearchableDropdown.java`
- `src/main/java/elements/api/capability/Uploadable.java`
- `src/main/java/elements/api/capability/EditableTable.java`
- ... and others

**Remove from tests:**
- All tests that verify capability-owned profiles

---

## Compilation Checkpoint

```bash
mvn -DskipTests compile
```

This may break if anything is calling deleted methods. If so, that's the point—those call sites need to be found and updated.

---

## Exit Criteria

- [ ] ActionCapabilityProvider contains only capability() method
- [ ] All capability interfaces have no profile constants or methods
- [ ] No references to deleted methods remain in code
- [ ] All tests pass
- [ ] Compilation succeeds without errors

---

## Design Principle Reinforcement

This phase enforces the core principle: **Capabilities describe targets. Actions describe execution.**

Compare:
- ✓ `Clickable`: "I am clickable" (target)
- ✓ `ClickAction.defaultSafeProfile()`: "When executed safely, use these hooks" (execution)
- ✗ `Clickable.safeProfile()`: "I know how to execute safely" (mixing layers)

---

## Next Phase

Phase 17 — Refactor Central Dispatch (delete switches from Profiles)

