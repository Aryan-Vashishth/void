# VOID Framework Architecture

> **Status:** Active refactor. Phase 0 — *Lock Architecture Rules*.
>
> This document defines the rules that govern the VOID codebase during the
> engine-decoupling refactor. The rules are non-negotiable. They are enforced
> by ArchUnit tests (`ArchitectureRulesTest`) and PR review
> (`.github/pull_request_template.md`). Changing a rule requires an ADR.
>
> Last updated: 2026-05-05

---

## 1. North Star

VOID has exactly one execution path:

```
Element → Action → Flow → FlowExecutor → UIEngine
```

Every UI behavior travels this path. There are no shortcuts, no parallel
pipelines, no exceptions. Code that violates this contract is rejected at PR
review and fails the build.

The audit found resolution living in three places (`Action`, `UIEngine`,
`Interactions`) and Selenium types leaking into the DSL, runtime, and
factories. Phase 0 does not fix any of that. Phase 0 stops it from getting
worse while later phases unwind it.

---

## 2. Layer Ownership

| Layer          | Responsibility                                  | Must Not                                                |
|----------------|-------------------------------------------------|---------------------------------------------------------|
| `Element`      | Identity + capability declaration               | Resolve itself; call engines                            |
| `Action`       | Intent + resolution (single source of truth)    | Be bypassed; expose engine types                        |
| `Flow`         | Composed sequence of Actions                    | Resolve elements; call the engine directly              |
| `FlowExecutor` | Run Flows; orchestrate hooks; manage retries    | Resolve elements; know about Selenium / Playwright      |
| `UIEngine`     | Execute `LocatorDescriptor` against driver/page | Accept `Element`; resolve intent; leak engine types     |
| `Interactions` | **Adapter only** — delegates to `FlowExecutor`  | Contain execution, resolution, waits, or retry logic    |
| `VoidDSL`      | Public, engine-agnostic contract                | Expose `By`, `WebElement`, or any Selenium type         |

The single most important rule: **only `Action` resolves.** Resolution is not
the engine's job, not Interactions' job, not the DSL's job.

---

## 3. The Rules

Each rule maps to one or more enforcement mechanisms. `[A]` = ArchUnit,
`[P]` = PR review, `[F]` = freeze list (existing violations grandfathered,
new ones blocked).

**R1 — Single execution path.** `[P]` New UI behavior enters the runtime
through `Action` and executes via `FlowExecutor`. No new code may call
`UIEngine.click()` or any other `UIEngine` method directly outside the
Action / Flow execution path.

**R2 — Engine purity.** `[A]` `UIEngine` and any non-`selenium` engine package
must not import `org.openqa.selenium.*`. The selenium adapter is the only
place Selenium types are allowed at runtime. Legacy packages (`interactions`,
`dsl`, `context`, `driver`, `locator`, `via`, `bootstrap`) are quarantined
with a removal phase assigned to each.

**R3 — Resolution ownership.** `[A][F]` `LocatorResolvers` may only be
referenced from `core.action`. Existing usages in `Interactions` and engine
code are frozen; new ones are blocked.

**R4 — Engine accepts descriptors only.** `[A][F]` `UIEngine` interface
methods must not accept `Element`. Inputs are `LocatorDescriptor` and
primitives.

**R5 — DSL purity.** `[A][F]` No public method or field on `VoidDSL` (or any
public DSL surface) may have a Selenium type in its signature.

**R6 — UIContext is frozen.** `[A]` No new code may depend on `UIContext`.
Existing consumers are listed in the frozen-consumer set inside
`ArchitectureRulesTest`. Removing UIContext entirely is Phase 6.

**R7 — Interactions is frozen.** `[A][F][P]` `Interactions` accepts no new
methods, no new direct engine calls, no new resolver calls. Existing direct
calls are frozen (Phase 5 removes them); new ones are blocked.

**R8 — Hooks are pipeline-native.** `[P]` Hooks (`Before`, `After`,
`ActionHandler`) execute as Action decorators via `withHooks(...)`. Hooks
must not access `UIContext` or any global state.

