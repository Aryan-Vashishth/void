# VOID Framework — Versatile Object-Oriented Integration for Debugging

## Overview

VOID (Versatile Object-Oriented Integration for Debugging) is a **next-generation Selenium test automation framework** — not a wrapper, but a **complete re-engineering of how Selenium frameworks are structured and executed**. Built on Java 17 with Maven, it combines an enum-driven element model, role-based locator resolution, and deep observability tooling into a cohesive, enterprise-ready platform.

---

## 🚀 Key Features

### 🧠 Not Just Another Selenium Wrapper
- Every UI element is modeled as a **typed enum constant** implementing fine-grained behavioral interfaces.
- The `VOID` façade (entry point) lazily initialises interaction helpers and keeps them cached per instance.
- Deep, color-coded logging with precise call-site tracing makes every failure reproducible.

### 🔧 Enum-Driven Object Model
- Elements are declared as enums implementing interfaces such as `Clickable`, `Dropdown`, or `ToolTipElement`.
- Nested enums (e.g., `ManageUsersElements.UserCards`) organise elements by page and functional context.
- Each enum constant carries its own locator key, external file reference, dynamic args, and display text.

### 📍 Role-Based Locator Resolution (`LocatorResolverV1`)
- All locator lookups go through `LocatorResolverV1`, which dispatches to **JSON** or **`.properties`** readers.
- Locator roles are typed via `ElementRole` enum — `PRIMARY`, `SECONDARY`, `TRIGGER`, `LIST`, `SEARCH_INPUT`, `SEARCH_RESULT`, `TOOLTIP_CONTENT`, `TABLE`, `ROW`, `CELL`, `MULTI_TRIGGER`, etc.
- Dynamic `%s` substitutions in locator templates are applied at resolve time.
- `UIContext` records the last resolved element/meta for stale-element retry and diagnostics.

### 🪝 Composable Before / After Action Hooks
- `Before` and `After` constant libraries provide pre-built `ActionHandler` instances:  
  `WAIT_FOR_ELEMENT_VISIBLE`, `WAIT_FOR_ELEMENT_CLICKABLE`, `HIGHLIGHT_ELEMENT`, `WAIT_FOR_ANGULAR_LOADER`, `LOG_INTENT`, `DO_NOTHING`, etc.
- Hook lists are passed directly to every interaction method overload for fully composable behaviour.

### 🧩 Interface-Driven Element Model

| Interface                        | Description                                                       |
|----------------------------------|-------------------------------------------------------------------|
| `Element`                        | Root contract — locator keys, args, display text, role map.       |
| `Clickable`                      | Clickable UI components.                                          |
| `Dropdown`                       | Trigger + list locators for single-value dropdowns.               |
| `MultipleIdenticalDropdowns`     | Repeated three-dots / context-menu dropdown patterns.             |
| `Searchable`                     | Search input + result list locators.                              |
| `SearchableDropdown`             | Searchable dropdown (input + option list).                        |
| `SearchField`                    | Standalone search-field abstraction.                              |
| `ToolTipElement`                 | Hover tooltip with `title`/`aria-label` attribute fallback.       |
| `TableElement`                   | Read-only structured table (rows, columns, cells, header).        |
| `WritableTableElement`           | Editable table — adds `ADD_ROW_BUTTON`, `REMOVE_ROW_BUTTON`, etc. |
| `ListElement`                    | Static or dynamic list-based UI patterns.                         |
| `Checkbox`                       | State validation and toggle logic.                                |
| `FileInputElement`               | File upload automation.                                           |
| `TextInputField`                 | Text input fields.                                                |
| `KeyValuePair`                   | Key-value display or edit pairs.                                  |
| `ReadOnlyElement`                | Non-editable / display-only elements.                             |
| `ResolvableEnum`                 | Reflection-driven enum resolution via `EnumClassRegistry`.        |

---

### 🧭 Centralized Interactions API

**`Interactions`** — core interaction class:

