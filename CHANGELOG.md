# Changelog

## [Unreleased] — 2026-07-03

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

- **Capability-based profile dispatch eliminated — Phase 17**
  - `Profiles.SAFE` removed — had `before(Action)` and `after(Action)` switches on `action.capability()`
  - `Profiles.RELIABLE` removed — had `before(Action)` switch on `action.capability()`
  - `Profiles.fromName("SAFE")` and `fromName("RELIABLE")` fall back to `RAW`
  - `ActionProfiles.reliableProfileFor(ActionCapability)` added — mirrors `safeProfileFor`; four capability-specific reliable profile constants: `DEFAULT_RELIABLE`, `CLICKABLE_RELIABLE`, `TYPEABLE_RELIABLE`, `SELECTABLE_RELIABLE`
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
  - All classes are `final`; profiles inherited via `ElementAction.defaultSafeProfile()` — no profile constants duplicated in subclasses

- **Execution policy moved to action layer — Phase 5 (SoC correction)**
  - `ActionProfiles.safeProfileFor(ActionCapability)` — package-private static method; maps each capability to its safe profile constant; execution policy lives in `core.actions`, not in capability interfaces
  - `ActionProfiles.CLICKABLE_SAFE` — `[WAIT_FOR_ELEMENT_CLICKABLE]` before, `[WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]` after
  - `ActionProfiles.TYPEABLE_SAFE` — `[CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE]` before, `[HIGHLIGHT_ELEMENT]` after
  - `ActionProfiles.SELECTABLE_SAFE` — `[WAIT_FOR_ELEMENT_VISIBLE, WAIT_FOR_ELEMENT_CLICKABLE, WAIT_FOR_ANGULAR_LOADER]` before, `[HIGHLIGHT_ELEMENT]` after
  - `ElementAction.safely()` calls `using(defaultSafeProfile())`; `defaultSafeProfile()` is a protected template method that returns `ActionProfiles.safeProfileFor(capability)` by default; subclasses override only when safe behavior differs (e.g. `DoubleClickAction`)
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

## 2.0-SNAPSHOT (chore/remove-deprecated-apis)

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