**R9 — Tests follow the same architecture.** `[A][P]` Tests obey R1–R8. Test
code must enter behavior through `VoidDSL` or `FlowExecutor`; it must not
call `UIEngine` methods directly, must not call `LocatorResolvers`, must not
depend on `UIContext`, and must not import `org.openqa.selenium.*`. Two
exemptions only:

- Engine adapter tests (`..core.engine.selenium..`, future
  `..core.engine.playwright..`) — the engine implementation *is* the unit
  under test
- Layer-boundary framework tests (`..core.action..`, `..core.flow..`) —
  these tests verify that the Action / Flow layer calls UIEngine correctly,
  typically via mocks

Feature tests, regression tests, integration tests, page-object tests, and
anything else — no exceptions.

---

## 4. Execution Traps

These are the failure modes that reintroduce dual pipelines. Each one is a
PR-blocking violation in new code.

**Trap 1 — The "temporary" shortcut.**

```java
engine.click(element);                       // BANNED: bypasses Action
LocatorResolvers.strict().resolve(element);  // BANNED: resolution outside Action
UIContext.getLastElement();                  // BANNED: global state
UIContext.setLastActionTarget(...);          // BANNED: global state
```

These look harmless. They are not. Even one occurrence permanently
re-establishes the dual-pipeline problem the audit identified.

**Trap 2 — Phase skipping.** Wiring `FlowExecutor` into the runtime before
resolution is unified, or keeping `UIEngine.resolve(Element, ...)` alive
after Phase 2 closes. Phases have explicit exit criteria; do not advance
without meeting them.

**Trap 3 — Interactions creep.** Adding direct `UIEngine` calls, retries, or
waits to `Interactions` "just for now." Interactions is an adapter. If a
behavior is missing, add it to an `Action`, not to `Interactions`.

**Trap 4 — The test escape hatch.** *"Just fixing a test quickly"* with
`engine.click(locator)` inside a test. This is the most common shortcut and
the fastest way to undo every other rule — once tests bypass the
architecture, production code follows within weeks. R9 scans test bytecode
specifically for this. Exemptions are narrow and listed in R9; everything
else goes through the DSL.

---

## 5. Enforcement

Three mechanisms. All three must pass for a change to merge.

### 5.1 ArchUnit (automated)

Two test classes run as part of the standard test suite:

- `src/test/java/io/voidframework/architecture/ArchitectureRulesTest.java`
  enforces R2, R3, R4, R5, R6, R7 against **production** code.
- `src/test/java/io/voidframework/architecture/TestArchitectureRulesTest.java`
  enforces R9 against **test** code. It uses a custom `OnlyTests` import
  option and applies the same structural constraints as the production
  rules, with the narrow exemptions listed in R9.

If a rule fails, the build fails.

Rules with pre-existing violations are wrapped in `FreezingArchRule.freeze(...)`.
The frozen baseline is committed to `archunit_store/`. Adding a new violation
fails the test; removing one updates the store.

Adding a deliberate violation requires either (a) fixing the violation, or
(b) an ADR documenting the exception, signed off by a code owner.

### 5.2 PR template (review gate)

Every PR completes the architecture checklist in
`.github/pull_request_template.md`. Reviewers verify each item before
approval.

### 5.3 Code owners (review gate)

A `CODEOWNERS` entry covering `core/`, `dsl/`, `ARCHITECTURE.md`, and
`ArchitectureRulesTest.java` ensures architecture-impacting changes route to
a designated reviewer.

---

## 6. Deprecation Strategy

Legacy paths kept for compatibility are marked explicitly:

- `@Deprecated(forRemoval = true)` on every method or class scheduled for removal
- `@SuppressWarnings("DeprecatedIsStillUsed")` is **forbidden** in new code
- IDE warnings on legacy calls are intentional and must not be hidden

Legacy classes targeted for removal:

