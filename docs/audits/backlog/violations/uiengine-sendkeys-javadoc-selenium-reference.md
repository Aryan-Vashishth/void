---
name: uiengine-sendkeys-javadoc-selenium-reference
description: RESOLVED -- UIEngine.sendKeys Javadoc example previously referenced Keys.ESCAPE (Selenium type); fixed on feature/engine-decoupling before merge
metadata:
  type: project
---

# UIEngine -- sendKeys Javadoc References Selenium Keys [RESOLVED]

**Principle:** ADR-018 (engine-agnostic layers Selenium-free) -- documentation only
**File:** `src/main/java/core/engine/UIEngine.java`
**Discovered:** 2026-07-20 (post-implementation audit: core-utils-engine-agnostic)
**Resolved:** 2026-07-22 (pre-merge cleanup, feature/engine-decoupling)
**Risk:** Very Low (no compile or runtime impact; doc purity only)

## What it was

```java
/**
 * @param keys keys to send (e.g., {@code Keys.ESCAPE}, {@code Keys.TAB})
 */
void sendKeys(CharSequence... keys);
```

The `@param` Javadoc example referenced `org.openqa.selenium.Keys` inside `UIEngine`,
an engine-agnostic interface.

## Fix applied

Replaced with engine-neutral phrasing:

```java
/**
 * @param keys keys to send (e.g., the ESCAPE or TAB key, or any {@link CharSequence} key value)
 */
void sendKeys(CharSequence... keys);
```
