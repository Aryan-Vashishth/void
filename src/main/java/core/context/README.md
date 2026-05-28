# `core.context` — Per-Session Execution Context

Immutable, explicitly-passed context objects replacing hidden global state.

---

## Overview

This package provides session-scoped context holders that bind together a session's configuration and execution resources. By passing context explicitly through the call chain, VOID eliminates global mutable singletons and enables safe parallel execution.

---

## Class Inventory

| Class | Engine Coupling | Status | Purpose |
|-------|----------------|--------|---------|
| `ExecutionContext` | `WebDriver` (Selenium) | Legacy/Internal | Holds config + raw WebDriver for a session |
| `SessionContext` | `UIEngine` (agnostic) | Modern/Preferred | Holds config + UIEngine for a session |

---

## `ExecutionContext`

Immutable holder for the resolved configuration and active WebDriver instance.

```java
ExecutionContext ctx = new ExecutionContext(config, driver);
ctx.getDriver().get("https://example.com");
String val = ctx.getConfig("some.key");
String valWithDefault = ctx.getConfig("some.key", "fallback");
```

**Used by:** `VOID` façade, legacy `Interactions`, internal framework code.

---

## `SessionContext`

Engine-agnostic replacement for `ExecutionContext`. This is the **preferred** context type for new code.

```java
SessionContext ctx = new SessionContext(config, engine);
ctx.engine().navigateTo("https://example.com");
String val = ctx.getConfig("some.key");
String engineName = ctx.getEngineName();  // "selenium", "playwright", etc.
```

**Why it exists:**
- `ExecutionContext` is Selenium-coupled (holds raw `WebDriver`)
- `SessionContext` holds a `UIEngine`, enabling Playwright or any future engine
- Same explicit-passing design, but engine-agnostic

---

## Design Benefits

| Benefit | How |
|---------|-----|
| No global mutable singletons | Each thread/test gets its own context instance |
| Safe parallel execution | No shared state between sessions |
| Visible dependencies | All requirements stated at construction time |
| Testable | Easy to mock or stub for unit tests |

---

## Construction

Both context classes validate their arguments at construction:

```java
// Throws NullPointerException if either arg is null
new ExecutionContext(null, driver);  // ❌ "config must not be null"
new SessionContext(config, null);   // ❌ "engine must not be null"
```

---

## Typical Lifecycle

```
VOID.start()
  → FrameworkBootstrap.init()        → loads config
  → DriverManager.createDriver()     → creates driver
  → new ExecutionContext(config, driver)
  → UIEngineFactory.create()         → creates engine
  → Context passed through the session
```

---

## See Also

- `core.runtime.VOID` — creates and holds the context
- `core.engine.UIEngine` — the engine abstraction used by `SessionContext`
- `core.bootstrap.FrameworkBootstrap` — produces the configuration

