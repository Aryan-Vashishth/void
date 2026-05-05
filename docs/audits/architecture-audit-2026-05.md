# VOID Framework — Architectural Audit

**Date:** 2026-05-05  
**Scope:** Full codebase analysis — element, action, flow, engine, hooks, runtime, DSL  
**Goal:** Evaluate engine-swap readiness, identify coupling, duplication, and migration risks

---

## 1. Current Architecture Summary

VOID is a UI automation framework in transition from a Selenium-coupled, imperative `Interactions` class to a declarative, engine-agnostic pipeline:

```
Element → Action → Flow → FlowExecutor → UIEngine
```

The new pipeline is well-designed in isolation but coexists with a large (~834-line) legacy `Interactions` class and supporting utilities (`Via`, `UIContext`, `DriverContext`, `VoidDSL`) that still reference Selenium types directly.

### Layer Overview

| Layer | Key Types | Status |
|-------|-----------|--------|
| Element | `Element`, capability interfaces (`Clickable`, `Typeable`, etc.) | ✅ Clean, engine-agnostic |
| Action | `Action`, `ElementActions`, `HookedAction` | ✅ Clean contract (Beta) |
| Flow/Executor | `Flow`, `FlowExecutor` | ✅ Minimal, clean |
| Engine | `UIEngine` (interface), `SeleniumEngine`, `LocatorDescriptor` | ✅ Solid abstraction |
| Legacy Orchestrator | `Interactions` (frozen, deprecated) | ⚠️ 834 lines, Selenium leakage |
| DSL | `VoidDSL` | ⚠️ Delegates to `Interactions`, imports `By`/`WebElement` |
| Runtime | `VOID`, `DriverContext`, `ExecutionContext`, `FrameworkBootstrap` | ⚠️ WebDriver-coupled startup + global bootstrap state |
| Hooks | `ActionHandler`, `Before`, `After` | ✅ Engine-agnostic |
| Context | `UIContext` | ⚠️ Global mutable state, deprecated paths still used |
| Resolver | `LocatorResolver`, `LocatorResolvers` | ⚠️ Dual-returns (`By` + `LocatorDescriptor`) |

---

## 2. Key Issues

### Critical

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| C1 | `Interactions` constructor forces `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver())` — any non-Selenium engine will ClassCastException | `Interactions.java:68` | **Hard blocker for engine swap** |
| C2 | `UIEngineFactory.create()` requires a `WebDriver` parameter — semantically nonsensical for Playwright | `UIEngineFactory.java:41` | **Hard blocker for engine swap** |
| C3 | `ExecutionContext` holds `WebDriver` directly — foundational context is Selenium-coupled | `ExecutionContext.java:33` | **Hard blocker for engine swap** |
| C4 | `FrameworkBootstrap.init()` hard-fails on missing `driver.properties` before engine selection; bootstrap is Selenium-gated | `FrameworkBootstrap.java:47-52` | **Hard blocker for non-Selenium startup** |
| C5 | **Resolution ownership violation:** resolution exists in `Action`, `UIEngine.resolve(...)`, and `Interactions` (`LOCATORS.resolveDescriptor(...)`) | `Action.java`, `UIEngine.java`, `Interactions.java:55,94-115` | **Three sources of truth; architecture cannot converge** |
| C6 | `VoidDSL` leaks Selenium (`By`, `WebElement`) in public API surface | `VoidDSL.java:13-14,304-308` | **Framework remains Selenium-bound even if engine layer is abstracted** |
| C7 | `UIEngine` still accepts `Element` in `resolve(Element, ...)`, creating a resolution backdoor in the execution layer | `UIEngine.java` | **Breaks single-model contract; engine can bypass Action ownership** |

