# 009 — Action / Flow / FlowExecutor Execution Model

**Date:** 2026-06-01  
**Status:** Accepted

---

## Context

VOID needed a primary execution path that is:
- Fully engine-agnostic (no `WebDriver`, `WebElement`, or `By` references)
- Composable (multiple UI operations as a single logical unit)
- Deferred (locator resolution at execution time, not definition time)
- Minimal (no framework overhead, no complex dispatch)

The legacy `Interactions` class served as both orchestrator and (previously) executor. While refactored to delegate to `UIEngine`, it still exposes Selenium-specific types in its API surface and uses imperative call patterns.

---

## Decision

Introduce a three-part execution model:

### `Action` — Deferred Execution Intent

```java
@FunctionalInterface
public interface Action {
    void perform(UIEngine engine);
}
```

- Single method: `perform(UIEngine)`
- Produced by capability interface default methods (e.g., `Clickable.click()`, `Typeable.type("text")`)
- Locator resolution happens **inside** the lambda
- Never references Selenium types

### `Flow` — Ordered Sequence of Actions

```java
public class Flow {
    public static Flow of(Action... actions) { ... }
    public List<Action> getActions() { ... }
}
```

- Immutable
- Factory method: `Flow.of(action1, action2, action3)`
- Represents a declarative UI workflow

### `FlowExecutor` — Sequential Executor

```java
public class FlowExecutor {
    public FlowExecutor(UIEngine engine) { ... }
    public void run(Flow flow) { ... }
    public void run(Action action) { ... }
}
```

- Holds a `UIEngine` reference
- `run(Flow)` iterates all Actions sequentially
- `run(Action)` runs a single Action
- Uses ONE verb consistently ("run") for both Flow and Action

---

## Reasoning

1. **Minimal surface area** — Action is a single functional interface. Flow is 5 methods. FlowExecutor is 2 methods. Total: ~100 lines of code.
2. **Composability** — `Flow.of(...)` reads like a test script. Actions can be mixed freely.
3. **Deferred resolution** — Locators aren't resolved until `action.perform(engine)` is called. No stale locator risk.
4. **Engine-agnostic by construction** — Action only knows about `UIEngine`. No Selenium imports possible.
5. **No framework tax** — No annotation processing, no reflection, no lifecycle hooks. Pure Java method calls.
6. **Clear naming** — `FlowExecutor` describes its responsibility: executing Actions and Flows via UIEngine. Visible in stack traces.

---

## Usage Example

```java
FlowExecutor executor = new FlowExecutor(engine);

// Compose a login flow
Flow loginFlow = Flow.of(
    LoginPage.USERNAME.type("admin@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
);

// Execute
executor.run(loginFlow);

// Single action
executor.run(DashboardPage.PROFILE_BUTTON.click());
```

---

## Consequences

- **Primary path for new code** — Action/Flow/FlowExecutor is the recommended execution path
- **Interactions remains** as frozen legacy orchestrator for backward compatibility
- **Capability interfaces are dual-purpose** — they define structure AND emit Actions
- **VoidDSL** continues to use `Interactions` internally (for BDD context-driven resolution)
- **No migration required** — both paths coexist. New examples use Action/Flow/FlowExecutor. Existing examples use `Interactions` unchanged.

### Path Comparison

| Aspect | Action/Flow/FlowExecutor | Interactions (Legacy) |
|--------|-------------------|----------------------|
| Engine coupling | None | `ActionHandler` receives `UIEngine` |
| Locator type | `LocatorDescriptor` | `By` (Selenium) |
| Hook support | `.withHooks(before, after)` on Action | `Before.*` / `After.*` composable hooks |
| Composability | `Flow.of(...)` | Imperative method calls |
| New features | Yes | Frozen — no new features |

---

## Related

- [007 — UIEngine as Single Execution Authority](007-uiengine-execution-authority.md) — the engine these Actions execute against
- [008 — Capability Interfaces](008-capability-interfaces.md) — the interfaces that emit Actions
- [System Overview](../../../5-architecture/system-overview.md) — full architecture diagram

