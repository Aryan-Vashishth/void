# Hooks Guide — Composable Before/After Action Pipeline

VOID's hook system allows you to compose reusable pre- and post-action behaviors around every UI interaction. This guide covers the hook model, built-in hook constants, and patterns for writing custom hooks.

> **Note:** Hooks are available on **both** execution paths:
> - **Action/Flow/FlowExecutor (modern):** Use `element.click().safely()` (profile-based, preferred)
>   or the directional fluent API `element.click().before(...).after(...)` for manual control.
> - **Interactions (legacy):** Pass hook lists to `Interactions` method overloads.
>
> UIEngine handles waits, scrolling, and retries internally regardless of path.
> Hooks are for **app-specific** pre/post behavior (Angular loaders, highlights, etc.).

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
9. [Hooks vs UIEngine Built-In Behavior](#hooks-vs-uiengine-built-in-behavior)
10. [Patterns and Best Practices](#patterns-and-best-practices)

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
1. Element resolution     → LocatorResolvers resolve the LocatorDescriptor
2. UIContext update        → last-resolved descriptor stored in UIContext
3. Before hooks execute   → each Before.* runs in list order
4. Core action            → UIEngine executes (click, type, etc.)
5. After hooks execute    → each After.* runs in list order
6. Logging                → CustomLogger emits the action record
```

If a before hook fails (throws), the core action is **not** executed. If the core action fails, after hooks are still attempted for cleanup.

### Two Execution Paths

| Path | Hooks Used? | Wait/Scroll/Retry |
|------|-------------|-------------------|
| **Action/Flow/FlowExecutor** | Optional — via `.safely()` (profile) or `.before(...).after(...)` (manual) | Built into UIEngine |
| **Interactions (legacy)** | Yes — Before/After hooks compose pre/post behavior | Hooks + UIEngine |

---

## ActionHandler Interface

```java
@FunctionalInterface
public interface ActionHandler {
    void execute(UIEngine engine, @Nullable LocatorDescriptor descriptor);
}
```

Every hook is a lambda or constant that receives:
- **`UIEngine`** — the active engine (engine-agnostic, not tied to WebDriver directly)
- **`LocatorDescriptor`** — the resolved descriptor of the element being acted upon

In the modern pipeline (`Action.before(...).after(...)`), the descriptor is always non-null.
In the legacy pipeline (`Interactions`), the descriptor may be `null` — hooks should guard accordingly.

### Legacy adapter

For migrating old single-arg hooks:

```java
@Deprecated(forRemoval = true)
static ActionHandler legacy(Consumer<UIEngine> handler) {
    return (engine, descriptor) -> handler.accept(engine);
}
```

---

## Built-In Before Hooks

All constants are in `core.interactions.hooks.Before`:

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

All constants are in `core.interactions.hooks.After`:

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
typeInto(ActionHandler before, Typeable element, String text)

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

> ⚠️ **Deprecated:** In the modern Action/Flow/FlowExecutor path, hooks receive the `LocatorDescriptor`
> directly as a parameter — no need to access `UIContext`. The section below applies only
> to the legacy `Interactions` path.

Many legacy hooks operate on "the last resolved element" — this is stored in `UIContext`, a thread-local state holder:

```java
// What legacy hooks see internally (DEPRECATED):
LocatorDescriptor descriptor = UIContext.getLastActionTarget();  // @Deprecated(forRemoval = true)
String file     = UIContext.getLastPropertyFile();               // locator file name
String key      = UIContext.getLastKey();                        // locator key
Object[] args   = UIContext.getLastArgs();                       // template arguments
```

In the modern path, hooks receive everything they need as parameters:

```java
// Modern hook — descriptor is passed directly
ActionHandler myHook = (engine, descriptor) -> {
    // descriptor is the element being acted upon — no UIContext needed
    if (descriptor == null) return; // legacy guard
    engine.highlight(descriptor);
};
```

---

## Writing Custom Hooks

### Inline Lambda

The simplest approach — write a lambda directly in your hook list:

```java
// Modern path — fluent before/after on Action
executor.run(
    MyElements.SUBMIT_BUTTON.click()
        .before(
            Before.WAIT_FOR_ANGULAR_LOADER,
            (engine, descriptor) -> {
                // Custom: dismiss any visible cookie banner
                // Use engine methods — never reference WebDriver directly
                engine.click(engine.resolve(/* banner element */));
            }
        )
        .after(After.DO_NOTHING)
);

// Legacy path — Interactions
app.interaction().clickOn(
    List.of(
        Before.WAIT_FOR_ANGULAR_LOADER,
        (engine, descriptor) -> {
            // Custom hook logic using engine
        }
    ),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);
```

### Reusable Named Constants (Recommended)

The recommended pattern for any hook used more than once is a **constants-holder class** — a `final` class with a private constructor and `public static final` typed constants. This mirrors how the framework's own `core.interactions.hooks.After` and `Before` classes are built, and is the pattern demonstrated by `tests.demo.hooks.DemoHooks` in this repository.

Use `AfterActionHandler` or `BeforeActionHandler` as the field type (not the raw `ActionHandler` supertype) so callers can see at a glance whether a constant is a before or after hook.

```java
package tests.your.hooks;

import core.engine.LocatorDescriptor;
import core.interactions.hooks.AfterActionHandler;
import core.interactions.hooks.BeforeActionHandler;
import your.pages.SomePage;
import elements.meta.ElementRole;
import java.time.Duration;
import static core.logging.CustomLogger.debug;

public final class AppHooks {

    private AppHooks() {}

    /** Dismiss the cookie consent banner if present before interacting. */
    public static final BeforeActionHandler DISMISS_COOKIE_BANNER = (engine, descriptor) -> {
        // Use engine methods — never reference WebDriver directly
        engine.click(engine.resolve(SomePage.Overlay.COOKIE_BANNER, ElementRole.PRIMARY));
    };

    /** Wait for the app's custom spinner to disappear after the action. */
    public static final AfterActionHandler WAIT_FOR_APP_SPINNER = (engine, descriptor) -> {
        engine.waitForInvisible(engine.resolve(SomePage.Overlay.SPINNER, ElementRole.PRIMARY),
                Duration.ofSeconds(10));
        debug.log("[HOOK] Spinner gone.");
    };
}
```

Usage — compose with built-in hooks or mix freely:

```java
// Modern path — fluent on Action
MyElements.CHECKOUT_BUTTON.click()
        .before(AppHooks.DISMISS_COOKIE_BANNER, Before.WAIT_FOR_ELEMENT_CLICKABLE)
        .after(AppHooks.WAIT_FOR_APP_SPINNER)

// Or via a safe profile first, then layer additional hooks
MyElements.CHECKOUT_BUTTON.click()
        .safely()
        .after(AppHooks.WAIT_FOR_APP_SPINNER)
```

### Parameterized Hook Factory

For hooks that need runtime state (a specific element, a timeout, a message), use a static factory method returning the typed handler:

```java
public final class AppHooks {

    /** Wait for a specific element to disappear before acting. */
    public static BeforeActionHandler waitForAbsence(Element element, ElementRole role) {
        return (engine, descriptor) -> {
            LocatorDescriptor target = engine.resolve(element, role);
            engine.waitForInvisible(target, Duration.ofSeconds(10));
        };
    }

    /** Log a fixed message before the action — useful for step-level tracing. */
    public static BeforeActionHandler logMessage(String message) {
        return (engine, descriptor) -> debug.log("[HOOK] " + message);
    }
}

// Usage
MyElements.SUBMIT.click()
        .before(AppHooks.waitForAbsence(MyElements.LOADING_OVERLAY, ElementRole.PRIMARY))
        .after(After.DO_NOTHING)
```

---

## Hooks vs UIEngine Built-In Behavior

The UIEngine already handles many concerns that hooks traditionally addressed:

| Concern | UIEngine (built-in) | Hook (explicit) | When to Use Hook |
|---------|-------------------|-----------------|-----------------|
| Scroll to element | ✅ Automatic | `SCROLL_TO_ELEMENT` | Never (UIEngine handles it) |
| Wait for visible | ✅ Before click/type | `WAIT_FOR_ELEMENT_VISIBLE` | Extra safety before complex flows |
| Wait for clickable | ✅ Before click | `WAIT_FOR_ELEMENT_CLICKABLE` | Extra safety before complex flows |
| Highlight element | ❌ Not built-in | `HIGHLIGHT_ELEMENT` | Always (debugging aid) |
| Wait for Angular loader | ❌ App-specific | `WAIT_FOR_ANGULAR_LOADER` | Always (app-specific wait) |
| Wait for spinner | ❌ App-specific | `WAIT_FOR_SPIN_SPINNER_LOADER` | Always (app-specific wait) |
| JS click fallback | ✅ Automatic | N/A | Never (UIEngine handles it) |
| Stale element retry | ✅ Automatic | N/A | Never (UIEngine handles it) |

**Rule of thumb:** Use hooks for **app-specific concerns** (Angular loaders, custom spinners, cookie banners). Let UIEngine handle **browser interaction concerns** (scroll, waits, retries, fallback).

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

### 3. Guard Against Null Descriptors

Hooks in the legacy path may receive `null` descriptors. Guard gracefully:

```java
public static final ActionHandler MY_HOOK = (engine, descriptor) -> {
    if (descriptor == null) return;  // graceful no-op in legacy path
    // ... operate on descriptor via engine
};
```

### 4. Don't Swallow Exceptions Silently

Let meaningful exceptions propagate — VOID's retry and logging mechanisms depend on them:

```java
// ✅ Good — let TimeoutException propagate
public static final ActionHandler WAIT_FOR_PANEL = (engine, descriptor) -> {
    engine.waitForVisible(/* panel descriptor */, Duration.ofSeconds(10));
};

// ❌ Bad — hides failures
public static final ActionHandler WAIT_FOR_PANEL = (engine, descriptor) -> {
    try {
        engine.waitForVisible(/* panel descriptor */, Duration.ofSeconds(10));
    } catch (Exception e) {
        // silently ignored
    }
};
```

### 5. Prefer Action/Flow/FlowExecutor for New Code

For new test code, prefer the Action/Flow/FlowExecutor pipeline with **profiles** as the primary hook mechanism. Profiles apply capability-aware hooks with no manual wiring:

```java
// ✅ Preferred — safely() picks the right hooks automatically by capability
executor.run(Flow.of(
    MyPage.USERNAME.type("admin").safely(),
    MyPage.SUBMIT.click().safely()
));

// ✅ Profile + extra inline hook for app-specific logic
executor.run(
    MyPage.SUBMIT.click()
        .safely()
        .after((eng, desc) -> eng.waitForVisible(eng.resolve(ResultPage.BANNER, TEXT), Duration.ofSeconds(5)))
);

// ✅ Custom profile via builder — reusable across tests
ActionProfile myProfile = ActionProfile.builder()
    .before(Before.WAIT_FOR_ANGULAR_LOADER)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();
executor.run(MyPage.SUBMIT.click().using(myProfile));

// ✅ Manual hooks — directional fluent API for precise control
executor.run(
    MyPage.SUBMIT.click()
        .before(Before.WAIT_FOR_ANGULAR_LOADER)
        .after(After.HIGHLIGHT_ELEMENT)
);

// ✅ Also valid — legacy Interactions with hooks
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyPage.SUBMIT,
    List.of(After.DO_NOTHING)
);
```

### 6. Common Hook Combinations

| Scenario                          | Before Hooks                                            | After Hooks                            |
|-----------------------------------|---------------------------------------------------------|----------------------------------------|
| Standard click                    | `SCROLL_TO_ELEMENT`, `WAIT_FOR_ELEMENT_CLICKABLE`       | `DO_NOTHING`                           |
| Click after page load             | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_ELEMENT_CLICKABLE` | `DO_NOTHING`                           |
| Type into field                   | `WAIT_FOR_ELEMENT_VISIBLE`, `CLEAR_FIELD`               | `DO_NOTHING`                           |
| Submit form                       | `WAIT_FOR_ELEMENT_CLICKABLE`                            | `WAIT_FOR_ANGULAR_LOADER`              |
| Debugging / visual verification   | `HIGHLIGHT_ELEMENT`                                     | `HIGHLIGHT_ELEMENT`                    |

---

## Related Documentation

- [System Overview](system-overview.md) — full architecture with Action/Flow/FlowExecutor and UIEngine
- [Quick Start Guide](quick-start.md) — first test with hooks and Flow
- [Configuration Reference](configuration-reference.md) — all config keys
- [`Before.java`](../../src/main/java/core/interactions/hooks/Before.java) — before-hook source
- [`After.java`](../../src/main/java/core/interactions/hooks/After.java) — after-hook source
- [`UIEngine.java`](../../src/main/java/core/engine/UIEngine.java) — execution contract

---

*MIT License © 2025–2026 VOID Project*