### High

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| H1 | Dual resolution paths: `Interactions` uses `LocatorResolvers.strict()` independently of engine's `resolve()` | `Interactions.java:55` vs `UIEngine.resolve()` | Inconsistent resolution, potential divergence |
| H2 | `UIContext` — 5 ThreadLocals tracking last element/descriptor/meta with no cleanup guarantee | `UIContext.java` | Thread-leak risk, hidden coupling |
| H3 | `Via` class returns `WebDriver`, `WebElement`, `By` — deprecated methods still present and referenced | `Via.java:246-302` | Ongoing Selenium coupling in callers |
| H4 | `VOID.getDriver()` accessor exposes raw `WebDriver` to subclasses | `VOID.java:145-147` | Subclasses couple to Selenium |
| H5 | `VOID.start(profile)` creates Selenium `WebDriver` before engine creation, forcing Selenium-first lifecycle even when engine is configurable | `VOID.java:104-111` | Engine swap path is structurally blocked |
| H6 | `FrameworkBootstrap.getUtilsConfig()` exposes mutable static `Properties` directly, enabling hidden cross-session mutation | `FrameworkBootstrap.java:31,73-75` | Global state coupling and unpredictable behavior |
| H7 | `Interactions` still acts as an execution surface instead of strict adapter, allowing permanent dual-pipeline drift | `Interactions.java` | New logic can keep bypassing Action/Flow/Runner path |

### Medium

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| M1 | `Interactions.selectFromDropdown` with `useJSExecutor` does nothing — both branches call `engine.click()` identically | `Interactions.java:419-436` | Dead code, misleading API |
| M2 | No `FlowExecutor` integration in `VOID`/`VoidDSL` — new pipeline not wired into runtime | `VOID.java` | Users must manually create FlowExecutor |
| M3 | `HookedAction.wrap()` deprecated but still exposed publicly | `HookedAction.java:87-101` | Confusion about hook usage |
| M4 | `Interactions.executeHooks()` passes `null` descriptor to hooks | `Interactions.java:123-130` | Element-dependent hooks silently no-op |
| M5 | `waitForOverlay()` hardcodes `div.cdk-overlay-pane` in SeleniumEngine — Angular Material assumption | `SeleniumEngine.java:350-361` | Not configurable, framework-specific |
| M6 | Missing UIEngine methods: no `doubleClick`, `rightClick`, `dragAndDrop`, `switchFrame`, `switchWindow` | `UIEngine.java` | Forces callers to `getNativeDriver()` escape hatch |
| M7 | `VOID.shutdown()` quits `DriverManager` but does not call `UIEngine.shutdown()`; engine-owned resources may leak | `VOID.java:124-127` | Lifecycle inconsistency and cleanup risk |

---

## 3. Duplication & Redundancy Map

| Concept | Path A (new pipeline) | Path B (legacy) |
|---------|----------------------|-----------------|
| Click execution | `Clickable.click()` → Action → FlowExecutor → `UIEngine.click()` | `Interactions.clickOn(Clickable)` → `UIEngine.click()` |
| Locator resolution | `UIEngine.resolve(Element, role)` (proxies `LocatorResolvers`) | `Interactions.LOCATORS.resolveDescriptor()` (same resolver, different entry) |
| Hook wrapping | `Action.withHooks()` / `HookedAction` (explicit descriptor) | `Interactions.executeHooks()` (null descriptor, pre/post manual) |
| By ↔ Descriptor | `SeleniumEngine.toBy(LocatorDescriptor)` | `SeleniumEngine.fromBy(By)` (fragile toString parsing) |
| Context tracking | Descriptor passed via `HookedAction` params | `UIContext.setLastActionTarget()` (global ThreadLocal) |
| WebElement resolution | Abstracted away in UIEngine | `Via.webElement(...)` / `DriverContext.getActiveDriver() + findElement` |
| Dropdown execution | `Selectable.select()` (Action — 1 method) | `Interactions.selectFromDropdown()` (6+ overloads) |
| Type execution | `Typeable.type(text)` (1 Action) | `Interactions.typeInto(...)` (5 overloads, 3 deprecated) |

---

## 4. Coupling & Leakage Points

