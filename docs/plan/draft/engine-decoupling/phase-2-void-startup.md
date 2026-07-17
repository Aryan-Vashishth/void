# Phase 2 — VOID Startup Pipeline: Builder API, Invert, SessionContext

Violations: **V2**, **V3**, **V5**
Deletes: `EngineBootstrap.FromDriver` and `EngineBootstrap.fromDriver()` (compatibility bridge from Phase 1)
Deprecates: `ExecutionContext`, `VOID.start(Profile)`

---

## Goal

`VOID.start()` creates a `WebDriver` before selecting an engine. After this phase:
- `VOID.builder()` is the public startup entry point; `VOID.start(Profile)` is deprecated.
- Engine selection happens first; driver creation is a side effect of `engine.initialize()`.
- Configuring `engine=playwright` will not open a Chrome window.
- `VOID` holds no reference to `WebDriver` or `ExecutionContext`.
- `VOID.shutdown()` delegates entirely to the engine.
- Multiple independent `VOID` instances can coexist without new global state.

> **Invariant**: Every `VOID` instance owns exactly one `SessionContext`, one `UIEngine`,
> and one native automation runtime.

---

## V2 -- `VOID.start()` creates WebDriver before engine selection

### Problem

```java
// VOID.java:138-144
WebDriver driver = DriverManager.createDriver(profile);    // Step 1: Selenium always
ExecutionContext ctx = new ExecutionContext(               // Step 2: WebDriver locked in
        FrameworkBootstrap.getUtilsConfig(), driver);
UIEngine engine = UIEngineFactory.create(                 // Step 3: engine created too late
        FrameworkBootstrap.getUtilsConfig(), driver);
```

The engine has no say in driver creation. A `PlaywrightEngine` would need to swallow an
already-open Chrome window.

### Fix

Replace `VOID.start(Profile)` with a `RuntimeBuilder` entry point. The builder collects
runtime configuration before anything is created; `start()` is the terminal operation that
initializes the engine and returns a fully-started `VOID` instance.

**`RuntimeBuilder.java` (new):**
```java
public final class RuntimeBuilder {
    private String engineName;           // null = resolved from config/env/System property
    private DriverFactory.Profile profile;

    RuntimeBuilder() {}  // package-private; callers use VOID.builder()

    public RuntimeBuilder engine(String engineName) {
        this.engineName = engineName;
        return this;
    }

    public RuntimeBuilder profile(DriverFactory.Profile profile) {
        this.profile = profile;
        return this;
    }

    public VOID start() {
        FrameworkBootstrap.init();

        Properties config = resolvedConfig();
        UIEngine engine = UIEngineFactory.create(config,
                EngineBootstrap.fromProfile(profile));

        SessionContext ctx = new SessionContext(config, engine);

        CustomLogger.info.log("VOID session started -- engine="
                + engine.getEngineName() + ", profile=" + profile);
        return new VOID(ctx, engine);
    }

    private Properties resolvedConfig() {
        Properties config = new Properties(FrameworkBootstrap.getUtilsConfig());
        if (engineName != null) {
            config.setProperty("void.engine", engineName);
        }
        return config;
    }
}
```

If `.engine()` is not called, `UIEngineFactory` resolves the name from System property ->
ENV -> config -> default ("selenium"). `.engine()` overrides that resolution by injecting
into the config copy; it does not set a global System property.

**`VOID.java` -- builder entry point:**
```java
public static RuntimeBuilder builder() {
    return new RuntimeBuilder();
}
```

Usage:
```java
// Single session -- engine resolved from config/env
VOID session = VOID.builder()
        .profile(CHROME)
        .start();

// Explicit engine
VOID session = VOID.builder()
        .engine("selenium")
        .profile(CHROME)
        .start();

// Multiple independent sessions -- each owns its engine and runtime
VOID admin    = VOID.builder().profile(CHROME).start();
VOID customer = VOID.builder().profile(FIREFOX).start();
```

**`VOID.java` -- `start(Profile)` deprecated, delegates to builder:**
```java
@Deprecated(since = "0.3", forRemoval = true)
public static VOID start(DriverFactory.Profile profile) {
    return builder().profile(profile).start();
}
```

`DriverManager.createDriver()` is no longer called from `RuntimeBuilder.start()`. Multiple
`VOID.builder().start()` calls produce independent instances -- there is no shared static
`VOID` field. The invariant (one engine, one runtime, one context per instance) holds
structurally.

---

## `EngineBootstrap` -- delete Phase 1 compatibility bridge

`EngineBootstrap.FromDriver` and `EngineBootstrap.fromDriver()` were introduced in Phase 1
so `VOID.start()` could pass a pre-built driver into the factory without creating a second
browser. `RuntimeBuilder.start()` calls `EngineBootstrap.fromProfile()` directly. The
`FromDriver` path has no remaining caller and is deleted in this phase.

