# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow [Semantic Versioning](https://semver.org/).

> **Release checklist:** when cutting a new version, update `version.json` at the repo
> root -- the README.md version badge reads from that file automatically.

---

## [Unreleased]

### Internal

- **Open capability set -- ActionCapability is now an interface (runtime-redesign I3.1)**
  - `ActionCapability` converted from a closed enum to an open interface; the 15 built-in
    constants (`CLICKABLE`, `TYPEABLE`, etc.) are preserved as static fields with
    identical names and string-equality semantics
  - New domains define capabilities via `ActionCapability.of("MY_CAP")` -- no edits to
    runtime files required
  - Equality is name-based (`record NamedCapability` backing); two capabilities with the
    same name are equal
  - Extension fitness test added: `ActionCapabilityExtensionTest` proves a custom
    capability registers and carries through an `Action` with zero runtime edits

  *Migration for existing callers:* `ActionCapability.CONSTANT_NAME` references are
  unchanged. `.values()`, `.ordinal()`, and `switch (capability)` idioms are no longer
  valid -- none existed in-repo.

- **UNKNOWN silent fallback removed (runtime-redesign I3.2)**
  - `Action.safely()` now throws `IllegalStateException` when the action's capability
    is `ActionCapability.UNKNOWN`; the runtime cannot select browser-wait hooks for an
    unrecognised capability -- use `.raw()` or declare a specific `ActionCapability`
  - `ActionProfiles.applyConfiguredDefault()` skips profile application for UNKNOWN
    capability and emits a `WARN` log naming the configuration remedy
  - `docs/contributing/configuration-reference.md` created: documents `void.profile.default`
    and the post-I3.2 UNKNOWN guard behaviour

  *Migration for custom actions relying on silent defaults:* declare a specific
  `ActionCapability.of("MY_CAP")` on your action, or call `.raw()` explicitly if
  no browser-wait contract applies.

