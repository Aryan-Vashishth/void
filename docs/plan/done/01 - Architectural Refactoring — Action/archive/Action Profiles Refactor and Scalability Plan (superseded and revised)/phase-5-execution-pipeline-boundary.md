# Phase 5 — Execution Pipeline Boundary

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low-Medium — new abstraction layer; existing behavior is 100% preserved via pass-through default

---

## Objective

Keep `FlowExecutor` permanently focused on orchestration only. Establish a dedicated `ExecutionPipeline` slot where all per-action cross-cutting concerns (retry, timeout, tracing, metrics) will live — without ever entering `FlowExecutor`.

---

## Context

`FlowExecutor` is currently minimal:

```java
public void run(Flow flow) {
    for (Action action : flow.getActions()) {
        action.perform(engine);
    }
}
```

This is correct. The risk is that without an explicit boundary, cross-cutting features accumulate in `FlowExecutor` over time:

```java
// FUTURE SMELL — what this phase prevents
public void run(Flow flow) {
    for (Action action : flow.getActions()) {
        long start = System.currentTimeMillis();
        try {
            action.perform(engine);
            metrics.record(action, SUCCESS, elapsed(start));
        } catch (Exception e) {
            if (config.getRetries() > 0) retry(action);
            else { metrics.record(action, FAILED, elapsed(start)); throw e; }
        }
    }
}
```

At that point `FlowExecutor` is a monolith. This phase creates the boundary before the pressure arrives.

---

## Target Design

```
VOID.run(flow)
      ↓
FlowExecutor.run(flow)             ← stays minimal: iterate actions, nothing else
      ↓
ExecutionPipeline.execute(action)  ← owns all per-action cross-cutting concerns
      ↓
action.perform(engine)             ← pure deferred intent execution
```

### `ExecutionPipeline` — New Interface

```java
package core.executor;

import core.actions.Action;
import core.engine.UIEngine;

/**
 * Executes a single action through the framework's cross-cutting concern layer.
 *
 * <h3>Composition</h3>
 * <p>Pipelines are composed via <b>decoration</b>. Each implementation wraps
 * the next in the chain. Build the chain at construction time; chains are
 * immutable at runtime. Do not use a list-of-stages model — each stage is
 * responsible for calling the next stage itself.</p>
 *
 * <pre>
 *   ExecutionPipeline pipeline =
 *       new RetryingExecutionPipeline(
 *           new TimedExecutionPipeline(
 *               DefaultExecutionPipeline.INSTANCE, Duration.ofSeconds(10)),
 *           3);
 * </pre>
 *
 * <h3>Naming convention</h3>
 * <p>Implementations follow {@code <Adjective>ExecutionPipeline}:
 * {@code RetryingExecutionPipeline}, {@code TimedExecutionPipeline},
 * {@code TracingExecutionPipeline}, {@code MetricsExecutionPipeline}.</p>
 */
public interface ExecutionPipeline {
    void execute(Action action, UIEngine engine);
}
```

### `DefaultExecutionPipeline` — Initial Implementation

```java
package core.executor;

/**
 * Pass-through pipeline. Calls {@code action.perform(engine)} directly.
 * Preserves existing single-step behavior while establishing the pipeline contract.
 *
 * <p>Use {@link #INSTANCE} rather than constructing a new instance.</p>
 */
public final class DefaultExecutionPipeline implements ExecutionPipeline {

    public static final ExecutionPipeline INSTANCE = new DefaultExecutionPipeline();

    private DefaultExecutionPipeline() {}

    @Override
    public void execute(Action action, UIEngine engine) {
        action.perform(engine);
    }
}
```

### Updated `FlowExecutor`

```java
public class FlowExecutor {

    private final UIEngine engine;
    private final ExecutionPipeline pipeline;

    public FlowExecutor(UIEngine engine) {
        this(engine, DefaultExecutionPipeline.INSTANCE);
    }

    public FlowExecutor(UIEngine engine, ExecutionPipeline pipeline) {
        this.engine   = Objects.requireNonNull(engine,   "engine must not be null");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline must not be null");
    }

    public void run(Flow flow) {
        for (Action action : flow.getActions()) {
            pipeline.execute(action, engine);
        }
    }

    public void run(Action action) {
        pipeline.execute(action, engine);
    }
}
```

`FlowExecutor` stays at this size. No retry, no timeout, no metrics, no tracing — ever.

---

## Composition Pattern

Pipelines are composed by **wrapping**, not by listing stages. Each implementation holds a reference to the next pipeline and calls it at the appropriate point:

