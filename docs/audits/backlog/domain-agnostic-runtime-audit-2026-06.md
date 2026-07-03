# VOID Architecture Audit — Domain-Agnostic Interaction Runtime?

**Date:** 2026-06-16
**Scope:** Full structural analysis of execution model, domain boundaries, coupling, and evolutionary fitness
**Question:** Is VOID genuinely evolving toward a Domain-Agnostic Interaction Runtime, or is it a UI automation framework with generic-looking abstractions?
**Method:** Trace the actual execution path through source code. Evaluate coupling at each layer. Apply the domain-substitution test: can a new domain (robotics, agents) be added without redesigning the runtime?

---

## Execution Path (Actual)

```
Element (domain descriptor)
 ↓
Capability.click() / type() / etc.  (domain: emits Action intent)
 ↓
Action.perform(UIEngine engine)  ← first runtime artifact; PINNED to UIEngine
 ↓
FlowExecutor(UIEngine engine)    ← pinned to UIEngine
 ↓
UIEngine.click(LocatorDescriptor)
 ↓
SeleniumEngine → By → WebDriver API
```

The runtime boundary is `Action`. Everything before `Action` is domain. Everything from `Action` through `FlowExecutor` to `UIEngine` is the runtime.

---

## 1. Runtime Boundary

**Where does the runtime begin?**

At `Action`.

`Element` and capabilities (`Clickable`, `Typeable`, etc.) are domain descriptors. They produce no side effects. `Clickable.click()` returns an `Action` object — it does not interact with any driver or engine. Resolution and execution are deferred until `Action.perform(engine)` is called.

The true execution boundary is:

```
Action → FlowExecutor → UIEngine
```

**Supporting evidence:**

`Clickable.java:50–53`:
```java
default Action click() {
    return ElementActions.of(this, ElementRole.TRIGGER,
            (engine, d) -> engine.click(d));
}
```

`FlowExecutor.java:39–43`:
```java
public void run(Flow flow) {
    for (Action action : flow.getActions()) {
        action.perform(engine);
    }
}
```

The domain layer produces Actions. The runtime executes them.

---

## 2. Domain Boundary

**What belongs to the domain:**

| Artifact | Belongs To | Reason |
|---|---|---|
| `Element` interface | Domain | Pure descriptor; no execution |
| `Clickable`, `Typeable`, `Selectable`, etc. | Domain | Vocabulary of the UI domain; emit Actions only |
| `ElementRole` | Domain | Semantic tagging of locator roles |
| Locator files (JSON, properties) | Domain | External data describing elements |
| `LocatorDescriptor`, `LocatorStrategy` | Disputed (see §3) | Conceptually domain-agnostic, but strategies are HTML-specific |
| `SeleniumEngine` | Domain implementation | Selenium realization of the UIEngine contract |

**What belongs to the runtime:**

| Artifact | Belongs To | Reason |
|---|---|---|
| `Action` | Runtime | Execution intent; the primitive of the runtime |
| `Flow` | Runtime | Compositional sequence of Actions |
| `FlowExecutor` | Runtime | Dispatch engine |
| `UIEngine` (interface) | Disputed (see §5) | Intended as runtime contract; actually UI-specific |
| `VOID` | Runtime | Session lifecycle and entry point |
| Hook system (`ActionHandler`, `Before`, `After`) | Runtime | Cross-cutting execution concerns |

**Disputed territory:**

`LocatorDescriptor`, `LocatorStrategy`, and `LocatorResolver` occupy the interface between domain and runtime. `LocatorDescriptor` itself imports no Selenium types and represents a resolved locator string — it is domain-agnostic in form. But `LocatorStrategy.XPATH / CSS / ID / NAME` are web/HTML strategies. A robot domain would not use these. This is addressed in §6.

---

## 3. Capability Analysis

**Are the capabilities evidence of UI coupling?**

The capability interfaces (`Clickable`, `Typeable`, `Selectable`, `Hoverable`) are **UI domain vocabulary**, not runtime coupling. They belong to the domain layer. The runtime does not depend on them.

`Clickable.click()` returns `Action`. The runtime sees only `Action`. It does not see `Clickable`.

**Domain substitution test — capabilities:**

Replacing `Clickable` with `Movable`, and `Typeable` with `Actuatable`, would require:

1. New capability interfaces: `Movable`, `Actuatable` — implement `Element`, emit `Action`
2. New element enums implementing those interfaces
3. No runtime changes required

