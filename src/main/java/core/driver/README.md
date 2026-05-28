# `core.driver` — WebDriver Lifecycle Management

Complete Selenium WebDriver lifecycle: creation, configuration, thread-local storage, and shutdown.

---

## Overview

All WebDriver concerns are isolated in this package to keep the rest of the framework engine-agnostic. Driver configuration is entirely file-driven — no code changes needed to switch browsers, enable headless mode, or connect to a Selenium Grid.

---

## Class Inventory

| Class | Responsibility |
|-------|----------------|
| `DriverFactory` | Fluent WebDriver builder — creates configured browser instances |
| `DriverContext` | Thread-local driver storage — safe parallel access |
| `DriverManager` | Lifecycle orchestrator — create, register, quit drivers |
| `Waiter` | Explicit-wait helpers for common conditions |

---

## `DriverFactory`

Fluent builder that creates a fully-configured `WebDriver` instance from a `Profile`.

**Supports:**
- **Browsers:** Chrome, Firefox, Edge
- **Modes:** Local, Remote (Selenium Grid / Selenoid)
- **Options:** Headless, mobile emulation, proxy, custom capabilities, custom binary paths
- **Page load strategies:** NORMAL, EAGER, NONE

```java
// Typically used internally by DriverManager:
WebDriver driver = DriverFactory.fromProfile(Profile.DEFAULT).build();
```

### Profiles

`DriverFactory.Profile` encapsulates a named configuration set (read from `driver.properties`):

| Profile | Source |
|---------|--------|
| `DEFAULT` | `core/driver/config/driver.properties` |
| Custom | Override via system properties or environment variables |

---

## `DriverContext`

Thread-local holder for WebDriver instances. Enables safe parallel test execution.

```java
// Set (done by DriverManager)
DriverContext.setPrimaryDriver(driver);

// Get
WebDriver driver = DriverContext.getActiveDriver();

// Cleanup
DriverContext.quitAllDrivers();
DriverContext.quitPrimaryDriver();
```

---

## `DriverManager`

Static utility that orchestrates the driver lifecycle:

```java
// Create + register
WebDriver driver = DriverManager.createDriver(Profile.DEFAULT);

// Quit all (end of session)
DriverManager.quitAll();

// Quit primary only
DriverManager.quitPrimary();
```

**Design:** No state of its own — delegates storage to `DriverContext`.

---

## `Waiter`

Explicit-wait helpers wrapping `WebDriverWait`:

- Wait for element visibility
- Wait for element clickability
- Wait for element presence
- Custom condition support

---

## Configuration

All driver behavior is controlled by `driver.properties` on the classpath:

```properties
browser=chrome             # chrome | firefox | edge
headless=false
remote=false
gridUrl=                   # http://localhost:4444/wd/hub (when remote=true)
maximize=true
implicitWait=5
pageLoadTimeout=60
scriptTimeout=30
pageLoadStrategy=NORMAL    # NORMAL | EAGER | NONE
acceptInsecureCerts=true
args=--no-sandbox,--disable-dev-shm-usage
```

### Resolution Order (via ConfigLoader)

1. System property (`-Dbrowser=firefox`)
2. Environment variable (`BROWSER=firefox`)
3. Classpath file (`driver.properties`)
4. Hardcoded defaults

---

## Thread Safety

| Class | Thread-safety mechanism |
|-------|------------------------|
| `DriverFactory` | Creates new instances each time — no shared state |
| `DriverContext` | `ThreadLocal` storage — each thread has its own driver |
| `DriverManager` | Delegates to `DriverContext` — inherits thread safety |
| `Waiter` | Stateless utility methods |

---

## Typical Flow

```
VOID.start()
  → FrameworkBootstrap.init()             validate configs
  → DriverManager.createDriver(profile)
      → DriverFactory.fromProfile(profile)
          → reads driver.properties
          → configures capabilities
          → .build()  → WebDriver instance
      → DriverContext.setPrimaryDriver(driver)
  → driver is now accessible via DriverContext.getActiveDriver()
```

---

## See Also

- `core.bootstrap.FrameworkBootstrap` — validates `driver.properties` at startup
- `core.utils.ConfigLoader` — hierarchical config resolution
- `core.engine.selenium.SeleniumEngine` — uses the driver for browser interaction
- `core.runtime.VOID` — orchestrates the startup pipeline

