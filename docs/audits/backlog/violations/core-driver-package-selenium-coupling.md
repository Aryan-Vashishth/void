---
name: core-driver-package-selenium-coupling
description: core/driver package contains Selenium-specific infrastructure under a neutral name -- placement and naming imply engine-agnostic scope, violating ADR-018 intent
metadata:
  type: project
---

# `core/driver` -- Selenium Infrastructure Named and Placed as Framework Infrastructure

**Principle:** ADR-018 (engine-agnostic layers must be Selenium-free), package cohesion
**Area:** `src/main/java/core/driver/` and `core/driver/config/driver.properties`
**Discovered:** 2026-07-20 (architectural review post engine-decoupling initiative)
**Risk:** Medium (misleading package name; allows future contributors to treat Selenium classes as engine-agnostic)

## What it is

The `core/driver/` package sits at the top level of the framework alongside engine-agnostic
packages (`core/runtime`, `core/engine`, `core/context`). Its contents are entirely
Selenium-specific:

| Class | Selenium coupling |
|---|---|
| `DriverContext.java` | Holds `WebDriver` instance |
| `DriverFactory.java` | Creates Selenium `WebDriver` via `ChromeDriver`, `FirefoxDriver`, etc. |
| `DriverManager.java` | Lifecycle orchestration for a `WebDriver` |
| `Waiter.java` | Returns `WebDriverWait` (see also: [[waiter-returns-webdriverwait]]) |

The config file `core/driver/config/driver.properties` uses the name `driver.properties`,
which carries the same neutral framing.

## Why it matters

A contributor reading the package tree sees `core/driver` adjacent to `core/runtime` and
`core/context` and infers it is framework infrastructure. They are then likely to:
- Import `DriverContext` or `DriverFactory` in engine-agnostic code without recognising it
  as a violation
- Place new Selenium helpers in `core/driver` rather than `core/engine/selenium/`

The neutral name actively works against the engine-neutrality invariant ADR-018 establishes.

## Recommended fix

A dedicated initiative (`initiative/selenium-driver-relocation`) after
`feature/engine-decoupling` is merged:

1. Move `core/driver/` to `core/engine/selenium/driver/` (co-located with `SeleniumEngine`).
2. Rename classes to make the coupling explicit:
   - `DriverContext` -> `SeleniumDriverContext`
   - `DriverFactory` -> `SeleniumDriverFactory`
   - `DriverManager` -> `SeleniumDriverManager`
   - `Waiter` -> best removed after [[waiter-returns-webdriverwait]] is resolved
3. Rename config file: `driver.properties` -> `selenium-webdriver.properties`. Update
   `ConfigPaths` constants accordingly.
4. Migrate all import sites (codebase-wide).
5. Write a new ADR documenting the placement rule: Selenium-specific infrastructure lives
   under `core/engine/selenium/`.

## API surface decision required before implementation

`DriverFactory.Profile` is exposed in the public API via `VOIDBuilder.profile(DriverFactory.Profile)`.
Moving or renaming `DriverFactory` changes this call site for all callers. Options:

- Re-expose `Profile` via a stable type (e.g., `SessionProfile` in `core/runtime`) and
  have `SeleniumDriverFactory` implement against it.
- Accept the breaking change and update callers under a deprecation cycle.

This decision requires its own ADR discussion -- do not start the initiative without it.

## Why not addressed in the current initiative

Surfaced during architectural review of the engine-decoupling branch. Scope is too large
(codebase-wide import migration, ADR required, API surface decision) and is independent of
the OOP violations being remediated in `oop-violations-remediation`.
