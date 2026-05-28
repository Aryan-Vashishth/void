# `core.runtime` — Framework Entry Point

The VOID façade — primary entry point for starting and managing automation sessions.

---

## Overview

`VOID` is the main façade class that wires together all framework components. It orchestrates the full startup pipeline (bootstrap → driver creation → engine initialisation) and provides access to both the modern UIEngine and the legacy Interactions helper.

---

## Class Inventory

| Class | Responsibility |
|-------|----------------|
| `VOID` | Main façade — session lifecycle, engine access, interaction access |

---

## Startup Pipeline

```
VOID.start()
  → FrameworkBootstrap.init()          (one-time: validate configs, seed utils)
  → DriverManager.createDriver()       (create + register WebDriver)
  → UIEngineFactory.create()           (instantiate engine from config)
  → ExecutionContext                   (holds config + driver for this session)
  → return VOID façade                 (thin wrapper, delegates to context)
```

---

## Usage

### Start a Session

```java
// Default profile
VOID app = VOID.start();

// Custom driver profile
VOID app = VOID.start(DriverFactory.Profile.DEFAULT);
```

### Modern Path: Action / Flow / FlowExecutor

```java
VOID app = VOID.start();
UIEngine engine = app.getEngine();
FlowExecutor executor = new FlowExecutor(engine);

executor.run(Flow.of(
    LoginPage.USERNAME.type("admin@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));
```

### Legacy Path: Interactions

```java
VOID app = VOID.start();
app.interaction().clickOn(MyPage.SUBMIT_BUTTON);
app.interaction().typeInto(MyPage.EMAIL, "user@example.com");
app.interaction().selectFrom(MyPage.COUNTRY, "Australia");
```

### Shutdown

```java
app.shutdown();  // quits all drivers for the current thread
```

---

## API Reference

| Method | Returns | Description |
|--------|---------|-------------|
| `VOID.start()` | `VOID` | Start session with DEFAULT profile |
| `VOID.start(profile)` | `VOID` | Start session with specified profile |
| `getEngine()` | `UIEngine` | Access the active execution engine |
| `interaction()` | `Interactions` | Access the legacy interaction helper (cached) |
| `shutdown()` | `void` | Quit all drivers, end session |

### Protected (for subclasses)

| Method | Returns | Description |
|--------|---------|-------------|
| `getContext()` | `ExecutionContext` | Access config + driver without re-fetching |
| `getDriver()` | `WebDriver` | Convenience access to the active WebDriver |

---

## Layer Model

```
┌─────────────────────────────────────────────────────┐
│  DSL layer         (dsl.VoidDSL)                    │
│    → context-driven DSL for BDD steps               │
├─────────────────────────────────────────────────────┤
│  Framework layer   (core.runtime.VOID)              │
│    → UIEngine      (modern, preferred)              │
│    → Interactions  (legacy, frozen)                 │
└─────────────────────────────────────────────────────┘
```

---

## Extending VOID

`VOID` is designed to be subclassed for framework extensions:

```java
public class MyAppVoid extends VOID {
    protected MyAppVoid(ExecutionContext ctx, UIEngine engine) {
        super(ctx, engine);
    }
    
    public MyCustomDSL dsl() {
        return new MyCustomDSL(getContext());
    }
}
```

---

## See Also

- `core.bootstrap.FrameworkBootstrap` — the init gate called during startup
- `core.driver.DriverManager` — creates the WebDriver
- `core.engine.UIEngine` — the execution engine
- `core.engine.UIEngineFactory` — creates engines from config
- `core.context.ExecutionContext` — the session context
- `core.interactions.Interactions` — legacy orchestrator
- `dsl.VoidDSL` — BDD DSL layer (extends VOID)

