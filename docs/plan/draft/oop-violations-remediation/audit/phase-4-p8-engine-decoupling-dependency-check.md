# Phase 4 / P8 -- Engine-Decoupling Dependency Check

**Scope:** P8 only (`UIEngineFactory` switch -> registry)
**Run:** before starting Phase 4 implementation
**Depends on:** `feature/engine-decoupling` Phases 1 and 2 merged to `main`

---

## Purpose

P8 changes `UIEngineFactory` internals. By the time P8 is implemented, the
engine-decoupling work will have already changed the factory signature and the
`EngineBootstrap` type. This document confirms the expected state before P8 begins, so
the registry introduction does not conflict with or undo those changes.

P9 (LocatorRoles) and P11 (Via.java) have no dependency on engine-decoupling and can
be validated and implemented independently.

---

## Prerequisite checks

### 1. Engine-decoupling Phase 1 merged

`UIEngineFactory.create()` must accept `EngineBootstrap`, not `WebDriver`:

```
grep -n "public static UIEngine create" src/main/java/core/engine/UIEngineFactory.java
# expected: create(Properties config, EngineBootstrap bootstrap)
# fail:     create(Properties config, WebDriver driver)
```

### 2. Engine-decoupling Phase 2 merged

`EngineBootstrap.FromDriver` must be deleted:

```
grep -n "FromDriver" src/main/java/core/engine/EngineBootstrap.java
# expected: zero results
# fail:     record FromDriver(WebDriver driver) implements EngineBootstrap {}
```

`EngineBootstrap` must permit only `FromProfile`:

```
grep -n "permits" src/main/java/core/engine/EngineBootstrap.java
# expected: permits EngineBootstrap.FromProfile
```

The factory inner switch must have exactly one case:

```
grep -A5 "case \"selenium\"" src/main/java/core/engine/UIEngineFactory.java
# expected: case EngineBootstrap.FromProfile fp -> new SeleniumEngine(fp.profile());
# fail:     two-branch switch including FromDriver
```

### 3. SeleniumEngine.ID constant exists

`SeleniumEngine.ID` was introduced in engine-decoupling Phase 2:

```
grep -n "public static final String ID" src/main/java/core/engine/selenium/SeleniumEngine.java
# expected: public static final String ID = "selenium";
```

---

## P8 starting state (what the registry replaces)

After engine-decoupling Phases 1-2, the factory looks like:

```java
public static UIEngine create(Properties config, EngineBootstrap bootstrap) {
    String engineName = resolveEngineName(config);

    UIEngine engine = switch (engineName) {
        case "selenium" -> switch (bootstrap) {
            case EngineBootstrap.FromProfile fp -> new SeleniumEngine(fp.profile());
        };
        default -> throw new IllegalStateException(...);
    };
    ...
}
```

P8 replaces this switch with a registry map. The Selenium creator in the static
initializer casts to `EngineBootstrap.FromProfile`:

```java
static {
    REGISTRY.put(SeleniumEngine.ID,
        host -> new SeleniumEngine(((EngineBootstrap.FromProfile) host).profile()));
}
```

The `create()` signature (`Properties config, EngineBootstrap bootstrap`) is unchanged by P8.

---

## If prerequisites are not met

Do not start P8 implementation. The engine-decoupling work must merge first.
P9 and P11 have no such dependency and can proceed independently.
