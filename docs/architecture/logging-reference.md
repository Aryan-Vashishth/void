# Logging Reference

This guide describes VOID runtime log channels, file layout, and naming rules.

## Overview

VOID writes logs to three file channels per run date:

1. `debug-trace` - includes caller suffix (`Class.method <- Caller.method`)
2. `partial-trace` - excludes caller suffix
3. `full-trace` - includes full project call chain (`[CHAIN] A <- B <- C`)

Console output remains enabled and can include ANSI colors.
File outputs strip ANSI escape sequences.

## Folder Layout

For run date `YYYY-MM-DD`, logs are written to:

```text
logs/
  YYYY-MM-DD/
    debug-trace/
      debug-trace-<runId>.log
    partial-trace/
      partial-trace-<runId>.log
    full-trace/
      full-trace-<runId>.log
```

Rollover archives are written under each channel's `archive/` subfolder.

## Run Identity

`LoggerContext` sets runtime properties once per process:

- `void.runDate` -> `yyyy-MM-dd`
- `void.runId` -> `yyyyMMdd-HHmmss-SSS-pid<processId>`

These properties are used in Log4j file names so each run gets unique files.

## Log4j Routing

Configured in both:

- `src/main/resources/log4j2.xml`
- `src/test/resources/log4j2.xml`

Routing model:

- Root logger -> console only
- `debug-trace` logger -> debug trace file appender
- `partial-trace` logger -> partial trace file appender
- `trace` logger -> full trace file appender

`LogActions` emits each runtime line to all three channels with different detail levels.

## Channel Semantics

### debug-trace

Includes message and caller suffix. Example:

```text
2026-05-07 13:28:00.482 | INFO | [1/3] Navigating... | MethodRunner.runInSequence <- TestMethodWorker.run
```

### partial-trace

Includes message only (no caller suffix):

```text
2026-05-07 13:28:00.482 | INFO | [1/3] Navigating...
```

### full-trace

Includes message and full project call chain:

```text
2026-05-07 13:28:00.482 | INFO | [1/3] Navigating... | [CHAIN] VoidDemo.login <- FlowExecutor.run <- ...
```

Project chain filtering keeps only project packages (`core.*`, `dsl.*`, `elements.*`, `examples.*`, `StepDefinition.*`).

## Runtime Properties

Optional runtime overrides:

- `-Dvoid.logDir=<path>` (default: `./logs`)
- `-Dvoid.runDate=<yyyy-MM-dd>` (normally auto-set)
- `-Dvoid.runId=<custom-id>` (normally auto-set)

Example:

```text
mvn test -Dvoid.logDir=D:/void-framework/logs
```

## Troubleshooting

- If files are not created in expected folders, ensure classpath uses updated `log4j2.xml`.
- If ANSI codes appear in files, verify file appenders use `%replace{%m}{\u001B\[[;\d]*m}{}`.
- If call chains look too noisy, check package filters in `LogActions` project-prefix list.

