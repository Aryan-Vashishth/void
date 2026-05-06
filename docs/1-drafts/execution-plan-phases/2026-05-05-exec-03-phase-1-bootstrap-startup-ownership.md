# Phase 1 — Fix Bootstrap & Startup Ownership

> **Status:** Prospect (pre-draft). Decisions and unknowns must close before
> implementation begins.
>
> **Phase number:** 1 (audit numbering, stable).  
> **Execution order:** 3 of 10 — runs after Phase 2 (resolution unification).  
> **Precedes:** Phase 1.5 (exec order 4).  
> **Succeeds:** Phase 2 (exec order 2).
>
> **Goal:** Remove Selenium-first assumptions from framework startup. Make
> bootstrap and engine instantiation engine-agnostic so a Playwright engine
> can boot the framework without classpath or runtime ties to Selenium.
>
> **Proposed:** 2026-05-05  
> **Last updated:** 2026-05-05

---

## Full Execution Sequence (for reference)

> Phase numbers are stable audit identifiers. Execution order reflects
> dependency-driven sequence — Phase 2 runs before Phase 1 by design
> (see active architecture doc §8).

| Exec | Phase # | Title | Depends On |
|:---:|:---:|---|---|
| 1 | 0 | Lock Architecture Rules | — |
| 2 | 2 | Resolution Unification | Phase 0 |
| **3** | **1** | **Fix Bootstrap & Startup Ownership ← you are here** | Phase 2 |
| 4 | 1.5 | Lock Execution Model in Code | Phase 1 |
| 5 | 3 | Wire `FlowExecutor` into Runtime | Phase 1, 2 |
| 6 | 4 | Remove Selenium Leakage from Public Surface | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 0. Snapshot Validation Delta (Codebase Reality Check)

This section re-baselines the plan against the current repository snapshot so
Phase 1 work starts from verified facts, not assumptions.

| Item | Current state | Impact on Phase 1 |
|---|---|---|
| `SessionContext` | Exists (`core.context.SessionContext`) | D3 partly addressed; runtime still uses `ExecutionContext` |
| `ExecutionContext` | Still used by `core.runtime.VOID` | Must be replaced in runtime lifecycle |
| `UIEngineFactory.create(Properties, WebDriver)` | Still required | Core Phase 1 API migration still pending |
| `VOID.start()` | Creates `WebDriver` via `DriverManager` directly | Violates engine-first startup target |
| `VOID.shutdown()` | Calls `DriverManager.quitAll()` only | Must call `engine.shutdown()` first |
| `Interactions(UIEngine)` | Casts `engine.getNativeDriver()` to `WebDriver` | Selenium assumption still present |
| `FrameworkBootstrap.getUtilsConfig()` | Returns mutable shared `Properties` | Must return defensive copy/snapshot |
| `DriverManager` | Exists and is active | Unknown #1 closed |
| Bootstrap tests | No dedicated `FrameworkBootstrap` tests found | Add bootstrap safety-net tests before invasive refactors |
| Architecture enforcement artifacts | `ArchitectureRulesTest` / `TestArchitectureRulesTest` not found in repo | Add in Phase 1.5 before/with risky refactors |

### 0.1 Unknowns status (updated)

- **Unknown #1 (`DriverManager` exists?)**: answered **Yes**.
- **Unknowns #2–#6** remain open and must be documented before implementation.

---

## 1. Why Phase 1 Runs After Phase 2

Phase 1 builds new bootstrap infrastructure (`SessionContext`, engine-first
factory) that holds a `UIEngine` reference. If `UIEngine` still accepts
`Element` at that point, the new infrastructure either accommodates the dual
API — entrenching it in fresh code — or gets refactored again in Phase 2.

Running Phase 2 first means Phase 1 builds against `UIEngine`'s final
shape. That is the only correct order.

---

## 2. Scope

### 2.1 In scope

The five hard blockers from the audit, plus their direct consequences:

| # | Change                                              | Source           |
|---|-----------------------------------------------------|------------------|
| 1 | `UIEngineFactory.create()` — drop `WebDriver` param | C2               |
| 2 | `ExecutionContext` — replace `WebDriver` field with `UIEngine` | C3   |
| 3 | `Interactions` constructor — remove `(WebDriver) engine.getNativeDriver()` cast | C1 |
| 4 | `FrameworkBootstrap.init()` — engine-aware validation | C4              |
| 5 | `VOID.start()` — engine creates its own driver/page  | H5              |
| 6 | `VOID.shutdown()` — call `engine.shutdown()` first   | M7              |
| 7 | `FrameworkBootstrap.getUtilsConfig()` — return defensive snapshot | H6 |

