# Action Profiles Refactor and Scalability Plan

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Last Updated:** 2026-06-13  
**Area:** `core.actions`, `core.interactions.hooks`, `core.executor`, docs and DX

---

## Goal

Keep `main` stable while evolving VOID from hook-heavy usage to profile-driven execution, with clear observability, bounded complexity, and a safe path to remove deprecated API.

---

## Why This Plan Exists

The audit identified healthy architecture with rising entropy risk in three places:

- Action API growth
- Branching growth (`if/else/switch` density)
- Hook/profile complexity hidden from users

This plan prevents accidental complexity while preserving backward compatibility.

---

## Non-Negotiable Guardrails (Apply in Every Phase)

- [ ] Keep `main` releasable; all risky refactor work stays on feature branches.
- [ ] Require local compile before push: `mvn -DskipTests compile`.
- [ ] If a class reaches 3+ branch paths, evaluate polymorphism.
- [ ] Keep core branch density target under 5 conditionals per class.
- [ ] Track Action API size; redesign trigger at 12-15 public methods.
- [ ] No engine leakage into test DSL (`UIEngine` access only where intended).

---

## Phase 0 - Stabilization Baseline (Done and Enforced)

> Detailed doc: [`phase-0-stabilization-baseline.md`](done/phase-0-stabilization-baseline.md)

**Objective:** lock a clean baseline after CI breakage so future refactors are traceable.

### Checklist

- [x] Fix compile blocker in `src/main/java/tests/demo/VoidDemo.java`.
- [x] Apply fix on both `main` and `feature/action-package-refactor`.
- [x] Verify compile locally on both branches.
- [ ] Add CI checklist item: demo API usage must match current Action API.

### Exit Criteria

- `main` builds in CI with no compile errors.
- Feature branch is rebased/cherry-picked with identical compile-critical fixes.

---

## Phase 1 - Profile API Consolidation

> Detailed doc: [`phase-1-profile-api-consolidation.md`](ongoing/phase-1-profile-api-consolidation.md)

**Objective:** make profile APIs predictable and reduce low-level hook exposure.

### Scope

- Keep user-friendly entry points (`safely()`, `debug()`, `raw()`, `using(...)`).
- Keep low-level hook plumbing available but secondary.
- Align demo and docs to one preferred style.

### Checklist

- [ ] Confirm canonical public API on `Action` and document it in `core.actions` docs.
- [ ] Ensure each shorthand maps to explicit before/after behavior.
- [ ] Audit examples to avoid mixed styles unless intentional.
- [ ] Add/refresh unit tests around shorthand behavior and composition order.

### Exit Criteria

- Public API is stable and documented.
- Profile behavior is deterministic across capabilities.

---

## Phase 2 - Observability First (`ActionTrace`)

> Detailed doc: [`phase-2-observability-action-trace.md`](ongoing/phase-2-observability-action-trace.md)

**Objective:** make execution pipeline visible before adding more behavior.

### Trace Model (MVP)

- Action identity (`element + operation`)
- Selected profile (`SAFE`, `DEBUG`, `RAW`, custom)
- Before hooks list
- Execution step and duration
- After hooks list
- Final status and error details (if any)

### Checklist

- [ ] Introduce `ActionTrace` data model.
- [ ] Add instrumentation point in action execution pipeline.
- [ ] Provide logger output in debug mode.
- [ ] Add tests for trace ordering and failure capture.

### Exit Criteria

- Developers can answer: what ran, in what order, and why it failed.

---

## Phase 3 - Capability Resolution Hardening

> Detailed doc: [`phase-3-capability-resolution-hardening.md`](ongoing/phase-3-capability-resolution-hardening.md)

**Objective:** prevent `instanceof` branching explosion as capabilities grow.

### Direction

Move toward self-describing capabilities:

```java
interface ActionCapabilityProvider {
    ActionCapability capability();
}
```

### Checklist

- [ ] Introduce provider contract and default adaptation path.
- [ ] Migrate top capabilities first (`Clickable`, `Typeable`, `Selectable`).
- [ ] Keep compatibility fallback while migration is incomplete.
- [ ] Remove central branching where provider is available.

### Exit Criteria

- New capabilities register themselves without adding central `if/else` chains.

---

## Phase 4 - Capability-Driven Hook Selection

> Detailed doc: [`phase-4-capability-driven-hook-selection.md`](ongoing/phase-4-capability-driven-hook-selection.md)

**Objective:** complete Phase 3's self-description promise by wiring `ActionCapabilityProvider` into the action pipeline. Move hook-selection knowledge from a central switch onto the capability interface that owns it.

### Direction

- Fix `ElementActions.capabilityFor()` to delegate to `ActionCapabilityProvider` (all 14 capabilities now report accurate metadata).
- Introduce `ActionProfiles` with `DEFAULT_SAFE` — a shared, immutable profile with no capability-dispatch switch.
- Add `safeProfile()` to `ActionCapabilityProvider` returning `ActionProfiles.DEFAULT_SAFE` (no switch in the default path).
- `ElementBoundAction.safely()` uses the capability's declared profile directly.
- `Profiles.SAFE` is preserved for `.using(Profiles.SAFE)` callers; its switch is the compatibility path, not the primary path.