| Method | Purpose |
|---|---|
| `clickOn(Clickable)` | Standard click with JS fallback + stale retry. |
| `clickOn(List<ActionHandler>, Clickable, List<ActionHandler>)` | Click with full before/after hooks. |
| `clickOn(WebElement)` | Click an already-resolved element (highlight → Selenium → JS). |
| `clickOn(By)` | Click by raw locator. |
| `clickOnWithin(WebElement scope, Clickable)` | Scoped click inside a parent element. |
| `selectFromDropdown(Dropdown)` | Single-value dropdown: trigger → overlay → option. |
| `selectFromDropdown(Integer index, MultipleIdenticalDropdowns)` | Indexed three-dots menu. |
| `triggerDropdown(Dropdown)` / `triggerDropdown(MultipleIdenticalDropdowns, Integer)` | Open only. |
| `getText(ReadOnlyElement)` | Read text from a display element. |
| `getTextViaToolTip(…, ToolTipElement, …, boolean)` | Text with hover/attribute tooltip fallback. |
| `getTextByWebElement(By)` | Raw locator text read. |
| `searchFor(Searchable, String)` | Search field → first result → click. |
| `searchAndGetResults(Searchable, String)` | Search → return all result elements. |
| `performSearch(…, WebElement, String, WebElement, …)` | Low-level search flow with hooks. |

**`StepDefInteractions`** (extends `Interactions`) — BDD/step-definition layer:
- `clickOnFrom(String keySuffix, String enumName, ActionHandler after)` — resolves enum by context key then clicks.
- `resolveByContext(String enumName, String resolvedKey)` — generic enum resolution via `EnumClassRegistry`.

---

### 🧠 Debug-Oriented Logging (`CustomLogger`)

- Static logger accessed via fluent channels: `info.log(…)`, `debug.click(…)`, `warn.fallback(…)`, `error.failed(…)`, etc.
- ANSI color-coded console output with timestamps (`yyyy-MM-dd HH:mm:ss.SSS`).
- Log format: `[LEVEL] OriginClass.method <message> ← CallerClass.method`.
- Dual-channel: real-time console + full-depth persistent trace log file.
- Initialised per class via `CustomLogger.initialize(this.getClass())`.

---

### 🧰 Config & Driver Management

**`DriverFactory`** — fluent WebDriver builder supporting:
- Browsers: **Chrome**, **Firefox**, **Edge**
- Local driver or **Remote / Selenium Grid / Selenoid** (`remote=true`, `gridUrl=…`)
- Headless, mobile emulation, proxy, custom capabilities, custom binary paths.
- Page load strategies: `NORMAL`, `EAGER`, `NONE`.
- Configured entirely via `driver.properties` — no code changes required.

**`DriverContext`** — manages active driver per thread; accessed by `Interactions` and `LocatorResolverV1`.

**`UIContext`** — thread-local state holding the last resolved `WebElement` and its meta (`propertyFile`, `key`, `args`) for stale-element re-resolution.

---

### 🧱 Modular Utilities

| Utility | Description |
|---|---|
| `DOMUtils` | JS scroll-to-element, highlight, DOM manipulation. |
| `WaitUtils` | Fluent waits, Angular CDK overlay stabilisation, flicker detection. |
| `TableHandler` | Table row/cell read and navigation helpers. |
| `KeyValuePairHandler` | Key-value pair interaction utilities. |
| `Upload` | File upload support. |
| `ConfigLoader` | Hierarchical config: System → ENV → classpath → defaults. |
| `EnumResolver` | Normalised name → enum constant lookup; supports `ResolvableEnum` labels. |
| `DataVerifier` | Data-level validation with normalization and tolerance settings. |
| `DataGenerator` | Test data generation utilities. |
| `FileUtils` | File resolution, download verification, JSON parsing. |
| `JsonReader` / `JsonLogger` | JSON read/write utilities with structured logging. |

---

### 🗂️ Locator Files

Locators live under `src/main/resources/locators/` in two formats:

| Format | Example | Reader |
|---|---|---|
| `.properties` | `manage-users-elements.properties` | `PropertiesFileLocatorReaderV1` |
| `.json` | `manage-users-elements.json`, `admin-home-page-elements.json` | `JsonLocatorReaderV1` |

`LocatorResolverV1` auto-selects the format at runtime. `JsonLocatorMigrator` converts `.properties` files to JSON.

---

### 🧾 Extent Reporting

- Configured via `src/main/resources/extent.properties`.
- Integrated through `extentreports-cucumber7-adapter` + `cucumber-reporting`.
- HTML spark reports generated under `target/ExtentReports/SparkReports/`.