- **ADR-021 -- Kernel boundary, ontology, and open decisions (runtime-redesign I0)**
  - Resolves the three open architectural decisions from the 2026-07 audit: AD1 (one
    session binds one domain), AD2 (execution owner named `Executor`; `UIEngine` is the
    web domain's refinement), AD3 (capability validation at dispatch time)
  - Establishes the closed kernel membership list and two neutrality axes (engine vs domain)
  - Formally adopts the five-concept ontology (Runtime, Session, Interaction, Capability,
    Target, Domain) as the framework's design vocabulary

- **Architecture fitness checks -- kernel boundary ratchet (Phase 0.2)**
  - `KernelBoundaryRulesTest`: seven ArchUnit rules encoding boundaries already true
    as of ADR-021: `core.logging`, `core.flow`, `core.actions`, `elements.*` verified
    Selenium-free; `core.runtime` verified free of WebDriver/DriverContext fields;
    `LocatorDescriptor` verified Selenium-free
  - `docs/contributing/architecture-rules.md` gains the ratchet-tightening protocol

- **Development workflow -- multi-initiative program branching documented (Phase 0.3)**
  - `docs/contributing/workflow.md` documents per-initiative branch topology for
    multi-phase programs; includes the full runtime-redesign reference table (I0-I9)

- **OOP violations remediation -- Phases 1-4 (P1-P7, P9, P10)**

  *Phase 1 -- Action layer (P1, P3, P4):*
  - `Action` gains four defaults: `mergeHooks()`, `withProfile()`, `elementLabel()`, `operationLabel()`; all `instanceof HookChainAction` checks in the existing defaults removed
  - `ActionLabeled` deleted -- `elementLabel()` and `operationLabel()` are now on `Action` directly
  - `HookedAction` deleted -- full hook pipeline (`performAndTrace()`, `LAST_TRACE` ThreadLocal, `lastTrace()`, `clearLastTrace()`, `forTesting()`) absorbed into `HookChainAction`

  *Phase 2 -- Element interface safety + capability (P5, P6, P7, P10):*
  - `ElementSupport` (package-private, `elements.api`) introduced: `nameOf()`, `declaringClassOf()`, `ordinalOf()` centralise all `(Enum<?>) this` casts; `Element` defaults use it exclusively
  - `Element.capability()` default added (returns `ActionCapability.UNKNOWN`); `ActionCapabilityProvider` deleted -- `capability()` ownership lives on `Element`
  - All 8 capability interfaces (`Clickable`, `Typeable`, `Selectable`, `Hoverable`, `Checkable`, `Uploadable`, `MultiSelectable`, `ReadOnly`) drop `implements ActionCapabilityProvider`; each keeps its own `capability()` override
  - `Listable.getIndex()` default added (delegates to enum ordinal; throws for non-enum implementors)

  *Phase 3 -- DSL capability dispatch (P2):*
  - `VoidDSL.selectFromDropdownByContext` and `triggerDropdownByContext` -- two ordering-sensitive `instanceof` chains replaced with `element.capability()` enum comparison
  - `Element.getDisplayText()` gains empty-token guard (prevents `StringIndexOutOfBoundsException` for anonymous-class elements)
  - `LocatorResolver.labelOf()` returns `null` for anonymous-class elements (was calling `getDisplayText()` which threw)

  *Phase 4 -- Infrastructure (P9):*
  - `LocatorRoles` (package-private, `elements.api.capability`) introduced: `roleMap(RoleEntry...)` eliminates O(n^2) equality chains in `getAllLocatorRoles()`
  - `SearchableDropdown.getAllLocatorRoles()` and `SearchField.getAllLocatorRoles()` both use `LocatorRoles.roleMap()`
  - `Via` capability cast helpers (`clickable()`, `typeable()`, `selectable()`, `readOnly()`, `searchable()`, `searchableDropdown()`, `multiSelectable()`, `checkable()`, `hoverable()`) and predicates (`isClickable()`, `isTypeable()`, `isSelectable()`, `isReadOnly()`, `isSearchable()`, `isCheckable()`) removed -- zero call sites; use `instanceof` patterns directly

  **Deferred:** P8 (`UIEngineFactory` registry) to runtime-redesign I4.1; P11 (`Via` full deletion) to runtime-redesign I9.3.

- **Target Model -- runtime-redesign Initiative I1 (phases 1.1-1.4)**
  - `core.target.Target` introduced: domain-neutral root carrying `getDisplayText()`,
    `getArgs()`, `effectiveArgs()`, `NO_ARGS` -- zero UI or Selenium imports
  - `elements.api.Element` renamed to `elements.api.UIElement`, now `extends Target`;
    `getArgs`/`effectiveArgs`/`NO_ARGS` removed from `UIElement` (inherited from `Target`);
    `getDisplayText()` kept as an `@Override` default preserving the enum-name-split label
  - `UIEngine.resolve()` / `SeleniumEngine.resolve()` retyped to `UIElement`; all capability
    interfaces, page object enums, and demo/test sources updated (pure rename, no logic change)
  - Kernel audited against ADR-021's membership list and found already `UIElement`-free;
    `KernelBoundaryRulesTest` gains four ratchet checks so this cannot regress:
    `core.actions.trace`, `core.executor`, `core.flow`, and `core.actions` kernel types
    (excluding the `ElementAction` family, UI-domain content pending I2.2) may never
    depend on `UIElement`
  - **BREAKING CHANGE:** `Element` is removed. Replace `implements Element` with
    `implements UIElement`

- **Hooks ownership -- runtime-redesign Initiative I2, phase 2.1**
  - The hook contract (`ActionHandler`, `BeforeActionHandler`, `AfterActionHandler`) moves
    from `core.interactions.hooks` to the kernel-owned `core.actions.hooks`, so the kernel
    no longer imports through the frozen `core.interactions` legacy zone (audit D4)
  - `core.interactions.hooks.ActionHandler`/`BeforeActionHandler`/`AfterActionHandler` remain
    as `@Deprecated(forRemoval = true)` bridges (old interface extends new); existing
    imports and implementations keep compiling until removal in I9.3
  - `Before`/`After` (the pre-built, domain-specific hook constant libraries) stay in
    `core.interactions.hooks` -- they are UI-domain content, not the neutral contract
  - `ActionTraceLogger.nameOf()` no longer imports `Before`/`After` directly: gains a
    `registerNameSource(Class)` registry that `Before`/`After` populate via a static
    initializer, removing a real kernel-to-domain coupling with no behavior change
  - `KernelBoundaryRulesTest` gains two ratchet checks: `core.actions.hooks` and (with a
    documented, named exclusion for `ActionProfiles`/`Profiles`, deferred to I2.2)
    `core.actions` may never depend on `core.interactions`
  - New `HookBridgeCompatibilityTest` verifies old-typed hook implementors (not just
    lambdas) still work against the new-typed `Action.before()`/`after()` call sites
  - **Deprecated:** `core.interactions.hooks.ActionHandler`, `BeforeActionHandler`,
    `AfterActionHandler` -- use `core.actions.hooks.*` instead; scheduled for removal in I9.3

- **Kernel/UI action split -- runtime-redesign Initiative I2, phase 2.2**
  - `ElementAction` and its family (the 3 abstract intermediaries, the 17 concrete leaf
    action classes, and the `ElementActions` factory) move from `core.actions` to the
    new `elements.api.actions` package -- UI-domain content that was physically
    co-located with the kernel, per audit Part I's bounded-context finding
  - `ActionProfiles`' 6 capability-specific constants (`CLICKABLE_SAFE`,
    `CLICKABLE_RELIABLE`, `TYPEABLE_SAFE`, `TYPEABLE_RELIABLE`, `SELECTABLE_SAFE`,
    `SELECTABLE_RELIABLE`) extract into the new `elements.api.actions.CapabilityProfiles`
    (package-private); `core.actions.ActionProfiles` keeps only the domain-neutral
    `DEFAULT_SAFE`/`DEFAULT_RELIABLE` and the config-driven default-selection mechanism
  - `ActionProfiles` becomes public (was package-private): the class itself,
    `DEFAULT_SAFE`, `DEFAULT_RELIABLE`, and `applyConfiguredDefault()` -- required for
    `elements.api.actions.ElementAction`/`ElementActions` to call across the new
    kernel/UI-domain package boundary; `configuredDefault()` and `DEFAULT_PROFILE_KEY`
    stay package-private (no external caller crosses the boundary for them)
  - `KernelBoundaryRulesTest`'s ADR-021 kernel-membership checks tighten: `core.actions`
    (root) now has zero exemptions for `UIElement`/`ElementRole`/capability-interface
    dependencies (previously exempted `ElementAction`/`ElementActions`, which no longer
    live there); gains checks for `ElementRole` and `elements.api.capability` directly,
    plus a Selenium-freedom check for the new `elements.api.actions` package
  - `core.actions` now contains exactly the ADR-021 kernel list: `Action`,
    `ActionCapability`, `ActionProfile`, `ActionProfiles`, `Profile`, `Profiles`,
    `HookChainAction` -- 8 files total (including `package-info.java`)
  - **BREAKING CHANGE (Beta tier):** every concrete UI action class and `ElementAction`/
    `ElementActions` move package. Import table (old → new), all under `core.actions` →
    `elements.api.actions`: `ElementAction`, `ClickableElementAction`,
    `TypeableElementAction`, `SelectableElementAction`, `ElementActions`, `ClickAction`,
    `ToggleAction`, `CheckAction`, `TypeAction`, `ClearAction`, `AppendTypeAction`,
    `TypeAndPressAction`, `TypeSearchAction`, `SubmitSearchAction`, `OpenAction`,
    `SelectAction`, `SelectByTextAction`, `SelectByValueAction`, `SearchAndSelectAction`,
    `HoverAction`, `UploadAction`, `ReadTextAction`. External code rarely names these
    directly (capability interfaces return them opaquely per ADR-014), so breakage is
    limited to any code importing them by name.