| Location | Coupling Type | Selenium API Leaked | Swap Impact |
|----------|---------------|---------------------|-------------|
| `Interactions` constructor (line 68) | Hard cast | `WebDriver` | 💀 ClassCastException |
| `UIEngineFactory.create()` (line 41) | Parameter type | `WebDriver` | 💀 Cannot instantiate |
| `ExecutionContext` (field) | Field type | `WebDriver` | 💀 Cannot construct |
| `DriverContext` (entire class) | Managed type | `WebDriver` | 💀 Useless for Playwright |
| `VOID.getDriver()` | Protected accessor | `WebDriver` | ⚠️ Subclasses break |
| `VoidDSL.verifyElementsAreVisible` | Method call | `By` (via deprecated overload) | ⚠️ Compile error |
| `Via.locator()` methods | Return type | `By` | ⚠️ Deprecated but used |
| `Via.webElement()` methods | Return type | `WebElement`, `WebDriver` | ⚠️ Deprecated but used |
| `LocatorResolver.resolve()` | Return type | `org.openqa.selenium.By` | ⚠️ Exposes Selenium in API |
| `DOMUtils` | Usage | `UIContext.getLastElement()` → `WebElement` | ⚠️ Silent failure |
| `Interactions` deprecated overloads | Parameter type | `WebElement`, `By`, `Keys` | ⚠️ Active callers exist |
| `FrameworkBootstrap.init()` | Bootstrap gate | `driver.properties` presence required up front | 💀 Blocks non-Selenium bootstraps |
| `FrameworkBootstrap.getUtilsConfig()` | Global mutable state | Shared mutable `Properties` object | ⚠️ Hidden cross-test coupling |
| `VOID.start(profile)` | Startup order coupling | Driver created before engine selection | 💀 Prevents true engine-first bootstrap |
| `VOID.shutdown()` | Lifecycle coupling | Driver manager cleanup without engine cleanup | ⚠️ Resource leak risk for non-WebDriver engines |

---

## 5. UIEngine Evaluation

### Strengths

- ✅ Clean single interface with grouped responsibilities (lifecycle, resolution, actions, retrieval, waits, advanced)
- ✅ `LocatorDescriptor` + `LocatorStrategy` provide a solid engine-agnostic locator model
- ✅ No Selenium types in the interface — perfectly portable
- ✅ `click()` encapsulates all retry/fallback logic — callers don't choose strategy
- ✅ `getCheckboxState()` and `getTextWithAttributeFallback()` are higher-level conveniences that reduce caller complexity
- ✅ Naming is consistent — no redundant variants (no `clickWithRetry`/`jsClick`)

### Weaknesses

| Issue | Severity | Detail |
|-------|----------|--------|
| `resolve()` contract is misleading | **Critical** | `UIEngine.resolve(Element...)` implies engine-owned resolution, but actual ownership lives in `LocatorResolvers`; this duplicates Action-level intent resolution and should be deprecated for element-based paths |
| `waitForOverlay()` is Angular-specific | Medium | Hardcodes `div.cdk-overlay-pane`; should be configurable or moved to a hook |
| `getNativeDriver()` returns `Object` | Low | Safe but fragile — callers inevitably downcast with no type guard |
| Missing interaction methods | Medium | No `doubleClick`, `rightClick`, `dragAndDrop`, `switchFrame`, `switchWindow`, `acceptAlert`, `dismissAlert` |
| No fluent/builder for actions | Low | Can't compose multi-step engine operations atomically (e.g., type + press in one engine call) |

### Resolution Ownership Rule (required)

- **Single owner:** only `Action` resolves element intent into executable descriptors
- `UIEngine` should execute descriptors only; it should not resolve `Element`
- **Contract rule:** `UIEngine` must never accept `Element`; execution inputs are `LocatorDescriptor` only
- `Interactions` must not resolve at all once adapter conversion starts
- `LocatorResolvers` usage must be confined to Action construction path (or dedicated resolver adapter used by Action only)

---

## 6. Hook System Evaluation

### Strengths

- ✅ `ActionHandler` is a clean `@FunctionalInterface` — `(UIEngine, LocatorDescriptor) → void`
- ✅ `Before`/`After` constants are fully engine-agnostic — only call UIEngine methods
- ✅ `Action.withHooks()` composes fluently and resolves descriptor once (shared across all hooks)
- ✅ Failure semantics are well-defined and documented (before-fails → abort; after-fails → propagate)
- ✅ Custom hooks are trivial lambdas