The runtime never inspects capability types. It dispatches `Action` objects. Capability replacement is a domain-only change.

**Verdict on capabilities:** Not evidence of runtime coupling. They are correctly domain-local.

**However:** The capability names (`Clickable`, `Typeable`, `Hoverable`) use browser terminology because the current domain is a browser domain. This is correct. Domain vocabulary should reflect the domain.

---

## 4. Action Analysis

**Is `Action` the true architectural primitive?**

Architecturally yes. Practically, no — because of its signature.

`Action.java:41–48`:
```java
@FunctionalInterface
public interface Action {
    void perform(UIEngine engine);
}
```

`Action` is formally the primitive. But its `perform` method takes `UIEngine` specifically — not an abstract `Engine` type. This is the central architectural defect.

**Consequence of this pin:**

A `MoveAction` for a robotics domain would need to call `engine.moveTo(x, y, z)`. But `UIEngine` has no `moveTo` method. The method cannot be called. The `Action` contract cannot express a non-UI operation without:
- Adding `moveTo` to `UIEngine` (which corrupts the UI contract with robot vocabulary)
- Or creating a parallel `RobotAction.perform(RobotEngine)` with a different signature (which creates two incompatible Action types and renders the runtime non-unified)

Neither option is domain-agnostic. The execution contract is effectively:

```
Action → [must call UIEngine methods] → browser behavior
```

**For the architecture to be domain-agnostic, the signature must become:**

```java
void perform(Engine engine);
```

where `Engine` is an abstract supertype:

```java
UIEngine extends Engine
RobotEngine extends Engine
AgentEngine extends Engine
```

This single change, propagated through `FlowExecutor` and `VOID`, would make the runtime genuinely domain-agnostic.

**Current state:** `Action` looks like a domain-agnostic primitive but is pinned to the current domain's execution contract.

---

## 5. Engine Analysis

**Is `UIEngine` a runtime primitive, a domain implementation, or a leaky abstraction?**

`UIEngine` is a domain implementation masquerading as a runtime contract.

Evidence from the interface:

```java
void navigateTo(String url);         // "url" is a web concept
String getCurrentUrl();              // web concept
void waitForOverlay(Duration);       // hardcoded CDK/Material concern
void executeScript(String, Object...); // JavaScript — browser-only
void selectByVisibleText(...);       // HTML <select> element concept
void selectByValue(...);             // HTML <select> element concept
void getTitle();                     // browser tab concept
```

`waitForOverlay` is the most revealing method. Its implementation in `SeleniumEngine.java:361`:
```java
By overlayPane = By.cssSelector("div.cdk-overlay-pane");
```

An Angular Material-specific CDK selector is embedded in the engine contract. This is not a UI-level coupling — it is an application-level coupling baked into what is supposed to be a framework-level contract.

**Can the architecture support `RobotEngine`, `AgentEngine`, `WorkflowEngine`?**

No. Not without redesigning `UIEngine`.

A `RobotEngine` implementing `UIEngine` would need to provide:
- `navigateTo(String url)` — meaningless for a robot
- `waitForOverlay(Duration)` — meaningless for a robot
- `executeScript(String, Object...)` — meaningless for a robot
- `selectByVisibleText(LocatorDescriptor, String)` — meaningless for a robot
- `getCurrentUrl()` — meaningless for a robot

The interface cannot be implemented by a non-browser engine without hollow stub implementations that violate the Liskov Substitution Principle.

**Root cause:** There is no abstract `Engine` supertype. `UIEngine` is both the abstract contract and the UI domain contract. These are two different things that have been collapsed into one interface.

**What a multi-domain architecture requires:**

```java
interface Engine {
    void initialize(EngineConfig config);
    void shutdown();
}

interface UIEngine extends Engine {
    void navigateTo(String url);
    void click(LocatorDescriptor locator);
    // ... all current UIEngine methods
}

interface RobotEngine extends Engine {
    void moveTo(double x, double y, double z);
    void actuate(String joint, double angle);
}
```

`FlowExecutor` would then operate on `Engine`, and `Action.perform(Engine)` would work with any domain-specific engine.

---

## 6. Hidden Coupling Audit

### 6.1 Necessary UI Coupling

These are correctly isolated. They belong to the Selenium implementation layer and do not leak into the runtime contracts.

| Location | Coupling | Assessment |
|---|---|---|
| `SeleniumEngine.java` | `WebDriver`, `By`, `WebElement`, `ExpectedConditions` | Correct — implementation detail |
| `DriverFactory`, `DriverManager`, `DriverContext` | `WebDriver` | Correct — Selenium lifecycle |
| `EngineConfig` | None (pure Properties + Duration) | Correctly agnostic |

