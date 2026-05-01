# Hooks Guide — Composable Before/After Action Pipeline

VOID's hook system allows you to compose reusable pre- and post-action behaviors around every UI interaction. This guide covers the hook model, built-in hook constants, and patterns for writing custom hooks.

---

## Table of Contents

1. [Overview](#overview)
2. [How Hooks Work](#how-hooks-work)
3. [ActionHandler Interface](#actionhandler-interface)
4. [Built-In Before Hooks](#built-in-before-hooks)
5. [Built-In After Hooks](#built-in-after-hooks)
6. [Using Hooks in Interactions](#using-hooks-in-interactions)
7. [UIContext and Element State](#uicontext-and-element-state)
8. [Writing Custom Hooks](#writing-custom-hooks)
9. [Patterns and Best Practices](#patterns-and-best-practices)

---

## Overview

Every `Interactions` method that accepts an element also accepts optional `List<ActionHandler>` parameters for **before** and **after** hooks:

```java
// Without hooks (uses SDK defaults)
app.interaction().clickOn(element);

// With explicit hooks
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.HIGHLIGHT_ELEMENT),
    element,
    List.of(After.DO_NOTHING)
);
```

Hooks are **composable** — you can chain any number in a list, and they execute in order.

---

## How Hooks Work

The execution flow for a hooked interaction is:

```
1. Element resolution     → LocatorResolvers resolve the By locator
2. UIContext update        → last-resolved element stored in UIContext
3. Before hooks execute   → each Before.* runs in list order
4. Core action            → Selenium action (click, sendKeys, etc.)
5. After hooks execute    → each After.* runs in list order
6. Logging                → CustomLogger emits the action record
```

If a before hook fails (throws), the core action is **not** executed. If the core action fails, after hooks are still attempted for cleanup.

---

## ActionHandler Interface

```java
@FunctionalInterface
public interface ActionHandler {
    void execute(WebDriver driver);
}
```

Every hook is simply a lambda or constant that receives the active `WebDriver`. The interface is deliberately minimal — the `WebDriver` parameter gives hooks full access to the browser, and `UIContext` provides the last-resolved element.

---

## Built-In Before Hooks

All constants are in `interactions.hooks.Before`:

| Constant                          | Description                                                    |
|-----------------------------------|----------------------------------------------------------------|
| `DO_NOTHING`                      | No-op — useful as a placeholder in hook lists                  |
| `LOG_INTENT`                      | Logs a debug message indicating a UI action is about to occur  |
| `WAIT_FOR_ANGULAR_LOADER`         | Waits for Angular CDK/Material loader overlay to disappear     |
| `WAIT_FOR_SPIN_SPINNER_LOADER`    | Waits for a `spin spinner` CSS class loader to disappear       |
| `WAIT_FOR_ELEMENT_CLICKABLE`      | Waits (up to 10s) for the last-resolved element to be clickable|
| `WAIT_FOR_ELEMENT_VISIBLE`        | Waits (up to 10s) for the last-resolved element to be visible  |
| `CLEAR_FIELD`                     | Clears the last-resolved input element                         |
| `HIGHLIGHT_ELEMENT`               | Highlights the last-resolved element with a colored border     |
| `SCROLL_TO_ELEMENT`               | Scrolls the last-resolved element into view via JavaScript     |

### Example

```java
// Wait for Angular overlay, then wait for the button to be clickable
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_CLICKABLE),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);
```

---

## Built-In After Hooks

All constants are in `interactions.hooks.After`:

| Constant                          | Description                                                    |
|-----------------------------------|----------------------------------------------------------------|
| `DO_NOTHING`                      | No-op — explicit "no cleanup needed"                           |
| `WAIT_FOR_ANGULAR_LOADER`         | Waits for Angular loader to disappear after the action         |
| `WAIT_FOR_SPIN_SPINNER_LOADER`    | Waits for spinner loader to disappear after the action         |
| `WAIT_FOR_ELEMENT_VISIBLE`        | Waits for the last-resolved element to be visible post-action  |
| `HIGHLIGHT_ELEMENT`               | Highlights the element with a green border (success indicator) |
| `SCROLL_TO_ELEMENT`               | Scrolls the element into view after the action                 |

### Example

```java
// After clicking, wait for the Angular loader and highlight the result
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE),
    MyElements.SAVE_BUTTON,
    List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
);
```

---

## Using Hooks in Interactions

### Methods That Accept Hooks

Most `Interactions` methods have overloads accepting hook lists:

```java
// Click
clickOn(List<ActionHandler> before, Clickable element, List<ActionHandler> after)

// Type
typeInto(ActionHandler before, TextInputField element, String text)

// Search
performSearch(List<ActionHandler> before, WebElement input, String term,
              WebElement results, List<ActionHandler> after)
```

### Shorthand for Single Hooks

When you only need one before hook, some methods accept a single `ActionHandler`:

```java
app.interaction().typeInto(Before.WAIT_FOR_ANGULAR_LOADER, emailField, "user@example.com");
```

### Default Behavior (No Hooks)

When you call a method without specifying hooks, VOID applies sensible defaults internally (typically scroll + highlight for clicks).

---

## UIContext and Element State

Many hooks operate on "the last resolved element" — this is stored in `UIContext`, a thread-local state holder:

```java
// What hooks see internally:
WebElement element = UIContext.getLastElement();     // the resolved WebElement
String file     = UIContext.getLastPropertyFile();   // locator file name
String key      = UIContext.getLastKey();            // locator key
Object[] args   = UIContext.getLastArgs();           // template arguments
```

`UIContext` is updated **before** the hook pipeline runs, so before-hooks always see the correct element. This also enables **stale-element retry** — if a `StaleElementReferenceException` occurs, VOID can re-resolve using the stored meta.

---

## Writing Custom Hooks

### Inline Lambda

The simplest approach — write a lambda directly in your hook list:

```java
app.interaction().clickOn(
    List.of(
        Before.WAIT_FOR_ANGULAR_LOADER,
        driver -> {
            // Custom: dismiss any visible cookie banner
            List<WebElement> banners = driver.findElements(By.id("cookie-dismiss"));
            if (!banners.isEmpty()) banners.get(0).click();
        }
    ),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);
```

### Reusable Constants

For hooks you use repeatedly, define them as `public static final` constants:

```java
public final class CustomHooks {

    private CustomHooks() {}

    /** Dismiss the cookie consent banner if present. */
    public static final ActionHandler DISMISS_COOKIE_BANNER = driver -> {
        List<WebElement> banners = driver.findElements(By.id("cookie-dismiss"));
        if (!banners.isEmpty()) {
            banners.get(0).click();
            WaitUtils.resolveAngularLoader();
        }
    };

    /** Wait for a custom loading spinner specific to your app. */
    public static final ActionHandler WAIT_FOR_APP_SPINNER = driver -> {
        WaitUtils.resolveLoader(By.cssSelector(".app-loading-spinner"));
    };

    /** Take a screenshot before the action (for debugging). */
    public static final ActionHandler SCREENSHOT_BEFORE = driver -> {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        // copy to target/screenshots/...
    };
}
```

Usage:

```java
app.interaction().clickOn(
    List.of(CustomHooks.DISMISS_COOKIE_BANNER, Before.WAIT_FOR_ELEMENT_CLICKABLE),
    MyElements.CHECKOUT_BUTTON,
    List.of(CustomHooks.WAIT_FOR_APP_SPINNER)
);
```

### Parameterized Hook Factory

For hooks that need runtime parameters, use a static factory method:

```java
public final class CustomHooks {

    /** Wait for a specific element (by locator) to disappear. */
    public static ActionHandler waitForAbsence(By locator) {
        return driver -> {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        };
    }

    /** Scroll to a specific pixel offset. */
    public static ActionHandler scrollToY(int yOffset) {
        return driver -> {
            ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, arguments[0]);", yOffset
            );
        };
    }
}

// Usage
app.interaction().clickOn(
    List.of(CustomHooks.waitForAbsence(By.id("loading-overlay"))),
    MyElements.SUBMIT,
    List.of(After.DO_NOTHING)
);
```

---

## Patterns and Best Practices

### 1. Keep Hooks Focused

Each hook should do **one thing**. Combine them in lists rather than building mega-hooks:

```java
// ✅ Good — composable, reusable
List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.SCROLL_TO_ELEMENT, Before.HIGHLIGHT_ELEMENT)

// ❌ Bad — monolithic, not reusable
List.of(driver -> {
    WaitUtils.resolveAngularLoader();
    DOMUtils.scrollToElement(UIContext.getLastElement());
    DOMUtils.highlightElement(UIContext.getLastElement());
})
```

### 2. Use DO_NOTHING Explicitly

When you intentionally want no before/after processing, use `DO_NOTHING` rather than an empty list — it communicates intent:

```java
app.interaction().clickOn(
    List.of(Before.DO_NOTHING),
    element,
    List.of(After.DO_NOTHING)
);
```

### 3. Guard Against Null Elements

Hooks that use `UIContext.getLastElement()` should null-check gracefully:

```java
public static final ActionHandler MY_HOOK = driver -> {
    WebElement el = UIContext.getLastElement();
    if (el == null) return;  // graceful no-op
    // ... operate on el
};
```

### 4. Don't Swallow Exceptions Silently

Let meaningful exceptions propagate — VOID's retry and logging mechanisms depend on them:

```java
// ✅ Good — let TimeoutException propagate
public static final ActionHandler WAIT_FOR_PANEL = driver -> {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.visibilityOfElementLocated(By.id("panel")));
};

// ❌ Bad — hides failures
public static final ActionHandler WAIT_FOR_PANEL = driver -> {
    try {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("panel")));
    } catch (Exception e) {
        // silently ignored
    }
};
```

### 5. Common Hook Combinations

| Scenario                          | Before Hooks                                            | After Hooks                            |
|-----------------------------------|---------------------------------------------------------|----------------------------------------|
| Standard click                    | `SCROLL_TO_ELEMENT`, `WAIT_FOR_ELEMENT_CLICKABLE`       | `DO_NOTHING`                           |
| Click after page load             | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_ELEMENT_CLICKABLE` | `DO_NOTHING`                           |
| Type into field                   | `WAIT_FOR_ELEMENT_VISIBLE`, `CLEAR_FIELD`               | `DO_NOTHING`                           |
| Submit form                       | `WAIT_FOR_ELEMENT_CLICKABLE`                            | `WAIT_FOR_ANGULAR_LOADER`              |
| Debugging / visual verification   | `HIGHLIGHT_ELEMENT`                                     | `HIGHLIGHT_ELEMENT`                    |

---

## Related Documentation

- [Architecture Deep-Dive](architecture.md) — execution flow and hook pipeline details
- [Quick Start Guide](quick-start.md) — first test with hooks
- [`Interactions` API](../src/main/java/interactions/Interactions.java) — all interaction method overloads
- [`Before.java`](../src/main/java/interactions/hooks/Before.java) — before-hook source
- [`After.java`](../src/main/java/interactions/hooks/After.java) — after-hook source

---

*MIT License © 2025–2026 VOID Project*