**`EngineBootstrap.java` -- remove `FromDriver`:**
```java
public sealed interface EngineBootstrap
        permits EngineBootstrap.FromProfile {   // FromDriver removed

    record FromProfile(DriverFactory.Profile profile) implements EngineBootstrap {}

    static EngineBootstrap fromProfile(DriverFactory.Profile profile) {
        return new FromProfile(profile);
    }
    // fromDriver(WebDriver) deleted
}
```

**`UIEngineFactory.java` -- simplify inner switch:**

The `FromDriver` branch is unreachable. The inner switch reduces to one case:
```java
UIEngine engine = switch (engineName) {
    case "selenium" -> switch (bootstrap) {
        case EngineBootstrap.FromProfile fp -> new SeleniumEngine(fp.profile());
    };
    default -> throw new IllegalStateException(
            "Unsupported engine: '" + engineName + "'. Supported: selenium");
};
```

The factory signature (`create(Properties, EngineBootstrap)`) is unchanged.

---

## V3 -- `ExecutionContext` (WebDriver-typed) instead of `SessionContext` (engine-typed)

### Problem

```java
// VOID.java:80
private final ExecutionContext context;
```

`ExecutionContext` holds `WebDriver`. `SessionContext` was written specifically to replace
it and already exists at `core/context/SessionContext.java`. It holds `UIEngine`. It has
never been used.

### Fix

**`VOID.java` -- change field type and constructor:**
```java
// Before
private final ExecutionContext context;

protected VOID(ExecutionContext context, UIEngine engine) {
    this.context = context;
    this.engine = engine;
    this.executor = new FlowExecutor(engine);
}

// After
private final SessionContext context;

protected VOID(SessionContext context, UIEngine engine) {
    this.context = context;
    this.engine = engine;
    this.executor = new FlowExecutor(engine);
}
```

**`VOID.java` -- deprecated `getContext()` re-typed:**
```java
@Deprecated(since = "0.1", forRemoval = true)
protected SessionContext getContext() {
    return context;
}
```

If any external caller was using `getContext().getDriver()`, that call breaks -- but
`getDriver()` was never on `SessionContext`, only on `ExecutionContext`. Any such caller
was already using the deprecated API and should migrate to `getEngine().getNativeDriver()`.

**`VOID.java` -- deprecated `getDriver()` no longer relies on `context`:**
```java
@Deprecated(since = "0.1", forRemoval = true)
protected WebDriver getDriver() {
    return (WebDriver) engine.getNativeDriver();
}
```

This keeps backward compatibility for subclasses that call `getDriver()`. The cast is safe
as long as the active engine is `SeleniumEngine` -- which is the only supported engine today.
The `@Deprecated` annotation signals that this escape hatch is not portable.

**`ExecutionContext` -- mark deprecated:**
```java
/**
 * @deprecated Since 0.2 -- replaced by {@link SessionContext} which holds {@link UIEngine}
 *             rather than a raw {@link WebDriver}. {@code VOID} no longer creates this class.
 *             Will be removed when no external callers remain.
 */
@Deprecated(since = "0.2")
public final class ExecutionContext { ... }
```

`ExecutionContext.java` is not deleted. It remains compilable for projects that may
reference it directly. Deletion is a separate workstream once all known callers migrate.

**Remove `import org.openqa.selenium.WebDriver` from `VOID.java`:**

After this change `VOID.java` has one remaining `WebDriver` reference: in the deprecated
`getDriver()` return type. Keep the import for that method. Once `getDriver()` is
removed the import goes with it.

---

## V5 -- `VOID.shutdown()` calls `DriverContext.removePrimary()` directly

### Problem

```java
// VOID.java:165
public void shutdown() {
    engine.shutdown();
    DriverContext.removePrimary();   // bypasses engine; Selenium-specific
}
```

`VOID.shutdown()` does two things: delegate to the engine, then manually clean up the
Selenium registry. If the engine is Playwright, `DriverContext.removePrimary()` is a no-op
that refers to a `WebDriver` that never existed -- harmless but wrong.

The deeper issue: `SeleniumEngine.shutdown()` currently calls only `driver.quit()`. It does
not clean up `DriverContext`. So `VOID` is compensating for an incomplete `SeleniumEngine`.

### Fix

**`SeleniumEngine.java` -- `shutdown()` owns its own registry cleanup:**
```java
@Override
public void shutdown() {
    if (driver != null) {
        try {
            driver.quit();
            debug.log("[SeleniumEngine] Driver shut down.");
        } catch (Exception e) {
            warn.log("[SeleniumEngine] Error during shutdown: " + e.getMessage());
        } finally {
            DriverContext.removePrimary();
            debug.log("[SeleniumEngine] Driver removed from DriverContext.");
        }
    }
}
```

