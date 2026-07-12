# Phase 17 — AdvancedLocatorFamily

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — additive extension of Phase 16; no existing behavior changed

---

## Objective

Introduce `AdvancedLocatorFamily` for enums that share a locator template but have some constants whose semantic values cannot be automatically derived — acronyms, symbols, punctuation, or domain-specific labels.

---

## Dependencies

- Phase 16 (`LocatorFamily`) must be implemented first

---

## Context

Most constants in a family group have predictable display-style labels. A few do not:

```java
public interface VendorPage {

    enum Filters implements Clickable, AdvancedLocatorFamily {

        COUNTRY,                                  // auto: "Country"

        HQ_STATE_PROVINCE("HQ State/Province"),   // explicit: slash and mixed casing

        SAVE_AND_CONTINUE("Save & Continue"),     // explicit: ampersand

        CRM("CRM");                               // explicit: all-caps acronym
    }
}
```

Generated properties remain:

```properties
VendorPage.Filters=
```

Only exceptional values are explicitly authored. Everything else follows standard `LocatorFamily` automatic derivation.

---

## Behavior

| Constant | Constructor value | Resolved arg |
|---|---|---|
| `COUNTRY` | none | `"Country"` (auto) |
| `HQ_STATE_PROVINCE` | `"HQ State/Province"` | `"HQ State/Province"` (explicit) |
| `SAVE_AND_CONTINUE` | `"Save & Continue"` | `"Save & Continue"` (explicit) |
| `CRM` | `"CRM"` | `"CRM"` (explicit) |

---

## Affected Files

- `src/main/java/elements/api/AdvancedLocatorFamily.java` (new interface extending `LocatorFamily`)
- `src/main/java/elements/locator/LocatorResolver.java` — when resolving an `AdvancedLocatorFamily` element, use the constructor value if present, otherwise fall back to word-transform

---

## Checklist

### Interface
- [ ] Create `AdvancedLocatorFamily` extending `LocatorFamily`
- [ ] Define how a constant signals its explicit value — constructor, method override, or annotation
- [ ] Document the fallback rule: word-transform applies when no explicit value is present

### Resolver
- [ ] Detect `AdvancedLocatorFamily` at resolution time
- [ ] Read explicit value when present; use word-transform when absent
- [ ] Confirm the key lookup remains `PageName.EnumName` (unchanged from Phase 16)

### Tests
- [ ] Unit test: constant with no constructor value → word-transform arg
- [ ] Unit test: constant with constructor value → explicit arg used
- [ ] Unit test: mixed enum — some auto, some explicit — all resolve correctly
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `AdvancedLocatorFamily` interface exists and is documented
- Constants with explicit values use them; constants without fall back to auto-derivation
- Properties key format is unchanged from Phase 16 (`PageName.EnumName`)
- All tests pass

---

## What NOT to Do

- Do not require every constant to have an explicit value — automatic derivation for the common case is the point
- Do not change the key format — the same `PageName.EnumName=` key applies to all family interfaces
- Do not implement `SwitchLocatorFamily` here — that is Phase 18

---

*MIT License Copyright (c) 2025-2026 VOID Project*
