# Architectural Refactoring — Action Ownership with Layering Principle

**Status:** Complete
**Branch:** `feature/element-api-simplification` (merged into main)

Established the Architectural Layering Principle and migrated all capability methods from anonymous lambdas to typed concrete action subclasses.

## Phases

| Phase | Title | ADR |
|---|---|---|
| 13 | ElementAction Base Class | ADR-014 |
| 14 | Concrete Action Subclasses | ADR-014 |
| 15 | Capability Refactoring | ADR-014 |
| 16 | Delete Execution Policy from Capabilities | ADR-013 |
| 17 | Refactor Central Dispatch | ADR-013 |
| 18 | Audit ElementRole | -- |
| 19 | ElementActions Factory | ADR-012 |
| 20 | Update Documentation | -- |

## Outcome

17 concrete action subclasses (`ClickAction`, `TypeAction`, etc.). Three abstract intermediaries (`ClickableElementAction`, `TypeableElementAction`, `SelectableElementAction`). `ReadTextAction` replaces the last `ElementActions.of()` production call site. `ElementActions` marked `@Internal`. ADRs 012-014 published.