- **Cycle break -- runtime-redesign Initiative I2, phase 2.3**
  - Audit D1 (the `elements.api` <-> kernel mutual dependency was proof the two packages
    were one bounded context): verified that after 2.2's physical relocation, no
    kernel-owned package (`core.actions`, `core.actions.trace`, `core.actions.hooks`,
    `core.flow`, `core.executor`, `core.context`, `core.runtime`) imports anything from
    `elements.*` -- the cycle was already gone as a side effect of 2.2's move; no
    production code changed in this phase
  - `KernelBoundaryRulesTest` gains `kernelPackagesDoNotDependOnElements`: a permanent
    ratchet forbidding any kernel package from depending on `elements.*` at all, broader
    than the existing type-specific checks -- the dependency direction between the
    kernel and the UI domain is now enforced as one-way by construction
  - Updates `docs/architecture/elements.md` (new invariant #7: the kernel never depends
    on `elements.*`) and `docs/architecture/actions.md` (documents the three-stage split
    across I1.4, I2.2, I2.3)

- **Kernel purity gate -- runtime-redesign Initiative I2, phase 2.4 (closes I2)**
  - Consolidates the I2.1-I2.3 negative ("must not depend on X") checks into one named,
    positive-allowlist boundary: `KernelBoundaryRulesTest.kernelPurity` asserts the kernel
    depends only on JDK/javax, `core.logging`, `core.annotations`, `core.target`, itself
    (`core.actions`/`.trace`/`.hooks`, `core.flow`, `core.executor`, `core.context` minus
    the legacy `ExecutionContext`, `core.runtime`, `core.bootstrap`), and a short,
    explicitly documented list of temporary exceptions, each cross-referenced to its
    closing phase (`core.engine.UIEngine`/`LocatorDescriptor` close at I4;
    `EngineBootstrap`/`UIEngineFactory` at 4.2; `DriverFactory`/`DriverFactory.Profile` at
    I6.4; `core.interactions.hooks.Before`/`After` and `core.interactions.Interactions`/
    `WebDriver` at I9.3; `ConfigLoader`/`ConfigPaths` unscheduled, narrow utility use)
  - Mutation demo recorded (`docs/contributing/architecture-rules.md`): a temporary
    disallowed dependency added to `core.actions.Action` was confirmed to fail the check
    with a precise violation message, then reverted; full suite re-verified green before
    the phase commit
  - `CLAUDE.md`'s Architecture Invariants table gains an Axis column (engine / domain /
    scope, per ADR-021's two neutrality axes) retrofitted onto existing rows, plus the new
    kernel purity row (axis: domain)
  - This closes runtime-redesign Initiative I2 (Kernel Extraction) -- all 4 phases done

---

## [0.4.1] - 2026-07-23

### Fixed

- **`LogActions.logMultiline` -- appender-routing regressions (hotfix/debug-trace-caller-chain)**

  Three bugs caused `debug-trace` and `partial-trace` log files to be identical:

  1. *DEBUG entries absent from debug-trace when root logger is INFO* --
     `isLogLevelEnabled()` checked only the root logger (default `INFO`), so
     `debug`-level `CustomLogger` calls returned early before reaching any file
     appender. Fixed by computing per-appender enablement (`rootOk` / `debugOk`)
     and only exiting early when neither appender accepts the level.

  2. *DEBUG entries incorrectly appearing in partial-trace* -- once (1) was fixed, the
     single unguarded method body wrote to partial-trace for all levels the debug-trace
     logger accepted. Fixed by gating console + partial-trace writes on `rootOk` and
     debug-trace writes on `debugOk`; each appender now respects its own level
     independently.

  3. *Caller chain missing from debug-trace when root logger is INFO* --
     `rawCaller` (the `Callee.method` suffix) was gated on `isDebugEnabled()` which
     also checked the root logger, so it was always empty at the default level.
     Fixed by computing `rawCaller = getCallerString()` unconditionally before the
     per-appender blocks and reusing the result rather than calling
     `getCallerString()` twice.

- **`emitGitHubWorkflowNotice` redundant guard removed** -- the `isLogLevelEnabled()`
  check inside the method was unreachable (the method is only called after the outer
  per-appender gate has passed). Replaced `isLogLevelEnabled()` with `resolveLevel()`
  which returns the typed `Level` value used for the `isEnabled()` queries.

### Tests

- **`LogAppenderRoutingTest`** -- 11 deterministic unit tests covering all three regressions:
  programmatic `CapturingAppender`s attached to the named Log4j2 loggers; root logger
  level is pinned to `INFO` per test and restored on teardown so the suite is
  order-independent. `CallerChainHelper` (in `tests.*`) provides a project-frame
  call-site so the caller chain can be asserted without modifying production filter rules.

---

## [0.3.1] - 2026-07-22

### Added

- **`VOID.builder()` -- fluent session startup (ADR-018, engine-decoupling Phase 2)**
  - `VOID.builder()` returns a new `VOIDBuilder` for fluent session configuration
  - `VOIDBuilder.profile(Profile)` -- set the driver configuration profile
  - `VOIDBuilder.engine(String)` -- override engine selection for the session
  - `VOIDBuilder.start()` -- terminal operation; selects the engine first, then defers
    driver creation to `SeleniumEngine.initialize()`; returns a ready VOID session
  - `VOIDBuilder` is single-use -- calling `start()` twice throws `IllegalStateException`;
    call `VOID.builder()` for each new session
  - Multiple independent sessions (e.g., `admin` and `customer`) are now safe to run
    concurrently -- each session owns its own `SessionContext`, engine, and driver

- **`EngineBootstrap` -- engine-agnostic factory contract (ADR-018, Phase 1)**
  - `EngineBootstrap` sealed interface decouples `UIEngineFactory.create()` from
    Selenium-specific types; replaces the `WebDriver` parameter
  - `EngineBootstrap.fromProfile(Profile)` -- current variant; engine creates and
    manages its own driver during `initialize()`

