# Outcome: Phase 0 — Lock Architecture Rules

**Status:** In Progress (Phase 0 has no exit criteria — active for entire refactor duration)  
**Date:** 2026-05-05  
**Scope:** Architectural rule definition, enforcement infrastructure, and codebase guardrails  
**Impact on user-facing API:** None

---

## Summary

Phase 0 established the non-negotiable rules that govern the VOID codebase during the
engine-decoupling refactor. The goal was not to fix anything — it was to stop the
architecture from getting worse while later phases unwind the legacy coupling identified
in the audit.

The rules are enforced automatically (ArchUnit), at review (PR template, CODEOWNERS),
and through explicit deprecation markers. Any new code that violates the rules fails the
build.

---

## What Was Achieved

### 1. Architecture Rules Defined (9 rules — R1 through R9)

| Rule | Name | Enforcement |
|------|------|-------------|
| R1 | Single execution path | PR review |
| R2 | Engine purity — no Selenium in non-selenium packages | ArchUnit |
| R3 | Resolution ownership — `LocatorResolvers` only from `core.action` | ArchUnit + Freeze |
| R4 | UIEngine accepts descriptors only — no `Element` in method signatures | ArchUnit + Freeze |
| R5 | DSL purity — no Selenium types on public `VoidDSL` surface | ArchUnit + Freeze |
| R6 | UIContext frozen — no new dependencies | ArchUnit |
| R7 | Interactions frozen — no new methods, engine calls, or resolver calls | ArchUnit + Freeze + PR |
| R8 | Hooks are pipeline-native, no global state access | PR review |
| R9 | Tests obey R1–R8, with two narrow exemptions | ArchUnit |

### 2. Enforcement Infrastructure Wired

- `ArchitectureRulesTest` — enforces R2, R3, R4, R5, R6, R7 against production bytecode
- `TestArchitectureRulesTest` — enforces R9 against test bytecode (custom `OnlyTests` import option)
- `FreezingArchRule.freeze(...)` — pre-existing violations grandfathered; new ones fail the build
- Frozen baselines committed to `archunit_store/`

### 3. Deprecation Layer Established

All legacy paths targeted for removal are marked `@Deprecated(forRemoval = true)`. A removal
schedule is committed alongside the rules:

| Class / Member | Removal Phase |
|----------------|---------------|
| `Interactions` (entire class) | Phase 5 |
| `UIContext` (entire class) | Phase 6 |
| `UIEngine.resolve(Element, ...)` | Phase 2 |
| `Via.locator(...)` / `Via.webElement(...)` | Phase 4 |
| `UIEngineFactory.create(Properties, WebDriver)` | Phase 1 |
| `VOID.getDriver()` | Phase 4 |
| `ExecutionContext` (with `WebDriver` field) | Phase 1 |
| `HookedAction.wrap(...)` (deprecated overload) | Phase 7 |

### 4. Structural Work Already Delivered (from Archive)

Cross-referencing `2026-05-01-multi-engine-execution.md`, the following structural
pieces were delivered as part of the Phase 0 / Phase 1–2 wave and are now locked
under the rules:

**Core Contracts (all in `core.engine`):**
- `UIEngine` interface — lifecycle, resolution, actions, retrieval, waits, advanced ops
- `LocatorDescriptor` record — value, strategy, args, optional scoped parent
- `LocatorStrategy` enum — XPATH, CSS, ID, NAME with inference
- `EngineConfig` holder — timeout, polling, baseUrl

**Execution Pipeline (locked under R1):**
- `Action` functional interface (`core.actions`) — deferred execution intent
- `Flow` (`core.flow`) — immutable ordered sequence of Actions
- `FlowExecutor` (`core.executor`) — iterates Flows against UIEngine; orchestrates hooks and retries

**Engine Layer (locked under R2):**
- `SeleniumEngine` — implements `UIEngine`, wraps current WebDriver behavior
- `UIEngineFactory` — config-driven engine selection (`engine=selenium|playwright`)
- `FrameworkBootstrap` — one-time JVM initialization

**Capability Interfaces (locked under R4):**
- All 15 capability interfaces emit deferred `Action` objects
- `Clickable`, `Typeable`, `Selectable`, `CheckboxAction`, `DropdownAction`, `SearchableDropdownAction`,
  `MultiDropdownAction`, `ReadOnlyAction` all enriched with engine-delegating methods

