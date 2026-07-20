# Plans -- Draft

Active and upcoming implementation plans. Phases here are planned or in progress; none are complete yet.

## Initiatives

- `runtime-redesign/` -- **master roadmap** for the next major release: 10 initiatives, 37 phases migrating to the domain-neutral runtime (Runtime / Interaction / Capability / Target / Domain). Absorbs `runtime-kernel-boundary` (superseded) and `generalize-element-into-target` (merged as Initiative I1); owns P8 from `oop-violations-remediation`
- `oop-violations-remediation/` -- 4-phase plan to eliminate `instanceof` dispatch chains and `(Enum<?>) this` casts; Phases 1-3 remain an independent prerequisite of `runtime-redesign`; P8 absorbed by runtime-redesign 4.1, P11 by 9.3; P9 remains here
- `generalize-element-into-target/` -- MERGED into `runtime-redesign/` Initiative I1 (phases 1.1-1.3 lifted verbatim); retained for its phase docs and audit until I1 activates
- `locator-sync-trigger/` -- 4-phase plan for locator sync build integration and developer CLI; absorbs void-cli-simplification; independent of runtime-redesign (avoid landing alongside its phase 7.3)
- `runtime-kernel-boundary/` -- SUPERSEDED by `runtime-redesign/` (its four phases map to 0.1, 2.1, 4.1+4.2, 3.1+3.2); retained for traceability until archival at runtime-redesign 9.5
