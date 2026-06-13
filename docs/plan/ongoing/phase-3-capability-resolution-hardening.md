# Phase 3 — Capability Resolution Hardening

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Medium — touches capability hierarchy

---

## Objective

Prevent the capability resolution code from becoming a 150-line chain of `instanceof` checks as new capabilities are added. Make capabilities self-describing so no central registry or branching is needed.

---

## Context

Currently there are 14 capability interfaces:

```text
Checkable
Clickable
EditableTable
Hoverable
Listable
MultiSelectable
ReadOnly
Searchable
SearchableDropdown
SearchField
Selectable
Table
Typeable
Uploadable
```

Any code that needs to resolve capability type must currently do:

```java
if (element instanceof Clickable)    { ... }
else if (element instanceof Typeable)  { ... }
else if (element instanceof Selectable) { ... }
// ... and growing
```

Every new capability increases this chain. It is not critical today. It becomes unmanageable at 20+ capabilities.

---

## Target Design

### `ActionCapabilityProvider` (new interface)

```java
package core.actions;

/**
 * Capabilities that implement this interface are self-describing.
 * No central registry or instanceof chain needed.
 */
public interface ActionCapabilityProvider {
    ActionCapability capability();
}
```

### `ActionCapability` (enum)

```java
public enum ActionCapability {
    CLICKABLE,
    TYPEABLE,
    SELECTABLE,
    HOVERABLE,
    UPLOADABLE,
    CHECKABLE,
    MULTI_SELECTABLE,
    SEARCHABLE,
    READ_ONLY,
    TABLE,
    EDITABLE_TABLE,
    SEARCH_FIELD
}
```

### Updated Capability (Example — `Clickable`)

```java
public interface Clickable extends Element, ActionCapabilityProvider {

    @Override
    default ActionCapability capability() {
        return ActionCapability.CLICKABLE;
    }

    // existing methods unchanged
}
```

### Resolving Without Branching

```java
// BEFORE (branching grows with each new capability)
if (element instanceof Clickable c) { return c.click(); }
else if (element instanceof Typeable t) { return t.type(""); }

// AFTER (self-describing, zero branching)
if (element instanceof ActionCapabilityProvider p) {
    return switch (p.capability()) {
        case CLICKABLE -> ((Clickable) element).click();
        case TYPEABLE  -> ((Typeable) element).type("");
        // exhaustive — compiler warns when new enum values are added
    };
}
```

---

## Affected Files

New:
- `src/main/java/core/actions/ActionCapabilityProvider.java`
- `src/main/java/core/actions/ActionCapability.java`

Modified:
- `src/main/java/elements/api/capability/Clickable.java`
- `src/main/java/elements/api/capability/Typeable.java`
- `src/main/java/elements/api/capability/Selectable.java`
- `src/main/java/elements/api/capability/Hoverable.java`
- `src/main/java/elements/api/capability/Uploadable.java`
- `src/main/java/elements/api/capability/Checkable.java`
- _(remaining capabilities in follow-up batches)_

---

## Migration Strategy

Migrate in batches. Keep compatibility fallback while migration is incomplete.

**Batch 1 (this phase):** `Clickable`, `Typeable`, `Selectable`  
**Batch 2:** `Hoverable`, `Uploadable`, `Checkable`  
**Batch 3:** remaining capabilities  

---

## Checklist

### Infrastructure
- [ ] Create `ActionCapability` enum with all current capability entries.
- [ ] Create `ActionCapabilityProvider` interface with `capability()` method.

### Batch 1 Migration
- [ ] Add `ActionCapabilityProvider` to `Clickable` — return `ActionCapability.CLICKABLE`.
- [ ] Add `ActionCapabilityProvider` to `Typeable` — return `ActionCapability.TYPEABLE`.
- [ ] Add `ActionCapabilityProvider` to `Selectable` — return `ActionCapability.SELECTABLE`.
- [ ] Ensure backward compatibility — existing code using `instanceof Clickable` still works.

### Batch 2 Migration
- [ ] Add `ActionCapabilityProvider` to `Hoverable`.
- [ ] Add `ActionCapabilityProvider` to `Uploadable`.
- [ ] Add `ActionCapabilityProvider` to `Checkable`.

### Batch 3 Migration
- [ ] Add `ActionCapabilityProvider` to `MultiSelectable`.
- [ ] Add `ActionCapabilityProvider` to `Searchable`.
- [ ] Add `ActionCapabilityProvider` to `SearchField`.
- [ ] Add `ActionCapabilityProvider` to `SearchableDropdown`.
- [ ] Add `ActionCapabilityProvider` to `ReadOnly`.
- [ ] Add `ActionCapabilityProvider` to `Table`.
- [ ] Add `ActionCapabilityProvider` to `EditableTable`.
- [ ] Add `ActionCapabilityProvider` to `Listable`.

### Tests
- [ ] Unit test: each migrated capability returns the correct `ActionCapability` constant.
- [ ] Unit test: resolution via `ActionCapabilityProvider` matches old `instanceof` resolution.
- [ ] Compile-level test: adding a new `ActionCapability` enum value without a switch case produces a compiler warning.

### Cleanup
- [ ] Remove any central `instanceof` chains that have been fully replaced.
- [ ] Update Javadoc on affected capability interfaces.

---

## Exit Criteria

- All 14 capabilities implement `ActionCapabilityProvider`.
- No central `if (element instanceof X)` chains remain for resolved capabilities.
- New capabilities added after this phase register themselves without touching any central code.

---

## What NOT to Do

- Do not remove `instanceof` checks from legacy code in this phase — that is Phase 8.
- Do not change how `ElementActions.of(...)` works — only add provider behavior alongside.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

