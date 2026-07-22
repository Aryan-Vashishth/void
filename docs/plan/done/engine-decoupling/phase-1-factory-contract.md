# Phase 1 — Factory Contract: Engine Owns Its Driver

Violations: **V1**
Touches: `UIEngineFactory.java`, `SeleniumEngine.java`

---

## Goal

`UIEngineFactory.create()` must not accept a `WebDriver`. Each engine implementation owns
creation, lifecycle, and shutdown of its native automation runtime. After this phase:
- A non-Selenium engine can be registered in the factory switch without ever touching
  `WebDriver`, `DriverFactory`, or `DriverContext`.
- `SeleniumEngine` owns creation and lifecycle of its native automation runtime.
- `VOID.start()` still creates a WebDriver (that changes in Phase 2); Phase 1 only fixes
  the factory contract so the Phase 2 inversion is safe.
- `UIEngineFactory` no longer owns or understands native automation resources; it only
  constructs engines.

> **Invariant**: Driver creation occurs at most once per VOID runtime. All compatibility
> bridge logic in this phase exists solely to preserve this invariant during the transition.

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

Replace the `WebDriver driver` parameter with an `EngineBootstrap` abstraction.
`EngineBootstrap` encapsulates whatever initialization data the factory needs to pass to the
engine. In Phase 1 it can carry a pre-built driver (compatibility path from `VOID.start()`);
in Phase 2 it carries only a `DriverFactory.Profile`.

> **Migration note**: `EngineBootstrap` exists solely to keep Phase 1 and Phase 2
> independently compilable while avoiding duplicate driver creation. It is intentionally
> simplified after Phase 2 and is not intended as a permanent cross-engine abstraction.

The profile is only consumed by `SeleniumEngine`. Other engines obtain their initialization
data through their own engine-specific configuration once engine registration becomes
extensible.

**`EngineBootstrap.java` — new sealed type:**
```java
public sealed interface EngineBootstrap
        permits EngineBootstrap.FromDriver, EngineBootstrap.FromProfile {

    record FromDriver(WebDriver driver) implements EngineBootstrap {}
    record FromProfile(DriverFactory.Profile profile) implements EngineBootstrap {}

    static EngineBootstrap fromDriver(WebDriver driver) { return new FromDriver(driver); }
    static EngineBootstrap fromProfile(DriverFactory.Profile profile) { return new FromProfile(profile); }
}
```

`FromDriver` is the compatibility path used by `VOID.start()` in Phase 1.
`FromDriver` and `EngineBootstrap.fromDriver()` are deleted in the Phase 2 commit.

**`UIEngineFactory.java` — new signature:**
```java
public static UIEngine create(Properties config, EngineBootstrap bootstrap) {
    String engineName = resolveEngineName(config);
    info.log("[UIEngineFactory] Creating engine: " + engineName);

    UIEngine engine = switch (engineName) {
        case "selenium" -> switch (bootstrap) {
            case EngineBootstrap.FromDriver  fd -> new SeleniumEngine(fd.driver());
            case EngineBootstrap.FromProfile fp -> new SeleniumEngine(fp.profile());
        };
        // case "playwright" -> ...   // Phase 3 of engine roadmap
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

## `SeleniumEngine` — dual-constructor for primary and compatibility paths

`SeleniumEngine` currently takes a `WebDriver` in its only constructor. After Phase 1 it
needs two constructors: one for the primary engine-factory path, one for the compatibility
`Interactions(WebDriver)` bridge that still exists.

**`SeleniumEngine.java` — new primary constructor:**
```java
// Primary path: engine manages its own driver lifecycle
SeleniumEngine(DriverFactory.Profile profile) {
    this.profile = profile;
    this.driver = null;  // created during initialize()
}
```

**`SeleniumEngine.java` — compatibility constructor:**
```java
// Compatibility bridge for: Interactions(WebDriver) -> SeleniumEngine(WebDriver)
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
    // DriverContext remains a temporary Selenium compatibility mechanism;
    // it is removed from the runtime startup path in Phase 2.

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

In Phase 1, the call chain would be temporarily inconsistent without a bridge:

```
VOID.start(profile)
  1. DriverManager.createDriver(profile)   -> creates WebDriver, registers in DriverContext
  2. UIEngineFactory.create(config, bootstrap)
       -> SeleniumEngine(profile) -> initialize() -> tries to build ANOTHER driver
```

This would create two browsers. The `EngineBootstrap` abstraction prevents this.

**`VOID.start()` in Phase 1** wraps the already-created driver in a compatibility bootstrap
and passes it to the factory. No new factory method is introduced:

```java
// VOID.start() — Phase 1 (before inversion)
WebDriver driver = DriverManager.createDriver(profile);
UIEngine engine = UIEngineFactory.create(
        config,
        EngineBootstrap.fromDriver(driver));  // compatibility path
```