### 2.2 Out of scope (explicit non-goals)

These are tempting to fix while in the runtime layer. They belong to later
phases. **Do not** include them in Phase 1 PRs.

| Item                                              | Owning Phase |
|---------------------------------------------------|--------------|
| Remove `VOID.getDriver()`                         | 4            |
| Remove `By` / `WebElement` from `VoidDSL`         | 4            |
| Remove `Via.locator()` / `Via.webElement()`       | 4            |
| Convert `Interactions` to strict adapter          | 5            |
| Delete `UIContext`                                | 6            |
| Implement `PlaywrightEngine`                      | 8            |

If Phase 1 touches any of these, the PR is rejected and split.

---

## 3. Decisions That Must Close Before Implementation

Each decision below must be answered and documented (ADR if non-obvious)
before the first Phase 1 PR opens.

### D1 — Engine selection mechanism

**Question:** How does the framework know which engine to instantiate?

**Options:**
- `engine.type=selenium|playwright` in `framework.properties`
- System property `void.engine=selenium`
- Programmatic API on `VOID` builder
- Default to `selenium` when unspecified (backward compat)

**Recommendation:** Properties file with system property override; default
to `selenium`. Standard, reversible, zero-friction for existing tests.

**Status:** ⬜ Open

### D2 — `EngineConfig` shape

**Question:** What does `UIEngineFactory.create(EngineConfig)` accept?

**Options:**
- Subclass per engine (`SeleniumConfig`, `PlaywrightConfig`) — typesafe but
  forces callers to know engine type, defeats abstraction
- Flat `Properties` bag the engine interprets — flexible but stringly-typed
- Discriminated union with engine-specific sections — middle ground

**Recommendation:** Flat `EngineConfig` wrapping `Properties` for Phase 1.
Refactor to typed configs in Phase 8 when Playwright lands and the second
engine reveals what actually needs to vary. Do not over-engineer ahead of
the second engine.

**Status:** ⬜ Open

### D3 — `SessionContext` vs. modify `ExecutionContext`

**Question:** Rename or keep the type that holds `UIEngine`?

**Options:**
- Rename `ExecutionContext` → `SessionContext`
- Keep name, change field type only

**Recommendation:** Rename. Phase 1 is the right time. Later phases will
lean on the new name as a clear signal that the type is engine-agnostic.

**Status:** ⬜ Open

### D4 — `getDriver()` survival strategy

**Question:** `VOID.getDriver()` removal is Phase 4. How does it work
between Phase 1 and Phase 4?

**Options:**
- Keep the field, populate via `engine.getNativeDriver()` cast — perpetuates
  the cast Phase 1 tried to eliminate
- Make accessor compute `(WebDriver) engine.getNativeDriver()` at the
  chokepoint — moves the cast to a single place, defensible
- Remove `getDriver()` in Phase 1 — scope creep into Phase 4

**Recommendation:** Single chokepoint cast at the accessor, marked
`@Deprecated(forRemoval = true)`. One cast in the entire codebase. Removed
in Phase 4.

**Status:** ⬜ Open

### D5 — `DriverContext` fate

**Question:** Does `DriverContext` survive Phase 1, get replaced, or get
bypassed?

**Context:** `DriverContext.setPrimaryDriver` / `getActiveDriver` smell like
ThreadLocal-based driver tracking for parallel tests. Replacing
engine-agnostically means `SessionContext` ThreadLocal — re-thinking
concurrency model.

**Recommendation:** Decide *explicitly* before starting. Do not leave
ambiguous. This is the single biggest Phase 1 risk; see §6 R3.

**Status:** ⬜ Open — depends on Unknown #3 (parallel execution)

---

## 4. Unknowns To Resolve First

Before drafting code, answer these by reading the actual codebase. Budget
half a day; do not skip.

