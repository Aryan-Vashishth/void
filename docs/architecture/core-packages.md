# Core Package Architecture

> Detailed reference for every sub-package inside `src/main/java/core/`.

---

## Package Map

```
core/
├── actions/          ← Kernel deferred execution contracts (Action, HookChainAction)
├── adapters/         ← External framework adapters (Cucumber)
│   └── cucumber/     ← BDD step definitions
├── annotations/      ← Stability-tier markers (@Beta, @Internal)
├── bootstrap/        ← One-time framework initialisation
├── bridge/
│   └── selenium/     ← Selenium compatibility bridge (deprecated)
├── context/          ← Per-session execution context holders
├── engine/           ← Kernel engine contracts (Executor, EngineBootstrap, EngineConfig,
│                       DomainRegistrar, DomainRegistry); web types moved to domain/
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
├── resolvers/        ← Locator dev-tooling (migration, sync, template gen)
│   └── locator/      ← Locator file tooling (not the runtime resolution pipeline)
│       ├── json/     ← EnumLocatorScanner, JsonLocatorMigrator, JsonMigratorCli
│       ├── sync/     ← LocatorSyncOrchestrator, orphan detection, template writer
│       └── template/ ← LocatorTemplate (substitution model)
├── runtime/          ← Framework entry point (VOID façade)
├── target/           ← Domain-neutral Target root interface
└── utils/            ← Cross-cutting utilities
    ├── data/         ← Test data generation and verification
    ├── io/           ← File I/O, JSON/Properties readers
    │   ├── json/     ← JsonReader, JsonLogger
    │   └── properties/ ← PropertiesReader
    └── web/          ← DOM utilities, waits, table/upload helpers

domain/
└── automation/
    └── web/          ← Web-domain module root (WebDomainRegistrar)
        ├── engine/   ← UIEngine, UIEngineFactory, EngineRegistrar
        ├── locator/  ← LocatorDescriptor, LocatorStrategy (open interface), NamedStrategy
        ├── resolve/  ← Runtime locator resolution pipeline
        │   ├── api/  ← LocatorResolver, LocatorResolvers, LocatorRequest, LocatorContext
        │   ├── json/ ← JsonLocatorReader, JsonNodeLookup, JsonTreeBuilder
        │   ├── parser/   ← ByParser, ByPrefixStrategy
        │   ├── properties/ ← PropertiesFileLocatorReader
        │   └── source/   ← LocatorSource, LocatorSourceRegistry, layered sources
        ├── selenium/ ← SeleniumEngine, SeleniumEngineRegistrar
        │   └── driver/   ← SeleniumDriverFactory, SeleniumDriverContext,
        │                   SeleniumDriverManager, Waiter
        └── vocabulary/ ← Web UI-domain types
            ├── actions/  ← Concrete UI action classes (ElementAction and 17 subclasses)
            ├── capability/ ← Capability interfaces + LocatorRoles
            ├── element/  ← UIElement, LocatorFamily, ElementSupport, KeyValuePair
            └── role/     ← ElementRole, EnumClassRegistry
```

---

## Sub-Package Details

### `core.actions` — Kernel Deferred Execution Model

**Purpose:** Defines the domain-neutral `Action` contract — the kernel side of VOID's
primary execution path. As of runtime-redesign I2 phase 2.2 (kernel/UI action split,
ADR-021), this package holds only kernel types; concrete UI actions live in
`elements.api.actions` (below).

**Key Classes:**

| Class | Role |
|-------|------|
| `Action` | Functional interface — a single deferred operation |
| `ActionCapability` | Metadata enum identifying the capability category of an action |
| `ActionProfile` / `Profile` | Named hook-bundle contract |
| `ActionProfiles` | Public (since 2.2): domain-neutral `DEFAULT_SAFE`/`DEFAULT_RELIABLE` and config-driven default-profile selection (`applyConfiguredDefault()`) |
| `Profiles` | Public presets: RAW, DEBUG, FAST, VISUAL — applied via `action.using(Profiles.X)` |
| `HookChainAction` | Decorator: applies before/after hooks around a delegate `Action`, sharing a single resolved descriptor; owns the full trace pipeline (`performAndTrace()`, `LAST_TRACE`, `lastTrace()`, `clearLastTrace()`) |

