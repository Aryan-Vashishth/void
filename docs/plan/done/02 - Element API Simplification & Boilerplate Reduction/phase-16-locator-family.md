# Phase 16 — LocatorFamily

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — new interface + key naming convention different from static elements; must integrate with resolution order

**Implementation note:** Java only allows a default method to satisfy an abstract method from another interface when both are in the same hierarchy. Because `LocatorFamily extends Element` but does NOT extend capability interfaces (`Clickable`, `Typeable`, etc.), the capability-specific abstract methods (`getTriggerLocator()`, `getInputLocator()`, etc.) must be provided by the implementing enum as one-liners returning `getPrimaryLocator()`. Phase 12 will eliminate this requirement.

---

## Objective

Introduce `LocatorFamily` — a marker interface for enums whose constants share a single locator template and differ only by their runtime argument, which VOID derives automatically from the constant name.

---

## Dependencies

- Phase 4 (automatic display text) — the same word-transform algorithm is used for runtime argument derivation
- Phase 5 (deterministic repository convention) — the family key format builds on the same convention
- Phase 9 (locator resolution order) — the resolver must recognise family elements and apply family-specific lookup

---

## Context

Some pages have groups of elements that share one locator pattern:

```java
public interface AdminHome {

    enum Tiles implements Clickable, LocatorFamily {
        AUDIT_INFO,
        MANAGE_USERS,
        MANAGE_DOCS,
        MANAGE_VENDORS
    }
}
```

Instead of four separate keys each with the same template value, one family key captures the shared template:

```properties
AdminHome.Tiles=//button[preceding-sibling::p[normalize-space(.)='%s']]
```

VOID resolves the runtime argument automatically:

```
MANAGE_USERS  →  "Manage Users"  →  Object[]{"Manage Users"}
```

---

## Key Format

Family key = `PageName.EnumName` (no constant suffix).

This distinguishes family keys from static element keys (`PageName.EnumName.CONSTANT`) at a glance.

---

## Automatic Argument Derivation

Uses the same algorithm as `getDisplayText()` (Part 4):

1. Split constant name on underscores.
2. Capitalise first character of each token, lowercase the rest.
3. Join with a space.

Applies only when the locator template has exactly one `%s` token.  
Multi-argument templates continue to use explicit `.with(...)` calls.

---

## Affected Files

- `src/main/java/elements/api/LocatorFamily.java` (new interface)
- `src/main/java/elements/locator/LocatorResolver.java` — recognise family elements; look up `PageName.EnumName` key; inject derived arg
- Properties template generator (Phase 6) — emit `PageName.EnumName=` for family enums

---

## Checklist

### Interface
- [ ] Create `LocatorFamily` as a marker interface extending `Element`
- [ ] Document: implementing this declares that all constants share a single template

### Key Derivation
- [ ] Implement `PageName.EnumName` key derivation for family elements (separate from static element `PageName.EnumName.CONSTANT` derivation)
- [ ] Confirm family key and static key are unambiguous at lookup time

### Runtime Argument Derivation
- [ ] Implement word-transform argument derivation (reuse Phase 4 logic)
- [ ] Confirm single-`%s` template receives derived arg correctly
- [ ] Confirm multi-`%s` templates fall through to explicit args

### Tests
- [ ] Unit test: family constant produces `PageName.EnumName` key
- [ ] Unit test: `MANAGE_USERS` → arg `"Manage Users"` → XPath resolves correctly
- [ ] Unit test: multi-arg template requires explicit `.with(...)` call
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `LocatorFamily` interface exists and is documented
- Family constants look up `PageName.EnumName` key (not per-constant key)
- Runtime argument is derived automatically from the constant name
- All examples pass

---

## What NOT to Do

- Do not generate per-constant keys for family enums — one key per enum group is the entire point
- Do not attempt automatic arg derivation for multi-`%s` templates
- Do not implement `AdvancedLocatorFamily` or `SwitchLocatorFamily` here — those are Phases 17 and 18

---

*MIT License Copyright (c) 2025-2026 VOID Project*
