# VOID — Versatile Object-Oriented Interactions for DOM

## Overview

VOID is a **structured, engine-agnostic execution model** for UI automation. It ships as a Maven dependency and enforces an opinionated architecture for element definition, locator resolution, action composition, and browser execution. Built on Java 17, it combines an enum-driven element model, capability-based interfaces, role-based locator resolution, composable hook pipelines, a deferred Action/Flow execution system, and deep observability tooling into a cohesive, enterprise-ready platform.

### What VOID Is

VOID separates:
- **Intent** (Action) — what the user wants to do
- **Structure** (Element + Capability) — what UI elements exist and what roles they play
- **Execution** (UIEngine) — how the browser interaction physically happens

This enables interchangeable engines (Selenium, Playwright), composable flows, deterministic execution behavior, and clean separation of concerns.

---

## 🚀 Key Features

### 🧠 Not Just Another Selenium Wrapper
- Every UI element is modeled as a **typed enum constant** implementing fine-grained capability interfaces.
- Elements emit **deferred Action objects** (intent) — they never execute directly.
- **UIEngine** is the single execution authority — handles scroll, waits, retries, and fallback internally.
- Deep, color-coded logging with precise call-site tracing makes every failure reproducible.

### 🔧 Enum-Driven Object Model
- Elements are declared as enums implementing capability interfaces such as `Clickable`, `Selectable`, `Typeable`, or `Hoverable`.
- Nested enums (e.g., `AccountMappingElements.FilterPanel.StatusDropdown`) organize elements by page and functional context.
- Each enum constant carries its own locator key, external file reference, dynamic args, and display text.

### 📍 Role-Based Locator Resolution (`LocatorResolvers`)
- All locator lookups go through `LocatorResolvers` (`strict()` for clean role-keyed JSON, `legacyPadded()` for legacy properties files), which dispatch to **JSON** or **`.properties`** readers via a `LocatorRequest`.
- Locator roles are typed via `ElementRole` enum — `PRIMARY`, `SECONDARY`, `TRIGGER`, `LIST`, `SEARCH_INPUT`, `SEARCH_RESULT`, `TOOLTIP_CONTENT`, `TABLE`, `ROW`, `CELL`, `MULTI_TRIGGER`, etc.
- Dynamic `%s` substitutions in locator templates are applied at resolve time.
- Resolution produces `LocatorDescriptor` objects — engine-agnostic, not tied to Selenium `By`.

### 🪝 Composable Before / After Action Hooks
- `Before` and `After` constant libraries provide pre-built `ActionHandler` instances:
  `WAIT_FOR_ELEMENT_VISIBLE`, `WAIT_FOR_ELEMENT_CLICKABLE`, `HIGHLIGHT_ELEMENT`, `WAIT_FOR_ANGULAR_LOADER`, `LOG_INTENT`, `DO_NOTHING`, etc.
- Hook lists are passed directly to every interaction method overload for fully composable behaviour.

### ⚡ Action / Flow / Runner Pipeline
- Capability interfaces emit **deferred `Action` objects** — lambdas over `UIEngine`.
- `Flow` composes multiple Actions into ordered sequences.
- `Runner` iterates Flows and calls `action.perform(engine)` for each.
- Locator resolution happens **inside** the Action lambda at execution time — never eagerly.

### 🧩 Capability-Based Element Model