- **`SeleniumLocatorBridge` -- isolated By-to-descriptor conversion (ADR-019, Phase 3)**
  - `core.bridge.selenium.SeleniumLocatorBridge.fromBy(By)` -- converts a Selenium
    `By` to `LocatorDescriptor`; recognises `By.xpath:`, `By.cssSelector:`, `By.id:`,
    `By.name:` prefixes; unrecognised prefixes fall back to XPATH and emit a WARN log
  - Isolated in `core.bridge.selenium`; the entire package is `@Deprecated(forRemoval=true)`
    and will be removed alongside the deprecated `By`-parameter methods in `Interactions`

- **`UIEngine` -- three new contract methods (core-utils-engine-agnostic, Phase 1)**
  - `UIEngine.switchToFrame(LocatorDescriptor)` -- switch browser context into an iframe
  - `UIEngine.switchToDefaultContent()` -- return to the top-level document
  - `UIEngine.sendKeys(CharSequence...)` -- send global key events (keyboard shortcuts,
    ESCAPE, TAB, arrow navigation); `SeleniumEngine` implements all three

- **`SeleniumEngine.ID` constant** -- `"selenium"`; prefer this over raw string literals
  when overriding engine selection via `VOIDBuilder.engine(SeleniumEngine.ID)`

### Changed

- **`UIEngineFactory.create()` signature** -- parameter changed from `WebDriver driver`
  to `EngineBootstrap bootstrap`; the factory no longer receives or owns a pre-built driver
- **`SeleniumEngine` driver lifecycle** -- `SeleniumEngine(DriverFactory.Profile)` is the
  primary constructor; the engine creates and registers its own `WebDriver` during
  `initialize()` and removes it from `DriverContext` during `shutdown()`
- **Selenium JUL logger suppression** -- moved from `FrameworkBootstrap.init()` to
  `SeleniumEngine.initialize()`; a non-Selenium session no longer touches Selenium internals
  during bootstrap (ADR-018, Phase 4)
- **`VOID` session context** -- `VOID` now holds `SessionContext` (engine-typed) instead
  of `ExecutionContext` (WebDriver-typed); `VOID.shutdown()` fully delegates to
  `engine.shutdown()`
- **`VoidDSL.verifyElementsAreVisible`** -- active execution path no longer passes through
  a Selenium `By`; uses `LocatorDescriptor` end-to-end

### Deprecated

The following are deprecated with `forRemoval = true`:

| API | Replacement |
|---|---|
| `VOID.start()` | `VOID.builder().start()` |
| `VOID.start(Profile)` | `VOID.builder().profile(profile).start()` |
| `ExecutionContext` | `SessionContext` (already used internally) |
| `DOMUtils` (class + all methods) | equivalent `UIEngine` methods |
| `WaitUtils` By-parameter methods + `ANGULAR_LOADER` / `SPIN_SPINNER_LOADER` fields | `UIEngine` wait methods |
| `TableHandler` (class + all methods) | `UIEngine` + `LocatorDescriptor` |
| `SeleniumEngine(WebDriver)` constructor | `SeleniumEngine(DriverFactory.Profile)` via factory |
| `SeleniumEngine.fromBy(By)` | `SeleniumLocatorBridge.fromBy(By)` (itself deprecated; migrate to element-based resolution) |
| `SeleniumLocatorBridge.fromBy(By)` | element-based or string-based locator resolution |

### Migration

| Old | New |
|---|---|
| `VOID.start()` | `VOID.builder().start()` |
| `VOID.start(DriverFactory.Profile.CHROME)` | `VOID.builder().profile(DriverFactory.Profile.CHROME).start()` |
| `VOID.builder().engine("selenium")` | `VOID.builder().engine(SeleniumEngine.ID)` |
| `new UIEngineFactory.create(config, driver)` | `UIEngineFactory.create(config, EngineBootstrap.fromProfile(profile))` |

---

## [0.3.0] - 2026-07-17

### Added

- **Element API Simplification & Boilerplate Reduction** (Phases 1-19 complete)

  **Boilerplate elimination**
  - `Element.getPrimaryLocator()` defaults to `PageName.EnumName.CONSTANT_NAME.ROLE` derived
    from the Java type hierarchy -- no constructor string argument needed for locator keys
  - `Element.getArgs()` defaults to `NO_ARGS` -- eliminates the universal `return new Object[0]`
    override from every static element
  - `Element.getDisplayText()` defaults to a word-transform of the enum constant name
    (`LOGIN_BUTTON` → `"Login Button"`) -- no display text override needed for the common case
  - Static elements (no runtime args, no custom display text) now require only the bare enum
    declaration -- no constructor, no field, no overrides

  **Locator resolution architecture**
  - Deterministic repository convention -- VOID derives each page's repository path from its
    fully qualified type: `tests.demo.pages.DemoLoginPage` →
    `src/main/resources/tests/demo/pages/DemoLoginPage/locators.json`; no path constant,
    no annotation, no `getExternalFileName()` override in the common case
  - `LocatorContext` -- new interface abstracting repository discovery; default implementation
    (`DefaultLocatorContext.INSTANCE`) applies the convention; injectable via builder for
    custom project layouts
  - Repository cache -- `ConcurrentHashMap<Class<?>, LocatorRepository>` caches the resolved
    repository per page type; constant-time element lookups after first resolution
  - Three-step locator resolution order: (1) `getExternalFileName()` override,
    (2) deterministic convention, (3) hardcoded fallback
  - Mixed strategies -- pages may combine convention-resolved and hardcoded elements freely
    within the same interface

  **Developer tooling**
  - Properties template generator (`JsonMigratorCli --sync <ClassName>`) -- generates a
    pre-populated `locators.properties` with all keys derived from enum declarations;
    merge-with-preserve: adds new keys, retains existing values, warns on orphaned keys;
    `--prune` flag for explicit orphan removal
  - Runtime Repository Generation -- existing JSON Migration CLI repositioned as the named
    pipeline step that converts a filled `locators.properties` into `locators.json`

  **Locator families**
  - `LocatorFamily` -- marker interface for enums whose constants share one locator template;
    VOID derives the runtime argument from the constant name via the same word-transform as
    `getDisplayText()`
  - `AdvancedLocatorFamily` -- extends `LocatorFamily`; per-constant constructor overrides the
    derived label for values that cannot be derived (acronyms, symbols, domain-specific text);
    constants without a constructor continue to use automatic derivation
  - `SwitchLocatorFamily` -- exhaustive-switch alternative for a centralised semantic mapping;
    the compiler reports missing branches when a new constant is added
  - `getExternalFileName()` preserved as an advanced override for shared repositories, generated
    repositories, or custom project layouts; takes precedence over the convention when non-null

