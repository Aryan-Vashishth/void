# Plans -- Done

Completed implementation plans. All phases in these directories are merged, verified, and reflected in the current codebase.

## Initiatives -- in implementation sequence (01-06; runtime-redesign continues in draft/ as 06)

| Order | Directory | Release | Summary |
|---|---|---|---|
| 1 | `01 - Architectural Refactoring — Action/` | v0.2.0 | Action layer refactoring: Action Profiles (phases 0-5) then Action Ownership with Layering Principle (phases 13-20). ADRs 012-015. |
| 2 | `02 - Element API Simplification & Boilerplate Reduction/` | v0.3.0 | 19-phase element API simplification: automatic locator keys, default args/display text, locator families, LocatorContext. ADRs 016-017. |
| 3 | `03 - engine-decoupling/` | v0.3.1 | 4-phase Selenium hotswap enablement: EngineBootstrap factory contract, VOIDBuilder startup, Interactions cleanup, bootstrap cleanup. ADR-018, ADR-019. |
| 4 | `04 - core-utils-engine-agnostic/` | v0.3.1 | 4-phase utility deprecation: extend UIEngine with switchToFrame/switchToDefaultContent/sendKeys; deprecate DOMUtils, WaitUtils (By-based), TableHandler. ADR-020. |
| 5 | `05 - oop-violations-remediation/` | v0.4.0 | 4-phase SOLID remediation: Action extension hooks (P1/P3/P4), Element interface safety (P5/P6/P7/P10), DSL dispatch (P2), infrastructure helpers (P9). P8 and P11 deferred to runtime-redesign. |
| 6 | `06 - runtime-redesign/` | v0.5.0+ | Multi-initiative program (I0-I9). M2 complete: Target Model (I1), Kernel Extraction (I2), Capability Model (I3). I4-I9 in `docs/plan/draft/runtime-redesign/`. ADR-021. |
