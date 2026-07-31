# ADR-022 -- Session Model: Identity, Neutral Bootstrap, and Unified Execution

**Date:** 2026-07-29
**Status:** Accepted (merged to main, v0.7.0)

---

## Context

After I4 established `Executor` as the kernel's neutral execution-owner contract, the session
layer -- the unit of lifetime, identity, and isolation -- still violated domain neutrality in
three ways.

### SessionContext typed to UIEngine

`SessionContext(Properties, UIEngine)` held a `UIEngine` reference despite `Executor` now
being the kernel's contract. This meant the session type, which is in the kernel membership
list (ADR-021), carried a web-domain type. Any code that received a `SessionContext` and called
`context.engine()` got a `UIEngine` -- a web concept -- from what was supposed to be a
domain-neutral carrier.

`UIEngineFactory.create()` compounded this: its return type was `UIEngine`, and it contained
an explicit bridge cast `(UIEngine) registrar.create(bootstrap)` that was intentionally left
open in I4.3 with a note to close it when callers were retyped.

### No session identity

`SessionContext` had no session ID. Traces logged engine name and profile but carried no
stable identifier across the lifetime of a single session. In multi-session runs, log lines
from concurrent sessions were indistinguishable. `VOID` had no lifecycle state -- a shut-down
session was structurally identical to an active one.

### Bootstrap coupled to the web domain

`FrameworkBootstrap.init()` hard-failed on missing `driver.properties`
(finding C4 in the domain-model audit). Every JVM start -- regardless of whether a web session
was ever requested -- required a web-platform configuration file. Non-web domains (REST, CLI,
probe from I6.3) could not start the framework.

### Second execution model in the facade

`VOID.navigateTo`, `getCurrentUrl`, `getTitle`, and `refresh` called `engine.*` directly,
bypassing `FlowExecutor`. These are the only operations in the framework that skip the hook
pipeline entirely. Logs showed no trace for them; hooks could not be attached. Every other
session operation -- `run(Flow)`, `run(Action)` -- went through the single pipeline.

---

## Decision

### 1. Session gains identity and lifecycle state

`SessionContext` gains a `final String sessionId` generated as a UUID at construction time,
exposed via `sessionId()`. `SessionContext.toString()` includes the session ID.

`VOID` gains a `SessionState` enum (`ACTIVE`, `SHUTDOWN`) and a `volatile SessionState state`
field initialized to `ACTIVE`. `VOID.shutdown()` transitions to `SHUTDOWN`. The session ID
appears in the start and shutdown log lines emitted by `VOIDBuilder.start()` and
`VOID.shutdown()`.

Session identity is immutable after construction. Lifecycle state is write-once in the
`ACTIVE -> SHUTDOWN` direction.

### 2. SessionContext is retyped to Executor; UIEngineFactory bridge cast removed

`SessionContext(Properties, Executor)` -- the constructor parameter changes from `UIEngine`
to `Executor`. `engine()` returns `Executor`. `getEngineName()` delegates to
`Executor.getEngineName()`, which is already on the neutral interface.

`UIEngineFactory.create(Properties, EngineBootstrap)` return type changes from `UIEngine` to
`Executor`. The bridge cast `(UIEngine) registrar.create(bootstrap)` is removed. The cast was
introduced in I4.3 with a documented closing condition ("until I4.4 retypes all callers");
that condition is now met.

`VOID.engine` (the `UIEngine`-typed field) is removed. UIEngine is accessed from
`context.engine()` via explicit cast only at the three deprecated or escape-hatch call sites:
`getEngine()`, `interaction()`, and `getDriver()`. These casts are intentional and documented;
they close when the deprecated surface is deleted in I9.3 / I9.4.

`VOIDBuilder.start()` no longer holds a `UIEngine` local variable. The bridge cast in
`VOIDBuilder` is gone. `new VOID(ctx)` replaces `new VOID(ctx, engine)`.

### 3. Framework bootstrap is domain-neutral

`FrameworkBootstrap.init()` removes the `driver.properties` existence check. The Javadoc is
updated to state that the class does not validate web or driver configuration.

The check relocates to `SeleniumEngine.initialize()` on the primary driver-creation path
(when `this.driver == null`), firing before `DriverFactory.fromProfile(profile).build()`. This
preserves fail-fast semantics for web sessions: the failure occurs before any interaction
executes, which is the same guarantee that the bootstrap check provided. The failure point
moves from JVM init to web session creation -- same JVM run, later moment, different message.

Non-web domains may now call `FrameworkBootstrap.init()` without `driver.properties` on
the classpath.

### 4. All session-level operations route through FlowExecutor

`VOID.navigateTo`, `getCurrentUrl`, `getTitle`, and `refresh` route through
`executor.run(action)` where `executor` is the session's `FlowExecutor`. The UIEngine is
accessed inside the action lambda via cast, since these are web-domain operations.

```java
// Before (direct engine call, bypasses pipeline):
public void navigateTo(String url) { engine.navigateTo(url); }

// After (routes through FlowExecutor):
public void navigateTo(String url) {
    executor.run(e -> ((UIEngine) e).navigateTo(url));
}
```

For value-returning operations (`getCurrentUrl`, `getTitle`), a single-element array holder
captures the result inside the lambda:

```java
public String getCurrentUrl() {
    String[] result = {null};
    executor.run(e -> result[0] = ((UIEngine) e).getCurrentUrl());
    return result[0];
}
```

The facade surface is byte-identical to callers. The second execution model is eliminated.

**Hook semantics for session-subject operations.** Session operation lambdas do not override
`Action.resolve(Executor)`. If a caller explicitly attaches hooks to a session operation, the
`HookChainAction` would call `resolve()` and throw `UnsupportedOperationException`. This is
an explicit architectural deferral: hook composability for session-level operations requires a
no-descriptor hook contract that is out of scope here. The deferred work is documented as R3
in the I5 pre-implementation audit and assigned to I8.2.

### 5. Configuration ownership is explicit

`driver.properties` and its overlay files (`driver-local`, `driver-ci`, `driver-grid`) are
documented as belonging to the Web domain. `test.properties` is Runtime (neutral).
`log4j2.xml` is Runtime / Logging. No neutral component documents or validates a driver key.

`ConfigPaths` constants carry ownership annotations (`[Web domain]`, `[Runtime]`).
`configuration-reference.md` is restructured with an ownership table and validation-timing
section.

---

## Scope constraints

### Hook contract for session operations is deferred

Attaching explicit before/after hooks to `navigateTo` and similar calls is not supported in
this initiative. The deferred work requires defining a sentinel or absent-descriptor contract
on `ActionHandler.execute(Executor, LocatorDescriptor)` and ensuring all existing hook
implementations handle a null or sentinel descriptor gracefully. This is I8.2's scope.

### VOID facade surface is frozen

`VOID`'s public method signatures are unchanged. `getEngine()` continues to return `UIEngine`.
`interaction()` continues to exist as a deprecated method. The facade re-declaration (including
removal of deprecated methods) is I9.5.

### H6 (mutable Properties exposure) is not fixed here

`FrameworkBootstrap.utilsConfig` is a mutable `Properties` field on a static class. This is
tracked in the backlog (H6). Fixing it in I5.2 would have added a second objective; it stays
backlogged.

---

## Reasoning

### Why return Executor from UIEngineFactory.create(), not UIEngine?

`UIEngineFactory.create()` is the kernel factory. The kernel's execution-owner contract is
`Executor`. Returning `UIEngine` required a downcast from the neutral kernel layer into the
web domain. Every caller except the web-specific `VOID` constructor was receiving a type
wider than needed. The return type should match the contract, not the current sole implementor.

### Why move the driver.properties check to SeleniumEngine?

The check belongs with the code that needs the file. `SeleniumEngine.initialize()` is the
first point at which `driver.properties` is actually required. Moving it there makes the
dependency explicit and co-located. A test for any non-web domain simply never calls
`SeleniumEngine.initialize()`, so it is never blocked by the check.

### Why a UUID for session identity, not a counter?

UUIDs are globally unique across JVMs, processes, and time. A counter scoped to one JVM
provides no isolation guarantee in distributed runs or when the JVM restarts. The cost is
negligible (one UUID.randomUUID() call per session). The benefit is that session IDs can be
correlated across logs, CI runs, and external observability systems without a central counter.

### Why route getCurrentUrl/getTitle through a lambda rather than a typed result action?

The pipeline (`FlowExecutor.run`) is `void`. The options for value-returning operations are:
(a) a typed result wrapper that requires changes to `FlowExecutor` and `Action`, (b) a
mutable holder captured in the lambda, or (c) a direct engine call. Option (a) extends the
pipeline API, which is deliberately frozen. Option (c) preserves the second execution model.
Option (b) satisfies the unified execution model requirement with zero API surface growth. The
holder pattern is not novel Java; the capture is safe because the array is effectively final
and the lambda executes synchronously before `executor.run()` returns.

---

## Consequences

- `SessionContext(Properties, Executor)` -- constructor parameter type changed
- `SessionContext.engine()` -- return type changed from `UIEngine` to `Executor`
- `SessionContext.sessionId()` -- new accessor
- `VOID.SessionState` -- new public enum
- `VOID.getSessionState()` -- new method
- `VOID.engine` field removed
- `VOID(SessionContext)` -- constructor takes one argument, not two
- `UIEngineFactory.create()` -- return type `Executor` (was `UIEngine`); bridge cast gone
- `FrameworkBootstrap.init()` -- no longer validates `driver.properties`
- `SeleniumEngine.initialize()` -- validates `driver.properties` on primary path
- `VOID.navigateTo`, `getCurrentUrl`, `getTitle`, `refresh` -- route through `FlowExecutor`
- `configuration-reference.md` -- restructured with ownership table
- `ConfigPaths` constants -- ownership annotations added

---

## Related

- [ADR-021 -- Runtime Redesign: Kernel Boundary, Ontology, Open Decisions](021-runtime-redesign-kernel-boundary.md)
- [ADR-018 -- Engine Lifecycle Ownership](../accepted/018-engine-lifecycle-ownership.md)
- [ADR-011 -- VOID as Primary Session Facade](../accepted/011-void-facade-boundary.md)
- [I5 pre-implementation audit](../../plan/draft/06%20-%20runtime-redesign/audit/i5-pre-implementation-audit.md)
- [I5 full-system audit (M4)](../../plan/draft/06%20-%20runtime-redesign/audit/i5-full-system-audit.md)