**Interaction–Execution Separation (from `2026-06-interaction-execution-separation.md`):**
- `Interactions` refactored to pure orchestrator — no `WebDriver` or `WebElement` in new paths
- All new code paths: `resolve → LocatorDescriptor → UIContext → before-hooks → engine.* → after-hooks`
- `LocatorDescriptor.withParent()` — composable scoped locator tree
- `UIContext` made engine-agnostic (`lastActionTarget` as `ThreadLocal<LocatorDescriptor>`)
- `Via.descriptor(...)` methods added; all `Via.locator(...)` and `Via.webElement(...)` deprecated

### 5. Phase Execution Order Locked

The dependence between phases was explicitly reasoned and documented:
- Phase 2 (Resolution unification) **runs before** Phase 1 (Bootstrap) so new infrastructure
  builds against UIEngine's final shape — not a shape that gets refactored again.
- Phase 0 has no exit criteria. It remains active until Phase 8 closes.

---

## Architecture Guardrails (the Four Traps blocked by these rules)

| Trap | Pattern | Blocked by |
|------|---------|------------|
| Trap 1 — "Temporary" shortcut | `engine.click(element)`, `UIContext.getLastElement()` | R1, R6 (ArchUnit) |
| Trap 2 — Phase skipping | Wiring FlowExecutor before resolution is unified | R3, R4 (ArchUnit + Freeze) |
| Trap 3 — Interactions creep | Adding execution logic to `Interactions` | R7 (ArchUnit + Freeze + PR) |
| Trap 4 — Test escape hatch | `engine.click(locator)` inside feature/regression tests | R9 (ArchUnit on test bytecode) |

---

## What Was Not Achieved (by design)

Phase 0 intentionally did **not** fix any existing violations. The following remain in
place and are quarantined under freeze rules:

- `Interactions` still contains legacy `WebDriver`-accepting constructors and methods (frozen, Phase 5)
- `UIContext` is still consumed in its legacy form by quarantined callers (frozen, Phase 6)
- `LocatorResolvers` is still referenced from `Interactions` (frozen under R3, Phase 5)
- Selenium types still leak through `Via.locator(...)` / `Via.webElement(...)` call sites (frozen, Phase 4)

These are known, bounded, and tracked. No new violations have been introduced.

---

## Design Decisions Made

1. **Rules before fixes** — Phase 0 locks the contract before touching any legacy code.
   This prevents churn where a fix in Phase 1 gets quietly re-violated before Phase 3 lands.

2. **Freeze over mass-migration** — Pre-existing violations are frozen (not force-migrated)
   to avoid a big-bang refactor that destabilizes a working test suite.

3. **Two ArchUnit test classes** — Production rules and test rules are separated so that
   test exemptions (engine adapter tests, layer-boundary tests) are explicit and auditable,
   not implicit carve-outs in a single rule set.

4. **Narrow test exemptions (R9)** — Only engine adapter tests and layer-boundary framework
   tests may use `UIEngine` directly. Everything else — feature tests, regression tests,
   page-object tests — goes through `VoidDSL` or `FlowExecutor`. No exceptions.

---

## Success Criteria (Phase 0)

- [x] All nine rules defined
- [x] ArchUnit enforcement wired for R2–R7 (production) and R9 (test)
- [x] Freeze baselines committed for pre-existing violations
- [x] Deprecation schedule committed for all targeted legacy classes
- [x] Phase execution order reasoned and documented
- [x] PR template and CODEOWNERS gates specified
- [x] Structural foundation delivered: `UIEngine`, `Action`, `Flow`, `FlowExecutor`, `SeleniumEngine`, `UIEngineFactory`, `FrameworkBootstrap`
- [x] `Interactions` refactored to pure orchestrator (new code paths)
- [x] Zero new violations introduced since rules were locked

---

## Next Phase

**Phase 2 — Resolution Unification** (NOT STARTED)

Resolution must be unified in the new pipeline (`Action` owns it). `UIEngine.resolve(Element, ...)`
must be deprecated. `LocatorResolvers` calls from `Interactions` remain quarantined and are removed
in Phase 5. Phase 2 is complete only when *new* pipeline code does not resolve — not when
`Interactions` stops resolving.

