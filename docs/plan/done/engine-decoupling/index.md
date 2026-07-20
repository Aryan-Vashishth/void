# Engine Decoupling — Selenium Hotswap Enablement

Identified: 2026-07-15 bootstrap coupling analysis.
Branch target: `feature/engine-decoupling` cut from `main`.

---

## Problem statement

The framework has one engine implementation (`SeleniumEngine`) and claims to support others
(Playwright stub at `UIEngineFactory.java:47`). The startup pipeline prevents that claim from
ever being true. `VOID.start()` unconditionally creates a Selenium `WebDriver` **before**
reading the configured engine — so even if `engine=playwright` is set, a Chrome browser opens.

The root cause is an inverted dependency: the engine factory accepts a pre-built `WebDriver`
rather than letting each engine manage its own driver lifecycle.

---

## Coupling violation map

| ID | Priority | Layer            | File / Line                             | Violation                                                                 |
|----|----------|------------------|-----------------------------------------|---------------------------------------------------------------------------|
| V1 | CRITICAL | Engine factory   | `UIEngineFactory.java:41`              | `create()` signature requires `WebDriver` — non-Selenium engines can never fit |
| V2 | CRITICAL | Session startup  | `VOID.java:138`                         | WebDriver created unconditionally before engine selection                 |
| V3 | CRITICAL | Session context  | `VOID.java:80`, `ExecutionContext.java:33` | `VOID` holds `ExecutionContext` (Selenium-typed); `SessionContext` exists but is dead code |
| V4 | HIGH     | Interactions     | `Interactions.java:68`                  | `(WebDriver) engine.getNativeDriver()` — runtime bomb for any non-Selenium engine |
| V5 | MEDIUM   | Session teardown | `VOID.java:165`                         | `VOID.shutdown()` calls `DriverContext.removePrimary()` directly, bypassing engine |
| V6 | LOW      | Bootstrap        | `FrameworkBootstrap.java:47`            | Selenium JUL logger suppressed unconditionally in engine-agnostic bootstrap |

---

## Current startup sequence (broken for hotswap)

```
VOID.start(profile)
  1. FrameworkBootstrap.init()                        // OK
  2. DriverManager.createDriver(profile)              // ALWAYS creates a Selenium WebDriver ← V2
  3. new ExecutionContext(config, driver)             // session context is WebDriver-typed ← V3
  4. UIEngineFactory.create(config, driver)           // factory receives a pre-built driver ← V1
       └─ switch(engine) { "selenium" → new SeleniumEngine(driver) }
```

Any `engine=playwright` config falls through to the `default → throw` branch — but only
**after** a browser window has already opened at step 2.

## Target startup sequence (engine-agnostic)

```
VOID.start(profile)
  1. FrameworkBootstrap.init()
  2. UIEngineFactory.create(config, profile)          // engine selected first, driver deferred
       └─ switch(engine) {
            "selenium"   → new SeleniumEngine(profile) → initialize() creates WebDriver internally
            "playwright" → new PlaywrightEngine()      → initialize() launches Playwright browser
          }
  3. new SessionContext(config, engine)               // engine-typed session context (already exists)
```

---

## Phase overview

| Phase | Goal                                             | Violations fixed | Key changes                                               |
|-------|--------------------------------------------------|------------------|-----------------------------------------------------------|
| 1     | Engine factory contract: engine owns its driver  | V1               | Drop `WebDriver` from `UIEngineFactory.create()`; `SeleniumEngine` creates its own driver in `initialize()` |
| 2     | Invert VOID startup; wire SessionContext         | V2, V3, V5       | `VOID.start()` calls factory first; replaces `ExecutionContext` with `SessionContext` |
| 3     | Interactions: remove unsafe cast                 | V4               | Drop `DriverContext` call from `Interactions` constructor; remove `SeleniumEngine` import |
| 4     | Bootstrap cleanup                                | V6               | Move Selenium logger suppression into `SeleniumEngine.initialize()` |

Phase docs:
- [Phase 1 — Factory contract](phase-1-factory-contract.md)
- [Phase 2 — VOID startup pipeline](phase-2-void-startup.md)
- [Phase 3 — Interactions cleanup](phase-3-interactions-cleanup.md)
- [Phase 4 — Bootstrap cleanup](phase-4-bootstrap-cleanup.md)

---

## Dependency rationale

V1 must be fixed before V2: `VOID.start()` cannot stop creating a `WebDriver` until
`UIEngineFactory.create()` no longer requires one. Phase 1 changes the factory signature;
Phase 2 changes the caller.

V3 follows V2: once `VOID.start()` no longer passes a `WebDriver` to anything,
`ExecutionContext` (which holds one) has no remaining construction site in `VOID` and
can be replaced by `SessionContext`.

V4 is independent of V1–V3 at the compile level but must come after Phase 1 stabilises
the engine contract. The unsafe cast in `Interactions` is only a runtime hazard once
non-Selenium engines are actually reachable — which requires Phase 1 to complete first.

