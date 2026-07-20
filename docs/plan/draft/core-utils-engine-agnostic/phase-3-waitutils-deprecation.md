# Phase 3 -- WaitUtils Deprecation

**Status:** Complete
**Commit:** `2c53804 refactor(utils): deprecate WaitUtils By-based API and Angular CDK selector fields`

---

## Goal

Deprecate the two hardcoded Angular CDK selector fields and all public `By`-based
wait methods in `WaitUtils`. Resolves I1-C (ADR-007 and ADR-018 violations).

---

## Changes

### `core/utils/web/WaitUtils.java`

**Fields deprecated (ADR-018 -- application-specific selectors in framework layer):**

| Field | Reason |
|---|---|
| `ANGULAR_LOADER = By.tagName("app-loader")` | Application-specific Angular CDK selector in framework code |
| `SPIN_SPINNER_LOADER = By.xpath(...)` | Application-specific spinner selector in framework code |

**Methods deprecated (ADR-007 -- By-based API bypasses UIEngine):**

| Method | UIEngine equivalent |
|---|---|
| `waitForElementToBeVisible(By)` | `UIEngine.waitForVisible(LocatorDescriptor, Duration)` |
| `waitForElementToDisappear(By)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `waitForElementToDisappear(ReadOnly)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `waitForElementToBeAbsent(By, int)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `waitForElementToBeClickable(By)` | `UIEngine.waitForClickable(LocatorDescriptor, Duration)` |
| `resolveAngularLoader()` | `UIEngine.waitForOverlay(Duration)` |
| `resolveAngularLoader(int, int, int, boolean)` | `UIEngine.waitForOverlay(Duration)` |
| `resolveLoader(By)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `resolveLoader(By, boolean)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `resolveLoader(By, int, int)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `resolveLoader(By, int, int, int, boolean)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |
| `waitForElementTextToBePresent(By, String, int)` | `UIEngine.getText(LocatorDescriptor)` |
| `waitForElementTextToBePresent(By, String)` | `UIEngine.getText(LocatorDescriptor)` |
| `waitForElementTextToBePresent(By)` | `UIEngine.getText(LocatorDescriptor)` |
| `waitForElementTextToBePresent(By, int)` | `UIEngine.getText(LocatorDescriptor)` |
| `resolveLoaderTemp(By, int, int, int, boolean)` | `UIEngine.waitForAbsence(LocatorDescriptor, Duration)` |

**Not deprecated (internal / non-public):**

`waitForCondition(WebDriver, ExpectedCondition, ...)` and
`waitForCondition(String, Duration, Duration, Supplier<Boolean>)` remain non-deprecated.
They are called internally from deprecated methods and are not part of the public
engine-coupling violation -- they are implementation utilities.

`isLoaderPresent(WebDriver, By, boolean)` is private and unchanged.

---

## Incidental Observations

`waitForElementToDisappear(ReadOnly element)` calls `LocatorResolvers.strict().resolve(element)`
(the deprecated By-returning path). This is acceptable since the entire method is now
deprecated. No fix needed -- the deprecated method calling a deprecated resolver is
consistent behavior.

---

## Verification

```
mvn compile -q
grep -n "@Deprecated" src/main/java/core/utils/web/WaitUtils.java
grep -n "ANGULAR_LOADER\|SPIN_SPINNER" src/main/java/core/utils/web/WaitUtils.java
```

All public method signatures and both By fields carry `@Deprecated`. Compile passes clean.
