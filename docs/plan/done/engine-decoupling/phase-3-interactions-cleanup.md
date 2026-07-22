# Phase 3 — Interactions: Remove Unsafe Cast; Isolate Selenium Bridge

Violations: **V4**
Touches: `Interactions.java`

---

> **Implementation dependency**: This phase assumes Phase 2 has already moved `DriverContext`
> registration into `SeleniumEngine.initialize()`. Applying Phase 3 before Phase 2 breaks
> legacy `DriverContext` consumers.

## Goal

`Interactions` is a frozen legacy class. Its constructor currently registers a `WebDriver` in
`DriverContext` by casting `engine.getNativeDriver()` — a cast that throws `ClassCastException`
for any non-Selenium engine. After this phase, `Interactions` no longer assumes Selenium when
constructed from a `UIEngine`:
- The `Interactions(UIEngine)` constructor is safe for any engine.
- `SeleniumEngine.fromBy()` call sites are replaced by `SeleniumLocatorBridge.fromBy()`;
  `LocatorDescriptor` stays Selenium-free.
- The `SeleniumEngine` import in `Interactions.java` is reduced to the single deprecated
  `Interactions(WebDriver)` constructor (both are `@Deprecated(forRemoval=true)`; that
  constructor is not removed here).
- All remaining `By`/`WebElement` bridge methods are left in place.

## Non-goals

- Removing deprecated `By`-parameter methods (`clickOn(By)`, `typeInto(By)`, etc.).
- Removing deprecated `WebElement`-parameter methods.
- Removing the deprecated `Interactions(WebDriver)` constructor.
- Rewriting or deleting the `By`/`WebElement` legacy bridge.
- Any change to `LocatorDescriptor`, `ByParser`, or the core locator model.

---

## V4 — `Interactions(UIEngine)` casts `getNativeDriver()` to `WebDriver`

### Problem

```java
// Interactions.java:65–69
public Interactions(UIEngine engine) {
    this.engine = engine;
    // Backward compat: register native driver in DriverContext for legacy paths
    DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver());
}
```

`getNativeDriver()` returns `Object`. The cast to `WebDriver` is correct for
`SeleniumEngine` but throws `ClassCastException` for any other engine at the moment
`new Interactions(engine)` is called — before any interaction runs. The comment says
"backward compat", but it is actually a correctness hazard that would block hotswapping.

### Why this registration is no longer needed

After Phase 2, `SeleniumEngine.initialize()` calls `DriverContext.setPrimaryDriver(driver)`.
By the time `VOID.interaction()` creates an `Interactions` instance, the driver is already
registered. The `Interactions` constructor is doing redundant double-registration work.

The only scenario where this mattered was the pre-Phase-1 path where `VOID` held a raw
`WebDriver` and `Interactions` was constructed before `DriverContext` had a primary driver.
That construction path no longer exists after Phase 2.

### Fix

Remove the cast entirely. The `Interactions(UIEngine)` constructor becomes:

```java
public Interactions(UIEngine engine) {
    this.engine = engine;
}
```

No `DriverContext` call. No cast. Safe for any `UIEngine` implementation.

**`VOID.java` — `interaction()` is unaffected:**
```java
@Deprecated(since = "0.1", forRemoval = true)
public Interactions interaction() {
    if (interactions == null) {
        interactions = new Interactions(engine);   // now safe for any engine
    }
    return interactions;
}
```

---

## Remove `SeleniumEngine` import from `Interactions.java`

### Problem

```java
// Interactions.java:5
import core.engine.selenium.SeleniumEngine;
```

`Interactions` imports a concrete engine implementation. This appears in two call sites:

**1. `Interactions(WebDriver)` deprecated constructor (line 79–81):**
```java
@Deprecated(forRemoval = true)
public Interactions(WebDriver driver) {
    this(new SeleniumEngine(driver));
}
```

**2. `SeleniumEngine.fromBy()` calls in deprecated bridge methods (lines 161, 254, 384, 622, 821, 829):**
```java
LocatorDescriptor descriptor = SeleniumEngine.fromBy(locator);
```

### Fix — `SeleniumEngine.fromBy()` moves to `SeleniumLocatorBridge`

`SeleniumEngine.fromBy(By)` converts a Selenium `By` into a `LocatorDescriptor`. It is a
pure conversion utility that does not touch any engine instance. Moving it into
`LocatorDescriptor` would make the engine-neutral locator model depend on Selenium -- the
wrong direction. Instead it moves to a dedicated bridge class in the Selenium compatibility
layer, where it is scoped to deprecated paths and clearly marked for deletion.

