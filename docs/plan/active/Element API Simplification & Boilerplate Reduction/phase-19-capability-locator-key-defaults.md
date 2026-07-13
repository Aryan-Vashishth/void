# Phase 19 — Capability Locator Key Defaults

**Status:** Partial — single-role `name()` defaults shipped; fully-qualified key format with `ElementRole` suffix pending Phase 6  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — Part B changes the properties key format; requires resolver update and migration of existing `.properties` files

---

## Objective

Remove the last category of repetitive locator boilerplate from page enum declarations. Every capability interface's locator method will default to a fully-qualified, self-describing key that encodes the page class, the enum group, the constant name, and the locator role — matching the same information the JSON file already expresses via nesting.

---

## Problem

Every enum currently must explicitly override its capability's locator method, even when the intent is trivially "use the constant name":

```java
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
    @Override public String getInputLocator() { return name(); }   // ← boilerplate
}
```

---

## Role Suffix Source — `ElementRole.name()` (NOT method-name trimming)

The role token appended to the locator key comes from `ElementRole.name()` directly — the same source the JSON already uses when emitting multi-role entries via `EnumLocatorScanner.writeInto()`:

```java
// EnumLocatorScanner — already in production
rolesNode.put(entry.getKey().name(), resolvedVal);
//             ^^^^^^^^^^^^^^^^^^^^
//             ElementRole.TRIGGER.name() → "TRIGGER"
//             ElementRole.INPUT.name()   → "INPUT"
//             ElementRole.LIST.name()    → "LIST"
```

The Phase 19 plan initially described a method-name-trimming approach (`getTriggerLocator` → strip `get`/`Locator` → lowercase `trigger`). **That does not exist in the codebase and will not be implemented.** Using `ElementRole.name()` is:

- Already consistent with JSON emission (no new logic needed in `EnumLocatorScanner`)
- Free of any trimming / casing transforms
- Auditable: the `ElementRole` enum is the single source of truth for role names

| Capability method | `ElementRole` constant | Key suffix |
|-------------------|------------------------|------------|
| `getTriggerLocator()` | `ElementRole.TRIGGER` | `TRIGGER` |
| `getInputLocator()` | `ElementRole.INPUT` | `INPUT` |
| `getTextLocator()` | `ElementRole.TEXT` | `TEXT` |
| `getListLocator()` | `ElementRole.LIST` | `LIST` |
| `getToolTipContentLocator()` | `ElementRole.TOOLTIP_CONTENT` | `TOOLTIP_CONTENT` |
| `getSearchInputLocator()` | `ElementRole.SEARCH_INPUT` | `SEARCH_INPUT` |
| `getSearchButtonLocator()` | `ElementRole.SEARCH_BUTTON` | `SEARCH_BUTTON` |
| `getSearchResultLocator()` | `ElementRole.SEARCH_RESULT` | `SEARCH_RESULT` |
| `getTableLocator()` | `ElementRole.TABLE` | `TABLE` |

---

## Key Format Convention

The fully-qualified locator key for any element is:

```
PageClass.EnumClass.CONSTANT_NAME.ROLE
```

Where `ROLE` is the `ElementRole` constant name (uppercase, matching `ElementRole.name()`).

**Examples from `DemoLoginPage`:**

```properties
# tests/demo/pages/DemoLoginPage/locators.properties
DemoLoginPage.Credentials.USERNAME_INPUT.INPUT=//input[@id='username']
DemoLoginPage.Credentials.PASSWORD_INPUT.INPUT=//input[@id='password']
DemoLoginPage.Button.LOGIN_BUTTON.TRIGGER=//button[@type='submit']
DemoLoginPage.Labels.SUCCESS_MESSAGE.TEXT=//div[@id='flash']
```

Multi-role element:
```properties
DemoLoginPage.NavBar.PARTNER.TRIGGER=//button[@data-partner]
DemoLoginPage.NavBar.PARTNER.LIST=//ul[@class='partner-options']
```

### JSON stays nested, properties stays flat — same `ElementRole` token in both

```
JSON nesting:           DemoLoginPage → Credentials → USERNAME_INPUT → INPUT  (→ XPath)
Properties flat key:    DemoLoginPage.Credentials.USERNAME_INPUT.INPUT         (→ XPath)
```

The JSON already uses `ElementRole.name()` as the sub-key for multi-role elements. After Part B, single-role elements in JSON would also carry the role sub-key for symmetry:

```json
{
  "Credentials": {
    "USERNAME_INPUT": { "INPUT": "//input[@id='username']" }
  }
}
```

`EnumLocatorScanner.writeInto()` already has the `roles.size() > 1` branch that emits a nested role object. After Part B it emits the same structure for single-role elements (the branch condition changes from `> 1` to `>= 1`).

---

## Consistency Check — Current Code

