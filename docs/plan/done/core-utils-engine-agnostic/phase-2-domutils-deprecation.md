# Phase 2 -- DOMUtils Deprecation

**Status:** Complete
**Commit:** `e7e7142 refactor(utils): deprecate all DOMUtils public methods; point to UIEngine equivalents`

---

## Goal

Deprecate all public methods in `DOMUtils` and the class itself, pointing each to its
UIEngine equivalent. Resolves I1-A (ADR-007 violation).

---

## Changes

### `core/utils/web/DOMUtils.java`

Class-level `@Deprecated(forRemoval=true)` added. All six public methods annotated:

| Method | UIEngine equivalent |
|---|---|
| `scrollToElement(WebElement)` | `UIEngine.scrollTo(LocatorDescriptor)` |
| `hoverOnElement(WebElement)` | `UIEngine.hover(LocatorDescriptor)` |
| `switchToFrame(By)` | `UIEngine.switchToFrame(LocatorDescriptor)` |
| `switchToDefaultContent()` | `UIEngine.switchToDefaultContent()` |
| `sendKey(Keys)` | `UIEngine.sendKeys(CharSequence...)` |
| `sendEscapeKey()` | `UIEngine.sendKeys(Keys.ESCAPE)` |

No method bodies were changed. Internal logic and the DOMUtils -> WaitUtils call in
`switchToFrame()` remain intact to preserve backward compatibility until deletion.

---

## Incidental Observations

`DOMUtils.switchToFrame(By)` internally calls `WaitUtils.waitForCondition()` and
`UIContext.getLastElement()` -- both are deprecated paths. These references are
acceptable since the entire class is now deprecated for removal.

---

## Verification

```
mvn compile -q
grep -n "@Deprecated" src/main/java/core/utils/web/DOMUtils.java
```

Seven `@Deprecated` annotations (one on class, six on methods). Compile passes clean.
