# VOID — Virtual Object Interaction Domain

## Overview

VOID is an **interaction runtime for modeling and executing interaction workflows**, currently configured for UI automation. It ships as a Maven dependency and enforces an opinionated architecture for element definition, locator resolution, action composition, and browser execution. Built on Java 17, it combines an enum-driven element model, capability-based interfaces, role-based locator resolution, composable hook pipelines, a deferred Action/Flow execution system, and deep observability tooling into a cohesive, enterprise-ready platform.

### What VOID Is

VOID separates interaction modeling from execution:
- **Intent** (Action) — what should happen
- **Structure** (UIElement + Capability) — what UI elements exist and what roles they play
- **Execution** (FlowExecutor + UIEngine) — how interactions physically happen

Elements emit actions. Actions compose flows. Flows are executed by the VOID Runtime through interchangeable engines that own waits, retries, locator resolution, synchronization, and native automation concerns.

Test code describes intent. The runtime handles execution.

This enables interchangeable engines (Selenium today, Playwright-ready by contract), composable flows, deterministic execution behavior, and clean separation of concerns.

---

## Decision Traceability

Architecture in this document is projected from accepted decisions under `docs/decisions/accepted/`:

- [001 - Remove WebDriverManager](../decisions/accepted/001-remove-webdrivermanager.md)
- [002 - Cucumber as Optional Dependency](../decisions/accepted/002-cucumber-optional-dependency.md)
- [003 - No Compile-Time Code Generation](../decisions/accepted/003-no-lombok-no-codegen.md)
- [004 - Dependency Philosophy](../decisions/accepted/004-dependency-philosophy.md)
- [005 - Logging Architecture](../decisions/accepted/005-logging-architecture.md)
- [006 - Replace JavaFaker with Datafaker](../decisions/accepted/006-replace-javafaker-with-datafaker.md)
- [007 - UIEngine as Single Execution Authority](../decisions/accepted/007-uiengine-execution-authority.md)
- [008 - Capability Interfaces](../decisions/accepted/008-capability-interfaces.md)
- [009 - Action / Flow / FlowExecutor Execution Model](../decisions/accepted/009-action-flow-runner.md)
- [010 - Hook Evolution](../decisions/accepted/010-hook-evolution.md)
- [011 - VOID as Primary Session Façade](../decisions/accepted/011-void-facade-boundary.md)
- [012 - ElementActions Factory Scope](../decisions/accepted/012-elementactions-factory-scope.md)
- [013 - Architectural Layering Principle](../decisions/accepted/013-architectural-layering-principle.md)
- [014 - Concrete Actions over Anonymous Lambdas](../decisions/accepted/014-concrete-actions-over-lambdas.md)
- [015 - Capability Self-Description via ActionCapabilityProvider](../decisions/accepted/015-capability-self-description.md)
- [016 - capability() Ownership Migration](../decisions/accepted/016-capability-ownership-migration.md)
- [017 - ElementSupport and LocatorRoles Utility Scope](../decisions/accepted/017-element-support-locator-roles.md)
- [018 - Engine Lifecycle Ownership](../decisions/accepted/018-engine-lifecycle-ownership.md)
- [019 - LocatorDescriptor as Engine-Agnostic Locator](../decisions/accepted/019-locator-descriptor.md)
- [020 - Target as Domain-Neutral Root](../decisions/accepted/020-target-neutral-root.md)
- [021 - Runtime Redesign: Kernel Boundary, Ontology, Open Decisions](../decisions/accepted/021-runtime-redesign-kernel-boundary.md)
- [022 - Session Model: Identity, Neutral Bootstrap, and Unified Execution](../decisions/accepted/022-session-model.md)
- [023 - Locator Generalization: Open LocatorStrategy Interface](../decisions/accepted/023-locator-generalization.md)
- [024 - Domain Registration: Web-Domain Package Structure](../decisions/accepted/024-domain-registration.md)

---

## 🚀 Key Features