### Changed

- **`Element.EMPTY_ARGS`** renamed to `Element.NO_ARGS` -- name now communicates intent rather
  than state
- **Locator key format** -- capability locator method defaults now produce fully-qualified keys
  in `PageName.EnumName.CONSTANT.ROLE` format using `ElementRole.name()` as the role token
  (e.g. `DemoLoginPage.Credentials.USERNAME_INPUT.INPUT`); existing `.properties` files must
  be regenerated via `JsonMigratorCli --sync`
- **Capability interfaces** -- forwarding implementations that delegated to `Element` without
  adding behavior removed; interfaces now contain only capability-specific declarations and
  action emission

### Migration

| Old | New |
|-----|-----|
| `Element.EMPTY_ARGS` | `Element.NO_ARGS` |
| `USERNAME_INPUT("USERNAME_INPUT")` constructor | `USERNAME_INPUT` bare constant |
| `return new Object[0]` in `getArgs()` | delete the override |
| `String LOCATOR_FILE = "..."` constant | delete; convention resolves path automatically |
| `getExternalFileName() { return LOCATOR_FILE; }` | delete; only override for non-standard layouts |
| Bare `CONSTANT.ROLE` key in `.properties` | `PageName.EnumName.CONSTANT.ROLE` -- run `JsonMigratorCli --sync <PageClass>` to regenerate |

### Planned

- OOP violations remediation Phases 1-4 -- **Complete (2026-07-23)** (see `docs/plan/done/oop-violations-remediation/`)
  - Phase 1: remove `instanceof HookChainAction` from `Action` defaults; promote label methods
  - Phase 2: replace `(Enum<?>) this` casts in `Element`; move `capability()` to `Element`; default `Listable.getIndex()`
  - Phase 3: replace `instanceof` dispatch chains in `VoidDSL` with typed overloads and `EnumMap` dispatch
  - Phase 4: `UIEngineFactory` registry map; `LocatorRoles` dedup helper; `Via` capability-helper reduction

---

## [0.2.0] — 2026-07-12

### Changed

- **Open/Closed Principle applied to three action extension points**
  - `ElementAction.operationLabel()` — capability-based `switch` replaced with class-name derivation: strips the `"Action"` suffix and lowercases the first character (`ClickAction` → `"click"`, `SearchAndSelectAction` → `"searchAndSelect"`). Anonymous subclasses (e.g., `ElementActions.of()`) return `"perform"`. Adding a new concrete action subclass requires no change to `ElementAction`.
  - `ActionProfiles.safeProfileFor(ActionCapability)` and `reliableProfileFor(ActionCapability)` deleted — the central dispatch switches are gone. Profile ownership now lives directly in each concrete action subclass via `defaultSafeProfile()` / `defaultReliableProfile()` overrides: click-family (`ClickAction`, `ToggleAction`, `CheckAction`) → `CLICKABLE_*`; type-family (`TypeAction`, `ClearAction`, `AppendTypeAction`, `TypeAndPressAction`, `TypeSearchAction`, `SubmitSearchAction`) → `TYPEABLE_*`; select-family (`OpenAction`, `SelectAction`, `SelectByTextAction`, `SelectByValueAction`, `SearchAndSelectAction`) → `SELECTABLE_*`; read-family (`HoverAction`, `UploadAction`, `ReadTextAction`) inherit `DEFAULT_*` from the base class. Adding a new action type is fully self-contained — no changes to `ActionProfiles` or `ElementAction`.
  - `ElementActions.capabilityFor()` — collapsed. The three role-based fallbacks (`ElementRole.INPUT → TYPEABLE`, `LIST → SELECTABLE`, `TRIGGER → CLICKABLE`) were dead code for all production elements (all implement `ActionCapabilityProvider`). Simplified to: `instanceof ActionCapabilityProvider → return p.capability()`, else return `UNKNOWN`. Adding a new `ElementRole` requires no change here.

### Added

- **Action profiles — Phase 1 (profile API consolidation)**
  - `Action.safely()` — applies the SAFE profile (capability-aware hooks: before/after chosen by Clickable / Typeable / Selectable)
  - `Action.debug()` — applies the DEBUG profile (`LOG_INTENT` + `HIGHLIGHT_ELEMENT` before, `HIGHLIGHT_ELEMENT` after)
  - `Action.raw()` — applies the RAW profile (no hooks; bare `perform()` only)
  - `Action.using(ActionProfile)` — applies any custom or built-in profile
  - `ActionProfile.name()` — default `"custom"`; named presets (SAFE, DEBUG, RAW, FAST, VISUAL, RELIABLE) override with stable identifiers
  - `Profile.builder()` / `ActionProfile.builder()` — fluent builder for custom profiles
  - `Profiles.FAST`, `Profiles.VISUAL`, `Profiles.RELIABLE` — additional built-in presets

- **Capability-driven hook selection — Phase 4**
  - `ActionProfiles.DEFAULT_SAFE` — shared immutable `ActionProfile` (wait-for-visible before, no after); the switch-free fallback for capabilities without a specific safe profile
  - `ElementActions.capabilityFor()` — refactored: first checks `ActionCapabilityProvider.capability()` via pattern match; all 14 capability types now report accurate metadata through the action pipeline (11 previously returned UNKNOWN)
  - `ElementAction` — new abstract base class implementing the Template Method pattern; `perform()` is final (resolve then execute); `safely()`, `debug()`, `reliable()`, `raw()` are final fluent APIs; `execute()` is the single abstract primitive for subclasses

