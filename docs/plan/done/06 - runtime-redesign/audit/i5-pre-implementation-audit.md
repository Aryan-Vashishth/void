# I5 Pre-Implementation Audit

**Phase:** runtime-redesign I5 -- Session Model
**Date:** 2026-07-29
**Verdict:** READY -- four in-scope findings; one bridge-cast observation carried from I4.3; implementation plan below

---

## Scope Reminder

Session becomes the first-class, domain-neutral unit of lifetime, identity, and isolation.
Four phases:

- **5.1** Session contract: `SessionContext` retype to `Executor`; session identity + lifecycle state; `EngineBootstrap` ledger closure.
- **5.2** Bootstrap de-Seleniumization: remove `driver.properties` hard-fail from `FrameworkBootstrap`; relocate check to the web-engine path.
- **5.3** Session as interaction subject: `VOID` facade navigation methods route through `FlowExecutor`; second execution model eliminated.
- **5.4** Config identity split: documentation and ownership clarification only; no code changes.

---

## Findings

### A1 -- `SessionContext` is typed to `UIEngine`, not `Executor` (fixable in 5.1)

**File:** `src/main/java/core/context/SessionContext.java:36,45,51`

`SessionContext.engine` is declared `UIEngine`; the constructor accepts `UIEngine`; `engine()`
returns `UIEngine`. Post-I4.3 the neutral contract is `Executor` (`UIEngine extends Executor`).
The only live non-deprecated caller that goes through `context.engine()` is the deprecated
`VOID.getContext()` path -- `VOID` holds its own `UIEngine engine` field (line 79) for navigation
and does not go through `SessionContext.engine()` in any active path.

Fix: retype the `engine` field, constructor parameter, and `engine()` return type from
`UIEngine` to `Executor`. `getEngineName()` delegates to `engine.getEngineName()` which is on
`Executor` -- no change needed. `VOID.engine` (line 79) remains `UIEngine`-typed and becomes the
sole `UIEngine` handle for navigation until 5.3 routes it through the pipeline.

**Severity:** LOW. No active non-deprecated callers of `context.engine()` cast the result to
`UIEngine`.

---

### A2 -- No session identity or lifecycle state in the stack (fixable in 5.1)

**Files:** `src/main/java/core/context/SessionContext.java`, `src/main/java/core/runtime/VOID.java`

`SessionContext` has no session ID and no lifecycle-state field. `VOID` has no lifecycle state
enum. `VOIDBuilder.start()` logs "VOID session started" but with no persistent identifier.
`SessionContext.toString()` reports config key count and engine name only. Traces and logs cannot
correlate operations to a specific session in multi-session runs.

Per ADR-021: Session is the unit of isolation; identity must be visible in traces. The plan
(phase 5.1) requires: a session ID, lifecycle (create/active/shutdown), and the session ID
appearing in construction and shutdown log lines.

Fix:
1. Add `final String sessionId` (UUID) to `SessionContext`; generate in constructor; expose via
   `sessionId()`.
2. Update `SessionContext.toString()` to include `sessionId`.
3. Add a minimal `SessionState` enum (`ACTIVE`, `SHUTDOWN`) to `VOID`; set to `ACTIVE` on
   construction, `SHUTDOWN` in `shutdown()`.
4. Include `sessionId` in `VOIDBuilder.start()` log and `VOID.shutdown()` log.

**Severity:** MEDIUM. Touches construction path; no external API change; low risk of regressions.

---

### A3 -- `UIEngineFactory.create()` bridge cast is open with no assigned closing phase (observation -- 5.1)

**File:** `src/main/java/core/engine/UIEngineFactory.java:80`

```java
// Bridge cast: EngineRegistrar.create() returns Executor (neutral contract);
// UIEngineFactory.create() still returns UIEngine until I4.4 retypes all callers.
UIEngine engine = (UIEngine) registrar.create(bootstrap);
```

I4.3 introduced this cast as a temporary bridge; I4.4 retyped `Action`, `FlowExecutor`, and
`ActionHandler` to `Executor` but did not update `UIEngineFactory.create()` itself -- because
`VOIDBuilder.start()` (the sole caller) still assigns the result to `UIEngine`. Once A1 retypes
`SessionContext.engine` to `Executor`, `VOIDBuilder.start()` can receive an `Executor` from the
registrar directly and pass it to `SessionContext`, while retaining a `UIEngine` typed local for
the `VOID` constructor's `UIEngine engine` argument. At that point the bridge cast in
`UIEngineFactory.create()` and the `UIEngine` return type can be removed.

**Action in 5.1:** When retying `VOIDBuilder.start()` for A1, also remove the bridge cast from
`UIEngineFactory.create()` by changing its return type to `Executor`. Update the `VOID`
constructor to accept `Executor` for `SessionContext` and keep `UIEngine` typed separately for
navigation. This closes the I4.3 bridge.

**Severity:** LOW. The cast is safe today (all registered engines are `UIEngine`); removal is a
cleanup with no behavior change.

---

### A4 -- `FrameworkBootstrap.init()` hard-fails on missing `driver.properties` (fixable in 5.2)

**File:** `src/main/java/core/bootstrap/FrameworkBootstrap.java:46-51`

```java
if (Thread.currentThread().getContextClassLoader()
        .getResource(ConfigPaths.DRIVER_DEFAULT) == null) {
    throw new IllegalStateException(
            "FrameworkBootstrap failed: driver.properties not found ...");
}
```