| # | Unknown                                                          | Why it matters                                          |
|---|------------------------------------------------------------------|---------------------------------------------------------|
| 1 | Does `DriverManager` exist as a separate class?                  | If yes, it's a Phase 1 touchpoint                       |
| 2 | How many tests directly call `VOID.getDriver()`?                 | Determines viability of D4's chokepoint approach        |
| 3 | Does the framework run tests in parallel today?                  | Determines `DriverContext` urgency (D5)                 |
| 4 | Is there a `profile → Properties` mapping already?               | Determines whether `EngineConfig` is wrapper or new build |
| 5 | What does `SeleniumEngine.shutdown()` currently do?              | Determines whether shutdown ordering needs fixing too   |
| 6 | Are there integration tests for `FrameworkBootstrap`?            | Determines confidence level for changing init order     |

Document the answers in the Phase 1 ADR or in a `phase-1-discovery.md`
alongside this plan.

---

## 5. Required Migration Recipes

Phase 1 must ship with concrete migration recipes for each user-visible
change. Without these, downstream test suites will resist or break silently.

### 5.1 For test authors using `VOID.getDriver()`

```java
// Before
WebDriver driver = void_.getDriver();
driver.findElement(By.id("foo")).click();

// After (Phase 1, transitional)
WebDriver driver = void_.getDriver();   // still works, deprecated
driver.findElement(By.id("foo")).click();

// Target (Phase 4+)
void_.dsl().clickOn(fooElement);        // through DSL, engine-agnostic
```

### 5.2 For tests that construct engines directly

```java
// Before
WebDriver driver = DriverManager.createDriver(profile);
UIEngine engine = UIEngineFactory.create(props, driver);

// After
EngineConfig config = EngineConfig.from(profile);
UIEngine engine = UIEngineFactory.create(config);
// Engine creates its own driver internally.
```

### 5.3 For framework-extending code touching `ExecutionContext`

```java
// Before
ExecutionContext ctx = new ExecutionContext(props, driver);
ctx.getDriver().manage().timeouts()...;

// After
SessionContext ctx = new SessionContext(config, engine);
ctx.engine().setImplicitWait(...);   // engine-agnostic
```

---

## 6. Risks

### R1 — Scope creep into Phase 4

**Trigger:** "While I'm in `VOID.start()` anyway, let me also clean up
`getDriver()`."

**Mitigation:** Reviewer enforces §2.2 (out-of-scope list). Phase 1 PRs that
touch out-of-scope items get split or rejected. The PR template's "Phase"
field flags it explicitly.

### R2 — Test breakage cascade

**Trigger:** Changing `UIEngineFactory.create()` signature breaks every test
that constructs engines directly (likely some unit tests).

**Mitigation:** Add a `@Deprecated(forRemoval = true)` overload preserving
the old signature for one phase. Delete in Phase 2. Catalog all call sites
*before* changing the signature.

### R3 — Parallel execution regression

**Trigger:** If `DriverContext` provides parallel test isolation and Phase 1
replaces it with something subtly different, parallel runs break in ways
that surface late (flaky CI, never on developer machines).

**Mitigation:** Resolve D5 before coding. Add a parallel-execution
verification job to CI before merging the `DriverContext` replacement.
Smoke test must run with `-DforkCount=4` or equivalent.

### R4 — Properties mutation surprise

**Trigger:** Audit H6 — `FrameworkBootstrap.getUtilsConfig()` returns
mutable `Properties`. If callers rely on mutating the returned object to
influence runtime, they break silently when Phase 1 returns a defensive
snapshot.

**Mitigation:** `grep -r "getUtilsConfig().setProperty"` (and `put`,
`remove`, `clear`) before changing the return type. Document any callers
found. If any exist, they get migrated to a proper config-update API as
part of Phase 1.

### R5 — Missing abstraction surfaces

**Trigger:** Making bootstrap engine-aware reveals that "framework-level"
config is actually Selenium-specific (browser type, driver path, etc.).

**Mitigation:** Expected and acceptable. Park Selenium-specific keys inside
`EngineConfig` (interpreted by `SeleniumEngine`). Do not try to design the
final config schema; revisit in Phase 8 when Playwright forces the
question. Pretending no separation is needed will hurt later — explicit
Selenium-namespaced keys are fine for now.

### R6 — Shutdown order regression

