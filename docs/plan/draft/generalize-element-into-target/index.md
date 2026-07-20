# Generalize Element into Target

> **MERGED (2026-07-20)** into [`../runtime-redesign/`](../runtime-redesign/index.md)
> as Initiative I1 (phases 1.1-1.3 of this draft are lifted verbatim; a new phase 1.4
> is added there). The phase docs below remain the authoritative implementation text
> and are consumed when I1 activates. Track status in runtime-redesign.

Identified: 2026-07-20 post-engine-decoupling domain model audit.
Branch target: cut from `initiative/engine-decoupling` once merged.

---

## Problem statement

`Element` is the root abstraction for every page object in VOID. The name and its
contents are UI-specific: locator keys, locator roles, external locator file lookup,
and default method implementations that hard-cast to `Enum<?>`. Any future non-UI engine
(Playwright, API, mobile) would face an interface that encodes Selenium-era assumptions
at the domain root level.

The fix is two steps: introduce a domain-neutral `Target` root in `core.target`, and
rename `Element` to `UIElement` so the UI scope is explicit in the name. Capability
interfaces, page object enums, and all locator infrastructure are unchanged.

See [audit/generalize-element-into-target-pre-plan-architecture-audit.md](audit/generalize-element-into-target-pre-plan-architecture-audit.md) for the full architectural analysis (Q1-Q11).

---

## Concern map

| ID | Concern | Layer | File |
|----|---------|-------|------|
| C1 | `Element` name does not reflect its UI-only scope | Domain root | `elements/api/Element.java` |
| C2 | `getArgs`, `effectiveArgs`, `NO_ARGS` exist on `Element` but have zero UI semantics; they belong on a domain-neutral root | Domain root | `elements/api/Element.java` |
| C3 | `UIEngine.resolve()` takes `Element` -- the engine is bound to a UI-named type at its public API boundary | Engine contract | `core/engine/UIEngine.java` |

---

## Phase overview

| Phase | Goal | Risk | Key changes |
|-------|------|------|-------------|
| 1 | Introduce `Target` | Low | New `core/target/Target.java`; no existing files change |
| 2 | Rename `Element` to `UIElement` | Medium | Rename interface; add `extends Target`; update all imports, `implements` declarations, and `UIEngine.resolve()` parameter |
| 3 | Validation and cleanup | Low | Regression pass, documentation audit, verify no remaining `Element` references |

Phase docs:
- [Phase 1 -- Introduce Target](phase-1-introduce-target.md)
- [Phase 2 -- Rename Element to UIElement](phase-2-rename-element.md)
- [Phase 3 -- Validation and cleanup](phase-3-validation-cleanup.md)

---

## Dependency rationale

Phase 1 before Phase 2: `UIElement extends Target` requires `Target` to exist and compile
first. Phase 1 creates the anchor; Phase 2 hooks into it.

Phase 2 before Phase 3: validation only makes sense once the rename is complete. Phase 3
is the final gate -- confirms no `Element` reference survived Phase 2 and that all tests
pass.

**Rule**: nothing in Phase N depends on Phase N+1. Each phase compiles and passes
`mvn compile -q` on its own before the next phase begins. Never mix phases in one commit.

---

## What does NOT change

- Capability interfaces (`Clickable`, `Typeable`, etc.) -- standalone mixins; no body or
  `extends` changes
- `ElementRole`, `LocatorDescriptor`, `LocatorStrategy` -- untouched
- `LocatorFamily`, `AdvancedLocatorFamily`, `SwitchLocatorFamily` -- locator resolution
  contract unchanged; only import/`extends` reference updates if they reference `Element`
- `UIEngine` contract other than the `Element` -> `UIElement` parameter rename
- All page object enum constant bodies -- only the `implements` declaration line changes
- `DriverContext`, `DriverFactory`, `SeleniumEngine` internals -- untouched
- `VOID`, `VOIDBuilder`, `SessionContext` -- no changes
- All test behavior

---

## Commit sequence

```
# Phase 1
feat(core): introduce Target domain root in core.target

# Phase 2
refactor(elements): rename Element to UIElement; extend core.target.Target

# Phase 3
docs(elements): update documentation to reflect UIElement and Target hierarchy
```

All commits follow Conventional Commits format. No em dashes. Imperative present tense.

---

## Future watch (do not act on these now)

Once a second concrete engine type (Playwright, API) is under active development, revisit:

- Whether a three-tier hierarchy (`Target > UITarget > UIElement`) is warranted for
  non-role-based UI targets
- Whether a shared `Engine` superinterface above `UIEngine` should extract `initialize()`
  and `shutdown()`
- Whether `ElementRole` generalizes to a shared role concept or whether each domain
  defines its own role enum
- Whether the `(Enum<?>) this` cast in `UIElement` defaults should be replaced with an
  explicit enum constraint (Java does not have `interface UIElement<E extends Enum<E>>`)

These decisions require two concrete implementations to validate against.
Do not design them speculatively.
