# Core Package Architecture

> Detailed reference for every sub-package inside `src/main/java/core/`.

---

## Package Map

```
core/
├── actions/          ← Deferred execution model (Action, HookedAction)
├── adapters/         ← External framework adapters (Cucumber)
│   └── cucumber/     ← BDD step definitions
├── annotations/      ← Stability-tier markers (@Beta, @Internal)
├── bootstrap/        ← One-time framework initialisation
├── context/          ← Per-session execution context holders
├── driver/           ← WebDriver lifecycle management
├── engine/           ← Engine abstraction layer
│   └── selenium/     ← Selenium implementation of UIEngine
├── executor/         ← Flow execution (FlowExecutor)
├── flow/             ← Declarative action composition (Flow)
├── interactions/     ← Legacy interaction orchestrator (frozen)
│   └── hooks/        ← Before/After action hook pipeline
├── logging/          ← ANSI-colored, theme-aware logging
│   ├── ansi/         ← ANSI escape primitives and color constants
│   ├── config/       ← Runtime log configuration and Log4j holder
│   ├── intent/       ← Semantic intent taxonomy (LogIntent)
│   ├── render/       ← Log rendering pipeline (LogActions)
│   └── theme/        ← Color theme model and built-in catalog
├── resolvers/        ← Locator resolution subsystem
│   └── locator/      ← Role-based locator pipeline
│       ├── api/      ← Public API (LocatorResolvers, LocatorRequest)
│       ├── parser/   ← Raw string → By (ByParser, ByPrefixStrategy)
│       ├── template/ ← Locator template substitution
│       ├── source/   ← Polymorphic backing sources
│       ├── json/     ← JSON format readers and migration tools
│       └── properties/ ← Properties format reader
├── runtime/          ← Framework entry point (VOID façade)
└── utils/            ← Cross-cutting utilities
    ├── data/         ← Test data generation and verification
    ├── io/           ← File I/O, JSON/Properties readers
    │   ├── json/     ← JsonReader, JsonLogger
    │   └── properties/ ← PropertiesReader
    └── web/          ← DOM utilities, waits, table/upload helpers
```

---

## Sub-Package Details

### `core.actions` — Deferred Execution Model

**Purpose:** Defines the Action/Flow/FlowExecutor pipeline — VOID's primary execution path.

**Key Classes:**

| Class | Role |
|-------|------|
| `Action` | Functional interface — a single deferred UI operation |
| `ElementAction` | Abstract base class (Template Method): `resolve()` → `execute()` |
| `ClickAction` | Concrete: `engine.click()`, TRIGGER role, CLICKABLE capability |
| `TypeAction` | Concrete: `engine.type()`, INPUT role, TYPEABLE capability |
| `SelectAction` | Concrete composite: TRIGGER open + LIST select |
| `HoverAction` | Concrete: `engine.hover()`, TEXT role, HOVERABLE capability |
| `ReadTextAction` | Concrete: `engine.getText()`, TEXT role, READ_ONLY capability |
| *(+ 12 more)* | `ClearAction`, `CheckAction`, `AppendTypeAction`, `TypeAndPressAction`, `UploadAction`, `OpenAction`, `SelectByTextAction`, `SelectByValueAction`, `ToggleAction`, `TypeSearchAction`, `SubmitSearchAction`, `SearchAndSelectAction` |
| `ActionProfiles` | Package-private: owns safe/reliable profile constants (`CLICKABLE_SAFE`, `TYPEABLE_SAFE`, etc.) — referenced directly by each concrete action subclass |
| `Profiles` | Action-independent preset profiles: RAW, DEBUG, FAST, VISUAL |
| `ElementActions` | `@Internal` factory — custom-operation actions for test infrastructure only |
| `HookedAction` | Decorator applying before/after hooks around a delegate Action |

