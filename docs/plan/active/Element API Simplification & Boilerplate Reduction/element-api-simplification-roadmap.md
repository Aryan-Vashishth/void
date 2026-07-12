# Element API Simplification & Boilerplate Reduction — Roadmap

**Status:** Active  
**Branch:** `feature/element-api-simplification`  
**Version target:** 0.3.0  
**Design document:** `docs/plan/active/Element API Simplification & Boilerplate Reduction/element-api-simplification-and-boilerplate-reduction.md`

---

## Principle

> **Developer-authored code should remain the single source of truth.**
>
> Whenever VOID or its tooling can deterministically derive runtime artifacts from that source, those artifacts should be generated rather than manually maintained.

Developers author intent — page structure, capability groupings, locator values.  
VOID generates everything else.

---

## Open Decisions

Resolve before the relevant phases begin.

| # | Decision | Affects |
|---|----------|---------|
| 1 | Locator repository convention root path — fixed or configurable | Phase 5 |
| 2 | Properties template generator CLI design — command name, scope, merge behavior | Phase 6 |
| 3 | `LocatorContext` contract — method signatures, composition with `LocatorResolver` | Phase 13 |
| 4 | Repository abstraction boundaries — how far `LocatorRepository` decouples format | Phases 7, 13, 14 |
| 5 | Regeneration strategy — recommended: merge-with-preserve | Phase 6 |

---

## Phase Summary

| Phase | Description | Risk | Status |
|-------|-------------|------|--------|
| 1 | Automatic locator keys — `name()` as default `getPrimaryLocator()` | Low | Pending |
| 2 | Default empty args — `NO_ARGS` default in `Element` | Low | Pending |
| 3 | Rename `EMPTY_ARGS` → `NO_ARGS` | Medium | Pending |
| 4 | Automatic display text — derive from enum constant name | Low | Pending |
| 5 | Deterministic locator repository convention | High | Pending |
| 6 | Properties template generator (new CLI command) | Low | Pending |
| 7 | Runtime repository generation (existing CLI repositioned) | Low | Pending |
| 8 | `getExternalFileName()` as advanced override | Medium | Pending |
| 9 | Locator resolution order | High | Pending |
| 10 | Mixed locator strategies | Medium | Pending |
| 11 | Remove constructors from static elements | Low | Pending |
| 12 | Simplify capability interfaces | Medium | Pending |
| 13 | `LocatorContext` abstraction (new) | High | Pending |
| 14 | Cache `LocatorContext` resolution | Medium | Pending |
| 15 | Preserve nested enum organization | Low | Pending |

---

## Recommended Order

**Phases 1–4** are purely additive defaults — safe to implement first, in any order.  
They unlock **Phase 11** (constructor removal) once all four defaults are in place.

**Phases 5, 8, 9** form the locator resolution chain — implement in that sequence.  
Resolve Open Decision 1 before Phase 5.

**Phase 6** depends on Phase 5 (the convention must exist before the generator can target it).  
Resolve Open Decisions 2 and 5 before Phase 6.

**Phase 13** introduces `LocatorContext` — resolve Open Decisions 3 and 4 first.  
**Phase 14** (caching) depends on Phase 13.

**Phase 12** (simplify capability interfaces) is independent and can run at any point.

**Phase 15** is a validation pass, not an implementation phase — run last.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