**Rules:**
- Kernel action types never reference `WebDriver`, `WebElement`, `By`, `UIElement`, `ElementRole`, or capability interfaces (ADR-021; enforced by `KernelBoundaryRulesTest`).
- `ActionCapability` is metadata only — never used to select execution paths.
- Hook composition is fluent: `element.click().before(...).after(...)`

**Stability:** `@Beta` — API may change between releases.

---

### `domain.automation.web.vocabulary.actions` — UI-Domain Concrete Action Layer

**Purpose:** Concrete UI actions and the abstract family bases that emit them. Split
from `core.actions` in runtime-redesign I2 phase 2.2 (ADR-021), then relocated from
`elements.api.actions` to `domain.automation.web.vocabulary.actions` in I6 (ADR-024,
Domain Registration).

**Key Classes:**

| Class | Role |
|-------|------|
| `ElementAction` | Abstract base (Template Method): `resolve()` → `execute()`; owns `safely()`, `reliable()`, `debug()`, `raw()`, `before()`, `after()` fluent APIs |
| `ClickableElementAction` | Package-private abstract: provides `CLICKABLE_SAFE`/`CLICKABLE_RELIABLE` defaults for the 3 click-family classes |
| `TypeableElementAction` | Package-private abstract: provides `TYPEABLE_SAFE`/`TYPEABLE_RELIABLE` defaults for the 6 type-family classes |
| `SelectableElementAction` | Package-private abstract: provides `SELECTABLE_SAFE`/`SELECTABLE_RELIABLE` defaults for the 5 select-family classes |
| `ClickAction` | Concrete: `engine.click()`, TRIGGER role, CLICKABLE capability |
| `TypeAction` | Concrete: `engine.type()`, INPUT role, TYPEABLE capability |
| `SelectAction` | Concrete composite: TRIGGER open + overlay wait + LIST select |
| `HoverAction` | Concrete: `engine.hover()`, PRIMARY role, HOVERABLE capability |
| `ReadTextAction` | Concrete: `engine.getText()`, TEXT role, READ_ONLY capability |
| *(+ 11 more)* | `ClearAction`, `CheckAction`, `AppendTypeAction`, `TypeAndPressAction`, `UploadAction`, `OpenAction`, `SelectByTextAction`, `SelectByValueAction`, `ToggleAction`, `TypeSearchAction`, `SubmitSearchAction`, `SearchAndSelectAction` |
| `CapabilityProfiles` | Package-private: 6 capability-specific safe/reliable constants (`CLICKABLE_SAFE`, `CLICKABLE_RELIABLE`, `TYPEABLE_SAFE`, `TYPEABLE_RELIABLE`, `SELECTABLE_SAFE`, `SELECTABLE_RELIABLE`) |
| `ElementActions` | `@Internal` factory — custom-operation actions for test infrastructure only (ADR-012) |

**How it works:**
1. Capability interfaces (e.g., `Clickable.click()`) emit **typed concrete action subclasses** — `new ClickAction(this)`.
2. Each concrete class extends the appropriate abstract intermediary (`ClickableElementAction`, `TypeableElementAction`, `SelectableElementAction`, or `ElementAction` directly) and implements only `execute()`.
3. Profile defaults (`defaultSafeProfile()` / `defaultReliableProfile()`) are centralized in the intermediary — concrete classes inherit them without any boilerplate. Family-specific defaults come from `CapabilityProfiles`; the un-specialized default (`HoverAction`, `UploadAction`, `ReadTextAction`) comes from `core.actions.ActionProfiles`.
4. Fluent profile APIs (`safely()`, `debug()`, `reliable()`) return a new wrapped action with hooks applied.
5. Actions are composed into `Flow` sequences (kernel, `core.flow`).
6. `FlowExecutor` (kernel, `core.executor`) iterates and calls `action.perform(engine)`.
7. `perform()` calls `resolve(engine)` then `execute(engine, descriptor)` — resolution is deferred to execution time.
8. `HookChainAction` (kernel, `core.actions`) wraps an action with before/after hooks, sharing a single resolved descriptor.

