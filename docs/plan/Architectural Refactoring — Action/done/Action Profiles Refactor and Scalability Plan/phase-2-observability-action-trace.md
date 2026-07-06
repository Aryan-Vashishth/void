# Phase 2 — Observability First (ActionTrace)

**Status:** Complete  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — instrumentation only, no behavioral changes

---

## Objective

Make the action execution pipeline visible. Before any more behavior is layered on, developers must be able to see exactly what ran, what profile was applied, what hooks fired, and why an action failed.

Without this, every future debug session becomes archaeology.

---

## Context

A call like this:

```java
LOGIN.click().safely()
```

actually executes this sequence:

```text
Action Created          → LOGIN_BUTTON.click wrapped in SAFE profile
Profile Applied         → before=[WAIT_FOR_ELEMENT_CLICKABLE], after=[WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]
Before Hooks Executed   → engine.waitForClickable(descriptor, 10s)
Action Executed         → engine.click(descriptor)
After Hooks Executed    → engine.waitForAbsence(loader, 10s) → engine.highlight(descriptor, "green")
```

The user sees none of this unless it fails. And when it fails, they have no trace.

---

## Trace Model

### `ActionTrace` (new class — `core.actions.trace.ActionTrace`)

```java
public class ActionTrace {
    String elementName;          // e.g. "LOGIN_BUTTON"
    String operation;            // e.g. "click"
    String profileName;          // e.g. "SAFE", "DEBUG", "RAW", "custom"
    List<String> beforeHooks;    // hook names in execution order
    List<String> afterHooks;     // hook names in execution order
    long durationMs;             // total execution time
    TraceStatus status;          // SUCCESS, FAILED, HOOK_FAILED
    Throwable failure;           // null if SUCCESS
}
```

### `TraceStatus` (enum)

```java
public enum TraceStatus {
    SUCCESS,        // all hooks and action completed normally
    FAILED,         // action itself threw
    HOOK_FAILED     // a before or after hook threw
}
```

---

## Log Output Example

When trace logging is active (debug mode), emit:

```text
╔══ ACTION TRACE ═══════════════════════════════╗
║  Element  : LOGIN_BUTTON
║  Operation: click
║  Profile  : SAFE
║  Before   : [WAIT_FOR_ELEMENT_CLICKABLE]
║  Execute  : click()
║  After    : [WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]
║  Duration : 847ms
║  Status   : SUCCESS
╚════════════════════════════════════════════════╝
```

On failure:

```text
╔══ ACTION TRACE ═══════════════════════════════╗
║  Element  : LOGIN_BUTTON
║  Operation: click
║  Profile  : SAFE
║  Before   : [WAIT_FOR_ELEMENT_CLICKABLE]  ← FAILED HERE
║  Status   : HOOK_FAILED
║  Error    : TimeoutException: element not clickable after 10s
╚════════════════════════════════════════════════╝
```

---

## Affected Files

New:
- `src/main/java/core/actions/trace/ActionTrace.java`
- `src/main/java/core/actions/trace/TraceStatus.java`
- `src/main/java/core/actions/trace/ActionTraceLogger.java`

Modified:
- `src/main/java/core/actions/HookedAction.java` — add instrumentation points (when not deprecated yet)
- `src/main/java/core/actions/Action.java` — optionally expose trace-enabled withHooks variant

---

## Checklist

### Data Model
- [x] Create `ActionTrace` with all fields above.
- [x] Create `TraceStatus` enum (SUCCESS, FAILED, HOOK_FAILED).
- [x] Create `ActionTraceLogger` — responsible for emitting formatted log output.

### Instrumentation
- [x] Record start time before first before-hook.
- [x] Record which hooks were executed in order (their class/field name if possible).
- [x] Record action execution success or failure.
- [x] Record after-hook execution success or failure.
- [x] Record total elapsed time.

### Output
- [x] Emit trace only when debug logging is active (use `CustomLogger.debug`).
- [x] Format output with clear section headers (see example above).
- [x] Do not emit trace in `raw()` mode (intentionally silent).

### Tests
- [x] Unit test: trace records correct hook order for `safely()` on click.
- [x] Unit test: trace records `HOOK_FAILED` status when before hook throws.
- [x] Unit test: trace records `FAILED` status when action itself throws.
- [x] Unit test: trace is not emitted for `raw()`.

### Integration
- [x] Verify trace output appears correctly in `VoidDemo` debug run.
- [x] Verify trace does not affect test results (observability only).

---

## Exit Criteria

- Running a flow in debug mode emits a readable trace per action.
- Hook failures are distinguishable from action failures in the trace.
- Zero behavioral change to non-debug execution.

---

## What NOT to Do

- Do not store traces in-memory for reporting in this phase (that is a future phase).
- Do not expose `ActionTrace` through the public `Action` API yet.
- Do not add trace output to production (non-debug) log level.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

