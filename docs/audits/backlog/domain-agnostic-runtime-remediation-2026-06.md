# VOID Architecture Remediation Plan

**Date:** 2026-06-16
**Input:** Three rounds of architectural audit (domain-agnostic-runtime-audit-2026-06.md, -revised.md, -stress-test.md)
**Role:** Lead architect responsible for evolving the system, not redesigning it

---

## Part 1 — Re-ranked Findings

All findings from all three audit rounds, classified by remediation priority.

### Critical — Must fix before further evolution

| Finding | File | Why Critical |
|---|---|---|
| `Action.perform(UIEngine)` type pin | `core/actions/Action.java` | Every future domain capability and every non-Selenium engine is blocked here. All downstream generalization work requires this to move first. |
| `ActionCapability` closed enum | `core/actions/ActionCapability.java` | Newly introduced on this branch. Not yet released. Changing it after release costs 3× more. `Profiles.SAFE`/`RELIABLE` switch exhaustively — adding a second domain today produces silently wrong behavior, not a compile error. Fix before it hardens. |
| `ActionHandler.execute(UIEngine, LocatorDescriptor)` | `core/interactions/hooks/ActionHandler.java` | Hooks are called from `HookedAction` via `HookChainAction`. Every `Before.*` and `After.*` constant is typed to `UIEngine`. When `Action.perform(Engine)` changes, the hook dispatch layer must handle the type transition. This is the largest hidden surface area in the migration. |

### Important — Fix within next few releases

| Finding | File | Why Important |
|---|---|---|
| No abstract `Engine` interface | n/a | Required for `UIEngine extends Engine`. Without it, Critical items above cannot be resolved. One new file, zero risk. |
| `VOID.start()` unconditional WebDriver creation | `core/runtime/VOID.java` | `SessionContext` already exists and is unwired. Once Engine is generalized, this is the entry point that prevents non-browser sessions from starting at all. |
| `UIEngineFactory.create(Properties, WebDriver)` signature | `core/engine/UIEngineFactory.java` | `WebDriver` in the factory parameter is acknowledged as wrong in its own Javadoc. After Engine generalization, engine factories should receive engine-specific config, not Selenium types. |
| `LocatorStrategy` closed enum | `core/engine/LocatorStrategy.java` | Same extensibility problem as `ActionCapability` but lower severity — adding a strategy for a new domain requires a framework fork. Fix when touching nearby code. |
| `ByParser` in resolver layer | `core/resolvers/locator/parser/ByParser.java` | Wrong layer for Selenium imports. The `resolveDescriptor()` path should be the primary path. `ByParser` should be internal to `SeleniumEngine`. |
| `resolve()` returning `By` in `LocatorResolver` | `core/resolvers/locator/api/LocatorResolver.java` | Both paths coexist. The `By`-returning path actively fights engine agnosticism. It should be formally deprecated and the deletion scheduled. |
| `HookChainAction.resolve(UIEngine)` / `HookedAction.perform(UIEngine)` | `core/actions/HookChainAction.java`, `HookedAction.java` | These carry the UIEngine pin into the hook orchestration path. `HookedAction` is already `@Deprecated(forRemoval)` which is correct. `HookChainAction` needs the same type-pin updates as `Action`. |

### Opportunistic — Fix when touching nearby code

| Finding | Why Opportunistic |
|---|---|
| `UIEngine.waitForOverlay(Duration)` CDK concept | Application-level concept in engine contract. Doesn't block generalization — just design debt. Suppress it with `@Deprecated` pointing toward a profile-level hook instead. |
| `Before.WAIT_FOR_ANGULAR_LOADER` in profile library | Application-level constant in framework. Should live in the test project's custom hooks. Move when updating `Before`/`After` constants anyway. |
| `WaitUtils.ANGULAR_LOADER = By.tagName("app-loader")` | `core/utils/web/WaitUtils.java` is a test utility that happens to carry an application-specific selector. Remove when next touching WaitUtils. |
| `Interactions` legacy class | Already `@Deprecated(since="2.1", forRemoval)`. Actively maintains UIEngine coupling in a public method. Leave it for scheduled 3.0 removal, but do not add any new methods. |

### Ignore — Not worth fixing currently

| Finding | Why Ignore |
|---|---|
| `Clickable`, `Typeable`, `Selectable` interface naming | Correct domain vocabulary. These are UI-domain elements; the names are semantically accurate. |
| `SeleniumEngine` using `By`, `WebElement`, `WebDriver` internally | Correctly isolated to the implementation class. Not a framework-level concern. |
| Multi-domain flow coexistence (actions from two domains in one Flow) | Premature. Single-domain-per-session is the right initial model. Design this when the second domain exists. |
| `LocatorDescriptor.parent` field name | Low value for high cost. All call sites would change. The conceptual bias is real but has no practical effect until a second domain developer encounters it. Fix later — see Part 5. |

