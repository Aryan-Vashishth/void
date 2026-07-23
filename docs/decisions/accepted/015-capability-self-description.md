# ADR-015 — Capability Self-Description via ActionCapabilityProvider

**Date:** 2026-07-08  
**Status:** Superseded by ADR-016

> **Status: Superseded by ADR-016.** `ActionCapabilityProvider` has been deleted. `capability()` now lives on `elements.api.Element` as a default method. This ADR is preserved for historical context.

---

## Context

Before Phase 3, capability identity was resolved externally. Utilities and infrastructure that
needed to know which capability an element had were forced to branch on `instanceof`:

```java
if (element instanceof Clickable) {
    log("capability: CLICKABLE");
} else if (element instanceof Typeable) {
    log("capability: TYPEABLE");
} else if (element instanceof Hoverable) {
    log("capability: HOVERABLE");
}
// ...continues for every capability
```

This pattern had three failure modes:

1. **Growth problem** — adding a new capability required updating every branching site across
   logging, tracing, diagnostics, and serialization code.
2. **Ordering problem** — `instanceof` chains are order-sensitive. A `Selectable` that also
   `implements Clickable` would be misidentified if `Clickable` appeared first in the chain.
3. **Scattering problem** — capability identity logic lived in the consumer, not in the capability
   itself. There was no single authoritative way to ask "what capability is this?"

Additionally, the `ActionCapability` enum only had 4 values at the time (`CLICKABLE`, `TYPEABLE`,
`SELECTABLE`, `UNKNOWN`), leaving 10 of the 14 capability interfaces unrepresented.

---

## Decision

**Each capability interface self-describes its own identity by implementing `ActionCapabilityProvider`
and returning its canonical `ActionCapability` enum constant.**

Two new types were introduced in `core.actions`:

```java
public interface ActionCapabilityProvider {
    ActionCapability capability();
}

public enum ActionCapability {
    CLICKABLE, TYPEABLE, SELECTABLE, HOVERABLE, CHECKABLE, UPLOADABLE,
    SEARCHABLE, SEARCH_FIELD, SEARCHABLE_DROPDOWN, READ_ONLY,
    TABLE, EDITABLE_TABLE, LISTABLE, MULTI_SELECTABLE,
    UNKNOWN  // fallback for lambda/non-element-bound actions
}
```

All 14 capability interfaces were migrated in three batches:

**Batch 1** — `Clickable`, `Typeable`, `Selectable`  
**Batch 2** — `Hoverable`, `Uploadable`, `Checkable`  
**Batch 3** — `MultiSelectable`, `SearchField`, `Searchable`, `SearchableDropdown`, `ReadOnly`, `Table`, `EditableTable`, `Listable`

Root interfaces (`Clickable`, `Typeable`, `Listable`, `ReadOnly`, `Table`, `Uploadable`,
`MultiSelectable`) declare `extends ActionCapabilityProvider` directly. Derived interfaces
(e.g., `Selectable extends Clickable, Listable`) resolve the diamond by declaring an explicit
`@Override default ActionCapability capability()` that returns their own constant (e.g.,
`SELECTABLE`) rather than inheriting either parent's default.

---

## Why Not a Registry or Reflection

| Approach | Rejected because |
|----------|-----------------|
| Central `instanceof` chain | Grows with every new capability; order-sensitive |
| Static registry (`Map<Class, ActionCapability>`) | Requires startup registration; breaks without it; ordering dependency |
| Reflection (`getAnnotation(...)`) | Runtime cost; fragile; no compile-time safety |
| Self-description via interface | Zero cost; compile-time safe; no registration; polymorphic by design |

Self-description through an interface is the only approach that scales to any number of
capabilities with no changes to existing infrastructure.

---

## Metadata Only — No Behavioral Dispatch

`ActionCapability` is a **metadata identifier**. It is for logging, tracing, diagnostics,
metrics, and serialization only.

Correct usage:

```java
if (element instanceof ActionCapabilityProvider p) {
    logger.debug("Capability: {}", p.capability());
    metrics.increment("action." + p.capability().name().toLowerCase());
}
```

Forbidden — behavioral dispatch through the enum:

```java
// Do NOT do this
switch (p.capability()) {
    case CLICKABLE -> ((Clickable) element).click();   // replaces polymorphism with dispatch
    case TYPEABLE  -> ((Typeable) element).type("");
}
```

This constraint is also enforced by ADR-013 (Architectural Layering Principle, rule #2).
Behavioral execution continues through the capability interface directly — polymorphism, not
enum dispatch.

---

## UNKNOWN

`UNKNOWN` is retained for actions not bound to a capability interface — primarily raw lambda
actions created via `ElementActions.of()` in test infrastructure. It is the `Action.capability()`
default on the base `Action` interface. `UNKNOWN` is not a capability interface; it carries no
`ActionCapabilityProvider` implementation.

---

## Consequences

- All 14 production capability interfaces implement `ActionCapabilityProvider`
- `ActionCapability` expanded from 4 to 15 values (14 real capabilities + `UNKNOWN`)
- No central registry, no reflection, no startup registration required
- Diamond inheritance resolved by explicit `@Override` in derived interfaces
- Adding a new capability interface: implement `ActionCapabilityProvider`, return the new
  enum constant — no changes to existing infrastructure
- `instanceof ActionCapabilityProvider` is the idiomatic check before reading capability metadata

---

## Related

- [ADR-008 — Capability Interfaces](008-capability-interfaces.md) — what capability interfaces are
- [ADR-013 — Architectural Layering Principle](013-architectural-layering-principle.md) — capabilities describe, actions execute; ActionCapability is metadata
- [ADR-014 — Concrete Actions over Anonymous Lambdas](014-concrete-actions-over-lambdas.md) — how capability identity flows into the action pipeline
