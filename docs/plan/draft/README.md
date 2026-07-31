# Plans -- Draft

Active and upcoming implementation plans. Phases here are planned or in progress. Completed plans live in `../done/`; scrapped plans live in `../archive/`.

## Implementation roadmap

Plan-level execution order (versioning per each plan's "Versioning (CHANGELOG.md)" section):

| Order | Work | Release | Notes |
|---|---|---|---|
| 1 | `oop-violations-remediation` Phases 1-4 | **0.4.0** | **Complete.** Moved to `../done/`. |
| 2 | `runtime-redesign` I0-I3 | **0.5.0** | **Complete.** I0 (ADR-021), I1 (Target), I2 (Kernel), I3 (Capability). Plan docs in `../done/`. |
| 3 | `runtime-redesign` I4 | **0.6.0** | **Complete.** Execution boundary; 4.1 absorbs P8. Plan docs in `../done/`. |
| 4 | `runtime-redesign` I5 + I7 (parallel) | **0.7.0 / 0.8.0** | **Complete.** Session (v0.7.0), Locator Generalization (v0.8.0). Plan docs in `../done/`. |
| 5 | `runtime-redesign` I6 | tbd | **Complete on branch.** Domain registration, probe, physical relocation. Plan docs in `../done/`. Pending merge. |
| 6 | `runtime-redesign` I8 -> I9 | **1.0.0** | Not started. Interaction semantics, legacy removal, vocabulary reclaim -- M5. |
| flex | `locator-sync-trigger` | flex | Independent; avoid alongside runtime-redesign 7.3. |

**Start here**: `runtime-redesign` I8 (I0-I7 are complete; M4 is done).

## Initiatives

- `06 - runtime-redesign/` -- **master roadmap** (active). I0-I7 complete (see `../done/`). I8-I9 remain: Interaction Semantics and Legacy Removal / Public API. Index and I8/I9 plan docs live here.
- `07 - generalize-element-into-target/` -- MERGED into `runtime-redesign/` Initiative I1; retained for phase docs and audit
- `08 - locator-sync-trigger/` -- 4-phase plan for locator sync build integration and developer CLI; independent of runtime-redesign (avoid landing alongside 7.3)
- `09 - release-automation/` -- 2-phase plan: `scripts/set-version` and `scripts/release`. Independent; can land at any time.
