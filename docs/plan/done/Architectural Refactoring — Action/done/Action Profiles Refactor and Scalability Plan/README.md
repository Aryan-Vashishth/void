# Action Profiles Refactor and Scalability Plan

**Status:** Complete
**Branch:** `feature/element-api-simplification` (merged into main)

Developer experience refactor for the action hook layer. Addresses the gaps identified in `docs/audits/fulfilled/developer-experience-audit-2026-06.md`.

## Phases

| Phase | Title | ADR |
|---|---|---|
| 0 | Stabilization Baseline | -- |
| 1 | Profile API Consolidation | -- |
| 2 | Observability / ActionTrace | -- |
| 3 | Capability Self-Description | ADR-015 |
| 4 | Capability-Driven Hook Selection | ADR-013 |
| 5 | Execution Policy in Action Layer | ADR-013 |

## Outcome

`safely()`, `reliable()`, `debug()`, `raw()`, `using(ActionProfile)` on all actions. `ActionProfile.builder()` for custom profiles. `void.profile.default` config key for session-wide defaults. `Profiles.SAFE` / `Profiles.RELIABLE` removed; replaced by per-family `defaultSafeProfile()` / `defaultReliableProfile()` on `ElementAction` subclasses.
