# Phase 4 — Bootstrap Cleanup: Move Selenium Logger to SeleniumEngine

Violations: **V6**
Touches: `FrameworkBootstrap.java`, `SeleniumEngine.java`

---

## Goal

`FrameworkBootstrap.init()` suppresses Selenium's JUL logger as a one-time side effect of
bootstrapping. This runs unconditionally regardless of the configured engine. After this phase:
- `FrameworkBootstrap` contains no Selenium-specific code.
- Selenium logger suppression happens inside `SeleniumEngine.initialize()`.
- A Playwright session's bootstrap path does not touch Selenium internals.

---

## V6 — Selenium JUL logger suppressed in engine-agnostic bootstrap

### Problem

```java
// FrameworkBootstrap.java:45–47
// Suppress Selenium's JUL logging (CDP version-mismatch warnings, verbose
// protocol messages) at source, before any driver or Selenium Manager is created.
Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
```

The comment acknowledges this must happen "before any driver is created". That concern
belongs to the Selenium engine, not to the bootstrap. After Phase 2, the driver is created
inside `SeleniumEngine.initialize()`, so `SeleniumEngine` already controls the moment
before driver creation. The logger suppression can safely move there.

The JUL logger is a JVM-global side effect — once set to `SEVERE` it stays there for the
life of the JVM regardless of which engine subsequently runs. Moving it into
`SeleniumEngine.initialize()` does not change this behavior; it only moves the call site to
where it logically belongs.

### Fix

**`SeleniumEngine.java` — suppress at the start of `initialize()`, before driver creation:**
```java
@Override
public void initialize(EngineConfig config) {
    this.config = config;
    this.defaultTimeout = config.getDefaultTimeout();

    // Suppress Selenium's JUL logging before Selenium Manager or any driver code runs.
    // CDP version-mismatch warnings and verbose protocol messages appear during driver startup.
    Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);

    if (this.driver == null) {
        this.driver = DriverFactory.fromProfile(profile).build();
        DriverContext.setPrimaryDriver(this.driver);
        debug.log("[SeleniumEngine] Driver created and registered via profile: " + profile);
    } else {
        debug.log("[SeleniumEngine] Driver provided externally (legacy path).");
    }

    debug.log("[SeleniumEngine] Initialized with timeout=" + defaultTimeout.toSeconds() + "s");
}
```

`SeleniumEngine.java` already imports `org.openqa.selenium.*` — no new imports needed.
Add `java.util.logging.Level` and `java.util.logging.Logger` to `SeleniumEngine`'s import block.

**`FrameworkBootstrap.java` — remove the three lines and their imports:**

Remove:
```java
// Lines 45–47 (comment + logger call)
Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
```

Remove imports:
```java
import java.util.logging.Level;
import java.util.logging.Logger;
```

The `init()` method comment block (lines 38–41 in the Javadoc) references "before any driver
or Selenium Manager is created" — update it to remove that note since the logger is no longer
suppressed here.

---

## Side effect: `FrameworkBootstrap` is now fully engine-agnostic

After Phase 4, `FrameworkBootstrap.init()` does exactly two things:
1. Verifies `driver.properties` is on the classpath (existence check only).
2. Loads utils/test config.

The class Javadoc already says it is "intentionally free of driver logic" — after Phase 4
this is actually true. Update the class-level Javadoc to remove the self-referential note
about the Selenium logger.

---

## Timing consideration

The logger suppression in `SeleniumEngine.initialize()` runs after `UIEngineFactory.create()`
resolves the engine name. The engine name resolution uses `System.getProperty("engine")` and
`System.getenv("ENGINE")` — neither of which triggers any Selenium code. There is no window
between bootstrap and `initialize()` where Selenium logger output could escape if the
suppression is in `initialize()` rather than `init()`.

The original placement "before any driver or Selenium Manager is created" is still satisfied
because `DriverFactory.fromProfile(profile).build()` is called after the logger is set.

---

## Files changed

| File                                          | Change                                                                          |
|-----------------------------------------------|---------------------------------------------------------------------------------|
| `core/bootstrap/FrameworkBootstrap.java`      | Remove `Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE)` and the two `java.util.logging` imports; update Javadoc |
| `core/engine/selenium/SeleniumEngine.java`    | Add `Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE)` at start of `initialize()`; add `java.util.logging.Level` and `java.util.logging.Logger` imports |

---

## Commits

```
refactor(bootstrap): move Selenium JUL logger suppression to SeleniumEngine.initialize()
```

---

## Verification

```
mvn compile -q

grep -n "org.openqa.selenium" src/main/java/core/bootstrap/FrameworkBootstrap.java
# must return zero results

grep -n "Logger.getLogger" src/main/java/core/engine/selenium/SeleniumEngine.java
# must include "org.openqa.selenium"
```

Run the demo test and confirm no CDP version-mismatch warnings appear at the INFO level
(they should still be suppressed):
```
mvn test -Dtest=VoidDemo 2>&1 | grep -i "cdp\|version.*mismatch"
# must return zero results
```
