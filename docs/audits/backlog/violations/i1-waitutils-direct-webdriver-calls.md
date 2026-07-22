---
name: i1-waitutils-direct-webdriver-calls
description: WaitUtils calls driver.findElement(s)() directly and hardcodes an Angular CDK selector -- ADR-007 and ADR-018 violations
metadata:
  type: project
---

# I1-C: WaitUtils -- Direct WebDriver Calls and Angular CDK Selector

**Principle:** ADR-007 (UIEngine execution authority), ADR-018 (engine-agnostic layers Selenium-free)
**File:** `src/main/java/core/utils/web/WaitUtils.java`
**Discovered:** 2026-07-20 (architecture-rules.md audit on hotfix/engine-decoupling-final-audit)
**Risk:** High

## What it does

`WaitUtils` is a static utility class in `core.utils.web` with two distinct violations:

**Violation 1 -- Direct WebDriver calls (ADR-007):**
Non-deprecated methods call `driver.findElements()` and `driver.findElement()` directly.

**Violation 2 -- Hardcoded Angular CDK selector (ADR-018):**
```java
private static final By ANGULAR_LOADER = By.tagName("app-loader");
```
This constant couples the framework to a specific Angular CDK loading spinner. It is an
application-level concept embedded in a framework utility class, and it carries a Selenium
`By` import into an engine-agnostic layer.

## Code

```java
// ADR-007: direct driver access
public static boolean isElementPresent(WebDriver driver, By locator) {
    return !driver.findElements(locator).isEmpty();
}

// ADR-018: application-specific selector in framework code
private static final By ANGULAR_LOADER = By.tagName("app-loader");

public static void waitForAngularLoader(WebDriver driver) {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.invisibilityOfElementLocated(ANGULAR_LOADER));
}
```

## Why this violates ADR-007 and ADR-018

ADR-007: `driver.findElement(s)()` calls outside an engine implementation.

ADR-018: `By.tagName("app-loader")` is a Selenium type (`org.openqa.selenium.By`) used in
`core.utils.web`, which is an engine-agnostic layer. Additionally, `app-loader` is an
Angular application concern; the framework must not know about specific front-end components.

The remediation document notes this as "Opportunistic -- fix when touching WaitUtils."

## Recommended fix

- Route wait operations through `UIEngine` (e.g. `engine.waitForInvisibility(descriptor)`).
- Move the Angular loader wait to a `Before`/`After` profile hook in the test project, not
  the framework. The `Before.WAIT_FOR_ANGULAR_LOADER` constant that calls into this class
  should be the removal target.
- Deprecate `WaitUtils` static methods pointing callers to engine API.

**Estimated cost:** Dedicated -- coupled to the engine-capabilities work.
See also [[i1-domutils-direct-webdriver-calls]] and [[i1-tablehandler-direct-webdriver-calls]].