---

## Part 2 — Two-Week Sprint

### Week 1: Engine Boundary and ActionCapability

**Day 1–2: Introduce `Engine` and update core execution types**

Create `core/engine/Engine.java`. Add `extends Engine` to `UIEngine`. Update `Action`, `FlowExecutor`, and `HookChainAction` type pins (full detail in Part 3).

*Definition of done:* All tests pass. UIEngine still works. Compile succeeds with zero changes to any element or capability code.

**Day 3: Replace `ActionCapability` enum with extensible interface**

Replace `ActionCapability.java` with an interface-with-constants design. Update `Profiles.SAFE`/`RELIABLE`/`FAST` switch statements to map-based dispatch. Full detail in Part 4.

*Definition of done:* All profile behavior is identical for existing UI capabilities. New capabilities can be added by external code without modifying framework classes.

**Day 4–5: Wire `SessionContext` into `VOID.start()` and update factory**

`SessionContext` already exists. It holds `UIEngine`, not `WebDriver`. Replace `ExecutionContext` in `VOID.java`'s constructor and `start()` factory. Refactor `UIEngineFactory` to not require `WebDriver` in its public signature — let each engine implementation wire its own connection internally.

*Definition of done:* `VOID.start()` no longer holds `WebDriver` directly. `SessionContext` is the live context object. `ExecutionContext` becomes `@Deprecated(forRemoval)`. All existing browser sessions still work.

---

### Week 2: Locator Path Cleanup and Hook Layer

**Day 6–7: Formalize the `By`-based resolution as deprecated**

Mark all `LocatorResolver.resolve()` methods that return `By` with `@Deprecated(since="2.0", forRemoval=true)`. Add Javadoc pointing to `resolveDescriptor()`. Do not remove yet — do not break callers. Move `ByParser` usage to be internal to `SeleniumEngine` only (remove it from the resolver builder as a public option). This doesn't delete code — it draws a line.

*Definition of done:* `LocatorResolver.resolveDescriptor()` is the primary path. IDE warnings appear on all `resolve()->By` callers. A removal target release is named in Javadoc.

**Day 8–9: Make `LocatorStrategy` extensible**

Apply the same interface-with-constants pattern as `ActionCapability` to `LocatorStrategy`. The strategy inference logic in `LocatorResolver.inferStrategy()` and `LocatorStrategy.infer()` stays in place — it is already a static utility pattern that works with any strategy type.

*Definition of done:* New strategies can be added by external code. `LocatorResolver.inferStrategy()` falls back to `CSS` for unrecognized prefixes (which it already does).

**Day 10: Audit and checkpoint**

Review all `switch(action.capability())` callers in the codebase — confirm none remain as exhaustive enum switches. Confirm `HookedAction` has correct `@Deprecated(forRemoval)` and no new callers have been added. Add a migration note to `CHANGELOG.md` covering the Engine generalization and `ActionCapability` changes.

---

## Part 3 — Engine Generalization Plan

### 1. Exact Files Changed

| File | Change Type | Nature of Change |
|---|---|---|
| `core/engine/Engine.java` | New | Defines abstract engine contract |
| `core/engine/UIEngine.java` | Edit | Add `extends Engine` |
| `core/actions/Action.java` | Edit | `perform(UIEngine)` → `perform(Engine)`; `resolve(UIEngine)` → `resolve(Engine)`; add `Action.ui()` static helper |
| `core/actions/HookChainAction.java` | Edit | `perform(UIEngine)` → `perform(Engine)`; `resolve(UIEngine)` → `resolve(Engine)`; internal cast to UIEngine before hook dispatch |
| `core/actions/HookedAction.java` | Edit | `perform(UIEngine)` → `perform(Engine)`; `executeHooks(UIEngine)` → accepts Engine with cast — already deprecated, minimal effort |
| `core/actions/ElementActions.java` | Edit | Inner `ElementBoundAction.perform(UIEngine)` → `perform(Engine)`; `resolve(UIEngine)` → `resolve(Engine)`; internal UIEngine cast preserved |
| `core/executor/FlowExecutor.java` | Edit | `private final UIEngine engine` → `private final Engine engine`; constructor param |
| `core/runtime/VOID.java` | Edit | Constructor and `start()` factory; wire `SessionContext`; `UIEngine` stays for navigation methods via session |

### 2. Exact Signature Changes

**`core/engine/Engine.java` (new)**
```java
package core.engine;

public interface Engine {
    String getEngineName();
    void shutdown();
}
```

Only two methods. `UIEngine` already has both. No behavioral change.

**`core/engine/UIEngine.java`**
```java
// Before:
public interface UIEngine { ... }

// After:
public interface UIEngine extends Engine { ... }
```

