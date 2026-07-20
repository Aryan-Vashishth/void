# Plans -- Draft

Active and upcoming implementation plans. Phases here are planned or in progress; none are complete yet. Scrapped plans live in `../archive/`.

## Implementation roadmap

Plan-level execution order (versioning per each plan's "Versioning (CHANGELOG.md)" section):

| Order | Work | Release | Notes |
|---|---|---|---|
| 1 | `oop-violations-remediation` Phases 1-3 | **0.4.0** | Prerequisite; stabilizes Action/Element surfaces. May run parallel with order 2 |
| 2 | `runtime-redesign` I0 (start with 0.1, ADR-021) | none (docs-only) | Gates every later re-typing phase -- milestone M1 |
| 3 | `runtime-redesign` I1 -> I2 -> I3 | **0.5.0** | I1 executes `generalize-element-into-target` phases 1.1-1.3 verbatim -- M2 |
| 4 | `runtime-redesign` I4 | **0.6.0** | Execution boundary; 4.1 absorbs P8 -- M3 |
| 5 | `runtime-redesign` I5 + I7 (parallel), then I6 | **0.7.0** | Session, locator generalization, domain registration + probe -- M4 |
| 6 | `runtime-redesign` I8 -> I9 | **1.0.0** | Semantics, legacy removal, vocabulary reclaim -- M5 |
| flex | `locator-sync-trigger` | **0.8.0** (default slot) | Independent; any free minor, just not alongside runtime-redesign 7.3 |

**Start here**: `oop-violations-remediation` Phase 1 and `runtime-redesign` 0.1 (ADR-021), in either order or in parallel.

## Initiatives

- `runtime-redesign/` -- **master roadmap** for the next major release: 10 initiatives, 37 phases migrating to the domain-neutral runtime (Runtime / Interaction / Capability / Target / Domain). Absorbs `runtime-kernel-boundary` (scrapped, now in `../archive/`) and `generalize-element-into-target` (merged as Initiative I1); owns P8 from `oop-violations-remediation`
- `oop-violations-remediation/` -- 4-phase plan to eliminate `instanceof` dispatch chains and `(Enum<?>) this` casts; Phases 1-3 remain an independent prerequisite of `runtime-redesign`; P8 absorbed by runtime-redesign 4.1, P11 by 9.3; P9 remains here
- `generalize-element-into-target/` -- MERGED into `runtime-redesign/` Initiative I1 (phases 1.1-1.3 lifted verbatim); retained for its phase docs and audit until I1 activates
- `locator-sync-trigger/` -- 4-phase plan for locator sync build integration and developer CLI; absorbs void-cli-simplification; independent of runtime-redesign (avoid landing alongside its phase 7.3)