| Interface | Package | Description |
|-----------|---------|-------------|
| `Element` | `elements.api` | Root contract — locator keys, args, display text, role map. |
| `Clickable` | `elements.api.capability` | Clickable UI components (buttons, links). Emits `click()`. |
| `Typeable` | `elements.api.capability` | Text input fields. Emits `type()`, `clear()`, `append()`. |
| `Selectable` | `elements.api.capability` | Trigger + list locators for single-value dropdowns. Emits `open()`, `select()`. |
| `MultiSelectable` | `elements.api.capability` | Repeated dropdown patterns (indexed). Emits `open()`, `selectAtIndex()`. |
| `Searchable` | `elements.api.capability` | Search input + result list locators. |
| `SearchableDropdown` | `elements.api.capability` | Searchable dropdown (trigger + input + results). Emits `searchAndSelect()`. |
| `SearchField` | `elements.api.capability` | Standalone search-field (input + button). Emits `typeSearch()`, `submitSearch()`. |
| `Hoverable` | `elements.api.capability` | Hover tooltip with attribute fallback. Emits `hover()`. |
| `Table` | `elements.api.capability` | Read-only structured table (rows, columns, cells, header). |
| `EditableTable` | `elements.api.capability` | Editable table — adds add/remove row buttons. Emits `clickAddRow()`. |
| `Listable` | `elements.api.capability` | Static or dynamic list-based UI patterns. |
| `Checkable` | `elements.api.capability` | Checkbox toggle logic. Emits `toggle()`, `set(boolean)`. |
| `Uploadable` | `elements.api.capability` | File upload automation. Emits `upload(path)`. |
| `ReadOnly` | `elements.api.capability` | Non-editable / display-only elements. Emits `getText()`. |
| `KeyValuePair` | `elements.api` | Key-value display or edit pairs. |
| `ResolvableEnum` | `core.utils` | Mixin for name↔label enum resolution. Not a locator interface. |

---

### 🧭 Execution Architecture

**Primary path (new code):**

```
Element → Action (intent) → Flow (composition) → Runner (iteration) → UIEngine (execution)
```

**Legacy path (backward compat):**

```
Element → Interactions (frozen orchestrator) → UIEngine (execution)
```

**`Interactions`** is a **frozen legacy orchestrator** — preserved for backward compatibility with existing step definitions and page objects. No new features should be added there. New development should use the Action/Flow/Runner pipeline.

**`UIEngine`** is the single execution authority:
- Resolves `LocatorDescriptor` to native locators
- Handles scroll, waits, retries, and fallback internally
- Each engine (Selenium, Playwright) provides its own implementation
- Callers must NOT perform scroll, waits, or direct execution

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

**`DriverContext`** — manages active driver per thread; accessed by engine and resolvers.

**`UIContext`** — thread-local state holding the last resolved `LocatorDescriptor` and meta for stale-element re-resolution and diagnostics.

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
| `.json` | `manage-users-elements.json` | `JsonLocatorReader` |

`LocatorResolvers` auto-selects the format at runtime. `JsonMigratorCli` converts `.properties` files to JSON.

---


## 📂 Project Structure

