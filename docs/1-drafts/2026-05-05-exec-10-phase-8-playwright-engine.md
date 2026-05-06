# Phase 8 — Add Playwright Engine

> **Status:** Prospect.
>
> **Phase number:** 8 (audit numbering, stable).  
> **Execution order:** 10 of 10 — runs after all preceding phases.  
> **Precedes:** — (end of planned sequence).  
> **Succeeds:** Phase 7 (exec order 9).
>
> **Goal:** Deliver a production-quality Playwright engine while preserving one
> architecture path and no Selenium leakage outside selenium adapter code.
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
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| **10** | **8** | **Add Playwright Engine ← you are here** | Phases 1–7 |

---

## 1. In scope

- Add `core.engine.playwright` implementation of `UIEngine`.
- Extend `UIEngineFactory` to instantiate Playwright engine.
- Add engine contract parity tests across Selenium and Playwright.
- Validate full lifecycle (`start -> execute -> shutdown`) with Playwright.

## 2. Out of scope

- New DSL feature expansion.
- Reopening resolved architecture decisions from earlier phases.

---

## 3. Code touchpoints

- `src/main/java/core/engine/playwright/*` (new)
- `src/main/java/core/engine/UIEngineFactory.java`
- `src/main/java/core/engine/EngineConfig.java`
- `src/main/java/core/runtime/VOID.java`
- test suites for cross-engine contract validation

---

## 4. Preconditions

- Phase 1 through Phase 7 complete.
- Descriptor-only engine contract stabilized.
- Architecture enforcement for R2/R5/R9 active.

---

## 5. Implementation sequence

1. Implement Playwright lifecycle and descriptor translation.
2. Wire factory selection and config defaults.
3. Build cross-engine action/flow contract test matrix.
4. Add adapter-level Playwright tests in allowed exemption scope.
5. Update docs and quick-start engine selection guidance.

---

## 6. Risks and mitigations

- Behavior parity drift between engines.
  - Mitigation: shared contract tests run for both engines.
- Hidden Selenium dependencies block portability.
  - Mitigation: architecture checks before enabling Playwright in CI.
- Configuration ambiguity.
  - Mitigation: explicit engine-namespaced config keys.

---

## 7. Test strategy

- Contract tests for all core actions under Selenium and Playwright.
- Engine adapter tests for Playwright package.
- Smoke tests with runtime engine selection set to Playwright.

---

## 8. Exit criteria

- Playwright engine passes shared action/flow contract suite.
- Runtime boots and shuts down correctly with Playwright selected.
- No non-selenium package imports Selenium types.
- Public DSL remains engine-agnostic.

