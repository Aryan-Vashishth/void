# ADR-018 -- Engine Lifecycle Ownership

**Date:** 2026-07-20
**Status:** Pending review (`feature/engine-decoupling`)

---

## Context

### Inverted startup dependency

`VOID.start(Profile)` unconditionally created a Selenium `WebDriver` before reading the
configured engine:

```java
// VOID.java (pre-decoupling)
WebDriver driver = DriverManager.createDriver(profile);          // always Selenium
ExecutionContext ctx = new ExecutionContext(config, driver);      // WebDriver locked in
UIEngine engine = UIEngineFactory.create(config, driver);        // factory receives pre-built driver
```

`UIEngineFactory.create()` accepted a `WebDriver` parameter -- making it structurally
impossible to register a non-Selenium engine. Any `engine=playwright` configuration reached
the `default -> throw` branch of the factory switch only after a Chrome window had already
opened at step one. The startup sequence contradicted the framework's claim to support
multiple engine implementations.

### Selenium-specific context type

`VOID` held an `ExecutionContext` (a class that carries a raw `WebDriver`). `SessionContext`
(engine-typed, already written) existed but was dead code. Every `VOID` instance was
implicitly Selenium.

### Ownership scattered across startup

Shutdown was split: `SeleniumEngine.shutdown()` called `driver.quit()` but `VOID.shutdown()`
separately called `DriverContext.removePrimary()`. The engine did not fully own its own
teardown.

---

## Decision

**Each `UIEngine` implementation owns creation, lifecycle, and shutdown of its native
automation runtime.**

### UIEngineFactory contract

`UIEngineFactory.create()` no longer accepts a `WebDriver`. It accepts an `EngineBootstrap`
(a sealed migration abstraction, not a permanent API) that carries initialization data for
the selected engine -- in practice a `DriverFactory.Profile`.

```java
// After
public static UIEngine create(Properties config, EngineBootstrap bootstrap)
```

`WebDriver` is not imported by `UIEngineFactory.java`.

### SeleniumEngine initializes its own driver

`SeleniumEngine` gains a primary constructor that takes a `DriverFactory.Profile`. Its
`initialize()` method creates the `WebDriver` internally and registers it in `DriverContext`.
The previous `SeleniumEngine(WebDriver)` constructor is kept as a `@Deprecated(forRemoval=true)`
compatibility path for the frozen `Interactions(WebDriver)` deprecated constructor.

### VOIDBuilder replaces VOID.start(Profile)

`VOID.builder()` returns a `VOIDBuilder` that collects runtime configuration before anything
is created. `VOIDBuilder.start()` is the terminal operation:

```java
// New entry point
VOID session = VOID.builder()
        .engine(SeleniumEngine.ID)  // optional; resolved from config/env if omitted
        .profile(CHROME)
        .start();
```

`VOIDBuilder` is single-use. Calling `start()` twice throws `IllegalStateException`. A new
`VOID.builder()` call is required for each independent session.

`VOID.start(Profile)` is deprecated (`@Deprecated(since = "0.3", forRemoval = true)`) and
delegates to the builder. It is not removed in this phase.

### SessionContext replaces ExecutionContext in VOID

`VOID` replaces its `ExecutionContext` field with `SessionContext`. `SessionContext` holds
a `UIEngine`, not a `WebDriver`. `VOID.java` no longer imports or constructs `ExecutionContext`.
`ExecutionContext` is marked `@Deprecated(since = "0.2")` and retained for external callers
until a separate removal workstream runs.

### SeleniumEngine owns its shutdown

`SeleniumEngine.shutdown()` calls `driver.quit()` and `DriverContext.removePrimary()` in a
`finally` block. `VOID.shutdown()` delegates entirely to `engine.shutdown()` and no longer
imports `DriverContext`.

---

## Scope constraints

### EngineBootstrap is a migration abstraction

`EngineBootstrap` exists to keep Phase 1 and Phase 2 independently compilable while
preventing duplicate driver creation during the transition. It is not a permanent
cross-engine API. After Phase 2, it contains only `FromProfile`; `FromDriver` was deleted
in the same phase. Future engine additions should revisit the bootstrap mechanism based on
their actual initialization requirements.

### DriverManager.createDriver() is unchanged

`DriverManager.createDriver()` still exists for callers who need it directly. Its
deprecation is deferred to a future workstream once no internal callers remain. It is not
called from `VOIDBuilder.start()`.

### resolvedConfig() carries a known design debt

`VOIDBuilder.resolvedConfig()` injects the engine name into a `Properties` copy to
communicate with the factory. This conflates runtime selection with framework configuration.
The cleaner design carries the engine name inside `EngineBootstrap`. Deferred post-decoupling;
does not affect correctness.

---

## Reasoning

### Why not keep the factory accepting WebDriver?

Every caller would need to know which driver type to produce before the engine is selected.
A Playwright engine has no `WebDriver`. The parameter type is a leaking assumption about
what every future engine will look like.

### Why a fluent builder rather than VOID.start(profile, engineName)?

Additional session configuration parameters (timeouts, tags, capabilities) would require
additional overloads. A builder grows with requirements without touching existing call sites.
The single-use guard is also natural in a builder -- impossible to express cleanly in a
static factory method.

### Why a single-use guard?

A `VOIDBuilder` instance configures one session. Reusing the same builder for a second
session with different parameters would be ambiguous about which calls applied to which
session. The guard makes the invariant explicit and surfaces misuse immediately.

---

## Consequences

- `VOIDBuilder.java` created in `core.runtime`
- `VOID.builder()` is the public session entry point
- `VOID.start(Profile)` deprecated; delegates to builder
- `UIEngineFactory.create(Properties, WebDriver)` signature removed
- `EngineBootstrap.java` introduced; `FromDriver` variant deleted in Phase 2
- `ExecutionContext` deprecated; `SessionContext` is the active context type in `VOID`
- `VOID.java` imports neither `WebDriver` nor `DriverContext`
- `SeleniumEngine.shutdown()` owns full Selenium cleanup
- Multiple independent `VOID` instances can coexist per JVM without new global state

---

## Related

- [ADR-011 -- VOID as Primary Session Facade](../accepted/011-void-facade-boundary.md)
- [ADR-007 -- UIEngine as Single Execution Authority](../accepted/007-uiengine-execution-authority.md)
- [ADR-019 -- SeleniumLocatorBridge Isolation](019-selenium-locator-bridge.md)
- [Engine Decoupling plan index](../../plan/done/engine-decoupling/index.md)
