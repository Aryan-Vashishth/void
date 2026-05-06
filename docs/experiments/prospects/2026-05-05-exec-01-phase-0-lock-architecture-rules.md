# Phase 0 — Lock Architecture Rules (Governance Note)

> **Status:** Active governance phase — always-on for the full refactor.
>
> **Phase number:** 0 (audit numbering, stable).  
> **Execution order:** 1 of 10 — first and always-on.  
> **Precedes:** Phase 2 (exec order 2).  
> **Succeeds:** — (start of sequence).
>
> **Proposed:** 2026-05-05  
> **Last updated:** 2026-05-05

---

## Full Execution Sequence (for reference)

| Exec Order | Phase # | Title |
|:---:|:---:|---|
| **1** | **0** | **Lock Architecture Rules ← you are here** |
| 2 | 2 | Resolution Unification |
| 3 | 1 | Fix Bootstrap & Startup Ownership |
| 4 | 1.5 | Lock Execution Model in Code |
| 5 | 3 | Wire `FlowExecutor` into Runtime |
| 6 | 4 | Remove Selenium Leakage from Public Surface |
| 7 | 5 | Convert `Interactions` to Strict Adapter |
| 8 | 6 | Remove `UIContext` |
| 9 | 7 | Finalize Hook Model |
| 10 | 8 | Add Playwright Engine |

---

## Why this is a note, not an implementation plan

Phase 0 has no delivery exit criteria and is not a one-time migration batch.
Its job is to continuously prevent architectural regressions while phases 1-8
execute.

## Current verification gaps to close under Phase 0/1.5

- Architecture enforcement classes referenced by docs are not present in the
  current `src/test/java` snapshot.
- `archunit_store/` baseline artifacts are not present in the workspace snapshot.
- Governance files referenced by docs should be verified/added:
  `.github/pull_request_template.md`, `CODEOWNERS`.

## Ongoing Phase 0 checklist

1. Keep R1-R9 docs authoritative and up to date.
2. Keep architecture tests green and freeze lists tight.
3. Block shortcuts that bypass Action/Flow/FlowExecutor.
4. Require ADR for any rule relaxation.