### 🧠 Not Just Another Driver Wrapper
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
- `ActionHandler` receives `(UIEngine engine, LocatorDescriptor descriptor)` — fully engine-agnostic.
- Hook lists are passed directly to every interaction method overload for fully composable behaviour.
- **Fluent API:** Actions created via capability interfaces support `.before(...).after(...)` for inline hook composition.

### ⚡ Action / Flow / FlowExecutor Pipeline
- Capability interfaces emit **typed concrete `Action` subclasses** — `ClickAction`, `TypeAction`, `SelectAction`, etc.
- Each concrete action type owns its execution logic; profile defaults are centralized in the family's abstract intermediary — `ClickableElementAction`, `TypeableElementAction`, or `SelectableElementAction` — eliminating profile boilerplate across 14 concrete classes (see ADR-014).
- `Flow` composes multiple Actions into ordered sequences.
- `FlowExecutor` iterates Flows and calls `action.perform(engine)` for each.
- Locator resolution happens **inside** `perform()` at execution time — never eagerly.
- `HookChainAction` decorates an Action with before/after hooks, sharing a single resolved descriptor.
- `ElementActions` (`@Internal`) provides a custom-operation factory for test infrastructure only (see ADR-012).

### 🧩 Capability-Based UIElement Model

| Interface | Package | Description |
|-----------|---------|-------------|
| `Target` | `core.target` | Domain-neutral root -- display text, args, effective-args. |
| `UIElement` | `domain.automation.web.vocabulary.element` | Web-domain root contract, extends `Target` -- locator keys, external file, role map. |
| `Clickable` | `domain.automation.web.vocabulary.capability` | Clickable UI components (buttons, links). Emits `click()`. |
| `Typeable` | `domain.automation.web.vocabulary.capability` | Text input fields. Emits `type()`, `clear()`, `append()`. |
| `Selectable` | `domain.automation.web.vocabulary.capability` | Trigger + list locators for single-value dropdowns. Emits `open()`, `select()`. |
| `MultiSelectable` | `domain.automation.web.vocabulary.capability` | Repeated dropdown patterns (indexed). Emits `open()`, `selectAtIndex()`. |
| `Searchable` | `domain.automation.web.vocabulary.capability` | Search input + result list locators. |
| `SearchableDropdown` | `domain.automation.web.vocabulary.capability` | Searchable dropdown (trigger + input + results). Emits `searchAndSelect()`. |
| `SearchField` | `domain.automation.web.vocabulary.capability` | Standalone search-field (input + button). Emits `typeSearch()`, `submitSearch()`. |
| `Hoverable` | `domain.automation.web.vocabulary.capability` | Hover tooltip with attribute fallback. Emits `hover()`. |
| `Table` | `domain.automation.web.vocabulary.capability` | Read-only structured table (rows, columns, cells, header). |
| `EditableTable` | `domain.automation.web.vocabulary.capability` | Editable table -- adds add/remove row buttons. Emits `clickAddRow()`. |
| `Listable` | `domain.automation.web.vocabulary.capability` | Static or dynamic list-based UI patterns. |
| `Checkable` | `domain.automation.web.vocabulary.capability` | Checkbox toggle logic. Emits `toggle()`, `set(boolean)`. |
| `Uploadable` | `domain.automation.web.vocabulary.capability` | File upload automation. Emits `upload(path)`. |
| `ReadOnly` | `domain.automation.web.vocabulary.capability` | Non-editable / display-only elements. Emits `readText()`. |
| `KeyValuePair` | `domain.automation.web.vocabulary.element` | Key-value display or edit pairs. |
| `ResolvableEnum` | `core.utils` | Mixin for name-label enum resolution. Not a locator interface. |

---

### 🧭 Execution Architecture

**Primary path (new code):**

```
VOID (session) → FlowExecutor → UIEngine
     ↑
UIElement → Action (intent) → Flow (composition)
```

Full expansion:

