# Prospect: Multi-Engine Execution (Selenium ↔ Playwright)

**Status:** Active — Phases 1–2 Complete, Phase 3 Next  
**Date:** May 2026 (updated June 2026)  
**Scope:** Execution layer portability  
**Impact on user-facing API:** None

---

## 1. Goal

### What Portability Means in VOID

Portability means that the execution backend — the thing that actually drives the browser — becomes a **pluggable implementation detail**. Tests, element definitions, locator files, DSL usage, and hook declarations remain identical regardless of whether Selenium or Playwright (or any future engine) performs the underlying browser interaction.

```java
// This code does not change. Ever.
app.interaction().clickOn(AccountMappingElements.FilterPanel.ApplyButton.APPLY);

VoidDSL dsl = new VoidDSL(app.interaction());
dsl.clickOnFrom(...);
```

### What Problems It Solves

| Problem | How Portability Addresses It |
|---------|------------------------------|
| Selenium-only execution limits deployment options | Engine choice becomes a config switch |
| Playwright offers faster execution in CI/CD contexts | Teams can opt into Playwright without rewriting tests |
| Future engines (CDP-direct, WebDriver BiDi) require rewrites | New engines are additive — implement the interface, plug in |
| Cross-team disagreement on tooling | Same test suite, different engine per environment |

---

## 2. Current State

### Where Selenium Coupling Exists

VOID's architecture is already **largely engine-agnostic by design**:

- Element model → pure enums + interfaces. No Selenium types.
- Locator resolution → produces string-based locator descriptors. Engine-independent.
- DSL layer → delegates to `Interactions`. No direct driver calls.
- Hook system → declarative intent (`WAIT_FOR_ELEMENT_VISIBLE`). Not tied to Selenium's `WebDriverWait`.

The coupling lives in exactly **one layer**: `Interactions` and its immediate dependencies.

```
Coupled to Selenium:
├── core/interactions/Interactions.java    ← calls WebDriver directly
├── core/interactions/Via.java             ← uses WebElement, By
├── core/driver/DriverFactory.java         ← creates WebDriver instances
├── core/driver/DriverContext.java         ← holds WebDriver references
├── core/utils/DOMUtils.java              ← JavascriptExecutor, WebElement
├── core/utils/WaitUtils.java             ← WebDriverWait, ExpectedConditions
└── hooks (Before/After implementations)   ← WebDriverWait-based waits
```

Everything **above** this layer (elements, locators, DSL, test code) is already engine-agnostic. Everything **below** (browser) is engine-agnostic by nature.

The coupling is narrow, well-contained, and sits at a single architectural seam.

---

## 3. Design Principle

> **Elements and DSL define WHAT. The engine defines HOW.**

VOID already enforces a three-layer separation:

| Layer | Responsibility | Engine-Aware? |
|-------|---------------|---------------|
| **Intent** | What the user wants to do (`clickOn`, `typeInto`, `selectFromDropdown`) | No |
| **Resolution** | Which locator maps to which element, with what strategy | No |
| **Execution** | How the click/type/select physically happens in the browser | **Yes** |

The key insight: VOID's current design already treats the first two layers as contracts. The execution layer is the only one that needs to become polymorphic.

This is not a refactor. It's a formalization of a boundary that already exists.

---

## 4. Proposed Architecture

### Current

```
Test Code → VOID → Interactions → WebDriver → Browser
                        ↓
                   Hooks + Logging + Context
```

### Future

```
Test Code → VOID → Interactions → UIEngine (interface)
                        ↓                    ↓
                   Hooks + Logging    ┌──────┴──────┐
                                      │              │
                               SeleniumEngine   PlaywrightEngine
                                      │              │
                                  WebDriver      Playwright Page
                                      │              │
                                   Browser        Browser
```

### The `UIEngine` Interface

A single interface is introduced at the execution boundary. `Interactions` delegates physical browser operations to whatever `UIEngine` implementation is active.

Key characteristics:

- **One active engine per VOID instance** — no mid-test switching
- **Engine selection at bootstrap** — via config (`engine=selenium` or `engine=playwright`)
- **`Interactions` remains the single surface area** — it delegates, it doesn't split

### Role of Engine Implementations

Each engine implementation:

1. Accepts resolved locator descriptors (not raw `By` objects)
2. Performs the physical browser action
3. Returns standardized results (text, visibility state, element handle)
4. Manages its own wait/retry semantics internally

---

## 5. Execution Flow (Before vs After)

### Current Flow (Selenium-Only)

