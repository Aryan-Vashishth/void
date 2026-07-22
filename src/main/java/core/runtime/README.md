# `core.runtime` — Framework Entry Point

The VOID facade -- primary entry point for starting and managing automation sessions.

---

## Overview

`VOID` is the main facade class. `VOIDBuilder` (returned by `VOID.builder()`) is the
entry point for creating sessions. The builder collects configuration before anything
is created; `start()` is the terminal operation that initializes the engine and returns
a ready-to-use `VOID` instance.

---

## Class Inventory

| Class | Responsibility |
|-------|----------------|
| `VOID` | Session facade -- navigation, flow execution, shutdown |
| `VOIDBuilder` | Fluent builder for session configuration; single-use per session |

---

## Startup Pipeline

```
VOID.builder().profile(profile).start()
  → FrameworkBootstrap.init()          (one-time: validate configs, seed utils)
  → UIEngineFactory.create()           (engine selected first; driver deferred)
       └─ SeleniumEngine.initialize()  (creates + registers WebDriver internally)
  → SessionContext                     (holds config + engine for this session)
  → return VOID facade                 (thin wrapper, delegates to engine)
```

Engine selection happens before driver creation. Setting `engine=playwright` will
not open a Chrome window.

---

## Usage

### Start a Session

```java
// Default profile -- engine resolved from config / ENV / System property
VOID app = VOID.builder().start();

// Explicit driver profile
VOID app = VOID.builder()
        .profile(DriverFactory.Profile.DEFAULT)
        .start();

// Explicit engine override (prefer the typed constant)
VOID app = VOID.builder()
        .engine(SeleniumEngine.ID)
        .profile(DriverFactory.Profile.DEFAULT)
        .start();
```

### Modern Path: Action / Flow

```java
VOID app = VOID.builder().start();

app.navigateTo("https://the-internet.herokuapp.com/login");

app.run(Flow.of(
    LoginPage.USERNAME.type("admin@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));

app.shutdown();
```

### Multiple Independent Sessions

Each builder call produces an independent session. There is no shared static state
between instances.

```java
VOID admin    = VOID.builder().profile(DriverFactory.Profile.DEFAULT).start();
VOID customer = VOID.builder().profile(DriverFactory.Profile.DEFAULT).start();

admin.navigateTo(ADMIN_URL);
admin.run(adminLoginFlow);

customer.navigateTo(APP_URL);
customer.run(customerLoginFlow);

admin.shutdown();    // does NOT affect the customer session
customer.shutdown();
```

### Shutdown

```java
app.shutdown();  // engine quits the driver and cleans up its registry entry
```

---

## API Reference

| Method | Returns | Description |
|--------|---------|-------------|
| `VOID.builder()` | `VOIDBuilder` | Create a new session builder |
| `VOIDBuilder.profile(Profile)` | `VOIDBuilder` | Set the driver configuration profile |
| `VOIDBuilder.engine(String)` | `VOIDBuilder` | Override engine selection for this session |
| `VOIDBuilder.start()` | `VOID` | Build and start the session (single-use) |
| `VOID.start()` | `VOID` | *Deprecated since 0.3* -- delegates to `builder().start()` |
| `VOID.start(Profile)` | `VOID` | *Deprecated since 0.3* -- delegates to `builder().profile(p).start()` |
| `getEngine()` | `UIEngine` | Access the active execution engine |
| `run(Flow)` | `void` | Execute a flow |
| `run(Action)` | `void` | Execute a single action |
| `navigateTo(String)` | `void` | Navigate to a URL |
| `getCurrentUrl()` | `String` | Current page URL |
| `getTitle()` | `String` | Current page title |
| `refresh()` | `void` | Reload the current page |
| `shutdown()` | `void` | Quit the engine and end the session |

### Protected (for subclasses)

| Method | Returns | Description |
|--------|---------|-------------|
| `getContext()` | `SessionContext` | Access config + engine without re-fetching |
| `getDriver()` | `WebDriver` | *Deprecated* -- Selenium-specific escape hatch; use `getEngine().getNativeDriver()` |

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
    protected MyAppVoid(SessionContext ctx, UIEngine engine) {
        super(ctx, engine);
    }

    public static MyAppVoid start() {
        // Use the builder so the startup pipeline is correct
        VOID base = VOID.builder().start();
        return new MyAppVoid(base.getContext(), base.getEngine());
    }

    public MyCustomDSL dsl() {
        return new MyCustomDSL(getContext());
    }
}
```

---

## See Also

- `core.bootstrap.FrameworkBootstrap` -- the init gate called during startup
- `core.engine.UIEngine` -- the execution engine contract
- `core.engine.UIEngineFactory` -- creates engines from config
- `core.engine.EngineBootstrap` -- initialization token passed to the factory
- `core.engine.selenium.SeleniumEngine` -- Selenium engine implementation
- `core.context.SessionContext` -- per-session context (config + engine)
- `core.context.ExecutionContext` -- *deprecated since 0.2*; replaced by `SessionContext`
- `core.interactions.Interactions` -- legacy orchestrator (frozen)
