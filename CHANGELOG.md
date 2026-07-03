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