### 6.2 Accidental UI Coupling

These represent coupling that has leaked past where it belongs.

---

**Finding 1 — `Action.perform(UIEngine)`: the execution primitive is domain-pinned**

`Action.java:48`

Severity: **Critical**

The runtime's core primitive takes a UI-specific type. This single pin prevents the runtime from executing any non-UI domain. All downstream coupling is a consequence of this pin.

No fix to `UIEngine`, `FlowExecutor`, or `VOID` matters until this pin is generalized.

---

**Finding 2 — `ByParser` in the resolver layer**

`core/resolvers/locator/parser/ByParser.java`

```java
import org.openqa.selenium.By;
// ...
public By parse(String raw) { ... }
```

`ByParser` lives inside `core.resolvers.locator` — the resolver layer. The resolver's job is to translate element descriptors into `LocatorDescriptor` objects. Producing a Selenium `By` object is the engine's job, not the resolver's job.

The resolver layer now has two output types:
- `LocatorResolver.resolve()` → returns `By` (old path, still used)
- `LocatorResolver.resolveDescriptor()` → returns `LocatorDescriptor` (new path)

The `By`-returning path is not an isolated legacy concern. `WaitUtils.waitForElementToDisappear(ReadOnly element)` at `WaitUtils.java:131` actively calls:
```java
By locator = LocatorResolvers.strict().resolve(element);
```

This means current production code in `core.utils.web` depends on the `By`-returning path. The migration is incomplete.

Severity: **High** — resolver layer is coupled to Selenium despite having a working `LocatorDescriptor` abstraction.

---

**Finding 3 — `UIEngineFactory.create(Properties, WebDriver)`: factory signature leaks Selenium**

`UIEngineFactory.java:41`:
```java
public static UIEngine create(Properties config, WebDriver driver) {
```

The factory takes a `WebDriver` as a required parameter. This means any invocation of the factory is tied to Selenium. There is no path through this factory to create a non-Selenium engine. A future `PlaywrightEngine` would not accept a `WebDriver`.

The comment in the code acknowledges this: `@param driver WebDriver instance (used by SeleniumEngine; ignored by other engines)`. If it is ignored by other engines, it should not be in the signature.

Severity: **High** — entry point to engine creation is Selenium-gated.

---

**Finding 4 — `VOID.start()` hardwires Selenium driver creation at session startup**

`VOID.java:136–144`:
```java
public static VOID start(DriverFactory.Profile profile) {
    FrameworkBootstrap.init();
    WebDriver driver = DriverManager.createDriver(profile);
    ExecutionContext ctx = new ExecutionContext(
            FrameworkBootstrap.getUtilsConfig(), driver);
    UIEngine engine = UIEngineFactory.create(FrameworkBootstrap.getUtilsConfig(), driver);
    ...
}
```

The session facade's primary factory method instantiates a `WebDriver` unconditionally. There is no code path through `VOID.start()` that does not create a Selenium WebDriver. A Playwright session, robot session, or agent session cannot be started without modifying this method.

Severity: **High** — the user-facing entry point is Selenium-only.

---

**Finding 5 — `core.utils.web` package in `core`**

`DOMUtils.java`, `WaitUtils.java`, `TableHandler.java`, `WaitUtils.java`, `Upload.java`, `KeyValuePairHandler.java` live in `core.utils.web`. The package name `web` signals their domain. The class name `DOMUtils` (Document Object Model) is a browser-specific term. These files import `By`, `WebDriver`, `WebElement`.

These utilities are in the `core` package, which should be the framework runtime. They are domain utilities — they belong in a `selenium/` or `ui/` implementation package, not in the core.

`DOMUtils.java:4–8`:
```java
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import core.driver.DriverContext;
```

`DOMUtils` also reaches into `DriverContext` directly, bypassing the engine abstraction entirely.

Severity: **Medium** — the coupling is isolated to a utility subpackage but lives in the wrong layer.

---

**Finding 6 — `WaitUtils` with hardcoded Angular CDK selectors**

`WaitUtils.java:44–45`:
```java
public static final By ANGULAR_LOADER = By.tagName("app-loader");
public static final By SPIN_SPINNER_LOADER = By.xpath("//span[contains(@class, 'spin spinner')]");
```

