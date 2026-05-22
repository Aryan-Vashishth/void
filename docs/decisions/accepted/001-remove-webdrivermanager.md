# 001 — Remove WebDriverManager

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

VOID originally declared `io.github.bonigarcia:webdrivermanager:6.3.3` as a dependency for automatic browser driver binary management (chromedriver, geckodriver, msedgedriver).

Selenium 4.6+ (released late 2022) ships with **Selenium Manager** — a built-in binary that handles driver discovery and download natively, making WebDriverManager redundant.

VOID uses Selenium 4.38.0.

---

## Decision

Remove WebDriverManager from the dependency tree entirely.

---

## Reasoning

1. **Zero active usage** — no Java file imports WebDriverManager. The only reference is a Javadoc comment in `DriverFactory.java`.
2. **Redundant** — Selenium Manager handles the same functionality natively since 4.6.
3. **Hidden behavior** — WebDriverManager uses reflection, HTTP calls to GitHub/Google APIs, and caches binaries in `~/.cache/selenium`. Silent failures are non-trivial to debug.
4. **Unnecessary transitive tree** — adds its own HTTP client and caching layer for zero value.

---

## Consequences

- One fewer dependency (and its transitive tree removed)
- Selenium Manager handles driver binaries automatically — no configuration needed
- The Javadoc comment in `DriverFactory` was updated to reflect native Selenium Manager usage
- Any consumer that still wants WebDriverManager can declare it in their own POM