---

## 📂 Actual Project Structure

```
void-framework/
├── src/main/java/
│   ├── WebApplication/
│   │   └── VOID.java                        ← Framework entry point / façade
│   ├── elements/
│   │   ├── api/                             ← Element interfaces
│   │   │   ├── Element.java
│   │   │   ├── Clickable.java
│   │   │   ├── Dropdown.java
│   │   │   ├── MultipleIdenticalDropdowns.java
│   │   │   ├── Searchable.java
│   │   │   ├── SearchableDropdown.java
│   │   │   ├── SearchField.java
│   │   │   ├── ToolTipElement.java
│   │   │   ├── TableElement.java
│   │   │   ├── WritableTableElement.java
│   │   │   ├── ListElement.java
│   │   │   ├── Checkbox.java
│   │   │   ├── FileInputElement.java
│   │   │   ├── TextInputField.java
│   │   │   ├── KeyValuePair.java
│   │   │   ├── ReadOnlyElement.java
│   │   │   └── ResolvableEnum.java
│   │   ├── meta/
│   │   │   ├── ElementRole.java             ← Role enum (PRIMARY, TRIGGER, LIST, …)
│   │   │   └── EnumClassRegistry.java       ← Context key → enum class registry
│   │   └── ManageUsersElements.java         ← Example page element enum
│   ├── interactions/
│   │   ├── Interactions.java                ← Core UI interaction methods
│   │   ├── StepDefInteractions.java         ← BDD-layer interactions
│   │   └── hooks/
│   │       ├── ActionHandler.java
│   │       ├── Before.java                  ← Pre-action hooks
│   │       └── After.java                   ← Post-action hooks
│   ├── core/
│   │   ├── driver/
│   │   │   ├── DriverFactory.java
│   │   │   ├── DriverContext.java
│   │   │   └── Waiter.java
│   │   ├── logging/
│   │   │   ├── CustomLogger.java
│   │   │   └── LogConfig.java
│   │   ├── resolvers/locator/
│   │   │   ├── LocatorResolverV1.java
│   │   │   ├── LocatorReader.java
│   │   │   ├── ElementLocatorResolverV1.java
│   │   │   ├── json/
│   │   │   │   ├── JsonLocatorReaderV1.java
│   │   │   │   └── JsonLocatorMigrator.java
│   │   │   └── properties/
│   │   │       └── PropertiesFileLocatorReaderV1.java
│   │   └── utils/
│   │       ├── ConfigLoader.java
│   │       ├── EnumResolver.java
│   │       ├── UIContext.java
│   │       ├── web/
│   │       │   ├── DOMUtils.java
│   │       │   ├── WaitUtils.java
│   │       │   ├── TableHandler.java
│   │       │   ├── KeyValuePairHandler.java
│   │       │   └── Upload.java
│   │       ├── data/
│   │       │   ├── DataVerifier.java
│   │       │   └── DataGenerator.java
│   │       └── io/
│   │           ├── FileUtils.java
│   │           └── json/
│   │               ├── JsonReader.java
│   │               └── JsonLogger.java
│   └── StepDefinition/
│       ├── accountMappingStepDefPack/
│       └── CommonStepDef/
├── src/main/resources/
│   ├── config/
│   │   ├── driver.properties
│   │   └── test.properties
│   ├── locators/
│   │   ├── *.properties
│   │   └── *.json
│   ├── feature/
│   ├── fallbacks/
│   ├── extent.properties
│   └── log4j.properties
└── src/testNgXml/
    └── testng.xml
```

---

## ⚙️ Execution Flow

1. **VOID instantiation** → `DriverContext.getActiveDriver()` provides the WebDriver.
2. **Element resolution** → enum constant's `getExternalFileName()` + `getPrimaryLocator()` + `getArgs()` feed `LocatorResolverV1`.
3. **Locator lookup** → JSON reader tried first, falls back to `.properties` reader; template args substituted.
4. **Hook pipeline** → `Before.*` hooks execute (wait visible, wait clickable, highlight, Angular wait).
5. **Action execution** → Selenium action; JS fallback on failure; stale-element retry via `UIContext` meta.
6. **Hook pipeline** → `After.*` hooks execute.
7. **Logging** → `CustomLogger` emits color-coded, timestamped, call-site-traced output.

