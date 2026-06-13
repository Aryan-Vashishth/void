# Phase 5 — Execution Pipeline Boundary

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Medium — new abstraction, existing behavior preserved as default

---

## Objective

Keep `FlowExecutor` permanently small and focused on orchestration only. Extract the per-action execution concerns into a dedicated `ExecutionPipeline` layer so future features (retry, timeout, tracing, metrics, parallel execution) have a home without polluting `FlowExecutor`.

---

## Context

Current `FlowExecutor` is elegant and minimal:

```java
public void run(Flow flow) {
    for (Action action : flow.getActions()) {
        action.perform(engine);
    }
}
```

This is good. The risk is that features like retry and timeout will eventually be bolted onto `FlowExecutor` directly, turning it into an orchestrator with embedded cross-cutting logic:

```java
// FUTURE SMELL — what we want to prevent
public void run(Flow flow) {
    for (Action action : flow.getActions()) {
        int retries = config.getRetries();
        Duration timeout = config.getTimeout();
        long start = System.currentTimeMillis();
        try {
            action.perform(engine);
            recordMetric(action, SUCCESS);
        } catch (Exception e) {
            if (retries > 0) retry(action, retries);
            else fail(action, e);
        }
    }
}
```

At that point `FlowExecutor` is no longer an orchestrator — it is a monolith.

---

## Target Design

```text
VOID.run(flow)
      ↓
FlowExecutor.run(flow)       ← stays minimal, iterates actions
      ↓
ExecutionPipeline.execute(action, engine)  ← owns cross-cutting concerns
      ↓
action.perform(engine)       ← pure intent execution
```

### `ExecutionPipeline` (new interface)

```java
package core.executor;

import core.actions.Action;
import core.engine.UIEngine;

/**
 * Executes a single action through the framework pipeline.
 * Cross-cutting concerns (retry, tracing, metrics) live here — not in FlowExecutor.
 */
public interface ExecutionPipeline {
    void execute(Action action, UIEngine engine);
}
```

### `DefaultExecutionPipeline` (initial implementation)

```java
package core.executor;

/**
 * Default pipeline: executes action directly with no extra wrapping.
 * Preserves the existing single-step behavior while establishing the contract.
 */
public class DefaultExecutionPipeline implements ExecutionPipeline {

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
        this(engine, new DefaultExecutionPipeline());
    }

    public FlowExecutor(UIEngine engine, ExecutionPipeline pipeline) {
        this.engine = engine;
        this.pipeline = pipeline;
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

`FlowExecutor` stays tiny. Forever.

---

## Future Pipeline Implementations (Out of Scope for This Phase)

These show why the abstraction is worth making now:

```java
RetryingExecutionPipeline     // wraps execute() in retry loop
TimedExecutionPipeline        // wraps execute() in timeout check
TracingExecutionPipeline      // emits ActionTrace before/after execute()
MetricsExecutionPipeline      // records duration and success/fail metrics
CompositeExecutionPipeline    // chains multiple pipelines
```

---

## Affected Files

New:
- `src/main/java/core/executor/ExecutionPipeline.java`
- `src/main/java/core/executor/DefaultExecutionPipeline.java`

Modified:
- `src/main/java/core/executor/FlowExecutor.java`
- `src/main/java/core/runtime/VOID.java` _(if it constructs FlowExecutor directly)_

---

## Checklist

### Infrastructure
- [ ] Create `ExecutionPipeline` interface.
- [ ] Create `DefaultExecutionPipeline` — pure `action.perform(engine)`.
- [ ] Update `FlowExecutor` to accept `ExecutionPipeline` in constructor.
- [ ] Keep no-arg-style constructor for backward compatibility (default pipeline).

### Verification
- [ ] Existing behavior is 100% unchanged — `DefaultExecutionPipeline` is a direct pass-through.
- [ ] `VOID.run(flow)` still works without any code changes.
- [ ] `VOID.run(action)` still works without any code changes.

### Tests
- [ ] Unit test: `FlowExecutor` with `DefaultExecutionPipeline` executes all actions in order.
- [ ] Unit test: `FlowExecutor` with a custom `ExecutionPipeline` delegates to it per action.
- [ ] Unit test: custom pipeline receives the correct `action` and `engine` arguments.
- [ ] Integration test: full flow run through VOID session works unchanged.

---

## Exit Criteria

- `FlowExecutor` delegates all per-action execution to `ExecutionPipeline`.
- `FlowExecutor` contains no retry, timeout, tracing, or metrics logic.
- All existing tests pass without modification.

---

## What NOT to Do

- Do not implement retry or timeout in this phase — the goal is to create the slot for them.
- Do not change the `VOID` session API — it must continue to work transparently.
- Do not make `ExecutionPipeline` configurable via properties in this phase.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