```
void-framework/
├── src/main/java/
│   ├── core/
│   │   ├── actions/
│   │   │   └── Action.java                   ← Deferred execution intent (functional interface)
│   │   ├── flow/
│   │   │   └── Flow.java                     ← Composes Actions into sequences
│   │   ├── runner/
│   │   │   └── Runner.java                   ← Iterates Flow, calls action.perform(engine)
│   │   ├── engine/
│   │   │   ├── UIEngine.java                 ← Execution contract (interface)
│   │   │   ├── EngineConfig.java             ← Engine configuration
│   │   │   ├── LocatorDescriptor.java        ← Engine-agnostic locator descriptor
│   │   │   └── selenium/
│   │   │       └── SeleniumEngine.java       ← Selenium implementation of UIEngine
│   │   ├── bootstrap/
│   │   │   └── VOID.java                     ← Framework entry point / façade
│   │   ├── interactions/
│   │   │   ├── Interactions.java             ← Legacy orchestrator (frozen, deprecated)
│   │   │   ├── Via.java                      ← Static casting / locator helpers
│   │   │   ├── UIContext.java                ← Thread-local descriptor state
│   │   │   └── hooks/
│   │   │       ├── ActionHandler.java
│   │   │       ├── Before.java               ← Pre-action hooks
│   │   │       └── After.java                ← Post-action hooks
│   │   ├── driver/
│   │   │   ├── DriverFactory.java
│   │   │   ├── DriverContext.java
│   │   │   └── Waiter.java
│   │   ├── logging/
│   │   │   ├── CustomLogger.java             ← Public facade
│   │   │   ├── ansi/                         ← ANSI color support
│   │   │   ├── config/                       ← Log configuration
│   │   │   ├── intent/                       ← Log intent types
│   │   │   ├── render/                       ← Log rendering
│   │   │   └── theme/                        ← Color themes
│   │   ├── resolvers/locator/
│   │   │   ├── api/                          ← LocatorResolvers, LocatorResolver, LocatorRequest
│   │   │   ├── source/                       ← JSON, Properties, Hardcoded sources
│   │   │   ├── parser/                       ← ByParser, prefix strategies
│   │   │   ├── template/                     ← LocatorTemplate substitution
│   │   │   └── json/                         ← JSON migration tools
│   │   ├── runtime/                          ← Runtime lifecycle
│   │   └── utils/
│   │       ├── ConfigLoader.java
│   │       ├── EnumResolver.java
│   │       ├── ResolvableEnum.java
│   │       ├── web/                          ← DOMUtils, WaitUtils, TableHandler, Upload
│   │       ├── data/                         ← DataVerifier, DataGenerator
│   │       └── io/                           ← FileUtils, JSON/Properties readers
│   ├── dsl/
│   │   └── VoidDSL.java                      ← BDD/context-driven DSL layer
│   ├── elements/
│   │   ├── api/
│   │   │   ├── Element.java                  ← Root element contract
│   │   │   ├── KeyValuePair.java
│   │   │   └── capability/                   ← All capability interfaces
│   │   │       ├── Clickable.java
│   │   │       ├── Typeable.java
│   │   │       ├── Selectable.java
│   │   │       ├── Checkable.java
│   │   │       ├── Hoverable.java
│   │   │       ├── Searchable.java
│   │   │       ├── SearchField.java
│   │   │       ├── SearchableDropdown.java
│   │   │       ├── ReadOnly.java
│   │   │       ├── Uploadable.java
│   │   │       ├── Listable.java
│   │   │       ├── MultiSelectable.java
│   │   │       ├── Table.java
│   │   │       └── EditableTable.java
│   │   ├── meta/
│   │   │   ├── ElementRole.java
│   │   │   └── EnumClassRegistry.java
│   │   └── exapmlepages/                     ← Example page element enums
│   └── StepDefinition/
├── src/main/resources/
│   ├── locators/
│   │   ├── properties/                       ← *.properties locator files
│   │   └── json/                             ← *.json locator files
│   └── log4j2.xml
└── src/testNgXml/
    └── testng.xml
```

---

## ⚙️ Execution Flow

### Primary Path: Action / Flow / Runner

```
1. Page enum        → element.click() / element.type("text")     ← returns Action (deferred)
2. Flow             → Flow.of(action1, action2, action3)         ← groups Actions
3. Runner           → runner.run(flow)                            ← iterates
4. Action lambda    → engine.resolve(this, ROLE)                 ← resolves LocatorDescriptor
5. UIEngine         → engine.click(descriptor)                   ← executes (scroll, wait, retry internal)
6. Logging          → CustomLogger emits color-coded output
```

### Legacy Path: Interactions (Frozen)

```
1. VOID instantiation → UIEngine created, wrapped in Interactions
2. Element resolution → LocatorResolvers resolve LocatorDescriptor
3. Hook pipeline      → Before.* hooks execute
4. Action execution   → Interactions delegates to engine.click(descriptor) / engine.type(descriptor, text)
5. Hook pipeline      → After.* hooks execute
6. Logging            → CustomLogger emits the action record
```

---

## 🧠 Example Usage

### Modern: Action / Flow / Runner

```java
import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import core.flow.Flow;
import core.runner.Runner;

// Define elements
enum LoginField implements Typeable {
    USERNAME("USERNAME_INPUT"), PASSWORD("PASSWORD_INPUT");
    private final String key; LoginField(String k) { this.key = k; }
    @Override public String getExternalFileName() { return "login.properties"; }
    @Override public String getInputLocator()     { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}

enum LoginButton implements Clickable {
    SUBMIT("SIGN_IN_BTN");
    private final String key; LoginButton(String k) { this.key = k; }
    @Override public String getExternalFileName() { return "login.properties"; }
    @Override public String getTriggerLocator()   { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}

// Execute
Runner runner = new Runner(engine);
runner.run(Flow.of(
    LoginField.USERNAME.type("admin@example.com"),
    LoginField.PASSWORD.type("secret"),
    LoginButton.SUBMIT.click()
));
```