---

## 🧠 Example Usage

```java
// Instantiate framework entry point
VOID void = new VOID();

// Click using a Clickable enum
void.interaction().clickOn(ManageUsersElements.UserCards.LOGIN_AS_BUTTON);

// Click with before/after hooks
void.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);

// Select from a dropdown
void.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);

// Select from indexed three-dots menu (row 2)
void.interaction().selectFromDropdown(2, ManageUsersElements.ActionsMenu.VIEW_REGISTRATION);

// Read text from a read-only element
String name = void.interaction().getText(ManageUsersElements.UserCards.FULL_NAME);

// Read tooltip-resolved text
String email = void.interaction().getTextViaToolTip(null, ManageUsersElements.UserCards.EMAIL, null, true);

// Search and click first result
void.interaction().searchFor(CommonElements.GlobalSearch.SEARCH, "Deal Registration");

// Step-definition layer (BDD)
void.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Account Mapping");
```

---

## ⚙️ Configuration

### `driver.properties`
```properties
browser=chrome           # chrome | firefox | edge
headless=false
remote=false
gridUrl=                 # http://localhost:4444/wd/hub (when remote=true)
maximize=true
implicitWait=5
pageLoadTimeout=60
scriptTimeout=30
pageLoadStrategy=NORMAL  # NORMAL | EAGER | NONE
acceptInsecureCerts=true
args=--no-sandbox,--disable-dev-shm-usage
```

### `test.properties`
```properties
json.logger.base.path=target/logs/json
test.data.base.path=target/logs/test-data/
test.data.fallback.path=fallback/
upload.base.path=uploads/
locators.template.output.dir=locators/
```

---

## 📦 Core Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `selenium-java` | 4.38.0 | WebDriver API |
| `webdrivermanager` | 6.3.3 | Automatic driver binary management |
| `cucumber-java` / `cucumber-testng` | 7.31.0 | BDD test execution |
| `testng` | 7.10.2 | Test runner |
| `extentreports-cucumber7-adapter` | 1.14.0 | HTML reporting |
| `cucumber-reporting` | 5.10.1 | Cucumber reports |
| `log4j` | 1.2.17 | Logging backend |
| `org.json` | 20250517 | JSON parsing |
| `commons-csv` | 1.14.1 | CSV data handling |
| `mssql-jdbc` | 13.2.1.jre11 | SQL Server connectivity |

> **Java 17**, **Maven 3.x** required.

---

## 🧾 Log Output Example

```
[INFO]  2026-04-16 10:23:01.452  Interactions.clickOn  Clicked on: Login As  ← StepDefInteractions.clickOnFrom
[DEBUG] 2026-04-16 10:23:01.391  LocatorResolverV1.getLocator  Resolved JSON locator for manage-users-elements.json#LOGIN_AS_BUTTON
[WARN]  2026-04-16 10:23:02.100  Interactions.clickOn  Selenium click failed, retrying with JavaScript click...
```

---

## 🧩 Locator File Formats

**JSON** (`manage-users-elements.json`):
```json
{
  "FULL_NAME": "xpath=//div[@class='user-card']//span[@class='name']",
  "EMAIL":     "css=.user-card .email"
}
```

**Properties** (`manage-users-elements.properties`):
```properties
FULL_NAME=xpath=//div[@class='user-card']//span[@class='name']
EMAIL=css=.user-card .email
```

Prefix tokens supported by `PropertiesFileLocatorReaderV1`: `xpath=`, `css=`, `id=`, `name=`, `tag=`, `linkText=`, `partialLinkText=`.

---

## 🧠 Design Philosophy

> *VOID is not a Selenium wrapper.* It's a **scalable, object-oriented automation platform** purpose-built for clarity, extensibility, and precision debugging.
> Every line of code is designed for introspection — enabling you to see not only what failed, but **why** and **how**.

---

## 🧪 Authors & Maintainers

**VOID Framework Team**  
Maintained by: Automation Engineering Group  
Inspired by: Clean Architecture × Enum-Driven Design × Precision Debugging

---

## 📜 License

MIT License © 2025–2026 VOID Framework Project
