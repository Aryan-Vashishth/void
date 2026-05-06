# Phase 2 — Resolution Unification

> **Status:** Prospect.
>
> **Phase number:** 2 (audit numbering, stable).  
> **Execution order:** 2 of 10 — runs directly after Phase 0, before Phase 1.  
> **Precedes:** Phase 1 (exec order 3).  
> **Succeeds:** Phase 0 (exec order 1).
>
> **Goal:** Make `Action` the only owner of element-intent resolution. `UIEngine`
> executes descriptors only.
>
> **Proposed:** 2026-05-05  
> **Last updated:** 2026-05-05

---

## Full Execution Sequence (for reference)

> Phase numbers are stable audit identifiers. Execution order reflects
> dependency-driven sequence — Phase 2 runs before Phase 1 by design
> (see active architecture doc §8).

| Exec | Phase # | Title | Depends On |
|:---:|:---:|---|---|
| 1 | 0 | Lock Architecture Rules | — |
| **2** | **2** | **Resolution Unification ← you are here** | Phase 0 |
| 3 | 1 | Fix Bootstrap & Startup Ownership | Phase 2 |
| 4 | 1.5 | Lock Execution Model in Code | Phase 1 |
| 5 | 3 | Wire `FlowExecutor` into Runtime | Phase 1, 2 |
| 6 | 4 | Remove Selenium Leakage from Public Surface | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. Why this phase is first after Phase 0

Current code still resolves in multiple places:

- `UIEngine.resolve(Element, ElementRole, ...)`
- `SeleniumEngine.resolve(...)` via `LocatorResolvers`
- capability interfaces that call `engine.resolve(...)`
- `Interactions` direct `LocatorResolvers` calls (legacy, quarantined)

If this remains, later bootstrap/runtime work locks in dual ownership.

---

## 2. In scope

- Remove/deprecate `Element`-based resolve methods from `UIEngine`.
- Move modern-pipeline resolution into Action-layer helpers (`core.actions`).
- Keep legacy `Interactions` resolution quarantined (no growth).
- Update docs that still claim engine-owned resolution.

## 3. Out of scope

- Interactions full adapter rewrite (Phase 5).
- DSL Selenium leak cleanup (Phase 4).
- UIContext removal (Phase 6).

---

## 4. Code touchpoints

- `src/main/java/core/engine/UIEngine.java`
- `src/main/java/core/engine/selenium/SeleniumEngine.java`
- `src/main/java/core/actions/ElementActions.java`
- `src/main/java/core/actions/HookedAction.java`
- `src/main/java/elements/api/capability/Selectable.java`
- `src/main/java/elements/api/capability/SearchField.java`
- `src/main/java/elements/api/capability/SearchableDropdown.java`
- `src/main/java/elements/api/capability/MultiSelectable.java`
- `src/main/java/elements/api/capability/Uploadable.java`
- `src/main/java/elements/api/capability/EditableTable.java`

---

## 5. Implementation sequence

1. Add Action-layer resolution utility (single entrypoint for modern path).
2. Migrate capability default methods to resolve through Action layer.
3. Deprecate/remove `UIEngine.resolve(Element, ...)`.
4. Keep only descriptor-based engine operations.
5. Add architecture checks for resolution ownership.

---

## 6. Risks and mitigations

- Capability churn across many interfaces.
  - Mitigation: migrate per capability type in small PRs.
- Hidden legacy dependencies on `engine.resolve(...)`.
  - Mitigation: grep + architecture tests before removal.
- Hook bridge regressions (`HookedAction.wrap(...)`).
  - Mitigation: migrate bridge callers early and keep compatibility shim deprecated.

---

## 7. Test strategy

- Unit tests for each migrated capability action path.
- Contract tests: action execution uses descriptor-only engine methods.
- Architecture tests: no new `LocatorResolvers` usage outside approved areas.

---

## 8. Exit criteria

- `UIEngine` no longer accepts `Element` in public method signatures.
- Modern pipeline resolution occurs in Action layer only.
- No new `LocatorResolvers` usage outside `core.actions` and frozen legacy set.
- `Interactions` resolver calls are unchanged or reduced (never increased).
- Docs updated to reflect Action-owned resolution.

