---
name: uiengine-sendkeys-javadoc-selenium-reference
description: UIEngine.sendKeys(CharSequence...) Javadoc example references Keys.ESCAPE (a Selenium type) inside an engine-agnostic interface
metadata:
  type: project
---

# UIEngine -- sendKeys Javadoc References Selenium Keys

**Principle:** ADR-018 (engine-agnostic layers Selenium-free) -- documentation only
**File:** `src/main/java/core/engine/UIEngine.java:357`
**Discovered:** 2026-07-20 (post-implementation audit: core-utils-engine-agnostic)
**Risk:** Very Low (no compile or runtime impact; doc purity only)

## What it is

```java
/**
 * @param keys keys to send (e.g., {@code Keys.ESCAPE}, {@code Keys.TAB})
 */
void sendKeys(CharSequence... keys);
```

The `@param` Javadoc example references `org.openqa.selenium.Keys`, a Selenium-specific
class, inside `UIEngine` -- an interface designed to be engine-agnostic. The interface
itself carries no Selenium import; the reference exists only in the comment.

## Why it matters

`UIEngine` is the documented contract that all engine implementations must satisfy.
Referencing Selenium types in its Javadoc implies to readers that callers must use Selenium
constants. A Playwright or BiDi engine's callers would be confused by this example.

## Recommended fix

Replace the Javadoc example with engine-neutral phrasing:

```java
/**
 * @param keys keys to send (e.g., {@code Keys.ESCAPE} or any {@link CharSequence} key value)
 */
```

Or, if the interface gains an engine-neutral key enum in the future, reference that instead.

**Estimated cost:** Minimal -- one Javadoc line.