**Layering rule (ADR-013):** Execution policy (hooks, waits, retries) lives in actions, never in capability interfaces. Capabilities describe structure; actions describe execution.

**Rules:**
- Actions never reference `WebDriver`, `WebElement`, or `By`.
- Extension via new action subclasses: extend the appropriate intermediary and implement `execute()`. No changes to `CapabilityProfiles`, existing action classes, or `ElementAction` are required (OCP).
- For a full breakdown of the hierarchy, all 17 concrete classes, and profile content, see [Action Layer Architecture](actions.md).

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
1. Loads utils/test configuration from classpath.
2. Seeds `ConfigLoader.ACTIVE` for backward-compatibility.

`driver.properties` validation was removed from `FrameworkBootstrap.init()` in I5.2
(ADR-022) and relocated to `SeleniumEngine.initialize()`, where it fires only when a
web session is created. Non-web domains may call `init()` without `driver.properties`.

**Properties:**
- Safe to call `init()` multiple times — only the first invocation performs work.
- Free of driver logic, test logic, or mutable global state beyond the `initialized` guard.

---

### `core.bridge.selenium` — Selenium Compatibility Bridge *(deprecated)*

**Purpose:** Temporary adapter that converts Selenium `By` instances to `LocatorDescriptor` for deprecated `Interactions` method overloads. Deleted together with those overloads.

**Key Class:** `SeleniumLocatorBridge` *(deprecated, `forRemoval = true`)*

**Rule:** No new call sites. Do not import in engine-agnostic layers (`core.runtime`, `core.interactions` active paths, `dsl`).

---

### `core.context` — Per-Session Execution Context

**Purpose:** Immutable, explicitly-passed context objects replacing global state.

| Class | Engine Dependency | Use Case |
|-------|-------------------|----------|
| `ExecutionContext` | `WebDriver` (Selenium-coupled) | Legacy path, internal |
| `SessionContext` | `Executor` (engine-agnostic) | Modern path, preferred |

**Benefits:**
- No global mutable singletons.
- Enables safe parallel execution.
- Makes dependencies visible and testable at construction time.

---

### `domain.automation.web.selenium.driver` — WebDriver Lifecycle

**Purpose:** Complete WebDriver lifecycle management isolated from the rest of the framework.
Relocated from `core.driver` in I6 (ADR-024, Domain Registration).

| Class | Responsibility |
|-------|---------------|
| `SeleniumDriverFactory` | Fluent WebDriver builder (Chrome, Firefox, Edge, headless, remote, grid) |
| `SeleniumDriverContext` | Thread-local driver storage |
| `SeleniumDriverManager` | Lifecycle orchestration (create, register, quit) |
| `Waiter` | Explicit-wait helpers (visibility, clickability, presence) |

**Configuration:** Entirely via `driver.properties` -- no code changes required.

**Thread safety:** All classes use `ThreadLocal` storage for safe parallel execution.

---

### `core.engine` — Kernel Engine Contracts

**Purpose:** Kernel-level execution contracts and domain-registration SPI. Pure kernel: no
Selenium, no web types.

| Class | Role |
|-------|------|
| `Executor` | Kernel neutral execution contract (`perform`, `getEngineName`, `shutdown`) |
| `EngineBootstrap` | Sealed initialization token passed to domain registrars |
| `EngineConfig` | Engine-agnostic init parameters (timeouts, baseUrl, polling) |
| `DomainRegistrar` | SPI for registering a new execution domain (I6, ADR-024) |
| `DomainRegistry` | Discovers and holds all `DomainRegistrar` implementations via `ServiceLoader` |