**`SeleniumLocatorBridge.java` (new):**
```java
package core.bridge.selenium;

/**
 * Temporary compatibility bridge.
 * Exists only to support deprecated Selenium-based APIs in {@link Interactions}.
 * Delete together with those APIs.
 *
 * @deprecated Selenium-specific. No new call sites.
 */
@Deprecated(forRemoval = true)
public final class SeleniumLocatorBridge {

    /**
     * @deprecated Use element-based or string-based locator resolution instead.
     */
    @Deprecated(forRemoval = true)
    public static LocatorDescriptor fromBy(By by) {
        String raw = by.toString();
        // By.toString() format: "By.cssSelector: .foo" or "By.xpath: //div"
        int colon = raw.indexOf(':');
        if (colon < 0) return LocatorDescriptor.of(raw);
        String type  = raw.substring(0, colon).trim().toLowerCase(Locale.ROOT);
        String value = raw.substring(colon + 1).trim();
        LocatorStrategy strategy = switch (type) {
            case "by.id"          -> LocatorStrategy.ID;
            case "by.cssselector" -> LocatorStrategy.CSS;
            case "by.xpath"       -> LocatorStrategy.XPATH;
            case "by.name"        -> LocatorStrategy.NAME;
            default               -> LocatorStrategy.CSS;
        };
        return new LocatorDescriptor(value, strategy);
    }

    private SeleniumLocatorBridge() {}
}
```

**Verify `SeleniumEngine.fromBy()` implementation first** -- if it delegates to `ByParser`,
replicate the `ByParser` call in `SeleniumLocatorBridge`. If it parses `By.toString()`
directly, replicate that logic. The goal is the same output, not a new dependency.

**`Interactions.java` -- replace call sites:**
```java
// Before (6 sites)
LocatorDescriptor descriptor = SeleniumEngine.fromBy(locator);

// After
LocatorDescriptor descriptor = SeleniumLocatorBridge.fromBy(locator);
```

The `SeleniumEngine` import is **not fully removed** from `Interactions.java` because the
deprecated `Interactions(WebDriver)` constructor still delegates to `new SeleniumEngine(driver)`.
That constructor is `@Deprecated(forRemoval=true)` and is not changed in this phase. The
import scope narrows: from 7 references (1 constructor + 6 `fromBy` call sites) to 1
reference (the deprecated constructor only).

The cast is P0. The import narrowing is cleanup. Do not block Phase 3 on the bridge
extraction if it adds scope -- the minimum acceptable fix is removing the cast and the
`DriverContext` registration.

### Fix — `Interactions(WebDriver)` deprecated constructor

The deprecated `Interactions(WebDriver)` constructor uses `SeleniumEngine(WebDriver)` which
is itself marked `@Deprecated(forRemoval=true)` after Phase 1:

```java
@Deprecated(forRemoval = true)
public Interactions(WebDriver driver) {
    this(new SeleniumEngine(driver));   // SeleniumEngine(WebDriver) is deprecated — compiles with warning
}
```

This is acceptable. Both are deprecated, both compile. No change needed here.

---

## Remaining `By`/`WebElement` deprecated methods

`Interactions` contains approximately 12 deprecated methods with `By` or `WebElement`
parameters. They are left unchanged in this phase. They are already `@Deprecated(forRemoval=true)`.
Their removal is a future workstream, not part of engine decoupling.

Examples left intact:
- `clickOn(By locator)` — line 253
- `clickOn(WebElement element)` — line 287
- `typeInto(By locator, String text)` — line 621
- `isAnyDisplayed(By locator, Duration timeout, Duration poll)` — line 820

These do not affect engine hotswapping and are unchanged in this phase.

---

## Files changed

| File                                                        | Change                                                                                                                       |
|-------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `core/interactions/Interactions.java`                       | Remove `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver())` from constructor; replace 6x `SeleniumEngine.fromBy()` with `SeleniumLocatorBridge.fromBy()`; add `SeleniumLocatorBridge` import |
| `core/bridge/selenium/SeleniumLocatorBridge.java`           | **NEW** -- `@Deprecated` bridge with `fromBy(By)` static method; deletes with the `By`/`WebElement` deprecated API workstream |

---

## Commits

```
fix(interactions): remove unsafe WebDriver cast from Interactions(UIEngine) constructor
refactor(bridge): introduce SeleniumLocatorBridge; relocate Selenium By adapter
refactor(interactions): replace SeleniumEngine.fromBy() call sites with SeleniumLocatorBridge
```

The second and third commits are conditional on extracting `SeleniumLocatorBridge`. If the
bridge extraction adds too much scope, skip those commits and note that the `SeleniumEngine`
import remains scoped to the deprecated constructor -- to be cleaned up when the deprecated
`By`/`WebElement` API workstream runs.

---

## Verification

```
mvn compile -q

grep -n "getNativeDriver" src/main/java/core/interactions/Interactions.java   # must be empty
grep -n "WebDriver.*cast\|cast.*WebDriver\|(WebDriver)" src/main/java/core/interactions/Interactions.java
# must be empty (the deprecated WebDriver constructor uses the type, not a cast — that is OK)
```

Manual check: confirm `VOID.interaction()` still returns a usable `Interactions` object
after removing the constructor side-effect:
```
mvn test -Dtest=VoidDemo -q
```