**How it works:**
1. Capability interfaces (e.g., `Clickable.click()`) emit **typed concrete action subclasses** — `new ClickAction(this)`.
2. Each concrete action owns its locator role, capability, and default profile (see ADR-014).
3. Fluent profile APIs (`safely()`, `debug()`, `reliable()`) return a new wrapped action with hooks applied.
4. Actions are composed into `Flow` sequences.
5. `FlowExecutor` iterates and calls `action.perform(engine)`.
6. `perform()` calls `resolve(engine)` then `execute(engine, descriptor)` — resolution is deferred to execution time.
7. `HookedAction` wraps an action with before/after hooks, sharing a single resolved descriptor.

**Layering rule (ADR-013):** Execution policy (hooks, waits, retries) lives in actions, never in capability interfaces. Capabilities describe structure; actions describe execution.

**Rules:**
- Actions never reference `WebDriver`, `WebElement`, or `By`.
- `ActionCapability` is metadata only — never used to select execution paths.
- Hook composition is fluent: `element.click().before(...).after(...)`
- Extension via new action subclasses: each declares its own profile via `defaultSafeProfile()` / `defaultReliableProfile()` override; no changes to existing classes required (OCP).

**Stability:** `@Beta` — API may change between releases. `ElementAction` subclasses are concrete and stable within a release.

---

### `core.adapters` — External Framework Integration

**Purpose:** Adapter layers wiring VOID to external test frameworks.

**Sub-packages:**

| Package | Integration |
|---------|-------------|
| `core.adapters.cucumber` | Cucumber BDD step definitions |

**Design rules:**
- Adapters are `@Internal` — they may change without notice.
- Adapters contain no business logic — only translation.
- Each adapter is independently optional — VOID works without any.
- Cucumber is an optional dependency ([ADR-002](../decisions/accepted/002-cucumber-optional-dependency.md)).

---

### `core.annotations` — Stability Tier Markers

**Purpose:** Communicates API stability guarantees to consumers.

| Annotation | Tier | Guarantees |
|------------|------|------------|
| *(none)* | Stable | Backward-compatible evolution |
| `@Beta(since)` | Beta | May change without notice |
| `@Internal` | Internal | No guarantees; framework plumbing only |
| `@Deprecated` | Frozen | No changes, no new features |

**Rules:**
1. Beta APIs must not be used inside stable modules.
2. Stable APIs may depend on stable APIs only.
3. Internal APIs are not for external consumption.

---

### `core.bootstrap` — Framework Initialisation

**Purpose:** One-time, idempotent startup that must run before any driver or test logic.

**Key Class:** `FrameworkBootstrap`

**What it does:**
1. Verifies `driver.properties` is on the classpath (fail-fast).
2. Loads utils/test configuration from classpath.
3. Seeds `ConfigLoader.ACTIVE` for backward-compatibility.

**Properties:**
- Safe to call `init()` multiple times — only the first invocation performs work.
- Free of driver logic, test logic, or mutable global state beyond the `initialized` guard.

---

### `core.context` — Per-Session Execution Context

**Purpose:** Immutable, explicitly-passed context objects replacing global state.

| Class | Engine Dependency | Use Case |
|-------|-------------------|----------|
| `ExecutionContext` | `WebDriver` (Selenium-coupled) | Legacy path, internal |
| `SessionContext` | `UIEngine` (engine-agnostic) | Modern path, preferred |

**Benefits:**
- No global mutable singletons.
- Enables safe parallel execution.
- Makes dependencies visible and testable at construction time.

---

### `core.driver` — WebDriver Lifecycle

**Purpose:** Complete WebDriver lifecycle management isolated from the rest of the framework.

| Class | Responsibility |
|-------|---------------|
| `DriverFactory` | Fluent WebDriver builder (Chrome, Firefox, Edge, headless, remote, grid) |
| `DriverContext` | Thread-local driver storage |
| `DriverManager` | Lifecycle orchestration (create, register, quit) |
| `Waiter` | Explicit-wait helpers (visibility, clickability, presence) |

**Configuration:** Entirely via `driver.properties` — no code changes required.

**Thread safety:** All classes use `ThreadLocal` storage for safe parallel execution.

---

### `core.engine` — Engine Abstraction Layer

