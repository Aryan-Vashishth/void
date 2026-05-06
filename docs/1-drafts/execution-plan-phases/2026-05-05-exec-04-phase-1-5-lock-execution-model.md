# Phase 1.5 — Lock Execution Model in Code

> **Status:** Prospect.
>
> **Phase number:** 1.5 (audit numbering, stable).  
> **Execution order:** 4 of 10 — runs after Phase 1 (bootstrap).  
> **Precedes:** Phase 3 (exec order 5).  
> **Succeeds:** Phase 1 (exec order 3).
>
> **Goal:** Convert architecture rules from documentation intent into enforced
> build gates.
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
| **4** | **1.5** | **Lock Execution Model in Code ← you are here** | Phase 1 |
| 5 | 3 | Wire `FlowExecutor` into Runtime | Phase 1, 2 |
| 6 | 4 | Remove Selenium Leakage from Public Surface | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. Problem statement

The architecture document references enforcement artifacts that are not present
in the snapshot:

- `ArchitectureRulesTest` not found
- `TestArchitectureRulesTest` not found
- `archunit_store/` not found
- `.github/pull_request_template.md` and `CODEOWNERS` references are not verified

Without these, R1-R9 are policy-only, not code-enforced.

---

## 2. In scope

- Add ArchUnit production rules for R2-R7.
- Add ArchUnit test rules for R9 (with narrow exemptions).
- Add freeze baseline mechanism for existing violations.
- Add/update PR template checklist and CODEOWNERS gate.

## 3. Out of scope

- Runtime API redesign.
- Selenium leakage removal itself (Phase 4+).

---

## 4. Code and repo touchpoints

- `pom.xml` (ArchUnit dependencies, test execution wiring)
- `src/test/java/.../architecture/ArchitectureRulesTest.java` (new)
- `src/test/java/.../architecture/TestArchitectureRulesTest.java` (new)
- `archunit_store/` (new baseline)
- `.github/pull_request_template.md` (new/update)
- `CODEOWNERS` (new/update)

---

## 5. Implementation sequence

1. Define production constraints (R2-R7) with explicit freeze allowlists.
2. Define test constraints (R9) with adapter/layer-boundary exemptions only.
3. Generate baseline store and commit it.
4. Add PR checklist + codeowner routing.
5. Fail CI on new violations.

---

## 6. Risks and mitigations

- Over-broad freeze list hides real regressions.
  - Mitigation: freeze by exact classes/packages, not wildcards.
- False positives block delivery.
  - Mitigation: phase-tagged temporary exceptions with ADR requirement.
- Rule drift from docs.
  - Mitigation: require docs and tests update in same PR.

---

## 7. Test strategy

- Rules run under standard `mvn test`.
- Add negative fixture tests to verify rule failures.
- Verify bytecode scanning includes both `src/main` and `src/test` scopes.

---

## 8. Exit criteria

- R2-R7 and R9 are enforced automatically in CI.
- Freeze baseline is committed and blocks new violations.
- PR checklist and CODEOWNERS gate architecture-impacting changes.
- Architecture doc and enforcement code reference the same rule definitions.