```
UIElement → Action → Flow → VOID.run() → FlowExecutor → UIEngine
```

**Legacy path (backward compat):**

```
UIElement → Interactions (frozen orchestrator) → UIEngine (execution)
```

**`VOID`** is the primary test-facing session object:
- `VOID.navigateTo(url)` — session-level navigation
- `VOID.getCurrentUrl()` / `VOID.getTitle()` / `VOID.refresh()` — session-level queries
- `VOID.run(Flow)` / `VOID.run(Action)` — execution via internal FlowExecutor
- `VOID.getEngine()` — advanced escape hatch (most examples never need this)
- `VOID.shutdown()` — session-scoped teardown (only this session's browser is closed)

**`Interactions`** is a **frozen legacy orchestrator** — preserved for backward compatibility with existing step definitions and page objects. No new features should be added there. New development uses the VOID session façade.

**`UIEngine`** is the single execution authority:
- Resolves `LocatorDescriptor` to native locators
- Handles scroll, waits, retries, and fallback internally
- Each engine (Selenium, Playwright) provides its own implementation
- Callers must NOT perform scroll, waits, or direct execution
- Advanced operations: `switchToFrame`, `switchToDefaultContent`, `sendKeys` (global key dispatch), `executeScript`, `hover`, `scrollTo`, `highlight`, `uploadFile`

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

**`UIContext`** — *(deprecated)* thread-local state holding the last resolved `LocatorDescriptor`. In the modern path, hooks receive descriptors directly as parameters. Retained only for legacy `Interactions` compatibility.

---

### 🧱 Modular Utilities

| Utility | Description |
|---|---|
| `DOMUtils` | *(deprecated -- use `UIEngine.scrollTo`, `UIEngine.hover`, `UIEngine.switchToFrame`, `UIEngine.switchToDefaultContent`, `UIEngine.sendKeys`)* |
| `WaitUtils` | Fluent waits and loader stabilisation. By-based public API deprecated -- use `UIEngine.waitForVisible`, `UIEngine.waitForAbsence`, `UIEngine.waitForClickable`, `UIEngine.waitForOverlay`. Internal condition-wait utilities remain. |
| `TableHandler` | *(deprecated -- no UIEngine table-read API yet; deferred until active callers drive the design)* |
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

> For detailed documentation on each `core/` sub-package, see [Core Package Architecture](core-packages.md).
> For the element and capability layer, see [UIElement Layer Architecture](elements.md).

```
void-framework/
├── src/main/java/
│   ├── core/
│   │   ├── actions/
│   │   │   ├── Action.java                    ← Deferred execution intent (functional interface)
│   │   │   ├── ElementAction.java             ← Abstract base (Template Method): resolve → execute
│   │   │   ├── ClickableElementAction.java    ← Abstract (pkg-private): CLICKABLE profile defaults for 3 click-family classes
│   │   │   ├── TypeableElementAction.java     ← Abstract (pkg-private): TYPEABLE profile defaults for 6 type-family classes
│   │   │   ├── SelectableElementAction.java   ← Abstract (pkg-private): SELECTABLE profile defaults for 5 select-family classes
│   │   │   ├── ClickAction.java               ← Concrete: engine.click(), TRIGGER role
│   │   │   ├── TypeAction.java                ← Concrete: engine.type(), INPUT role
│   │   │   ├── SelectAction.java              ← Concrete: composite TRIGGER + LIST
│   │   │   ├── HoverAction.java               ← Concrete: engine.hover(), TEXT role
│   │   │   ├── ReadTextAction.java            ← Concrete: engine.getText(), TEXT role
│   │   │   ├── ...                            ← 11 further concrete action subclasses
│   │   │   ├── ElementActions.java            ← @Internal factory (test infrastructure only)
│   │   │   ├── ActionProfiles.java            ← Package-private: 8 capability-specific safe/reliable profile constants
│   │   │   ├── Profiles.java                  ← Action-independent presets (RAW, DEBUG, FAST, VISUAL)
│   │   │   └── HookedAction.java              ← Pure decorator: before → action → after
│   │   ├── adapters/
│   │   │   └── cucumber/                      ← BDD step definitions (optional)
│   │   ├── annotations/
│   │   │   ├── Beta.java                      ← @Beta stability marker
│   │   │   └── Internal.java                  ← @Internal stability marker
│   │   ├── bootstrap/
│   │   │   └── FrameworkBootstrap.java        ← One-time init gate
│   │   ├── context/
│   │   │   ├── ExecutionContext.java          ← Immutable per-session context (WebDriver)
│   │   │   └── SessionContext.java            ← Engine-agnostic session context
│   │   ├── flow/
│   │   │   └── Flow.java                     ← Composes Actions into sequences
│   │   ├── executor/
│   │   │   └── FlowExecutor.java              ← Iterates Flow, calls action.perform(engine)
│   │   ├── bridge/
│   │   │   └── selenium/
│   │   │       └── SeleniumLocatorBridge.java ← *(deprecated)* By → LocatorDescriptor adapter
│   │   ├── engine/
│   │   │   ├── UIEngine.java                 ← Execution contract (interface)
│   │   │   ├── EngineBootstrap.java          ← Engine startup parameter (replaces WebDriver factory param)
│   │   │   ├── UIEngineFactory.java          ← Engine selection and instantiation
│   │   │   ├── EngineConfig.java             ← Engine configuration
│   │   │   ├── LocatorDescriptor.java        ← Engine-agnostic locator descriptor
│   │   │   └── selenium/
│   │   │       └── SeleniumEngine.java       ← Selenium implementation of UIEngine
│   │   ├── runtime/
│   │   │   ├── VOID.java                     ← Framework entry point / façade
│   │   │   └── VOIDBuilder.java              ← Fluent builder for VOID sessions (single-use)
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
│   │   │   ├── DriverManager.java             ← Lifecycle orchestration
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
│   │   ├── target/
│   │   │   └── Target.java                   ← Domain-neutral root (display text, args, effective-args)
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
│   │   │   ├── UIElement.java                ← Web-domain root contract, extends core.target.Target
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
│   │   └── exapmlepages/                     ← (removed — see examples.pages)
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

### Primary Path: VOID Session Façade

```
1. VOID.builder().start() → bootstrap → engine → VOID session ready
2. app.navigateTo(url)  → engine.navigateTo(url)
3. Page enum            → element.click() / element.type("text")     ← returns Action (deferred)
4. Flow                 → Flow.of(action1, action2, action3)         ← groups Actions
5. app.run(flow)        → FlowExecutor.run(flow) (internal)          ← iterates
6. Action lambda        → engine.resolve(this, ROLE)                 ← resolves LocatorDescriptor
7. UIEngine             → engine.click(descriptor)                   ← executes (scroll, wait, retry internal)
8. Logging              → CustomLogger emits color-coded output
9. app.getCurrentUrl()  → engine.getCurrentUrl()                     ← session-level query
10. app.shutdown()      → engine.shutdown() + DriverContext cleanup   ← session-scoped
```

### Legacy Path: Interactions (Frozen)

```
1. VOID instantiation → UIEngine created, wrapped in Interactions
2. UIElement resolution → LocatorResolvers resolve LocatorDescriptor
3. Hook pipeline      → Before.* hooks execute
4. Action execution   → Interactions delegates to engine.click(descriptor) / engine.type(descriptor, text)
5. Hook pipeline      → After.* hooks execute
6. Logging            → CustomLogger emits the action record
```

---

## 🧠 Example Usage

### Session Façade (Preferred)

```java
import core.flow.Flow;
import core.runtime.VOID;

VOID app = VOID.builder().start();

app.navigateTo("https://example.com/login");

app.run(Flow.of(
    LoginField.USERNAME.type("admin@example.com"),
    LoginField.PASSWORD.type("secret"),
    LoginButton.SUBMIT.click()
));

assertTrue(app.getCurrentUrl().contains("/dashboard"));
String title = app.getTitle();

app.shutdown();
```

### Multi-Session

```java
VOID admin    = VOID.builder().start();
VOID customer = VOID.builder().start();

admin.navigateTo(ADMIN_URL);
admin.run(adminLoginFlow);

customer.navigateTo(APP_URL);
customer.run(customerLoginFlow);

admin.run(createUserFlow);

customer.run(searchUserFlow);

admin.run(approveFlow);

customer.run(verifyApprovalFlow);

admin.shutdown();    // session-scoped — does NOT affect customer
customer.shutdown();
```

### Legacy: Interactions (Backward Compat, Deprecated)

```java
// @Deprecated since 2.1 — use app.run(element.click()) instead
VOID app = VOID.builder().start();
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

Playwright is supported at the architecture contract level through `UIEngine`; adding a Playwright adapter does not require changes to test-level Action/Flow code.

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
> It separates intent (Action), structure (UIElement), and execution (UIEngine).
> Every line of code is designed for introspection — enabling you to see not only what failed, but **why** and **how**.

### Architecture Invariants

- **Elements NEVER execute** — they emit typed Action subclasses (intent) only
- **Capabilities describe, Actions execute** — execution policy lives in actions, never in capability interfaces (ADR-013)
- **Actions are concrete types** — `ClickAction`, `TypeAction`, etc. own their execution logic and profile defaults; no anonymous lambdas or central dispatch (ADR-014)
- **ActionCapability is metadata** — never used to select execution paths; logging/tracing/diagnostics only
- **Action extension is additive** — adding a new action type or `ElementRole` value requires no changes to existing classes; new subclass declares its own profile and `operationLabel()` is derived automatically
- **Actions NEVER perform work** until executed by UIEngine via FlowExecutor
- **UIEngine owns ALL execution concerns** — scroll, waits, retries, fallback
- **`VOID` is the primary session object** — examples navigate, run flows, and teardown through it
- **`FlowExecutor` is internal** — test code calls `app.run()`, not `new FlowExecutor(engine)`
- **One execution path**: UIElement → Action → Flow → `VOID.run()` → FlowExecutor → UIEngine
- **No compile-time code generation** — all behavior is visible and debuggable

---

## 🏷️ Stability Tiers

VOID uses explicit stability tiers to control how the architecture evolves. Each API surface has a defined tier that determines what guarantees consumers get.

| Tier | Scope | Guarantees | Rules |
|------|-------|------------|-------|
| **Stable (frozen)** | `Interactions` | No breaking changes. No new features. | Will not evolve further. |
| **Stable (user-facing)** | Capability interfaces (`Clickable`, `Typeable`, etc.), `UIElement`, `UIEngine`, `ActionHandler`, `Before`, `After` | No breaking changes. May gain new methods. | Backward-compatible evolution only. |
| **Beta** | `Action`, `Flow`, `FlowExecutor`, `HookedAction` | May change without notice between releases. | Must not be used inside stable modules. |
| **Internal** | `ElementActions`, migration bridges, adapters, helper classes | No guarantees. May be changed, moved, or removed at any time. | External consumers must not depend on these. |

### Usage Rules

1. **Beta APIs must not be used inside stable modules.**
2. **Stable APIs may depend on stable APIs only.**
3. **Beta APIs may change, be renamed, or be removed in any release.**
4. **Internal APIs are not for external consumption** — they exist for framework plumbing only.
5. **Capability interfaces are stable contracts** — they define structure. The Action objects they return are beta types, but callers consume them opaquely (pass to `Flow.of(...)` / `FlowExecutor`), so beta internals don't leak into test-level code.

### Annotations

| Annotation | Meaning |
|---|---|
| `@Beta(since = "2.0")` | API is evolving — do not depend on from stable modules |
| `@Internal` | Framework plumbing — external consumers must not use |
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
