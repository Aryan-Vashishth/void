> **Status: Complete.** All phases merged to `initiative/runtime-redesign` (2026-07-23). P8 reallocated to runtime-redesign I4.1; P11 to runtime-redesign I9.3.

# OOP Violations Remediation

> **Coordination note (2026-07-20)**: Phases 1-3 are an independent prerequisite of
> [`../runtime-redesign/`](../runtime-redesign/index.md) and proceed unchanged.
> From Phase 4: **P8 is absorbed** by runtime-redesign phase 4.1 (engine registry)
> and **P11 by phase 9.3** (Via deletion) -- do not implement them here. P9 remains
> owned by this initiative.

Identified: 2026-07-15 interface audit.
Supersedes: `action-layer-ocp-violations.md` (covered P1/P3/P4/P6/P7 as an earlier partial draft -- fully absorbed here).
Branch target: `initiative/oop-violations-remediation`

---

## Violation map

| ID  | Priority | Principle | Phase | Violation summary |
|-----|----------|-----------|-------|-------------------|
| P1  | CRITICAL | DIP, OCP  | 1     | `instanceof HookChainAction` in 4 `Action` default methods |
| P2  | CRITICAL | OCP       | 3     | Sequential `instanceof` chains in `VoidDSL` dispatch |
| P3  | HIGH     | OCP       | 1     | `switch (ActionCapability)` in `HookChainAction.operationLabel` |
| P4  | HIGH     | LSP, DIP  | 1     | `instanceof ActionLabeled` in `HookChainAction` + `HookedAction` |
| P5  | HIGH     | LSP       | 2     | `(Enum<?>) this` hard cast in `Element` interface defaults |
| P6  | MEDIUM   | DRY, LSP  | 2     | Duplicated `instanceof Enum<?>` in `ElementAction` + `LocatorResolver` |
| P7  | MEDIUM   | ISP, OCP  | 2     | `instanceof ActionCapabilityProvider` in `ElementActions.capabilityFor` |
| P8  | MEDIUM   | OCP       | 4     | `switch` on engine name string in `UIEngineFactory` |
| P9  | LOW      | OCP       | 4     | O(n^2) dedup in `SearchableDropdown`/`SearchField.getAllLocatorRoles` |
| P10 | LOW      | ISP       | 2     | Forced abstract `getIndex()` in `Listable` with no default |
| P11 | LOW      | OCP       | 4     | Per-capability static helpers in `Via` growing with capability count |

---

## Phase overview

| Phase | Goal                                        | Violations      | Key deletions                                      |
|-------|---------------------------------------------|-----------------|----------------------------------------------------|
| 1     | Action layer: extension hooks + label promo | P1, P3, P4      | `ActionLabeled.java`, `HookedAction.java`          |
| 2     | Element interface safety + capability       | P5, P6, P7, P10 | `ActionCapabilityProvider.java`                    |
| 3     | DSL: capability-driven dispatch             | P2              | --                                                  |
| 4     | Infrastructure: registry + helpers          | P8, P9, P11     | `Via.java` (or reduced to 1 generic method)        |

Phase docs:
- [Phase 1 -- Action layer](phase-1-action-layer.md)
- [Phase 2 -- Element interface](phase-2-element-interface.md)
- [Phase 3 -- DSL dispatch](phase-3-dsl-dispatch.md)
- [Phase 4 -- Infrastructure](phase-4-infrastructure.md)

---

## Dependency rationale

P3 and P4 share one root fix (label methods on `Action`) -- do them in the same commit as P1.
P6 depends on Phase 1: `ElementAction.elementLabel()` delegates to `Action.elementLabel()`,
which must exist before the call site can be simplified.
P7 requires `capability()` on `Element` -- grouped with the other `Element` changes (P5) in
Phase 2 so all `Element` defaults are stabilised in one pass.
P2 is isolated to the DSL layer -- interfaces must be stable first (Phase 2), then the DSL
is safe to refactor without risk of re-touching the same interfaces.
P8, P9, P11 have no cross-cutting dependencies -- they are self-contained and go in Phase 4.