The factory's `FromDriver` branch reaches `new SeleniumEngine(driver)` (compatibility
constructor). `initialize()` sees `this.driver != null` and skips driver creation.

**`VOID.start()` in Phase 2** (inversion) stops creating the driver itself:

```java
// VOID.start() — Phase 2 (after inversion)
UIEngine engine = UIEngineFactory.create(
        config,
        EngineBootstrap.fromProfile(profile));  // primary path
```

`EngineBootstrap.fromDriver()` and `EngineBootstrap.FromDriver` are deleted in the Phase 2
commit. The factory signature is unchanged across both phases.

---

## Interaction with OOP violations Phase 4 (P8)

OOP violations Phase 4 / P8 also modifies `UIEngineFactory` -- it replaces the `switch`-on-string
dispatch with a registry `Map<String, Function<Object, UIEngine>>`. The two changes are orthogonal
and must be applied in sequence:

1. **Phase 1** (this phase): changes the **parameter type** (`WebDriver` -> `EngineBootstrap`).
2. **Phase 2**: inverts `VOID.start()`, deletes `EngineBootstrap.FromDriver`.
3. **OOP P8** (after Phase 2): changes the **dispatch mechanism** (`switch` -> registry map).

P8 must not be applied before Phase 2 is complete. After Phase 2, `EngineBootstrap` holds only
`FromProfile`, and the registered Selenium creator becomes:

```java
REGISTRY.put("selenium",
    host -> new SeleniumEngine(((EngineBootstrap.FromProfile) host).profile()));
```

**EngineBootstrap as EngineHost**: P8 uses the term "EngineHost" for the opaque bootstrap object
passed to a registered creator, and types the registry as `Function<Object, UIEngine>`. After
Phase 1-2, `EngineBootstrap` is the concrete EngineHost type for Selenium. The cast inside the
registered lambda is safe because the factory only produces `EngineBootstrap` instances today.

**Future engines**: `EngineBootstrap` is an internal migration abstraction. Once additional engine
implementations are introduced, the bootstrap mechanism will be revisited based on their actual
initialization requirements.

See: `docs/plan/draft/oop-violations-remediation/phase-4-infrastructure.md` (P8 section).

---

## Files changed

| File | Change |
|---|---|
| `core/engine/UIEngineFactory.java` | `create()` signature: `WebDriver` -> `EngineBootstrap`; nested switch dispatches on bootstrap type to reach the correct `SeleniumEngine` constructor; `WebDriver` import removed |
| `core/engine/EngineBootstrap.java` | New sealed interface with `FromDriver(WebDriver)` and `FromProfile(DriverFactory.Profile)` records; `fromDriver()` deleted in Phase 2 commit |
| `core/engine/selenium/SeleniumEngine.java` | Add `SeleniumEngine(Profile)` primary constructor; `SeleniumEngine(WebDriver)` marked `@Deprecated(forRemoval=true)` as compatibility constructor; `initialize()` creates driver when `this.driver == null` |
| `core/runtime/VOID.java` | `VOID.start()` factory call: wrap existing `WebDriver` in `EngineBootstrap.fromDriver(driver)` to match new factory signature |

---

## Commits

```
feat(engine): add SeleniumEngine(Profile) constructor; initialize() creates driver internally
refactor(engine): replace WebDriver factory parameter with EngineBootstrap
```

---

## Verification

```
mvn compile -q

grep -n "import org.openqa.selenium.WebDriver" src/main/java/core/engine/UIEngineFactory.java
# expected: zero results

grep -rn "new SeleniumEngine(" src/main/java/
# expected: only inside UIEngineFactory (EngineBootstrap dispatch) and Interactions (compatibility path)

grep -rn "DriverFactory.fromProfile" src/main/java/
# expected during Phase 1: two creation paths --
#   SeleniumEngine.initialize() (primary path, reached via EngineBootstrap.FromProfile)
#   DriverManager.createDriver() (compatibility path, still active in VOID.start())
# expected after Phase 2: exactly one creation path (SeleniumEngine.initialize() only)
# use this as a regression check during the migration

# Behavioral check: call VOID.start() and confirm exactly one browser window opens.
# If the EngineBootstrap compatibility bridge is missing or broken, DriverManager and
# SeleniumEngine.initialize() will both create a driver -- two windows will open instead of one.
```

---

## Phase complete when

- [ ] `UIEngineFactory.create()` has no `WebDriver` parameter.
- [ ] `SeleniumEngine` can create its own driver from a `DriverFactory.Profile`.
- [ ] `VOID.start()` still launches exactly one browser.
- [ ] Existing tests compile and pass without behavior changes.