Single-line change. All existing `UIEngine` references remain valid — `UIEngine` is still a fully valid type everywhere.

**`core/actions/Action.java`**
```java
// Before:
@FunctionalInterface
public interface Action {
    void perform(UIEngine engine);
    default LocatorDescriptor resolve(UIEngine engine) { return null; }
    ...
}

// After:
@FunctionalInterface
public interface Action {
    void perform(Engine engine);
    default LocatorDescriptor resolve(Engine engine) { return null; }

    // Migration bridge for UI-domain lambdas that reference UIEngine directly.
    // Wraps a UIEngine-typed lambda so callers don't need inline casts.
    static Action ui(java.util.function.Consumer<UIEngine> uiOp) {
        return engine -> uiOp.accept((UIEngine) engine);
    }
    ...
}
```

The `ui()` static helper is important for migrating any test or framework code that wrote:
```java
Action a = (UIEngine engine) -> engine.click(descriptor);
```
Those become:
```java
Action a = Action.ui(engine -> engine.click(descriptor));
```

**`core/actions/HookChainAction.java`**
```java
// Before:
public void perform(UIEngine engine) {
    LocatorDescriptor descriptor = delegate.resolve(engine);
    new HookedAction(delegate, descriptor, before, after).perform(engine);
}

// After:
public void perform(Engine engine) {
    LocatorDescriptor descriptor = delegate.resolve(engine);
    // Hooks are UIEngine-typed (they are UI-domain hooks).
    // Non-UI domains use profiles that return no ActionHandler hooks,
    // so this cast is only reached for UI-domain actions.
    UIEngine uiEngine = (UIEngine) engine;
    new HookedAction(delegate, descriptor, before, after).perform(uiEngine);
}
```

**`core/actions/ElementActions.java` (inner `ElementBoundAction`)**
```java
// Before:
public void perform(UIEngine engine) {
    op.accept(engine, resolve(engine));
}

public LocatorDescriptor resolve(UIEngine engine) {
    return engine.resolve(element, role);
}

// After:
public void perform(Engine engine) {
    UIEngine uiEngine = (UIEngine) engine;
    op.accept(uiEngine, resolve(engine));
}

public LocatorDescriptor resolve(Engine engine) {
    return ((UIEngine) engine).resolve(element, role);
}
```

The `BiConsumer<UIEngine, LocatorDescriptor> op` field stays UIEngine-typed. `ElementBoundAction` is a UI-domain class. It will always receive a UIEngine from a UI-domain session. The cast is safe within the UI domain.

**`core/executor/FlowExecutor.java`**
```java
// Before:
private final UIEngine engine;
public FlowExecutor(UIEngine engine) { this.engine = engine; }

// After:
private final Engine engine;
public FlowExecutor(Engine engine) { this.engine = engine; }
// run() stays identical — calls action.perform(engine) which now accepts Engine
```

### 3. Hidden Dependencies

**`ActionHandler.execute(UIEngine, LocatorDescriptor)`** — the most underestimated surface area. Every hook constant in `Before` and `After` (`WAIT_FOR_ELEMENT_VISIBLE`, `CLEAR_FIELD`, `WAIT_FOR_ANGULAR_LOADER`, etc.) is typed to `UIEngine`. Changing `ActionHandler` to accept `Engine` would cascade to all hook lambdas.

**Decision:** Do not change `ActionHandler` in this sprint. Instead, the `HookChainAction` cast (above) bridges the boundary. `ActionHandler`-typed hooks are UI-domain hooks and should remain UIEngine-typed. They are correctly placed in the UI domain. A future `DomainActionHandler<E extends Engine>` abstraction can generalize this when a second domain is built.

**`engine.resolve(Element, ElementRole)`** — this call in `ElementBoundAction.resolve()` is how descriptors are obtained at execution time. The `resolve(Element, ElementRole)` method lives on `UIEngine`, not on `Engine`. The internal cast handles this. A future step could move resolution to a separate `LocatorResolver` injection point, removing the engine's responsibility for locator resolution entirely — but that is a separate refactoring not needed now.

**`VOID.navigateTo()`, `getCurrentUrl()`, `getTitle()`, `refresh()`** — these are browser-specific methods called on `engine` directly in `VOID.java`. After Engine generalization, these should either:
- Stay on `UIEngine` (called via `getEngine()` cast), or
- Move to a `BrowserSession extends VOID` subclass for browser-specific sessions

For now, `VOID.java` holds `UIEngine engine` (not `Engine engine`) because it needs these browser methods. This is fine — `VOID` is currently the browser session facade. A future `RobotSession extends VOIDSession` would be the robot domain entry point. This architectural point is outside the scope of the type-pin migration.

### 4. What Was Underestimated in Previous Audits

