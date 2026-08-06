# I5 Full-System Audit (M4)

**Phase:** runtime-redesign I5 -- Session Model
**Date:** 2026-07-29
**Verdict:** PASS

---

## Deliverables Verified

| Phase | Deliverable | Status |
|---|---|---|
| I5.1 | `SessionContext.engine` retyped from `UIEngine` to `Executor` | PASS |
| I5.1 | `SessionContext.sessionId()` (UUID) added; `toString()` updated | PASS |
| I5.1 | `VOID.SessionState` enum (`ACTIVE`, `SHUTDOWN`) + `getSessionState()` | PASS |
| I5.1 | `sessionId` in start and shutdown log lines | PASS |
| I5.1 | `UIEngineFactory.create()` bridge cast removed; return type `Executor` | PASS |
| I5.1 | `EngineBootstrap` ledger closed | PASS |
| I5.2 | `driver.properties` gate removed from `FrameworkBootstrap.init()` | PASS |
| I5.2 | Equivalent check added to `SeleniumEngine.initialize()` primary path | PASS |
| I5.2 | `FrameworkBootstrapTest.init_succeedsWithoutDriverPropertiesOnClasspath` | PASS |
| I5.3 | `navigateTo`, `getCurrentUrl`, `getTitle`, `refresh` route through `FlowExecutor` | PASS |
| I5.3 | `VOID.engine` field removed; UIEngine accessed from `context.engine()` at call sites | PASS |
| I5.3 | `VOIDBuilder` bridge cast removed; `new VOID(ctx)` -- no UIEngine constructor argument | PASS |
| I5.4 | `configuration-reference.md` ownership table and validation-timing section | PASS |
| I5.4 | `ConfigPaths.java` constants annotated with domain ownership labels | PASS |

---

## Architecture Invariant Check

| Invariant | Status | Notes |
|---|---|---|
| `UIEngine` is the single execution authority | PASS | Unchanged |
| Engine-agnostic layers are Selenium-free | PASS | `core.context.SessionContext` now imports `Executor` not `UIEngine` |
| `LocatorDescriptor` is Selenium-free | PASS | Unchanged |
| `ElementSupport` scope is frozen | PASS | Unchanged |
| `Target` carries no enum-specific defaults | PASS | Unchanged |
| `VOIDBuilder` is single-use | PASS | Guard unchanged |
| Kernel purity | PASS | `core.runtime.VOID` retains UIEngine import (exception set, documented) |

---

## Audit Findings Resolution

| Finding | Resolution |
|---|---|
| A1: `SessionContext` typed to `UIEngine` | Fixed in I5.1 -- retyped to `Executor` |
| A2: No session identity or lifecycle state | Fixed in I5.1 -- UUID + `SessionState` enum |
| A3: `UIEngineFactory` bridge cast open | Fixed in I5.1 -- cast removed, return type `Executor` |
| A4: `FrameworkBootstrap` driver.properties gate (C4) | Fixed in I5.2 -- gate removed; check relocated to `SeleniumEngine` |
| A5: VOID facade navigation bypasses `FlowExecutor` | Fixed in I5.3 -- all four methods route through pipeline |

---

## Kernel Purity Gate State

`KernelBoundaryRulesTest.kernelPurity` -- all green. The `UIEngine` import in `core.runtime.VOID`
remains a named exception (documented in the rule): `getEngine()`, `interaction()`, and
`getDriver()` are all deprecated escape-hatch or deprecated-legacy methods. The cast
`(UIEngine) context.engine()` at those sites is intentional and documented as closing when the
deprecated surface is removed in I9.3 / I9.4.

`core.context.SessionContext` no longer imports `UIEngine` -- the longest-standing typing debt
from the pre-I4.3 era is resolved.

---

## Accepted Risks

| Risk | Status |
|---|---|
| R1: `SessionContext.engine()` return-type change (source compat) | Accepted -- only non-deprecated callers use `VOID.getEngine()` not `getContext().engine()` |
| R2: Failure-point move for missing `driver.properties` | Accepted -- documented in CHANGELOG; same JVM run, later moment, clear message, fail-fast before interactions |
| R3: Hook contract for session-subject operations | Deferred -- session ops are raw lambda actions; hooks require explicit no-descriptor contract (I8.2 or later) |

---

## Tests Green

1213 examples, 0 failures, 0 errors, 0 skipped.

`FrameworkBootstrapTest` (4 examples) added: verifies idempotency, initialized flag, empty config
pre-init, and `init_succeedsWithoutDriverPropertiesOnClasspath` (filtering classloader).

---

## Remaining Open Findings

D5-D17 from the domain-model audit remain open, tracked in I6-I9. No new findings opened by I5.
