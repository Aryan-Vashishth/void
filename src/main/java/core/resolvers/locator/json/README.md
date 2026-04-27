# `core.resolvers.locator.json` — JSON Locator Package

JSON-backed locator readers and migration utilities for the `core.resolvers.locator` layer.

## Overview

This package provides two core capabilities:

1. **Runtime reading** of pre-built JSON locator files (used by the framework during test execution).
2. **Migration / code-generation** of JSON locator files from enum-based page-element descriptors and their companion `.properties` bundles.

The original implementation was a single ~300-line monolith (`JsonLocatorMigrator`). It was decomposed in **Phase 5** into five focused classes following the Single Responsibility Principle.

## Class Inventory

| Class | Responsibility |
|---|---|
| [`JsonLocatorReader`](JsonLocatorReader.java) | Runtime shim — loads a JSON locator file from the classpath and returns the **raw** (unformatted) XPath template for a given `(fileName, key)` pair. |
| [`JsonNodeLookup`](JsonNodeLookup.java) | Pure traversal helpers for Jackson `JsonNode` trees. Supports **dot-path** lookup (`a.b.c`), **deep-find** fallback, and **text coercion**. |
| [`JsonLocatorMigrator`](JsonLocatorMigrator.java) | Public façade for building and persisting resolved JSON locator files. Provides `buildResolvedJson`, `writeResolvedJson`, and `writeJsonString` APIs. |
| [`JsonTreeBuilder`](JsonTreeBuilder.java) | Recursive class-tree walker that produces a Jackson `ObjectNode` mirroring the nested-enum structure of a root element class. Delegates enum scanning to `EnumLocatorScanner`. |
| [`EnumLocatorScanner`](EnumLocatorScanner.java) | Scans enum constants implementing `Element` and emits JSON entries per constant — simple strings for single-role elements, nested objects for multi-role elements (e.g. `Dropdown` with `TRIGGER` / `LIST`). |
| [`PropertiesIndex`](PropertiesIndex.java) | Per-migration-run cache of `.properties` files. Loads and merges TEST-over-MAIN classpath layers (TEST wins on conflict). Replaces the former `ThreadLocal` approach. |
| [`JsonMigratorCli`](JsonMigratorCli.java) | Command-line entry point for running migrations outside the test harness. |

## How It Works

### Runtime (Reading)

```
Test Step
  └─► ElementLocatorResolver
        └─► JsonLocatorReader.getRaw(fileName, key)
              ├─ loads  locators/json/<fileName>.json  from classpath
              └─ delegates to JsonNodeLookup.findText(root, key)
                    ├─ 1. dot-path traversal ("Section.CONSTANT")
                    └─ 2. deep-find fallback (field name anywhere in tree)
```

### Migration (Writing)

```
JsonLocatorMigrator.writeResolvedJson(MyPageElements.class)
  └─► JsonTreeBuilder.build(rootClass)
        ├─ walks nested classes recursively
        ├─ for each enum → EnumLocatorScanner.writeInto(node, enumClass)
        │     ├─ loads .properties via PropertiesIndex
        │     └─ resolves raw keys → XPath values
        └─ returns ObjectNode tree
  └─► serializes to pretty-printed JSON
  └─► writes to  src/main/resources/locators/json/<name>-locators.json
```

## CLI Usage

```bash
# Print resolved JSON to stdout
java core.resolvers.locator.json.JsonMigratorCli --print  elements.AccountMappingElements

# Write to the default output directory (src/main/resources/locators/json/)
java core.resolvers.locator.json.JsonMigratorCli --write  elements.AccountMappingElements

# Write to a specific file
java core.resolvers.locator.json.JsonMigratorCli --write  elements.AccountMappingElements  path/to/output.json
```

## Example Output

A migration of `AccountMappingElements` produces a JSON file like:

```json
{
  "AccountMappingElements" : {
    "Header" : {
      "PAGE_TITLE" : "//h1[contains(@class,'page-title')]",
      "BREADCRUMB" : "//nav[@aria-label='breadcrumb']//li[last()]"
    },
    "SearchBar" : {
      "ACCOUNT_SEARCH" : {
        "SEARCH_INPUT" : "//input[@placeholder='Search accounts...']",
        "SEARCH_BUTTON" : "//button[@aria-label='Search']"
      }
    },
    "FilterPanel" : {
      "StatusDropdown" : {
        "USER_STATUS" : {
          "TRIGGER" : "//mat-select[@formcontrolname='status']",
          "SEARCH_INPUT" : "//input[@placeholder='Filter statuses...']"
        }
      }
    }
  }
}
```

Locator values are resolved from companion `.properties` files (e.g. `account-mapping-elements.properties`) where enum constants reference property keys rather than raw XPaths.

## Key Design Decisions

- **Dot-path + deep-find fallback** — `JsonNodeLookup` first attempts strict dot-path traversal; if that misses, it performs a depth-first search. This supports both qualified (`FilterPanel.StatusDropdown.USER_STATUS`) and simple (`USER_STATUS`) key styles.
- **TEST-over-MAIN merge** — `PropertiesIndex` loads both TEST and MAIN classpath variants, with TEST values taking precedence. This mirrors the runtime `ElementLocatorResolverV1` contract.
- **No thread-local state** — Each migration creates a fresh `PropertiesIndex` instance, eliminating cross-run contamination and thread-local memory leaks.
- **CLI separated from library** — `JsonMigratorCli` is isolated so `JsonLocatorMigrator` can be unit-tested without `System.exit` side effects.

