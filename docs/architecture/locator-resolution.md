# Locator Resolution Guide

How VOID resolves element locators at runtime — from enum constant to engine-agnostic `LocatorDescriptor`.

---

## Table of Contents

1. [Overview](#overview)
2. [The Resolution Pipeline](#the-resolution-pipeline)
3. [LocatorDescriptor](#locatordescriptor)
4. [LocatorStrategy](#locatorstrategy)
5. [LocatorResolvers — strict() vs legacyPadded()](#locatorresolvers--strict-vs-legacypadded)
6. [LocatorRequest](#locatorrequest)
7. [ElementRole](#elementrole)
8. [Locator File Formats](#locator-file-formats)
9. [LocatorSource Implementations](#locatorsource-implementations)
10. [Template Substitution](#template-substitution)
11. [ByParser and Prefix Strategies](#byparser-and-prefix-strategies)
12. [Engine Resolution: UIEngine.resolve()](#engine-resolution-uiengineresolve)
13. [Scoped Locators: Parent→Child](#scoped-locators-parentchild)
14. [Migration: Properties → JSON](#migration-properties--json)
15. [Configuration](#configuration)
16. [Troubleshooting](#troubleshooting)

---

## Overview

VOID separates **what** an element is (enum constant) from **where** it lives on the page (locator string). The resolution pipeline bridges the gap:

```
Enum Constant → LocatorRequest → LocatorSource → Template Substitution → LocatorDescriptor
```

Locators are never hardcoded in Java. They live in external `.properties` or `.json` files, resolved at runtime. The result is a `LocatorDescriptor` — an engine-agnostic record that each `UIEngine` translates into its native locator type.

### Two Resolution Entry Points

| Entry Point | Used By | Returns |
|-------------|---------|---------|
| `UIEngine.resolve(element, role, args)` | Action/Flow/FlowExecutor (preferred) | `LocatorDescriptor` |
| `LocatorResolvers.strict().resolve(request)` | Interactions (legacy) | `By` (Selenium) |

Both paths use the same underlying sources, templates, and files. The difference is the output type.

---

## The Resolution Pipeline

### Primary Path (UIEngine)

```
1. Capability interface method   → element.click() / element.type("text")
2. Inside Action lambda          → engine.resolve(this, ElementRole.TRIGGER)
   ├─ Engine reads element.getExternalFileName() + getAllLocatorRoles()
   ├─ Builds LocatorRequest internally
   ├─ Delegates to LocatorSourceRegistry
   ├─ Template substitution applied
   └─ Returns LocatorDescriptor(value, strategy, args)
3. Engine executes               → engine.click(descriptor)
```

### Legacy Path (LocatorResolvers)

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

## LocatorDescriptor

`LocatorDescriptor` is the engine-agnostic locator record that bridges resolution and execution:

```java
public record LocatorDescriptor(
    String value,              // "//button[@id='apply']"
    LocatorStrategy strategy,  // XPATH, CSS, ID, NAME
    Object[] args,             // dynamic substitution args (metadata/logging)
    LocatorDescriptor parent   // optional parent scope (null = global)
) {}
```

### Factory Methods

```java
// Infer strategy from value
LocatorDescriptor.of("//button[@id='apply']")                        // XPATH inferred

// Explicit strategy
LocatorDescriptor.of("button.primary", LocatorStrategy.CSS)          // CSS explicit

// With args metadata
LocatorDescriptor.of("//tr[@data-user='john']", LocatorStrategy.XPATH, "john")
```

### Engine Translation

Each engine translates `LocatorDescriptor` into its native locator:

| Engine | XPATH | CSS | ID |
|--------|-------|-----|-----|
| **SeleniumEngine** | `By.xpath(value)` | `By.cssSelector(value)` | `By.id(value)` |
| **PlaywrightEngine** (via same `UIEngine` contract) | `page.locator("xpath=" + value)` | `page.locator(value)` | `page.locator("#" + value)` |

---

## LocatorStrategy

```java
public enum LocatorStrategy {
    XPATH,   // XPath expression
    CSS,     // CSS selector
    ID,      // Element ID
    NAME;    // Element name attribute

    public static LocatorStrategy infer(String locatorValue) {
        // Starts with // or (// → XPATH
        // Otherwise → CSS (safest default)
    }
}
```

### Inference Rules

| Pattern | Strategy |
|---------|----------|
| Starts with `//` or `(//` or `(./` | `XPATH` |
| Starts with `#` | `CSS` (ID shorthand) |
| Everything else | `CSS` (default) |

---

## LocatorResolvers — strict() vs legacyPadded()

`LocatorResolvers` is the main entry point for direct resolution (legacy path). It provides two preconfigured resolver singletons:

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
// Engine-agnostic descriptors (preferred)
LocatorDescriptor d = Via.descriptor(MyElements.SAVE_BUTTON);
LocatorDescriptor d = Via.descriptor(MyElements.COUNTRY_DROPDOWN, ElementRole.LIST, "Australia");
LocatorDescriptor d = Via.descriptor("common-elements.json", "searchInput");

// Legacy: Selenium By (deprecated)
By locator = Via.locator(MyElements.SAVE_BUTTON);
By listLocator = Via.locator(MyElements.COUNTRY_DROPDOWN, ElementRole.LIST, "Australia");
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
| `TRIGGER`         | `Clickable`, `Selectable`, `Checkable`    | Clickable trigger (button/icon)           |
| `INPUT`           | `Typeable`, `Uploadable`                  | Text or file input field                  |
| `LIST`            | `Selectable`, `Listable`                  | Options panel / list container            |
| `TEXT`            | `ReadOnly`, `Hoverable`                   | Static text element                       |
| `SEARCH_INPUT`    | `Searchable`, `SearchableDropdown`, `SearchField` | Search text input              |
| `SEARCH_BUTTON`   | `SearchField`, `SearchableDropdown`       | Search action button                      |
| `SEARCH_RESULT`   | `Searchable`, `SearchableDropdown`        | Search result list/panel                  |
| `TOOLTIP_CONTENT` | `Hoverable`                               | Full tooltip text element                 |
| `TABLE`           | `Table`, `EditableTable`                  | Table root element                        |
| `ROW`             | `Table`, `EditableTable`                  | Row locator                               |
| `COLUMN`          | `Table`, `EditableTable`                  | Column locator                            |
| `CELL`            | `Table`, `EditableTable`                  | Cell locator                              |
| `HEADER`          | `Table`, `EditableTable`                  | Header cell locator                       |
| `ADD_ROW_BUTTON`  | `EditableTable`                           | "Add row" button                          |
| `REMOVE_ROW_BUTTON` | `EditableTable`                        | "Remove row" button                       |
| `FOOTER_INPUT_ROW` | `EditableTable`                          | Footer input field                        |
| `MULTI_TRIGGER`   | `MultiSelectable`                         | Repeated dropdown trigger (e.g. 3-dots)   |
| `MULTI_LIST`      | `MultiSelectable`                         | Repeated dropdown list                    |

### Role Maps

Every element exposes its locators via `getAllLocatorRoles()`:

```java
Map<ElementRole, String> roles = element.getAllLocatorRoles();
// e.g. {TRIGGER="DROPDOWN_BUTTON", LIST="DROPDOWN_LIST"}
```

### Capability Interface → Role Mapping

| Capability | Primary Role | Additional Roles |
|------------|-------------|-----------------|
| `Clickable` | `TRIGGER` | — |
| `Typeable` | `INPUT` | — |
| `ReadOnly` | `TEXT` | — |
| `Checkable` | `TRIGGER` | — |
| `Selectable` | `TRIGGER` | `LIST` |
| `MultiSelectable` | `MULTI_TRIGGER` | `MULTI_LIST` |
| `SearchField` | `SEARCH_INPUT` | `SEARCH_BUTTON` |
| `Searchable` | `SEARCH_INPUT` | `SEARCH_BUTTON`, `SEARCH_RESULT` |
| `SearchableDropdown` | `TRIGGER` | `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT` |
| `Hoverable` | `TEXT` | `TOOLTIP_CONTENT` |
| `Uploadable` | `INPUT` | — |
| `Table` | `TABLE` | `ROW`, `COLUMN`, `CELL`, `HEADER` |
| `EditableTable` | `TABLE` | `ROW`, `COLUMN`, `CELL`, `HEADER`, `ADD_ROW_BUTTON`, `REMOVE_ROW_BUTTON`, `FOOTER_INPUT_ROW` |
| `Listable` | `LIST` | — |

---

## Conventional Repository Path

When `getExternalFileName()` returns `null` (the default for all minimal element enums), VOID derives the locator file path from the element's declaring page class:

```
FQCN: tests.demo.pages.DemoLoginPage
→ Classpath path: tests/demo/pages/DemoLoginPage/locators.json
```

The file is looked up on the classpath under `src/main/resources/`. No configuration needed — the path is deterministic and always unique per page class.

### Qualified Key Format

Locator keys in the conventional JSON use the fully-qualified format `PageClass.EnumClass.CONSTANT.ROLE`, mirroring the Java navigation path:

```json
{
  "DemoLoginPage": {
    "Credentials": {
      "USERNAME_INPUT": { "INPUT":   "//input[@id='username']" },
      "PASSWORD_INPUT": { "INPUT":   "//input[@id='password']" }
    },
    "Button": {
      "LOGIN_BUTTON":   { "TRIGGER": "//button[@type='submit']" }
    },
    "Labels": {
      "SUCCESS_MESSAGE": { "TEXT":   "//div[@id='flash']" }
    }
  }
}
```

The role key (`TRIGGER`, `INPUT`, `TEXT`, etc.) maps to `ElementRole` and is filled in by the `--sync` generator from the element's capability interface.

### Generate with `--sync`

```bash
# Creates locators.properties template — fill in XPath values
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.demo.pages.DemoLoginPage"

# Re-run after filling values — writes locators.json
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.demo.pages.DemoLoginPage"
```

In Claude Code: `/sync-locators tests.demo.pages.DemoLoginPage`.

Add `--prune` to remove keys for constants that no longer exist in the enum.

### Opting out of the convention

Override `getExternalFileName()` on the enum to point to a named file:

```java
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;

    @Override public String getExternalFileName() { return "shared-credentials.json"; }
}
```

Named files are resolved from `locators/json/` (or `locators/properties/`). Both conventional and named-file elements can coexist in the same page interface.

---

## Locator File Formats

### Properties Format (named files only)

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

| Prefix             | Strategy / LocatorStrategy    | Example                              |
|--------------------|-------------------------------|--------------------------------------|
| `xpath=`           | `XPATH` / `By.xpath(...)`     | `xpath=//input[@id='user']`          |
| `css=`             | `CSS` / `By.cssSelector(...)` | `css=input.login-field`              |
| `id=`              | `ID` / `By.id(...)`          | `id=signInBtn`                       |
| `name=`            | `NAME` / `By.name(...)`      | `name=username`                      |
| `tag=`             | `By.tagName(...)`             | `tag=button`                         |
| `linkText=`        | `By.linkText(...)`            | `linkText=Sign In`                   |
| `partialLinkText=` | `By.partialLinkText(...)`     | `partialLinkText=Sign`               |
| *(no prefix)*      | `XPATH` (default)             | `//input[@id='user']`                |

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

### Effective Args

Elements support override args via `effectiveArgs()`:

```java
// Element's own args used by default
element.getArgs()           // → ["john.doe"]

// Override args take precedence when non-empty
element.effectiveArgs("jane.doe")  // → ["jane.doe"]
element.effectiveArgs()             // → ["john.doe"] (falls back to getArgs())
```

---

## ByParser and Prefix Strategies

`ByParser` converts a raw string (with optional prefix) into a Selenium `By` for legacy resolver paths:

```java
By result = ByParser.parse("css=input.login");    // → By.cssSelector("input.login")
By result = ByParser.parse("//div[@id='main']");  // → By.xpath("//div[@id='main']")
```

The parser uses `ByPrefixStrategy` to match prefix tokens. If no prefix is found, the default is `By.xpath`. In the modern path, `UIEngine` consumes `LocatorDescriptor` and each engine performs its own native translation.

---

## Engine Resolution: UIEngine.resolve()

In the primary Action/Flow/FlowExecutor path, `UIEngine.resolve()` is the single resolution authority:

```java
// Resolution inside an Action lambda
default Action click() {
    return engine -> {
        var d = engine.resolve(this, ElementRole.TRIGGER);  // returns LocatorDescriptor
        engine.click(d);                                     // engine translates internally
    };
}
```

### UIEngine.resolve() Signatures

```java
// Resolve from element + role
LocatorDescriptor resolve(Element element, ElementRole role, Object... args);

// Resolve from raw file + key
LocatorDescriptor resolve(String fileName, String key, Object... args);
```

The engine internally:
1. Reads the element's `getAllLocatorRoles()` to find the key for the given role
2. Reads `getExternalFileName()` and `effectiveArgs(args)`
3. Delegates to `LocatorSourceRegistry` for file/key lookup
4. Applies template substitution
5. Wraps the result in a `LocatorDescriptor` with inferred `LocatorStrategy`

---

## Scoped Locators: Parent→Child

For elements within a parent scope, use `LocatorDescriptor.withParent()`:

```java
LocatorDescriptor table = engine.resolve(MyPage.DATA_TABLE, ElementRole.TABLE);
LocatorDescriptor row = engine.resolve(MyPage.DATA_TABLE, ElementRole.ROW, "john")
                              .withParent(table);

// Engine finds parent first, then searches within
engine.click(row);
```

### How scoped resolution works

```java
descriptor.isScoped()  // true if parent != null
descriptor.parent()    // the parent LocatorDescriptor
```

The engine recursively resolves parent→child at execution time:
1. Find the parent element in the DOM
2. Search within the parent for the child element
3. Perform the action on the child

---

## Migration: Properties → JSON / Sync

VOID includes a built-in CLI for generating and syncing locator files.

### CLI

```bash
# Sync: generate locators.properties template + write locators.json (conventional path)
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.demo.pages.DemoLoginPage"

# Sync with orphan key removal
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.demo.pages.DemoLoginPage --prune"

# Preview: print resolved JSON to stdout (no files written)
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--print elements.LoginPageElements"

# Write to default directory (src/main/resources/locators/json/)
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--write elements.LoginPageElements"

# Write to conventional path
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--write-conventional tests.demo.pages.DemoLoginPage"

# Write to a specific file
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--write elements.LoginPageElements path/to/output.json"
```

> Always prefix with `mvn process-resources -q` when using `--sync` on a newly created page class, so the classpath reflects the latest `.properties` file.

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

**Conventional path** (default — `getExternalFileName()` returns `null`):

| Page Class FQCN | Conventional Path |
|-----------------|------------------|
| `tests.demo.pages.DemoLoginPage` | `tests/demo/pages/DemoLoginPage/locators.json` |
| `tests.app.pages.ManageUsersPage` | `tests/app/pages/ManageUsersPage/locators.json` |

**Named files** (opt-in via `getExternalFileName()`):

| Element Interface                 | Properties File                          | JSON File                            |
|-----------------------------------|------------------------------------------|--------------------------------------|
| `LoginPageElements`               | `locators/properties/login-page-elements.properties` | `locators/json/login-page-elements.json` |
| `ManageUsersElements`             | `locators/properties/manage-users-elements.properties` | `locators/json/manage-users-elements.json` |

---

## Troubleshooting

### "Locator not found" Errors

**Symptoms**: `NullPointerException` or "no value for key" in logs.

**Check**:
1. The `.properties` or `.json` file exists at the correct classpath location.
2. The key in the file matches exactly what `getPrimaryLocator()` / `getAllLocatorRoles()` returns (case-sensitive).
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

### LocatorDescriptor Strategy Mismatch

**Symptoms**: Engine uses wrong locator type (e.g., treats CSS as XPath).

**Check**:
1. Ensure the locator value uses the correct prefix (`xpath=`, `css=`, `id=`).
2. If no prefix, verify the value format — XPath must start with `//` or `(//`.
3. The strategy is inferred via `LocatorStrategy.infer()` when no prefix is present.

---

## Related Documentation

- [System Overview](system-overview.md) — full execution flow with UIEngine
- [Quick Start Guide](quick-start.md) — defining elements and locators
- [Configuration Reference](configuration-reference.md) — all config keys
- [Hooks Pipeline](hooks-pipeline.md) — how hooks interact with resolution
- [`core/resolvers/locator/json/README.md`](../../src/main/java/core/resolvers/locator/json/README.md) — JSON migration internals
- [`UIEngine.java`](../../src/main/java/core/engine/UIEngine.java) — execution contract with resolve() methods

---

*MIT License © 2025–2026 VOID Project*
