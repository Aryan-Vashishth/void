# Phase 10 — Execution Pipeline Implementations

**Status:** Ongoing (blocked on Phase 5)  
**Architecture Version:** 2.3  
**Branch:** Per-implementation feature branch  
**Risk:** Low per implementation — each is additive, isolated, and independently removable

---

## Objective

Prove the `ExecutionPipeline` abstraction from Phase 5 by implementing the four cross-cutting concerns it was designed to carry. Each implementation is a focused, independently deployable decoration — no changes to `FlowExecutor`, `ExecutionPipeline`, or `DefaultExecutionPipeline`.

---

## Context

Phase 5 established the contract:

```
FlowExecutor  →  ExecutionPipeline  →  action.perform(engine)
```

`DefaultExecutionPipeline.INSTANCE` is a pass-through. The abstraction boundary exists — but an abstraction without implementations proves nothing about the design. This phase provides the implementations that validate the pattern, each addressing one concern.

---

## Implementations

### 1. `RetryingExecutionPipeline`

Retries failed action executions up to a configurable limit. Retries on `RuntimeException` only; configurable exception predicate for finer control.

```java
public final class RetryingExecutionPipeline implements ExecutionPipeline {
    private final ExecutionPipeline next;
    private final int maxAttempts;
    private final Predicate<RuntimeException> retryIf;

    public RetryingExecutionPipeline(ExecutionPipeline next, int maxAttempts) {
        this(next, maxAttempts, e -> true);
    }

    public RetryingExecutionPipeline(ExecutionPipeline next, int maxAttempts,
                                     Predicate<RuntimeException> retryIf) {
        this.next = Objects.requireNonNull(next);
        this.maxAttempts = maxAttempts;
        this.retryIf = Objects.requireNonNull(retryIf);
    }

    @Override
    public void execute(Action action, UIEngine engine) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                next.execute(action, engine);
                return;
            } catch (RuntimeException e) {
                if (!retryIf.test(e)) throw e;
                last = e;
            }
        }
        throw last;
    }
}
```

### 2. `TimedExecutionPipeline`

Enforces a per-action wall-clock limit. Throws `ActionTimeoutException` (or a configured RuntimeException subclass) when the limit is exceeded.

```java
public final class TimedExecutionPipeline implements ExecutionPipeline {
    private final ExecutionPipeline next;
    private final Duration limit;

    @Override
    public void execute(Action action, UIEngine engine) {
        // implementation: run next.execute() on a separate thread or
        // with engine-level timeout, throw on breach
    }
}
```

Implementation notes: prefer engine-level timeout when available (avoids thread creation overhead). Fall back to `Future.get(timeout)` if engine offers no per-action limit.

### 3. `TracingExecutionPipeline`

Records an orchestration-level trace per action (distinct from the hook-level `ActionTrace` in `HookedAction`). Orchestration trace includes: action identity, start/end timestamps, outcome, and pipeline depth.

```java
public final class TracingExecutionPipeline implements ExecutionPipeline {
    private final ExecutionPipeline next;
    private final TraceConsumer consumer;   // e.g. logger, metrics sink, in-memory store

    @Override
    public void execute(Action action, UIEngine engine) {
        long start = System.nanoTime();
        try {
            next.execute(action, engine);
            consumer.record(action, SUCCEEDED, elapsed(start));
        } catch (RuntimeException e) {
            consumer.record(action, FAILED, elapsed(start));
            throw e;
        }
    }
}
```

### 4. `MetricsExecutionPipeline`

Reports action duration and pass/fail rate to an external metric sink. Minimal overhead — no retry, no timeout, no tracing. Composed around `DefaultExecutionPipeline` when metrics-only is needed.

---

## Composition Pattern (Reminder from Phase 5)

Pipelines compose by wrapping — each holds a reference to the next and calls it:

```java
ExecutionPipeline pipeline =
    new RetryingExecutionPipeline(
        new TimedExecutionPipeline(
            new TracingExecutionPipeline(
                DefaultExecutionPipeline.INSTANCE,
                traceConsumer),
            Duration.ofSeconds(10)),
        3);

FlowExecutor executor = new FlowExecutor(engine, pipeline);
```

The composition root (where the chain is built) is in application setup code — not in `FlowExecutor`. `FlowExecutor` never changes.

---

## Affected Files

New (one per implementation):
- `src/main/java/core/executor/RetryingExecutionPipeline.java`
- `src/main/java/core/executor/TimedExecutionPipeline.java`
- `src/main/java/core/executor/TracingExecutionPipeline.java`
- `src/main/java/core/executor/MetricsExecutionPipeline.java`

Not modified:
- `FlowExecutor` — must not change
- `ExecutionPipeline` interface — must not change
- `DefaultExecutionPipeline` — must not change

---

## Checklist

Each implementation follows the same checklist independently:

### Per implementation
- [ ] Class implements `ExecutionPipeline` directly — no abstract superclass.
- [ ] Holds `next` reference; calls `next.execute(action, engine)` inside `execute()`.
- [ ] All constructor arguments are validated with `Objects.requireNonNull()`.
- [ ] No retry/timeout/tracing logic bleeds across implementations.
- [ ] Passes `FlowExecutor` integration test: behavior identical to `DefaultExecutionPipeline` when no error occurs.

### `RetryingExecutionPipeline` specific
- [ ] Retries exactly `maxAttempts - 1` times after the first failure.
- [ ] Does not retry if `retryIf` predicate returns false.
- [ ] Re-throws the last exception if all attempts fail.
- [ ] Does not swallow exceptions from non-retryable types.

### `TimedExecutionPipeline` specific
- [ ] Throws on deadline breach — does not return normally.
- [ ] Does not introduce unbound thread creation per action.
- [ ] Timeout applies per `execute()` call, not per pipeline lifetime.

### `TracingExecutionPipeline` specific
- [ ] Records a trace entry for both success and failure.
- [ ] Trace identity is derived from `action.resolve()` or a logged string — not from internal class names.
- [ ] Does not suppress exceptions.

### `MetricsExecutionPipeline` specific
- [ ] Minimal overhead — no allocation beyond the metric recording call.
- [ ] Does not duplicate tracing concerns (no timestamp logging, no exception detail).

---

## Tests

Per implementation (independent test classes):

### `RetryingExecutionPipelineTest`
- [ ] Succeeds on first attempt — `next` called once.
- [ ] Retries exactly `maxAttempts - 1` times then re-throws.
- [ ] Stops retrying when `retryIf` returns false.
- [ ] Re-throws the last exception, not a wrapped one.

### `TimedExecutionPipelineTest`
- [ ] Succeeds without timeout when action completes within limit.
- [ ] Throws when action exceeds limit.
- [ ] Cleanup: no thread leaks after timeout.

### `TracingExecutionPipelineTest`
- [ ] Trace consumer receives one call per `execute()`.
- [ ] Consumer receives SUCCEEDED on success, FAILED on exception.
- [ ] Exception is re-thrown after consumer is notified.

### Integration
- [ ] Full composition chain (`Retrying → Timed → Tracing → Default`) executes a flow correctly.
- [ ] `FlowExecutor` with `DefaultExecutionPipeline.INSTANCE` produces identical behavior as before Phase 10.

---

## Exit Criteria

- All four pipeline implementations exist and are independently composable.
- No implementation modifies `FlowExecutor`, `ExecutionPipeline`, or `DefaultExecutionPipeline`.
- Composition documentation in `src/main/java/core/executor/README.md` shows a concrete example chain.
- All examples pass.

---

## What NOT to Do

- Do not add a `CompositeExecutionPipeline` with a `List<ExecutionPipeline>` — use decoration.
- Do not add retry or timeout logic to `FlowExecutor` — that is exactly what this phase prevents.
- Do not add shared state between pipeline instances — each execution chain is stateless (configuration only).
- Do not implement all four in one PR — each is a separate, reviewable addition.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