- **Documentation updated — Phase 20**
  - `docs/architecture/system-overview.md` — Decision Traceability extended with ADR-012/013/014; Action/Flow section updated to concrete action subclasses; project structure listing updated; Architecture Invariants updated with layering and metadata-only rules
  - `docs/architecture/core-packages.md` — `core.actions` section rewritten: full concrete action class table, updated "How it works" steps, layering rule added
  - ADR-013 added: Architectural Layering Principle — capabilities describe, actions execute; derived rules for code review
  - ADR-014 added: Concrete Actions over Anonymous Lambdas — motivation, comparison table, covariant return types, consequences

- **ElementActions factory scope settled — Phase 19 (ADR-012)**
  - `ReadTextAction` added — 17th concrete action subclass; `ReadOnly.readText()` now returns `ReadTextAction` directly, consistent with the Phase 14/15 pattern
  - All 16 production capability interfaces now emit typed concrete subclasses; no production code calls `ElementActions.of()`
  - `ElementActions` marked `@Internal` — factory retained for test infrastructure (custom-operation lambdas that concrete subclasses cannot satisfy) and framework-internal edge cases
  - ADR-012 documents the decision and audit findings (15 call sites: 1 production migrated, 14 test infrastructure retained)

- **ElementRole audit — Phase 18 (investigation)**
  - Audited all 16 concrete action subclasses for ElementRole necessity
  - Decision: **Keep** — ElementRole is a public API contract (`UIEngine.resolve()`, `LocatorResolver.resolveDescriptor()`, `Element.getAllLocatorRoles()`); cannot be removed without breaking changes
  - Single-role actions (12 of 16) hardcode their locator role at compile time in the constructor — this is correct and needs no change
  - Composite actions (`SelectAction`, `SearchAndSelectAction`) call `engine.resolve()` with secondary roles directly in `execute()` — this is correct named-key usage, not dispatch
  - No code changes required; architecture is sound

- **Capability-based profile dispatch eliminated — Phase 17**
  - `Profiles.SAFE` removed — had `before(Action)` and `after(Action)` switches on `action.capability()`
  - `Profiles.RELIABLE` removed — had `before(Action)` switch on `action.capability()`
  - `Profiles.fromName("SAFE")` and `fromName("RELIABLE")` fall back to `RAW`
  - `ActionProfiles.reliableProfileFor(ActionCapability)` added — mirrors `safeProfileFor`; four capability-specific reliable profile constants: `DEFAULT_RELIABLE`, `CLICKABLE_RELIABLE`, `TYPEABLE_RELIABLE`, `SELECTABLE_RELIABLE` *(both dispatch methods later removed — see Changed section above)*
  - `ElementAction.reliable()` now calls `using(defaultReliableProfile())` — polymorphic, same pattern as `safely()`
  - `ElementAction.defaultReliableProfile()` calls `ActionProfiles.reliableProfileFor(capability)` — no static Profiles reference
  - `Action.safely()` default updated to `using(ActionProfiles.DEFAULT_SAFE)` — applies wait-for-visible for plain lambda actions
  - `Profiles` now contains only action-independent presets: `RAW`, `DEBUG`, `FAST`, `VISUAL`
  - Profile resolution is 100% polymorphic: no `switch(action.capability())` outside of `ActionProfiles` dispatch methods

- **Execution policy deleted from capability interfaces — Phase 16**
  - Re-audit post Phase 14/15 confirms zero execution policy in `elements/api/capability`: no `safeProfile()`, no `reliableProfile()`, no `*_SAFE_PROFILE` constants
  - `ActionCapabilityProvider` contains only `capability()` — pure metadata interface
  - All profile dispatch lives exclusively in `ActionProfiles` (package-private, `core.actions`)
  - `ElementAction.defaultSafeProfile()` is the single hook-wiring entry point for action subclasses

- **Capability action emission — Phase 15**
  - `Clickable.click()` returns `ClickAction` (was anonymous `ElementActions.of()` lambda)
  - `Checkable.toggle()` returns `ToggleAction`; `Checkable.set(boolean)` returns `CheckAction`
  - `Hoverable.hover()` returns `HoverAction`
  - `Typeable.type()`, `clear()`, `append()`, `typeAndPress()` return `TypeAction`, `ClearAction`, `AppendTypeAction`, `TypeAndPressAction`
  - `Selectable.open()`, `select()`, `selectByText()`, `selectByValue()` return `OpenAction`, `SelectAction`, `SelectByTextAction`, `SelectByValueAction`
  - `SearchField.typeSearch()` returns `TypeSearchAction`; `submitSearch()` returns `SubmitSearchAction`
  - `SearchableDropdown.searchAndSelect()` returns `SearchAndSelectAction`
  - `Uploadable.upload()` returns `UploadAction` (was plain lambda)
  - All concrete return types remain polymorphically assignable to `Action` — no call sites broken
  - `ElementActions`, `Action`, and `java.time.Duration` imports removed from all updated capability interfaces

- **Concrete action subclasses — Phase 14**
  - `ClickAction(Clickable)` — `engine.click()`, TRIGGER role, CLICKABLE capability
  - `ToggleAction(Checkable)` — unconditional click, TRIGGER, CHECKABLE
  - `CheckAction(Checkable, boolean)` — conditional click when state differs, TRIGGER, CHECKABLE
  - `HoverAction(Hoverable)` — `engine.hover()`, TEXT role, HOVERABLE capability
  - `TypeAction(Typeable, String)` — `engine.type()`, INPUT role, TYPEABLE capability
  - `ClearAction(Typeable)` — `engine.clear()`, INPUT, TYPEABLE
  - `AppendTypeAction(Typeable, String)` — `engine.appendType()`, INPUT, TYPEABLE
  - `TypeAndPressAction(Typeable, String, String)` — `engine.type()` then `sendKey()`, INPUT, TYPEABLE
  - `OpenAction(Selectable)` — clicks TRIGGER only, SELECTABLE capability
  - `SelectAction(Selectable)` — composite: click TRIGGER + `waitForOverlay` + click LIST, SELECTABLE
  - `SelectByTextAction(Selectable, String)` — `engine.selectByVisibleText()`, LIST, SELECTABLE
  - `SelectByValueAction(Selectable, String)` — `engine.selectByValue()`, LIST, SELECTABLE
  - `UploadAction(Uploadable, String)` — `engine.uploadFile()`, INPUT, UPLOADABLE
  - `TypeSearchAction(SearchField, String)` — `engine.type()`, SEARCH_INPUT, SEARCH_FIELD
  - `SubmitSearchAction(SearchField)` — `engine.click()`, SEARCH_BUTTON, SEARCH_FIELD
  - `SearchAndSelectAction(SearchableDropdown, String)` — composite: click TRIGGER + type SEARCH_INPUT + `waitForVisible` + click SEARCH_RESULT, SEARCHABLE_DROPDOWN
  - All classes are `final`; profiles declared via `defaultSafeProfile()` / `defaultReliableProfile()` overrides — constants owned by `ActionProfiles` and referenced locally in each subclass (click-family → `CLICKABLE_*`, type-family → `TYPEABLE_*`, select-family → `SELECTABLE_*`)

