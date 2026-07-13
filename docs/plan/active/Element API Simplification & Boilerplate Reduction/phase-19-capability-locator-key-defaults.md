# Phase 19 — Capability Locator Key Defaults

**Status:** Partial — single-role defaults shipped; multi-role and fully-qualified key format pending Phase 6  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — changes the properties key format; requires resolver update and migration of existing `.properties` files

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

enum Button implements Clickable {
    LOGIN_BUTTON("Login");
    @Override public String getTriggerLocator() { return name(); } // ← boilerplate
}
```

---

## Key Format Convention

The locator key for any element is the fully-qualified dotted path:

```
PageClass.EnumClass.CONSTANT_NAME.roleSuffix
```

Where `roleSuffix` is the capability method name with `get` stripped from the front and `Locator` stripped from the end, lowercased:

| Method | Role suffix |
|--------|-------------|
| `getTriggerLocator` | `trigger` |
| `getInputLocator` | `input` |
| `getTextLocator` | `text` |
| `getListLocator` | `list` |
| `getToolTipContentLocator` | `toolTipContent` |
| `getSearchInputLocator` | `searchInput` |
| `getSearchButtonLocator` | `searchButton` |
| `getSearchResultLocator` | `searchResult` |
| `getTableLocator` | `table` |

**Examples from `DemoLoginPage`:**

```properties
# tests/demo/pages/DemoLoginPage/locators.properties
DemoLoginPage.Credentials.USERNAME_INPUT.input=//input[@id='username']
DemoLoginPage.Credentials.PASSWORD_INPUT.input=//input[@id='password']
DemoLoginPage.Button.LOGIN_BUTTON.trigger=//button[@type='submit']
DemoLoginPage.Labels.SUCCESS_MESSAGE.text=//div[@id='flash']
```

Multi-role element (`Selectable` / `NavBar`):
```properties
DemoLoginPage.NavBar.PARTNER.trigger=//button[@data-partner]
DemoLoginPage.NavBar.PARTNER.list=//ul[@class='partner-options']
```

### Why fully-qualified — not just `CONSTANT_NAME.role`

The properties file is flat; without the page and enum prefix, two pages with a `USERNAME_INPUT` would collide if ever merged or referenced from a shared context. The full path is also self-describing — a developer reading the key knows exactly which element it belongs to, with no need to look at the file path. This mirrors what the JSON file expresses through nesting:

```
JSON nesting:          DemoLoginPage → Credentials → USERNAME_INPUT   (→ XPath)
Properties flat key:   DemoLoginPage.Credentials.USERNAME_INPUT.input  (→ XPath)
```

Same information, different representation.

### Default key derivation

The default implementation in each capability interface derives the key from the enum's class hierarchy at runtime:

```java
// example: Typeable
default String getInputLocator() {
    Enum<?> e = (Enum<?>) this;
    Class<?> enumClass = e.getDeclaringClass();
    Class<?> pageClass = enumClass.getEnclosingClass();
    String prefix = (pageClass != null)
        ? pageClass.getSimpleName() + "." + enumClass.getSimpleName()
        : enumClass.getSimpleName();
    return prefix + "." + e.name() + ".input";
}
```

An enum that needs a custom key simply overrides the method and returns whatever string it needs.

---

## Resolver Impact

The runtime locator resolver and `EnumLocatorScanner` currently do a flat properties lookup using the value returned by the locator method. After this phase, the value is the full qualified key (`DemoLoginPage.Credentials.USERNAME_INPUT.input`). The flat lookup in `PropertiesIndex` requires no structural change — it already does `props.getProperty(rawVal)` — the key just changes from `"USERNAME_INPUT"` to `"DemoLoginPage.Credentials.USERNAME_INPUT.input"`.

**What must change:**
- All existing `.properties` files — keys must be migrated to the fully-qualified format.
- `EnumLocatorScanner` — currently emits the constant name as the JSON key by calling `((Enum<?>) constant).name()` directly. After this phase it still does that (JSON key stays as constant name, JSON is nesting-aware). The properties lookup already uses the locator method return value, which will now be the full qualified key.
- Phase 6 template generator — must emit fully-qualified keys from the start.

**What does NOT change:**
- The JSON file format — nesting already encodes the qualification. The JSON tree is written using `e.name()` as the leaf key and the class hierarchy for nesting. This is unchanged.
- Hardcoded elements — an enum that overrides and returns an XPath directly continues to work; the resolver falls back to the raw return value if no properties entry is found.

---

## Part A — Single-Role Defaults (unblocked)

| Interface | Method | Default return |
|-----------|--------|----------------|
| `Clickable` | `getTriggerLocator()` | `pageClass.SIMPLE.enumClass.SIMPLE.name().trigger` |
| `Typeable` | `getInputLocator()` | `… .input` |
| `ReadOnly` | `getTextLocator()` | `… .text` |
| `Hoverable` | `getToolTipContentLocator()` | `… .toolTipContent` |
| `Listable` | `getListLocator()` | `… .list` |
| `Uploadable` | `getInputLocator()` | `… .input` |

After Part A, page enums that need no custom key remove the locator override entirely:

```java
// BEFORE
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
    @Override public String getInputLocator() { return name(); }
}

