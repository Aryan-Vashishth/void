# Phase 1 — Factory Contract: Engine Owns Its Driver

Violations: **V1**
Touches: `UIEngineFactory.java`, `SeleniumEngine.java`, `DriverManager.java`

---

## Goal

`UIEngineFactory.create()` must not accept a `WebDriver`. Each engine implementation creates
and manages its own native driver internally during `initialize()`. After this phase:
- A non-Selenium engine can be registered in the factory switch without ever touching
  `WebDriver`, `DriverFactory`, or `DriverContext`.
- `SeleniumEngine` is self-contained: given a `DriverFactory.Profile` it produces and
  manages its own driver.
- `VOID.start()` still creates a WebDriver (that changes in Phase 2); Phase 1 only fixes
  the factory contract so the Phase 2 inversion is safe.

---

## V1 — `UIEngineFactory.create()` requires a pre-built `WebDriver`

### Problem

```java
// UIEngineFactory.java:41
public static UIEngine create(Properties config, WebDriver driver)
```

`WebDriver` is Selenium-specific. A Playwright engine does not have a `WebDriver` — it has
a `Page`. The current signature makes it structurally impossible to register any
non-Selenium engine here:

```java
case "selenium"   -> new SeleniumEngine(driver);   // WebDriver passed in
// case "playwright" -> new PlaywrightEngine();     // what does it do with `driver`?
```

The factory's job is to create an engine. It should not receive a pre-built artifact that
only one engine type can use.

### Fix

Replace the `WebDriver driver` parameter with `DriverFactory.Profile profile`.
`profile` is an enum that describes what kind of Selenium driver to build — it is only
meaningful to `SeleniumEngine`. Any non-Selenium engine ignores it.

**`UIEngineFactory.java` — new signature:**
```java
public static UIEngine create(Properties config, DriverFactory.Profile profile) {
    String engineName = resolveEngineName(config);
    info.log("[UIEngineFactory] Creating engine: " + engineName);

    UIEngine engine = switch (engineName) {
        case "selenium"   -> new SeleniumEngine(profile);
        // case "playwright" -> new PlaywrightEngine();   // Phase 3 of engine roadmap
        default -> throw new IllegalStateException(
                "Unsupported engine: '" + engineName + "'. Supported: selenium");
    };

    EngineConfig engineConfig = new EngineConfig(config);
    engine.initialize(engineConfig);

    info.log("[UIEngineFactory] Engine '" + engineName + "' initialized. Timeout="
            + engineConfig.getDefaultTimeout().toSeconds() + "s");
    return engine;
}
```

`resolveEngineName()` is unchanged — it still checks System property → ENV → config → default.

The import `org.openqa.selenium.WebDriver` is **removed** from `UIEngineFactory.java`.

---

## `SeleniumEngine` — dual-constructor for clean and legacy paths

`SeleniumEngine` currently takes a `WebDriver` in its only constructor. After Phase 1 it
needs two constructors: one for the clean engine-factory path, one for the legacy
`Interactions(WebDriver)` bridge that still exists.

**`SeleniumEngine.java` — new primary constructor:**
```java
// Primary path: engine manages its own driver lifecycle
SeleniumEngine(DriverFactory.Profile profile) {
    this.profile = profile;
    this.driver = null;  // created during initialize()
}
```

**`SeleniumEngine.java` — deprecated legacy constructor:**
```java
// Legacy bridge for: Interactions(WebDriver) → SeleniumEngine(WebDriver)
// Kept until the Interactions(WebDriver) deprecated constructor is removed.
@Deprecated(forRemoval = true)
public SeleniumEngine(WebDriver driver) {
    this.profile = null;
    this.driver = driver;  // already built by caller; initialize() skips creation
}
```