**Purpose:** Decouples VOID's interaction layer from any specific browser automation library.

| Class | Role |
|-------|------|
| `UIEngine` | Execution contract interface |
| `LocatorDescriptor` | Engine-agnostic locator representation |
| `LocatorStrategy` | Locator type enum (XPATH, CSS, ID, NAME, etc.) |
| `EngineConfig` | Engine initialisation parameters |
| `UIEngineFactory` | Creates engine instances from config |

**Sub-packages:**

| Package | Content |
|---------|---------|
| `core.engine.selenium` | `SeleniumEngine` — default production implementation |

**UIEngine responsibilities:**
- Resolve `LocatorDescriptor` → native locator
- Handle scroll, waits, retries, fallback internally
- Callers must NOT perform their own scroll/wait/retry logic

**Extensibility:** Adding a Playwright engine means creating `core.engine.playwright` — no test code changes.

---

### `core.executor` — Flow Execution

**Purpose:** Terminal component that iterates flows and delegates to UIEngine.

**Key Class:** `FlowExecutor`

**Design:**
- Intentionally "dumb" — only iterates and calls `action.perform(engine)`.
- All smart execution logic lives in UIEngine.
- Hook orchestration is in `HookedAction`, not here.

**Stability:** `@Beta`

---

### `core.flow` — Action Composition

**Purpose:** Groups multiple Actions into declarative, reusable workflows.

**Key Class:** `Flow`

**Properties:**
- Immutable and reusable.
- Pure data — describes what to do, not how.
- Creating a Flow triggers no browser interaction.

**Stability:** `@Beta`

---

### `core.interactions` — Legacy Interaction Orchestrator

**Purpose:** Original UI action layer — **frozen, deprecated, no new features**.

| Class | Role |
|-------|------|
| `Interactions` | Full-featured orchestrator (click, type, select, search, etc.) |
| `Via` | Static cast/locator helpers |

**Sub-package: `core.interactions.hooks`**

| Class | Role |
|-------|------|
| `ActionHandler` | Functional interface: `(UIEngine, LocatorDescriptor) → void` |
| `Before` | Pre-built before-hook constants (WAIT_FOR_VISIBLE, HIGHLIGHT, etc.) |
| `After` | Pre-built after-hook constants (HIGHLIGHT_ELEMENT, LOG_INTENT, etc.) |

**Hook ordering:** Before hooks (list order) → Action → After hooks (list order).

**Migration:** New code should use the Action/Flow/FlowExecutor pipeline instead.

---

### `core.logging` — ANSI-Colored Logging System

**Purpose:** Deep observability with color-coded, theme-aware, dual-channel logging.

**Public API:**
- `CustomLogger` — facade with `info.log()`, `debug.click()`, `warn.fallback()`, etc.
- `ConsoleOnly` — annotation marking terminal-only features.

**Internal sub-packages:**

| Package | Responsibility |
|---------|---------------|
| `ansi` | ANSI escape primitives (`AnsiEscape`), named color constants (`AnsiColors`), diagnostic matrix |
| `config` | Runtime settings (`LogConfig`), Log4j logger handle (`LoggerContext`) |
| `intent` | Semantic taxonomy (`LogIntent`) — classifies what a log communicates |
| `render` | Rendering pipeline (`LogActions`) — composes and dispatches log lines via Log4j |
| `theme` | Theme model (`LogTheme`, `ThemeColors`), 8 built-in themes (`BuiltInThemes`) |

**Log format:** `[LEVEL] OriginClass.method <message> ← CallerClass.method`

**Dual-channel:** Real-time ANSI console + persistent full-depth trace log file.

---

### `core.resolvers.locator` — Locator Resolution Pipeline

**Purpose:** Reads element locators from external sources and resolves them into engine-agnostic descriptors.

**Sub-packages:**

