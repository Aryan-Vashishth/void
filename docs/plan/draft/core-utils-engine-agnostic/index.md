# core-utils-engine-agnostic

**Branch:** `initiative/core-utils-engine-agnostic`
**Identified:** 2026-07-20 (architecture-rules.md audit on hotfix/engine-decoupling-final-audit)
**Governing ADR:** ADR-007 (UIEngine execution authority), ADR-018 (engine-agnostic layers)
**Violations addressed:** I1-A (DOMUtils), I1-B (TableHandler), I1-C (WaitUtils) from `docs/audits/backlog/violations/`

---

## Problem

Three utility classes in `core.utils.web` bypass `UIEngine` and call `WebDriver` directly
in non-deprecated methods, violating ADR-007:

| File | Violation | Risk |
|---|---|---|
| `DOMUtils.java` | `DriverContext.getDriver()` + direct `JavascriptExecutor`, `Actions`, `driver.switchTo()` | High |
| `WaitUtils.java` | `DriverContext.getDriver()` + `FluentWait<WebDriver>`, `driver.findElements()`, hardcoded Angular CDK selectors as `By` fields | High |
| `TableHandler.java` | `DriverContext.getDriver()` + `driver.findElements()` + deprecated `resolve()` path returning `By` | High |

All active production callers in `CommonStepDef.java` are commented out. No active
call sites remain in non-deprecated code paths outside the utils package itself.
The utilities remain live API that callers could add -- hence the ADR-007 violation.

---

## Scope

| In scope | Out of scope |
|---|---|
| Add 3 missing UIEngine methods (`switchToFrame`, `switchToDefaultContent`, `sendKeys`) | Full table-read API on UIEngine (no concrete callers, premature) |
| Implement all three in SeleniumEngine | DriverFactory OCP violation (logged separately, Low risk) |
| Deprecate all public DOMUtils methods pointing to UIEngine equivalents | Migration of `core.adapters.cucumber.CommonStepDef` callers (already commented out) |
| Deprecate ANGULAR_LOADER / SPIN_SPINNER_LOADER `By` fields in WaitUtils | Removal of deprecated utils (separate cleanup workstream) |
| Deprecate all public WaitUtils By-based methods pointing to UIEngine equivalents | |
| Fix TableHandler internal `resolve()` -> `resolveDescriptor()` migration | |
| Deprecate all public TableHandler methods | |

---

## Why not add a full table API to UIEngine?

`getColumnHeaders(TableElementV1)` and `getRow(...)` require a multi-element collection
read with relative child queries (`row.findElements(By.xpath("./td"))`). Adding this
to UIEngine would require:
1. `List<String> getTextList(LocatorDescriptor)` for header reads
2. A scoped element query model for cell reads within rows
3. All future engines to implement table-reading behavior

No active caller exists outside the commented-out CommonStepDef. Architecture should
emerge from repeated requirements, not anticipated ones (Stability Rule 4). The table
API is deferred until concrete use cases emerge.

---

## Phase overview

| Phase | Changes | Violations addressed |
|---|---|---|
| 1 | Add `switchToFrame`, `switchToDefaultContent`, `sendKeys` to UIEngine + SeleniumEngine | Prerequisite for Phase 2 |
| 2 | Deprecate all DOMUtils methods | I1-A |
| 3 | Deprecate ANGULAR_LOADER/SPIN_SPINNER_LOADER fields; deprecate all WaitUtils public methods | I1-C |
| 4 | Fix TableHandler resolve() -> resolveDescriptor(); deprecate all TableHandler methods | I1-B |

Phase docs:
- [Phase 1 -- UIEngine extension](phase-1-uiengine-extension.md)
- [Phase 2 -- DOMUtils deprecation](phase-2-domutils-deprecation.md)
- [Phase 3 -- WaitUtils deprecation](phase-3-waitutils-deprecation.md)
- [Phase 4 -- TableHandler migration and deprecation](phase-4-tablehandler.md)

---

## Dependency rationale

Phase 1 must come first: DOMUtils deprecation (Phase 2) points callers to
`UIEngine.switchToFrame()` and `UIEngine.sendKeys()` -- these must exist before
the Javadoc references compile.

Phases 2-4 are independent of each other after Phase 1.

---

## Verification

```
# After Phase 1
mvn compile -q
grep -n "switchToFrame\|switchToDefaultContent\|sendKeys" src/main/java/core/engine/UIEngine.java  # must appear
grep -n "@Override" src/main/java/core/engine/selenium/SeleniumEngine.java | grep -A1 "switch\|sendKeys"  # must appear

# After Phase 2
mvn compile -q
grep -n "@Deprecated" src/main/java/core/utils/web/DOMUtils.java  # all public methods marked

# After Phase 3
mvn compile -q
grep -n "@Deprecated" src/main/java/core/utils/web/WaitUtils.java  # ANGULAR_LOADER + all public methods marked
grep -n "ANGULAR_LOADER\|SPIN_SPINNER" src/main/java/core/utils/web/WaitUtils.java  # must be @Deprecated

# After Phase 4
mvn compile -q
grep -n "\.resolve(" src/main/java/core/utils/web/TableHandler.java  # must be empty (replaced by resolveDescriptor)
grep -n "@Deprecated" src/main/java/core/utils/web/TableHandler.java  # all public methods marked
```

---

## Commit sequence

```
# Phase 1
feat(engine): add switchToFrame, switchToDefaultContent, sendKeys to UIEngine
feat(engine): implement switchToFrame, switchToDefaultContent, sendKeys in SeleniumEngine

# Phase 2
refactor(utils): deprecate all DOMUtils public methods; point to UIEngine equivalents

# Phase 3
refactor(utils): deprecate ANGULAR_LOADER and SPIN_SPINNER_LOADER By fields in WaitUtils
refactor(utils): deprecate all WaitUtils public By-based wait methods

# Phase 4
refactor(utils): switch TableHandler internal resolution from resolve() to resolveDescriptor()
refactor(utils): deprecate all TableHandler public methods
```
