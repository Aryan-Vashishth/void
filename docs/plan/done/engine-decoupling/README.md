# Engine Decoupling

**Status:** Complete
**Branch:** `feature/engine-decoupling` -- merged 2026-07-20
**Identified:** 2026-07-15

Plan to remove the direct Selenium bootstrap coupling from VOID's startup path, enabling engine hot-swap and cleaner engine registration.

## Phases

| Phase | File | Summary |
|---|---|---|
| 1 | `phase-1-factory-contract.md` | Establish `UIEngineFactory` contract and registration API |
| 2 | `phase-2-void-startup.md` | Introduce `VOID.builder()`; invert startup order; wire `SessionContext` |
| 3 | `phase-3-interactions-cleanup.md` | Remove legacy `Interactions` engine dependency |
| 4 | `phase-4-bootstrap-cleanup.md` | Clean up `FrameworkBootstrap` Selenium-specific wiring |

See `index.md` for the full plan.
