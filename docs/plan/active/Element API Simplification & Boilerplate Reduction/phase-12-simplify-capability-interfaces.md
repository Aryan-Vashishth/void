# Phase 12 — Simplify Capability Interfaces

**Status:** Complete — analysis found no forwarding no-ops; all overrides are required or meaningful  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — changes to shared interfaces; must not alter any externally visible behavior

---

## Objective

Remove forwarding implementations from capability interfaces that do nothing beyond delegating to a parent interface, and relocate common default behavior into `Element` where it belongs.

## Analysis Result

Full scan of all 14 capability interfaces found **zero forwarding no-ops** — every default override either:
- Maps to a capability-specific concrete getter (`getPrimaryLocator()` → `getTriggerLocator()`, etc.)
- Provides capability-specific logic (display text, role map building)
- Returns a capability-specific `ActionCapability` constant
- Performs diamond disambiguation required by the compiler (`SearchField`, `SearchableDropdown`, `Selectable`)

Diamond disambiguations that must stay: `SearchField.getPrimaryLocator/getDisplayText`, `SearchableDropdown.getPrimaryLocator/getSecondaryLocator/getDisplayText/getIndex`, `Selectable.getDisplayText`.

---

## Context

Several capability interfaces contain overrides whose only purpose is to call a parent interface method:

```java
@Override
default String getPrimaryLocator() {
    return Typeable.super.getPrimaryLocator();
}
```

These add maintenance cost without adding capability-specific behavior. They make interfaces harder to read. Their only historical purpose was satisfying the compiler when multiple super-interfaces provided conflicting defaults — in those cases, an explicit override is required and must be kept.

---

## Rules

**Remove** an override when:
- It only calls `SuperInterface.super.method()` with no modification
- The parent interface's default would be applied correctly without the override (no diamond ambiguity)

**Keep** an override when:
- Two or more super-interfaces provide conflicting defaults — the compiler requires an explicit resolution
- The override adds capability-specific logic

---

## Affected Interfaces

Identify all capability interfaces and check each for forwarding-only overrides. Common candidates:
- `Typeable`
- `Clickable`
- `Selectable`
- `Hoverable`
- Sub-interfaces with complex inheritance chains

For diamond-inheritance situations (e.g. `Selectable extends Clickable, Listable`), the explicit override must remain — document why it is required.

---

## Checklist

### Analysis
- [ ] List all capability interfaces and their inheritance relationships
- [ ] For each interface, identify forwarding-only overrides (no-op delegates)
- [ ] For each candidate removal, confirm no diamond ambiguity requires it

### Implementation
- [ ] Remove all no-op forwarding overrides identified above
- [ ] Confirm all capability interfaces still compile correctly
- [ ] Add a comment on any override kept for diamond-disambiguation explaining why

### Tests
- [ ] Regression: all capability behaviors are unchanged after removal of forwarding overrides
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- No capability interface contains a forwarding-only override that the compiler does not require
- All required diamond-disambiguation overrides are present and documented
- All tests pass

---

## What NOT to Do

- Do not remove an override required for diamond-inheritance disambiguation — the code will not compile
- Do not move capability-specific behavior into `Element` — it belongs in the capability interface
- Do not change any externally visible method signatures

---

*MIT License Copyright (c) 2025-2026 VOID Project*
