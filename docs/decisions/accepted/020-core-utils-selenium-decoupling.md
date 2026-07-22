# ADR-020 -- Core Utils Selenium Decoupling

**Date:** 2026-07-20
**Status:** Accepted (merged to main 2026-07-22)
**Branch:** `initiative/core-utils-engine-agnostic`
**Supersedes:** Partial remediation of I1-A, I1-B, I1-C violations from the engine-decoupling final audit

---

## Context

Three static utility classes in `core.utils.web` bypassed `UIEngine` and called
`DriverContext.getDriver()` + Selenium types directly in non-deprecated public methods,
violating ADR-007:

| Class | Violation | Risk |
|---|---|---|
| `DOMUtils` | `JavascriptExecutor`, `Actions`, `driver.switchTo()` in non-deprecated methods | High |
| `WaitUtils` | `FluentWait<WebDriver>`, `driver.findElements()`, and two hardcoded Angular CDK `By` constants | High |
| `TableHandler` | `driver.findElements()` via the deprecated `LocatorResolver.resolve()` path | High |

All active production callers in `CommonStepDef.java` were already commented out. No live
call sites existed outside the utils package itself, making the utilities unreachable
dead API -- but still violating the engine-agnostic layer contract.

Three UIEngine methods had no equivalent for `DOMUtils` deprecation:
`switchToFrame`, `switchToDefaultContent`, and `sendKeys` were absent from the interface.

---

## Decision

### 1. Extend UIEngine with three missing methods

Add to `core.engine.UIEngine`:

```java
void switchToFrame(LocatorDescriptor locator);
void switchToDefaultContent();
void sendKeys(CharSequence... keys);
```

Implement in `SeleniumEngine`. These provide the engine-agnostic equivalents that
`DOMUtils` methods can reference in their deprecation Javadoc.

### 2. Deprecate DOMUtils entirely

Annotate the class and all six public methods with `@Deprecated(forRemoval = true)`.
Each method's Javadoc references the UIEngine equivalent. No method bodies are changed.

| Deprecated method | UIEngine replacement |
|---|---|
| `scrollToElement(WebElement)` | `UIEngine.scrollTo(LocatorDescriptor)` |
| `hoverOnElement(WebElement)` | `UIEngine.hover(LocatorDescriptor)` |
| `switchToFrame(By)` | `UIEngine.switchToFrame(LocatorDescriptor)` |
| `switchToDefaultContent()` | `UIEngine.switchToDefaultContent()` |
| `sendKey(Keys)` | `UIEngine.sendKeys(CharSequence...)` |
| `sendEscapeKey()` | `UIEngine.sendKeys(Keys.ESCAPE)` |

### 3. Deprecate WaitUtils By-based API and Angular CDK selector fields

Two `By` constants and sixteen public By-based wait methods deprecated with
`@Deprecated(forRemoval = true)`. Each references the UIEngine wait equivalent.

`waitForCondition(WebDriver, ExpectedCondition<T>, By, ...)` and
`waitForCondition(String, Duration, Duration, Supplier<Boolean>)` are NOT deprecated:
- The `WebDriver` overload is an internal dispatch utility called only by deprecated methods.
  Logged to backlog for a follow-up deprecation.
- The `Supplier<Boolean>` overload has a JDK-only caller API and is engine-agnostic at the
  boundary. It requires a `UIEngine.waitUntil(Supplier<Boolean>, Duration)` equivalent
  before it can be deprecated.

### 4. Migrate TableHandler internal resolution; deprecate entirely

`LocatorResolver.resolve()` (returns `By`) replaced with `resolveDescriptor()` (returns
`LocatorDescriptor`) + `SeleniumEngine.toBy()` for the three methods that call
`driver.findElements()`. The class and all three public methods are deprecated for removal.

`driver.findElements(By)` is NOT removed from `TableHandler`. Full removal requires:
1. `UIEngine.getTextList(LocatorDescriptor)` -- no concrete callers justify adding this yet
2. A scoped element query model for cell reads within rows

Both are deferred under Stability Rule 4 (no premature abstractions).

### 5. Full table API deferred

`UIEngine.getColumnHeaders()`, `UIEngine.getRow()` and related table-read methods are NOT
added. No active non-deprecated callers exist. Architecture should emerge from repeated
requirements, not anticipated ones.

---

## Consequences

- `UIEngine` gains three new methods; all implementations must add them.
- `DOMUtils`, `WaitUtils` (By-based surface), and `TableHandler` are scheduled for removal.
- Active callers: none (all `CommonStepDef` call sites are commented out).
- `SeleniumEngine.toBy(LocatorDescriptor)` is used inside a deprecated class -- acceptable;
  the bridge helper is already documented as a transition-period tool.
- `VoidDSL.verifyElementsAreVisible()` was fixed simultaneously: replaced `resolve()` +
  `By` path with `resolveDescriptor()` + `isAnyDisplayed(LocatorDescriptor)`.

---

## Invariants established

- No new `By`-returning resolver calls may be introduced in non-deprecated code.
- UIEngine must provide an equivalent before any utility method can be deprecated.
- Table operations are deferred until a concrete non-deprecated caller drives the design.

---

## Related

- [ADR-007 -- UIEngine Execution Authority](../accepted/007-uiengine-execution-authority.md)
- [ADR-018 -- Engine Lifecycle Ownership](018-engine-lifecycle-ownership.md)
- [Initiative plan](../../plan/done/core-utils-engine-agnostic/index.md)
- [Post-implementation audit](../../plan/done/core-utils-engine-agnostic/audit/post-implementation-audit.md)
- Backlog: `waiter-returns-webdriverwait`, `waitutils-waitforcondition-selenium-webdriver-param`
