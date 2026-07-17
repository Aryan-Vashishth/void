# Phase 2 — VOID Startup Pipeline: Invert and Wire SessionContext

Violations: **V2**, **V3**, **V5**
Deletes: `UIEngineFactory.createWithDriver()` (temporary bridge from Phase 1)
Deprecates: `ExecutionContext` (not deleted — external callers may use it directly)

---

## Goal

`VOID.start()` creates a `WebDriver` before selecting an engine. After this phase, the
engine is selected first and creates its own driver as a side effect of `initialize()`.
`SessionContext` — which already exists and holds `UIEngine` — replaces `ExecutionContext`
(which holds `WebDriver`) as the session state holder in `VOID`. After this phase:
- Configuring `engine=playwright` will not open a Chrome window.
- `VOID` holds no reference to `WebDriver` or `ExecutionContext`.
- `VOID.shutdown()` delegates entirely to the engine.

---

## V2 — `VOID.start()` creates WebDriver before engine selection

### Problem

```java
// VOID.java:138–144
WebDriver driver = DriverManager.createDriver(profile);    // Step 1: Selenium always
ExecutionContext ctx = new ExecutionContext(               // Step 2: WebDriver locked in
        FrameworkBootstrap.getUtilsConfig(), driver);
UIEngine engine = UIEngineFactory.create(                 // Step 3: engine created too late
        FrameworkBootstrap.getUtilsConfig(), driver);
```

The engine has no say in driver creation. A `PlaywrightEngine` would need to swallow an
already-open Chrome window.

### Fix

Invert steps 1 and 3. Engine selection happens first; driver creation is a consequence of
`engine.initialize()`, which happens inside `UIEngineFactory.create()`.

**`VOID.java` — rewritten `start(Profile)`:**
```java
public static VOID start(DriverFactory.Profile profile) {
    FrameworkBootstrap.init();

    UIEngine engine = UIEngineFactory.create(FrameworkBootstrap.getUtilsConfig(), profile);

    SessionContext ctx = new SessionContext(FrameworkBootstrap.getUtilsConfig(), engine);

    CustomLogger.info.log("VOID session started — engine=" + engine.getEngineName()
            + ", profile=" + profile);
    return new VOID(ctx, engine);
}
```

`DriverManager.createDriver()` is no longer called from `VOID.start()`.
`UIEngineFactory.createWithDriver()` (temporary Phase 1 bridge) is deleted in this commit.

---

## V3 — `ExecutionContext` (WebDriver-typed) instead of `SessionContext` (engine-typed)

### Problem

```java
// VOID.java:80
private final ExecutionContext context;
```

`ExecutionContext` holds `WebDriver`. `SessionContext` was written specifically to replace
it and already exists at `core/context/SessionContext.java`. It holds `UIEngine`. It has
never been used.

### Fix

**`VOID.java` — change field type and constructor:**
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

**`VOID.java` — deprecated `getContext()` re-typed:**
```java
@Deprecated(since = "0.1", forRemoval = true)
protected SessionContext getContext() {
    return context;
}
```

If any external caller was using `getContext().getDriver()`, that call breaks — but
`getDriver()` was never on `SessionContext`, only on `ExecutionContext`. Any such caller
was already using the deprecated API and should migrate to `getEngine().getNativeDriver()`.

**`VOID.java` — deprecated `getDriver()` no longer relies on `context`:**
```java
@Deprecated(since = "0.1", forRemoval = true)
protected WebDriver getDriver() {
    return (WebDriver) engine.getNativeDriver();
}
```

This keeps backward compatibility for subclasses that call `getDriver()`. The cast is safe
as long as the active engine is `SeleniumEngine` — which is the only supported engine today.
The `@Deprecated` annotation signals that this escape hatch is not portable.

**`ExecutionContext` — mark deprecated:**
```java
/**
 * @deprecated Since 0.2 — replaced by {@link SessionContext} which holds {@link UIEngine}
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

## V5 — `VOID.shutdown()` calls `DriverContext.removePrimary()` directly

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
that refers to a `WebDriver` that never existed — harmless but wrong.

The deeper issue: `SeleniumEngine.shutdown()` currently calls only `driver.quit()`. It does
not clean up `DriverContext`. So `VOID` is compensating for an incomplete `SeleniumEngine`.

### Fix

**`SeleniumEngine.java` — `shutdown()` owns its own registry cleanup:**
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

**`VOID.java` — `shutdown()` delegates entirely:**
```java
public void shutdown() {
    CustomLogger.info.log("VOID session shutting down — engine=" + engine.getEngineName());
    engine.shutdown();
    // DriverContext cleanup owned by the engine (SeleniumEngine.shutdown handles it)
}
```

`DriverContext` is no longer imported by `VOID.java`.

---

## `DriverManager.createDriver()` — mark deprecated

`DriverManager.createDriver()` is no longer called by `VOID.start()`. It may still be
called by tests or utilities that construct a `WebDriver` directly. Mark it deprecated:

```java
/**
 * @deprecated Since 0.2 — {@code VOID.start()} no longer uses this method.
 *             Use {@code VOID.start(Profile)} which manages driver lifecycle through the engine.
 *             Direct callers should construct a {@link SeleniumEngine} explicitly.
 */
@Deprecated(since = "0.2")
public static WebDriver createDriver(DriverFactory.Profile profile) { ... }
```

`DriverManager.java` is not deleted — its other methods (`quitAll`, `quitPrimary`) may
still be useful to direct Selenium users. Deletion is a separate workstream.

---

## Files changed

| File                                         | Change                                                                               |
|----------------------------------------------|--------------------------------------------------------------------------------------|
| `core/runtime/VOID.java`                     | `start()` inverted; field `ExecutionContext` → `SessionContext`; `shutdown()` removes `DriverContext` call; `getDriver()` re-routes through engine |
| `core/engine/UIEngineFactory.java`           | Delete `createWithDriver()` temporary bridge                                         |
| `core/engine/selenium/SeleniumEngine.java`   | `shutdown()` adds `DriverContext.removePrimary()` in `finally`                       |
| `core/driver/DriverManager.java`             | `createDriver()` marked `@Deprecated(since = "0.2")`                                |
| `core/context/ExecutionContext.java`         | Class marked `@Deprecated(since = "0.2")`                                            |

---

## Commits

```
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

grep -n "DriverContext"    src/main/java/core/engine/selenium/SeleniumEngine.java
# must include removePrimary() in shutdown() — confirm it is present
```

Smoke test — confirm a session can start and shut down without a double driver or orphaned
registry entry:
```
mvn test -Dtest=VoidDemo -q
```
