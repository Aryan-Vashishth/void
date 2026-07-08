# Phase 3 — Capability Self-Description

**Status:** Done  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low–Medium — touches capability hierarchy

---

# Objective

Make capabilities self-describing without introducing centralized capability resolution.

Capabilities should own their identity and metadata so that logging, diagnostics, tracing, serialization, and tooling never require growing `instanceof` chains or registries.

This phase **does not** change how actions are executed.

---

# Background

VOID currently exposes the following capability interfaces:

- Checkable
- Clickable
- EditableTable
- Hoverable
- Listable
- MultiSelectable
- ReadOnly
- Searchable
- SearchableDropdown
- SearchField
- Selectable
- Table
- Typeable
- Uploadable

Many internal utilities identify capabilities through branching:

```java
if (element instanceof Clickable) {
    ...
} else if (element instanceof Typeable) {
    ...
} else if (element instanceof Hoverable) {
    ...
}
// ...
```

As additional capabilities are introduced, these branches become increasingly difficult to maintain for diagnostics and metadata extraction.

Execution, however, is already polymorphic and should remain so.

---

# Design Goals

- Capabilities identify themselves.
- No central registry.
- No reflection.
- No startup registration.
- Preserve existing APIs.
- Preserve polymorphic execution.
- Future capabilities register themselves by implementing a single interface.

---

# New Interface

```java
package core.actions;

/**
 * Marker for capabilities that can describe themselves.
 */
public interface ActionCapabilityProvider {

    ActionCapability capability();

}
```

---

# ActionCapability

```java
package core.actions;

/**
 * Canonical identifiers for all supported capabilities.
 */
public enum ActionCapability {

    CLICKABLE,
    TYPEABLE,
    SELECTABLE,
    HOVERABLE,
    CHECKABLE,
    UPLOADABLE,
    SEARCHABLE,
    SEARCH_FIELD,
    SEARCHABLE_DROPDOWN,
    READ_ONLY,
    TABLE,
    EDITABLE_TABLE,
    LISTABLE,
    MULTI_SELECTABLE

}
```

---

# Example Migration

```java
public interface Clickable
        extends Element, ActionCapabilityProvider {

    @Override
    default ActionCapability capability() {
        return ActionCapability.CLICKABLE;
    }

    Action click();

}
```

No implementation classes require modification.

---

# Intended Uses

Capability metadata may now be consumed directly.

Logging:

```java
if (element instanceof ActionCapabilityProvider provider) {
    logger.debug("Capability: {}", provider.capability());
}
```

Tracing:

```java
trace.record(provider.capability());
```

Metrics:

```java
metrics.increment(provider.capability());
```

Serialization:

```java
dto.setCapability(provider.capability());
```

No registry is required.

---

# What This Phase Does NOT Do

This phase does **not** introduce capability-based dispatch.

Avoid patterns such as:

```java
switch (provider.capability()) {

    case CLICKABLE ->
        ((Clickable) element).click();

    case TYPEABLE ->
        ((Typeable) element).type(...);

    case SELECTABLE ->
        ((Selectable) element).select(...);

}
```

Although cleaner than large `instanceof` chains, this simply replaces one centralized dispatch mechanism with another.

Behavior should continue to be expressed through polymorphism.

Preferred:

```java
Clickable clickable = ...

clickable.click();
```

Not:

```java
dispatch(provider.capability());
```

---

# Migration Strategy

## Batch 1

Migrate:

- Clickable
- Typeable
- Selectable

Each interface:

- extends `ActionCapabilityProvider`
- returns its corresponding `ActionCapability`

---

## Batch 2

Migrate:

- Hoverable
- Uploadable
- Checkable

---

## Batch 3

Migrate:

- MultiSelectable
- Searchable
- SearchField
- SearchableDropdown
- ReadOnly
- Table
- EditableTable
- Listable

---

# Affected Files

## New

```
src/main/java/core/actions/ActionCapability.java
src/main/java/core/actions/ActionCapabilityProvider.java
```

## Modified

```
elements/api/capability/Clickable.java
elements/api/capability/Typeable.java
elements/api/capability/Selectable.java
elements/api/capability/Hoverable.java
elements/api/capability/Uploadable.java
elements/api/capability/Checkable.java
elements/api/capability/MultiSelectable.java
elements/api/capability/Searchable.java
elements/api/capability/SearchField.java
elements/api/capability/SearchableDropdown.java
elements/api/capability/ReadOnly.java
elements/api/capability/Table.java
elements/api/capability/EditableTable.java
elements/api/capability/Listable.java
```

---

# Tests

## Unit

Each migrated capability returns the expected enum value.

Example:

```java
assertEquals(
    ActionCapability.CLICKABLE,
    clickable.capability()
);
```

---

## Logging

Verify capability metadata appears correctly in logs and traces.

---

## Backward Compatibility

Existing code using:

```java
instanceof Clickable
```

continues to work unchanged.

No public API behavior changes.

---

# Cleanup

- Update Javadocs for migrated capability interfaces.
- Replace metadata-related `instanceof` chains where appropriate.
- Do **not** remove behavioral `instanceof` checks in this phase.

---

# Exit Criteria

- All capability interfaces implement `ActionCapabilityProvider`.
- No capability requires external registration.
- Capability metadata is available without central registries.
- Existing execution paths remain unchanged.
- No behavior is dispatched through `ActionCapability`.

---

# Out of Scope

- Removing legacy behavioral `instanceof` checks (Phase 8).
- Refactoring `ElementActions.of(...)`.
- Introducing a capability registry.
- Reflection-based capability discovery.
- Replacing polymorphic execution with enum dispatch.

---

**MIT License**  
Copyright (c) 2025–2026 VOID Project