This is the C4 finding from the domain-model audit. The check runs unconditionally in `init()`,
which `VOIDBuilder.start()` calls for every session. Any non-web use (REST domain, CLI domain,
probe from I6.3) cannot reach session creation without a `driver.properties` on the classpath --
a web platform artifact forced onto the framework's neutral init path.

Fix in 5.2:
1. Delete lines 44-51 (the check and its success log line) from `FrameworkBootstrap.init()`.
2. Add equivalent classpath validation inside `SeleniumEngineRegistrar` (or the Selenium engine's
   `initialize()`) so the web path still fails fast before any interaction executes.
3. Add test: `FrameworkBootstrap.init()` completes without `driver.properties` on the classpath.
4. Update `FrameworkBootstrap` Javadoc; remove the step-1 bullet about `driver.properties`.

**Severity:** HIGH visibility. The failure point moves from framework init to web session
creation. Same JVM run, later moment, different message. Document in CHANGELOG. Verified
fail-fast-ness is preserved: the check fires before `engine.initialize()` returns, which is before
any navigation or action is possible.

---

### A5 -- `VOID` facade navigation methods bypass `FlowExecutor` (fixable in 5.3)

**File:** `src/main/java/core/runtime/VOID.java:175-200`

`navigateTo`, `getCurrentUrl`, `getTitle`, and `refresh` all call `engine.*` directly, bypassing
`FlowExecutor`. No hooks fire, no traces are emitted, no pipeline context exists for these
operations. `run(Flow)` and `run(Action)` go through `executor` (correct). The discrepancy is the
"subjectless interactions / second execution model" finding from the domain-model audit.

Fix in 5.3: define session-subject operation types (e.g., `Navigate`, `GetCurrentUrl`,
`GetTitle`, `Refresh`) that the pipeline can execute with the Session as subject. `VOID` facade
methods delegate to `executor.run(...)`. The facade surface is byte-identical to callers.

Hook semantics for session-subject operations must be explicit: no `LocatorDescriptor` exists;
hooks must accept a `null` or absent descriptor gracefully, or the no-descriptor contract must be
named in the hook interface.

**Severity:** MEDIUM (architectural). No behavior change for callers; traces and hooks gain
coverage of session-level operations.

---

## Risks

### R1 (LOW) -- `SessionContext.engine()` return-type change

`engine()` changing from `UIEngine` to `Executor` is a source-compatible narrowing only if no
external callers cast the result. The deprecated `VOID.getContext()` is the only visible path to
`SessionContext`; its callers would need to cast `engine()` to `UIEngine` manually. Mark the
change in CHANGELOG and note in the deprecation Javadoc that `getEngine()` on `VOID` is the
correct escape hatch.

### R2 (HIGH) -- Failure-point move for missing `driver.properties`

Removing the `FrameworkBootstrap` gate (A4) shifts the web-config failure from JVM init to web
session creation. This is a deliberate and documented change (plan 5.2 risk). Validation: a new
test asserts bootstrap succeeds with no `driver.properties`; an existing integration test asserts
the web path still fails before any interaction when the file is missing.

### R3 (MEDIUM) -- Hook contract for session-subject operations (5.3)

`navigateTo` and similar operations have no `LocatorDescriptor`. If `ActionHook` implementations
assume a non-null descriptor, 5.3 breaks them. Audit hook implementations before committing 5.3;
establish an explicit no-descriptor contract or a sentinel `LocatorDescriptor` for session
subjects.

---

## EngineBootstrap Ledger

`EngineBootstrap` is a sealed interface with a single `WithSettings(Properties)` record. Its role
-- carrying opaque engine-owned settings from `VOIDBuilder` to the registered engine -- is now
stable and final. No further evolution is planned. Ledger closed in 5.1.

---

## Implementation Plan

**5.1 -- Session contract**

1. Retype `SessionContext.engine` field, constructor parameter, and `engine()` return to
   `Executor`. Update `@param` Javadoc (A1).
2. Add `String sessionId` (UUID) to `SessionContext`; expose `sessionId()`; update `toString()`
   (A2).
3. Add `SessionState` enum (`ACTIVE`, `SHUTDOWN`) to `VOID`; set on construction and in
   `shutdown()`; include `sessionId` in the start and shutdown log lines (A2).
4. In `VOIDBuilder.start()`: obtain `Executor` from the registrar directly (or receive `Executor`
   from `UIEngineFactory.create()` after its return type is fixed); pass `Executor` to
   `SessionContext`; retain a separate `UIEngine` local via cast for the `VOID` constructor
   (A3).
5. Remove bridge cast from `UIEngineFactory.create()`; change return type to `Executor` (A3).
6. One commit. Suite green.

**5.2 -- Bootstrap de-Seleniumization**

1. Delete `driver.properties` existence check (lines 46-51) from `FrameworkBootstrap.init()`;
   update Javadoc.
2. Add equivalent check inside the Selenium engine's initialization path.
3. Add test: `bootstrapSucceedsWithNoDriverProperties_noWebSessionRequested`.
4. Add or update test: web session creation fails with expected message when
   `driver.properties` absent.
5. One commit. Suite green.

**5.3 -- Session as interaction subject**

1. Define session-subject operation types; confirm hook no-descriptor contract.
2. Rewrite `VOID.navigateTo`, `getCurrentUrl`, `getTitle`, `refresh` to delegate through
   `executor`.
3. Before-and-after trace/log comparison on demo suite: verify behavioral parity.
4. One commit. Suite green.

**5.4 -- Config identity split**

1. Restructure `configuration-reference.md` by owner (runtime / web-domain / logging).
2. Document every config key with its owner; note `driver.properties` as the Web domain's file.
3. One commit (docs only).
