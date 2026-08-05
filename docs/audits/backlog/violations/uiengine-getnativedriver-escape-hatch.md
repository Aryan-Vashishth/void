---
name: uiengine-getnativedriver-escape-hatch
description: UIEngine.getNativeDriver() lets callers cast to WebDriver, breaking engine-neutrality — raw Selenium calls in test code bypass the UIEngine execution contract
metadata:
  type: project
  status: open
---

# `UIEngine.getNativeDriver()` — Engine-Neutrality Escape Hatch

**Principle:** ADR-007 (UIEngine is the single execution authority), ADR-018 (engine-agnostic layers must be Selenium-free)
**Area:** `domain/automation/web/engine/UIEngine.java` / test call sites
**Axis:** engine
**Discovered:** 2026-08-05 (SauceDemoTest VOID rewrite audit)
**Risk:** High (any test can silently bypass UIEngine; real engine-swap compatibility cannot be
verified while these call sites exist)

## What it is

`UIEngine` exposes:

```java
/** @deprecated use UIEngine methods instead of reaching into the native driver */
@Deprecated
Object getNativeDriver();
```

The return type is `Object` to preserve nominal engine-neutrality, but in practice callers
immediately cast to `WebDriver`:

```java
@SuppressWarnings("deprecation")
private WebDriver driver() {
    return (WebDriver) engine().getNativeDriver();
}
```

Every subsequent call through `driver()` is a raw Selenium call that bypasses `UIEngine`
entirely. This was observed in `SauceDemoTest` before the 2026-08-05 rewrite, where it was
used for:

- `driver().findElements(By.className("inventory_item_name"))` — bulk text collection
- `driver().findElement(By.cssSelector("[data-test='add-to-cart-...']")).click()` — slug-specific clicks
- `driver().findElements(...)` — element-presence checks

## Why it matters

| Risk | Impact |
|---|---|
| Engine swap breaks silently | Test code compiled against `WebDriver` will fail to compile or will throw `ClassCastException` when a non-Selenium engine is active, but the failure is at runtime, not design time |
| UIEngine wait/retry logic skipped | `driver().findElement()` has no implicit wait; `engine().click()` includes waitForClickable, scroll, and JS fallback. Escape-hatch calls are inherently less robust |
| Arbitrage grows | Once the pattern exists in one test, it spreads. Each new `driver()` call entrenches Selenium coupling further |
| Contract tested under wrong assumptions | Tests verify Selenium behavior, not UIEngine behavior. A future engine may implement the same action differently; tests written against `driver()` cannot catch that |

## Root cause

The escape hatch was introduced as a compatibility bridge and is `@Deprecated` in its own
Javadoc. The deprecation alone does not prevent use — it only logs a WARN. Without a
compile-error backstop, callers continue to accumulate.

The proximate trigger in SauceDemoTest was a gap in the UIEngine API: no
`getAllTexts(LocatorDescriptor)` method existed, forcing bulk text reads through raw
`driver().findElements()`. That gap has been closed on `hotfix/uiengine-get-all-texts`
(2026-08-05).

## Recommended fix

### Short term (non-breaking)

- Ensure `getNativeDriver()` is marked `@Deprecated(forRemoval = true)` with a clear removal
  milestone in its Javadoc.
- Add a `ArchUnit` or `KernelBoundaryRulesTest`-style rule that rejects `getNativeDriver()`
  call sites outside `domain.automation.web.selenium`.

### Long term (breaking, requires initiative)

Remove `getNativeDriver()` from the `UIEngine` interface entirely. Before removal:

1. Audit all call sites (test code + production code).
2. For each call site, identify the missing UIEngine API it is working around.
3. Add the missing API to UIEngine + implement in each engine.
4. Replace the escape-hatch call with the proper UIEngine call.
5. Remove `getNativeDriver()` from the interface.

This is the same approach applied in `hotfix/uiengine-get-all-texts` for the
`getAllTexts(LocatorDescriptor)` gap.

## Why not addressed now

The hotfix branch closes the specific API gap that forced `SauceDemoTest` to use the escape
hatch. A full audit of all remaining `getNativeDriver()` call sites (if any) and removal of
the method from the interface is a larger initiative requiring its own ADR. This entry tracks
the outstanding design debt.
