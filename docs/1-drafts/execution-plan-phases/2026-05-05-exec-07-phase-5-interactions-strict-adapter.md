# Phase 5 — Convert Interactions to Strict Adapter

> **Status:** Prospect.
>
> **Phase number:** 5 (audit numbering, stable).  
> **Execution order:** 7 of 10 — runs after Phase 4 (public surface cleanup).  
> **Precedes:** Phase 6 (exec order 8).  
> **Succeeds:** Phase 4 (exec order 6).
>
> **Goal:** Reduce `Interactions` to a strict compatibility adapter with no
> independent execution or resolution authority.
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
| **7** | **5** | **Convert `Interactions` to Strict Adapter ← you are here** | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. In scope

- Remove direct resolver ownership from `Interactions` modern paths.
- Route `Interactions` behavior through Action/Flow/FlowExecutor pipeline.
- Remove Selenium cast from `Interactions(UIEngine)` constructor.
- Keep legacy method names as thin wrappers where required.

## 2. Out of scope

- Deleting `Interactions` class entirely.
- Removing `UIContext` class (Phase 6).

---

## 3. Code touchpoints

- `src/main/java/core/interactions/Interactions.java`
- `src/main/java/core/actions/*`
- `src/main/java/core/flow/Flow.java`
- `src/main/java/core/executor/FlowExecutor.java`
- `src/main/java/core/driver/DriverContext.java`

---

## 4. Implementation sequence

1. Group Interactions methods by behavior (click/type/dropdown/search).
2. Introduce internal adapters that emit Actions for each behavior.
3. Delegate execution through FlowExecutor.
4. Remove direct `LocatorResolvers` field usage from migrated paths.
5. Remove constructor native-driver cast and rewire legacy dependency points.

---

## 5. Risks and mitigations

- Large class surface (high churn risk).
  - Mitigation: migrate in behavior slices with parity tests.
- Behavioral drift in waits/retries.
  - Mitigation: keep waits inside engine; avoid adapter re-implementation.
- Hook sequencing differences.
  - Mitigation: preserve before/after ordering via Action decorators.

---

## 6. Test strategy

- Characterization tests before each migration slice.
- Parity tests for key Interactions methods after delegation.
- Architecture checks ensuring no new resolver or direct execution logic is added.

---

## 7. Exit criteria

- Interactions no longer acts as independent resolver/executor.
- Interactions constructor has no `WebDriver` cast from native driver.
- Legacy APIs remain available only as thin delegating adapters.
- New behavior additions happen in Action/Flow pipeline, not in Interactions.

