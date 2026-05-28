# `core.resolvers` — Locator Resolution Subsystem

Role-based locator resolution pipeline supporting JSON and Properties formats.

---

## Overview

Reads element locators from external source files (`.json`, `.properties`, or hardcoded inline) and resolves them into engine-agnostic `LocatorDescriptor` objects at runtime. Supports role-based multi-locator elements, dynamic `%s` template substitution, and automatic format detection.

---

## Package Structure

```
core.resolvers.locator/
├── api/           ← Public API (LocatorResolvers, LocatorResolver, LocatorRequest, LocatorPaths)
├── parser/        ← Raw string → By conversion (ByParser, ByPrefixStrategy)
├── template/      ← LocatorTemplate with STRICT/PAD_LAST policies
├── source/        ← Polymorphic backing sources (LocatorSource interface + implementations)
├── json/          ← JSON format reader + migration tools
└── properties/    ← Properties format reader
```

---

## Quick Start

```java
// Recommended: use the strict resolver
import core.resolvers.locator.api.LocatorResolvers;

// Resolve from an element enum
By by = LocatorResolvers.strict().resolve(myElement);

// Resolve from file + key + args
By by = LocatorResolvers.strict().resolve("login.json", "USERNAME_INPUT", args);

// Engine-agnostic descriptor (preferred for modern code)
LocatorDescriptor d = LocatorResolvers.strict().resolveDescriptor(myElement);
```

---

## Sub-Package Details

### `api/` — Public API

| Class | Responsibility |
|-------|----------------|
| `LocatorResolvers` | Entry point — `strict()` for clean JSON, `legacyPadded()` for legacy properties |
| `LocatorResolver` | Core resolution logic — dispatches to sources, applies templates |
| `LocatorRequest` | Immutable request DTO (file, key, args, hardcoded flag) |
| `LocatorPaths` | Configurable base paths for locator file directories |

### `parser/` — String → By Conversion

| Class | Responsibility |
|-------|----------------|
| `ByParser` | Converts `"xpath=//div"` → `By.xpath("//div")` |
| `ByPrefixStrategy` | Strategy pattern for each prefix token |

**Supported prefixes:** `xpath=`, `css=`, `id=`, `name=`, `tag=`, `linkText=`, `partialLinkText=`

### `template/` — Template Substitution

| Class | Responsibility |
|-------|----------------|
| `LocatorTemplate` | Applies `%s` argument substitution with STRICT or PAD_LAST policy |

```
Template: //input[@placeholder='%s']
Args:     ["username"]
Result:   //input[@placeholder='username']
```

### `source/` — Polymorphic Backing Formats

| Class | Responsibility |
|-------|----------------|
| `LocatorSource` | Interface — contract for all locator file readers |
| `JsonLocatorSource` | Reads from JSON locator files |
| `PropertiesLocatorSource` | Reads from `.properties` files |
| `LayeredPropertiesLocatorSource` | TEST-over-MAIN layered properties |
| `HardcodedLocatorSource` | Returns inline locator strings |
| `LocatorSourceRegistry` | Registry selecting the correct source for a file |

### `json/` — JSON Format

| Class | Responsibility |
|-------|----------------|
| `JsonLocatorReader` | Runtime reader — loads JSON, returns raw locator for a key |
| `JsonLocatorMigrator` | Migration façade — builds resolved JSON from enums |
| `JsonTreeBuilder` | Recursive walker producing Jackson ObjectNode from enum trees |
| `EnumLocatorScanner` | Scans Element enum constants → emits JSON entries |
| `JsonNodeLookup` | Jackson JsonNode traversal helpers (dot-path, deep-find) |
| `PropertiesIndex` | Per-migration cache of .properties files |
| `JsonMigratorCli` | CLI entry point for migration |

### `properties/` — Properties Format

| Class | Responsibility |
|-------|----------------|
| `PropertiesFileLocatorReader` | Reads `.properties` locator files from classpath |

---

## Resolution Flow

```
LocatorResolvers.strict().resolve(element)
  │
  ├── Build LocatorRequest (file, key, args from element)
  ├── LocatorSourceRegistry → select source (JSON or Properties)
  ├── Source → read raw string (e.g., "xpath=//input[@id='%s']")
  ├── LocatorTemplate → apply %s substitution
  ├── ByParser → convert to Selenium By
  └── Return By (or LocatorDescriptor for engine-agnostic path)
```

---

## Locator File Formats

### JSON (Recommended)

```json
{
  "USERNAME_INPUT": "xpath=//input[@id='username']",
  "SUBMIT_BUTTON": "css=#submit-btn",
  "DYNAMIC_FIELD": "xpath=//input[@placeholder='%s']"
}
```

### Properties (Legacy)

```properties
USERNAME_INPUT=xpath=//input[@id='username']
SUBMIT_BUTTON=css=#submit-btn
DYNAMIC_FIELD=xpath=//input[@placeholder='%s']
```

### Location

```
src/main/resources/locators/
├── json/         ← *.json locator files
└── properties/   ← *.properties locator files
```

---

## Role-Based Resolution

Elements can have multiple locator roles:

```json
{
  "STATUS_DROPDOWN": {
    "TRIGGER": "xpath=//button[@data-role='trigger']",
    "LIST": "xpath=//ul[@role='listbox']",
    "SEARCH_INPUT": "xpath=//input[@placeholder='Search']"
  }
}
```

Resolved via:
```java
By trigger = LocatorResolvers.strict().resolve(element, ElementRole.TRIGGER);
By list    = LocatorResolvers.strict().resolve(element, ElementRole.LIST);
```

---

## See Also

- `core.engine.LocatorDescriptor` — the engine-agnostic output of resolution
- `core.engine.LocatorStrategy` — locator type enum
- `elements.meta.ElementRole` — role enum for multi-locator elements
- `core.interactions.Via` — helper for ad-hoc resolution