Framework-level wait utilities contain application-specific selectors (`app-loader`, `spin spinner`). This is coupling below the framework layer — application vocabulary embedded in a framework class. The `waitForOverlay` concern in `UIEngine` is the same failure at a higher level.

Severity: **Low** (in context of the architectural question) — application concern leaking into framework, but isolated to one class.

---

**Finding 7 — `LocatorStrategy` is a closed enum of HTML strategies**

`LocatorStrategy.java`:
```java
public enum LocatorStrategy {
    XPATH, CSS, ID, NAME;
}
```

These four strategies are specific to web/HTML element location. A robot domain would use coordinate-based location. An agent domain might use semantic identifiers or API paths. The enum is not extensible — adding a new strategy requires modifying the enum, which modifies the engine contract.

For a domain-agnostic runtime, `LocatorStrategy` would need to be an interface or an open set, not a closed enum.

Severity: **Medium** — currently correct for the UI domain, but structurally blocks new domains.

---

**Finding 8 — `UIEngine.waitForOverlay(Duration)` encodes Angular Material concern in the engine contract**

`UIEngine.java:289`:
```java
void waitForOverlay(Duration timeout);
```

`SeleniumEngine.java:361`:
```java
By overlayPane = By.cssSelector("div.cdk-overlay-pane");
```

The engine contract includes a method specific to Angular Material's CDK overlay pane. This is not a UI-level abstraction — it is an application-level detail embedded in the engine interface. Every engine implementation (including a future Playwright engine) must implement a method that searches for `cdk-overlay-pane`.

This method does not belong in `UIEngine`. It belongs in a higher-level application utility or as a configurable wait hook.

Severity: **Low** (in context of the architectural question) — does not prevent domain-agnosticism, but indicates the engine contract is accumulating application-level concerns.

---

### 6.3 Coupling Summary

| Layer | Coupling Type | Severity | Notes |
|---|---|---|---|
| `Action.perform(UIEngine)` | Execution contract pinned to UI domain | Critical | Root cause of all runtime coupling |
| `UIEngineFactory(WebDriver)` | Factory signature leaks Selenium | High | No path to non-Selenium engine |
| `VOID.start()` | Session startup hardwired to WebDriver | High | User-facing entry point Selenium-only |
| `ByParser` in resolver | Resolver produces `By` | High | Migration to `LocatorDescriptor` incomplete |
| `core.utils.web.*` in core | DOM utilities in runtime layer | Medium | Wrong layer, not wrong code |
| `LocatorStrategy` closed enum | Not extensible for new domains | Medium | Blocks new domain strategies |
| `UIEngine.waitForOverlay` | App-level concern in engine contract | Low | Accumulating tech debt |
| Capability interfaces | UI vocabulary | None | Correctly domain-local |
| `LocatorDescriptor` | No Selenium imports | None | Correctly domain-agnostic |
| `SeleniumEngine` internals | Full Selenium coupling | Correct | Correctly isolated |

---

## 7. Future Evolution Test

**Assumed future:** Pages, flows, and tests become JSON assets. Multiple engine families exist. UI automation is one domain among many.

### What survives unchanged

| Artifact | Survives | Reason |
|---|---|---|
| `Element` interface | ✅ | Pure descriptor; no coupling |
| `Clickable`, `Typeable`, etc. | ✅ | Domain vocabulary; engine-agnostic |
| `LocatorDescriptor` | ✅ | No Selenium imports; pure record |
| `Flow` | ✅ | Pure sequence; no coupling |
| Hook system (`ActionHandler`, `Before`, `After`) | ✅ | Engine-agnostic; receives descriptor |
| `LocatorSourceRegistry` / `LocatorSource` | ✅ | Pluggable source chain; no coupling |
| JSON/properties locator files | ✅ | Data; no coupling |
| `EngineConfig` | ✅ | Pure configuration; no coupling |

### What requires redesign

| Artifact | Problem | Required Change |
|---|---|---|
| `Action.perform(UIEngine)` | Pinned to UIEngine | Introduce abstract `Engine`; change signature to `perform(Engine)` |
| `FlowExecutor(UIEngine)` | Depends on UIEngine | Change constructor to `FlowExecutor(Engine)` |
| `UIEngine` (interface) | Is both runtime contract and UI domain contract | Extract abstract `Engine`; `UIEngine extends Engine` |
| `UIEngineFactory.create(Properties, WebDriver)` | Requires WebDriver | Remove `WebDriver` param; each engine creates its own driver |
| `VOID.start()` | Hardwires Selenium driver creation | Make engine-configurable; inject engine, not driver |
| `ByParser` in resolver layer | Resolver produces `By` | Internalize `ByParser` to `SeleniumEngine`; resolver always returns `LocatorDescriptor` |
| `LocatorResolver.resolve()` returning `By` | Dual output types | Deprecate and remove; `resolveDescriptor` is the only path |
| `LocatorStrategy` enum | Closed set of HTML strategies | Convert to interface or extensible enum |
| `core.utils.web.*` | In core layer, uses WebDriver | Move to `engine/selenium/utils/` or similar implementation package |