V5 is addressed in Phase 2: once `SeleniumEngine.shutdown()` owns `DriverContext` cleanup,
`VOID.shutdown()` delegates fully to the engine and removes its direct registry call.

V6 is cosmetic and has no functional dependency on any other phase. It goes last.

**Rule:** nothing in Phase N depends on Phase N+1. Each phase compiles and passes
`mvn compile -q` on its own before the next phase begins. Never mix phases in one commit.

---

## What does NOT change

- `DriverFactory` — still the Selenium driver builder; now called from inside `SeleniumEngine` instead of from `VOID`
- `DriverContext` — still the Selenium ThreadLocal registry; now written to from inside `SeleniumEngine` only
- `DriverManager.createDriver()` — still exists for callers who need it directly; deprecation deferred to a future workstream once no internal or external callers remain
- `UIEngine` interface — no changes to contract
- `EngineConfig` — no structural changes; Phase 1 notes that it already passes raw `Properties` through
- `Interactions` — frozen; only the constructor side-effect is removed in Phase 3
- `ExecutionContext` — not deleted; gets `@Deprecated` in Phase 2, removal is a separate workstream
- All locator resolution, page objects, element enums, `.properties`/`.json` files

---

## Commit sequence

One commit per step. No commit spans two phases.

```
# Phase 1
feat(engine): add SeleniumEngine(Profile) constructor; initialize() creates driver internally
refactor(engine): replace WebDriver factory parameter with EngineBootstrap

# Phase 2
feat(runtime): introduce VOIDBuilder; VOID.builder() replaces VOID.start(Profile)
refactor(engine): delete EngineBootstrap.FromDriver; simplify UIEngineFactory inner switch
refactor(runtime): replace ExecutionContext with SessionContext in VOID; invert startup order
refactor(runtime): VOID.shutdown() delegates DriverContext cleanup to SeleniumEngine

# Phase 3
fix(interactions): remove unsafe WebDriver cast from Interactions(UIEngine) constructor
refactor(bridge): introduce SeleniumLocatorBridge; relocate Selenium By adapter
refactor(interactions): replace SeleniumEngine.fromBy() call sites with SeleniumLocatorBridge

# Phase 4
refactor(bootstrap): move Selenium JUL logger suppression to SeleniumEngine.initialize()
```

All commits follow Conventional Commits format. No em dashes. Imperative present tense.

---

## Post-completion: runtime isolation

Once all four phases are complete, each `VOID` instance is the correct unit of isolation.
A natural follow-on is to make that isolation total -- every execution concern owned by the
runtime, nothing shared by default:

```
VOID runtime
 ├── SessionContext
 ├── UIEngine
 ├── FlowExecutor
 ├── Logger
 ├── EventBus (future)
 ├── RuntimeConfig
 └── Resources (screenshots, downloads, traces)
```

Then two concurrent runtimes have no shared mutable state unless explicitly wired together:

```java
VOID admin    = VOID.builder().profile(CHROME).start();
VOID customer = VOID.builder().profile(CHROME).start();
// admin and customer share no mutable execution state
```

This is not part of the current four phases. The current phases remove the forced coupling
to Selenium. Full runtime isolation is the logical destination once that coupling is gone.
It would likely involve replacing `DriverContext` (a ThreadLocal singleton) with per-instance
state inside `SeleniumEngine`, and scoping logging to the runtime instance rather than the
JVM. Track as a separate initiative once Phase 4 is verified.

---

## Post-completion naming

Once all four phases are complete, two Selenium-specific names will be inconsistent with
the engine-agnostic architecture:

- `DriverContext` -- the ThreadLocal registry for the active WebDriver. Can be renamed
  `EngineHostContext` to reflect that it holds the native runtime handle, not a Selenium
  artifact specifically.
- The `driver` field in `SeleniumEngine` -- can be renamed to `engineHost` or `nativeRuntime`
  to match the vocabulary established by the decoupling plan.

Neither rename is required for correctness. Both should be deferred until after the
engine-decoupling phases are verified, since renaming during the migration adds noise to
diffs and makes the phase-boundary commits harder to review.

---

## Verification

```
# After Phase 1
mvn compile -q
grep -r "new SeleniumEngine(driver)" src/main/java/   # must be empty outside SeleniumEngine itself
grep -n "WebDriver" src/main/java/core/engine/UIEngineFactory.java   # must be empty

# After Phase 2
mvn compile -q
grep -n "createDriver"      src/main/java/core/runtime/VOID.java   # must be empty
grep -n "ExecutionContext"  src/main/java/core/runtime/VOID.java   # must be empty
grep -n "SessionContext"    src/main/java/core/runtime/VOID.java   # must appear for field + constructor

# After Phase 3
mvn compile -q
grep -n "getNativeDriver"  src/main/java/core/interactions/Interactions.java   # must be empty
grep -n "SeleniumEngine"   src/main/java/core/interactions/Interactions.java   # must be empty

# After Phase 4
mvn compile -q
grep -n "org.openqa.selenium" src/main/java/core/bootstrap/FrameworkBootstrap.java   # must be empty
```
