# Configuration Reference

Complete reference for all VOID configuration files, the `ConfigLoader` layering system, and engine configuration.

---

## Configuration Ownership

Every configuration file and key in VOID has a declared owner. No neutral (kernel) component
validates or reads a key that belongs to a domain layer.

| Owner | Config file | ConfigPaths constant | Validated by |
|---|---|---|---|
| **Runtime** (neutral) | `test.properties` | `UTILS_TEST` | `FrameworkBootstrap.init()` |
| **Runtime** (neutral) | *(engine key: `engine`)* | -- | `UIEngineFactory.resolveEngineName()` |
| **Runtime** (neutral) | `log4j2.xml` | -- | Log4j 2 on first log call |
| **Web domain** | `driver.properties` | `DRIVER_DEFAULT` | `SeleniumEngine.initialize()` |
| **Web domain** | `driver-local.properties` | `DRIVER_LOCAL` | `SeleniumEngine.initialize()` overlay |
| **Web domain** | `driver-ci.properties` | `DRIVER_CI` | `SeleniumEngine.initialize()` overlay |
| **Web domain** | `driver-grid.properties` | `DRIVER_GRID` | `SeleniumEngine.initialize()` overlay |

### Validation timing

`FrameworkBootstrap.init()` is domain-neutral: it validates only runtime configuration
(`test.properties`). Web-domain configuration (`driver.properties`) is validated by
`SeleniumEngine.initialize()` at web session creation time -- before any interaction executes,
but after a non-web domain can start without web configuration present. This is a deliberate
design decision (runtime-redesign I5.2 / audit C4).

---

## Table of Contents

