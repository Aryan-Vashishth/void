# 007 — UIEngine as Single Execution Authority

**Date:** 2026-06-01  
**Status:** Accepted

---

## Context

VOID's `Interactions` class was both orchestrator and executor — it resolved locators, ran hooks, AND directly called `WebDriver`, `WebDriverWait`, `JavascriptExecutor`, and `ExpectedConditions`. This tight coupling to Selenium prevented:

1. Engine portability (Playwright, CDP-direct, WebDriver BiDi)
2. Clean separation of intent (what) from execution (how)
3. Testability of orchestration logic without a browser

The `Interactions` class contained 11 instances of `driver.findElement(...)`, managed its own `WebDriver` and `WebDriverWait` fields, and mixed orchestration with execution in every method.

---

## Decision

Introduce `UIEngine` (`core.engine.UIEngine`) as the **single execution authority** for all browser interactions. `Interactions` becomes a pure orchestrator — it resolves locators, runs hooks, and delegates all execution to `UIEngine`.

### Key Components

| Component | Package | Responsibility |
|-----------|---------|----------------|
| `UIEngine` | `core.engine` | Execution contract — click, type, wait, scroll, retry, fallback |
| `SeleniumEngine` | `core.engine.selenium` | Selenium implementation of UIEngine |
| `LocatorDescriptor` | `core.engine` | Engine-agnostic locator record (value, strategy, args, parent) |
| `LocatorStrategy` | `core.engine` | Enum: XPATH, CSS, ID, NAME with inference |
| `EngineConfig` | `core.engine` | Configuration holder (timeout, polling, baseUrl) |
| `UIEngineFactory` | `core.engine` | Factory: config → engine instance |
| `Action` | `core.actions` | Deferred execution intent (functional interface) |
| `Flow` | `core.flow` | Immutable ordered sequence of Actions |
| `FlowExecutor` | `core.executor` | Iterates Flows against UIEngine |

---

## Reasoning

1. **Engine portability** — `UIEngine` is the single seam. Implementing `PlaywrightEngine` requires zero changes to elements, locators, DSL, or test code.
2. **Separation of concerns** — Intent (Action) is separate from execution (UIEngine). Elements emit Actions, UIEngine executes them.
3. **Deferred resolution** — Locator resolution happens inside Action lambdas at execution time, not at definition time. This prevents stale locators and enables lazy evaluation.
4. **UIEngine owns all execution concerns** — scroll, waits, retries, JS fallback, stale element recovery are all internal to the engine. Callers must NOT perform these.
5. **Backward compatibility** — `Interactions` is preserved as a frozen legacy orchestrator. All existing method signatures work unchanged. Legacy methods are `@Deprecated(forRemoval = true)`.

---

## Consequences

### Architecture

- Two execution paths: `Element → Action → Flow → FlowExecutor → UIEngine` (preferred) and `Element → Interactions → UIEngine` (legacy)
- 15 capability interfaces emit deferred `Action` objects: `Clickable.click()`, `Typeable.type()`, `Selectable.select()`, etc.
- `LocatorDescriptor` replaces `By` as the standard locator representation in new code
- `UIContext.getLastActionTarget()` replaces `UIContext.getLastElement()` for engine-agnostic state

### Files Changed

- New: `UIEngine`, `SeleniumEngine`, `LocatorDescriptor`, `LocatorStrategy`, `EngineConfig`, `UIEngineFactory`, `Action`, `Flow`, `FlowExecutor`
- Refactored: `Interactions` (removed WebDriver/WebDriverWait/ExpectedConditions, delegates to engine)
- Updated: `Via` (added `descriptor()` methods, deprecated `locator()`/`webElement()`)
- Updated: `UIContext` (added `lastActionTarget` ThreadLocal<LocatorDescriptor>)
- Updated: All 15 capability interfaces (added Action-emitting default methods)

### Deprecations

All methods accepting raw Selenium types are deprecated:
- `Interactions(WebDriver)` constructor
- `clickOn(WebElement)`, `clickOn(By)`
- `typeInto(WebElement, String)`, `typeInto(By, String)`
- `pressKey(WebElement, Keys)`, `typeIntoAndPress(..., Keys)`
- `selectFromDropdown(By, By)`
- `getTextByWebElement(By)`
- `Via.locator(...)`, `Via.webElement(...)`
- `UIContext.getLastElement()`

---

## Related

- [Experiment: Multi-Engine Execution](../experiments/active/2026-05-01-multi-engine-execution.md) — design vision and roadmap
- [Outcome: Interaction–Execution Separation](../experiments/outcomes/2026-06-interaction-execution-separation.md) — implementation details
- [004 — Dependency Philosophy](004-dependency-philosophy.md) — alignment with minimal-dependency principles

