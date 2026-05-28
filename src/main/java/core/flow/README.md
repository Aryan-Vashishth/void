# `core.flow` — Declarative Action Composition

Groups multiple Actions into immutable, reusable workflow sequences.

---

## Overview

A `Flow` is an immutable, ordered list of `Action` objects that represents a complete UI workflow. Flows are pure data — they describe **what** to do, not **how** to do it. Creating a Flow triggers no browser interaction; execution is always deferred until a `FlowExecutor` runs it.

---

## Class Inventory

| Class | Stability | Responsibility |
|-------|-----------|----------------|
| `Flow` | @Beta | Immutable action sequence with static factory |

---

## Usage

### Create a Flow

```java
Flow loginFlow = Flow.of(
    LoginPage.USERNAME.type("admin@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
);
```

### Execute a Flow

```java
FlowExecutor executor = new FlowExecutor(engine);
executor.run(loginFlow);
```

### Reuse a Flow

```java
// Same flow can be executed multiple times
executor.run(loginFlow);  // first run
executor.run(loginFlow);  // second run — safe because Flow is immutable
```

### Compose with Hooks

```java
Flow secureLogin = Flow.of(
    LoginPage.USERNAME.type("admin")
        .withHooks(List.of(Before.CLEAR_FIELD), null),
    LoginPage.PASSWORD.type("secret")
        .withHooks(List.of(Before.CLEAR_FIELD), null),
    LoginPage.SUBMIT.click()
        .withHooks(List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE), null)
);
```

---

## Design Philosophy

| Principle | Description |
|-----------|-------------|
| **Pure data** | Flows describe intent, not execution mechanics |
| **Immutable** | Once created, a Flow never changes |
| **Reusable** | Execute the same Flow instance many times |
| **Composable** | Build flows from actions, optionally with hooks |
| **Deferred** | Creating a Flow does NOT trigger browser interaction |

---

## API Reference

| Method | Description |
|--------|-------------|
| `Flow.of(Action... actions)` | Static factory — creates an immutable Flow |
| `flow.getActions()` | Returns the ordered, unmodifiable list of Actions |

---

## How Flows Fit in the Pipeline

```
Element.click() / .type("text")     → Action (deferred)
    ↓
Flow.of(action1, action2, action3)  → Flow (composition)
    ↓
executor.run(flow)                  → FlowExecutor (iteration)
    ↓
action.perform(engine)              → UIEngine (execution)
```

---

## Stability

**@Beta** — this API may change without notice between releases.

---

## See Also

- `core.actions.Action` — the building blocks that flows contain
- `core.executor.FlowExecutor` — executes flows
- `core.engine.UIEngine` — where physical execution happens
- ADR-009: Action / Flow / FlowExecutor Execution Model