```
1. Test calls: app.interaction().clickOn(element)
2. Interactions resolves locator via LocatorResolvers
3. Before hooks execute (WebDriverWait-based)
4. Interactions calls driver.findElement(By.xpath(...)).click()
5. After hooks execute
6. Logging captures result
```

### Future Flow (Engine-Agnostic)

```
1. Test calls: app.interaction().clickOn(element)          ← UNCHANGED
2. Interactions resolves locator via LocatorResolvers      ← UNCHANGED
3. Before hooks execute (delegated to engine)              ← same intent, engine-specific impl
4. Interactions calls engine.click(locatorDescriptor)      ← NEW: delegated
5. After hooks execute (delegated to engine)               ← same intent, engine-specific impl
6. Logging captures result                                 ← UNCHANGED
```

From the test's perspective: nothing changes.  
From `Interactions`' perspective: it stops calling `WebDriver` directly and calls `UIEngine` instead.

---

## 6. Engine Abstraction

### What `UIEngine` Should Expose

The interface must cover VOID's **actual interaction vocabulary** — nothing more.

```java
public interface UIEngine {

    // Lifecycle
    void initialize(EngineConfig config);
    void shutdown();
    void navigateTo(String url);

    // Core actions
    void click(LocatorDescriptor locator);
    void type(LocatorDescriptor locator, String text);
    void clear(LocatorDescriptor locator);
    void selectByVisibleText(LocatorDescriptor locator, String text);
    void selectByValue(LocatorDescriptor locator, String value);

    // Retrieval
    String getText(LocatorDescriptor locator);
    String getAttribute(LocatorDescriptor locator, String attribute);
    boolean isVisible(LocatorDescriptor locator);
    boolean isEnabled(LocatorDescriptor locator);

    // Waits
    void waitForVisible(LocatorDescriptor locator, Duration timeout);
    void waitForClickable(LocatorDescriptor locator, Duration timeout);
    void waitForAbsence(LocatorDescriptor locator, Duration timeout);

    // Advanced
    void executeScript(String script, Object... args);
    void scrollTo(LocatorDescriptor locator);
    void uploadFile(LocatorDescriptor locator, String filePath);
    byte[] takeScreenshot();

    // Context
    Object getNativeDriver();  // escape hatch — returns WebDriver or Page
}
```

### Why Abstraction Must Remain Minimal

- Every method added is a method that **every engine must implement**
- VOID's interaction vocabulary is finite and known — it's exactly what `Interactions` already does
- The interface should mirror `Interactions`' action set, not Selenium's API surface
- Edge cases use `getNativeDriver()` as an explicit escape hatch (with logged warnings)

The abstraction is **not** a general-purpose browser interface. It's a VOID-specific execution contract.

---

## 7. Locator Handling

### Current Resolver Output

VOID's locator resolution system currently produces a **string locator value** + a **strategy hint** (xpath, css, id, etc.):

```
LocatorResolvers → resolves key → returns raw string like "//button[@id='apply']"
```

This string is then consumed by `Via` or `Interactions` and converted to a Selenium `By` object.

### How Engines Interpret Locators Differently

| Aspect | Selenium | Playwright |
|--------|----------|------------|
| XPath | Full support via `By.xpath()` | Full support via `page.locator("xpath=...")` |
| CSS | `By.cssSelector()` | Default strategy — `page.locator(".class")` |
| ID | `By.id()` | `page.locator("#id")` |
| Text-based | Not native | `page.getByText(...)`, `page.getByRole(...)` |

### Translation Layer

A `LocatorDescriptor` object bridges resolution and execution:

```java
public record LocatorDescriptor(
    String value,           // "//button[@id='apply']"
    LocatorStrategy strategy, // XPATH, CSS, ID, NAME
    String[] args           // dynamic substitution args (already applied)
) {}
```

Each engine implementation translates `LocatorDescriptor` into its native locator:

- **SeleniumEngine**: `LocatorDescriptor` → `By.xpath(value)` / `By.cssSelector(value)`
- **PlaywrightEngine**: `LocatorDescriptor` → `page.locator("xpath=" + value)` / `page.locator(value)`

The resolver doesn't change. The translation is engine-internal.

---

## 8. Hook & Wait Behavior

### Selenium vs Playwright Wait Models

| Aspect | Selenium | Playwright |
|--------|----------|------------|
| Default behavior | No waiting — immediate failure if not found | Auto-wait with configurable timeout |
| Explicit waits | `WebDriverWait` + `ExpectedConditions` | `locator.waitFor()` or built-in to actions |
| Retry model | Manual retry loops | Built into action methods |
| Loader guards | Custom hook implementation | Custom hook implementation |

