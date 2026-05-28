# `core.bootstrap` — Framework Initialisation

One-time, idempotent startup gate that must run before any driver or test logic.

---

## Overview

`FrameworkBootstrap` performs exactly-once initialisation tasks that validate the runtime environment and seed configuration stores. It is called automatically by `VOID.start()` but can also be invoked directly.

---

## Class Inventory

| Class | Responsibility |
|-------|----------------|
| `FrameworkBootstrap` | Static, thread-safe init gate with fail-fast validation |

---

## What It Does

When `FrameworkBootstrap.init()` is called for the first time:

1. **Verify `driver.properties`** — checks that the file exists on the classpath. Throws `IllegalStateException` immediately if missing (fail-fast).
2. **Load utils/test config** — reads `test.properties` from the classpath and seeds it into `ConfigLoader.ACTIVE` for legacy compatibility.

```
FrameworkBootstrap.init()
  ├── [1] Verify driver.properties on classpath  (fail-fast)
  ├── [2] Load test.properties → Properties object
  └── [3] Seed ConfigLoader.ACTIVE (backward-compat)
```

---

## Key Properties

| Property | Description |
|----------|-------------|
| **Idempotent** | Safe to call `init()` multiple times — only the first invocation performs work |
| **Thread-safe** | Uses `synchronized` + `volatile` for safe concurrent access |
| **No driver logic** | Intentionally free of WebDriver concerns — those live in `core.driver` |
| **No mutable state** | Beyond the `initialized` guard, no global mutable state |

---

## Usage

### Automatic (recommended)

```java
// VOID.start() calls FrameworkBootstrap.init() automatically
VOID app = VOID.start();
```

### Manual (advanced)

```java
// Can be called explicitly if you need early validation
FrameworkBootstrap.init();
Properties config = FrameworkBootstrap.getUtilsConfig();
```

---

## API Reference

| Method | Description |
|--------|-------------|
| `init()` | Bootstrap the framework (idempotent) |
| `getUtilsConfig()` | Returns loaded utils/test config (empty until `init()` completes) |
| `isInitialized()` | Whether bootstrap has completed |
| `reset()` | **Test-only** — resets state for unit testing |

---

## Error Handling

If `driver.properties` is not found on the classpath:

```
IllegalStateException: FrameworkBootstrap failed: driver.properties not found on classpath
at 'core/driver/config/driver.properties'.
Ensure the file exists at src/main/resources/core/driver/config/driver.properties
```

---

## See Also

- `core.runtime.VOID` — calls `FrameworkBootstrap.init()` during startup
- `core.utils.ConfigLoader` — the config system that gets seeded
- `core.utils.ConfigPaths` — standard config file path constants
- `core.driver.DriverFactory` — consumes `driver.properties` later in the pipeline

