# 005 — Logging Architecture (CustomLogger over Raw Log4j)

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

VOID requires logging that goes beyond standard structured logging:

- Color-coded ANSI output for visual distinction in console
- Call-site tracing (who called whom)
- Semantic action methods (`click`, `dropdown`, `fallback`, `success`, `failed`)
- Dual-channel output (real-time console + persistent trace file)
- Themeable color schemes

Standard Log4j 2 patterns and appenders cannot provide call-site tracing at the depth VOID requires (arbitrary caller chain, not just the immediate logger location).

---

## Decision

VOID wraps Log4j 2 behind `CustomLogger` — a purpose-built logging façade that adds:

1. **ANSI color-coded output** with 8 built-in themes (`LogTheme`, `BuiltInThemes`)
2. **Call-site tracing** — `OriginClass.method ← CallerClass.method` in every log line
3. **Semantic log channels** — `info.log()`, `debug.click()`, `warn.fallback()`, `error.failed()`
4. **Per-class initialization** — `CustomLogger.initialize(this.getClass())`
5. **`@ConsoleOnly` annotation** — marks log calls that skip the persistent file

Log4j 2 serves as the underlying engine (`log4j-api` + `log4j-core`), configured via `log4j2.xml`.

---

## Reasoning

1. **Debugging is VOID's primary value** — generic log output ("element not found") is useless. VOID needs structured, traceable, color-differentiated output.
2. **Call-site chains are critical** — knowing that `LocatorResolver.resolve` was called from `Interactions.clickOn` which was called from `LoginTest.testLogin` provides full context.
3. **Semantic methods enforce consistency** — `debug.click(element)` produces uniform output across the codebase, unlike ad-hoc `logger.debug("clicked " + element)`.
4. **Log4j 2 as engine, not facade** — VOID uses native Log4j 2 API (after removing the 1.x bridge), keeping the underlying implementation explicit.

---

## Consequences

- All logging goes through `CustomLogger` — consistent format and traceability everywhere
- Log4j 2 handles file rotation, appender configuration, and level filtering
- ANSI colors are managed by VOID directly (not Log4j patterns)
- The `log4j-1.2-api` bridge was removed — native Log4j 2 imports only
- Themes are configurable but ship with sensible defaults

