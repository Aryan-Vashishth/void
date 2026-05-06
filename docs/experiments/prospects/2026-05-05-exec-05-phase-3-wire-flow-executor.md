# Phase 3 — Wire FlowExecutor into Runtime

> **Status:** Prospect.
>
> **Phase number:** 3 (audit numbering, stable).  
> **Execution order:** 5 of 10 — runs after Phase 1.5 (enforcement gates).  
> **Precedes:** Phase 4 (exec order 6).  
> **Succeeds:** Phase 1.5 (exec order 4).
>
> **Goal:** Make runtime execution pipeline-first (`Action → Flow → FlowExecutor
> → UIEngine`) while keeping backward compatibility.
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
| **5** | **3** | **Wire `FlowExecutor` into Runtime ← you are here** | Phase 1, 2 |
| 6 | 4 | Remove Selenium Leakage from Public Surface | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. In scope

- Add runtime-owned `FlowExecutor` lifecycle in `VOID` session.
- Expose executor-first runtime API for new code.
- Ensure shutdown calls `engine.shutdown()` before residual cleanup.
- Keep `Interactions` as compatibility adapter only.

## 2. Out of scope

- Removing Selenium types from public DSL (Phase 4).
- Full Interactions conversion/removal (Phase 5).

---

## 3. Code touchpoints

- `src/main/java/core/runtime/VOID.java`
- `src/main/java/core/executor/FlowExecutor.java`
- `src/main/java/core/flow/Flow.java`
- `src/main/java/core/interactions/Interactions.java`
- `src/main/java/dsl/VoidDSL.java`

---

## 4. Implementation sequence

1. Introduce executor field into runtime session object.
2. Add API surface to run `Action` and `Flow` directly from runtime.
3. Route all new runtime pathways through executor.
4. Update `shutdown()` ordering to engine-first lifecycle.
5. Mark runtime shortcuts as deprecated where they bypass pipeline.

---

## 5. Risks and mitigations

- Runtime dual-path divergence.
  - Mitigation: add parity tests between `interaction()` and executor path.
- Shutdown regressions.
  - Mitigation: integration test for start -> execute -> shutdown.
- Caller confusion.
  - Mitigation: docs/examples prioritize executor API first.

---

## 6. Test strategy

- Runtime integration tests around `VOID.start()` with flow execution.
- Backward compatibility tests for `interaction()`.
- Lifecycle tests verifying engine-first shutdown ordering.

---

## 7. Exit criteria

- Runtime has a first-class executor path for new code.
- New runtime behavior enters through FlowExecutor by default.
- `VOID.shutdown()` executes `engine.shutdown()` first.
- No new direct `UIEngine` calls are introduced outside Action/Flow path.

