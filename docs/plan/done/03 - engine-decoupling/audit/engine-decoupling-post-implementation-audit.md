# Post-Implementation Audit: Engine Decoupling

**Date:** 2026-07-22
**Branch:** `feature/engine-decoupling` (pre-merge to main)
**Auditing ADRs:** ADR-018 (Engine Lifecycle Ownership), ADR-019 (SeleniumLocatorBridge Isolation)
**Verdict:** PASS -- all six violations resolved; two hotfix findings addressed; backlog items logged

---

## Phases Verified

| Phase | Key commit | Compile | Outcome |
|---|---|---|---|
| 1 -- Factory contract | `a258515` | Pass | `UIEngineFactory.create` accepts `EngineBootstrap`, not `WebDriver`; `SeleniumEngine(Profile)` constructor introduced |
| 2 -- VOID startup pipeline | `ca93733` | Pass | `VOIDBuilder` introduced; `VOID.start()` deprecated; `VOID` holds `SessionContext`, not `ExecutionContext` |
| 3 -- Interactions cleanup | `5d85172` | Pass | Unsafe `WebDriver` cast removed; `SeleniumLocatorBridge` introduced; six `fromBy` call sites redirected |
| 4 -- Bootstrap cleanup | `92bad24` | Pass | Selenium JUL logger suppression moved from `FrameworkBootstrap` to `SeleniumEngine.initialize()` |
| Hotfix: coverage | `1aae3dd` (merge) | Pass | `SeleniumEngineToByTest` (8 cases), `VOIDBuilderTest` (10 cases), `CoreUtilsDeprecationTest` (8 cases) added |
| Hotfix: final audit | `0995685` (merge) | Pass | `VoidDSL.verifyElementsAreVisible` Selenium `By` removed; `SeleniumEngine.fromBy` deprecated; stale Javadoc cleaned |
| Pre-merge cleanup | `feature/engine-decoupling` HEAD | Pass | `SeleniumLocatorBridgeTest` (7 cases) added; `package-info.java` for `core.bridge.selenium` added; `UIEngine.sendKeys` Javadoc engine-neutralised |

---

## Violation Resolution

| ID | Priority | Violation | Status | Evidence |
|---|---|---|---|---|
| V1 | CRITICAL | `UIEngineFactory.create()` requires `WebDriver` | **Resolved** | Signature is now `create(Properties, EngineBootstrap)`; `WebDriver` import removed from factory |
| V2 | CRITICAL | `VOID.start()` creates `WebDriver` before engine selection | **Resolved** | `VOID.builder().start()` selects engine first; driver created inside `SeleniumEngine.initialize()` |
| V3 | CRITICAL | `VOID` holds `ExecutionContext` (WebDriver-typed) | **Resolved** | `VOID` field is `SessionContext context`; `ExecutionContext` is `@Deprecated(since="0.2")` |
| V4 | HIGH | `Interactions(UIEngine)` casts `getNativeDriver()` to `WebDriver` | **Resolved** | Cast and `DriverContext.setPrimaryDriver` call removed from constructor; `SeleniumLocatorBridge` handles the bridge |
| V5 | MEDIUM | `VOID.shutdown()` calls `DriverContext.removePrimary()` directly | **Resolved** | `VOID.shutdown()` delegates entirely to `engine.shutdown()`; `SeleniumEngine.shutdown()` owns `DriverContext` cleanup |
| V6 | LOW | Selenium JUL logger suppressed in engine-agnostic bootstrap | **Resolved** | `Logger.getLogger("org.openqa.selenium")` call moved to `SeleniumEngine.configureLogging()`, called from `initialize()` |

---

## Architecture Invariant Check

| Invariant | Status | Notes |
|---|---|---|
| UIEngine is the single execution authority (ADR-007) | **Pass** | No new direct `WebDriver` call sites introduced. Pre-existing gaps (WaitUtils, DOMUtils) are deprecated and logged. |
| Engine-agnostic layers are Selenium-free (ADR-018) | **Pass** | `core.runtime`, `core.interactions`, `core.bootstrap` have no new Selenium imports. `SeleniumLocatorBridge` is isolated in `core.bridge.selenium` and is itself `@Deprecated(forRemoval=true)`. |
| `LocatorDescriptor` is Selenium-free (ADR-019) | **Pass** | No `org.openqa.selenium.By` fields or parameters on `LocatorDescriptor`. |
| `ElementSupport` scope frozen (ADR-017) | **Pass** | Not touched. |
| `VOIDBuilder` is single-use (ADR-018) | **Pass** | `started` guard enforces single-use; `VOIDBuilderTest.start_throwsIllegalStateExceptionWhenAlreadyStarted` covers it. |

