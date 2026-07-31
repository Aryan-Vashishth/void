# Plans -- Done

Completed implementation plans. All phases in these directories are merged, verified, and reflected in the current codebase.

## Initiatives -- in implementation sequence

| Order | Directory | Release | Summary |
|---|---|---|---|
| 1 | `01 - Architectural Refactoring — Action/` | v0.2.0 | Action layer refactoring: Action Profiles (phases 0-5) then Action Ownership with Layering Principle (phases 13-20). ADRs 012-015. |
| 2 | `02 - Element API Simplification & Boilerplate Reduction/` | v0.3.0 | 19-phase element API simplification: automatic locator keys, default args/display text, locator families, LocatorContext. ADRs 016-017. |
| 3 | `03 - engine-decoupling/` | v0.3.1 | 4-phase Selenium hotswap enablement: EngineBootstrap factory contract, VOIDBuilder startup, Interactions cleanup, bootstrap cleanup. ADR-018, ADR-019. |
| 4 | `04 - core-utils-engine-agnostic/` | v0.3.1 | 4-phase utility deprecation: extend UIEngine with switchToFrame/switchToDefaultContent/sendKeys; deprecate DOMUtils, WaitUtils (By-based), TableHandler. ADR-020. |
| 5 | `05 - oop-violations-remediation/` | v0.4.0 | 4-phase SOLID remediation: Action extension hooks (P1/P3/P4), Element interface safety (P5/P6/P7/P10), DSL dispatch (P2), infrastructure helpers (P9). P8 and P11 deferred to runtime-redesign. |
| 6 | `06 - runtime-redesign/` (I0-I7) | v0.5.0 -- v0.8.0 | Multi-initiative program. M1 complete (I0). M2 complete: Target Model (I1), Kernel Extraction (I2), Capability Model (I3). M3 complete: Execution Boundary (I4). M4 complete: Session Model (I5), Locator Generalization (I7), Domain Registration (I6). I8-I9 in `docs/plan/draft/runtime-redesign/`. ADRs 021-024. |
