# Full-System Audit: core-utils-engine-agnostic

**Date:** 2026-07-20
**Branch:** `initiative/core-utils-engine-agnostic`
**Auditing ADRs:** ADR-007 (UIEngine execution authority), ADR-018 (engine-agnostic layers)
**Verdict:** CONDITIONAL PASS -- one hotfix required before merge

---

## Phases Verified

| Phase | Commit | Compile | Outcome |
|---|---|---|---|
| 1 -- UIEngine extension | `5b1a16b` | Pass | All three interface methods present; SeleniumEngine implements all three |
| 2 -- DOMUtils deprecation | `e7e7142` | Pass | Class + 6 public methods deprecated; Javadoc references correct UIEngine equivalents |
| 3 -- WaitUtils deprecation | `2c53804` | Pass | ANGULAR_LOADER, SPIN_SPINNER_LOADER, and all 16 public By-based methods deprecated |
| 4 -- TableHandler migration | `c7e7a06` | Pass | All 3 methods use `resolveDescriptor()` + `SeleniumEngine.toBy()`; class + 3 methods deprecated |
| VoidDSL I2 fix | (hotfix) | Pass | `verifyElementsAreVisible` uses `resolveDescriptor()` + `LocatorDescriptor`; no Selenium imports |

---

## Architecture Invariant Check

| Invariant | Status | Notes |
|---|---|---|
| UIEngine is the single execution authority (ADR-007) | **Partial** -- see F1 | All initiative-scoped violations addressed. One pre-existing gap surfaced (see F1). |
| Engine-agnostic layers are Selenium-free (ADR-018) | Pass | DOMUtils/WaitUtils/TableHandler are all deprecated and contain no new Selenium introductions. |
| LocatorDescriptor is Selenium-free (ADR-019) | Pass | No new `By` fields on LocatorDescriptor. |
| ElementSupport scope frozen (ADR-017) | Pass | Not touched. |
| VOIDBuilder is single-use (ADR-018) | Pass | Not touched. |

---

## Caller Verification

`CommonStepDef.java` contains 11 commented-out call sites spanning `DOMUtils`, `WaitUtils`, and `TableHandler`. All are commented out. No live non-deprecated production callers remain.

No call sites outside `CommonStepDef.java` were found for the deprecated utilities.

---

## Findings

### F1 -- WaitUtils.waitForCondition(WebDriver, ...) is public and non-deprecated [Hotfix required]

**File:** `WaitUtils.java:71`

```java
public static <T> T waitForCondition(
        WebDriver driver,
        ExpectedCondition<T> condition,
        By locator,
        Integer escapeTimeInSeconds,
        Integer pollingRateInMillis,
        Boolean enableLogging,
        String conditionLabel)
```

This method is public, non-deprecated, and accepts `WebDriver`, `ExpectedCondition<T>`, and `By` -- all Selenium-specific types. It exposes Selenium API outside `UIEngine` implementations, violating ADR-007.

Current callers: `DOMUtils.switchToFrame()` (deprecated) and WaitUtils deprecated internal methods only. No live non-deprecated callers. Risk is low today but grows with new test code that can reach this method.

**Fix:** `@Deprecated(forRemoval = true)` with Javadoc pointing callers to:
- `waitForCondition(String, Duration, Duration, Supplier<Boolean>)` for engine-agnostic condition checks
- UIEngine wait methods for element-specific waits

**Cost:** Minimal -- annotation + one-line Javadoc.

---

### F2 -- WaitUtils.waitForCondition(String, Duration, Duration, Supplier<Boolean>) uses WebDriver internally [Backlog]

**File:** `WaitUtils.java:110`

Public API is JDK-only (`String`, `Duration`, `Supplier<Boolean>`), so callers are not Selenium-coupled. Internal implementation uses `FluentWait<WebDriver>` for polling -- this is Selenium-specific underneath but not visible to callers.

Not deprecatable without providing a replacement polling mechanism. Not a callsite ADR-007 violation from the caller perspective. Defer until a `UIEngine.waitUntil(Supplier<Boolean>, Duration)` or similar is designed.

**Action:** Log to `docs/audits/backlog/violations/`.

---

### F3 -- Waiter.java is an existing ADR-007 violation outside this initiative's scope [Backlog]

**File:** `core/driver/Waiter.java`

Returns `WebDriverWait` (Selenium type) from two public non-deprecated methods. Three active callers in non-deprecated code: `Upload.java`, `KeyValuePairHandler.java`, `EnumResolver.java`. Pre-existing; not in the violation map for this initiative.

**Action:** Verify not already in backlog; log to `docs/audits/backlog/violations/` if absent.

---

### F4 -- UIEngine.sendKeys(CharSequence...) Javadoc references Keys.ESCAPE [Minor / inline fix]

**File:** `UIEngine.java:357`

The `@param` example references `Keys.ESCAPE` and `Keys.TAB` -- both Selenium types -- in the interface-level Javadoc. `UIEngine` is meant to be engine-agnostic; its documentation should not reference Selenium constants.

**Fix:** Replace the Javadoc example with a plain description (e.g., `"ESCAPE, TAB, or any key sequence"`).

**Cost:** One-line Javadoc edit. Can be done inline in the hotfix commit.

---

## What Passed Cleanly

- All three `UIEngine`/`SeleniumEngine` phase 1 methods placed in correct sections, correctly implemented.
- All deprecated Javadoc references to `UIEngine` methods are accurate -- methods referenced exist on the interface.
- `TableHandler.resolveDescriptor()` migration: all three methods updated; `resolve()` no longer appears anywhere in the file.
- VoidDSL `verifyElementsAreVisible`: no `org.openqa.selenium` import, correct `resolveDescriptor()` path, correct `isAnyDisplayed(LocatorDescriptor)` overload.
- All CommonStepDef call sites are commented out -- no active non-deprecated callers for the entire utils package.
- `WaitUtils.resolveLoader(By, boolean)` parameter type: `boolean` primitive is annotated `@Nullable` in the source. This is a pre-existing annotation misuse (you cannot null a primitive) but unchanged by this initiative.

---

## Recommendation

Open `hotfix/core-utils-engine-agnostic-final-audit` to address:

1. **F1:** Deprecate `WaitUtils.waitForCondition(WebDriver, ExpectedCondition<T>, By, ...)` with pointing Javadoc.
2. **F4:** Clean up UIEngine.sendKeys Javadoc to remove the Selenium-specific example.

Log F2 and F3 to `docs/audits/backlog/violations/` (one file each).

After the hotfix, this initiative is clear for:
- ADR authoring (`docs/decisions/pending-review/`)
- Merge to `main`
