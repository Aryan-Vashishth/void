# ADR-016 -- capability() Ownership Migration

**Date:** 2026-07-17
**Status:** Accepted

---

## Context

ADR-015 introduced `ActionCapabilityProvider` as the mechanism for capability interfaces to
self-describe their identity. Each capability interface extends `ActionCapabilityProvider`
and returns its canonical `ActionCapability` constant from a default `capability()` method.

This solved the instanceof-branching problem for consumers that needed to log or trace
capability identity. However, the design introduced a coupling that creates a layering
violation:

1. `ActionCapabilityProvider` lives in `core.actions` -- an action-layer package.
2. `Element` (in `elements.api`) and all capability interfaces (in `elements.api.capability`)
   must extend an interface from the action layer just to self-describe.
3. This means the element layer depends on the action layer. The correct dependency
   direction is element layer independent of action layer -- elements describe structure,
   actions consume elements.

Additionally, `ElementActions.capabilityFor(element)` contains an
`instanceof ActionCapabilityProvider` check to resolve the capability. After Phase 2 of the
OOP violations remediation, this check is itself an instanceof dispatch pattern that the
remediation is eliminating.

The root cause is that capability identity belongs to the element contract, not to the
action package. An element's capability is structural metadata -- it does not change based
on what action is being built. Placing its declaration in the action layer is an ownership
error.

---

## Decision

**Move `capability()` to `Element` as a `default` method. Delete `ActionCapabilityProvider`.**

```java
// Element.java (post-migration)
public interface Element {
    // ... existing contract ...

    default ActionCapability capability() {
        return ActionCapability.UNKNOWN;
    }
}
```

Each capability interface overrides `capability()` directly without extending
`ActionCapabilityProvider`:

```java
// Before (via ActionCapabilityProvider)
public interface Clickable extends Element, ActionCapabilityProvider {
    @Override
    default ActionCapability capability() { return ActionCapability.CLICKABLE; }
}

// After (directly on Element)
public interface Clickable extends Element {
    @Override
    default ActionCapability capability() { return ActionCapability.CLICKABLE; }
}
```

`ActionCapabilityProvider` is deleted from `core.actions`. No class outside
`elements.api.capability` should need to extend it after this migration.

---

## Reasoning

### Dependency direction

The element layer describes UI structure. The action layer produces execution intent over
that structure. Elements do not depend on actions -- elements are consumed by actions.
`ActionCapabilityProvider` residing in `core.actions` while being extended by every
capability interface is a layer inversion. Moving `capability()` to `Element` corrects
the direction: the element contract owns its own metadata.

### Reduction of instanceof surface

`ElementActions.capabilityFor(element)` used `instanceof ActionCapabilityProvider` as a
guard before calling `capability()`. With `capability()` as a default on `Element`, every
`Element` implementation has the method. The guard becomes unnecessary and is removed.
This eliminates one instanceof dispatch site, consistent with the goals of the OOP
violations remediation.

### Unchanged consumer behavior

Consumers that call `element.capability()` see no difference. The return type, return
values, and fallback default (`UNKNOWN`) are identical. The migration is a structural
reclassification with no behavioral change.

---

## Diamond inheritance resolution

Derived capability interfaces that extend multiple parents with `capability()` defaults
must still resolve the diamond explicitly. The existing override pattern is unchanged:

```java
// Selectable extends both Clickable and Listable, each with capability()
public interface Selectable extends Clickable, Listable {
    @Override
    default ActionCapability capability() { return ActionCapability.SELECTABLE; }
}
```

All derived interfaces that previously resolved this diamond via `ActionCapabilityProvider`
must retain their explicit `@Override`. The list of interfaces requiring explicit overrides
is identical before and after the migration.

---

## Relationship to ADR-015

ADR-015 established the self-description pattern and introduced `ActionCapabilityProvider`.
This decision retains the self-description principle and the `ActionCapability` enum, but
relocates the declaration point. ADR-015 documents the motivation for capability
self-description (scaling, ordering safety, elimination of instanceof branching). That
motivation is still valid and is unchanged by this decision.

ADR-015 is not superseded -- the self-description contract it established is preserved.
The only change is that the mechanism (`ActionCapabilityProvider`) is replaced by a simpler
one (`capability()` default on `Element`).

---

## Consequences

- `ActionCapabilityProvider.java` is deleted
- `Element.java` gains `default ActionCapability capability()` returning `ActionCapability.UNKNOWN`
- All 14 capability interfaces remove `extends ActionCapabilityProvider` and retain their
  `@Override default ActionCapability capability()` returning the correct constant
- `ElementActions.capabilityFor(element)` simplifies to `element.capability()`
- The `instanceof ActionCapabilityProvider` guard in `ElementActions` is removed
- Element layer packages have no compile-time dependency on `core.actions`
- Adding a new capability interface: extend `Element`, override `capability()` to return
  the new `ActionCapability` constant -- no action-layer changes required
- Grep surface for `ActionCapabilityProvider` returns zero across all source

---

## Related

- [ADR-008 -- Capability Interfaces](008-capability-interfaces.md) -- capability interface contract
- [ADR-013 -- Architectural Layering Principle](013-architectural-layering-principle.md) -- capabilities describe, actions execute
- [ADR-015 -- Capability Self-Description via ActionCapabilityProvider](015-capability-self-description.md) -- the prior mechanism
- [OOP Violations Remediation Phase 2](../../plan/draft/oop-violations-remediation/phase-2-element-interface.md)
