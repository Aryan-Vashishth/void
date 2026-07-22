---
name: i1-tablehandler-direct-webdriver-calls
description: TableHandler calls driver.findElements() directly in non-deprecated static methods -- ADR-007 violation
metadata:
  type: project
---

# I1-B: TableHandler -- Direct WebDriver Calls in Non-Deprecated Methods

**Principle:** ADR-007 (UIEngine is the single execution authority)
**File:** `src/main/java/core/utils/web/TableHandler.java`
**Discovered:** 2026-07-20 (architecture-rules.md audit on hotfix/engine-decoupling-final-audit)
**Risk:** High

## What it does

`TableHandler` is a static utility class in `core.utils.web` that reads HTML table data.
Its non-deprecated methods call `driver.findElements()` directly with Selenium `By`
locators, bypassing the `UIEngine` abstraction and the `LocatorDescriptor` resolution path.

## Code

Representative pattern:

```java
public static List<String> getColumnValues(WebDriver driver, By tableLocator, int columnIndex) {
    List<WebElement> rows = driver.findElements(tableLocator);
    // ...
}
```

## Why this violates ADR-007

ADR-007: nothing outside `UIEngine` implementations calls `WebDriver` methods directly.
`TableHandler` is in `core.utils.web`, outside any engine implementation. Every method
accepting `WebDriver` and `By` bypasses the engine contract. A Playwright engine would
silently break any caller that routes through `TableHandler`.

## Recommended fix

Add table-reading capability to `UIEngine` (e.g. `engine.getTableCellText(LocatorDescriptor, int, int)`)
and implement in `SeleniumEngine`. Deprecate `TableHandler` methods pointing to the new
engine API. Remove at next major version.

**Estimated cost:** Dedicated -- requires new UIEngine contract methods. Scope as part of
the engine-capabilities initiative alongside DOMUtils (see [[i1-domutils-direct-webdriver-calls]]).
