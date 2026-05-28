# `core.engine` — Engine Abstraction Layer

Decouples VOID's interaction layer from any specific browser automation library.

---

## Overview

The engine package defines the **execution contract** that all browser automation backends must implement. By programming against `UIEngine` (an interface), VOID's entire test layer — Actions, Flows, Interactions — remains engine-agnostic. Swapping from Selenium to Playwright requires only a new engine implementation, with zero changes to test code.

---

## Class Inventory

| Class | Type | Responsibility |
|-------|------|----------------|
| `UIEngine` | Interface | The execution contract — click, type, select, hover, resolve, etc. |
| `LocatorDescriptor` | Value Object | Engine-agnostic locator representation (strategy + value + metadata) |
| `LocatorStrategy` | Enum | Locator type: XPATH, CSS, ID, NAME, TAG, LINK_TEXT, PARTIAL_LINK_TEXT |
| `EngineConfig` | Config | Engine initialization parameters |
| `UIEngineFactory` | Factory | Creates engine instances from configuration |

### Sub-Package: `core.engine.selenium`

| Class | Responsibility |
|-------|----------------|
| `SeleniumEngine` | Default production implementation — translates descriptors to Selenium `By` |

---

## `UIEngine` — The Execution Contract

The single authority for all browser interaction. Key responsibilities:

| Category | Methods (examples) |
|----------|-------------------|
| **Element actions** | `click(descriptor)`, `type(descriptor, text)`, `select(descriptor, value)` |
| **Reading** | `readText(descriptor)`, `readAttribute(descriptor, attr)` |
| **Resolution** | `resolve(element, role)` → `LocatorDescriptor` |
| **Navigation** | `navigateTo(url)`, `refresh()` |
| **Waits** | Internally managed — callers must NOT wait |

### Execution Ownership

UIEngine **owns ALL execution concerns**:
- ✅ Scrolling elements into view
- ✅ Explicit waits (visibility, clickability)
- ✅ Retry on transient failures (stale element, intercepted click)
- ✅ JavaScript fallbacks when standard interactions fail

**Callers must NOT:**
- ❌ Perform their own scrolling
- ❌ Add explicit waits before calling engine methods
- ❌ Implement retry logic

---

## `LocatorDescriptor`

Engine-agnostic value object representing a resolved locator:

```java
LocatorDescriptor descriptor = engine.resolve(element, ElementRole.PRIMARY);
// descriptor.strategy() → XPATH
// descriptor.value()    → "//button[@id='submit']"
```

**Properties:**
- `strategy` — the locator type (from `LocatorStrategy` enum)
- `value` — the locator expression string
- Additional metadata (file, key, original template)

---

## `LocatorStrategy`

Enum of supported locator types:

| Value | Prefix | Example |
|-------|--------|---------|
| `XPATH` | `xpath=` | `//div[@class='card']` |
| `CSS` | `css=` | `.user-card .email` |
| `ID` | `id=` | `submitBtn` |
| `NAME` | `name=` | `username` |
| `TAG` | `tag=` | `input` |
| `LINK_TEXT` | `linkText=` | `Sign In` |
| `PARTIAL_LINK_TEXT` | `partialLinkText=` | `Sign` |

---

## `UIEngineFactory`

Creates engine instances from configuration:

```java
UIEngine engine = UIEngineFactory.create(config, driver);
// Reads 'engine' property from config → selects implementation
// Default: SeleniumEngine
```

---

## `SeleniumEngine` (sub-package)

The default (and currently sole production) implementation:

```
LocatorDescriptor
  → Convert to Selenium By (By.xpath, By.cssSelector, etc.)
  → WebDriverWait for condition
  → Scroll into view (JS)
  → Execute action
  → Retry on StaleElementReferenceException
  → JS fallback on ElementClickInterceptedException
```

**Key design:** This is the **only place in VOID** that directly depends on `org.openqa.selenium` for element interaction.

---

## Adding a New Engine

1. Create sub-package: `core.engine.playwright/`
2. Implement `UIEngine` interface
3. Register in `UIEngineFactory`
4. No changes to test-level code needed

---

## Architecture Invariant

```
Actions / Flows / Interactions
        │
        ▼ (always through UIEngine interface)
    ┌─────────┐
    │ UIEngine │  ← The single execution authority
    └────┬────┘
         │
    ┌────┴────────────┐
    │ SeleniumEngine   │  (or PlaywrightEngine, etc.)
    └─────────────────┘
```

---

## See Also

- `core.actions` — deferred operations that call `engine.perform()`
- `core.executor.FlowExecutor` — iterates and delegates to engine
- `core.resolvers.locator` — resolves locator descriptors
- `core.driver` — provides the WebDriver that SeleniumEngine uses
- ADR-007: UIEngine as Single Execution Authority