| Package | Role |
|---------|------|
| `api` | Public API: `LocatorResolvers`, `LocatorResolver`, `LocatorRequest`, `LocatorPaths` |
| `parser` | Raw string → `By` conversion (`ByParser`, `ByPrefixStrategy`) |
| `template` | `LocatorTemplate` with STRICT/PAD_LAST substitution policies |
| `source` | Polymorphic backing sources (`LocatorSource` interface + impls) |
| `json` | JSON locator reader, migration tools, CLI migrator |
| `properties` | Properties file locator reader |

**Resolution flow:**
1. `LocatorResolvers.strict()` receives a request (file + key + args).
2. Source registry selects the correct reader (JSON or Properties).
3. Template engine applies `%s` argument substitution.
4. Parser converts the raw string to a `LocatorDescriptor`.

**Supported formats:**
- JSON (recommended): `{ "KEY": "xpath=//div[@class='card']" }`
- Properties: `KEY=xpath=//div[@class='card']`

**Prefix tokens:** `xpath=`, `css=`, `id=`, `name=`, `tag=`, `linkText=`, `partialLinkText=`

---

### `core.runtime` — Framework Entry Point

**Purpose:** The VOID façade — main entry point for starting and managing sessions.

**Key Class:** `VOID`

**Startup pipeline:**
```
VOID.start()
  → FrameworkBootstrap.init()          (validate configs)
  → DriverManager.createDriver()       (create + register WebDriver)
  → UIEngineFactory.create()           (instantiate engine)
  → ExecutionContext                   (bind config + driver)
  → return VOID façade
```

**Public API:**
- `VOID.start()` / `VOID.start(profile)` — create a session
- `app.getEngine()` — access the UIEngine for modern usage
- `app.interaction()` — access the legacy Interactions helper
- `app.shutdown()` — clean up drivers

---

### `core.utils` — Cross-Cutting Utilities

**Purpose:** Configuration management, enum resolution, and domain-specific helpers.

**Top-level classes:**

| Class | Responsibility |
|-------|---------------|
| `ConfigLoader` | Hierarchical config: System → ENV → classpath → defaults |
| `ConfigPaths` | Standard config file path constants |
| `EnumResolver` | Normalised name → enum constant lookup |
| `ResolvableEnum` | Mixin for enums with human-readable labels |
| `UIContext` | *(deprecated)* Thread-local descriptor state for legacy compatibility |

**Sub-packages:**

| Package | Classes | Purpose |
|---------|---------|---------|
| `data` | `DataGenerator`, `DataVerifier` | Test data generation and verification |
| `io` | `FileUtils`, `JsonReader`, `JsonLogger`, `PropertiesReader` | File I/O and format readers |
| `web` | `DOMUtils`, `WaitUtils`, `TableHandler`, `KeyValuePairHandler`, `Upload` | Browser/DOM utilities |

---

## Execution Flow Summary

### Modern Path (Recommended)

```
1. Page enum        → element.click() / element.type("text")     ← returns Action (deferred)
2. Flow             → Flow.of(action1, action2, action3)         ← groups Actions
3. FlowExecutor     → executor.run(flow)                         ← iterates
4. Action lambda    → engine.resolve(this, ROLE)                 ← resolves LocatorDescriptor
5. UIEngine         → engine.click(descriptor)                   ← executes (scroll, wait, retry)
6. Logging          → CustomLogger emits color-coded output
```

### Legacy Path (Frozen — backward compat only)

```
1. VOID.start()      → engine + interactions created
2. Element           → LocatorResolvers resolve LocatorDescriptor
3. Before hooks      → Before.* hooks execute
4. Interactions      → delegates to engine.click(descriptor) / engine.type(descriptor, text)
5. After hooks       → After.* hooks execute
6. Logging           → CustomLogger emits action record
```

---

## See Also

- [System Overview](system-overview.md) — high-level architecture and design philosophy
- [Hooks Pipeline](hooks-pipeline.md) — composable before/after action hooks
- [Locator Resolution](locator-resolution.md) — role-based locator lookup details
- [Logging Reference](logging-reference.md) — logger configuration and usage
- [Configuration Reference](configuration-reference.md) — all config properties
- [Quick Start](quick-start.md) — getting started guide

