# Changelog

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