---

## Hotfix Findings (found during final audit pass, now resolved)

### HF-1 -- `VoidDSL.verifyElementsAreVisible` used Selenium `By` in the active path

**File:** `dsl/VoidDSL.java`

`verifyElementsAreVisible` called `LocatorResolvers.strict().resolve(...)` which returned a
Selenium `By`, then passed it to `Interactions.isAnyDisplayed(By)` -- a deprecated bridge method.
This kept Selenium in the live execution path.

**Fix:** Replaced with `resolveDescriptor(...)` returning `LocatorDescriptor`, passed to
`isAnyDisplayed(LocatorDescriptor)`. Selenium `By` and `WebElement` imports removed from
`VoidDSL.java`.

### HF-2 -- `SeleniumEngine.fromBy()` was not deprecated

**File:** `core/engine/selenium/SeleniumEngine.java`

`SeleniumEngine.fromBy(By)` was the original conversion utility. After Phase 3 introduced
`SeleniumLocatorBridge.fromBy(By)` as the canonical home for this logic, `SeleniumEngine.fromBy`
became a duplicate that remained non-deprecated. This could mislead callers into using the
engine class as a utility.

**Fix:** `SeleniumEngine.fromBy` annotated `@Deprecated(forRemoval = true)` with Javadoc
pointing to `SeleniumLocatorBridge.fromBy`.

### HF-3 -- Stale `EngineBootstrap.FromDriver` Javadoc reference in `SeleniumEngine`

**File:** `core/engine/selenium/SeleniumEngine.java`

A Javadoc comment referenced `EngineBootstrap.FromDriver`, a variant that was present in an
early plan draft but was replaced by `EngineBootstrap.FromProfile` before implementation.

**Fix:** Javadoc corrected; `EngineBootstrap.FromDriver` reference removed.

---

## Test Coverage Added

| Class | Test file | Cases | Scope |
|---|---|---|---|
| `VOIDBuilder` | `VOIDBuilderTest` | 10 | Fluent API, field assignment, `resolvedConfig()`, single-use guard |
| `SeleniumEngine.toBy` | `SeleniumEngineToByTest` | 8 | All four strategies, round-trip, label isolation, null guards |
| `DOMUtils`, `WaitUtils`, `TableHandler` | `CoreUtilsDeprecationTest` | 8 | Deprecation annotation regression guard |
| `SeleniumLocatorBridge.fromBy` | `SeleniumLocatorBridgeTest` | 8 | All four strategies, XPATH fallback (with warning), raw value preservation, complex expression preservation |

No browser-dependent examples were added; all cases use reflection or in-process Selenium type construction.

---

## Remaining Backlog Items

Items found during the initiative and hotfix that are outside scope and logged for future remediation:

| Finding | File | Logged |
|---|---|---|
| `WaitUtils.waitForCondition(WebDriver,...)` -- public non-deprecated Selenium-typed method | `WaitUtils.java:71` | `backlog/violations/waitutils-waitforcondition-selenium-webdriver-param.md` |
| `WaitUtils.waitForCondition(String,...)` -- Selenium `FluentWait` internal coupling | `WaitUtils.java:110` | `backlog/violations/waitutils-waitforcondition-internal-webdriver-coupling.md` |
| `Waiter.java` -- returns `WebDriverWait` (ADR-007 violation) | `core/driver/Waiter.java` | `backlog/violations/waiter-returns-webdriverwait.md` |
| `DriverFactory` `instanceof`-preference dispatch (P8 in OOP registry) | `DriverFactory.java:722` | `backlog/violations/oop-driverfactory-instanceof-preference-dispatch.md` |
| `core/driver/` package -- mixed Selenium coupling and engine-agnostic config | `core/driver/` | `backlog/violations/core-driver-package-selenium-coupling.md` |

None of these were introduced or worsened by this initiative. All pre-existed or were found
incidentally during the audit pass.

---

## Post-Merge Actions Required

1. Move ADRs 018 and 019 from `docs/decisions/pending-review/` to `docs/decisions/accepted/`.
2. Update `docs/decisions/accepted/README.md` with rows for ADR-018 and ADR-019.
3. Correct branch name in ADR-018 and ADR-019 from `initiative/engine-decoupling` to
   `feature/engine-decoupling` (cosmetic -- the branch naming convention was formalised after
   this initiative was already underway).