- **Execution policy moved to action layer — Phase 5 (SoC correction)**
  - `ActionProfiles.safeProfileFor(ActionCapability)` — package-private static method; maps each capability to its safe profile constant; execution policy lives in `core.actions`, not in capability interfaces
  - `ActionProfiles.CLICKABLE_SAFE` — `[WAIT_FOR_ELEMENT_CLICKABLE]` before, `[WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]` after
  - `ActionProfiles.TYPEABLE_SAFE` — `[CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE]` before, `[HIGHLIGHT_ELEMENT]` after
  - `ActionProfiles.SELECTABLE_SAFE` — `[WAIT_FOR_ELEMENT_VISIBLE, WAIT_FOR_ELEMENT_CLICKABLE, WAIT_FOR_ANGULAR_LOADER]` before, `[HIGHLIGHT_ELEMENT]` after
  - `ElementAction.safely()` calls `using(defaultSafeProfile())`; `defaultSafeProfile()` is a protected template method; concrete subclasses override to declare their profile directly (`ClickAction` → `ActionProfiles.CLICKABLE_SAFE`, etc.) rather than going through a central dispatch method
  - `ActionCapabilityProvider` reduced to a single-method interface — `capability()` only; execution policy is not a capability concern
  - `Clickable`, `Typeable`, `Selectable`, `SearchField`, `SearchableDropdown` no longer contain `ActionProfile` constants or `safeProfile()` overrides; capability interfaces are pure structural contracts
  - Open/Closed at the action level: a new action type with different safe hooks overrides `defaultSafeProfile()` without touching capability interfaces or framework files

- **Capability self-description — Phase 3**
  - `core.actions.ActionCapabilityProvider` — new interface; capability interfaces implement it to self-describe without a registry
  - `ActionCapability` enum expanded from 4 to 15 values: added HOVERABLE, CHECKABLE, UPLOADABLE, SEARCHABLE, SEARCH_FIELD, SEARCHABLE_DROPDOWN, READ_ONLY, TABLE, EDITABLE_TABLE, LISTABLE, MULTI_SELECTABLE alongside the existing CLICKABLE, TYPEABLE, SELECTABLE, UNKNOWN
  - All 14 capability interfaces (`Clickable`, `Typeable`, `Selectable`, `Hoverable`, `Checkable`, `Uploadable`, `MultiSelectable`, `Searchable`, `SearchField`, `SearchableDropdown`, `ReadOnly`, `Table`, `EditableTable`, `Listable`) implement `ActionCapabilityProvider` and return their canonical constant
  - No behavioral change — capability metadata is for logging, tracing, diagnostics, serialization only

- **Action execution trace — Phase 2 (observability)**
  - `core.actions.trace.ActionTrace` — immutable record of a single action execution (element, operation, profile, hooks, timing, status, failure)
  - `core.actions.trace.TraceStatus` — outcome enum: `SUCCESS`, `FAILED`, `HOOK_FAILED`
  - `core.actions.trace.ActionTraceLogger` — formats and emits trace output at DEBUG level; resolves named `Before`/`After` constants via reflection
  - `HookedAction` now instruments every execution: records hook order, distinguishes `HOOK_FAILED` from `FAILED`, captures elapsed time, emits formatted trace block

- **`VOID` session façade — ADR-011**
  - `VOID.navigateTo(String url)` — navigate without touching the engine directly
  - `VOID.getCurrentUrl()` — read URL from the session façade
  - `VOID.getTitle()` — read page title from the session façade
  - `VOID.refresh()` — reload the page from the session façade
  - `VOID.run(Action action)` — execute a single Action without wrapping in `Flow.of()`
  - `UIEngine.getTitle()` — new engine contract method
  - `UIEngine.refresh()` — new engine contract method
  - `SeleniumEngine` implements `getTitle()` and `refresh()`

- **`VOID.shutdown()` — session-scoped teardown**
  - Now calls `engine.shutdown()` (releases browser) then `DriverContext.removePrimary()` (cleans ThreadLocal)
  - Previously called `DriverManager.quitAll()` which killed **all** drivers on the thread — a multi-session isolation bug
  - Multi-session tests can now call `admin.shutdown()` without affecting `customer`

- **ArchUnit façade boundary enforcement** (`FacadeBoundaryRulesTest`)
  - Rule 1: No `UIEngine` fields in `tests.*` classes — use the VOID façade instead
  - Rule 2: No direct `new FlowExecutor(engine)` construction in `tests.*` — use `app.run()`
  - Rule 3: No `FlowExecutor` fields in `tests.*` classes
  - All rules include actionable `because()` messages pointing to ADR-011
  - `archunit:1.3.0` added as a test-scoped dependency

### Changed

- **`VoidDemo.loginWithHookedActions()`** — refactored to use `safely()` as primary pattern; inline after-hook shows how to extend a profile
- **`core/actions/README.md`** — added Profiles section with capability expansion table and builder examples; `withHooks()` moved to Manual/Advanced
- **`docs/architecture/hooks-pipeline.md`** — `safely()` promoted as primary modern path in overview, table, and best-practices section