// AFTER — zero boilerplate
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;
}
```

**Blocker:** Existing `.properties` files must be updated to use the qualified format before Part A can be committed. Implement the migration in the same PR.

---

## Part B — Multi-Role Defaults (unblocked — same format, no ambiguity)

Because the role suffix is always present in the key, multi-role interfaces are no longer a special case. `Selectable.getTriggerLocator()` and `Selectable.getListLocator()` both default to the fully-qualified key with their respective suffix — they are distinct by definition:

```
DemoLoginPage.NavBar.PARTNER.trigger  ← getTriggerLocator()
DemoLoginPage.NavBar.PARTNER.list     ← getListLocator()
```

This means Part B is not blocked on Phase 6. The fully-qualified format already solves the multi-role disambiguation problem that made Part B appear harder.

| Interface | Method | Default return |
|-----------|--------|----------------|
| `Selectable` | `getTriggerLocator()` | `… .trigger` |
| `Selectable` | `getListLocator()` | `… .list` |
| `MultiSelectable` | `getTriggerLocator()` | `… .trigger` |
| `MultiSelectable` | `getListLocator()` | `… .list` |
| `SearchField` | `getSearchInputLocator()` | `… .searchInput` |
| `SearchField` | `getSearchButtonLocator()` | `… .searchButton` |
| `Searchable` | `getSearchResultLocator()` | `… .searchResult` |
| `SearchableDropdown` | `getTriggerLocator()` | `… .trigger` |
| `Table` | `getTableLocator()` | `… .table` |

---

## Implementation Order

1. Update all existing `.properties` files to the fully-qualified key format.
2. Add defaults to all capability interfaces (Parts A and B together — same format, same commit).
3. Delete the now-redundant locator overrides from all page enums.
4. Update Phase 6 template generator spec to emit fully-qualified keys from the start.

---

## Affected Files

**Capability interfaces (all):**
- `elements/api/capability/Clickable.java`
- `elements/api/capability/Typeable.java`
- `elements/api/capability/ReadOnly.java`
- `elements/api/capability/Hoverable.java`
- `elements/api/capability/Listable.java`
- `elements/api/capability/Uploadable.java`
- `elements/api/capability/Selectable.java`
- `elements/api/capability/MultiSelectable.java`
- `elements/api/capability/SearchField.java`
- `elements/api/capability/Searchable.java`
- `elements/api/capability/SearchableDropdown.java`
- `elements/api/capability/Table.java`

**Page enums:** all files with redundant `return name()` overrides.

**Resources:** all `.properties` locator files — key format migration.

---

## Exit Criteria

- Page enums that need the default key declare only the enum constants — no locator override
- Properties files use `PageClass.EnumClass.CONSTANT.role=` for all entries
- JSON file format is unchanged (nesting stays)
- `EnumLocatorScanner` resolves properties via the full qualified key returned by locator methods
- Phase 6 template generator spec records the fully-qualified key format as the target output format
- Full suite passes with no regressions

---

*MIT License Copyright (c) 2025-2026 VOID Project*