1. [Configuration Ownership](#configuration-ownership)
2. [Configuration Layering](#configuration-layering)
3. [`driver.properties` (Web domain)](#driverproperties-web-domain)
4. [`test.properties` (Runtime)](#testproperties-runtime)
5. [Engine Configuration (Runtime)](#engine-configuration-runtime)
6. [Locator Path Configuration](#locator-path-configuration)
7. [Overriding at Runtime](#overriding-at-runtime)
8. [`log4j2.xml` (Runtime / Logging)](#log4j2xml-runtime--logging)

---

## Configuration Layering

`ConfigLoader` resolves property values using a **four-layer hierarchy**. The first match wins:

```
1. System Property       →  -Dbrowser=firefox
2. Environment Variable  →  BROWSER=firefox
3. Classpath Resource    →  core/driver/config/driver.properties (TEST classpath wins over MAIN)
4. Hardcoded Default     →  built into VOID's code
```

### How It Works

```java
// Returns the value from the highest-priority layer
String browser = ConfigLoader.get("browser");

// Returns the value, or the specified default if no layer provides it
String browser = ConfigLoader.get("browser", "chrome");
```

### Classpath Scope

When loading `.properties` files from the classpath, `ConfigLoader` supports three scopes:

| Scope          | Behavior                                                  |
|----------------|-----------------------------------------------------------|
| `MAIN`         | Only loads from `src/main/resources/`                     |
| `TEST`         | Only loads from `src/test/resources/`                     |
| `ANY` (default)| Loads from the thread context classloader (TEST wins)     |

This means you can define production defaults in `src/main/resources/core/driver/config/driver.properties` and override specific values in `src/test/resources/core/driver/config/driver.properties` for test runs.

---

## `driver.properties` (Web domain)

**Owner**: Web domain (`core.engine.selenium`)
**Location**: `src/main/resources/core/driver/config/driver.properties`
**Classpath**: `core/driver/config/driver.properties` (via `ConfigPaths.DRIVER_DEFAULT`)
**Purpose**: Configures the Selenium WebDriver instance created by `DriverFactory`. Required only for web sessions; non-web domains do not need this file.
**Validated by**: `SeleniumEngine.initialize()` at web session creation.

### Full Key Reference

#### Core Settings

| Key                   | Type     | Default    | Description                                            |
|-----------------------|----------|------------|--------------------------------------------------------|
| `browser`             | `String` | `chrome`   | Browser to launch: `chrome`, `firefox`, or `edge`      |
| `headless`            | `boolean`| `false`    | Run browser without a visible window                   |
| `maximize`            | `boolean`| `true`     | Maximize the browser window on launch                  |
| `width`               | `int`    | *(none)*   | Explicit window width (ignored when `maximize=true`)   |
| `height`              | `int`    | *(none)*   | Explicit window height (ignored when `maximize=true`)  |

#### Remote / Grid

| Key                   | Type     | Default    | Description                                            |
|-----------------------|----------|------------|--------------------------------------------------------|
| `remote`              | `boolean`| `false`    | Use a remote WebDriver (Selenium Grid / Selenoid)      |
| `gridUrl`             | `String` | *(empty)*  | Remote hub URL, e.g. `http://localhost:4444/wd/hub`    |

#### Timeouts

| Key                   | Type     | Default    | Description                                            |
|-----------------------|----------|------------|--------------------------------------------------------|
| `implicitWait`        | `int`    | `5`        | Implicit wait timeout in seconds                       |
| `pageLoadTimeout`     | `int`    | `60`       | Page load timeout in seconds                           |
| `scriptTimeout`       | `int`    | `30`       | Async script timeout in seconds                        |
| `pageLoadStrategy`    | `String` | `NORMAL`   | Page load strategy: `NORMAL`, `EAGER`, or `NONE`       |

#### Security

| Key                   | Type     | Default    | Description                                            |
|-----------------------|----------|------------|--------------------------------------------------------|
| `acceptInsecureCerts` | `boolean`| `true`     | Accept self-signed / invalid SSL certificates          |

#### Downloads

| Key                   | Type     | Default    | Description                                            |
|-----------------------|----------|------------|--------------------------------------------------------|
| `downloadsDir`        | `String` | *(empty)*  | Custom download directory path                         |

#### Mobile Emulation (Chrome only)

| Key                       | Type     | Default    | Description                                        |
|---------------------------|----------|------------|----------------------------------------------------|
| `mobileEmulationDevice`   | `String` | *(empty)*  | Chrome DevTools device name, e.g. `Pixel 7`        |

#### Binary Overrides

| Key               | Type     | Default    | Description                                            |
|-------------------|----------|------------|--------------------------------------------------------|
| `chromeBinary`    | `String` | *(empty)*  | Path to a custom Chrome binary                         |
| `firefoxBinary`   | `String` | *(empty)*  | Path to a custom Firefox binary                        |
| `edgeBinary`      | `String` | *(empty)*  | Path to a custom Edge binary                           |

#### Proxy

| Key                  | Type     | Default    | Description                                         |
|----------------------|----------|------------|-----------------------------------------------------|
| `proxy.http`         | `String` | *(empty)*  | HTTP proxy address                                  |
| `proxy.ssl`          | `String` | *(empty)*  | SSL proxy address                                   |
| `proxy.socks`        | `String` | *(empty)*  | SOCKS proxy address                                 |
| `proxy.socksVersion` | `int`    | *(empty)*  | SOCKS proxy version (4 or 5)                        |

#### Browser Arguments

| Key        | Type     | Default    | Description                                                |
|------------|----------|------------|------------------------------------------------------------|
| `args`     | `String` | *(empty)*  | Comma-separated browser arguments, e.g. `--no-sandbox,--disable-dev-shm-usage` |
| `arg.1`    | `String` | *(empty)*  | Numbered argument (alternative to CSV `args`)              |
| `arg.2`    | `String` | *(empty)*  | Additional numbered argument                               |

#### Browser Preferences

| Key                                   | Type     | Default    | Description                            |
|---------------------------------------|----------|------------|----------------------------------------|
| `pref.download.prompt_for_download`   | `String` | *(empty)*  | Browser preference for download prompts|

#### Extra Capabilities

| Key                 | Type     | Default    | Description                                          |
|---------------------|----------|------------|------------------------------------------------------|
| `cap.<name>`        | `String` | *(empty)*  | Add arbitrary capability, e.g. `cap.someCapability`  |

### Example

```properties
browser=chrome
headless=false
remote=false
gridUrl=
maximize=true
implicitWait=5
pageLoadTimeout=60
scriptTimeout=30
pageLoadStrategy=NORMAL
acceptInsecureCerts=true
args=--no-sandbox,--disable-dev-shm-usage
```

### Config Profiles

VOID supports multiple config profiles for different environments:

| Profile | File | Classpath | Use Case |
|---------|------|-----------|----------|
| Default | `driver.properties` | `core/driver/config/driver.properties` | Local development |
| Local   | `driver-local.properties` | `core/driver/config/driver-local.properties` | Developer overrides |
| CI      | `driver-ci.properties` | `core/driver/config/driver-ci.properties` | CI/CD pipelines |
| Grid    | `driver-grid.properties` | `core/driver/config/driver-grid.properties` | Remote Grid execution |

Accessed via `ConfigPaths`:
```java
ConfigPaths.DRIVER_DEFAULT  // "core/driver/config/driver.properties"
ConfigPaths.DRIVER_LOCAL    // "core/driver/config/driver-local.properties"
ConfigPaths.DRIVER_CI       // "core/driver/config/driver-ci.properties"
ConfigPaths.DRIVER_GRID     // "core/driver/config/driver-grid.properties"
```

---

## `test.properties` (Runtime)

**Owner**: Runtime (neutral -- `core.bootstrap`, `core.utils`)
**Location**: `src/main/resources/core/utils/config/test.properties`
**Classpath**: `core/utils/config/test.properties` (via `ConfigPaths.UTILS_TEST`)
**Purpose**: Controls file paths and data-layer settings for test execution. Domain-neutral; loaded by `FrameworkBootstrap.init()` before any session is created.
**Validated by**: `FrameworkBootstrap.init()`.

### Full Key Reference

| Key                              | Type     | Default                    | Description                                          |
|----------------------------------|----------|----------------------------|------------------------------------------------------|
| `json.logger.base.path`         | `String` | `target/logs/json`         | Base directory for JSON log output files             |
| `test.data.base.path`           | `String` | `target/logs/test-data/`   | Base directory for generated test data files         |
| `test.data.fallback.path`       | `String` | `fallback/`                | Path to fallback test data resources                 |
| `upload.base.path`              | `String` | `uploads/`                 | Base directory for file upload test resources         |
| `locators.template.output.dir`  | `String` | `locators/`                | Output directory for generated locator templates     |

### Example

```properties
json.logger.base.path=target/logs/json
test.data.base.path=target/logs/test-data/
test.data.fallback.path=fallback/
upload.base.path=uploads/
locators.template.output.dir=locators/
```

---

## Engine Configuration (Runtime)

**Owner**: Runtime (neutral -- `core.engine.UIEngineFactory`)
**Purpose**: Selects and configures the active `Executor` (execution owner). Domain-neutral; the `engine` key is read before any domain-specific configuration.

### Engine Selection

| Key      | Type     | Default    | Description                                            |
|----------|----------|------------|--------------------------------------------------------|
| `engine` | `String` | `selenium` | Execution engine: `selenium` or `playwright` |

Engine selection priority:
1. System property: `-Dengine=selenium` or `-Dengine=playwright`
2. Environment variable: `ENGINE=selenium` or `ENGINE=playwright`
3. Config file property
4. Default: `selenium`

### Engine Timeout Settings

These are consumed by `EngineConfig` and passed to the active `UIEngine` during initialization:

| Key                    | Type     | Default    | Description                                            |
|------------------------|----------|------------|--------------------------------------------------------|
| `engine.timeout`       | `int`    | `10`       | Default element interaction timeout in seconds         |
| `engine.pollingMs`     | `int`    | `200`      | Polling interval for explicit waits in milliseconds    |
| `engine.baseUrl`       | `String` | *(empty)*  | Base URL for navigation                                |

### Example

```properties
# Engine config (can be in driver.properties or passed via system properties)
engine=selenium
engine.timeout=10
engine.pollingMs=200
engine.baseUrl=https://app.example.com
```

```properties
engine=playwright
engine.timeout=10
engine.pollingMs=200
engine.baseUrl=https://app.example.com
```

### UIEngineFactory

Engine creation is handled by `UIEngineFactory`:

```java
// Automatic engine creation from config (prefer VOID.builder().start() over direct calls)
UIEngine engine = UIEngineFactory.create(config, EngineBootstrap.fromProfile(profile));

// Engine name resolution
String name = UIEngineFactory.resolveEngineName(config);  // "selenium" or "playwright"
```

### EngineConfig Access

```java
EngineConfig config = new EngineConfig(properties);
config.getDefaultTimeout();    // Duration.ofSeconds(10)
config.getPollingInterval();   // Duration.ofMillis(200)
config.getBaseUrl();           // "https://app.example.com"
config.getProperty("engine");  // "selenium" or "playwright"
```

---

## Locator Path Configuration

Locator base paths are configurable via `test.properties` or `ConfigLoader`:

| Key                              | Default                | Description                                      |
|----------------------------------|------------------------|--------------------------------------------------|
| `locator.properties.base.path`   | `locators/properties/` | Classpath root for `.properties` locator files   |
| `locator.json.base.path`         | `locators/json/`       | Classpath root for `.json` locator files         |

These are loaded at startup by `LocatorPaths` and used by all locator source implementations.

---

## Overriding at Runtime

### System Properties

Pass `-D` flags to Maven or the JVM:

```bash
mvn clean test -Dbrowser=firefox -Dheadless=true -DgridUrl=http://grid:4444/wd/hub -Dengine=selenium
mvn clean test -Dengine=playwright
```

### Environment Variables

Set environment variables before running:

```bash
# Linux/macOS
export BROWSER=firefox
export HEADLESS=true
export ENGINE=selenium
mvn clean test

export ENGINE=playwright
mvn clean test

# Windows PowerShell
$env:BROWSER = "firefox"
$env:HEADLESS = "true"
$env:ENGINE = "selenium"
mvn clean test

$env:ENGINE = "playwright"
mvn clean test
```

### Programmatic Access

```java
// Read a config value with default
String browser = ConfigLoader.get("browser", "chrome");

// Load a full properties file from classpath (using ConfigPaths)
Properties props = ConfigLoader.loadFromClasspath(ConfigPaths.DRIVER_DEFAULT);

// Load with explicit scope (TEST classpath only)
Properties testProps = ConfigLoader.loadFromClasspath(
    ConfigPaths.UTILS_TEST,
    ConfigLoader.ClasspathScope.TEST
);
```

---

## `log4j2.xml` (Runtime / Logging)

**Owner**: Runtime / Logging (`core.logging`)
**Location**: `src/main/resources/log4j2.xml` (and `src/test/resources/log4j2.xml` for examples)
**Purpose**: Configures Apache Log4j 2 -- the underlying logging engine.

VOID's `CustomLogger` wraps Log4j 2 but respects its configuration for:
- Log level thresholds
- File appenders (persistent trace logs)
- Console output patterns

> **Note**: ANSI coloring is handled by `CustomLogger` directly (not by Log4j console appenders), so the Log4j pattern does not need ANSI configuration.

---

## Quick Reference

```
ConfigLoader.get("key")          →  System prop → ENV → classpath → null
ConfigLoader.get("key", "def")   →  System prop → ENV → classpath → "def"

Override priority:
  1. -Dkey=value          (highest)
  2. KEY=value (env var)
  3. core/<module>/config/*.properties  (TEST classpath wins over MAIN)
  4. hardcoded default    (lowest)

Config file locations (single source of truth: ConfigPaths.java):
  DRIVER_DEFAULT  = core/driver/config/driver.properties
  DRIVER_LOCAL    = core/driver/config/driver-local.properties
  DRIVER_CI       = core/driver/config/driver-ci.properties
  DRIVER_GRID     = core/driver/config/driver-grid.properties
  UTILS_TEST      = core/utils/config/test.properties

Engine selection:
  UIEngineFactory.resolveEngineName(config)
  Priority: -Dengine → ENV:ENGINE → config property → default ("selenium")
```

---

## Related Documentation

- [System Overview](system-overview.md) — architecture and UIEngine
- [Logging Reference](logging-reference.md) — log channels, folder layout, and run-id naming
- [Quick Start Guide](quick-start.md) — configuring your first test
- [Locator Resolution](locator-resolution.md) — locator path config details
- [`EngineConfig.java`](../../src/main/java/core/engine/EngineConfig.java) — engine config source
- [`UIEngineFactory.java`](../../src/main/java/core/engine/UIEngineFactory.java) — engine creation
- [`ConfigLoader.java`](../../src/main/java/core/utils/ConfigLoader.java) — config layering

---

*MIT License © 2025–2026 VOID Project*