- **`VOID` Javadoc** — rewritten to reflect session-façade model with multi-session examples
- **`FlowExecutor` Javadoc** — updated to prefer `VOID.run()` over direct construction
- **`VoidDemo`** — migrated to session façade: removed `UIEngine` and `FlowExecutor` fields; all interactions now via `app.*`

### Deprecated

The following are deprecated since **2.1** and scheduled for removal in **3.0**:

| Method | Replacement |
|--------|------------|
| `VOID.interaction()` | `app.run(flow)` / `app.run(action)` |
| `VOID.getDriver()` | `app.getEngine().getNativeDriver()` (escape hatch) |
| `VOID.getContext()` | engine-level abstractions |

### Migration

| Old pattern | New pattern |
|-------------|-------------|
| `engine.navigateTo(url)` | `app.navigateTo(url)` |
| `engine.getCurrentUrl()` | `app.getCurrentUrl()` |
| `new FlowExecutor(engine).run(flow)` | `app.run(flow)` |
| `executor.run(action)` | `app.run(action)` |
| `app.interaction().clickOn(element)` | `app.run(element.click())` |
| `app.getDriver()` | `app.getEngine().getNativeDriver()` |
| `admin.shutdown()` then `customer.run(flow)` → crash | Now safe — each shutdown is session-scoped |

### Documentation

- Added `docs/audits/facade-boundary-audit-2026-05.md` — façade boundary audit (10 findings, A–D execution plan)
- Added `docs/decisions/accepted/011-void-facade-boundary.md` — ADR-011

---

## [0.1.0]

### Removed (binary-breaking)

- **Locator façades**: `core.resolvers.locator.LocatorResolverV1`,
  `ElementLocatorResolverV1`, `LocatorReader`. Use
  `core.resolvers.locator.api.LocatorResolvers#strict()` (or `#legacyPadded()`)
  with `LocatorRequest.of(file, key, args)` instead.
- **Legacy logger class**: `core.utils.CustomLogger`. The active implementation
  is `core.logging.CustomLogger`.
- **Legacy element-API adapters**: `getAllLocators()` default method on
  `Element` and 13 sub-interfaces. Use the type-safe
  `getAllLocatorRoles()` (returns `Map<ElementRole, String>`).
- **Cross-layer test-flow helpers**: `TableHandler#insertNewRecords`,
  `DataGenerator#saveFieldTypeSamples`, `DataGenerator#saveFieldTypeMapAsJson`.
  Move I/O orchestration to step definitions / page objects; for JSON output
  use `JsonLogger.Write.MapWriter#writeFlatMap` directly.
- **Misc one-liners**: `Interactions#searchThisList` (alias of
  `searchAndGetResults`), `DriverFactory#createEmptyTemplate` (alias of
  `createPropertiesTemplate`), `JsonLocatorMigrator#main` (moved to
  `JsonMigratorCli#main`), `ThemeColors#theme()` (renamed to `builder()`).
- **`core.logging.CustomLogger` color constants** (~50 `FG_*`/`BG_*`/`ANSI_*`
  re-exports) and `Experimental#fgFromBg`. Import
  `core.logging.ansi.AnsiColors.*` directly and use `RESET`/`BOLD`/`DIM`/`ITALIC`
  in place of the `ANSI_*` aliases.
- **`AnsiColors#FG_DIM_WHITE`** (use `FG_BRIGHT_BLACK`),
  **`LoggerContext#TS_FMT_get()`** (use `LogConfig.current().getTsFormat()`),
  **`LoggerContext#MAX_COL_WIDTH`** (use
  `LogConfig.current().getTableCellLimit()`).

### Fixed

- `src/testNgXml/testng.xml` no longer references the missing
  `registry.EnumClassRegistryTest` and `core.utils.TestListener`. The suite
  now picks up tests via package globs (`core.*`, `elements.*`), so
  `mvn test` runs the full unit-test set without extra flags.

### Migration notes

After bumping to `2.0-SNAPSHOT`:

| Old call                                                         | New call                                                                                  |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `LocatorResolverV1.getLocator(file, key, args)`                  | `LocatorResolvers.strict().resolve(LocatorRequest.of(file, key, args))`                   |
| `LocatorResolverV1.getLocator(element)`                          | `LocatorResolvers.strict().resolve(element)`                                              |
| `ElementLocatorResolverV1.getLocator(element)`                   | `LocatorResolvers.legacyPadded().resolve(element)` *(or `strict()` if pad-last unneeded)* |
| `import static core.utils.CustomLogger.*`                        | `import static core.logging.CustomLogger.*`                                               |
| `CustomLogger.FG_RED` / `CustomLogger.ANSI_RESET`                | `AnsiColors.FG_RED` / `AnsiColors.RESET`                                                  |
| `CustomLogger.Experimental.fgFromBg(s)`                          | `CustomLogger.Experimental.fgFromStyle(s)`                                                |
| `element.getAllLocators()` (`Map<String,String>`)                | `element.getAllLocatorRoles()` (`Map<ElementRole,String>`)                                |
| `LoggerContext.TS_FMT_get()` / `LoggerContext.MAX_COL_WIDTH`     | `LogConfig.current().getTsFormat()` / `LogConfig.current().getTableCellLimit()`           |
| `AnsiColors.FG_DIM_WHITE`                                        | `AnsiColors.FG_BRIGHT_BLACK`                                                              |
| `Interactions#searchThisList(field, term)`                       | `Interactions#searchAndGetResults(field, term)`                                           |
| `DriverFactory.createEmptyTemplate()`                            | `DriverFactory.createPropertiesTemplate(Profile.DEFAULT, true, true, false, false)`       |
| `JsonLocatorMigrator.main(args)`                                 | `JsonMigratorCli.main(args)`                                                              |
| `ThemeColors.theme()...build()`                                  | `ThemeColors.builder()...build()`                                                         |

---

[Unreleased]: https://github.com/Aryan-Vashishth/void-framework/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/Aryan-Vashishth/void-framework/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/Aryan-Vashishth/void-framework/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/Aryan-Vashishth/void-framework/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Aryan-Vashishth/void-framework/releases/tag/v0.1.0
