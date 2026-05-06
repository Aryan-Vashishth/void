# Phase 6 — Remove UIContext

> **Status:** Prospect.
>
> **Phase number:** 6 (audit numbering, stable).  
> **Execution order:** 8 of 10 — runs after Phase 5 (Interactions adapter).  
> **Precedes:** Phase 7 (exec order 9).  
> **Succeeds:** Phase 5 (exec order 7).
>
> **Goal:** Remove global thread-local `UIContext` state and replace remaining
> behavior with explicit descriptor/context passing.
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
| **8** | **6** | **Remove `UIContext` ← you are here** | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. In scope

- Eliminate production dependencies on `UIContext`.
- Refactor callers to use explicit action target data.
- Remove deprecated `WebElement`-based context methods.

## 2. Out of scope

- Hook package redesign (Phase 7).
- Engine feature expansion.

---

## 3. Code touchpoints

- `src/main/java/core/utils/UIContext.java`
- `src/main/java/core/interactions/Interactions.java`
- `src/main/java/core/utils/web/DOMUtils.java` (fallback usage)
- any residual hook/util classes using `UIContext`

---

## 4. Implementation sequence

1. Inventory all `UIContext` reads/writes.
2. Replace each usage with explicit method parameters or action context objects.
3. Remove write-side calls in Interactions/adapters.
4. Remove read-side fallbacks in utilities.
5. Delete `UIContext` and cleanup dead code.

---

## 5. Risks and mitigations

- Utility behavior relies on implicit thread-local state.
  - Mitigation: add explicit failure messages and clear context contracts.
- Legacy tests assume global state.
  - Mitigation: migrate tests to explicit descriptors and keep temporary helper shims if needed.

---

## 6. Test strategy

- Unit tests for refactored utilities formerly relying on `UIContext`.
- Integration tests for workflows that used last-target behavior.
- Architecture test: forbid new `UIContext` imports/usages.

---

## 7. Exit criteria

- Zero production references to `UIContext`.
- No global thread-local action-target mechanism in runtime path.
- Hooks/actions operate with explicit descriptor passing only.

