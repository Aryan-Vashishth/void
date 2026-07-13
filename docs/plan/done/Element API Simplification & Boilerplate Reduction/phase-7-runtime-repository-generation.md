# Phase 7 — Runtime Repository Generation

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — repositioning of existing CLI; no new tool introduced

---

## Objective

Formally position the existing JSON Migration CLI as the "Runtime Repository Generation" step in the developer workflow, completing the pipeline from enum declarations to a runtime-ready JSON repository.

---

## Open Decision Consideration

**Open Decision 4** is relevant here:

> How far `LocatorRepository` abstracts the underlying source — and whether future repository types (YAML, remote) can be introduced at that level without touching the CLI or the runtime.

This phase does not need to resolve it, but the CLI's output should conform to whatever the `LocatorRepository` interface expects.

---

## Context

The existing CLI converts a filled properties file into a JSON repository:

```
locators.properties  →  CLI  →  locators.json
```

This capability already exists. This phase:
- Names it officially as "Runtime Repository Generation" in documentation, workflow descriptions, and developer guides
- Confirms it integrates correctly with the convention introduced in Phase 5
- Introduces Phase 5-compatible code changes that were not anticipated in the original plan

---

## Pipeline Position

```
Enum constants
      │
      ▼
Generated properties template  (Phase 6)
      │
Developer fills locator values
      │
      ▼
Runtime Repository Generation (this phase — existing CLI)
      │
      ▼
locators.json  →  Runtime
```

---

## Plan Correction: Code Changes Were Required

The original plan stated "Confirm no code changes to the CLI are required for Phase 5 compatibility." Investigation revealed two incompatibilities:

### 1. `EnumLocatorScanner.loadPropsFor()` — broken after Phase 5

**Problem:** `loadPropsFor()` called `e.getExternalFileName()` to find the properties file. After Phase 5, `getExternalFileName()` returns the conventional `.json` path — not a `.properties` path. The scanner could no longer find properties files for pages using the conventional layout.

**Fix applied:** `loadPropsFor()` now receives `enumClass`, derives the enclosing page class, and probes the conventional `.properties` path first via `ConventionalLocatorPath.forClassProperties(pageClass)`. The explicit-file fallback remains but is only honoured when `getExternalFileName()` returns a path ending in `.properties`.

**Resolution priority:**
1. Phase 5 conventional path: `pkg/ClassName/locators.properties`
2. Explicit `getExternalFileName()` — only if it ends with `.properties`

### 2. `JsonLocatorMigrator` — wrote to the old convention

**Problem:** `writeResolvedJson()` wrote to `src/main/resources/locators/json/{name}-locators.json`, not the Phase 5 conventional path.

**Fix applied:** Added `writeResolvedJsonConventional(Class<?> rootClass)` that resolves the output to `src/main/resources/{pkg/ClassName}/locators.json` via `ConventionalLocatorPath.forClass(rootClass)`.

### 3. `JsonMigratorCli` — no conventional mode

**Problem:** No way to invoke the conventional write path from the CLI.

**Fix applied:** Added `--write-conventional` mode:
```
java JsonMigratorCli --write-conventional  com.example.MyPageElements
```

---

## Affected Files

| File | Change |
|------|--------|
| `core/resolvers/locator/json/EnumLocatorScanner.java` | `loadPropsFor()` now receives `enumClass`; probes conventional properties path first |
| `core/resolvers/locator/json/JsonLocatorMigrator.java` | Added `writeResolvedJsonConventional(Class<?>)` |
| `core/resolvers/locator/json/JsonMigratorCli.java` | Added `--write-conventional` mode |
| `test: EnumLocatorScannerTest` | 2 new tests for conventional properties resolution |
| `test: JsonLocatorMigratorTest` | 3 new tests for conventional write path |
| `test: elements/fixture/ConventionalPropsPage` | New fixture with conventional `.properties` at classpath |

---

## Checklist

### Verification
- [x] Confirm the scanner correctly reads a properties file at the conventional path
- [x] Confirm the CLI writes the JSON file at the path the runtime will look up (Phase 5 convention)
- [x] Code changes to the CLI were required for Phase 5 compatibility (original plan was wrong)

### Tests
- [x] `EnumLocatorScanner`: conventional `.properties` path resolves XPath values correctly
- [x] `EnumLocatorScanner`: pages without properties files continue to emit raw constant names
- [x] `JsonLocatorMigrator`: `writeResolvedJsonConventional` target path follows `pkg/ClassName/locators.json`
- [x] `JsonLocatorMigrator`: `writeResolvedJsonTo` with conventional path produces valid JSON
- [x] `JsonLocatorMigrator`: conventional path places file inside `ClassName/` directory
- [x] Full suite (1022 tests) — 0 failures

---

## Exit Criteria

- The scanner probes the conventional `.properties` path before falling back to explicit file names
- The CLI can write to the conventional path via `--write-conventional`
- The full pipeline (enum → properties → CLI → JSON → runtime) works end-to-end

---

## What NOT to Do

- Do not introduce a new JSON generation tool — the existing CLI is the tool for this step
- Do not remove `writeResolvedJson()` — it remains the default for legacy (non-conventional) pages

---

*MIT License Copyright (c) 2025-2026 VOID Project*
