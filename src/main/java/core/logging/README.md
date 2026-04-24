# `core.logging` — CustomLogger Package

A structured, ANSI-colored, theme-aware logger for Selenium/TestNG automation frameworks.
Wraps **Apache Log4j** with semantic action methods, multiple built-in color themes, and a
clean object-oriented architecture — all without requiring any external logging framework changes.

---

## Table of Contents

1. [Quick Start](#1-quick-start)
2. [Architecture Overview](#2-architecture-overview)
3. [Class Reference](#3-class-reference)
   - [AnsiEscape](#ansiescape)
   - [AnsiColors](#ansicolors)
   - [LogIntent](#logintent)
   - [LogTheme](#logtheme)
   - [ThemeColors](#themecolors)
   - [BuiltInThemes](#builtinthemes)
   - [LogConfig](#logconfig)
   - [LoggerContext](#loggercontext)
   - [LogActions](#logactions)
   - [CustomLogger](#customlogger)
   - [ConsoleOnly](#consoleonly)
4. [Log Levels](#4-log-levels)
5. [Action Methods (LogIntent Groups)](#5-action-methods-logintent-groups)
6. [Themes](#6-themes)
7. [Structured Output](#7-structured-output)
8. [Custom Themes](#8-custom-themes)
9. [Configuration (LogConfig)](#9-configuration-logconfig)
10. [ANSI & Caller-Color Control](#10-ansi--caller-color-control)
11. [Call-Chain Filtering](#11-call-chain-filtering)
12. [Testing](#12-testing)

---

## 1. Quick Start

```java
import static core.logging.CustomLogger.*;

// Initialize once (usually in @BeforeClass or @BeforeSuite)
initialize(MyTestClass.class);

// Basic level logs
info.log("Test started");
warn.log("Slow response detected");
error.log("Assertion failed");
debug.log("Element located");

// Semantic action methods
info.click("Submit button");
info.tab("Dashboard tab");
info.wait("Loading spinner to disappear");
info.success("Login completed");
info.error("Element not found");

// Structured key/value tree
info.log("Request", fields(
    "method",   "POST",
    "endpoint", "/api/login",
    "timeout",  5000
));

// ASCII table
info.table(List.of(
    fields("ID", 1, "Name", "Alice", "Role", "Admin"),
    fields("ID", 2, "Name", "Bob",   "Role", "User")
), "Users");

// Switch theme
setTheme(LogTheme.COCKPIT);

// Configure via LogConfig (new preferred API)
CustomLogger.configure(
    LogConfig.builder()
        .theme(LogTheme.COCKPIT)
        .tableCellLimit(60)
        .callerColor(false)
        .build()
);
```

---

## 2. Architecture Overview

The package is split into **10 single-responsibility classes** — each with one clear job:

```
core.logging/
│
├── AnsiEscape.java       ← Stateless factory for ANSI escape sequences (rgbFg, fg256, sgr, …)
├── AnsiColors.java       ← Pure data catalog of named color constants (built via AnsiEscape)
├── ConsoleOnly.java      ← @ConsoleOnly annotation — marks terminal-only features
├── LogIntent.java        ← Enum: semantic intent of a log line (INTERACTION, DATA, …)
├── LogTheme.java         ← Enum: theme catalogue (PLAIN, COCKPIT, MODERN_CLEAN, …)
├── ThemeColors.java      ← Immutable theme model + fluent builder
├── BuiltInThemes.java    ← 8 pre-built theme instances + active-theme registry
├── LogConfig.java        ← Central config object (ANSI, theme, cell limit, filters, divider, …)
├── LoggerContext.java    ← Log4j logger holder + ANSI/caller-color delegates into LogConfig
├── LogActions.java       ← All action methods base class (click, table, success, …)
└── CustomLogger.java     ← Public facade — debug/info/warn/error instances + config API
```

> **Design note (SRP):** `AnsiEscape` owns the *behavior* of building ANSI strings (and validates
> all inputs). `AnsiColors` owns the *data* — a stable catalog of named constants, every one of
> which is built by an `AnsiEscape` factory call. The escape grammar (`\u001B[…m`) lives in
> exactly one place.

### Rendering formula

Every log line is composed as:

```
intentFg + levelBg  →  rendered line  →  RESET
```

- **`levelBg`** — background color for the **entire** line, driven by log level (`info.*`, `warn.*`, …).
- **`intentFg`** — foreground color driven by **what the line communicates** (`click`, `success`, `error`, …).
- Both are resolved together by `ThemeColors.resolve(logLevel, intent)`.

Log line format:

```
[yyyy-MM-dd HH:mm:ss.SSS] │ ACTION_LABEL │ message text
```

The segment divider (`│`) and timestamp format are configurable via `LogConfig`.

---

## 3. Class Reference

### `AnsiEscape`

Stateless **factory** for ANSI SGR (Select Graphic Rendition) escape sequences. The single
source of truth for *building* escape strings — every constant in `AnsiColors` is produced
by one of these methods.

| API | Purpose |
|---|---|
| `RESET`, `BOLD`, `DIM`, `ITALIC` | SGR control constants |
| `sgr(int... codes)` | Generic builder — `\u001B[c1;c2;…m`. Used for combos like `sgr(38, 5, 208, 1)` (256-color orange + bold). |
| `fg16(int code)` / `bg16(int code)` | Standard 16-color FG (30–37 / 90–97) / BG (40–47 / 100–107) with range validation |
| `fg256(int code)` / `bg256(int code)` | 256-color palette FG/BG (0–255) with range validation |
| `rgbFg(int r, int g, int b)` / `rgbBg(int r, int g, int b)` | True-color (24-bit) FG/BG with 0–255 validation |
| `rgbFg(int rgb)` / `rgbBg(int rgb)` | Packed `0xRRGGBB` overloads |
| `colorize(text, fg)` / `colorize(text, fg, bg)` | Wrap text with style + auto-`RESET` |

All builders **validate at call time** — an out-of-range value throws `IllegalArgumentException`
immediately rather than producing a silently broken escape sequence at runtime.

```java
import static core.logging.AnsiEscape.*;

// Build colors on the fly
String brand   = rgbFg(255, 136, 0);          // bespoke orange FG
String banner  = rgbBg(0x101820);             // packed-int BG
String bgRed   = bg16(41);                    // standard red BG
String warning = sgr(38, 5, 208, 1);          // 256-color orange + bold

// Compose with auto-reset
System.out.println(colorize("WARNING", warning, RESET));
System.out.println(colorize("Build complete", rgbFg(80, 255, 120)));
```

---

### `AnsiColors`

**Pure data catalog** of named color constants — contains no behavior. Every constant is
built by an `AnsiEscape` factory call so the escape grammar exists in exactly one place.

Three tiers of constants:

| Tier | Built via | Example |
|---|---|---|
| Standard 16-color | `fg16(n)` / `bg16(n)` | `FG_BRIGHT_WHITE`, `BG_BLACK` |
| 256-color | `fg256(n)` / `bg256(n)` | `FG_256_ORANGE`, `BG_256_DARK_GREEN` |
| True RGB | `rgbFg(r,g,b)` / `rgbBg(r,g,b)` | `RGB_FG_SNOW_WHITE`, `RGB_BG_CRIMSON` |

The control constants (`RESET`, `BOLD`, `DIM`, `ITALIC`) are re-exported from `AnsiEscape`
for convenient single-import use.

```java
// Use directly or via static import
import static core.logging.AnsiColors.*;

String myStyle = RGB_FG_LIME_GREEN + BOLD + RGB_BG_DARK_FOREST;
```

> **When to use which:**
> - Reach for **`AnsiColors.*`** when you want a named, semantic palette entry.
> - Reach for **`AnsiEscape.*`** when you need an ad-hoc color from raw RGB / 256-palette
>   indices, or to build SGR combos.

---

### `LogIntent`

Enum that classifies **what** a log line is communicating — independently of the log level.

| Value | Methods that use it |
|---|---|
| `BASE` | `log()` — plain label lines |
| `INTERACTION` | `click`, `checkbox`, `text`, `input`, `dropdown`, `toggle`, `upload` |
| `NAVIGATION` | `tab`, `frame`, `breadcrumb` |
| `OBSERVE` | `wait`, `search`, `result` |
| `DATA` | `table`, `grid`, `row` |
| `SUCCESS` | `success`, `complete`, `resolved` |
| `ALERT` | `error`, `failed`, `timeout`, `validation`, `fallback`, `skip` |

---

### `LogTheme`

Enum of all available built-in themes. Pass to `CustomLogger.setTheme(LogTheme)`.

| Value | Personality |
|---|---|
| `PLAIN` ⭐ | Standard 16-color ANSI. Default. Renders correctly everywhere. |
| `SOLARIZED_DARK` | Ethan Schoonover's Solarized Dark — reduced eye-strain. |
| `HIGH_CONTRAST` | Pure-black BGs + max-luminance FGs. WCAG AA+, projectors. |
| `MODERN_CLEAN` | VS Code Dark+ style — neutral dark slates, vivid intent rainbow. |
| `INDUSTRIAL_STEEL` | Factory-floor HMI — brushed steel INFO, safety-yellow WARN, alarm-red ERROR. |
| `NIGHT_CLUB` | Vivid neon — jewel-tone BGs, hot-pink/sky-blue/neon-green intent FGs. |
| `CARBON_ORANGE` | Carbon-fibre dark + amber/orange accent system. |
| `COCKPIT` | Mission-control — semantic status-light BGs (green/amber/maroon/dark-grey). |

---

### `ThemeColors`

Immutable value object holding the full color definition of one theme.
Built via the fluent **builder pattern**.

```java
ThemeColors myTheme = ThemeColors.builder()
    // Level backgrounds
    .infoBg (RGB_BG_CHARCOAL)
    .warnBg (RGB_BG_DARK_AMBER)
    .errorBg(RGB_BG_CRIMSON)
    .debugBg(RGB_BG_MIDNIGHT)
    // Level foregrounds (BASE intent)
    .infoFg (RGB_FG_SNOW_WHITE + BOLD)
    .warnFg (RGB_FG_GOLD       + BOLD)
    .errorFg(RGB_FG_CORAL      + BOLD)
    .debugFg(RGB_FG_COOL_GREY)
    // Intent foregrounds
    .interactionFg(RGB_FG_SNOW_WHITE  + BOLD)
    .navigationFg (RGB_FG_STEEL_CYAN  + BOLD)
    .observeFg    (RGB_FG_GOLD        + BOLD)
    .dataFg       (RGB_FG_LAVENDER    + BOLD)
    .successFg    (RGB_FG_LIME_GREEN  + BOLD)
    .alertFg      (RGB_FG_CORAL       + BOLD)
    .callerFg     (RGB_FG_COOL_GREY)           // @ConsoleOnly suffix
    .build();

// Apply it
CustomLogger.setCustomTheme(myTheme);
```

**Key method:**

```java
String ansiStyle = myTheme.resolve("INFO", LogIntent.INTERACTION);
// returns: interactionFg + infoBg  (ready to prepend to a log line)
```

---

### `BuiltInThemes`

Holds all 8 pre-built `ThemeColors` instances as `public static final` fields and manages the
**active theme**.

```java
// Switch via CustomLogger (preferred)
CustomLogger.setTheme(LogTheme.COCKPIT);

// Or set a fully custom theme
CustomLogger.setCustomTheme(myTheme);

// Read the current theme
LogTheme active = CustomLogger.getCurrentTheme();

// Access a built-in theme directly for inspection
ThemeColors cockpit = BuiltInThemes.COCKPIT;
```

---

### `LogConfig`

**New in this version.** Central configuration object — the single source of truth for all
mutable runtime settings. Replaces the scattered flags that previously lived in `LoggerContext`.

| Setting | Default | Description |
|---|---|---|
| `theme` | `LogTheme.PLAIN` | Active color theme |
| `ansi` | auto-detected | ANSI color output on/off |
| `callerColor` | `false` | Dim caller-trace suffix (`@ConsoleOnly`) |
| `segmentDivider` | `" │ "` | Separator between log segments |
| `tsFormat` | `"yyyy-MM-dd HH:mm:ss.SSS"` | Timestamp pattern |
| `tableCellLimit` | `40` (disabled) | Max cell width — **off by default** |
| `suppressContains` | framework internals | Class-name substrings to suppress in caller trace |
| `suppressMethodPrefixes` | `log`, `debug`, `info`, … | Method prefixes to suppress |
| `includeOnlyPrefixes` | *(empty)* | If set, only these package prefixes appear in trace |

**Builder (initial setup):**

```java
LogConfig.builder()
    .theme(LogTheme.COCKPIT)
    .ansi(true)
    .callerColor(false)
    .segmentDivider(" │ ")
    .tsFormat("yyyy-MM-dd HH:mm:ss.SSS")
    .tableCellLimit(60)          // also enables truncation
    .suppressContaining("com.example.proxy")
    .includeOnlyPackages("automation.", "steps.")
    .build()
    .apply();                    // makes this the live config
```

**Runtime toggle (no rebuild needed):**

```java
LogConfig.current().setTheme(LogTheme.HIGH_CONTRAST);
LogConfig.current().setAnsi(true);
LogConfig.current().setTableCellLimit(60);
LogConfig.current().disableTableCellLimit();   // unlimited
```

**Patch a single field:**

```java
LogConfig.patch(c -> c.setTheme(LogTheme.HIGH_CONTRAST));
```

**Via `CustomLogger` facade:**

```java
// Full config object
CustomLogger.configure(
    LogConfig.builder().theme(LogTheme.COCKPIT).tableCellLimit(60).build()
);

// Mutator lambda
CustomLogger.configure(c -> c.setTheme(LogTheme.HIGH_CONTRAST).enableCallerColor());

// Direct access
CustomLogger.config().setAnsi(true);
```

> **Table cell truncation is disabled by default.** To enable it, call `.tableCellLimit(n)`
> on the builder or `LogConfig.current().setTableCellLimit(n)` at runtime. Cell values
> longer than the limit are truncated with `...`.

---

### `LoggerContext`

Singleton holder for the **Log4j `Logger` instance**. Runtime flags (ANSI, caller-color,
filters) are now delegated to `LogConfig` — `LoggerContext` exists mainly to provide
the Log4j logger and a handful of compatibility delegates.

| What it holds | How to configure it |
|---|---|
| Log4j `Logger` instance | `LoggerContext.initLogger(Class<?>)` |
| ANSI enabled delegate | `LoggerContext.enableAnsi()` / `disableAnsi()` → `LogConfig.current()` |
| Caller-color delegate | `LoggerContext.enableCallerColor()` → `LogConfig.current()` |
| Call-chain filter delegates | `SUPPRESS_CONTAINS`, `SUPPRESS_METHOD_PREFIXES`, `INCLUDE_ONLY_PREFIXES` |

> ℹ️ Prefer `LogConfig.current()` or `CustomLogger.configure()` for all configuration.
> `LoggerContext` methods are kept for backward compatibility.

---

### `LogActions`

Base class for `CustomLogger.Info`, `Warn`, `Error`, and `Debug`. Contains **all action methods**
and the full rendering pipeline.

**You don't instantiate `LogActions` directly** — use `CustomLogger.info`, `CustomLogger.warn`, etc.

Rendering pipeline:
```
logMessage(intent, label, text)
    └─ resolve ANSI style via BuiltInThemes.getColors().resolve(logLevel, intent)
    └─ logMultiline(style, label, text, showCaller)
        └─ split on newlines
        └─ wrap each line: style + "[timestamp] │ [LABEL] │ text" + RESET
        └─ dispatch to Log4j at the correct level
```

---

### `CustomLogger`

The **public facade** — the only class most callers need to import.

```java
// Logger instances (use via static import)
public static final Debug debug;
public static final Info  info;
public static final Warn  warn;
public static final Error error;

// Configuration (new LogConfig API)
CustomLogger.configure(LogConfig)          // replace live config with a built instance
CustomLogger.configure(Consumer<LogConfig>)// patch live config in-place
CustomLogger.config()                      // access live LogConfig directly

// Log4j logger
CustomLogger.initialize(Class<?>)          // set Log4j logger category

// ANSI (also available via LogConfig)
CustomLogger.enableAnsi() / disableAnsi()

// Theme shortcuts (also available via LogConfig)
CustomLogger.setTheme(LogTheme)            // switch built-in theme
CustomLogger.setCustomTheme(ThemeColors)   // apply fully custom theme
CustomLogger.getCurrentTheme()             // read active theme

// Caller-color (@ConsoleOnly)
CustomLogger.enableCallerColor()
CustomLogger.disableCallerColor()

// Call-chain filtering (also available via LogConfig)
CustomLogger.includeOnlyPackages(String…)
CustomLogger.suppressClassContains(String…)
CustomLogger.clearIncludes()

// Helper
CustomLogger.fields(Object… pairs)         // builds LinkedHashMap for tree/table

// Experimental utilities
CustomLogger.Experimental.stripAnsi(String)
CustomLogger.Experimental.fgFromStyle(String)
```

---

### `ConsoleOnly`

Annotation marking methods or fields that are **only safe when output goes to a live
ANSI-capable terminal**.

```java
@ConsoleOnly
public static void enableCallerColor() { … }
```

> ⚠️ **Do NOT enable `@ConsoleOnly` features in CI or file-appender runs.**
> They break the one-ANSI-block-per-line contract, causing split entries in
> IntelliJ Test History and raw escape codes in log files.

---

## 4. Log Levels

| Instance | Log4j level | Level BG | Level FG (PLAIN theme) |
|---|---|---|---|
| `CustomLogger.info` | INFO | deep steel grey | bright white |
| `CustomLogger.warn` | WARN | deep amber | black |
| `CustomLogger.error` | ERROR | deep crimson | bright white |
| `CustomLogger.debug` | DEBUG | near-black | white |

Each action method (e.g. `click`) automatically uses the **calling instance's level BG**,
so the same action looks different at each level without any extra configuration:

```java
info.click("Submit");   // interactionFg + infoBg  (grey background)
warn.click("Submit");   // interactionFg + warnBg  (amber background)
error.click("Submit");  // interactionFg + errorBg (crimson background)
debug.click("Submit");  // interactionFg + debugBg (near-black background)
```

---

## 5. Action Methods (LogIntent Groups)

### BASE — plain log lines
```java
info.log("message");
info.log("Heading", fields("key", "value", "key2", 42));

// Object overloads
info.log(someObject);                    // toString, or delegates to table/list
info.log("Label", someObject);           // heading + object
info.log(List.of("a", "b", "c"));       // indexed list output
```

### INTERACTION
```java
info.click("Submit button");
info.checkbox("Accept Terms");
info.text("alice@example.com");
info.input("Display name");
info.dropdown("Country: Australia");
info.toggle("Dark mode ON");
info.upload("report.pdf");
```

### NAVIGATION
```java
info.tab("Dashboard");
info.frame("payment-iframe");
info.breadcrumb("Home / Users / Edit");
```

### OBSERVE
```java
info.wait("Spinner to disappear (10 s)");
info.search("Searched for 'alice'");
info.result("42 records returned");
```

### DATA
```java
info.table("Table label");
info.grid("Grid label");
info.row(fields("Key", "Value", "Key2", "Value2"));

// Render a full ASCII table
info.table(List.of(
    fields("ID", 1, "Name", "Alice", "Role", "Admin"),
    fields("ID", 2, "Name", "Bob",   "Role", "User")
), "Optional Title");

// Single-row table
info.table(fields("Host", "localhost", "Port", 8080), "Server Config");
```

### SUCCESS
```java
info.success("Login completed");
info.complete("Checkout flow finished");
info.resolved("CSS locator found");

// With structured fields
info.resolved("Locator resolved", fields(
    "strategy", "CSS",
    "value",    "#save-btn",
    "retries",  1
));
```

### ALERT
```java
info.error("Element not found on page");
info.failed("Step 'clickSubmit' — FAILED");
info.timeout("Element did not appear within 10 s");
info.validation("Email format is invalid");
info.fallback("Primary locator failed, using XPath");
info.skip("Skipping optional cookie banner");
```

### TREE (key/value structured output)
```java
info.tree("Request payload", fields(
    "method",   "POST",
    "endpoint", "/api/login",
    "timeout",  5000
));

// Varargs shorthand
info.tree("Server Config", "host", "localhost", "port", 8080);
```

---

## 6. Themes

### Switching themes

```java
// Built-in themes
CustomLogger.setTheme(LogTheme.PLAIN);            // ⭐ default
CustomLogger.setTheme(LogTheme.SOLARIZED_DARK);
CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);
CustomLogger.setTheme(LogTheme.MODERN_CLEAN);
CustomLogger.setTheme(LogTheme.INDUSTRIAL_STEEL);
CustomLogger.setTheme(LogTheme.NIGHT_CLUB);
CustomLogger.setTheme(LogTheme.CARBON_ORANGE);
CustomLogger.setTheme(LogTheme.COCKPIT);

// Custom theme
CustomLogger.setCustomTheme(myTheme);

// Via LogConfig
CustomLogger.configure(c -> c.setTheme(LogTheme.COCKPIT));

// Revert to PLAIN
CustomLogger.setTheme(LogTheme.PLAIN);
```

### Theme anatomy (COCKPIT example)

| Level | Background | FG (BASE) | Description |
|---|---|---|---|
| INFO | `rgb(18,52,28)` dark forest green | lime green | "all systems go" |
| WARN | `rgb(95,62,0)` dark amber | gold | "caution" |
| ERROR | `rgb(90,16,36)` deep wine | snow white | "critical alert" |
| DEBUG | `rgb(20,22,30)` near-black | cool grey | "diagnostic" |

Intent foregrounds (same across all level BGs in COCKPIT):

| Intent | Color |
|---|---|
| INTERACTION | snow white |
| NAVIGATION | mint |
| OBSERVE | gold |
| DATA | lavender ⭐ signature |
| SUCCESS | lime green |
| ALERT | coral |

---

## 7. Structured Output

### Key/value tree

```java
info.tree("Request payload", fields(
    "method",   "POST",
    "endpoint", "/api/login",
    "body",     "{\"user\":\"alice\"}",
    "timeout",  5000
));
```

Output:
```
2026-04-24 13:15:37.584 │ INFO │ === InteractionsEndToEndTest starting === │ InteractionsEndToEndTest.setupClass ← TestMethodWorker.run
2026-04-24 13:15:37.663 │ DEBUG │ Setting driver for key: primary │ DriverContext.setPrimaryDriver ← Interactions.(constructor)
2026-04-24 13:15:37.668 │ DEBUG │ [get] key=locator.properties.base.path src=DEFAULT val=locators/properties/ │ ConfigLoader.get ← LocatorPaths.(static init)
2026-04-24 13:15:37.668 │ DEBUG │ [get] key=locator.json.base.path src=DEFAULT val=locators/json/ │ ConfigLoader.get ← LocatorPaths.(static init)
2026-04-24 13:15:37.672 │ DEBUG │ [LOCATOR] Resolving: │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ File        : test-locators.properties │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ Key         : TEMPLATE_WITH_ARG │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           ├─ Args        : [username] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           └─ Hardcoded   : false │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.678 │ DEBUG │ [LOCATOR] Final: │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           ├─ Key         : TEMPLATE_WITH_ARG │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           ├─ Resolved    : //input[@placeholder='username'] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.679 │ DEBUG │           └─ By          : By.xpath: //input[@placeholder='username'] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.685 │ DEBUG │ Getting driver for key: primary │ DriverContext.getDriver ← DOMUtils.scrollToElement
2026-04-24 13:15:37.688 │ TEXT [T] │ Appended to 'username': -extra │ Interactions.appendTo ← InteractionsEndToEndTest.interactions_appendTo_doesNotClearButTypes
```

### ASCII table

```java
info.table(List.of(
    fields("Tag",   "TI-204", "Value", "182.4°C", "Status", "NORMAL"),
    fields("Tag",   "PI-112", "Value", "4.2 bar", "Status", "NORMAL"),
    fields("Tag",   "FI-310", "Value", "0 L/min", "Status", "LOW FLOW")
), "Process Values");
```

Output:
```
+--------+----------+----------+
|      Process Values          |
+--------+----------+----------+
| Tag    | Value    | Status   |
+--------+----------+----------+
| TI-204 | 182.4°C  | NORMAL   |
| PI-112 | 4.2 bar  | NORMAL   |
| FI-310 | 0 L/min  | LOW FLOW |
+--------+----------+----------+
```

> **Cell truncation is disabled by default.** Enable it via
> `LogConfig.current().setTableCellLimit(40)` or in the builder.
> When enabled, cell values longer than the limit are truncated with `...`.

---

## 8. Custom Themes

### Full example

```java
import static core.logging.AnsiColors.*;

ThemeColors myTheme = ThemeColors.builder()
    // ── Level backgrounds ────────────────────────────────────
    .infoBg ("\u001B[48;2;20;30;50m")   // custom deep navy
    .warnBg (RGB_BG_DARK_AMBER)
    .errorBg(RGB_BG_CRIMSON)
    .debugBg(RGB_BG_MIDNIGHT)
    // ── Level foregrounds (BASE intent) ──────────────────────
    .infoFg (RGB_FG_SKY_BLUE   + BOLD)
    .warnFg (RGB_FG_GOLD       + BOLD)
    .errorFg(RGB_FG_CORAL      + BOLD)
    .debugFg(RGB_FG_COOL_GREY)
    // ── Intent foregrounds ────────────────────────────────────
    .interactionFg(RGB_FG_SNOW_WHITE + BOLD)
    .navigationFg (RGB_FG_ELECTRIC_BLUE + BOLD)
    .observeFg    (RGB_FG_GOLD       + BOLD)
    .dataFg       (RGB_FG_LAVENDER   + BOLD)
    .successFg    (RGB_FG_LIME_GREEN + BOLD)
    .alertFg      (RGB_FG_CORAL      + BOLD)
    .callerFg     (RGB_FG_COOL_GREY)
    .build();

CustomLogger.setCustomTheme(myTheme);

// Now all log output uses myTheme
info.click("Using my custom theme");

// Revert to a built-in theme
CustomLogger.setTheme(LogTheme.PLAIN);
```

### Contrast guidelines

- Intent foregrounds are composited on **all four level backgrounds** simultaneously.
- Choose FGs that remain readable on every level BG you define, or accept that some
  combinations (e.g. `warn.click()`) may be sub-optimal.
- Minimum recommended contrast ratio: **WCAG 3:1** between every FG+BG pair.

---

## 9. Configuration (LogConfig)

`LogConfig` is the **single source of truth** for all logging settings.

### Builder reference

```java
LogConfig.builder()
    // ── Column widths (0 = free-flow, no padding) ──────────
    .tsWidth(23)
    .levelWidth(7)
    .actionWidth(18)
    // ── Timestamp ──────────────────────────────────────────
    .tsFormat("yyyy-MM-dd HH:mm:ss.SSS")
    // ── Segment divider ────────────────────────────────────
    .segmentDivider(" │ ")
    // ── Table cell truncation ──────────────────────────────
    .tableCellLimit(40)            // enables truncation at 40 chars
    .noTableCellLimit()            // disable truncation (full values shown)
    // ── ANSI / color ───────────────────────────────────────
    .ansi(true)
    .callerColor(false)
    // ── Theme ──────────────────────────────────────────────
    .theme(LogTheme.COCKPIT)
    .customTheme(myThemeColors)
    // ── Call-chain filters ─────────────────────────────────
    .suppressContaining("com.example.proxy")
    .suppressMethodPrefix("myInternalMethod")
    .includeOnlyPackages("automation.", "steps.")
    .build()
    .apply();                      // make live
```

### Runtime setters (no rebuild)

```java
LogConfig cfg = LogConfig.current();
cfg.setTheme(LogTheme.HIGH_CONTRAST);
cfg.setAnsi(false);
cfg.setTableCellLimit(60);
cfg.disableTableCellLimit();
cfg.enableCallerColor();
cfg.setSegmentDivider(" | ");
cfg.setTsFormat("HH:mm:ss");
cfg.suppressContaining("MyProxy");
cfg.includeOnlyPackages("automation.");
cfg.clearIncludeFilter();
```

---

## 10. ANSI & Caller-Color Control

### ANSI on/off

```java
// Via CustomLogger
CustomLogger.enableAnsi();
CustomLogger.disableAnsi();

// Via LogConfig
CustomLogger.configure(c -> c.setAnsi(true));
LogConfig.current().setAnsi(false);
```

Auto-detection rules (applied at startup, in order):

1. System property `-Dlogger.ansi.enabled=true/false` → explicit override
2. IntelliJ test runner properties detected → **OFF**
3. `System.console() != null` → ON
4. `$TERM` environment variable set → ON
5. `ANSICON=true` → ON
6. `COLORTERM` set → ON
7. Otherwise → OFF

### Caller-color `@ConsoleOnly`

Appends a dim `Callee.method ← Caller.method` suffix using a second ANSI segment.

```java
// ⚠️ LIVE TERMINAL ONLY — breaks IntelliJ Test History and file logs
CustomLogger.enableCallerColor();

info.click("Will show caller suffix");

CustomLogger.disableCallerColor();  // always restore before CI / file runs
```

---

## 11. Call-Chain Filtering

Controls which stack frames appear in the caller suffix.

```java
// Only show frames from your own packages
CustomLogger.includeOnlyPackages("automation.", "pages.", "steps.");

// Suppress additional class-name substrings
CustomLogger.suppressClassContains("MyProxyClass", "CGLib$$");

// Clear include-only filter (revert to default suppression)
CustomLogger.clearIncludes();

// Same operations via LogConfig
LogConfig.current().includeOnlyPackages("automation.", "steps.");
LogConfig.current().suppressContaining("MyProxy");
LogConfig.current().clearIncludeFilter();
```

**Default suppressed patterns:**

| Type | Patterns |
|---|---|
| Classes (substring) | `core.logging.CustomLogger`, `core.logging.LogActions`, `core.logging.LoggerContext`, `core.logging.LogConfig`, `org.apache.log4j`, `java.`, `sun.`, `jdk.`, `com.sun.proxy`, `jdk.proxy`, `net.bytebuddy`, `reflect.` |
| Method prefixes | `log`, `debug`, `info`, `warn`, `error`, `lambda$`, `invoke` |

---

## 12. Testing

Tests live in `src/test/java/core/logging/`:

| Test class | Purpose |
|---|---|
| `CustomLoggerThemeTest` | Visual smoke test — every theme × level × intent combination. Factory creates ANSI=ON and ANSI=OFF runs. Uses `@DataProvider` over all 8 themes. |
| `CustomLoggerTest` | General smoke test — all action labels, multiline messages, object overloads, tree/table rendering. |
| `LogMultilineExperimentTest` | Edge-case experiments — multiline messages, null inputs, array rendering. |

### Running theme tests

```bash
# All tests
mvn test

# Theme tests only
mvn test -Dtest=CustomLoggerThemeTest

# Single theme test (e.g. COCKPIT smoke test)
mvn test -Dtest=CustomLoggerThemeTest#testCockpitTheme
```

### Recommended IntelliJ setup

1. Run `CustomLoggerThemeTest` with **ANSI=ON** in the live console.
2. Check each theme visually — confirm contrast between level BGs and intent FGs.
3. Run in CI / file-appender mode with `ANSI=OFF` to verify plain-text cleanliness.

---

## Quick reference card

```
CustomLogger
│
├── .info    ─────────────────────────── INFO  level (grey BG)
│   ├── .log(msg)          BASE          plain label
│   ├── .log(obj)                        auto-dispatch (Map→table, List→list, other→toString)
│   ├── .log(heading, obj)               heading + object
│   ├── .tree(heading, fields)           key/value tree (BASE intent)
│   ├── .click / .checkbox / .text      INTERACTION (white FG)
│   ├── .input / .dropdown / .toggle
│   ├── .upload
│   ├── .tab / .frame / .breadcrumb     NAVIGATION  (cyan FG)
│   ├── .wait / .search / .result       OBSERVE     (yellow FG)
│   ├── .table / .grid / .row           DATA        (magenta/lavender FG)
│   ├── .success / .complete / .resolved SUCCESS    (green FG)
│   └── .error / .failed / .timeout     ALERT       (red FG)
│       .validation / .fallback / .skip
│
├── .warn    ─────────────────────────── WARN  level (amber BG)
├── .error   ─────────────────────────── ERROR level (crimson BG)
└── .debug   ─────────────────────────── DEBUG level (near-black BG)

── Configuration ──────────────────────────────────────────────────────
configure(LogConfig)              replace live config (builder pattern)
configure(Consumer<LogConfig>)    patch live config in-place
config()                          direct access to live LogConfig

── Shortcuts (all delegate to LogConfig) ──────────────────────────────
setTheme(LogTheme.X)              switch built-in theme
setCustomTheme(ThemeColors)       apply fully custom theme
enableAnsi() / disableAnsi()      ANSI control
enableCallerColor()               @ConsoleOnly — dim caller suffix
initialize(Class<?>)              set Log4j category
fields("k",v, "k2",v2, …)        build key/value map

── LogConfig (live) ───────────────────────────────────────────────────
LogConfig.current().setTheme(…)
LogConfig.current().setAnsi(…)
LogConfig.current().setTableCellLimit(n)   // enables truncation
LogConfig.current().disableTableCellLimit()
LogConfig.current().setSegmentDivider("…")
LogConfig.current().setTsFormat("…")
```