```java
// Correct — each pipeline is responsible for calling the next
class RetryingExecutionPipeline implements ExecutionPipeline {
    private final ExecutionPipeline next;
    private final int maxAttempts;

    public RetryingExecutionPipeline(ExecutionPipeline next, int maxAttempts) {
        this.next = next;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void execute(Action action, UIEngine engine) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try { next.execute(action, engine); return; }
            catch (RuntimeException e) { last = e; }
        }
        throw last;
    }
}

// Wrong — do not build a list-of-stages model
class CompositeExecutionPipeline implements ExecutionPipeline {
    private final List<ExecutionPipeline> stages; // each stage executes independently, no natural chaining
}
```

---

## Planned Future Implementations (Out of Scope for This Phase)

These types justify the abstraction. Each will be a focused, independent addition requiring no changes to `FlowExecutor` or the `ExecutionPipeline` interface.

| Type | Wraps | Responsibility |
|------|-------|----------------|
| `RetryingExecutionPipeline` | next pipeline | Retries on `RuntimeException`; configurable attempt count and exception predicate |
| `TimedExecutionPipeline` | next pipeline | Enforces a per-action wall-clock limit |
| `TracingExecutionPipeline` | next pipeline | Records orchestration-level trace (distinct from hook-level `ActionTrace`) |
| `MetricsExecutionPipeline` | next pipeline | Reports action duration and pass/fail rate to external metric sinks |

These are implementation phases, not design phases. The contract is established here; implementations follow independently without modifying this interface.

---

## Affected Files

New:
- `src/main/java/core/executor/ExecutionPipeline.java`
- `src/main/java/core/executor/DefaultExecutionPipeline.java`

Modified:
- `src/main/java/core/executor/FlowExecutor.java` — add `pipeline` field, two-arg constructor, delegate run calls

Possibly modified:
- `src/main/java/core/runtime/VOID.java` — if it constructs `FlowExecutor` directly, ensure the single-arg constructor is used (no external API change)

---

## Migration Strategy

No migration required. The single-argument `FlowExecutor(UIEngine)` constructor defaults to `DefaultExecutionPipeline.INSTANCE`, which is a direct pass-through. All existing `VOID.run(flow)` and `VOID.run(action)` calls execute identically. The `@Beta` annotation on `FlowExecutor` already signals that its constructor signature may evolve.

---

## Checklist

### Infrastructure
- [ ] Create `ExecutionPipeline` interface with `execute(Action, UIEngine)`.
- [ ] Create `DefaultExecutionPipeline` with private constructor and `INSTANCE` singleton.
- [ ] Add `pipeline` field to `FlowExecutor`.
- [ ] Add two-arg constructor `FlowExecutor(UIEngine, ExecutionPipeline)`.
- [ ] Update single-arg constructor to delegate to two-arg with `DefaultExecutionPipeline.INSTANCE`.
- [ ] Update `FlowExecutor.run(Flow)` to call `pipeline.execute(action, engine)`.
- [ ] Update `FlowExecutor.run(Action)` to call `pipeline.execute(action, engine)`.

### Documentation
- [ ] Add Javadoc to `ExecutionPipeline` explaining the decoration pattern and naming convention.
- [ ] Add `@see ExecutionPipeline` note in `FlowExecutor` Javadoc.
- [ ] Add forward reference in `src/main/java/core/executor/README.md` (update when it exists).

---

## Tests

- [ ] `FlowExecutor` with `DefaultExecutionPipeline.INSTANCE` executes all actions in order — identical behavior to current.
- [ ] `FlowExecutor` with a recording `ExecutionPipeline` (test double) calls `execute()` once per action.
- [ ] Recording pipeline receives the correct `action` and `engine` arguments per invocation.
- [ ] `FlowExecutor(UIEngine)` and `FlowExecutor(UIEngine, DefaultExecutionPipeline.INSTANCE)` produce identical behavior.
- [ ] `FlowExecutor` does not call `action.perform(engine)` directly — all calls go through the pipeline.
- [ ] Integration: full flow run through `VOID` session produces unchanged behavior.

---

## Exit Criteria

- `FlowExecutor` delegates every per-action execution call to `ExecutionPipeline`.
- `FlowExecutor` contains no retry, timeout, metrics, or tracing logic.
- `DefaultExecutionPipeline` is a pure pass-through: zero additional overhead beyond `action.perform(engine)`.
- All existing tests pass without modification.

---

## What NOT to Do

- Do not implement retry, timeout, or metrics in this phase — the goal is to establish the slot, not fill it.
- Do not create a `CompositeExecutionPipeline` with a `List<ExecutionPipeline>` — use decoration (each pipeline wraps the next).
- Do not change the `VOID` session API — callers must see no difference.
- Do not make `ExecutionPipeline` configurable via properties files in this phase.
- Do not add pipeline selection logic inside `FlowExecutor` — pipeline selection belongs at construction time (composition root), not at orchestration time.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