### Legacy: Interactions (Backward Compat)

```java
VOID app = new VOID();
app.interaction().typeInto(LoginPageElements.Credentials.USERNAME_INPUT, "admin@example.com");
app.interaction().typeInto(LoginPageElements.Credentials.PASSWORD_INPUT, "secret");
app.interaction().clickOn(LoginPageElements.Actions.SIGN_IN_BUTTON);
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
| `selenium-java` | 4.38.0 | WebDriver API (SeleniumEngine) |
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
2026-04-24 13:15:37.672 │ DEBUG │ [LOCATOR] Resolving:          │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ File        : test-locators.properties
2026-04-24 13:15:37.674 │ DEBUG │           ├─ Key         : TEMPLATE_WITH_ARG
2026-04-24 13:15:37.674 │ DEBUG │           └─ Args        : [username]
2026-04-24 13:15:37.679 │ DEBUG │ [LOCATOR] Final:
2026-04-24 13:15:37.679 │ DEBUG │           ├─ Resolved    : //input[@placeholder='username']
2026-04-24 13:15:37.679 │ DEBUG │           └─ By          : By.xpath: //input[@placeholder='username']
```

---

## 🧩 Locator File Formats

**JSON** (recommended):
```json
{
  "FULL_NAME": "xpath=//div[@class='user-card']//span[@class='name']",
  "EMAIL":     "css=.user-card .email"
}
```

**Properties**:
```properties
FULL_NAME=xpath=//div[@class='user-card']//span[@class='name']
EMAIL=css=.user-card .email
```

Prefix tokens: `xpath=`, `css=`, `id=`, `name=`, `tag=`, `linkText=`, `partialLinkText=`.

---

## 🧠 Design Philosophy

> *VOID is a structured, engine-agnostic execution model for UI automation.*
> It separates intent (Action), structure (Element), and execution (UIEngine).
> Every line of code is designed for introspection — enabling you to see not only what failed, but **why** and **how**.

### Architecture Invariants

- **Elements NEVER execute** — they emit Action (intent) only
- **Actions NEVER perform work** until executed by UIEngine via Runner
- **UIEngine owns ALL execution concerns** — scroll, waits, retries, fallback
- **One execution path**: Element → Action → Flow → Runner → UIEngine
- **No compile-time code generation** — all behavior is visible and debuggable

---

## 🏷️ Stability Tiers

VOID uses explicit stability tiers to control how the architecture evolves. Each API surface has a defined tier that determines what guarantees consumers get.

| Tier | Scope | Guarantees | Rules |
|------|-------|------------|-------|
| **Stable (frozen)** | `Interactions`, hook system (`Before`, `After`, `ActionHandler`) | No breaking changes. No new features. Behavior will remain unchanged. | Will not evolve further. |
| **Stable (user-facing)** | Capability interfaces (`Clickable`, `Typeable`, etc.), `Element`, `UIEngine` | No breaking changes. May gain new methods. | Backward-compatible evolution only. |
| **Beta** | `Action`, `Flow`, `Runner`, Action-emitting default methods on capability interfaces | May change without notice between releases. | Must not be used inside stable modules. |

### Usage Rules

1. **Beta APIs must not be used inside stable modules.**
2. **Stable APIs may depend on stable APIs only.**
3. **Beta APIs may change, be renamed, or be removed in any release.**
4. **Capability interfaces are stable contracts** — they define structure. The `@Beta` Action-emitting methods they carry are intentionally beta: callers consume Actions opaquely, so beta internals don't leak into test-level code.

### Annotations

| Annotation | Meaning |
|---|---|
| `@Beta(since = "2.0")` | API is evolving — do not depend on from stable modules |
| `@Deprecated(since = "2.0", forRemoval = false)` | Stable but frozen — no new features, no removal planned |
| *(no annotation)* | Stable — normal backward-compatibility guarantees apply |

---

## 🧪 Author

**VOID**
A personal project.
Inspired by: Clean Architecture × Enum-Driven Design × Precision Debugging

---

## 📜 License

MIT License © 2025–2026 VOID Project