**`SeleniumEngine.java` — `initialize()` creates the driver when on the primary path:**
```java
@Override
public void initialize(EngineConfig config) {
    this.config = config;
    this.defaultTimeout = config.getDefaultTimeout();

    if (this.driver == null) {
        // Primary path: engine creates and registers its own driver
        this.driver = DriverFactory.fromProfile(profile).build();
        DriverContext.setPrimaryDriver(this.driver);
        debug.log("[SeleniumEngine] Driver created and registered via profile: " + profile);
    } else {
        // Legacy path: driver was provided by caller (Interactions bridge)
        debug.log("[SeleniumEngine] Driver provided externally (legacy path).");
    }

    debug.log("[SeleniumEngine] Initialized with timeout=" + defaultTimeout.toSeconds() + "s");
}
```

**New field declarations in `SeleniumEngine`:**
```java
private final DriverFactory.Profile profile;  // null on legacy path
private WebDriver driver;                      // null until initialize() on primary path
private EngineConfig config;
private Duration defaultTimeout;
```

The `shutdown()` method is unchanged: it calls `driver.quit()`. `DriverContext` cleanup
is added in Phase 2 (it belongs in shutdown, not initialize).

---

## `DriverManager` — no change in Phase 1

`DriverManager.createDriver()` still works. `VOID.start()` still calls it in Phase 1
because the `VOID` startup sequence is not inverted until Phase 2.

In Phase 1, the call chain is temporarily inconsistent:

```
VOID.start(profile)
  1. DriverManager.createDriver(profile)   → creates WebDriver, registers in DriverContext
  2. UIEngineFactory.create(config, profile)
       → SeleniumEngine(profile) → initialize() → tries to build ANOTHER driver
```

This would create two browsers. To prevent this: in Phase 1 the `VOID.start()` call
to `UIEngineFactory.create()` is updated to pass `null` profile or the factory method is
temporarily overloaded.

**Preferred approach:** update `VOID.start()` to use a temporary adapter overload that
passes the already-created driver through, and merge the full inversion in Phase 2 as a
single atomic commit. This keeps Phase 1 compiler-clean without a half-baked startup.

**Temporary `UIEngineFactory` overload for Phase 1 only:**
```java
/** @deprecated Internal bridge. Remove when VOID.start() is inverted in Phase 2. */
@Deprecated
static UIEngine createWithDriver(Properties config, WebDriver driver) {
    String engineName = resolveEngineName(config);
    UIEngine engine = switch (engineName) {
        case "selenium" -> new SeleniumEngine(driver);  // legacy constructor
        default -> throw new IllegalStateException("Unsupported: " + engineName);
    };
    engine.initialize(new EngineConfig(config));
    return engine;
}
```

`VOID.start()` calls `createWithDriver()` in Phase 1 and switches to `create(config, profile)`
in Phase 2. The `createWithDriver` method is deleted in the Phase 2 commit.

This keeps each phase independently compilable with zero double-browser risk.

---

## Files changed

| File                                          | Change                                                                      |
|-----------------------------------------------|-----------------------------------------------------------------------------|
| `core/engine/UIEngineFactory.java`            | `create()` signature: `WebDriver` → `DriverFactory.Profile`; add temporary `createWithDriver()` |
| `core/engine/selenium/SeleniumEngine.java`    | Add `SeleniumEngine(Profile)` primary constructor; `SeleniumEngine(WebDriver)` → `@Deprecated(forRemoval=true)`; `initialize()` creates driver when `driver == null` |

---

## Commits

```
feat(engine): add SeleniumEngine(Profile) constructor; initialize() creates driver internally
refactor(engine): drop WebDriver param from UIEngineFactory.create(), accept DriverFactory.Profile
```

---

## Verification

```
mvn compile -q
grep -n "import org.openqa.selenium.WebDriver" src/main/java/core/engine/UIEngineFactory.java
# must return zero results

grep -rn "new SeleniumEngine(" src/main/java/
# must only appear inside UIEngineFactory (legacy bridge) and Interactions (deprecated path)
```