Web-specific engine types (`UIEngine`, `UIEngineFactory`, `EngineRegistrar`) live in
`domain.automation.web.engine` (relocated in I6, ADR-024). The Selenium implementation
(`SeleniumEngine`, `SeleniumEngineRegistrar`) lives in `domain.automation.web.selenium`.

**Extensibility via `DomainRegistrar` SPI (I6, ADR-024):**

Adding a new execution domain requires zero edits to the kernel:

1. Implement `DomainRegistrar` in the domain's own package.
2. Add its fully-qualified name to `META-INF/services/core.engine.DomainRegistrar`.
3. `DomainRegistry` discovers it at class-load time via `ServiceLoader`.

---

### `core.executor` — Flow Execution

**Purpose:** Terminal component that iterates flows and delegates to UIEngine.

**Key Class:** `FlowExecutor`

**Design:**
- Intentionally "dumb" — only iterates and calls `action.perform(engine)`.
- All smart execution logic lives in UIEngine.
- Hook orchestration is in `HookChainAction`, not here.

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

### `core.resolvers.locator` — Locator Dev-Tooling

**Purpose:** Development and migration tools for locator files. The runtime resolution
pipeline moved to `domain.automation.web.resolve.*` in I6 (ADR-024).

**Sub-packages:**

| Package | Role |
|---------|------|
| `template` | `LocatorTemplate` with STRICT/PAD_LAST substitution policies |
| `json` | `EnumLocatorScanner`, `JsonLocatorMigrator`, `JsonMigratorCli` (migration CLI) |
| `sync` | `LocatorSyncOrchestrator`, orphan-key detection, template writer |

**Runtime resolution** (for call-site reference) lives in `domain.automation.web.resolve`:

| Package | Role |
|---------|------|
| `api` | `LocatorResolvers`, `LocatorResolver`, `LocatorRequest`, `LocatorContext`, `LocatorPaths` |
| `parser` | Raw string → `By` (`ByParser`, `ByPrefixStrategy`) |
| `source` | Polymorphic backing sources (`LocatorSource`, `LocatorSourceRegistry`, layered impls) |
| `json` | `JsonLocatorReader`, `JsonNodeLookup`, `JsonTreeBuilder`, `PropertiesIndex` |
| `properties` | `PropertiesFileLocatorReader` |

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

**Purpose:** The VOID façade -- main entry point for starting and managing sessions.

**Key Classes:** `VOID`, `VOIDBuilder`

**Startup pipeline:**
```
VOID.builder()
  .profile(SeleniumDriverFactory.Profile.DEFAULT)   (optional -- defaults to DEFAULT)
  .engine(SeleniumEngine.ID)                        (optional -- defaults to config/ENV)
  .start()
    → FrameworkBootstrap.init()             (one-time utils config load; no driver.properties gate)
    → UIEngineFactory.create()              (engine selected; driver deferred to SeleniumEngine.initialize())
    → SessionContext(config, Executor)      (bind config + engine-agnostic executor)
    → return VOID façade
```

**Public API:**
- `VOID.builder()` — obtain a `VOIDBuilder` (fluent, single-use per session)
- `VOID.builder().profile(p).start()` — create a session with a driver profile
- `VOID.builder().engine(id).profile(p).start()` — create a session with explicit engine override
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
| `web` | `KeyValuePairHandler`, `Upload`; `DOMUtils` *(deprecated)*, `WaitUtils` *(By-based API deprecated)*, `TableHandler` *(deprecated)* | Browser/DOM utilities -- deprecated classes migrated to `UIEngine` |

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
1. VOID.builder().start() → engine + interactions created
2. UIElement           → LocatorResolvers resolve LocatorDescriptor
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