| Location | What it uses | Status |
|----------|-------------|--------|
| `EnumLocatorScanner.writeInto()` — JSON emission | `ElementRole.name()` | ✅ already correct |
| `EnumLocatorScanner.resolve()` — properties lookup | locator method return value | ✅ will use full qualified key after Part B |
| `Clickable.getAllLocatorRoles()` | `ElementRole.TRIGGER` | ✅ correct |
| `Typeable.getAllLocatorRoles()` | `ElementRole.INPUT` | ✅ correct |
| `ReadOnly.getAllLocatorRoles()` | `ElementRole.TEXT` | ✅ correct |
| Phase 19 plan (original) | method-name trimming (lowercase) | ❌ **corrected by this revision** |

No code changes are needed for the consistency fix — the code was always right. Only the plan doc had the wrong description.

---

## What Shipped (Phase 19 Part A)

Single-role locator methods default to `((Enum<?>) this).name()`:

| Interface | Method | Default (shipped) |
|-----------|--------|-------------------|
| `Clickable` | `getTriggerLocator()` | `name()` |
| `Typeable` | `getInputLocator()` | `name()` |
| `ReadOnly` | `getTextLocator()` | `name()` |
| `Listable` | `getListLocator()` | `name()` |
| `Uploadable` | `getInputLocator()` | `name()` |

This eliminates the `return name()` boilerplate from all page enums using these interfaces. Properties and JSON lookups continue to use `CONSTANT_NAME` as the key — no format migration yet.

---

## Part B — Fully-Qualified Key with `ElementRole` Suffix (pending)

After Part B, locator methods default to the fully-qualified key with the `ElementRole` suffix appended. The default implementation derives it from the enum's class hierarchy:

```java
// example: Typeable (after Part B)
default String getInputLocator() {
    Enum<?> e = (Enum<?>) this;
    Class<?> enumClass = e.getDeclaringClass();
    Class<?> pageClass = enumClass.getEnclosingClass();
    String prefix = (pageClass != null)
        ? pageClass.getSimpleName() + "." + enumClass.getSimpleName()
        : enumClass.getSimpleName();
    return prefix + "." + e.name() + "." + ElementRole.INPUT.name();
    //                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //                                     "INPUT" — from ElementRole directly
}
```

**Multi-role interfaces** (e.g., `Selectable`) default to distinct keys by role — no diamond disambiguation problem:

```
DemoLoginPage.NavBar.PARTNER.TRIGGER  ← getTriggerLocator()
DemoLoginPage.NavBar.PARTNER.LIST     ← getListLocator()
```

### Required co-ordinated changes for Part B

These must land together:

1. **Capability interfaces** — update defaults to return the full qualified key.
2. **`EnumLocatorScanner.writeInto()`** — change the `roles.size() > 1` branch to `roles.size() >= 1` so single-role elements also emit a nested role object in JSON.
3. **All existing `.properties` files** — migrate keys from `CONSTANT_NAME=` to `PageClass.EnumClass.CONSTANT_NAME.ROLE=`.
4. **All existing JSON locator files** — migrate single-role entries from `"CONSTANT_NAME": "//xpath"` to `"CONSTANT_NAME": { "ROLE": "//xpath" }`.
5. **Phase 6 template generator** — must emit fully-qualified keys from the start (no legacy format).

### Role mismatch guard

Once Part B is live, the locator method return and the JSON sub-key derive from the same `ElementRole` constant — so structural mismatches (e.g., method returns `INPUT` but JSON has `TRIGGER`) are caught at migration time when `EnumLocatorScanner` fails to find the key in properties. No runtime-only mismatch is possible if the migration tooling and the defaults share the same `ElementRole` source.

---

## Implementation Order (Part B)

1. Audit all existing `.properties` and `.json` locator files to inventory the migration scope.
2. Migrate `.properties` keys to the fully-qualified format.
3. Migrate `.json` files to use nested role objects for single-role entries.
4. Update capability interface defaults.
5. Update `EnumLocatorScanner.writeInto()` branch condition.
6. Update Phase 6 template generator spec.
7. Run full suite.

---

## Affected Files (Part B)

**Capability interfaces — update defaults:**
- `Clickable`, `Typeable`, `ReadOnly`, `Listable`, `Uploadable` (update from `name()` to qualified key)
- `Selectable`, `MultiSelectable`, `SearchField`, `Searchable`, `SearchableDropdown`, `Table`, `Hoverable` (add defaults)

**Scanner:**
- `core/resolvers/locator/json/EnumLocatorScanner.java` — branch condition change

**Resources — key migration:**
- All `.properties` files under `src/main/resources/locators/properties/`
- All `.json` files under `src/main/resources/locators/json/`
- Phase 6 template generator spec

---

## Exit Criteria

- All locator methods on all capability interfaces have defaults; no page enum needs a `return name()` override
- Properties files use `PageClass.EnumClass.CONSTANT.ROLE=` (uppercase ROLE from `ElementRole.name()`)
- JSON files use `{ "CONSTANT": { "ROLE": "//xpath" } }` for both single-role and multi-role elements
- `EnumLocatorScanner` emits the nested role object for all elements
- `EnumLocatorScanner` resolves properties via the full qualified key
- Phase 6 template generator spec records this format
- Full suite passes with no regressions

---

*MIT License Copyright (c) 2025-2026 VOID Project*
