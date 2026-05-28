# `core.actions` — Deferred Execution Model

The foundation of VOID's primary execution pipeline: **Action / Flow / FlowExecutor**.

---

## Overview

Actions represent **deferred UI operations** (intent). They are produced by element capability interfaces and executed later by a `FlowExecutor`. Both locator resolution and browser execution are deferred until `perform(engine)` is called.

```
Element (capability interface)
  → Action (deferred intent)
    → Flow (composition)
      → FlowExecutor (iteration)
        → UIEngine (physical execution)
```

---

## Class Inventory

| Class | Stability | Responsibility |
|-------|-----------|----------------|
| `Action` | @Beta | Functional interface — a single deferred UI operation. The core building block of the execution pipeline. |
| `ElementActions` | @Internal | Factory that creates element-bound Actions supporting descriptor resolution. Used internally by capability interfaces. |
| `HookedAction` | @Beta | Decorator that wraps an Action with before/after `ActionHandler` hooks. |

---

## How It Works

### 1. Action Creation

Capability interfaces (e.g., `Clickable`, `Typeable`) emit `Action` objects when you call their methods:

```java
Action clickAction = LoginPage.SUBMIT.click();       // returns Action (deferred)
Action typeAction  = LoginPage.USERNAME.type("admin"); // returns Action (deferred)
```

Nothing happens yet — no browser interaction, no locator resolution.

### 2. Hook Composition (Optional)

Actions support fluent before/after hook attachment:

```java
Action hooked = LoginPage.USERNAME.type("admin")
    .withHooks(
        List.of(Before.WAIT_FOR_ELEMENT_VISIBLE),
        List.of(After.HIGHLIGHT_ELEMENT)
    );
```

This wraps the original action in a `HookedAction` decorator.

### 3. Execution

When `FlowExecutor` calls `action.perform(engine)`:
1. The locator is resolved (via `engine.resolve(element, role)`)
2. Before hooks execute (if attached)
3. The browser action executes (via `UIEngine`)
4. After hooks execute (if attached)

---

## Design Rules

| Rule | Rationale |
|------|-----------|
| Actions are **deferred** | Locator resolution happens inside `perform()`, never eagerly |
| Actions never reference `WebDriver`/`WebElement`/`By` | Engine-agnostic by design |
| Hook composition is optional and fluent | Clean separation of cross-cutting concerns |
| `ElementActions` is internal | Only capability interfaces should create element-bound actions |

---

## Usage Examples

### Simple Action Execution

```java
FlowExecutor executor = new FlowExecutor(engine);
executor.run(LoginPage.SUBMIT.click());
```

### Action with Hooks

```java
Action action = LoginPage.EMAIL.type("user@example.com")
    .withHooks(
        List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
        List.of(After.HIGHLIGHT_ELEMENT)
    );
executor.run(action);
```

### Inside Capability Interfaces (Internal)

```java
// How capability interfaces emit actions (framework code):
default Action type(String text) {
    return ElementActions.of(this, ElementRole.INPUT, (engine, descriptor) -> {
        engine.type(descriptor, text);
    });
}
```

---

## HookedAction Execution Order

```
┌─────────────────────────────────────┐
│  Before hooks (in list order)       │
│    → WAIT_FOR_ELEMENT_VISIBLE       │
│    → HIGHLIGHT_ELEMENT              │
├─────────────────────────────────────┤
│  Delegate Action (perform)          │
│    → engine.type(descriptor, text)  │
├─────────────────────────────────────┤
│  After hooks (in list order)        │
│    → HIGHLIGHT_ELEMENT              │
│    → LOG_INTENT                     │
└─────────────────────────────────────┘
```

**Failure behavior:**
- If a before hook throws → the action is **not** executed.
- If an after hook throws → it propagates (caller decides recovery).

---

## Stability

This package is **@Beta** — the API may change between releases. Do not depend on it from stable modules.

External consumers interact with Actions **opaquely** — they pass them to `Flow.of(...)` and `FlowExecutor.run(...)` without needing to understand the internal mechanics.

---

## See Also

- `core.flow` — composes Actions into sequences
- `core.executor` — iterates and executes flows
- `core.engine.UIEngine` — performs the actual browser interaction
- `core.interactions.hooks` — ActionHandler, Before, After hook constants

