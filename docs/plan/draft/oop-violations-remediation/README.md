# OOP Violations Remediation

**Status:** Draft -- planned, not yet implemented
**Branch target:** `initiative/oop-violations-remediation`

4-phase plan to eliminate all `instanceof`-dispatch chains, unguarded `(Enum<?>) this` casts, and `switch`-on-string patterns identified as OOP violations in the codebase.

## Phases

| Phase | File | Violations | Area |
|---|---|---|---|
| 1 | `phase-1-action-layer.md` | P1, P3, P4 | Remove `instanceof HookChainAction` from `Action`; add `mergeHooks()` and `withProfile()` extension hooks; delete `HookedAction` and `ActionLabeled` |
| 2 | `phase-2-element-interface.md` | P5, P6, P7, P10 | Replace `(Enum<?>) this` casts with `ElementSupport`; move `capability()` to `Element`; delete `ActionCapabilityProvider` |
| 3 | `phase-3-dsl-dispatch.md` | P2 | Replace `instanceof` chains in `VoidDSL` with `EnumMap<ActionCapability, BiConsumer<Element, String>>` dispatch |
| 4 | `phase-4-infrastructure.md` | P8, P9, P11 | Replace `UIEngineFactory` switch with registry map; introduce `LocatorRoles`; reduce `Via.java` capability helpers |

## Supporting documents

- `index.md` -- full violation index (P1-P11) with priority and phase assignment
- `audit/oop-violation-post-plan-architecture-validation.md` -- pre-implementation audit; verified all violations exist and flagged implementation risks

## Decisions

ADRs produced during planning (not yet implemented):
- ADR-016 -- `capability()` ownership migration
- ADR-017 -- `ElementSupport` and `LocatorRoles` utility scope
