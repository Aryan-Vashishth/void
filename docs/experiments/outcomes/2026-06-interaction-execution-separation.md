# Outcome: Interaction–Execution Separation

**Status:** Implemented  
**Date:** June 2026  
**Scope:** Decoupling orchestration from execution  
**Impact on user-facing API:** None (backward-compatible; legacy methods deprecated)

---

## Summary

`Interactions` is now a **pure orchestrator**. `UIEngine` is the **single authority for execution**. No exceptions.

All new code paths in `Interactions` use `LocatorDescriptor` (engine-agnostic) and delegate execution to `UIEngine`. Legacy methods accepting `WebDriver`, `WebElement`, `By`, and `Keys` are preserved but marked `@Deprecated(forRemoval = true)`.

---

## What Changed

### 1. UIEngine Contract Expanded (`core.engine.UIEngine`)

New methods added to the execution contract:

| Method | Purpose |
|--------|---------|
| `clickWithRetry(LocatorDescriptor)` | Full click pipeline: wait → click → JS fallback → stale retry |
| `typeAndSendKey(LocatorDescriptor, String, String)` | Atomic type + key press |
| `getTextWithAttributeFallback(LocatorDescriptor, String, String...)` | Text with attribute fallback for truncated/tooltip elements |
| `getCheckboxState(LocatorDescriptor)` | Multi-strategy checkbox state detection |
| `waitForOverlay(Duration)` | Angular CDK overlay wait |

### 2. LocatorDescriptor Enhanced (`core.engine.LocatorDescriptor`)

- Added optional `parent` field for scoped (parent→child) lookups
- `withParent(LocatorDescriptor)` — composable locator tree
- `isScoped()` — checks if descriptor has a parent context
- Engine interprets parent by finding parent element first, then searching within

### 3. SeleniumEngine Implements All New Methods

- `clickWithRetry` — migrated the full click pipeline from `Interactions` (4-phase: wait+highlight → standard click → JS fallback → re-locate)
- `findElement(LocatorDescriptor)` — respects scoped descriptors recursively
- All helper methods from `Interactions` (stale detection, URL change check, safe text extraction) moved into engine

### 4. Interactions Refactored to Pure Orchestrator

**Removed from Interactions:**
- `WebDriver driver` field
- `WebDriverWait wait` field
- All `driver.findElement(...)` calls (11 instances)
- All `wait.until(...)` calls
- All `DOMUtils.scrollToElement(...)` calls
- All `JavascriptExecutor` casts
- All `ExpectedConditions` usage
- `tryClickWithHooks` / `performClick` / `safeText` internal methods
- `waitForOverlayToAppear` internal method

**Every public method now follows this pattern:**
```
resolve → LocatorDescriptor
set UIContext (descriptor + action target)
execute before-hooks
delegate to engine.*(descriptor)
execute after-hooks
```

**Legacy bridges preserved (all `@Deprecated(forRemoval = true)`):**
- `Interactions(WebDriver)` constructor
- `clickOn(WebElement)`, `clickOn(By)`
- `clickOnWithin(WebElement, ClickTarget)`
- `typeInto(WebElement, String)`, `typeInto(By, String)`
- `typeIntoAndPress(TextInputTarget, String, Keys)`
- `pressKey(WebElement, Keys)`
- `appendTo(WebElement, String)`, `clearField(WebElement)`
- `performSearch(...)` with raw WebElements
- `selectFromDropdown(By, By)`
- `getTextByWebElement(By)`
- `isAnyDisplayed(By, ...)`

### 5. Action Interfaces Enriched

| Interface | New Method | Behavior |
|-----------|-----------|----------|
| `ClickAction` | `clickWithRetry()` | Delegates to `engine.clickWithRetry()` |
| `TextInputAction` | `typeAndPress(text, key)` | Delegates to `engine.typeAndSendKey()` |
| `TextInputAction` | `append(text)` | Delegates to `engine.appendType()` |
| `CheckboxAction` | `set(boolean)` | Reads state → conditionally toggles |
| `DropdownAction` | `select()` | Open → wait overlay → click option |
| `SearchableDropdownAction` | `searchAndSelect(term)` | Open → type → wait → click result |
| `MultiDropdownAction` | `selectAtIndex(index, label)` | Open Nth → wait overlay → click option |
| `ReadOnlyAction` | `readText()` | Scroll + read text via engine |

### 6. UIContext Made Engine-Agnostic

- Added `lastActionTarget` (`ThreadLocal<LocatorDescriptor>`)
- `setLastActionTarget(LocatorDescriptor)` / `getLastActionTarget()` — preferred API
- `setLastElement(WebElement)` / `getLastElement()` — deprecated

### 7. Via.java Deprecation Layer

- Added `descriptor(Element)`, `descriptor(Element, ElementRole, ...)`, `descriptor(String, String, ...)` — engine-agnostic
- Deprecated all `locator(...)` methods (return `By`)
- Deprecated all `webElement(...)` methods (return `WebElement`)

### 8. Action.java Javadoc Corrected

- Fixed incorrect claim that "descriptor is resolved eagerly at creation time"
- Updated to document the actual deferred-resolution pattern

---

## Architecture After

```
User Code
  │
  ├── Action → Flow → Runner → UIEngine  (preferred: fully engine-agnostic)
  │
  └── Interactions (orchestrator)
        ├── resolve → LocatorDescriptor
        ├── execute before-hooks
        ├── delegate → UIEngine
        ├── execute after-hooks
        └── maintain UIContext

UIEngine (executor)
  ├── translate LocatorDescriptor → native locator
  ├── handle waits, retries, fallbacks
  └── execute browser action
```

---

## Design Decisions

1. **No `ActionRequest` abstraction** — The existing `Action` functional interface (`core.actions.Action`) is the sole execution currency. `Interactions` delegates directly to `engine.*` methods rather than introducing a competing request/dispatch model.

2. **Dispatcher is internal, not user-facing** — The `executeHooks` helper in `Interactions` routes hooks, but there is no exposed dispatcher registry. All user-facing execution flows through `Action → Flow → Runner → UIEngine` or `Interactions → UIEngine`.

3. **`LocatorDescriptor.withParent()` instead of `findWithin()`** — Scoped operations use a composable locator tree rather than a method explosion on UIEngine. The engine recursively resolves parent→child at execution time.

4. **Hook `ActionHandler` unchanged** — The existing `core.interactions.hooks.ActionHandler` keeps its name and semantics. No dispatch handler was needed since we route directly to engine methods.

---

## Success Criteria (Met)

- [x] No execution logic in `Interactions` (new code paths)
- [x] No `WebDriver` / `WebElement` / `ExpectedConditions` in new code paths
- [x] All actions routed through `UIEngine`
- [x] Both source and test code compile cleanly
- [x] All method signatures preserved (backward compatible)
- [x] Legacy methods deprecated with clear migration path