| Class / Member                                  | Removal Phase | Replacement                          |
|-------------------------------------------------|---------------|--------------------------------------|
| `Interactions` (entire class)                   | Phase 5       | `Action` + `FlowExecutor`            |
| `UIContext` (entire class)                      | Phase 6       | Explicit descriptor passing          |
| `UIEngine.resolve(Element, ...)`                | Phase 2       | Action-owned resolution              |
| `Via.locator(...)` / `Via.webElement(...)`      | Phase 4       | `Via.descriptor(...)`                |
| `UIEngineFactory.create(Properties, WebDriver)` | Phase 1       | `UIEngineFactory.create(EngineConfig)` |
| `VOID.getDriver()`                              | Phase 4       | None (engine-internal)               |
| `ExecutionContext` (with `WebDriver` field)     | Phase 1       | `SessionContext` (engine-holding)    |
| `HookedAction.wrap(...)` (deprecated overload)  | Phase 7       | `Action.withHooks(...)`              |

---

## 7. Migration Policy

When you must touch a legacy class:

1. **Do not extend it.** New methods on `Interactions`, new ThreadLocals on
   `UIContext`, new By-returning helpers on `Via` are hard nos.
2. **Convert at the seam.** If your change makes a legacy callsite
   redundant, migrate it to the new pipeline as part of the same PR.
3. **Document the bridge.** When a temporary bridge is unavoidable, mark it

   ```java
   // PHASE-3 BRIDGE: remove when FlowExecutor wired into VOID.start (#1234)
   ```

   with the removal phase and a tracking issue.

---

## 8. Phase Status

> Rows are listed in **execution order**, not numerical order. Phase numbers
> reflect the original audit and are stable across documents — they do not
> change when execution order changes.

| Order | Phase | Description                                  | Status        |
|-------|-------|----------------------------------------------|---------------|
| 1     | 0     | Lock architecture rules                      | **ACTIVE**    |
| 2     | 2     | Resolution unification                       | NOT STARTED   |
| 3     | 1     | Fix bootstrap & startup ownership            | NOT STARTED   |
| 4     | 1.5   | Lock execution model in code                 | NOT STARTED   |
| 5     | 3     | Wire `FlowExecutor` into runtime             | NOT STARTED   |
| 6     | 4     | Remove Selenium leakage from public surface  | NOT STARTED   |
| 7     | 5     | Convert `Interactions` to strict adapter     | NOT STARTED   |
| 8     | 6     | Remove `UIContext`                           | NOT STARTED   |
| 9     | 7     | Finalize hook model                          | NOT STARTED   |
| 10    | 8     | Add Playwright engine                        | NOT STARTED   |

**Why Phase 2 runs before Phase 1:** Phase 1 builds new bootstrap
infrastructure (`SessionContext`, engine-first factory) that holds a
`UIEngine` reference. If `UIEngine` still accepts `Element` at that point,
the new infrastructure either accommodates the dual API (entrenching it) or
gets refactored again in Phase 2. Running Phase 2 first means Phase 1 builds
against UIEngine's final shape.

**Phase 2 exit criteria (revised for new order):** Resolution unified in
the new pipeline (Action owns it). `UIEngine.resolve(Element, ...)`
deprecated. `LocatorResolvers` calls from `Interactions` remain quarantined
under R3's freeze and are tracked for removal in Phase 5. Do not claim
Phase 2 done while `Interactions` still resolves — claim it done when *new*
pipeline code does not.

Phase 0 has no exit criteria. It remains active for the duration of the
refactor and can only be retired once Phase 8 closes successfully and the
team agrees the rules are no longer needed (they probably will be).

---

## 9. Adjusting These Rules

Rules change only via ADR (Architecture Decision Record). To propose a change:

1. Open a PR adding `docs/adr/NNNN-title.md`
2. Describe the change, the motivation, and the affected rules
3. Update `ArchitectureRulesTest` and this document in the same PR
4. Require approval from at least one code owner

The bar for relaxing a rule is high. The bar for adding a rule is low.