### How Hooks Remain Conceptually Same

VOID's hook system declares **intent**, not implementation:

```java
List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ANGULAR_LOADER)
```

This is a declaration: "before clicking, ensure visibility and wait for the Angular loader to disappear."

Each engine fulfills the intent differently:

- **SeleniumEngine**: `WebDriverWait` + `ExpectedConditions.visibilityOfElementLocated(...)`
- **PlaywrightEngine**: `locator.waitFor({ state: 'visible' })` + custom loader check

The hook **constants** don't change. The hook **execution** is engine-specific.

Implementation approach:

```java
public interface HookExecutor {
    void executeBefore(BeforeHook hook, LocatorDescriptor target, UIEngine engine);
    void executeAfter(AfterHook hook, LocatorDescriptor target, UIEngine engine);
}
```

Each engine provides its own `HookExecutor` that knows how to fulfill hook intent natively.

---

## 9. Risks & Challenges

### Locator Compatibility

| Risk | Detail | Mitigation |
|------|--------|------------|
| XPath engine differences | Playwright uses a different XPath engine than browsers via Selenium | Test critical XPaths against both engines in CI |
| CSS pseudo-selectors | Minor behavioral differences | Avoid engine-specific pseudo-selectors in locator files |
| Shadow DOM | Selenium requires `shadowRoot` traversal; Playwright pierces by default | Engine-specific shadow DOM handling in `UIEngine` |

### Timing Differences

Playwright's auto-wait model means some `Before` hooks become redundant (e.g., `WAIT_FOR_ELEMENT_VISIBLE` before click — Playwright already does this). Options:

1. **Let hooks be no-ops in Playwright** when the engine natively handles the intent
2. **Log that the hook was satisfied natively** — maintain audit trail

Recommendation: Option 2. Logging consistency matters more than micro-optimization.

### Playwright Auto-Wait Behavior

Playwright's built-in waiting can conflict with VOID's explicit hook-based waits:

- Double-waiting (VOID waits, then Playwright waits again) — minor perf cost, no correctness issue
- Timeout conflicts — VOID's timeout vs Playwright's timeout must be aligned

Resolution: Engine config aligns Playwright's default timeout to VOID's configured wait timeout. VOID's hooks take precedence for explicitness.

### Logging Consistency

Logs must look identical regardless of engine:

```
[CLICK] AccountMappingElements.FilterPanel.ApplyButton.APPLY
  ├─ Locator: //button[@data-test='apply']
  ├─ Engine: playwright
  ├─ Hooks: [WAIT_FOR_ELEMENT_VISIBLE, HIGHLIGHT_ELEMENT]
  └─ Result: SUCCESS (142ms)
```

The logging system sits **above** the engine — it observes `Interactions`, not the engine internals. This is already the correct architecture.

---

## 10. Incremental Roadmap

### Phase 1: Introduce Engine Abstraction (No Behavior Change) ✅ COMPLETE

**Goal:** Extract the `UIEngine` interface from existing Selenium usage.

- ✅ Defined `UIEngine` interface (`core.engine.UIEngine`) — 40+ methods covering lifecycle, resolution, actions, retrieval, waits, and advanced operations
- ✅ Defined `LocatorDescriptor` record (`core.engine.LocatorDescriptor`) with scoped parent support
- ✅ Defined `LocatorStrategy` enum (`core.engine.LocatorStrategy`) — XPATH, CSS, ID, NAME with inference
- ✅ Defined `EngineConfig` holder (`core.engine.EngineConfig`) — timeout, polling, baseUrl
- ✅ Created `SeleniumEngine` implementing `UIEngine` (wrapping current WebDriver behavior)
- ✅ Refactored `Interactions` to pure orchestrator — delegates all execution to `UIEngine`
- ✅ Introduced `Action` functional interface (`core.actions.Action`) — deferred execution intent
- ✅ Introduced `Flow` (`core.flow.Flow`) — immutable ordered sequence of Actions
- ✅ Introduced `FlowExecutor` (`core.executor.FlowExecutor`) — iterates Flows against UIEngine
- ✅ All capability interfaces emit deferred `Action` objects (Clickable, Typeable, Selectable, etc.)
- ✅ All existing tests pass without modification

**Validation:** Zero behavior change. Pure structural refactor. Backward compatible.

**Actual scope:** `UIEngine`, `SeleniumEngine`, `LocatorDescriptor`, `LocatorStrategy`, `EngineConfig`, `Action`, `Flow`, `FlowExecutor`, `Interactions`, `Via`, `UIContext`, all 15 capability interfaces

