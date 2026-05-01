# Configuration Reference

Complete reference for all VOID configuration files and the `ConfigLoader` layering system.

---

## Table of Contents

1. [Configuration Layering](#configuration-layering)
2. [`driver.properties`](#driverproperties)
3. [`test.properties`](#testproperties)
4. [`extent.properties`](#extentproperties)
5. [Locator Path Configuration](#locator-path-configuration)
6. [Overriding at Runtime](#overriding-at-runtime)
7. [`log4j2.xml`](#log4j2xml)

---

## Configuration Layering

`ConfigLoader` resolves property values using a **four-layer hierarchy**. The first match wins:

```
1. System Property       →  -Dbrowser=firefox
2. Environment Variable  →  BROWSER=firefox
3. Classpath Resource    →  config/driver.properties (TEST classpath wins over MAIN)
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

This means you can define production defaults in `src/main/resources/config/driver.properties` and override specific values in `src/test/resources/config/driver.properties` for test runs.

---

## `driver.properties`

**Location**: `src/main/resources/config/driver.properties`  
**Purpose**: Configures the Selenium WebDriver instance created by `DriverFactory`.

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

---

## `test.properties`

**Location**: `src/main/resources/config/test.properties`  
**Purpose**: Controls file paths and data-layer settings for test execution.

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

## `extent.properties`

**Location**: `src/main/resources/extent.properties`  
**Purpose**: Configures the Extent Reports HTML reporter (via `extentreports-cucumber7-adapter`).

### Common Keys

| Key                                      | Description                                    |
|------------------------------------------|------------------------------------------------|
| `extent.reporter.spark.start`            | Enable/disable Spark HTML reporter             |
| `extent.reporter.spark.out`              | Output path for the Spark report               |
| `extent.reporter.spark.config`           | Path to an XML config file for Spark theme     |

Reports are generated at `target/ExtentReports/SparkReports/` by default.

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
mvn clean test -Dbrowser=firefox -Dheadless=true -DgridUrl=http://grid:4444/wd/hub
```

### Environment Variables

Set environment variables before running:

```bash
# Linux/macOS
export BROWSER=firefox
export HEADLESS=true
mvn clean test

# Windows PowerShell
$env:BROWSER = "firefox"
$env:HEADLESS = "true"
mvn clean test
```

### Programmatic Access

```java
// Read a config value with default
String browser = ConfigLoader.get("browser", "chrome");

// Load a full properties file from classpath
Properties props = ConfigLoader.loadFromClasspath("config/driver.properties");

// Load with explicit scope (TEST classpath only)
Properties testProps = ConfigLoader.loadFromClasspath(
    "config/test.properties",
    ConfigLoader.ClasspathScope.TEST
);
```

---

## `log4j2.xml`

**Location**: `src/main/resources/log4j2.xml` (and `src/test/resources/log4j2.xml` for tests)  
**Purpose**: Configures Apache Log4j 2 — the underlying logging engine.

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
  3. config/*.properties  (TEST classpath wins over MAIN)
  4. hardcoded default    (lowest)
```