### Weaknesses

| Issue | Severity | Detail |
|-------|----------|--------|
| Package location | Medium | Hooks live in `core.interactions.hooks` — ties them to the legacy layer; they serve both pipelines |
| Null descriptor in legacy path | High | `Interactions.executeHooks()` passes `null` — element-dependent hooks silently degrade |
| No global hook mechanism | Medium | Hooks are manually passed at every call site; no "always-run" registration |
| No hook ordering/priority | Low | Hooks run in list order only — no explicit priority system |
| `ActionHandler.legacy()` adapter | Low | Deprecated single-arg bridge signals old patterns still linger |

### Hook Ownership Rule (required)

- Hooks are part of the **Action pipeline contract**, not an `Interactions` feature
- Canonical shape: `Action` decorator (`withHooks`) → `Flow` → `Runner` → `UIEngine`
- Hooks must not access `UIContext` or any global mutable state
- Hooks should not depend on global context (`UIContext`) for target resolution

---

## 7. Engine Swap Readiness (Selenium → Playwright)

### Ready Today (No Changes Needed)

| Component | Notes |
|-----------|-------|
| `UIEngine` interface | Fully portable — no Selenium types |
| `LocatorDescriptor` / `LocatorStrategy` | Engine-agnostic; Playwright maps naturally |
| `Action` / `Flow` / `FlowExecutor` | Pure engine delegation |
| Capability interfaces (`Clickable`, `Typeable`, etc.) | No Selenium imports anywhere |
| `ActionHandler` / `Before` / `After` hooks | Use UIEngine methods only |
| `ElementActions.of()` | Pure factory, no coupling |

### Hard Blockers (Must Fix First)

| Component | Reason | Effort |
|-----------|--------|--------|
| `UIEngineFactory.create(Properties, WebDriver)` | Requires `WebDriver` param | Small — refactor signature |
| `ExecutionContext(Properties, WebDriver)` | Stores `WebDriver` | Small — replace with engine-holding context |
| `Interactions` constructor line 68 | `(WebDriver) engine.getNativeDriver()` | Small — remove cast or guard |
| `DriverContext` | Entire class assumes `WebDriver` | Medium — needs engine-agnostic equivalent or bypass |
| `FrameworkBootstrap.init()` | Enforces Selenium-oriented `driver.properties` existence before engine choice | Small — make bootstrap engine-aware |
| `VOID.start(profile)` | Calls `DriverManager.createDriver(...)` before selecting engine | Small — move driver creation into engine factory |
| `VoidDSL` public API | Public methods and helpers use Selenium types (`By`/`WebElement`) | **Critical** — blocks framework-level engine portability |
| Resolution ownership split | Resolution currently distributed across Action/Engine/Interactions | **Critical** — prevents single execution model |

### Soft Blockers (Can Work Around But Should Fix)

| Component | Reason |
|-----------|--------|
| `VoidDSL.verifyElementsAreVisible` | Calls `engine.isAnyDisplayed(By)` — deprecated overload |
| `Via.locator()` / `webElement()` | Return Selenium types — callers must migrate |
| `LocatorResolver.resolve() → By` | Selenium-specific return type exposed in shared API |
| `VOID.getDriver()` | Subclasses depend on `WebDriver` accessor |
| `Interactions` deprecated overloads | Accept `WebElement`, `By`, `Keys` params |

### Verdict

> The **new pipeline** (Element → Action → Flow → FlowExecutor → UIEngine) is **Playwright-ready today**.  
> The **runtime wiring** (VOID, factory, context, DriverContext) is **not**.  
> A Playwright engine could be implemented but **cannot be started** without refactoring the bootstrap and context layers.

---

## 8. Recommended Direction

