# `core.annotations` — Stability Tier Markers

Communicates API stability guarantees to consumers through compile-time annotations.

---

## Overview

VOID uses explicit annotations to signal the stability level of every public type and method. This enables safe framework evolution while giving consumers clear expectations about what may change.

---

## Annotations

### `@Beta`

Marks a type or method as **evolving** — the API may change, be renamed, or removed in any release.

```java
@Beta(since = "2.0", note = "Action/Flow/FlowExecutor pipeline is evolving")
@FunctionalInterface
public interface Action { ... }
```

| Attribute | Default | Description |
|-----------|---------|-------------|
| `since` | `"2.0"` | Version when the API was introduced as beta |
| `note` | `"API may change without notice"` | Description of the beta contract |

**Rules:**
- Beta APIs must **not** be used inside stable modules
- Stable APIs may depend on stable APIs only
- Once graduated, the annotation is removed and normal backward-compat applies

---

### `@Internal`

Marks a type or method as **framework infrastructure** — exists only for plumbing.

```java
@Internal
public static Action wrap(Action delegate, Element element, ...) { ... }
```

**Rules:**
- Internal APIs may be changed, moved, or removed at any time
- External consumers must **not** depend on internal APIs
- Used for: migration bridges, helper classes, adapter layers

---

## Stability Tier Model

| Tier | Annotation | Guarantees | Examples |
|------|------------|------------|----------|
| Stable (frozen) | `@Deprecated` | No changes, no new features | `Interactions` |
| Stable (user-facing) | *(none)* | Backward-compatible evolution | `UIEngine`, `Clickable`, `Element` |
| Beta | `@Beta` | May change without notice | `Action`, `Flow`, `FlowExecutor` |
| Internal | `@Internal` | No guarantees | `ElementActions`, adapters |

---

## Usage Rules

1. **Beta APIs must not be used inside stable modules.**
2. **Stable APIs may depend on stable APIs only.**
3. **Internal APIs are not for external consumption.**
4. **Capability interfaces are stable contracts** — the Action objects they return are beta, but consumers pass them opaquely to `Flow.of(...)` / `FlowExecutor`.

---

## Retention & Targets

Both annotations use:
- `@Retention(RUNTIME)` — available for reflection-based tooling
- `@Target({TYPE, METHOD})` — applicable to classes, interfaces, enums, and methods

---

## See Also

- `core.actions` — primary consumer of `@Beta`
- `core.engine.UIEngine` — example of stable (un-annotated) API
- `core.interactions.Interactions` — example of stable-frozen (`@Deprecated`)

