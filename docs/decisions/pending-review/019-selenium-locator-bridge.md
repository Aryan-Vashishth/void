# ADR-019 -- SeleniumLocatorBridge Isolation

**Date:** 2026-07-20
**Status:** Pending review (`feature/engine-decoupling`)

---

## Context

### SeleniumEngine imported by the interaction layer

`Interactions.java` imported `SeleniumEngine` for two purposes:

1. The deprecated `Interactions(WebDriver)` constructor delegated to `new SeleniumEngine(driver)`.
2. Six call sites in deprecated `By`-parameter methods called `SeleniumEngine.fromBy(By)` to
   convert a Selenium `By` into a `LocatorDescriptor`.

The second usage was the structural problem. `fromBy(By)` is a pure conversion utility --
it does not touch any engine instance, does not require engine initialization, and has no
runtime side effects. Yet its address was on a concrete engine implementation, which meant
a frozen legacy class (`Interactions`) had a compile-time dependency on a core engine type.

### The unsafe constructor cast

`Interactions(UIEngine)` also contained an explicit `(WebDriver)` cast:

```java
public Interactions(UIEngine engine) {
    this.engine = engine;
    DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver());
}
```

After Phase 2 of engine decoupling, `SeleniumEngine.initialize()` already registers the
driver in `DriverContext`. The cast was redundant and would throw `ClassCastException` for
any non-Selenium engine at the moment of construction.

### LocatorDescriptor must stay Selenium-free

`LocatorDescriptor` is the engine-neutral locator model. Moving `fromBy(By)` into
`LocatorDescriptor` would introduce a `org.openqa.selenium.By` dependency into the core
locator API -- the wrong direction.

---

## Decision

**Extract `SeleniumEngine.fromBy(By)` into a dedicated compatibility bridge class,
`SeleniumLocatorBridge`, in `core.bridge.selenium`.**

```java
package core.bridge.selenium;

@Deprecated(forRemoval = true)
public final class SeleniumLocatorBridge {

    @Deprecated(forRemoval = true)
    public static LocatorDescriptor fromBy(By by) {
        // parse By.toString() format: "By.cssSelector: .foo" or "By.xpath: //div"
        ...
    }

    private SeleniumLocatorBridge() {}
}
```

`SeleniumLocatorBridge` is itself `@Deprecated(forRemoval = true)`. It exists solely to
support the deprecated `By`-parameter methods in `Interactions`. It deletes as part of
the `By`/`WebElement` deprecated API removal workstream -- not as part of engine decoupling.

`Interactions.java` replaces all six `SeleniumEngine.fromBy()` call sites with
`SeleniumLocatorBridge.fromBy()`. The `SeleniumEngine` import in `Interactions.java` is
narrowed from seven references to one: the deprecated `Interactions(WebDriver)` constructor.

The unsafe `(WebDriver)` cast and `DriverContext.setPrimaryDriver()` call are removed from
`Interactions(UIEngine)`. The constructor becomes:

```java
public Interactions(UIEngine engine) {
    this.engine = engine;
}
```

---

## Scope constraints

### Bridge deletes with the deprecated API

`SeleniumLocatorBridge` is not a permanent utility. No new call sites should be added. Its
lifecycle is bound to the deprecated `By`-parameter methods in `Interactions`. When those
methods are removed, `SeleniumLocatorBridge` is removed in the same commit.

### Deprecated By/WebElement methods are not removed here

Approximately twelve deprecated methods in `Interactions` with `By` or `WebElement`
parameters are unchanged. Their removal is a separate workstream. Engine decoupling
requires only that the unsafe cast is gone and that the `SeleniumEngine` import is scoped
to a single deprecated constructor.

### SeleniumEngine.fromBy() is not deleted immediately

`SeleniumEngine.fromBy()` may remain on `SeleniumEngine` as a `@Deprecated` method.
It has no callers after `Interactions` migrates to `SeleniumLocatorBridge`. It can be
deleted in the same commit that removes the deprecated API workstream.

---

## Reasoning

### Why not move fromBy() into LocatorDescriptor?

`LocatorDescriptor` is in the engine-neutral locator model. Adding a `By` parameter
to any method there would introduce `org.openqa.selenium.By` as a compile-time dependency
of the core locator API. Every non-Selenium engine implementation would then have a
transitive Selenium dependency on their classpath -- defeating the purpose of the
engine abstraction.

### Why a new bridge class rather than leaving fromBy() on SeleniumEngine?

`SeleniumEngine` is a runtime implementation class. A legacy conversion utility has no
business being on it. The bridge class makes the scope explicit: deprecated, Selenium-only,
scoped to the `Interactions` legacy API. A reader encountering `SeleniumLocatorBridge` knows
immediately that it is a compatibility shim, not a supported API.

### Why not remove Interactions(WebDriver) in this phase?

`Interactions(WebDriver)` is already `@Deprecated(forRemoval = true)`. Removing it is a
larger change (potential external callers) and is not required to fix the cast. The
minimum fix -- removing the runtime-unsafe cast -- is the correct scope for this phase.

---

## Consequences

- `SeleniumLocatorBridge.java` created in `core.bridge.selenium`
- `Interactions.java` no longer casts `getNativeDriver()` to `WebDriver`
- `Interactions.java` no longer calls `DriverContext.setPrimaryDriver()` in its constructor
- `SeleniumEngine` import in `Interactions.java` scoped to the deprecated `Interactions(WebDriver)` constructor only
- `SeleniumLocatorBridge` deletes with the `By`/`WebElement` deprecated API workstream
- `Interactions(UIEngine)` is safe for any `UIEngine` implementation

---

## Related

- [ADR-018 -- Engine Lifecycle Ownership](018-engine-lifecycle-ownership.md)
- [ADR-007 -- UIEngine as Single Execution Authority](../accepted/007-uiengine-execution-authority.md)
- [Engine Decoupling Phase 3](../../plan/done/engine-decoupling/phase-3-interactions-cleanup.md)