1. **Lock execution contract first** — single path only: `Element → Action → Flow → Runner → UIEngine`
2. **Enforce resolution ownership** — only Action resolves; deprecate `UIEngine.resolve(Element...)`; remove non-Action resolution paths
3. **Convert `Interactions` to strict adapter** — 100% delegate to `FlowExecutor`; no resolution, waits, or direct engine calls
4. **Treat DSL cleanup as critical** — remove Selenium types from `VoidDSL` public API before claiming engine portability
5. **Make `UIEngineFactory` engine-first** — remove the `WebDriver` parameter; let each engine create its own driver/page internally based on `EngineConfig`
6. **Replace `ExecutionContext`** with an engine-holding context (e.g., `SessionContext` wrapping `UIEngine` + config) — drop the raw `WebDriver` field
7. **Move hooks to pipeline ownership** — hooks are Action decorators, not Interactions/global helpers
8. **Remove direct `LocatorResolvers` usage outside Action path** — prevent future resolution re-fragmentation
9. **Delete `UIContext` global state** — pass descriptor through the call chain explicitly (already done in Action pipeline)
10. **Shrink `Via`** — keep only `descriptor()` methods; drop `locator()`/`webElement()` once DSL migrated
11. **Make bootstrap engine-aware** — validate only resources required by the selected engine; remove unconditional Selenium config gating
12. **Return defensive config snapshots** — avoid exposing mutable global `Properties` from `FrameworkBootstrap`
13. **Normalize lifecycle ownership** — `VOID.shutdown()` should call `UIEngine.shutdown()` first, then engine-specific driver cleanup if still needed

### Convergence Guardrails (non-negotiable)

- ❌ No new `Interactions`-based implementations in new features
- ❌ No new `UIContext` usage
- ❌ No new direct `LocatorResolvers` usage outside Action construction path
- ❌ `UIEngine` APIs must not accept `Element` in new contracts
- ✅ New work must enter through `Action` + `Flow` + `FlowExecutor`
- ✅ All new capabilities must be implemented as `Element → Action → Engine` mapping

### Implementation-Discipline Risk (Execution Regression)

**Regression trigger:** one shortcut can reintroduce parallel execution models.

Common shortcut examples (must be rejected):
- `engine.click(element)`
- `LocatorResolvers.resolve(...)` or `LocatorResolvers.strict().resolve(...)` outside `Action`
- `UIContext.getLast...` / `UIContext.setLast...` in new code

Execution rules (mandatory):
1. **Engine = dumb executor** (`UIEngine` executes `LocatorDescriptor`, does not resolve `Element`)
2. **Action = single source of truth** (intent + resolution ownership)
3. **Interactions = facade only** (compatibility adapter; no execution logic)
4. **DSL = public contract and must stay pure** (no Selenium types in public API)

PR enforcement (block merge if violated):
- Any new direct `UIEngine` call outside Action/Flow execution path
- Any new resolver call outside Action path
- Any new `UIContext` dependency
- Any new Selenium type in DSL/public contracts

### Execution Traps (must fail fast)

**Trap 1 — Temporary shortcut (architectural leak):**
- `engine.click(element)`
- `LocatorResolvers.resolve(...)`
- `UIContext.getLast...()` / `UIContext.setLast...()`

**Policy:** even one occurrence in new code is a regression and must be rejected.

**Trap 2 — Partial phase execution (inconsistent lock-in):**
- Wiring `FlowExecutor` before Resolution Unification is complete
- Keeping `UIEngine.resolve(Element...)` alive after Action ownership is enforced

**Policy:** do not advance to runtime wiring until resolution ownership is unified and enforced.

**Trap 3 — Interactions creep (dual pipeline return):**
- Adding direct `UIEngine` calls in `Interactions` (e.g., waits/clicks/retries)

**Policy:** `Interactions` remains adapter-only; no direct execution logic.

### Execution Discipline Rules (mandatory)

1. **If it touches `UIEngine`, it must come from `Action`.**
2. **If it resolves anything, it must be inside `Action`.**
3. **If it is new behavior, it must execute through `FlowExecutor`.**

---

## 9. Suggested Phased Refactor Plan

### Phase 0 — Lock Architecture Rules
**Goal:** Prevent further drift while refactor is in progress  
**Scope:** Architecture governance, code review gates, contribution rules  
**Changes:**
- Enforce single execution path contract in PR policy
- Block new usages of `Interactions`, `UIContext`, and direct `LocatorResolvers` outside Action
- Require new UI behaviors to be expressed as `Action`