### Prognosis

The locator resolution pipeline (`LocatorDescriptor`, `LocatorSourceRegistry`, `LocatorTemplate`) is already in the right shape. Pages and flows as JSON assets are structurally supported today.

The execution model (`Action`, `Flow`, `FlowExecutor`) is one pin change from being domain-agnostic. The pin is `UIEngine` in `Action.perform(UIEngine)`. One abstraction — an `Engine` supertype — removes that pin and unlocks multi-domain support without redesigning the runtime logic.

The session startup (`VOID.start()`) needs the most invasive rework. It currently constructs a `WebDriver` unconditionally, which is the deepest Selenium coupling in the user-visible API.

---

## 8. Final Verdict

**B — UI Automation Runtime**

This is not a UI automation *framework* (A), because the execution model is genuine: deferred action emission, compositional flows, lifecycle management, a hook pipeline, and an engine abstraction boundary. These are runtime behaviors, not framework glue.

This is not an Interaction Runtime Currently Configured For UI Automation (C), because being "currently configured for UI" implies the runtime is domain-agnostic at its core and happens to be pointing at a UI engine. That is not the case here.

**The test:** Can a `RobotEngine` be introduced primarily by adding new Elements, Capabilities, Actions, and an Engine implementation?

**Result:** No.

The execution contract is `Action.perform(UIEngine engine)`. A `RobotEngine` cannot be passed to this method unless it implements `UIEngine`. Implementing `UIEngine` requires implementing `navigateTo(url)`, `executeScript()`, `waitForOverlay()`, `selectByVisibleText()`, `getCurrentUrl()` — none of which have meaningful semantics for a robot. This is not a configuration difference. It is a contract violation.

The runtime cannot host a second domain without changing `Action.perform(UIEngine)`. That is a runtime redesign, not a domain addition. Therefore the runtime is not domain-agnostic.

**What the architecture has correctly achieved:**

1. The domain layer (capabilities, elements) is clean. Replacing capabilities with robot or agent vocabulary requires zero runtime changes.
2. `LocatorDescriptor` is correctly engine-agnostic. The resolver's descriptor path is correct.
3. `SeleniumEngine` is correctly isolated — Selenium coupling is contained within one class.
4. The hook system is correctly domain-agnostic.
5. `Flow` and `FlowExecutor` are structurally correct — one type signature change makes them truly domain-agnostic.

**What separates B from C:**

| Requirement for (C) | Current state |
|---|---|
| Abstract `Engine` type exists | ❌ — does not exist |
| `Action.perform(Engine)` | ❌ — `Action.perform(UIEngine)` |
| `FlowExecutor(Engine)` | ❌ — `FlowExecutor(UIEngine)` |
| Engine creation does not require domain driver | ❌ — factory requires `WebDriver` |
| Session start does not hardwire driver creation | ❌ — `VOID.start()` unconditionally creates `WebDriver` |
| Resolver does not produce domain-specific types | ❌ — `resolve()` still returns `By` |

**Minimum changes required to reach (C):**

1. Introduce `Engine` interface (or rename `UIEngine` → `Engine` and move domain methods to `UIEngine extends Engine`)
2. Change `Action` to `perform(Engine engine)`
3. Change `FlowExecutor` to `FlowExecutor(Engine engine)`
4. Remove `WebDriver` from `UIEngineFactory.create()` signature
5. Remove `WebDriver` driver creation from `VOID.start()`; inject engine directly
6. Remove `By`-returning methods from `LocatorResolver`; internalize `ByParser` to `SeleniumEngine`

None of these changes require redesigning the locator resolution system, the hook system, the flow composition model, or the capability model. The domain layer survives intact. The structural skeleton of the runtime survives intact. Only the type pin changes.

The framework is one abstraction away from (C). That abstraction is an `Engine` supertype.

---

*Audit performed against commit 8c081c6. All findings reference actual source code.*