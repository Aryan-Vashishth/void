---
name: i1-domutils-direct-webdriver-calls
description: DOMUtils calls WebDriver, JavascriptExecutor, and Actions directly in non-deprecated methods -- ADR-007 violation
metadata:
  type: project
---

# I1-A: DOMUtils -- Direct WebDriver Calls in Non-Deprecated Methods

**Principle:** ADR-007 (UIEngine is the single execution authority)
**File:** `src/main/java/core/utils/web/DOMUtils.java`
**Discovered:** 2026-07-20 (architecture-rules.md audit on hotfix/engine-decoupling-final-audit)
**Risk:** High

## What it does

`DOMUtils` is a static utility class in `core.utils.web`. Its non-deprecated methods call
`JavascriptExecutor`, `Actions`, and `driver.switchTo()` directly, bypassing `UIEngine`
entirely. Any call to these utilities couples the caller to Selenium and breaks the
engine-agnostic contract established by ADR-007.

## Code

Representative methods (non-deprecated):

```java
// Direct JavascriptExecutor call -- bypasses UIEngine
public static void scrollToElement(WebDriver driver, WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
}

// Direct Actions usage -- bypasses UIEngine
public static void hoverOverElement(WebDriver driver, WebElement element) {
    new Actions(driver).moveToElement(element).perform();
}

// Direct frame switching -- bypasses UIEngine
public static void switchToFrame(WebDriver driver, WebElement frame) {
    driver.switchTo().frame(frame);
}
```

## Why this violates ADR-007

ADR-007: nothing outside `UIEngine` implementations calls `WebDriver` methods directly.
`DOMUtils` is in `core.utils.web`, not inside an engine implementation. Every non-deprecated
method that accepts `WebDriver` or calls `JavascriptExecutor` / `Actions` is a direct
violation. Callers in test code and DSL layers import Selenium transitively through this class.

## Recommended fix

Move browser-specific operations into `UIEngine` interface methods or into
`SeleniumEngine` directly. Expose scroll, hover, and frame operations as named
capabilities on `UIEngine` (e.g. `engine.scrollToElement(descriptor)`,
`engine.hoverOver(descriptor)`, `engine.switchToFrame(descriptor)`).

Do not delete `DOMUtils` immediately -- deprecate existing methods pointing to the new
engine API, then schedule removal in a future release.

**Estimated cost:** Dedicated -- requires new UIEngine interface methods and SeleniumEngine
implementations. Scope this as part of the next engine-capabilities initiative.