**The `ElementActions.of(BiConsumer<UIEngine, LocatorDescriptor>)` factory parameter.** All three audits mentioned `Action.perform(UIEngine)` as the pin. None explicitly named `ElementActions.of()` as carrying the same pin in its parameter type. Since `ElementBoundAction` is the concrete Action class used for 100% of element-based actions, this parameter is equally important. It stays UIEngine-typed for now (correctly) but it was not named as a migration item.

**`HookChainAction.resolve(UIEngine)` as a second pin on Action.** The `resolve()` default method on Action has the same UIEngine pin as `perform()`. Audits focused on `perform()`; `resolve()` was not explicitly called out. Both must change together.

### 5. Migration Order That Minimizes Breakage

```
Step 1 (zero risk):
  Create core/engine/Engine.java

Step 2 (zero risk — additive):
  UIEngine extends Engine

Step 3 (zero risk — subtype compatibility):
  FlowExecutor(Engine engine)
  VOID(Engine engine) constructor — note: keep UIEngine field for navigation methods

Step 4 (zero risk — internal change only):
  ElementBoundAction.perform(Engine) — private inner class
  ElementBoundAction.resolve(Engine) — private inner class

Step 5 (zero risk — internal change):
  HookChainAction.perform(Engine) — package-private class
  HookChainAction.resolve(Engine) — package-private class

Step 6 (breaking — do last):
  Action.perform(Engine) — this is the public interface change
  Action.resolve(Engine) — same

Step 7 (breaking — do with Step 6):
  Add Action.ui(Consumer<UIEngine>) static helper before releasing

Step 8 (breaking):
  HookedAction.perform(Engine) — already deprecated; minimum effort
```

Steps 1–5 can be committed independently with no test failures. Step 6 is the only externally visible breaking change. All framework-internal implementations (ElementBoundAction, HookChainAction, HookedAction, FlowExecutor) will have already been updated by then.

---

## Part 4 — ActionCapability Replacement

### Evaluating the Alternatives

**Option A: Keep as enum**

Current state. The problem is known — closed enum, exhaustive switch. Any new domain value must modify the framework enum, triggering review of every existing switch case. Rejected.

**Option B: Interface with well-known constants**

```java
public interface ActionCapability {
    String name();

    // Framework-provided constants
    ActionCapability CLICKABLE  = new NamedCapability("CLICKABLE");
    ActionCapability TYPEABLE   = new NamedCapability("TYPEABLE");
    ActionCapability SELECTABLE = new NamedCapability("SELECTABLE");
    ActionCapability UNKNOWN    = new NamedCapability("UNKNOWN");
}
```

Implementors in other domains:
```java
// In robot domain — no framework modification required
public interface RobotCapabilities {
    ActionCapability JOINT_MOVE  = new NamedCapability("JOINT_MOVE");
    ActionCapability SENSOR_READ = new NamedCapability("SENSOR_READ");
}
```

