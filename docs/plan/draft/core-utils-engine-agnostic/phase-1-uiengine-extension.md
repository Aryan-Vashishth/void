# Phase 1 -- UIEngine Extension

**Status:** Complete
**Commit:** `5b1a16b feat(engine): add switchToFrame, switchToDefaultContent, and sendKeys to UIEngine`

---

## Goal

Add the three UIEngine interface methods that have no existing equivalent, required
before DOMUtils can be deprecated with valid Javadoc references.

---

## Changes

### `core/engine/UIEngine.java`

Three methods added to the ADVANCED section:

```java
void switchToFrame(LocatorDescriptor locator);
void switchToDefaultContent();
void sendKeys(CharSequence... keys);
```

### `core/engine/selenium/SeleniumEngine.java`

Three `@Override` implementations added after `hover()`:

```java
@Override
public void switchToFrame(LocatorDescriptor locator) {
    By by = toBy(locator);
    WebElement frame = new WebDriverWait(driver, defaultTimeout)
            .until(ExpectedConditions.presenceOfElementLocated(by));
    driver.switchTo().frame(frame);
    debug.log("[SeleniumEngine] Switched to frame: " + labelFor(locator));
}

@Override
public void switchToDefaultContent() {
    driver.switchTo().defaultContent();
    debug.log("[SeleniumEngine] Switched to default content.");
}

@Override
public void sendKeys(CharSequence... keys) {
    new Actions(driver).sendKeys(keys).perform();
    debug.log("[SeleniumEngine] Sent global keys.");
}
```

---

## Verification

```
mvn compile -q
grep -n "switchToFrame\|switchToDefaultContent\|sendKeys" src/main/java/core/engine/UIEngine.java
```

Both greps return matches. Compile passes clean.