**Rule:** nothing in Phase N depends on Phase N+1. Each phase compiles and passes
`mvn compile -q` on its own before the next phase begins. Never mix phases in one commit.

---

## Artifacts deleted

Deletion is mandatory in the phase it is listed. Leaving dead interfaces creates false
expectations about extension points and forces future readers to check whether they are still
in use.

| Artifact                                     | Phase | Reason                                                        |
|----------------------------------------------|-------|---------------------------------------------------------------|
| `core/actions/ActionLabeled.java`            | 1     | `elementLabel`/`operationLabel` promoted to `Action`          |
| `core/actions/HookedAction.java`             | 1     | Already `@Deprecated(forRemoval)`; Phase 1 removes its only non-trivial pattern |
| `core/actions/ActionCapabilityProvider.java` | 2     | `capability()` moves to `Element`; interface body is now empty |

---

## Commit sequence

One commit per step. No commit spans two phases.

```
# Phase 1
feat(actions): add mergeHooks extension hook, remove instanceof HookChainAction from Action defaults
feat(actions): promote elementLabel/operationLabel to Action, remove ActionLabeled
chore(actions): delete HookedAction (deprecated since 0.2, last pattern removed)

# Phase 2
feat(elements): add enum-safe static helpers to Element, replace Enum casts in defaults
feat(elements): move capability() to Element, delete ActionCapabilityProvider
fix(actions): ElementAction elementLabel delegates to element.getDisplayText()
fix(resolvers): LocatorResolver labelOf uses Element helpers, removes Enum cast
feat(elements): default getIndex() on Listable from ordinal

# Phase 3
refactor(dsl): replace instanceof dispatch chains with typed capability calls

# Phase 4
feat(engine): replace UIEngineFactory switch with registry map
refactor(elements): SearchableDropdown and SearchField use roleMap dedup helper
chore(interactions): reduce Via to generic cast helper or delete
```

All commits follow Conventional Commits format. No em dashes. Imperative present tense.

---

## Verification

```
# After Phase 1
mvn compile -q
grep -r "ActionLabeled" src/   # must be empty
grep -r "HookedAction"  src/   # must be empty

# After Phase 2
mvn compile -q
grep -r "ActionCapabilityProvider" src/   # must be empty
mvn compile -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli "-Dexec.args=--sync examples.pages.DemoLoginPage --prune"
# Expected: [sync] Done -- DemoLoginPage is in sync.

# After Phase 3
mvn test -Dtest=DemoLoginTest -q
grep -n "instanceof" src/main/java/dsl/VoidDSL.java   # must be empty or non-dispatch uses only

# After Phase 4
mvn compile -q
grep -n "switch" src/main/java/core/engine/UIEngineFactory.java   # must be empty
```

---

## What does NOT change

- `.properties` / `.json` locator files
- `LocatorFamily`, `AdvancedLocatorFamily`, `SwitchLocatorFamily`
- Page object enums (`DemoLoginPage`, etc.)
- `LocatorTemplateGenerator` / `OrphanKeyDetector` sync pipeline
- Public `Element` API surface -- Phase 2 adds static helpers and new defaults only
- `ActionCapability` enum constants -- only the dispatch-by-switch is removed, not the enum

---

## Versioning (CHANGELOG.md)

Target release: **0.4.0** (minor; pre-1.0 policy allows documented breaking
changes). `## [Unreleased]` entries as phases land:

- `### Removed` -- **`HookedAction`**, **`ActionLabeled`**,
  **`ActionCapabilityProvider`** -- Phase 1-2 deletions, each entry names its
  replacement
- `### Changed` -- **`Element`** -- enum-safe default helpers, `capability()`
  ownership, `Listable.getIndex()` default (Phase 2)
- Phase 3 (`VoidDSL` dispatch) is a behavior-neutral internal refactor -- entry
  only if its public surface shifts
- P8 and P11 entries belong to the runtime-redesign releases that absorbed them
  (0.6.0 and 1.0.0); P9 is internal, no entry