### Checklist

- [ ] Create `ActionProfiles` with `DEFAULT_SAFE` constant.
- [ ] Fix `capabilityFor()` — delegate to `ActionCapabilityProvider.capability()`.
- [ ] Add `safeProfile()` default to `ActionCapabilityProvider` returning `ActionProfiles.DEFAULT_SAFE`.
- [ ] Override `safeProfile()` in `Clickable`, `Typeable`, `Selectable`, `SearchField`, `SearchableDropdown`.
- [ ] Wire `safeProfile` into `ElementBoundAction` and override `safely()`.
- [ ] Verify no new central dispatcher, resolver, or strategy class is created.

### Exit Criteria

- Adding a new capability with custom safe hooks requires no changes to any existing framework file.
- `ElementActions.capabilityFor()` contains no `instanceof` checks.
- `ElementBoundAction.safely()` reaches no switch for element-bound actions.

---

## Phase 5 - Execution Pipeline Boundary

> Detailed doc: [`phase-5-execution-pipeline-boundary.md`](ongoing/phase-5-execution-pipeline-boundary.md)

**Objective:** keep `FlowExecutor` minimal while enabling retries/timeouts/metrics later.

### Direction

```text
FlowExecutor -> ExecutionPipeline -> Action
```

### Checklist

- [ ] Introduce `ExecutionPipeline` abstraction.
- [ ] Move cross-cutting concerns into pipeline stages.
- [ ] Preserve current behavior as default stage sequence.
- [ ] Add benchmark/regression check for flow overhead.

### Exit Criteria

- `FlowExecutor` remains orchestration-only and small.

---

## Phase 6 - Engine Portability Controls

> Detailed doc: [`phase-6-engine-portability-controls.md`](ongoing/phase-6-engine-portability-controls.md)

**Objective:** keep Playwright and future engine support practical.

### Checklist

- [ ] Create and maintain `Engine Portability Exceptions` section/doc.
- [ ] Tag hooks with portability notes where semantics differ.
- [ ] Audit demo and docs for engine-coupled examples.
- [ ] Add review gate: new engine-specific behavior must be documented.

### Exit Criteria

- Known portability gaps are explicit, not implicit.

---

## Phase 7 - Documentation Realignment

> Detailed doc: [`phase-7-documentation-realignment.md`](ongoing/phase-7-documentation-realignment.md)

**Objective:** ensure contributors learn current architecture, not legacy patterns.

### Checklist

- [ ] Add `Architecture Version` to core architecture docs.
- [ ] Add `Current Architecture` and `Legacy Architecture` sections where needed.
- [ ] Update examples to profile-first style.
- [ ] Keep advanced extension docs for `before/after` users.

### Exit Criteria

- Docs and production API examples are aligned.

---

## Phase 8 - Deprecated Code Removal

> Detailed doc: [`phase-8-deprecated-code-removal.md`](ongoing/phase-8-deprecated-code-removal.md)

**Objective:** remove deprecated APIs safely after migration is complete.

### Preconditions

- Migration guides published.
- Replacement APIs stable for at least one release cycle.
- CI demonstrates no internal usage of deprecated symbols.

### Checklist

- [ ] Inventory deprecated symbols (API + internal).
- [ ] Categorize by risk: low (internal), medium (test DSL), high (public API).
- [ ] Replace all internal call sites first.
- [ ] Add temporary compile checks or static scans for deprecated usage.
- [ ] Remove deprecated code in targeted PRs (small batches).
- [ ] Update changelog and migration notes with before/after examples.

### Exit Criteria

- No deprecated symbol usage remains in core paths.
- Removed APIs are documented in changelog with migration mapping.

---

## Release and Branch Workflow

- `main`: stable, releasable, no partial refactors.
- `feature/action-package-refactor`: active architecture work.
- Use small, phase-scoped PRs with explicit rollback plan.
- For every phase PR, include:
  - behavior diff
  - migration impact
  - test coverage impact

---

## Metrics Dashboard (Track Weekly)

- [ ] Action API public method count
- [ ] Branch density in core packages
- [ ] Number of direct `before/after` usages in docs/examples
- [ ] Number of `instanceof` checks in capability resolution paths
- [ ] Deprecated symbol count remaining
- [ ] CI compile and test pass rate by branch

---

## Priority Order

1. Observability (`ActionTrace`)
2. Capability resolution hardening
3. Hook strategy layer
4. Execution pipeline boundary
5. Docs realignment
6. Deprecated code removal

---

## Out of Scope (For Now)

- Full rewrite of `Action` into a large configurable object model.
- Removing low-level hooks from public surface entirely.
- Engine-specific optimization before portability baseline is documented.

---

## Notes

Hooks remain implementation plumbing. Profiles remain user language. The architecture should keep intent (`Element -> Action -> Flow`) clean while implementation detail moves behind traceable, testable strategies.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

