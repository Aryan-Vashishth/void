# Phase 4 — Hook Strategy Layer

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Medium — new abstraction layer, no API removal

---

## Objective

Stop hook list sprawl. Prevent the framework from becoming hook-oriented instead of action-oriented. Give developers named strategies instead of hook argument lists.

---

## Context

The current low-level hook library already has:

```java
Before.WAIT_FOR_ANGULAR_LOADER
Before.WAIT_FOR_SPIN_SPINNER_LOADER
Before.WAIT_FOR_ELEMENT_CLICKABLE
Before.WAIT_FOR_ELEMENT_VISIBLE
Before.CLEAR_FIELD
Before.SCROLL_TO_ELEMENT
Before.HIGHLIGHT_ELEMENT

After.WAIT_FOR_ANGULAR_LOADER
After.WAIT_FOR_SPIN_SPINNER_LOADER
After.WAIT_FOR_ELEMENT_VISIBLE
After.HIGHLIGHT_ELEMENT
After.SCROLL_TO_ELEMENT
```

As the framework grows, this will expand to 30-40 hook constants. Developers will start combining them manually:

```java
// This is what we want to prevent
LOGIN.click()
    .withHooks(
        List.of(
            Before.WAIT_FOR_ANGULAR_LOADER,
            Before.WAIT_FOR_ELEMENT_CLICKABLE,
            Before.SCROLL_TO_ELEMENT,
            Before.HIGHLIGHT_ELEMENT
        ),
        List.of(
            After.WAIT_FOR_ANGULAR_LOADER,
            After.HIGHLIGHT_ELEMENT
        )
    );
```

When instead the user should write:

```java
// This is what we want
LOGIN.click().using(Profiles.SAFE);
```

---

## Design

### `HookStrategy` (new interface)

```java
package core.actions.strategy;

import core.interactions.hooks.ActionHandler;
import java.util.List;

/**
 * A named bundle of before/after hooks that represents a reusable execution strategy.
 */
public interface HookStrategy {
    String name();
    List<ActionHandler> before();
    List<ActionHandler> after();
}
```

### Built-in Strategies

#### `SafeClickStrategy`

```java
before: [WAIT_FOR_ELEMENT_CLICKABLE]
after:  [WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]
```

#### `SafeTypeStrategy`

```java
before: [CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE]
after:  [HIGHLIGHT_ELEMENT]
```

#### `DebugStrategy`

```java
before: [LOG_INTENT, HIGHLIGHT_ELEMENT]
after:  [HIGHLIGHT_ELEMENT]
```

#### `FastStrategy`

```java
before: []
after:  []
```

---

## How Profiles Route to Strategies

```java
// Inside safely() — capability-aware routing
default Action safely() {
    HookStrategy strategy = HookStrategyResolver.forCapability(this);
    return withHooks(strategy.before(), strategy.after());
}
```

```java
// HookStrategyResolver
public class HookStrategyResolver {
    public static HookStrategy forCapability(Element element) {
        if (element instanceof ActionCapabilityProvider p) {
            return switch (p.capability()) {
                case CLICKABLE  -> new SafeClickStrategy();
                case TYPEABLE   -> new SafeTypeStrategy();
                default         -> new FastStrategy(); // no-op fallback
            };
        }
        return new FastStrategy();
    }
}
```

---

## Affected Files

New:
- `src/main/java/core/actions/strategy/HookStrategy.java`
- `src/main/java/core/actions/strategy/SafeClickStrategy.java`
- `src/main/java/core/actions/strategy/SafeTypeStrategy.java`
- `src/main/java/core/actions/strategy/DebugStrategy.java`
- `src/main/java/core/actions/strategy/FastStrategy.java`
- `src/main/java/core/actions/strategy/HookStrategyResolver.java`

Modified:
- `src/main/java/core/actions/Action.java` — route `safely()` and `debug()` through `HookStrategyResolver`

---

## Checklist

### Strategy Infrastructure
- [ ] Create `HookStrategy` interface.
- [ ] Create `SafeClickStrategy` with correct before/after hook list.
- [ ] Create `SafeTypeStrategy` with correct before/after hook list.
- [ ] Create `DebugStrategy` with correct before/after hook list.
- [ ] Create `FastStrategy` as no-op.
- [ ] Create `HookStrategyResolver` that maps capability to strategy.

### Routing
- [ ] Update `safely()` in `Action` to use `HookStrategyResolver`.
- [ ] Update `debug()` in `Action` to always use `DebugStrategy`.
- [ ] Ensure `raw()` bypasses all strategy resolution.
- [ ] Ensure `withHooks(List, List)` bypasses strategy resolution entirely.

### Tests
- [ ] Unit test: `SafeClickStrategy` returns the correct hook lists.
- [ ] Unit test: `SafeTypeStrategy` returns the correct hook lists.
- [ ] Unit test: `HookStrategyResolver.forCapability()` returns correct strategy per capability.
- [ ] Unit test: `safely()` on a `Clickable` action applies `SafeClickStrategy`.
- [ ] Unit test: `safely()` on a `Typeable` action applies `SafeTypeStrategy`.
- [ ] Unit test: `debug()` always applies `DebugStrategy` regardless of capability.
- [ ] Unit test: `raw()` produces zero hooks regardless of capability.

### Documentation
- [ ] Update `docs/architecture/hooks-pipeline.md` with strategy layer diagram.
- [ ] Add strategy layer to `src/main/java/core/actions/README.md`.

---

## Exit Criteria

- `safely()`, `debug()`, and `raw()` are backed by strategy implementations.
- Users do not need to know individual hook constants for standard use cases.
- `withHooks(List, List)` remains fully functional for advanced users.

---

## What NOT to Do

- Do not remove `Before.*` or `After.*` constants — they are still needed for custom profiles.
- Do not make strategies configurable via external config in this phase.
- Do not add strategies for every possible hook combination — start with the four above.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

