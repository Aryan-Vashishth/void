# Phase 11 — Remove Constructors From Static Elements

**Status:** Complete

**Implementation note:** The partial Phase 11 pass (commit `63a0e59`) removed all `getArgs()` overrides returning `new Object[0]`. By the time Phase 19 Part B shipped (commit `a6a111f`), the only conventionally-pathed page with remaining boilerplate was `DemoLoginPage` — and `Credentials` / `Labels` were already minimal from the same pass. No further production code changes remain within Phase 11's defined scope:

- `DemoLoginPage.Credentials`, `Labels` — minimal; no constructors, no overrides. ✅
- `DemoLoginPage.Button.LOGIN_BUTTON("Login")` — constructor carries the display label "Login" (genuinely custom information). Migrating to `AdvancedLocatorFamily` would change the locator key from Phase 19's `DemoLoginPage.Button.LOGIN_BUTTON.TRIGGER` to the `LocatorFamily` format `DemoLoginPage.Button`, adding boilerplate rather than removing it. Left as-is per "Do not remove overrides that carry genuinely custom information."
- `AccountMappingElements`, `ManageUsersElements` — not migrated to the Phase 5 conventional path (still use explicit flat-key `.properties` files). Phase 11 explicitly excludes these: `getExternalFileName()` overrides must stay until the pages are conventionally migrated.  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — mechanical cleanup enabled by Phases 1–4; no behavior change

---

## Objective

Simplify all static element enums by removing constructors, stored fields, and redundant method overrides that are now covered by the defaults introduced in Phases 1–4.

---

## Dependencies

All of the following must be complete before this phase begins:

- Phase 1 (automatic locator keys)
- Phase 2 (default empty arguments)
- Phase 3 (NO_ARGS rename)
- Phase 4 (automatic display text)
- Phase 16 (LocatorFamily) — establishes which enums are family-grouped, and thus which remaining constructors belong there, not here
- Phase 17 (AdvancedLocatorFamily) — the only enum type where constructors are intentionally preserved

---

## Context

Before:

```java
enum Credentials implements Typeable {

    USERNAME_INPUT("USERNAME_INPUT"),
    PASSWORD_INPUT("PASSWORD_INPUT");

    private final String key;
    Credentials(String k) { this.key = k; }

    @Override public String getInputLocator()     { return key; }
    @Override public String getExternalFileName() { return LOCATOR_FILE; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}
```

After:

```java
enum Credentials implements Typeable {
    USERNAME_INPUT,
    PASSWORD_INPUT
}
```

Every removed override was mechanically derivable. No behavior changes.

---

## Scope

Remove the following patterns wherever they appear and the defaults now cover them:

- Constructor that stores only the locator key string → remove
- `getInputLocator()` / `getTriggerLocator()` / equivalent overrides that return the stored key → remove (default `getPrimaryLocator()` covers this)
- `getExternalFileName()` overrides that return `LOCATOR_FILE` → remove if the page is migrated to the convention in Phase 5; leave if not yet migrated
- `getArgs()` overrides that return `new Object[0]` → remove (default covers this)
- `getDisplayText()` overrides derivable from the constant name → remove (default covers this)

**Constructor removal rule (revised for Locator Families):**

- `AdvancedLocatorFamily` enums — constructors that carry explicit semantic values are **preserved**; they are the mechanism of that interface.
- All other enum types — constructors must be removed. If a constructor was providing a custom label or arg, the element should instead be migrated to `AdvancedLocatorFamily` (Phase 17) or `SwitchLocatorFamily` (Phase 18), not kept as a raw constructor in a static element.

---

## Affected Files

- `src/main/java/examples/demo/pages/DemoLoginPage.java` (and any other page definitions in the project)
- Any other enum-based element definitions across the codebase

---

## Checklist

### Analysis
- [ ] List all element enums in the codebase
- [ ] For each, identify which overrides are now redundant

### Implementation
- [ ] Remove redundant constructors, fields, and overrides from each enum
- [ ] Confirm each simplified enum still compiles and resolves correctly
- [ ] Preserve any override that is not derivable (custom display text, dynamic args, custom file path)

### Tests
- [ ] Regression: all simplified elements resolve to the same locators as before
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- All static element enums use the minimal declaration form
- No redundant constructors, fields, or overrides remain in static elements
- All examples pass

---

## What NOT to Do

- Do not remove overrides that carry genuinely custom information
- Do not remove `getExternalFileName()` overrides from elements on pages not yet migrated to the convention
- Do not change dynamic elements that use `.with(...)` or non-empty args
- Do not preserve constructors in static element enums to carry custom labels or semantic values — migrate those enums to `AdvancedLocatorFamily` or `SwitchLocatorFamily` instead
- Do not treat `AdvancedLocatorFamily` constructors as redundant — they are the intended mechanism for that interface and must not be removed here

---

*MIT License Copyright (c) 2025-2026 VOID Project*
