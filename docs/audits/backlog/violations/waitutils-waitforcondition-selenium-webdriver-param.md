---
name: waitutils-waitforcondition-selenium-webdriver-param
description: WaitUtils.waitForCondition(WebDriver, ExpectedCondition, By, ...) is public and non-deprecated despite accepting Selenium types -- ADR-007 violation
metadata:
  type: project
---

# WaitUtils -- waitForCondition(WebDriver, ...) Public Non-Deprecated Selenium API

**Principle:** ADR-007 (UIEngine execution authority)
**File:** `src/main/java/core/utils/web/WaitUtils.java:71`
**Discovered:** 2026-07-20 (post-implementation audit: core-utils-engine-agnostic)
**Risk:** Low (no live non-deprecated callers today; grows if new test code reaches this method)

## What it is

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

Public, non-deprecated static method accepting `WebDriver`, `ExpectedCondition<T>`, and `By`
-- all Selenium-specific types. Violates ADR-007: Selenium API exposed outside UIEngine
implementations.

## Current callers

- `DOMUtils.switchToFrame(By)` (deprecated)
- WaitUtils deprecated internal methods only

No live non-deprecated callers. Not introduced or worsened by `initiative/core-utils-engine-agnostic`.

## Why it was not fixed in-initiative

The initiative (core-utils-engine-agnostic) scoped I1-C to the By-based public API and
Angular CDK selector fields. This overload was treated as an internal dispatch utility and
left non-deprecated. It was surfaced during the post-implementation audit.

## Recommended fix

Add `@Deprecated(forRemoval = true)`. Javadoc should point callers to:
- `waitForCondition(String, Duration, Duration, Supplier<Boolean>)` for engine-agnostic condition checks
- UIEngine wait methods (`waitForVisible`, `waitForAbsence`, `waitForClickable`) for element-specific waits

**Estimated cost:** Minimal -- annotation + Javadoc only.
See also [[waitutils-waitforcondition-internal-webdriver-coupling]].