---

### Phase 2: Runtime Engine Selection ✅ COMPLETE

**Goal:** Engine choice becomes configuration-driven.

- ✅ Added `engine` property to config (`selenium` | `playwright`)
- ✅ `UIEngineFactory` instantiates the correct engine at startup (switch-based dispatch)
- ✅ Engine name resolution: System property → ENV → config → default ("selenium")
- ✅ Engine lifecycle (init/shutdown) managed by `UIEngineFactory` + `EngineConfig`
- ✅ Fallback: defaults to Selenium if not specified
- ✅ `FrameworkBootstrap` performs one-time JVM initialization

**Validation:** Existing behavior unchanged. Config defaults to `selenium`.

---

### Phase 3: Playwright Prototype

**Goal:** Implement `PlaywrightEngine` covering core actions.

- Implement `click`, `type`, `getText`, `isVisible`, `navigateTo`
- Implement basic wait hooks
- Run a subset of existing tests against Playwright
- Identify and document behavioral gaps

**Validation:** Core smoke tests pass on both engines.

---

### Phase 4: Feature Parity

**Goal:** Full VOID capability on both engines.

- Implement all `UIEngine` methods for Playwright
- Implement all hook variants for Playwright
- Implement file upload, screenshots, script execution
- Run full test suite on both engines
- Document engine-specific behaviors and edge cases

**Validation:** Full test suite green on both engines.

---

### Phase 5: Optimization & Ecosystem

**Goal:** Leverage engine-native capabilities.

- Playwright: parallel browser contexts, trace viewer integration
- Playwright: network interception for loader detection
- Engine-specific performance tuning
- CI matrix: run tests on both engines in pipeline
- Documentation: engine comparison guide for teams

**Validation:** Performance benchmarks. Team adoption guide complete.

---

## 11. Final Outcome

After this evolution, VOID becomes:

### An Execution-Agnostic Automation System

```
┌─────────────────────────────────────────────────┐
│                   Test Code                       │
│  (elements, DSL, locators, hooks — UNCHANGED)    │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────┐
│               VOID Runtime                       │
│  (Interactions, LocatorResolvers, Logging)        │
└─────────────────────┬───────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────┐
│             UIEngine Interface                    │
└──────┬──────────────────────────────┬────────────┘
       │                              │
┌──────▼──────┐              ┌───────▼────────┐
│ SeleniumEngine│              │PlaywrightEngine │
│  (WebDriver) │              │    (Page)       │
└──────┬──────┘              └───────┬────────┘
       │                              │
       └──────────┬───────────────────┘
                  │
              Browser
```

### What This Means

| Aspect | Before | After |
|--------|--------|-------|
| Engine choice | Hardcoded Selenium | Config-driven (`engine=selenium\|playwright`) |
| Test code | Writes to VOID API | Writes to VOID API (unchanged) |
| Element model | Enums + interfaces | Enums + interfaces (unchanged) |
| Locator resolution | String-based, engine-agnostic | String-based, engine-agnostic (unchanged) |
| DSL | Delegates to Interactions | Delegates to Interactions (unchanged) |
| Execution | Direct WebDriver calls | Delegated to active `UIEngine` |
| New engine support | Rewrite Interactions | Implement `UIEngine` interface |

### The Core Promise

Same test code. Same element definitions. Same locator files. Same DSL. Same hooks. Same logs.

Different engine underneath.

```java
// This is the same on Selenium and Playwright.
// It will always be the same.
VOID app = new VOID();
app.interaction().clickOn(LoginPageElements.Actions.SIGN_IN_BUTTON);
app.interaction().typeInto(LoginPageElements.Credentials.USERNAME_INPUT, "admin@example.com");
app.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);
```

The engine is an implementation detail. The automation model is the constant.

---

## Why VOID's Current Design Enables This

This evolution is possible **because** of decisions already made:

1. **Elements are enums, not page objects wrapping WebElement** — no Selenium type leaks into element definitions
2. **Locators are resolved from external files as strings** — not constructed as `By` objects in test code
3. **All interactions go through one class** — single point of delegation, single seam to refactor
4. **Hooks are declarative** — they express intent, not implementation
5. **Logging sits above execution** — it observes actions, not driver internals

VOID wasn't designed for multi-engine support. But it was designed with **separation of concerns** rigorous enough that multi-engine support becomes a natural next step — not a rewrite.

---

*This document defines direction, not commitment. Implementation timing depends on team capacity and demand signal.*

