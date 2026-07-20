---
name: waiter-returns-webdriverwait
description: Waiter.get() and Waiter.get(int) return WebDriverWait (Selenium type) from public non-deprecated methods -- ADR-007 violation with active non-deprecated callers
metadata:
  type: project
---

# Waiter -- Returns WebDriverWait from Public Non-Deprecated API

**Principle:** ADR-007 (UIEngine execution authority)
**File:** `src/main/java/core/driver/Waiter.java`
**Discovered:** 2026-07-20 (post-implementation audit: core-utils-engine-agnostic)
**Risk:** Medium (active callers in non-deprecated production code)

## What it is

```java
public static WebDriverWait get() { ... }
public static WebDriverWait get(int timeoutSeconds) { ... }
```

Both methods return `WebDriverWait` -- a Selenium-specific type -- from public non-deprecated
static methods. Three active non-deprecated callers:

| File | Usage |
|---|---|
| `core/utils/web/Upload.java:52` | `Waiter.get().until(ExpectedConditions.presenceOfElementLocated(locator))` |
| `core/utils/web/KeyValuePairHandler.java:45` | `Waiter.get().until(ExpectedConditions.visibilityOfElementLocated(by))` |
| `core/engine/EnumResolver.java:24` | `Waiter.get().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator))` |

All three callers also pass Selenium `By` locators and receive `WebElement` results --
meaning the violation extends into the callers.

## Why it was not addressed in core-utils-engine-agnostic

`Waiter.java` was not in the violation map for I1-A, I1-B, or I1-C. Surfaced during the
post-implementation audit as a pre-existing gap.

## Recommended fix

Deprecate `Waiter` and migrate callers to UIEngine wait methods. Each caller's wait
operation has a UIEngine equivalent:

- `presenceOfElementLocated(locator)` -> `UIEngine.waitForPresence(LocatorDescriptor, Duration)`
- `visibilityOfElementLocated(locator)` -> `UIEngine.waitForVisible(LocatorDescriptor, Duration)`
- `visibilityOfAllElementsLocatedBy(locator)` -- no direct equivalent yet; requires UIEngine extension

**Estimated cost:** Dedicated -- callers need LocatorDescriptor migration and UIEngine access.
