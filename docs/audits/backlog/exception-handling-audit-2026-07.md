# VOID Exception Handling Audit

**Date:** 2026-07-27
**Branch evaluated:** `initiative/runtime-redesign`
**Scope:** `src/main/java` only (production sources; test sources excluded per audit request)
**Verdict:** C -- No Named Exception Vocabulary; Traceability Depends Entirely on Log Messages

---

## Verdict

> **C** -- The framework has **zero custom exception classes**. Every framework-specific
> failure (locator resolution failed, click exhausted all retries, enum not found, config
> missing) surfaces as a raw JDK `RuntimeException`, `IllegalStateException`, or
> `IllegalArgumentException`, indistinguishable by type from a caller's own programming bug.
> Propagation discipline is otherwise good -- the overwhelming majority of catch sites log
> before rethrowing and preserve the original cause -- so this is a traceability gap, not a
> correctness one. Separately, roughly a dozen catch sites around Selenium/JDK exceptions
> swallow silently or on overly broad `Exception`/`Throwable` types where a narrower catch
> or a log line would materially help someone debugging a failed run. Two sites (`Before`/
> `After`'s loader-wait catches) have a code comment that does not match what is actually
> being caught.

No `UIEngine`-execution-authority violation was found: every catch site that swallows or
rethrows a Selenium exception lives either inside `SeleniumEngine` itself (the correct
place, per ADR-007) or inside already-deprecated legacy utilities (`DOMUtils`, `TableHandler`,
`Upload`, `KeyValuePairHandler`, `WaitUtils`) that are tracked separately in
`docs/audits/backlog/violations/` for calling `WebDriver`/`WebElement` directly. This audit
does not duplicate those findings; it only evaluates the exception-handling shape at those
same sites.

---

## Custom Exception Inventory

| Search | Result |
|---|---|
| `class .* extends .*Exception` / `extends Throwable` in `src/main/java` | **Zero matches.** No custom exception type exists anywhere in the framework. |
| `class .* extends .*Exception` in `src/test/java` | Zero matches (test doubles use `RuntimeException` directly where needed). |

### Where a named exception would materially help (by call-site volume)

| Recommended type | Failure category it would cover | Approximate call sites | Traceability value |
|---|---|---|---|
| `VoidExecutionException` (or `InteractionException`) | An interaction/engine action ultimately failed after all retries/fallbacks | `Interactions.java` (~20 sites, e.g. `:146,212,232,275,311,333,351,371,396,440,460,479,495,518,571,607,654,681,706,732,756,789`), `SeleniumEngine.java:217` (click exhausts all 4 strategies), `DOMUtils.java:59,112,127,143`, `TableHandler.java:72,97,137`, `Upload.java:63`, `KeyValuePairHandler.java:51` | **High.** This is the failure a test author most needs to distinguish from "my own test code threw an NPE." Currently all of the above throw plain `RuntimeException`, so a `catch (RuntimeException e)` anywhere in a test's own helper code silently absorbs framework failures too. |
| `LocatorResolutionException` | A locator could not be resolved, parsed, or found in its backing source | `LocatorResolver.java:68,93,125,152,227` (`IllegalStateException`), `ByParser.java:47,51`, `ByPrefixStrategy.java:30`, `PropertiesFileLocatorReader.java:37`, `LocatorTemplate.java:87` (all `IllegalStateException`), `HardcodedLocatorSource.java:25`, `JsonLocatorSource.java:30`, `LayeredPropertiesLocatorSource.java:48`, `PropertiesLocatorSource.java:34`, `LocatorSourceRegistry.java:60` (all `IllegalArgumentException`), `JsonReader.java` (~12 sites, `RuntimeException`) | **Medium-high.** These are all "the test's locator data is wrong," a distinct failure mode from "my test assertion logic is wrong" -- exactly the kind of thing a test framework integration (e.g. a custom TestNG/JUnit listener) would want to catch and report specially. |
| `EnumResolutionException` | No matching enum constant/label found | `EnumResolver.java:47,61,68,96,104,132,139,143,160`, `VoidDSL.java` (~15 `IllegalArgumentException` sites for context/enum mismatches) | **Medium.** Mostly `IllegalArgumentException`, which is at least somewhat conventionally "caller passed bad input" -- lower urgency than the two above, but the sheer count (25+ sites) means a shared type would reduce guesswork. |
| `VoidConfigurationException` | Config/bootstrap/driver-setup failure | `FrameworkBootstrap.java:47`, `ConfigLoader.java:106,122,173,177`, `DriverFactory.java:282,501,764` | **Low-medium.** These already fail fast with clear messages at startup, not buried mid-run; lowest priority of the four. |

No custom exception class was found to be dead code (there are none to be dead). This
finding is the inventory: the gap itself, not an inconsistency among existing types.

---

## External Exception Catch-Site Review

Every `catch` in `src/main/java` handling a Selenium or defensively-caught JDK exception
type, classified per the audit's request.

### Silently swallowed (no log, no rethrow) -- needs fix

| # | Site | What's caught | Why it needs fix |
|---|---|---|---|
| 1 | `core/interactions/hooks/Before.java:53,60` and `After.java:54,61` | `Exception` around `engine.waitForAbsence(loader, DEFAULT_TIMEOUT)` for `WAIT_FOR_ANGULAR_LOADER`/`WAIT_FOR_SPIN_SPINNER_LOADER` | **Comment is misleading.** `waitForAbsence` -> `SeleniumEngine.waitForAbsence` -> `ExpectedConditions.invisibilityOfElementLocated` returns `true` immediately (no exception) when the loader was never present. The only way this catch fires is a genuine `TimeoutException` after the full 10s wait -- i.e. the loader **was** present and never went away. The comment `/* loader not present — continue */` describes the wrong case; the code is actually swallowing "loader got stuck," a real, test-relevant signal, with zero log line. |
| 2 | `SeleniumEngine.java:302,313,324` (`isVisible`, `isEnabled`, `isSelected`) | `Exception -> return false` | Any exception (a malformed locator throwing `InvalidSelectorException`, a crashed session, a real bug) is indistinguishable from "element genuinely not visible/enabled/selected." A test fails with "not visible" when the real cause could be a broken XPath -- no trace at any log level. |
| 3 | `SeleniumEngine.java:597` (`scrollToElement`), `:606` (`highlightElement`) | `Exception ignored`, no log at all (contrast with the public `scrollTo()`/`highlight()` at `:440,466` which do log at DEBUG) | Internal helpers used by `click()`'s pre-click phase; failures here are invisible even at DEBUG level, unlike their public counterparts one call up the stack. |
| 4 | `SeleniumEngine.java:612` (`urlChanged`) | `Exception -> return false` | Used to decide whether a `StaleElementReferenceException` during click means "page navigated, treat as success." If `driver.getCurrentUrl()` itself fails (session gone), this silently reports "URL unchanged," which can cause the retry logic to keep retrying against a dead session. |
| 5 | `SeleniumEngine.java:373` (`getCheckboxState`) | `Exception ignored -> return cb.isSelected()` | If `cb.getAttribute(...)` failed because the element went stale, the fallback `cb.isSelected()` call re-uses the same stale reference and will throw again, uncaught, one line later -- masks the real failure behind a second, unrelated-looking exception. |
| 6 | `DriverFactory.java:628,631,639` | `Exception ignored` around `window().maximize()`, `window().setSize(...)`, headless viewport fallback | A `maximize=true` or explicit `windowSize` config setting that silently fails to apply leaves a test running at an unexpected viewport with zero indication the setting didn't take -- a classic "why is this test flaky in CI but not locally" source. |
| 7 | `JsonLocatorReader.java:42` (`load`) | `Exception -> Optional.empty()`, cached via `computeIfAbsent` | A malformed JSON locator file (bad syntax, wrong encoding) is cached **permanently** as "resource not found" for the process lifetime, with no log. A locator-not-found error downstream gives no hint that the actual file exists but fails to parse. |
| 8 | `ConfigLoader.java:78,94` (classpath/file loaders) | `Exception` logged via `error.failed(...)` **but then returns an empty `Properties`** indistinguishable from "file legitimately doesn't exist" | Half-fixed: the exception message is logged, but the return value erases the distinction between "no config" and "config exists but is corrupt/unreadable" for every caller. Downstream `ConfigLoader.get()` then throws a generic "Missing config: X" that hides the real root cause. |

### Logged then swallowed -- acceptable, with two flagged as too quiet

| Site | What's caught | Assessment |
|---|---|---|
| `SeleniumEngine.java:102` (`shutdown`) | `Exception`, `warn.log` with message | Fine -- teardown errors shouldn't crash test cleanup, and it's visible at WARN. |
| `SeleniumEngine.java:169,187,205` (`click()` phases 1-3) | `Exception`, `debug.log` with message, falls through to next fallback phase | **Flag: log level too quiet for a 4-phase retry cascade.** DEBUG is routinely filtered out of default CI output; a click that silently limped through 3 failed strategies before finally succeeding on the 4th produces no visible signal that anything was wrong, even though it may indicate a flaky locator or slow-rendering app. Recommend WARN, or at minimum ensure `debug-trace` output is the one channel a flaky-test investigation always checks (confirm this is documented, not just assumed). |
| `SeleniumEngine.java:420` (`waitForOverlay`) | `Exception`, `debug.log("Overlay wait timed out — continuing")` | Message asserts "timed out" but the catch is broad `Exception`, not `TimeoutException` -- if a different exception fires, the log is misleading about what actually happened. |
| `SeleniumEngine.java:440,466` (`scrollTo`, `highlight`) | `Exception`, `debug.log` including `e.getMessage()`, comment "Best-effort — element may be gone if the action caused a navigation" | Acceptable -- intent is documented, message includes the actual exception text, DEBUG level is appropriate for a truly best-effort cosmetic operation. |
| `WaitUtils.java:~330` (`resolveLoader`, older overload) | `Exception`, `warn.timeout(...)` with message | Logged reasonably, but **does not distinguish `InterruptedException`** -- see next section. |
| `WaitUtils.java:507-510` (`resolveLoaderTemp`, newer overload) | `InterruptedException` (restores interrupt flag, logs) then `Exception` (logs) | **Done right** -- see "Examples of the pattern done right" below. |
| `DriverContext.java:132-134,152-155` (`quitDriver`, `quitAllDrivers`) | `RuntimeException`, `debug.log` per driver; `quitAllDrivers` collects and rethrows the first failure after attempting all quits | Good overall shape (cleanup isn't short-circuited, failure still surfaces) but only DEBUG-logged per driver; a browser process left running because `quit()` failed is arguably WARN-worthy on its own, independent of the rethrow. |

### Caught and rethrown wrapped -- correct pattern, generic type

The dominant pattern in the codebase. Roughly 45 sites across `Interactions.java` (~20),
`DOMUtils.java` (4, though `scrollToElement` at `:34-39` is the one exception that logs
and does **not** rethrow -- inconsistent with its four siblings in the same class),
`TableHandler.java` (3), `Upload.java` (2), `KeyValuePairHandler.java` (1),
`JsonReader.java` (~12), `JsonLocatorMigrator.java` (2), `EnumResolver.java` (2),
`ConfigLoader.java` (2), `SeleniumEngine.java:217` (1). Every one of these logs first
(usually `error.log`/`error.failed` with the original message) and rethrows with the
original exception preserved as `cause` -- the propagation discipline itself is correct.
The only gap is the wrapper type: all ~45 sites wrap into raw `RuntimeException` or
`IllegalStateException`, not a named VOID type (see Custom Exception Inventory above).

### Overly broad catch (`Exception`/`Throwable`) risking masked bugs

| Site | Catches | Risk |
|---|---|---|
| `SeleniumEngine.java:169,187,205` (click retry cascade) | `Exception` | A `NullPointerException` from a genuine bug elsewhere in the call chain (e.g. a malformed `LocatorDescriptor`) is caught identically to an expected `TimeoutException`/`StaleElementReferenceException` and silently triggers the next fallback phase instead of surfacing as a bug. |
| `EnumLocatorScanner.java:109` | `Throwable` (wider than `Exception`) | Comment ("tolerate misbehaving constants") justifies intent, but catching `Throwable` also swallows `Error` subtypes. Low practical risk (dev-time scanning tool, single constant skipped, loop continues) -- narrow to `Exception`. |
| `WaitUtils.java` inner lambda at `:355-388` (`waitForElementTextToBePresent`'s condition) | `Exception -> return false` inside a polling loop that already ignores `NoSuchElementException`/`StaleElementReferenceException` at the `FluentWait` level | The inner catch is redundant for the two ignored types and additionally masks anything else (e.g. a malformed locator) behind a generic "condition not met" / timeout message with no indication a different exception actually occurred. |

### Examples of the pattern done right

- **`WaitUtils.resolveLoaderTemp` (`:507-510`)** -- splits `InterruptedException` (restores
  `Thread.currentThread().interrupt()`, logs) from generic `Exception` (logs separately).
  This is the correct shape; `resolveLoader` (the older, still-public overload with the
  same polling-loop structure, `Thread.sleep` at `:325`) does **not** do this -- a thread
  interrupt during `resolveLoader`'s poll loop is silently absorbed by the single broad
  `catch (Exception e)` at `:330-332` without restoring the interrupt flag, breaking
  cooperative cancellation for that overload specifically.
- **`WaitUtils.isLoaderPresent` (`:346`)** -- narrow `catch (NoSuchElementException |
  StaleElementReferenceException ignored) { return false; }`. Exactly the right shape:
  specific types, semantically correct fallback ("not present" really does mean "not
  found"), no log needed because there's nothing to report.
- **`EnumResolver.stringToEnum` (`:83,119`)** -- `catch (Exception ignored) {}` immediately
  followed by a documented fallback strategy (try-by-label), with a clear
  `IllegalArgumentException` thrown if both strategies fail. Intentional "try A, then B"
  control flow, not a swallowed error.
- **`ActionTraceLogger.nameOf` (`:68`)** -- `catch (Exception ignored) {}` around
  `field.get(null)` on a `public static final` field, which cannot meaningfully fail. Blast
  radius is a cosmetic trace label falling back to the class simple name, not lost data.
- **`DriverContext.quitAllDrivers`** -- collects the first failure across all drivers,
  ensures every driver still gets a quit attempt, then rethrows after the loop. Correct
  "don't let one failure short-circuit cleanup" shape.

---

## Remediation Priorities

### Critical -- none

No finding here rises to "must fix before further evolution." The framework's exception
handling is a traceability gap, not a correctness or data-loss risk.

### Important -- fix within the next few phases touching these files

| Finding | File | Why Important |
|---|---|---|
| No named exception type for execution failures | `Interactions.java`, `SeleniumEngine.java:217`, `DOMUtils.java`, `TableHandler.java`, `Upload.java`, `KeyValuePairHandler.java` | ~30 sites wrap failures in raw `RuntimeException`. Highest call-site volume of any gap found; introduce `VoidExecutionException` and migrate these wrap sites when next touched. |
| `Before`/`After` loader-wait catches swallow a real timeout with a misleading comment | `core/interactions/hooks/Before.java:53,60`, `After.java:54,61` | A stuck-loader condition (app genuinely hung) is currently invisible. Log at WARN with the caught exception's message before continuing; correct the comment. |
| `isVisible`/`isEnabled`/`isSelected` swallow all exceptions identically to "false" | `SeleniumEngine.java:302,313,324` | A malformed locator and a genuinely-absent element produce the same silent `false`. At minimum log at DEBUG with `e.getClass().getSimpleName()` before returning false. |
| Click retry cascade catches broad `Exception` across 3 phases at DEBUG level | `SeleniumEngine.java:169,187,205` | A real bug (NPE) is indistinguishable from an expected Selenium retry condition, and even the expected case is invisible unless DEBUG output is being watched. Consider narrowing to Selenium's `WebDriverException` family, or at minimum confirm DEBUG-trace output is genuinely part of the standard flaky-test investigation workflow (if not, bump one level). |
| `JsonLocatorReader.load` permanently caches a parse failure as "not found" | `JsonLocatorReader.java:42` | Log at WARN before returning `Optional.empty()`, so a malformed JSON file doesn't look identical to a nonexistent one for the rest of the process lifetime. |
| `ConfigLoader` loaders swallow read failures behind an empty `Properties` | `ConfigLoader.java:78,94` | Already logs the exception message -- the gap is structural (no way for a caller to distinguish "no file" from "read error" without parsing log text). Consider a checked/marked return or at minimum ensure the WARN-level "file missing" log and the ERROR-level "read failed" log are never emitted for the same outcome, so log-scraping tooling can tell them apart. |
| `resolveLoader` doesn't restore interrupt status | `WaitUtils.java:~330` | Its sibling `resolveLoaderTemp` does this correctly one method away. Breaks cooperative thread cancellation if a test runner interrupts a hung wait. |

### Opportunistic -- fix when touching nearby code

| Finding | Why Opportunistic |
|---|---|
| `DriverFactory.java:628,631,639` silent window-sizing swallows | Real but low-frequency impact (viewport-dependent test flakiness); cheap one-line DEBUG log fix whenever this file is next touched. |
| `EnumLocatorScanner.java:109` catches `Throwable` instead of `Exception` | Narrow the type; comment already documents correct intent. |
| `DOMUtils.scrollToElement` logs but doesn't rethrow, unlike its four siblings in the same class | Inconsistent within one already-deprecated class; low value fix given the whole class is slated for removal (tracked in existing ADR-007 violations backlog). |
| `SeleniumEngine.waitForOverlay` log message says "timed out" for a broad `Exception` catch | Cosmetic message-accuracy fix; narrow to `TimeoutException` or generalize the message. |
| `DriverContext` quit-failure logs are DEBUG-only despite indicating a potential leaked browser process | Bump to WARN when next touched. |
| `LocatorResolver`/`ByParser`/`ByPrefixStrategy`/`LocatorTemplate`/`*LocatorSource` throw generic `IllegalStateException`/`IllegalArgumentException` | Candidates for `LocatorResolutionException` once introduced; no urgency to touch these files solely for this. |
| `EnumResolver`/`VoidDSL` throw generic `IllegalArgumentException` for resolution failures | Candidates for `EnumResolutionException`; lower volume/urgency than the execution-failure gap. |

### Ignore

| Finding | Why Ignore |
|---|---|
| `EnumResolver.stringToEnum`'s two `catch (Exception ignored) {}` sites | Justified try-A-then-B fallback control flow; a clear exception is thrown if both strategies fail. |
| `WaitUtils.isLoaderPresent`'s narrow `NoSuchElementException`/`StaleElementReferenceException` catch | Textbook correct: specific types, semantically accurate fallback, nothing to log. |
| `ActionTraceLogger.nameOf`'s `catch (Exception ignored) {}` around reflection on `public static final` fields | Cosmetic trace-label fallback only; cannot meaningfully fail given field visibility, and the blast radius (a display name) is inconsequential. |
| `core.logging` package has zero catch blocks | By design -- `CustomLogger`/`LogActions` delegate to Log4j2, which owns its own internal failure handling (its status logger). Nothing for VOID's own code to catch here; a logging call failing loudly would be the actual bug. |
| `DriverFactory.java` `NumberFormatException` catches (`:855,860,868-870,893`) around config parsing with sensible defaults | Correct, narrow, low-stakes defaulting pattern -- not a traceability concern. |
| `LocatorSyncOrchestrator.java` catches (`IOException`/`Exception` with `System.err`/exit codes) | A CLI tool, not test-runtime code; structured exit codes plus explicit error messages is the correct shape for that context. Its use of `System.out`/`System.err` instead of `CustomLogger` is a separate, pre-existing convention question outside this audit's scope -- noted here only as an aside, not a finding. |

---

## Summary Table

| Category | Count |
|---|---|
| Custom exception classes found | 0 |
| Recurring failure categories where a named exception is recommended | 4 (`VoidExecutionException`, `LocatorResolutionException`, `EnumResolutionException`, `VoidConfigurationException`) |
| Silently swallowed catch sites flagged | 8 |
| Logged-but-swallowed sites flagged as too quiet / misleading | 4 |
| Overly broad catch sites flagged | 3 |
| Sites confirmed as the pattern done right | 5 |
| Important-priority remediations | 7 |
| Opportunistic-priority remediations | 6 |
| Critical-priority remediations | 0 |

No code was modified as part of this audit.