`DriverContext.removePrimary()` is called in `finally` so it runs even if `driver.quit()`
throws. The legacy constructor path (driver not created by this engine) should also clean
up if a primary driver is registered. Checking `DriverContext.hasPrimary()` first is safer:

```java
} finally {
    if (DriverContext.hasPrimary()) {
        DriverContext.removePrimary();
    }
}
```

**`VOID.java` -- `shutdown()` delegates entirely:**
```java
public void shutdown() {
    CustomLogger.info.log("VOID session shutting down -- engine=" + engine.getEngineName());
    engine.shutdown();
    // DriverContext cleanup owned by the engine (SeleniumEngine.shutdown handles it)
}
```

`DriverContext` is no longer imported by `VOID.java`.

---

## `DriverManager.createDriver()` -- mark deprecated

`DriverManager.createDriver()` is no longer called by `VOID` or `RuntimeBuilder`. It may
still be called by tests or utilities that construct a `WebDriver` directly. Mark it deprecated:

```java
/**
 * @deprecated Since 0.3 -- {@code VOID} no longer uses this method.
 *             Use {@code VOID.builder().profile(profile).start()} to start a managed session.
 *             Direct callers should construct a {@link SeleniumEngine} explicitly.
 */
@Deprecated(since = "0.3")
public static WebDriver createDriver(DriverFactory.Profile profile) { ... }
```

`DriverManager.java` is not deleted -- its other methods (`quitAll`, `quitPrimary`) may
still be useful to direct Selenium users. Deletion is a separate workstream.

---

## Files changed

| File                                         | Change                                                                                                          |
|----------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `core/runtime/VOID.java`                     | Add `builder()` factory method; `start(Profile)` deprecated (delegates to builder); field `ExecutionContext` -> `SessionContext`; `shutdown()` removes `DriverContext` call; `getDriver()` re-routes through engine |
| `core/runtime/RuntimeBuilder.java`           | **NEW** -- fluent builder: `engine()`, `profile()`, `start()`                                                   |
| `core/engine/EngineBootstrap.java`           | Delete `FromDriver` record and `fromDriver()` static method; `sealed permits` reduced to `FromProfile` only    |
| `core/engine/UIEngineFactory.java`           | Inner switch simplified: `FromDriver` case removed; single `FromProfile` case remains                           |
| `core/engine/selenium/SeleniumEngine.java`   | `shutdown()` adds `DriverContext.removePrimary()` in `finally`                                                  |
| `core/driver/DriverManager.java`             | `createDriver()` marked `@Deprecated(since = "0.3")`                                                           |
| `core/context/ExecutionContext.java`         | Class marked `@Deprecated(since = "0.2")`                                                                       |

---

## Commits

```
feat(runtime): introduce RuntimeBuilder; VOID.builder() replaces VOID.start(Profile)
refactor(engine): delete EngineBootstrap.FromDriver; simplify UIEngineFactory inner switch
refactor(runtime): replace ExecutionContext with SessionContext in VOID; invert startup order
refactor(runtime): VOID.shutdown() delegates DriverContext cleanup to SeleniumEngine
```

---

## Verification

```
mvn compile -q

grep -n "createDriver"     src/main/java/core/runtime/VOID.java   # must be empty
grep -n "ExecutionContext" src/main/java/core/runtime/VOID.java   # must be empty
grep -n "SessionContext"   src/main/java/core/runtime/VOID.java   # must appear for field + constructor
grep -n "DriverContext"    src/main/java/core/runtime/VOID.java   # must be empty

grep -n "FromDriver"       src/main/java/core/engine/EngineBootstrap.java   # must be empty
grep -n "DriverContext"    src/main/java/core/engine/selenium/SeleniumEngine.java
# must include removePrimary() in shutdown() -- confirm it is present
```

Smoke test -- confirm a session starts and shuts down without a double driver or orphaned
registry entry:
```
mvn test -Dtest=VoidDemo -q
```

---

## Phase complete when

- [ ] `VOID.builder().profile(CHROME).start()` starts a session and returns a `VOID` instance.
- [ ] `VOID.start(Profile)` is annotated `@Deprecated(since = "0.3", forRemoval = true)`.
- [ ] Two `VOID.builder().start()` calls in the same JVM each open exactly one browser.
- [ ] `VOID.java` imports neither `WebDriver` (except in deprecated `getDriver()`) nor `DriverContext`.
- [ ] `EngineBootstrap` has no `FromDriver` record or `fromDriver()` method.
- [ ] Existing tests compile and pass without behavior changes.
