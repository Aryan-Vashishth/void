# `core.interactions` — Legacy Interaction Orchestrator

The original UI action layer — **frozen, deprecated, no new features**.

---

## Overview

`Interactions` is the legacy orchestrator that provides every low-level Selenium action (click, type, search, dropdown, etc.) driven by typed element enums and role-based locator resolution. It is preserved for backward compatibility with existing step definitions and page objects.

> ⚠️ **New code should use the Action/Flow/FlowExecutor pipeline instead.**

---

## Class Inventory

| Class | Status | Responsibility |
|-------|--------|----------------|
| `Interactions` | Frozen/Deprecated | Full-featured action orchestrator (click, type, select, search, etc.) |
| `Via` | Stable | Static cast helpers and locator resolution utilities |

### Sub-Package: `core.interactions.hooks`

| Class | Responsibility |
|-------|----------------|
| `ActionHandler` | Functional interface: `(UIEngine, LocatorDescriptor) → void` |
| `Before` | Pre-built before-hook constants |
| `After` | Pre-built after-hook constants |

---

## `Interactions`

Provides imperative action methods that delegate to `UIEngine`:

```java
VOID app = VOID.start();

// Click
app.interaction().clickOn(MyPage.SUBMIT_BUTTON);

// Type
app.interaction().typeInto(MyPage.EMAIL_FIELD, "user@example.com");

// Select dropdown
app.interaction().selectFrom(MyPage.COUNTRY_DROPDOWN, "Australia");

// Search
app.interaction().searchAndSelect(MyPage.USER_SEARCH, "alice");
```

**Design:** Acts as an orchestrator over `UIEngine` — does NOT perform waits/scroll itself.

---

## `Via` — Static Helpers

Provides cast helpers and locator resolution:

### Interface Casting

```java
Clickable btn  = Via.clickable(MyPage.SAVE_BUTTON);
Typeable  tf   = Via.typeable(MyPage.EMAIL_FIELD);
Selectable ddl = Via.selectable(MyPage.STATUS_DROPDOWN);
```

### Locator Resolution (Preferred — Engine-Agnostic)

```java
LocatorDescriptor d = Via.descriptor(element);
LocatorDescriptor d = Via.descriptor(element, ElementRole.LIST, "Active");
LocatorDescriptor d = Via.descriptor("common-elements.json", "searchInput");
```

### Type-Check Predicates

```java
if (Via.isClickable(element)) { ... }
if (Via.isTypeable(element))  { ... }
```

---

## `core.interactions.hooks` — Hook Pipeline

### `ActionHandler` (Functional Interface)

```java
@FunctionalInterface
public interface ActionHandler {
    void execute(UIEngine engine, LocatorDescriptor descriptor);
}
```

### `Before` — Pre-Action Hooks

| Constant | Description |
|----------|-------------|
| `WAIT_FOR_ELEMENT_VISIBLE` | Waits until the element is visible |
| `WAIT_FOR_ELEMENT_CLICKABLE` | Waits until the element is clickable |
| `HIGHLIGHT_ELEMENT` | Highlights the element (visual debugging) |
| `WAIT_FOR_ANGULAR_LOADER` | Waits for Angular CDK overlays to disappear |
| `LOG_INTENT` | Logs the intended action |
| `DO_NOTHING` | No-op placeholder |

### `After` — Post-Action Hooks

| Constant | Description |
|----------|-------------|
| `HIGHLIGHT_ELEMENT` | Highlights after action |
| `LOG_INTENT` | Logs completion |
| `DO_NOTHING` | No-op placeholder |

### Hook Ordering Guarantee

```
1. Before hooks execute (in list order)
2. The action executes
3. After hooks execute (in list order)
```

### Hook Usage

Hooks receive the element's `LocatorDescriptor` directly — no global state dependency:

```java
// With Action/Flow pipeline
LoginPage.USERNAME.type("admin")
    .withHooks(
        List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.HIGHLIGHT_ELEMENT),
        List.of(After.HIGHLIGHT_ELEMENT)
    );
```

---

## Migration Guide

| Legacy (Interactions) | Modern (Action/Flow/FlowExecutor) |
|-----------------------|-----------------------------------|
| `app.interaction().clickOn(el)` | `executor.run(el.click())` |
| `app.interaction().typeInto(el, text)` | `executor.run(el.type(text))` |
| `app.interaction().selectFrom(el, val)` | `executor.run(el.select(val))` |
| Imperative, one-at-a-time | Declarative, composable Flows |

---

## Design Notes

- **No BDD/Cucumber dependencies** — pure framework layer
- `Interactions` is **frozen** — no new features will be added
- Hooks are **stable** — they serve both legacy and modern paths
- `Via` remains useful for type casting and descriptor resolution

---

## See Also

- `core.actions` — modern replacement (deferred execution)
- `core.flow` / `core.executor` — modern composition and execution
- `core.engine.UIEngine` — what Interactions delegates to
- `dsl.VoidDSL` — BDD-specific DSL wrapper
- ADR-010: Hook Evolution

