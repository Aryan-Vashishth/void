# ADR 010 — Hook Evolution: UIContext → Descriptor-Based Hooks

| Field       | Value                              |
|-------------|------------------------------------|
| **Status**  | Accepted                           |
| **Date**    | 2026-05-04                         |
| **Authors** | VOID Framework team                |

## Context

Action hooks (`Before`, `After`) in the VOID framework relied on
`UIContext.getLastLocatorDescriptor()` — a thread-local global that
stored the most recently resolved `LocatorDescriptor`. This created:

- **Hidden coupling** — hooks depended on implicit state set by
  `Interactions.resolveAndTrack()`.
- **Non-determinism** — if a hook ran before the descriptor was set
  (e.g. in a before-hook chain), it silently received `null`.
- **Debugging difficulty** — failures inside hooks pointed to
  `UIContext` rather than the actual call site.

## Decision

Evolve `ActionHandler` from a single-arg interface to a two-arg
interface that receives the `LocatorDescriptor` explicitly:

```java
@FunctionalInterface
public interface ActionHandler {
    void execute(UIEngine engine, LocatorDescriptor descriptor);
}
```

Introduce `HookedAction` as a decorator over `Action` that owns
descriptor resolution and passes it into hooks — removing the need for
global state in the hook pipeline.

### Key design rules

1. **One interface, evolved** — no second `ContextAwareActionHandler`.
2. **No `_V2` constants** — hooks replaced in-place (same names).
3. **`HookedAction` owns descriptor resolution** — not `Runner`, not hooks.
4. **`Runner` stays dumb** — it just calls `action.perform(engine)`.
5. **`Interactions` stays legacy** — passes `null` descriptor; no migration.

## Migration phases

| Phase | Description                               | Risk |
|-------|-------------------------------------------|------|
| 1     | Stabilize — add comments + logging guards | none |
| 2     | Evolve `ActionHandler` + create `HookedAction` | low |
| 3     | Migrate hook implementations in-place     | low  |
| 4     | Deprecate `UIContext` hook access          | none |
| 5     | Remove legacy adapter + clean up          | none |

## Consequences

- **Hooks are deterministic** — they receive the descriptor of the
  element being acted upon, not "whatever was last resolved."
- **No global state dependency** in the new pipeline.
- **Legacy code (`Interactions`)** continues to work with `null`
  descriptors; hooks log warnings and return early.
- **`ActionHandler.legacy()`** adapter exists for migration; will be
  removed in Phase 5.

## Hook ordering guarantee (documented)

1. Before hooks execute in list order.
2. The delegate action executes.
3. After hooks execute in list order.

## One-liner

> Move from "what happened last" → to "what is being executed now."