Profile dispatch changes from exhaustive switch (which doesn't compile on interfaces) to map-based lookup. Fully extensible. Backward-compatible if the constants keep their names. **This is the recommended option.**

**Option C: Annotations**

```java
@Capability("CLICKABLE")
public interface Clickable { ... }
```

Requires reflection at runtime. Non-obvious to framework users. Annotation processors add build complexity. Rejected.

**Option D: Registry (mutable global)**

```java
ActionCapabilityRegistry.register("MY_CAP", MyCapability.INSTANCE);
```

Mutable global state in a library. Test isolation breaks. Class loading order matters. Rejected.

**Option E: Sealed interface hierarchy**

Java 17 `sealed` permits. Forces every domain capability to be declared at framework level. Worse than the enum. Rejected.

**Option F: `String` identity**

`ActionCapability` as a `String`. Zero type safety. Rejected.

---

### Recommended: Option B — Interface With Named Constants

**`ActionCapability.java` — full replacement**

```java
package core.actions;

/**
 * Classifies the interaction type of an {@link Action} for capability-aware profile dispatch.
 *
 * <p>Framework-defined constants ({@link #CLICKABLE}, {@link #TYPEABLE}, etc.) cover standard
 * UI interactions. Domain extensions define their own constants using the same pattern:
 *
 * <pre>
 *   ActionCapability JOINT_MOVE = ActionCapability.of("JOINT_MOVE");
 * </pre>
 *
 * <p>Identity is by {@link #name()} — two instances with the same name are equal.
 */
public interface ActionCapability {

    String name();

    // Well-known UI capabilities
    ActionCapability CLICKABLE  = of("CLICKABLE");
    ActionCapability TYPEABLE   = of("TYPEABLE");
    ActionCapability SELECTABLE = of("SELECTABLE");
    ActionCapability UNKNOWN    = of("UNKNOWN");

    static ActionCapability of(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("capability name must not be blank");
        return new NamedCapability(name);
    }

    record NamedCapability(String name) implements ActionCapability {
        @Override public String toString() { return "ActionCapability(" + name + ")"; }
    }
}
```

**`Profiles.java` — switch → map-based dispatch**

Replace the exhaustive switch in `SAFE` and `RELIABLE` with map-based lookup. Unknown capabilities fall through to the default entry:

```java
public static final ActionProfile SAFE = new ActionProfile() {

    private static final Map<ActionCapability, List<BeforeActionHandler>> BEFORE_MAP = Map.of(
        ActionCapability.TYPEABLE,   List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE),
        ActionCapability.SELECTABLE, List.of(Before.WAIT_FOR_ELEMENT_VISIBLE,
                                             Before.WAIT_FOR_ELEMENT_CLICKABLE,
                                             Before.WAIT_FOR_ANGULAR_LOADER),
        ActionCapability.CLICKABLE,  List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE)
    );
    private static final List<BeforeActionHandler> BEFORE_DEFAULT =
        List.of(Before.WAIT_FOR_ELEMENT_VISIBLE);

    private static final Map<ActionCapability, List<AfterActionHandler>> AFTER_MAP = Map.of(
        ActionCapability.CLICKABLE, List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
    );
    private static final List<AfterActionHandler> AFTER_DEFAULT =
        List.of(After.HIGHLIGHT_ELEMENT);

    @Override
    public List<BeforeActionHandler> before(Action action) {
        return BEFORE_MAP.getOrDefault(action.capability(), BEFORE_DEFAULT);
    }

    @Override
    public List<AfterActionHandler> after(Action action) {
        return AFTER_MAP.getOrDefault(action.capability(), AFTER_DEFAULT);
    }
};
```

Apply same pattern to `RELIABLE`. `DEBUG`, `FAST`, `VISUAL`, `RAW` are unchanged — they don't dispatch on capability.

**How a second domain extends the profile system**

A robot domain developer writes:
```java
// No framework code modified
public final class RobotCapabilities {
    public static final ActionCapability JOINT_MOVE  = ActionCapability.of("JOINT_MOVE");
    public static final ActionCapability SENSOR_READ = ActionCapability.of("SENSOR_READ");
}

public final class RobotProfiles {
    public static final ActionProfile SAFE = new ActionProfile() {
        @Override
        public List<BeforeActionHandler> before(Action action) {
            ActionCapability cap = action.capability();
            if (RobotCapabilities.JOINT_MOVE.equals(cap)) {
                return List.of(RobotBefore.CHECK_JOINT_LIMITS);
            }
            return List.of();
        }
    };
}
```

The framework `Profiles.SAFE` returns its defaults for `JOINT_MOVE` (i.e., `WAIT_FOR_ELEMENT_VISIBLE`, which is wrong). The robot domain should use `RobotProfiles.SAFE` via config: `void.profile.default=SAFE` resolved by its own `ActionProfiles` registration. This is the correct layering.

**Migration from enum**

The `ActionCapability` enum has no declared callers outside the framework's own `Profiles` and `ElementActions` classes. Switch to interface as part of the same PR that introduces `Engine`. No deprecation period needed since this is pre-release code.

---

## Part 5 — Locator Evolution

**Recommendation: Fix later. One exception applies now.**

### Is `LocatorDescriptor` good enough as-is?

For the current scope — UI automation with Selenium or Playwright — yes. The record's fields (`value`, `strategy`, `args`, `parent`) map cleanly to browser concepts. The `resolveDescriptor()` path uses it correctly. The JSON locator asset format maps to it. No second domain exists.

### Should it become `TargetDescriptor`?

When a second domain is introduced — not before. The renaming cost is all call sites across the framework. The conceptual benefit only materializes when a non-UI developer asks "what is a locator?" with no intuitive answer. That question doesn't exist yet.

A rename without a second domain is premature abstraction. It signals a future direction without delivering present value. It adds diff noise to the codebase without fixing a real problem.

### What about `LocatorStrategy`?

`LocatorStrategy` is handled in Week 2 of the sprint — not because it needs domain-agnostic renaming, but because it's a closed enum and making it extensible follows the exact same pattern as `ActionCapability`.

### The one exception: rename `parent` to `scope`

`parent` is a DOM-centric name. It implies `<div>` containment. `scope` says "find this descriptor within this context" — which is true for DOM nesting, but also for robot subsystems, agent registries, and any hierarchical target space.

This is a record field rename. It affects `LocatorDescriptor`, `SeleniumEngine.toBy()`, and the few callers of `isScoped()` and `withParent()`. The rename costs one focused hour and permanently reduces the conceptual DOM bias. Rename `parent` → `scope` and `withParent()` → `withScope()` in the same PR as the `LocatorStrategy` extensibility work (Week 2).

**Summary:** Fix the `parent` → `scope` rename now. Everything else waits for the second domain.

---

## Part 6 — AI-Native Direction

### What already exists

| Pipeline stage | Existing artifact | Status |
|---|---|---|
| Locator asset format | `JsonLocatorSource`, `JsonLocatorReader` | Ready |
| Parameterized locators | `LocatorTemplate` with `%s` formatting | Ready |
| Element discovery infrastructure | `EnumLocatorScanner` | Ready |
| Asset migration CLI | `JsonMigratorCli` | Ready |
| Capability classification | `Clickable`, `Typeable`, `Selectable` interfaces | Ready |
| Engine-neutral descriptor | `LocatorDescriptor` | Ready |
| Config-driven profile selection | `void.profile.default`, `ActionProfiles` | Ready |
| Flow execution of generated artifacts | `Flow.of(...)`, `app.run(flow)` | Ready |

### What is missing

| Pipeline stage | Gap | Nature |
|---|---|---|
| DOM → LLM analysis | No LLM integration | Implementation |
| LLM output schema | No JSON schema for element discovery results | Design + implementation |
| LLM → enum code generation | No code generator | Implementation |
| Capability assignment from LLM output | No schema + mapping | Design + implementation |
| Flow definition as external artifact | No external flow format | Design + implementation |

Every missing piece is tooling above the runtime. The runtime architecture does not need to change to support this pipeline. The gap between current LLM readiness (2/10) and architectural readiness (8/10) is entirely in tooling that sits on top of the runtime.

### Would you actively optimize for this direction today?

No. Not yet.

**Reasoning:** Adding AI-specific abstractions to the runtime before the tooling exists inverts the dependency. The correct sequence is:
1. Build the LLM tooling as a separate module or tool (`void-ai-tools` or similar)
2. Discover which runtime abstractions the tooling actually needs
3. If the runtime needs to change, change it with a concrete use case driving the change

Premature runtime optimization for an AI direction that doesn't yet have working tooling produces speculative abstractions. The runtime is already well-positioned. The descriptor-first design, externalized JSON assets, and capability classification are exactly what AI generation needs — and they exist today.

The one productive action now: write a `docs/architecture/ai-native-pipeline.md` that documents the intended flow and the artifact formats. This costs nothing architecturally, creates a shared understanding, and gives the tooling team a target. Do not add new runtime classes until the tooling validates the design.

---

## Part 7 — What the Audits Got Wrong

### Overstated Risks

**`LocatorDescriptor.parent` as a major concern.**

All three audits mentioned the DOM-centric scoping model as a meaningful blocker. It isn't. It's one field name in a private record. The structural pattern is sound. Until a second-domain developer asks "what does parent mean for a robot joint?", this concern has no practical effect. It was given attention proportional to its conceptual impact, not its actual remediation cost.

**Multi-domain flow coexistence.**

Round 3 raised "what happens when UI and robot actions coexist in a single Flow." This required architectural design work per Round 3. It does not. A `FlowExecutor` holding an `Engine` executes any `Action` — the actions themselves carry their domain logic via lambdas. The only requirement is that the engine type matches what the actions expect. Single-domain-per-session solves this entirely. The concern was answered correctly but raised unnecessarily.

**`ByParser` in the resolver layer.**

All three audits called this out as coupling in the wrong place. `ByParser` is a two-method class that wraps Selenium's `By.cssSelector`, `By.xpath`, etc. Moving it from `core.resolvers.locator.parser` to `core.engine.selenium` is a package rename with no architectural consequence. It was treated as a meaningful design violation when it's a 10-minute cleanup.

### Understated Risks

**`ActionHandler.execute(UIEngine, LocatorDescriptor)` as the true propagation bottleneck.**

None of the three audits named this explicitly as a migration surface. When `Action.perform(UIEngine)` changes to `Action.perform(Engine)`, the hook dispatch layer (`HookedAction.executeHooks`, `HookChainAction.perform`) must bridge the type change. Every `Before.*` and `After.*` constant carries `UIEngine` in its lambda parameter. The total number of impacted hook implementations is larger than the "six type signatures" claim from Round 2.

This doesn't invalidate the conclusion (the change is still mechanical), but the surface area was underestimated by roughly 3×.

**`ActionCapability` severity.**

Rounds 1 and 2 did not assess it. Round 3 classified it correctly as the next hidden UI assumption. But none of the audits noted that it was introduced on the branch under evaluation — meaning it is new code, not legacy debt. The window to fix it at zero migration cost was the branch it was introduced on. That window closes the moment the branch ships.

**The `VOID.start()` session API mismatch for non-browser domains.**

`VOID.navigateTo()`, `getCurrentUrl()`, `getTitle()`, `refresh()` are browser navigation methods on the session facade. No audit examined what `VOID.start()` means semantically for a robot or agent domain. A `RobotSession.start()` would return a different session type with different top-level methods. The `VOID` class is currently both the session contract AND the browser navigation API. These need separation before a second domain entry point is designed.

No audit named this as a concern. It surfaces the moment someone asks: "where does `robot.moveTo(joint)` go in the VOID API?"

### Incorrect Assumptions

**Round 1 and Round 2: "Six type signature changes cover the migration."**

Evidence from the code: `Action.java` (2 pins: `perform`, `resolve`), `FlowExecutor.java` (1), `HookChainAction.java` (2), `HookedAction.java` (2), `ElementBoundAction` inner class (2), `ActionHandler.java` (1) = 10+ pins, plus BiConsumer typing in `ElementActions.of()`. The "six signatures" claim was based on listing file names rather than counting all signatures in each file.

**Round 2: `ActionCapability` classified as "Implementation Coupling" with moderate severity.**

Code evidence: `Profiles.SAFE` and `Profiles.RELIABLE` both use exhaustive `switch` over `ActionCapability`. Two exhaustive switches over a four-value closed enum, in the framework's own profile library, introduced on the branch being evaluated. This is a structural lock-in being built in real time. "Implementation coupling" implies it can be changed later without ripple effects. The exhaustive switch guarantee means changing the enum causes mandatory review of both switches. This warranted "Important" remediation priority, not "Implementation Coupling."

---

## Part 8 — Roadmap

### Next Release

**Purpose:** Close the Engine abstraction gap. Make the framework capable of hosting a second engine without type system surgery.

| Change | Why | Cost | Risk | Payoff |
|---|---|---|---|---|
| Introduce `Engine` interface | Required for all further generalization | 1 file, 5 lines | Zero — additive | Unlocks Playwright, CDP, all future engines |
| `UIEngine extends Engine` | Completes the boundary | 1 line | Zero — backward compatible | UIEngine becomes a domain-specific subtype |
| `Action.perform(Engine)` + `Action.ui()` helper | Breaks the execution type pin | ~10 type signatures | Low — migration helper prevents breakage for UI lambdas | Any Action can run against any engine |
| `FlowExecutor(Engine)`, `HookChainAction(Engine)` | Same pin in orchestration layer | 4 signatures | Low | Execution infrastructure is domain-neutral |
| `ActionCapability` → interface | Prevents profile dispatch hardening | Replace 12 lines | Low — same names, same values, behavior identical | Second-domain profiles work without framework modification |
| `Profiles.SAFE`/`RELIABLE` → map-based dispatch | Required by ActionCapability change | ~20 lines | Low — output identical for existing capabilities | Profile system is open to extension |
| Wire `SessionContext` into `VOID.start()` | Completes the `ExecutionContext` migration | 10 lines in VOID.java | Low — SessionContext already tested | WebDriver is no longer the session anchor |

---

### Next 3 Releases

**Purpose:** Complete the locator path migration. Begin formal deprecation of Selenium-coupled APIs. Prepare factory for non-browser engines.

| Change | Why | Cost | Risk | Payoff |
|---|---|---|---|---|
| `LocatorStrategy` → interface with constants | Extensibility for non-browser strategies | 1 file replace + LocatorResolver.inferStrategy() update | Low | Robot/agent locator strategies possible |
| `LocatorDescriptor.parent` → `scope` | Reduce DOM-centric conceptual coupling | Record field rename + 10-20 call sites | Low | Second-domain developers won't be confused |
| Formal deprecation of all `By`-returning paths in `LocatorResolver` | End the dual-path state | Add `@Deprecated` to 5 methods | Zero | Forces callers onto `resolveDescriptor()` |
| Move `ByParser` to `core.engine.selenium` | Wrong layer | Package rename | Zero | Resolver layer has no Selenium imports |
| `UIEngineFactory.create()` removes `WebDriver` parameter | Factory contract is wrong per its own Javadoc | ~15 lines | Medium — callers of `UIEngineFactory` directly affected | Factory is domain-agnostic; each engine creates its own driver |
| Deprecate `ExecutionContext` (formal annotation + forRemoval=true) | `SessionContext` replaced it | 1 annotation | Zero | Signals clear removal |
| Deprecate `UIEngine.waitForOverlay()` | CDK concept in engine contract | 1 annotation + Javadoc pointing to profile hook | Zero | Engine contract stays domain-clean |

---

### Long-Term

**Purpose:** Second-domain entry point. Tooling layer. Formal 3.0 removal of deprecated APIs.

| Change | Why | Cost | Risk | Payoff |
|---|---|---|---|---|
| `BrowserSession extends VOID` (or `VOID` becomes abstract) | Separates browser navigation API from session contract | Medium refactor of VOID.java | Medium — public API shape changes | Non-browser domains have their own session facade without browser pollution |
| Remove `By`-returning resolution paths | Complete the migration started in "next 3 releases" | Delete 5 methods | Low (callers already warned) | Clean resolver API |
| Remove `ExecutionContext`, `HookedAction`, `Interactions` | `@Deprecated(forRemoval)` items scheduled for 3.0 | Delete + fix remaining callers | Low (callers already warned) | Framework removes ~300 lines of legacy code |
| `ActionHandler<E extends Engine>` generalization | Phase 2 hook generalization | Medium — all hook constants need generic typing | Medium — breaking for hook implementors | Non-UI domains can have first-class typed hooks in the profile system |
| `void-ai-tools` module: JSON schema, LLM integration, code generator | Unlock the AI generation pipeline | High implementation effort, zero runtime risk | Low — purely additive | Framework positions as AI-native automation runtime |
| Publish `LocatorDescriptor` JSON schema | Allows external tooling to generate valid assets | Low | Zero | LLM tooling has a target format |

---

## Final Question

**Smallest set of changes producing the largest long-term architectural improvement.**

Five changes. Approximately 60–80 lines modified or added across 8 files. One new file. No test logic changes.

---

### Change 1: `core/engine/Engine.java` (new file)

```java
public interface Engine {
    String getEngineName();
    void shutdown();
}
```

One file, five lines. Zero risk.

---

### Change 2: `core/engine/UIEngine.java` — one line

```java
public interface UIEngine extends Engine {
```

Additive. Every `UIEngine` reference remains valid. Every `SeleniumEngine` still compiles.

---

### Change 3: `core/actions/Action.java` — three changes

1. `void perform(Engine engine);`
2. `default LocatorDescriptor resolve(Engine engine) { return null; }`
3. Add `static Action ui(Consumer<UIEngine> op) { return engine -> op.accept((UIEngine) engine); }`

The `ui()` static helper is the migration bridge. Without it, this is a hard breaking change for all UI lambdas. With it, UI-domain code migrates in one search-and-replace.

---

### Change 4: `core/actions/ActionCapability.java` — full replacement

Replace the 12-line enum with the interface-with-constants design shown in Part 4. Constants keep their names. Behavior is identical. `Profiles.SAFE`/`RELIABLE` switch statements become map-based dispatch. No external callers need to change.

This is the only change that must ship with the branch. After release, making `ActionCapability` extensible costs a major version bump. Before release, it costs 30 lines.

---

### Change 5: Wire `core/context/SessionContext` into `VOID.start()`

`SessionContext` holds `UIEngine`, not `WebDriver`. It already exists. `VOID.start()` still uses `ExecutionContext` (which holds `WebDriver`). The wiring:

```java
// VOID.start() — after change:
public static VOID start(DriverFactory.Profile profile) {
    FrameworkBootstrap.init();
    WebDriver driver = DriverManager.createDriver(profile);
    UIEngine engine = UIEngineFactory.create(FrameworkBootstrap.getUtilsConfig(), driver);
    SessionContext ctx = new SessionContext(FrameworkBootstrap.getUtilsConfig(), engine);
    // ExecutionContext no longer created here
    return new VOID(ctx, engine);
}
```

`ExecutionContext` becomes `@Deprecated(since="2.0", forRemoval=true)` with a Javadoc note pointing to `SessionContext`. The public `getContext()` method on `VOID` (already deprecated) stays deprecated.

---

### Why these five changes have disproportionate payoff

Together they do the following without changing any element code, any capability interface, any test code, or any Selenium implementation:

1. The runtime's execution layer (`FlowExecutor`, `Action`, `HookChainAction`) no longer has a compile-time dependency on `UIEngine`. Any engine can be constructed and passed through the pipeline.

2. The profile dispatch system (`Profiles.SAFE`, `RELIABLE`) is no longer locked to a four-value closed enum. Adding a second domain's capabilities requires no modification of framework code.

3. The session context no longer holds a raw `WebDriver`. The framework's concept of "a session" is now an engine, not a browser driver. All future `VOID.start()` variants — `VOID.start(EngineType.PLAYWRIGHT)`, `VOID.start(EngineType.ROBOT)` — are architectural additions, not structural breaks.

4. `UIEngine` stays intact. `SeleniumEngine` stays intact. All existing test code continues to compile and run without modification.

The long-term payoff: Playwright support becomes a `UIEngine` implementation. A robot domain becomes an `Engine` implementation with its own capability constants and profiles. The AI tooling layer generates `LocatorDescriptor`-backed JSON assets without touching runtime code. None of these require further architectural surgery after these five changes.

---

*Remediation plan derived from three rounds of architectural audit against the updated branch. All file references verified against current source.*