**Trigger:** Today `VOID.shutdown()` quits the driver. After Phase 1, it
should call `engine.shutdown()` first. If `SeleniumEngine.shutdown()` is a
no-op (Unknown #5), shutdown may stop quitting the browser entirely.

**Mitigation:** Verify Unknown #5 first. If `SeleniumEngine.shutdown()` is
a no-op, Phase 1 must implement it (call `driver.quit()` from within the
engine). Smoke test verifies browser process actually exits.

---

## 7. Preconditions

Before Phase 1 starts, all must be true:

- ✅ Phase 0 merged — rules locked, ArchUnit passing
- ⬜ Phase 2 merged — `UIEngine` no longer accepts `Element`; resolution unified
- ⬜ Decisions D1–D5 closed and documented (ADR if non-obvious)
- ⬜ Unknowns 1–6 answered in writing
- ⬜ End-to-end smoke test exists exercising current bootstrap → click → shutdown
- ⬜ Parallel-execution verification job exists in CI (or documented as N/A
  per Unknown #3)

The smoke test is non-negotiable. Phase 1 is the kind of change where "all
tests pass" can mean "all tests pass because they share the broken code
path." A real browser-driving smoke test is the only safety net that
distinguishes the two.

---

## 8. Exit Criteria

Phase 1 is **done** when all of the following hold:

| # | Criterion                                                                      |
|---|--------------------------------------------------------------------------------|
| 1 | `UIEngineFactory.create()` accepts `EngineConfig` only — no `WebDriver` param  |
| 2 | `SessionContext` exists, holds `UIEngine`, replaces `ExecutionContext`         |
| 3 | `Interactions` constructor contains zero Selenium casts                        |
| 4 | `FrameworkBootstrap.init()` validates engine-specific resources, not Selenium-by-default |
| 5 | `VOID.start()` does not call `DriverManager.createDriver()` directly           |
| 6 | `VOID.shutdown()` calls `engine.shutdown()` before any other cleanup           |
| 7 | `FrameworkBootstrap.getUtilsConfig()` returns a defensive copy                 |
| 8 | A `NullEngine` (or `MockEngine`) test demonstrates bootstrap completing without Selenium on the classpath |
| 9 | Smoke test passes on Selenium engine (regression safety net)                   |
| 10| Parallel-execution job passes (R3 mitigation)                                  |

Criterion 8 is the proof Phase 1 worked. If a `NullEngine` cannot bootstrap
the framework, Phase 1 is not done — regardless of how many other criteria
pass.

---

## 9. Effort Estimate

Genuinely uncertain — depends on unknowns. Three bands:

| Scenario       | Conditions                                                                 | Estimate     |
|----------------|----------------------------------------------------------------------------|--------------|
| **Best case**  | `DriverManager` is small; no parallel execution; few `getDriver()` callers; `engine.shutdown()` already correct | 3–5 days     |
| **Expected**   | One or two unknowns moderately tangled                                     | 1.5–2 weeks  |
| **Worst case** | `DriverContext` load-bearing for parallel execution; hundreds of `getDriver()` callers; bootstrap config half Selenium-specific | 3–4 weeks    |

**If the worst case looks plausible after closing the unknowns, split:**

- **Phase 1a** — `UIEngineFactory`, `EngineConfig`, `SessionContext`,
  `Interactions` cast removal, `FrameworkBootstrap` validation, shutdown
  ordering
- **Phase 1b** — `DriverContext` replacement, parallel execution model

Splitting is a feature, not a failure. Two reviewable PRs beat one
unreviewable PR every time.

---

## 10. Sequencing Within Phase 1

Suggested order to keep PRs small and reviewable:

1. **PR 1 — Add `EngineConfig`** (additive, no breaks)
2. **PR 2 — Add `UIEngineFactory.create(EngineConfig)` overload** (additive,
   keep old signature deprecated)
3. **PR 3 — `SessionContext` introduced; `ExecutionContext` deprecated**
4. **PR 4 — `FrameworkBootstrap` engine-aware validation + defensive config copy**
5. **PR 5 — `VOID.start()` switched to engine-first lifecycle; `getDriver()` becomes single-chokepoint cast**
6. **PR 6 — `VOID.shutdown()` calls `engine.shutdown()` first; engine implements driver cleanup if needed**
7. **PR 7 — `Interactions` constructor cast removed**
8. **PR 8 — `NullEngine` test added; criterion 8 proven**
9. **(Optional, if 1b)** **PR 9+** — `DriverContext` replacement

Each PR is independently reviewable, mergeable, and revertible. None
introduces a Phase 1 exit criterion until PR 8.

---

## 11. Open Questions Tracker

Use this section to record discoveries during prospecting that change the
plan. Update before each weekly Phase 1 sync.

| Date | Question / Discovery                                | Resolution / Next Step      |
|------|-----------------------------------------------------|-----------------------------|
| —    | (none yet)                                          | —                           |
