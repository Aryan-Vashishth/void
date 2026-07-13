# Phase 19 — Capability Locator Key Defaults

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — purely additive defaults; no call sites change

---

## Objective

Remove the last category of repetitive locator boilerplate from page enum declarations by making each capability interface's locator method default to `name()` — the same convention the JSON repository already uses as its key.

---

## Problem

Every enum currently must explicitly override its capability's locator method, even when the intent is trivially "use the constant name":

```java
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
    @Override public String getInputLocator() { return name(); }   // ← boilerplate
}

enum Labels implements ReadOnly {
    SUCCESS_MESSAGE;
    @Override public String getTextLocator() { return name(); }    // ← boilerplate
}
```

The JSON file already uses the constant name as the key. The properties file already uses the constant name as the key for single-role elements. The `name()` override adds nothing but noise.

---

## Convention

Method name → properties/JSON key:
- Strip leading `get`, strip trailing `Locator`, lowercase → the **role suffix**
- `getTriggerLocator` → `trigger`
- `getInputLocator` → `input`
- `getTextLocator` → `text`
- `getListLocator` → `list`
- `getSearchInputLocator` → `searchInput` (camelCase preserved)

**Single-role elements** — one locator method, default returns `name()`:
```properties
USERNAME_INPUT=//input[@id='username']   # key is the constant name, no suffix needed
```

**Multi-role elements** — multiple locator methods, default returns `name() + "." + roleSuffix`:
```properties
PARTNER.trigger=//button[@data-partner]
PARTNER.list=//ul[@class='partner-options']
```

The same constant name used in JSON is reused in properties — no new naming scheme to learn.

---

## Part A — Single-Role Defaults (unblocked)

Add `default` to these methods across the single-role capability interfaces:

| Interface | Method | Default return |
|-----------|--------|----------------|
| `Clickable` | `getTriggerLocator()` | `((Enum<?>) this).name()` |
| `Typeable` | `getInputLocator()` | `((Enum<?>) this).name()` |
| `ReadOnly` | `getTextLocator()` | `((Enum<?>) this).name()` |
| `Hoverable` | `getToolTipContentLocator()` | `((Enum<?>) this).name()` |
| `Listable` | `getListLocator()` | `((Enum<?>) this).name()` |
| `Uploadable` | `getInputLocator()` | `((Enum<?>) this).name()` |

`Listable.getListLocator()` is safe to default because `Listable` is used standalone (it is not `Selectable`). `Selectable` and other multi-role interfaces are Part B.

After Part A, page enums that implement a single-role capability and have no custom locator key need no locator override at all:

```java
// BEFORE
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
    @Override public String getInputLocator() { return name(); }
}

// AFTER
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
}
```

**Resolver compatibility:** No change needed. The runtime already looks up `getInputLocator()` → `"USERNAME_INPUT"` against the JSON/properties repository. The default just moves that `return name()` from the call site to the interface.

---

## Part B — Multi-Role Defaults (depends on Phase 6)

Multi-role interfaces cannot default both methods to `name()` — that would emit the same lookup key for two different roles, making them indistinguishable.

The solution is to default to `name() + "." + roleSuffix`:

| Interface | Method | Default return |
|-----------|--------|----------------|
| `Selectable` | `getTriggerLocator()` | `name() + ".trigger"` |
| `Selectable` | `getListLocator()` | `name() + ".list"` |
| `MultiSelectable` | `getTriggerLocator()` | `name() + ".trigger"` |
| `MultiSelectable` | `getListLocator()` | `name() + ".list"` |
| `SearchField` | `getSearchInputLocator()` | `name() + ".searchInput"` |
| `SearchField` | `getSearchButtonLocator()` | `name() + ".searchButton"` |
| `Searchable` | `getSearchResultLocator()` | `name() + ".searchResult"` |
| `SearchableDropdown` | `getTriggerLocator()` | `name() + ".trigger"` |

**This requires two co-ordinated changes:**
1. The properties file template generator (Phase 6) must emit `CONSTANT_NAME.roleSuffix=` keys for multi-role elements.
2. `EnumLocatorScanner` must look up `CONSTANT_NAME.roleSuffix` (not raw `CONSTANT_NAME`) when the locator method returns a dotted key.

These changes are Phase 6 scope and must land together. Do not implement Part B independently.

**`Table` and `EditableTable`** — most methods already default to `null` (they are optional roles). Only `getTableLocator()` is abstract. Apply `name() + ".table"` as its default.

---

## Implementation Order

1. **Part A first** — add defaults to single-role interfaces; delete the boilerplate overrides from all page enums that were using `return name()`.
2. **Resolve Open Decision 2 and 5** (Phase 6 template format) to confirm the `.role` suffix convention.
3. **Part B** — add multi-role defaults + update Phase 6 template generator + update `EnumLocatorScanner` lookup logic, in a single coordinated commit.

---

## Affected Files (Part A)

- `src/main/java/elements/api/capability/Clickable.java`
- `src/main/java/elements/api/capability/Typeable.java`
- `src/main/java/elements/api/capability/ReadOnly.java`
- `src/main/java/elements/api/capability/Hoverable.java`
- `src/main/java/elements/api/capability/Listable.java`
- `src/main/java/elements/api/capability/Uploadable.java`
- All page enum files with a now-redundant `return name()` override

## Affected Files (Part B)

- All multi-role capability interfaces
- `src/main/java/core/resolvers/locator/json/EnumLocatorScanner.java` — role-suffixed lookup
- Phase 6 template generator (not yet created)
- Existing `.properties` files — keys must be migrated to `CONSTANT_NAME.role=` format

---

## Exit Criteria

- Single-role page enums declare only the enum constants and no locator override, unless a custom key is needed
- Multi-role page enums declare only the enum constants and no locator override (Part B)
- Properties files use `CONSTANT_NAME=` for single-role and `CONSTANT_NAME.role=` for multi-role
- `EnumLocatorScanner` resolves both formats correctly
- Full suite passes with no regressions

---

*MIT License Copyright (c) 2025-2026 VOID Project*
