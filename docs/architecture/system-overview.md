# VOID — Versatile Object-Oriented Interactions for DOM

## Overview

VOID (Versatile Object-Oriented Interactions for DOM) is a **structured Selenium automation system**. It ships as a Maven dependency, but internally enforces an opinionated model for element definition, locator resolution, action execution, and failure reporting. Built on Java 17, it combines an enum-driven element model, role-based locator resolution, composable hook pipelines, and deep observability tooling into a cohesive, enterprise-ready platform.

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

### 📍 Role-Based Locator Resolution (`LocatorResolvers`)
- All locator lookups go through `LocatorResolvers` (`strict()` for clean role-keyed JSON, `legacyPadded()` for legacy properties files), which dispatch to **JSON** or **`.properties`** readers via a `LocatorRequest`.
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
| `ResolvableEnum`                 | Mixin for name↔label enum resolution (lives in `core.utils`, not `elements.api`). |

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
- Powered by **Log4j 2**.
- Initialised per class via `CustomLogger.initialize(this.getClass())`.

---

### 🧰 Config & Driver Management

**`DriverFactory`** — fluent WebDriver builder supporting:
- Browsers: **Chrome**, **Firefox**, **Edge**
- Local driver or **Remote / Selenium Grid / Selenoid** (`remote=true`, `gridUrl=…`)
- Headless, mobile emulation, proxy, custom capabilities, custom binary paths.
- Page load strategies: `NORMAL`, `EAGER`, `NONE`.
- Configured entirely via `driver.properties` — no code changes required.

**`DriverContext`** — manages active driver per thread; accessed by `Interactions` and `LocatorResolvers`.

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
| `.properties` | `manage-users-elements.properties` | `PropertiesFileLocatorReader` |
| `.json` | `manage-users-elements.json`, `admin-home-page-elements.json` | `JsonLocatorReader` |

`LocatorResolvers` auto-selects the format at runtime. `JsonMigratorCli` converts `.properties` files to JSON.

---


## 📂 Actual Project Structure

```
void-framework/
├── src/main/java/
│   ├── WebApplication/
│   │   └── VOID.java                        ← Framework entry point / façade
│   ├── automation/
│   │   ├── interactions/
│   │   │   └── StepDefInteractions.java      ← BDD-layer interactions
│   │   └── WebApplication/
│   │       └── AutomationVOID.java           ← Extends VOID with stepDefInteraction()
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
│   │   │   └── ReadOnlyElement.java
│   │   ├── meta/
│   │   │   ├── ElementRole.java             ← Role enum (PRIMARY, TRIGGER, LIST, …)
│   │   │   └── EnumClassRegistry.java       ← Context key → enum class registry
│   │   └── exapmlepages/                    ← Example page element enums
│   ├── interactions/
│   │   ├── Interactions.java                ← Core UI interaction methods
│   │   ├── Via.java                         ← Static casting / locator / WebElement helpers
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
│   │   │   ├── CustomLogger.java            ← Public facade
│   │   │   ├── ConsoleOnly.java             ← @ConsoleOnly annotation
│   │   │   ├── ansi/
│   │   │   │   ├── AnsiEscape.java
│   │   │   │   ├── AnsiColors.java
│   │   │   │   └── AnsiColorMatrix.java
│   │   │   ├── config/
│   │   │   │   ├── LogConfig.java
│   │   │   │   └── LoggerContext.java
│   │   │   ├── intent/
│   │   │   │   └── LogIntent.java
│   │   │   ├── render/
│   │   │   │   └── LogActions.java
│   │   │   └── theme/
│   │   │       ├── LogTheme.java
│   │   │       ├── ThemeColors.java
│   │   │       └── BuiltInThemes.java
│   │   ├── resolvers/locator/
│   │   │   ├── api/
│   │   │   │   ├── LocatorResolvers.java    ← strict() + legacyPadded() factories
│   │   │   │   ├── LocatorResolver.java     ← resolver interface
│   │   │   │   ├── LocatorRequest.java      ← (file, key, args) value object
│   │   │   │   └── LocatorPaths.java        ← base path constants
│   │   │   ├── source/
│   │   │   │   ├── LocatorSource.java       ← reader contract
│   │   │   │   ├── LocatorSourceRegistry.java
│   │   │   │   ├── JsonLocatorSource.java
│   │   │   │   ├── PropertiesLocatorSource.java
│   │   │   │   ├── LayeredPropertiesLocatorSource.java
│   │   │   │   └── HardcodedLocatorSource.java
│   │   │   ├── parser/
│   │   │   │   ├── ByParser.java
│   │   │   │   └── ByPrefixStrategy.java
│   │   │   ├── template/
│   │   │   │   └── LocatorTemplate.java
│   │   │   ├── json/
│   │   │   │   ├── JsonLocatorReader.java
│   │   │   │   ├── JsonNodeLookup.java
│   │   │   │   ├── JsonLocatorMigrator.java
│   │   │   │   ├── JsonTreeBuilder.java
│   │   │   │   ├── EnumLocatorScanner.java
│   │   │   │   ├── PropertiesIndex.java
│   │   │   │   └── JsonMigratorCli.java     ← .properties → .json migrator CLI
│   │   │   └── properties/
│   │   │       └── PropertiesFileLocatorReader.java
│   │   └── utils/
│   │       ├── ConfigLoader.java
│   │       ├── EnumResolver.java
│   │       ├── ResolvableEnum.java          ← Mixin for name↔label enum resolution
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
│   │           ├── properties/
│   │           │   └── PropertiesReader.java
│   │           └── json/
│   │               ├── JsonReader.java
│   │               └── JsonLogger.java
│   └── StepDefinition/
│       └── package-info.java                 ← Optional Cucumber adapter layer
├── src/main/resources/
│   ├── config/
│   │   ├── driver.properties
│   │   └── test.properties
│   ├── locators/
│   │   ├── properties/                      ← *.properties locator files
│   │   └── json/                            ← *.json locator files
│   ├── feature/
│   ├── fallbacks/
│   └── log4j2.xml
└── src/testNgXml/
    └── testng.xml
```

