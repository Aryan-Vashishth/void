# `core.utils` — Cross-Cutting Framework Utilities

Configuration management, enum resolution, data helpers, file I/O, and DOM utilities.

---

## Overview

This package provides general-purpose utilities used across all VOID subsystems. The top-level contains configuration and resolution logic; domain-specific helpers are organized into focused sub-packages.

---

## Package Structure

```
core.utils/
├── ConfigLoader.java      ← Hierarchical config resolution
├── ConfigPaths.java       ← Standard config file path constants
├── EnumResolver.java      ← Normalised name → enum constant lookup
├── ResolvableEnum.java    ← Mixin for enums with human-readable labels
├── UIContext.java         ← (deprecated) Thread-local descriptor state
├── data/
│   ├── DataGenerator.java ← Test data generation (via Datafaker)
│   └── DataVerifier.java  ← Data-level verification with normalization
├── io/
│   ├── FileUtils.java     ← File resolution, download verification
│   ├── json/
│   │   ├── JsonReader.java  ← JSON file reading utilities
│   │   └── JsonLogger.java  ← JSON write/logging utilities
│   └── properties/
│       └── PropertiesReader.java ← Properties file reading
└── web/
    ├── DOMUtils.java          ← JS scroll, highlight, DOM manipulation
    ├── WaitUtils.java         ← Fluent waits, Angular stabilisation
    ├── TableHandler.java      ← Table row/cell read and navigation
    ├── KeyValuePairHandler.java ← Key-value pair interaction
    └── Upload.java            ← File upload support
```

---

## Top-Level Classes

### `ConfigLoader`

Hierarchical configuration with clear resolution order:

```
1. System property    (-Dkey=value)
2. Environment var    (KEY=value)
3. Classpath file     (*.properties)
4. Hardcoded default
```

```java
// Load from classpath
Properties props = ConfigLoader.loadFromClasspath("test.properties");

// Get with fallback
String val = ConfigLoader.get("browser", "chrome");
```

### `ConfigPaths`

Constants for standard configuration file paths:

```java
ConfigPaths.DRIVER_DEFAULT  // "core/driver/config/driver.properties"
ConfigPaths.UTILS_TEST      // "test.properties"
```

### `EnumResolver`

Resolves an enum constant from a normalised string (case-insensitive, underscore/space tolerant):

```java
ElementRole role = EnumResolver.resolve(ElementRole.class, "primary");
ElementRole role = EnumResolver.resolve(ElementRole.class, "PRIMARY");
ElementRole role = EnumResolver.resolve(ElementRole.class, "Primary");  // all work
```

Also supports `ResolvableEnum` custom labels.

### `ResolvableEnum`

Mixin interface for enums that provide a display label:

```java
enum Country implements ResolvableEnum {
    UNITED_STATES("United States"),
    UNITED_KINGDOM("United Kingdom");
    
    private final String label;
    Country(String label) { this.label = label; }
    
    @Override public String getLabel() { return label; }
}

// Can resolve by label OR name:
Country c = EnumResolver.resolve(Country.class, "United States");
```

### `UIContext` *(Deprecated)*

Thread-local holder for the last resolved `LocatorDescriptor`. Retained only for legacy `Interactions` compatibility.

> ⚠️ In the modern Action/Flow path, hooks receive descriptors directly as parameters.

---

## Sub-Package: `data/`

### `DataGenerator`

Test data generation powered by [Datafaker](https://www.datafaker.dev/):

```java
String name  = DataGenerator.fullName();
String email = DataGenerator.email();
String phone = DataGenerator.phoneNumber();
```

### `DataVerifier`

Data-level assertion/verification with normalization and tolerance:

```java
DataVerifier.assertEquals(expected, actual);
DataVerifier.assertContains(haystack, needle);
```

---

## Sub-Package: `io/`

### `FileUtils`

File resolution and download verification:

```java
File file = FileUtils.resolveFromClasspath("uploads/report.pdf");
boolean exists = FileUtils.waitForDownload("report.pdf", Duration.ofSeconds(10));
```

### `io/json/`

| Class | Responsibility |
|-------|----------------|
| `JsonReader` | Reads JSON files from classpath/filesystem |
| `JsonLogger` | Structured JSON logging/writing utilities |

### `io/properties/`

| Class | Responsibility |
|-------|----------------|
| `PropertiesReader` | Reads `.properties` files from classpath |

---

## Sub-Package: `web/`

### `DOMUtils`

JavaScript-based DOM manipulation:

```java
DOMUtils.scrollToElement(driver, element);
DOMUtils.highlightElement(driver, element);
DOMUtils.removeElement(driver, element);
```

### `WaitUtils`

Fluent wait utilities with Angular awareness:

```java
WaitUtils.waitForVisibility(driver, locator, timeout);
WaitUtils.waitForAngularLoader(driver);
WaitUtils.waitForOverlayToDisappear(driver);
```

### `TableHandler`

Table row/cell reading and navigation:

```java
List<Map<String, String>> rows = TableHandler.readTable(driver, tableLocator);
String cellValue = TableHandler.getCellValue(driver, tableLocator, row, col);
```

### `KeyValuePairHandler`

Key-value pair interaction utilities for form-like UI patterns.

### `Upload`

File upload support:

```java
Upload.uploadFile(driver, inputLocator, "path/to/file.pdf");
```

---

## See Also

- `core.bootstrap.FrameworkBootstrap` — consumes ConfigLoader during init
- `core.driver.DriverFactory` — configured via ConfigLoader
- `core.resolvers.locator` — uses file I/O utilities
- `core.engine.selenium.SeleniumEngine` — uses DOMUtils, WaitUtils

