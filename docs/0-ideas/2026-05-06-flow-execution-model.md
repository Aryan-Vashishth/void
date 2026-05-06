Doc-ID: IDEA-2026-05-06-void-execution-model-context-run
Status: DRAFT
State: 0-IDEAS
Owner: Void
Created: 2026-05-06
Updated: 2026-05-06
Related: N/A
Original-Path: N/A

# VOID Execution Model - Draft Plan

## Goal

Introduce context-driven execution (`run(() -> {})`) without breaking:

- existing flows
- executor pipeline
- action model

## Phase 0 - Baseline Freeze (Do this first)

Goal: Don't build on shaky ground.

- Freeze current behavior
- Ensure all tests pass
- Tag a version (e.g., `v0-flow-stable`)

This is your rollback point when things get weird (they will).

## Phase 1 - Introduce `FlowContext` (No behavior change yet)

Add:

```java
class FlowContext {
    private static ThreadLocal<List<Action>> actions = new ThreadLocal<>();

    static void start() {
        actions.set(new ArrayList<>());
    }

    static List<Action> end() {
        List<Action> collected = actions.get();
        actions.remove();
        return collected;
    }

    static void add(Action action) {
        actions.get().add(action);
    }

    static boolean isActive() {
        return actions.get() != null;
    }
}
```

Validation:

- Nothing should break
- Not used anywhere yet

## Phase 2 - Add `run(() -> {})` entry point

Add:

```java
public void run(Runnable block) {
    FlowContext.start();

    try {
        block.run();
        List<Action> actions = FlowContext.end();
        executor.run(Flow.of(actions));
    } catch (Exception e) {
        FlowContext.end(); // cleanup
        throw e;
    }
}
```

Validation:

- Still unused
- No regression

## Phase 3 - Update Action Methods (Core shift)

Change behavior of:

- `.type()`
- `.click()`
- etc.

Pattern:

```java
public Action type(String text) {
    Action action = new TypeAction(...);

    if (FlowContext.isActive()) {
        FlowContext.add(action);
        return action; // or return this if fluent
    }

    return action;
}
```

Rule:

- DO NOT execute action here
- Only create + optionally register

Validation:

- Existing code using `Flow.of(...)` must still work
- No change in execution behavior

## Phase 4 - First Usage (Controlled Test)

Convert one test only:

```java
run(() -> {
    USERNAME_INPUT.type("tomsmith");
    PASSWORD_INPUT.type("SuperSecretPassword!");
    LOGIN_BUTTON.click();
});
```

Validate deeply:

- execution order
- waits
- retries
- logging
- failures

This phase is where hidden issues show up.

## Phase 5 - Add Safety Guards

1. Prevent nested contexts (optional but smart)

```java
if (FlowContext.isActive()) {
    throw new IllegalStateException("Nested run() not allowed");
}
```

2. Empty flow protection

```java
if (actions.isEmpty()) {
    throw new IllegalStateException("No actions inside run()");
}
```

3. Debug logging (temporary)

- Log collected actions before execution

## Phase 6 - Optional Enhancement (Power move)

Allow capturing flow without executing:

```java
public Flow flow(Runnable block) {
    FlowContext.start();
    block.run();
    return Flow.of(FlowContext.end());
}
```

Now you get:

```java
Flow login = flow(() -> {
    USERNAME_INPUT.type("tomsmith");
    PASSWORD_INPUT.type("pass");
});
```

This is where your system becomes composable + clean.

## Phase 7 - Gradual Migration

Don't mass refactor.
Replace usage only when touching tests.

Pattern:

| Old | New |
| --- | --- |
| `executor.run(Flow.of(...))` | `run(() -> {...})` |

## What NOT to do

- Don't remove `Flow.of`
  - You'll need it for reusable flows and composition.
- Don't execute inside actions.
  - That kills your architecture.
- Don't over-engineer early.
  - No annotations, no magic proxies, no DSL madness (yet).

## Phase 8 - Stress Testing

Test edge cases:

- failure mid-flow
- retry scenarios
- parallel execution (if applicable)
- thread isolation

## Final State

You end up with:

- Clean test syntax

```java
run(() -> {
    login();
    addItemToCart();
    checkout();
});
```

- Reusable flows

```java
Flow checkoutFlow = flow(() -> {
    addItem();
    pay();
});
```

- Same engine underneath

```java
executor.run(flow);
```

## Final Insight

This change is small in code, but big in effect.

You're introducing execution context as a first-class concept.

That's the difference between:

- a tool
- and a system