---

## ⚙️ Execution Flow

1. **VOID instantiation** → `DriverContext.getActiveDriver()` provides the WebDriver.
2. **Element resolution** → enum constant's `getExternalFileName()` + `getPrimaryLocator()` + `getArgs()` feed `LocatorResolvers.strict()` (or `legacyPadded()`) via a `LocatorRequest`.
3. **Locator lookup** → `LocatorSourceRegistry` dispatches to JSON or `.properties` readers; template args substituted.
4. **Hook pipeline** → `Before.*` hooks execute (wait visible, wait clickable, highlight, Angular wait).
5. **Action execution** → Selenium action; JS fallback on failure; stale-element retry via `UIContext` meta.
6. **Hook pipeline** → `After.*` hooks execute.
7. **Logging** → `CustomLogger` emits color-coded, timestamped, call-site-traced output.

---

## 🧠 Example Usage

```java
// Instantiate framework entry point
VOID app = new VOID();

// Click using a Clickable enum
app.interaction().clickOn(ManageUsersElements.UserCards.LOGIN_AS_BUTTON);

// Click with before/after hooks
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);

// Select from a dropdown
app.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);

// Select from indexed three-dots menu (row 2)
app.interaction().selectFromDropdown(2, ManageUsersElements.ActionsMenu.VIEW_REGISTRATION);

// Read text from a read-only element
String name = app.interaction().getText(ManageUsersElements.UserCards.FULL_NAME);

// Read tooltip-resolved text
String email = app.interaction().getTextViaToolTip(null, ManageUsersElements.UserCards.EMAIL, null, true);

// Search and click first result
app.interaction().searchFor(CommonElements.GlobalSearch.SEARCH, "Deal Registration");

// Step-definition layer (BDD)
AutomationVOID bddApp = new AutomationVOID();
bddApp.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Account Mapping");
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
| `cucumber-java` / `cucumber-testng` | 7.31.0 | BDD test execution (optional) |
| `testng` | 7.11.0 | Test runner |
| `jackson-databind` | 2.19.0 (managed via BOM) | JSON parsing |
| `log4j-api` / `log4j-core` | 2.25.4 | Logging (Log4j 2) |
| `datafaker` | 2.4.2 | Test data generation |
| `jsr305` | 3.0.2 | `@Nullable` / `@Nonnull` annotations |

> **Java 17**, **Maven 3.x** required.

---

## 🧾 Log Output Example

```
2026-04-24 13:15:37.584 │ INFO │ === InteractionsEndToEndTest starting === │ InteractionsEndToEndTest.setupClass ← TestMethodWorker.run
2026-04-24 13:15:37.663 │ DEBUG │ Setting driver for key: primary │ DriverContext.setPrimaryDriver ← Interactions.(constructor)
2026-04-24 13:15:37.668 │ DEBUG │ [get] key=locator.properties.base.path src=DEFAULT val=locators/properties/ │ ConfigLoader.get ← LocatorPaths.(static init)
2026-04-24 13:15:37.668 │ DEBUG │ [get] key=locator.json.base.path src=DEFAULT val=locators/json/ │ ConfigLoader.get ← LocatorPaths.(static init)
2026-04-24 13:15:37.672 │ DEBUG │ [LOCATOR] Resolving: │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ File        : test-locators.properties │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ Key         : TEMPLATE_WITH_ARG │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           ├─ Args        : [username] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           └─ Hardcoded   : false │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.678 │ DEBUG │ [LOCATOR] Final: │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           ├─ Key         : TEMPLATE_WITH_ARG │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           ├─ Resolved    : //input[@placeholder='username'] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           └─ By          : By.xpath: //input[@placeholder='username'] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.685 │ DEBUG │ Getting driver for key: primary │ DriverContext.getDriver ← DOMUtils.scrollToElement
2026-04-24 13:15:37.688 │ TEXT [T] │ Appended to 'username': -extra │ Interactions.appendTo ← InteractionsEndToEndTest.interactions_appendTo_doesNotClearButTypes
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

Prefix tokens supported by `PropertiesFileLocatorReader`: `xpath=`, `css=`, `id=`, `name=`, `tag=`, `linkText=`, `partialLinkText=`.

---

## 🧠 Design Philosophy

> *VOID is not a Selenium wrapper. It's not a framework. And it's not an optional toolkit.*
> It's a structured automation system — consumed as a dependency, but opinionated about how elements are modeled, how locators are resolved, and how actions execute.
> Every line of code is designed for introspection — enabling you to see not only what failed, but **why** and **how**.

### No Compile-Time Code Generation

VOID deliberately avoids compile-time code-generation tools (e.g., Lombok, AutoValue).
All constructors, getters, builders, and utility methods are written explicitly in the source.
This guarantees that every behavior is visible, traceable through a debugger, and fully
controlled within the codebase — with no hidden transformations between the code you read
and the code that runs.

---

## 🧪 Author

**VOID**  
A personal project.  
Inspired by: Clean Architecture × Enum-Driven Design × Precision Debugging

---

## 📜 License

MIT License © 2025–2026 VOID Project