### Phase 1 — Fix Bootstrap & Startup Ownership
**Goal:** Remove Selenium-first boot assumptions and WebDriver dependency from startup  
**Scope:** `FrameworkBootstrap`, `UIEngineFactory`, `VOID.start()`, `ExecutionContext`  
**Changes:**
- Make bootstrap validation engine-aware
- Move native session creation into engine implementations/factory
- Introduce engine-agnostic session context (`SessionContext`)

### Phase 1.5 — Lock Execution Model in Code
**Goal:** Enforce `Element → Action → Flow → Runner → UIEngine` as the only path  
**Scope:** `VOID`, `Interactions`, action entry points  
**Changes:**
- No direct engine calls outside Action execution
- No resolution outside Action path
- Start converting Interactions methods into adapter delegations

### Phase 2 — Resolution Unification (**Critical**)
**Goal:** Establish single source of truth for resolution before runtime wiring  
**Scope:** `Action`, `UIEngine`, `Interactions`, resolver access points  
**Changes:**
- Move all element resolution logic to Action path
- Deprecate/remove `UIEngine.resolve(Element...)`
- Enforce engine execution inputs as `LocatorDescriptor` only
- Remove `LocatorResolvers` usage outside Action construction/execution path

**Exit criteria (must pass before Phase 3):**
- No new `UIEngine` calls outside Action execution path
- No new resolver calls outside Action path
- `UIEngine.resolve(Element...)` deprecation active and migration path defined

### Phase 3 — Wire FlowExecutor into Runtime
**Goal:** Make unified pipeline the default runtime entry  
**Scope:** `VOID`, `VoidDSL`, `FlowExecutor`  
**Changes:**
- Add `VOID.executor()` and route new runtime flows through it
- Update shutdown to call `engine.shutdown()`
- Start DSL migration from imperative calls to Action/Flow composition

**Guardrail:**
- Do not start this phase if Resolution Unification exit criteria are incomplete

### Phase 4 — Remove Selenium Leakage from Public Surface
**Goal:** Break framework-level Selenium binding  
**Scope:** `VoidDSL`, `Via`, resolver public APIs  
**Changes:**
- Remove `By`/`WebElement` from DSL public contracts
- Retire Selenium-returning helper methods from shared surface
- Keep Selenium specifics isolated to selenium engine package

### Phase 5 — Convert Interactions to Strict Adapter
**Goal:** Eliminate dual pipelines permanently  
**Scope:** `Interactions`  
**Changes:**
- 100% delegation to `FlowExecutor`
- Interactions must not call `LocatorResolvers`
- Interactions must not call `UIEngine` directly
- Interactions must not implement waits/retries/execution logic
- Compatibility shape only: `Flow.of(...).run()`
- Keep Interactions only as compatibility façade until removal window

**Guardrail:**
- Any new direct `UIEngine` call in `Interactions` fails review

### Phase 6 — Remove UIContext
**Goal:** Remove global mutable action state  
**Scope:** `UIContext`, dependent utilities (`DOMUtils`, legacy hooks/flows)  
**Changes:**
- Delete `UIContext` and migrate remaining consumers to explicit descriptor flow
- Remove fallback behaviors that rely on thread-local action target

### Phase 7 — Finalize Hook Model
**Goal:** Make hooks pipeline-native and deterministic  
**Scope:** `ActionHandler`, `Before`, `After`, hook packaging  
**Changes:**
- Relocate hooks to neutral package (not `core.interactions.*`)
- Enforce Action-decorator-only hook execution model
- Remove legacy hook adapters

### Phase 8 — Add Playwright Engine
**Goal:** Deliver true multi-engine runtime  
**Scope:** New `core.engine.playwright` package + factory wiring  
**Changes:**
- Implement `PlaywrightEngine implements UIEngine`
- Run full pipeline without Selenium classpath/runtime dependency
- Add engine parity tests against Action/Flow contract
