# Phase 7 — Finalize Hook Model

> **Status:** Prospect.
>
> **Phase number:** 7 (audit numbering, stable).  
> **Execution order:** 9 of 10 — runs after Phase 6 (UIContext removal).  
> **Precedes:** Phase 8 (exec order 10).  
> **Succeeds:** Phase 6 (exec order 8).
>
> **Goal:** Complete hook migration to a single pipeline-native model
> (`Action.withHooks(...)`) and remove legacy bridges.
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
| 3 | 1 | Fix Bootstrap & Startup Ownership | Phase 2 |
| 4 | 1.5 | Lock Execution Model in Code | Phase 1 |
| 5 | 3 | Wire `FlowExecutor` into Runtime | Phase 1, 2 |
| 6 | 4 | Remove Selenium Leakage from Public Surface | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| **9** | **7** | **Finalize Hook Model ← you are here** | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. In scope

- Remove deprecated hook bridges and legacy overloads.
- Guarantee descriptor-aware hook execution in supported paths.
- Keep hook ordering semantics stable (before -> action -> after).

## 2. Out of scope

- New engine implementation work (Phase 8).

---

## 3. Code touchpoints

- `src/main/java/core/actions/HookedAction.java`
- `src/main/java/core/interactions/hooks/ActionHandler.java`
- `src/main/java/core/interactions/hooks/Before.java`
- `src/main/java/core/interactions/hooks/After.java`
- `src/main/java/core/interactions/Interactions.java`

---

## 4. Implementation sequence

1. Identify remaining callers of deprecated hook bridges.
2. Migrate callers to `Action.withHooks(...)`.
3. Remove deprecated hook bridge APIs.
4. Simplify hook execution paths to descriptor-native contracts.
5. Update docs/examples to one canonical hook model.

---

## 5. Risks and mitigations

- Legacy callers still use old hook adapters.
  - Mitigation: release-note migration window before hard removal.
- Ordering regressions.
  - Mitigation: contract tests for sequencing and exception behavior.

---

## 6. Test strategy

- Expand `src/test/java/core/actions/HookPipelineTest.java`.
- Add contract tests for descriptor availability in hooks.
- Architecture checks against legacy hook bridge usage.

---

## 7. Exit criteria

- No supported path depends on deprecated hook bridges.
- Hooks execute only as Action decorators.
- Hook docs and code examples use pipeline-native APIs exclusively.

