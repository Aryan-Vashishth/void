# Locator Resolution Guide

How VOID resolves element locators at runtime — from enum constant to Selenium `By` object.

---

## Table of Contents

1. [Overview](#overview)
2. [The Resolution Pipeline](#the-resolution-pipeline)
3. [LocatorResolvers — strict() vs legacyPadded()](#locatorresolvers--strict-vs-legacypadded)
4. [LocatorRequest](#locatorrequest)
5. [ElementRole](#elementrole)
6. [Locator File Formats](#locator-file-formats)
7. [LocatorSource Implementations](#locatorsource-implementations)
8. [Template Substitution](#template-substitution)
9. [ByParser and Prefix Strategies](#byparser-and-prefix-strategies)
10. [Migration: Properties → JSON](#migration-properties--json)
11. [Configuration](#configuration)
12. [Troubleshooting](#troubleshooting)

---

## Overview

VOID separates **what** an element is (enum constant) from **where** it lives on the page (locator string). The resolution pipeline bridges the gap:

```
Enum Constant → LocatorRequest → LocatorSource → Template Substitution → ByParser → By
```

Locators are never hardcoded in Java. They live in external `.properties` or `.json` files, resolved at runtime by the `LocatorResolvers` API.

---

## The Resolution Pipeline

```
1. Element.getExternalFileName()   → "login-page-elements.properties"
2. Element.getPrimaryLocator()     → "USERNAME_INPUT"    (locator key)
3. Element.getArgs()               → ["admin"]           (template args)
         ↓
4. LocatorRequest.of(file, key, args)
         ↓
5. LocatorResolver.resolve(request)
   ├─ LocatorSourceRegistry selects source (JSON → Properties → Hardcoded)
   ├─ Source reads raw template: "//input[@id='%s']"
   ├─ LocatorTemplate substitutes args: "//input[@id='admin']"
   └─ ByParser converts: By.xpath("//input[@id='admin']")
         ↓
6. Result: org.openqa.selenium.By
```

---

## LocatorResolvers — strict() vs legacyPadded()

`LocatorResolvers` is the main entry point. It provides two preconfigured resolver singletons:

### `LocatorResolvers.strict()` ⭐ Recommended

- **Template policy**: `STRICT` — args must exactly match the number of `%s` placeholders.
- **Sources**: `HardcodedLocatorSource` → `PropertiesLocatorSource` → `JsonLocatorSource`
- **Use when**: Writing new code, JSON locator files.

```java
By locator = LocatorResolvers.strict().resolve(element);
By locator = LocatorResolvers.strict().resolve(LocatorRequest.of("login.json", "USERNAME"));
```

### `LocatorResolvers.legacyPadded()`

- **Template policy**: `PAD_LAST` — if there are fewer args than `%s` placeholders, the last arg is repeated to fill.
- **Sources**: `HardcodedLocatorSource` → `LayeredPropertiesLocatorSource` (cached, TEST-over-MAIN) → `JsonLocatorSource`
- **Use when**: Maintaining backward compatibility with existing `.properties`-based locator files.

```java
By locator = LocatorResolvers.legacyPadded().resolve(element);
```

### Using Via for convenience

The `Via` utility provides shorthand methods:

```java
// Resolve element's primary locator
By locator = Via.locator(MyElements.SAVE_BUTTON);

// Resolve a specific role
By listLocator = Via.locator(MyElements.COUNTRY_DROPDOWN, ElementRole.LIST, "Australia");

// Resolve from raw file + key
By raw = Via.locator("common-elements.json", "searchInput");
```

---

## LocatorRequest

`LocatorRequest` is an immutable value object that encapsulates the three inputs to locator resolution:

```java
public record LocatorRequest(String fileName, String key, Object[] args) { ... }
```

| Field      | Description                                                         |
|------------|---------------------------------------------------------------------|
| `fileName` | External locator bundle name (e.g. `"login-page.properties"`). `null` for hardcoded templates. |
| `key`      | Locator key inside the bundle, or the template itself when `fileName` is `null`. |
| `args`     | Formatting arguments for `%s` substitution. Never `null` (defaults to empty array). |

### Factory Methods

```java
// No args
LocatorRequest.of("login.properties", "USERNAME_INPUT")

// With args
LocatorRequest.of("user-cards.properties", "USER_ROW", "john.doe")

// Hardcoded template (no file)
LocatorRequest.of(null, "//input[@id='%s']", "username")
```

### Hardcoded Detection

```java
request.isHardcoded()  // true when fileName == null → key IS the template
```

---

## ElementRole

`ElementRole` is an enum that classifies the semantic purpose of each locator within a multi-role element:

| Role              | Used By                                   | Description                               |
|-------------------|-------------------------------------------|-------------------------------------------|
| `PRIMARY`         | `Element`                                 | Primary locator (first attempt)           |
| `SECONDARY`       | `Element`                                 | Fallback locator                          |
| `TRIGGER`         | `Clickable`, `Dropdown`, `Checkbox`       | Clickable trigger (button/icon)           |
| `INPUT`           | `TextInputField`, `FileInputElement`      | Text or file input field                  |
| `LIST`            | `Dropdown`                                | Options panel / list container            |
| `TEXT`            | `ReadOnlyElement`, `ToolTipElement`       | Static text element                       |
| `SEARCH_INPUT`    | `Searchable`, `SearchableDropdown`        | Search text input                         |
| `SEARCH_BUTTON`   | `SearchField`, `SearchableDropdown`       | Search action button                      |
| `SEARCH_RESULT`   | `Searchable`, `SearchableDropdown`        | Search result list/panel                  |
| `TOOLTIP_CONTENT` | `ToolTipElement`                          | Full tooltip text element                 |
| `TABLE`           | `TableElement`, `WritableTableElement`    | Table root element                        |
| `ROW`             | `TableElement`, `WritableTableElement`    | Row locator                               |
| `COLUMN`          | `TableElement`, `WritableTableElement`    | Column locator                            |
| `CELL`            | `TableElement`, `WritableTableElement`    | Cell locator                              |
| `HEADER`          | `TableElement`, `WritableTableElement`    | Header cell locator                       |
| `ADD_ROW`         | `WritableTableElement`                    | "Add row" button                          |
| `REMOVE_ROW`      | `WritableTableElement`                    | "Remove row" button                       |
| `FOOTER_INPUT`    | `WritableTableElement`                    | Footer input field                        |
| `MULTI_TRIGGER`   | `MultipleIdenticalDropdowns`              | Repeated dropdown trigger (e.g. 3-dots)   |
| `MULTI_LIST`      | `MultipleIdenticalDropdowns`              | Repeated dropdown list                    |

### Role Maps

Every element exposes its locators via `getAllLocatorRoles()`:

```java
Map<ElementRole, String> roles = element.getAllLocatorRoles();
// e.g. {TRIGGER="DROPDOWN_BUTTON", LIST="DROPDOWN_LIST"}
```

---

## Locator File Formats

### Properties Format

```properties
# src/main/resources/locators/properties/login-page-elements.properties
USERNAME_INPUT=//input[@id='username']
PASSWORD_INPUT=css=input[type='password']
SIGN_IN_BUTTON=id=signInBtn
USER_ROW=//tr[@data-user='%s']
```

### JSON Format ⭐ Recommended

```json
{
  "LoginPageElements": {
    "Credentials": {
      "USERNAME_INPUT": "//input[@id='username']",
      "PASSWORD_INPUT": "css=input[type='password']"
    },
    "Actions": {
      "SIGN_IN_BUTTON": "id=signInBtn"
    }
  }
}
```

JSON files mirror the nested-enum structure of your element interfaces, making them self-documenting.

### Prefix Tokens

Both formats support prefix tokens to specify the locator strategy:

| Prefix             | Strategy                  | Example                              |
|--------------------|---------------------------|--------------------------------------|
| `xpath=`           | `By.xpath(...)`           | `xpath=//input[@id='user']`          |
| `css=`             | `By.cssSelector(...)`     | `css=input.login-field`              |
| `id=`              | `By.id(...)`              | `id=signInBtn`                       |
| `name=`            | `By.name(...)`            | `name=username`                      |
| `tag=`             | `By.tagName(...)`         | `tag=button`                         |
| `linkText=`        | `By.linkText(...)`        | `linkText=Sign In`                   |
| `partialLinkText=` | `By.partialLinkText(...)` | `partialLinkText=Sign`               |
| *(no prefix)*      | `By.xpath(...)` (default) | `//input[@id='user']`                |

---

## LocatorSource Implementations

The `LocatorSourceRegistry` tries sources in order until one returns a result:

| Source                           | Priority | Description                                          |
|----------------------------------|----------|------------------------------------------------------|
| `HardcodedLocatorSource`         | 1st      | Returns the key itself when `fileName == null`       |
| `PropertiesLocatorSource`        | 2nd      | Reads uncached `.properties` from MAIN classpath     |
| `LayeredPropertiesLocatorSource` | 2nd*     | Cached, TEST-over-MAIN layered `.properties` reader  |
| `JsonLocatorSource`              | 3rd      | Reads `.json` files via `JsonLocatorReader`          |

> *`LayeredPropertiesLocatorSource` is used by `legacyPadded()` in place of `PropertiesLocatorSource`.

---

## Template Substitution

Locator templates use `%s` placeholders that are replaced at resolve time with the element's `getArgs()`:

```properties
# Template in .properties
USER_ROW=//tr[@data-user='%s']//td[@class='%s']
```

```java
// Element provides args
@Override public Object[] getArgs() { return new Object[]{"john.doe", "email"}; }

// Resolved: //tr[@data-user='john.doe']//td[@class='email']
```

### Template Policies

| Policy      | Behavior                                                    |
|-------------|-------------------------------------------------------------|
| `STRICT`    | Args count must match `%s` count exactly. Throws on mismatch.|
| `PAD_LAST`  | If fewer args than placeholders, the last arg fills remaining slots. |

---

## ByParser and Prefix Strategies

`ByParser` converts a raw string (with optional prefix) into a Selenium `By`:

```java
By result = ByParser.parse("css=input.login");    // → By.cssSelector("input.login")
By result = ByParser.parse("//div[@id='main']");  // → By.xpath("//div[@id='main']")
```

The parser uses `ByPrefixStrategy` to match prefix tokens. If no prefix is found, the default is `By.xpath`.

---

## Migration: Properties → JSON

VOID includes a built-in migration tool to convert `.properties` locator files to the recommended JSON format.

### CLI

```bash
# Preview: print resolved JSON to stdout
java core.resolvers.locator.json.JsonMigratorCli --print elements.LoginPageElements

# Write to default directory (src/main/resources/locators/json/)
java core.resolvers.locator.json.JsonMigratorCli --write elements.LoginPageElements

# Write to a specific file
java core.resolvers.locator.json.JsonMigratorCli --write elements.LoginPageElements path/to/output.json
```

### Programmatic

```java
import core.resolvers.locator.json.JsonLocatorMigrator;

// Build JSON string
String json = JsonLocatorMigrator.buildResolvedJson(LoginPageElements.class);

// Build and write to default directory
Path file = JsonLocatorMigrator.writeResolvedJson(LoginPageElements.class);
```

### How Migration Works

```
JsonLocatorMigrator.writeResolvedJson(MyPageElements.class)
  └─► JsonTreeBuilder.build(rootClass)
        ├─ Walks nested classes/enums recursively
        ├─ For each enum → EnumLocatorScanner reads .properties via PropertiesIndex
        ├─ Resolves raw keys → XPath/CSS values
        └─ Returns Jackson ObjectNode tree
  └─► Serializes to pretty-printed JSON
  └─► Writes to locators/json/<name>-locators.json
```

> See [`core/resolvers/locator/json/README.md`](../src/main/java/core/resolvers/locator/json/README.md) for implementation details.

---

## Configuration

### Locator Base Paths

| Config Key                       | Default                | Set In             |
|----------------------------------|------------------------|--------------------|
| `locator.properties.base.path`   | `locators/properties/` | `ConfigLoader`     |
| `locator.json.base.path`         | `locators/json/`       | `ConfigLoader`     |

Override via system property:

```bash
mvn test -Dlocator.json.base.path=custom/locators/json/
```

### File Naming Convention

| Element Interface                 | Properties File                          | JSON File                            |
|-----------------------------------|------------------------------------------|--------------------------------------|
| `LoginPageElements`               | `login-page-elements.properties`         | `login-page-elements.json`           |
| `ManageUsersElements`             | `manage-users-elements.properties`       | `manage-users-elements.json`         |

The file name is returned by each element's `getExternalFileName()` method.

---

## Troubleshooting

### "Locator not found" Errors

**Symptoms**: `NullPointerException` or "no value for key" in logs.

**Check**:
1. The `.properties` or `.json` file exists at the correct classpath location.
2. The key in the file matches exactly what `getPrimaryLocator()` returns (case-sensitive).
3. The `getExternalFileName()` value includes the correct extension.

### "Wrong number of format arguments" (STRICT mode)

**Symptoms**: `IllegalFormatException` during template substitution.

**Check**:
1. Count the `%s` placeholders in the locator template.
2. Ensure `getArgs()` returns exactly that many arguments.
3. If you need flexible arg handling, use `legacyPadded()` instead.

### Locator Resolved from Wrong File

**Symptoms**: Element uses a locator value from a different file than expected.

**Check**:
1. Verify `getExternalFileName()` returns the correct file name.
2. If using `legacyPadded()`, check for TEST-over-MAIN classpath conflicts.
3. Enable DEBUG logging to see the full resolution trace:

```
[LOCATOR] Resolving:
          ├─ File        : login-page-elements.properties
          ├─ Key         : USERNAME_INPUT
          ├─ Args        : []
          └─ Hardcoded   : false
[LOCATOR] Final:
          ├─ Key         : USERNAME_INPUT
          ├─ Resolved    : //input[@id='username']
          └─ By          : By.xpath: //input[@id='username']
```

---

## Related Documentation

- [System Overview](system-overview.md) — full execution flow
- [Quick Start Guide](quick-start.md) — defining elements and locators
- [Configuration Reference](configuration-reference.md) — all config keys
- [`core/resolvers/locator/json/README.md`](../../src/main/java/core/resolvers/locator/json/README.md) — JSON migration internals

---

*MIT License © 2025–2026 VOID Project*


