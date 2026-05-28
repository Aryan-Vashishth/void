# `core.executor` — Flow Execution Engine

The terminal component of the Action/Flow/FlowExecutor pipeline.

---

## Overview

`FlowExecutor` is intentionally simple — it iterates through a Flow's actions and calls `perform(engine)` on each. All smart execution logic (scroll, waits, retries, fallback) lives in `UIEngine`, not here.

---

## Class Inventory

| Class | Stability | Responsibility |
|-------|-----------|----------------|
| `FlowExecutor` | @Beta | Iterates Flow actions, delegates execution to UIEngine |

---

## Usage

### Execute a Flow

```java
UIEngine engine = app.getEngine();
FlowExecutor executor = new FlowExecutor(engine);

executor.run(Flow.of(
    LoginPage.USERNAME.type("admin"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));
```

### Execute a Single Action

```java
executor.run(DashboardPage.LOGOUT.click());
```

---

## How It Works

```
FlowExecutor.run(flow)
  │
  ├── for each Action in flow.getActions():
  │       action.perform(engine)
  │         │
  │         ├── (if HookedAction) → before hooks → delegate → after hooks
  │         └── (if plain Action) → engine.click/type/select/etc.
  │
  └── done (sequential, no parallelism)
```

---

## Design Philosophy

| Principle | Implementation |
|-----------|---------------|
| **"Dumb" executor** | Only iterates and delegates — no execution logic |
| **Engine owns execution** | All waits, scrolling, retries happen inside UIEngine |
| **Hook orchestration elsewhere** | `HookedAction` handles before/after — executor doesn't know about hooks |
| **Minimal responsibilities** | Keeps the execution boundary clean and testable |

---

## Construction

```java
// Executor is bound to an engine at construction time
FlowExecutor executor = new FlowExecutor(engine);
```

The executor is lightweight and stateless (beyond its engine reference). You can create one per test or share across a session.

---

## Stability

**@Beta** — this API may change without notice between releases.

---

## See Also

- `core.actions.Action` — what the executor calls `perform()` on
- `core.flow.Flow` — what the executor iterates over
- `core.engine.UIEngine` — where actual browser interaction happens
- `core.actions.HookedAction` — hook orchestration that's transparent to executor

