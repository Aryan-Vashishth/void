---
name: waitutils-waitforcondition-internal-webdriver-coupling
description: WaitUtils.waitForCondition(String, Duration, Duration, Supplier<Boolean>) uses FluentWait<WebDriver> internally despite an engine-agnostic caller API
metadata:
  type: project
---

# WaitUtils -- waitForCondition Supplier Overload Internal WebDriver Coupling

**Principle:** ADR-007 (UIEngine execution authority) -- internal only
**File:** `src/main/java/core/utils/web/WaitUtils.java:110`
**Discovered:** 2026-07-20 (post-implementation audit: core-utils-engine-agnostic)
**Risk:** Very Low (caller API is JDK-only; Selenium coupling is not visible to callers)

## What it is

```java
public static boolean waitForCondition(String conditionLabel,
                                       Duration timeout,
                                       Duration polling,
                                       java.util.function.Supplier<Boolean> condition) {
    WebDriver driver = DriverContext.getDriver();
    FluentWait<WebDriver> wait = new FluentWait<>(driver)...
    ...
}
```

The public method signature takes only JDK types (`String`, `Duration`, `Supplier<Boolean>`).
Internally it wraps the condition in `FluentWait<WebDriver>` backed by `DriverContext.getDriver()`.

From the caller's perspective this is engine-agnostic: callers pass a `Supplier<Boolean>` and
receive a `boolean`. The Selenium coupling is an internal implementation detail.

## Why it is not an immediate concern

ADR-007's invariant is: "Nothing outside UIEngine implementations calls WebDriver methods
directly." Callers of this method do not call WebDriver methods -- they pass a condition
supplier. The method is Selenium-coupled internally but not at the API boundary.

## Why it is logged

If a second engine (Playwright, BiDi) is added, the `FluentWait<WebDriver>` polling loop
would need to be replaced with an engine-neutral mechanism. The current implementation
assumes Selenium's threading model and exception types (`NoSuchElementException`,
`StaleElementReferenceException`) in its ignore list.

## Recommended fix

Design a `UIEngine.waitUntil(Supplier<Boolean>, Duration)` method (or equivalent) that
each engine implements with its native polling mechanism. This method would then delegate
to it.

**Estimated cost:** Dedicated -- requires UIEngine API extension and SeleniumEngine implementation.
Not deprecatable without a replacement.
See also [[waitutils-waitforcondition-selenium-webdriver-param]].
