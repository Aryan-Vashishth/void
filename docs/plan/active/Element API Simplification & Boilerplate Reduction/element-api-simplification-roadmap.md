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

| # | Decision | Affects | Status |
|---|----------|---------|--------|
| 1 | Locator repository convention root path — fixed or configurable | Phase 5 | Resolved in Phase 5 — fixed conventional path |
| 2 | Properties template generator CLI design — command name, scope, merge behavior | Phase 6 | **Resolved** — `JsonMigratorCli --sync <ClassName>`; one class per invocation; merge-with-preserve |
| 3 | `LocatorContext` contract — method signatures, composition with `LocatorResolver` | Phase 13 | **Resolved** — `resolveFileName(Element) → String`; injectable via Builder; defaults to `DefaultLocatorContext.INSTANCE` |
| 4 | Repository abstraction boundaries — how far `LocatorRepository` decouples format | Phases 7, 13, 14 | **Resolved** — `LocatorRepository` deferred to Phase 14; Phase 13 uses `String` file name as the seam |
| 5 | Regeneration strategy — recommended: merge-with-preserve | Phase 6 | **Resolved** — merge-with-preserve; orphan warnings only; `--prune` required to delete |

---

## Phase Summary

| Phase | Seq | Description | Risk | Status |
|-------|-----|-------------|------|--------|
| 1  | 1  | Automatic locator keys — `name()` as default `getPrimaryLocator()` | Low | Complete |
| 2  | 2  | Default empty args — `NO_ARGS` default in `Element` | Low | Complete |
| 3  | 3  | Rename `EMPTY_ARGS` → `NO_ARGS` | Medium | Complete |
| 4  | 4  | Automatic display text — derive from enum constant name | Low | Complete |
| 5  | 9  | Deterministic locator repository convention | High | Complete |
| 6  | 10 | Properties template generator (new CLI command) | Low | Complete |
| 7  | 11 | Runtime repository generation (existing CLI repositioned) | Low | Complete |
| 8  | 12 | `getExternalFileName()` as advanced override | Medium | Complete |
| 9  | 13 | Locator resolution order | High | Complete |
| 10 | 14 | Mixed locator strategies | Medium | Complete |
| 11 | 15 | Remove constructors from static elements | Low | Complete |
| 12 | 16 | Simplify capability interfaces | Medium | Complete |
| 13 | 17 | `LocatorContext` abstraction (new) | High | Complete |
| 14 | 18 | Cache `LocatorContext` resolution | Medium | Pending |
| 15 | 19 | Preserve nested enum organization | Low | Pending |
| 16 | 5  | `LocatorFamily` — shared template, auto arg from constant name | Medium | Complete |
| 17 | 6  | `AdvancedLocatorFamily` — family with explicit values for exceptions | Low | Complete |
| 18 | 7  | `SwitchLocatorFamily` — centralised switch with compiler exhaustiveness | Low | Complete |
| 19 | 8  | Capability locator key defaults — `PageClass.EnumClass.CONSTANT.role` default on all locator methods | Medium | Complete |

---

## Recommended Order

**Phases 1–4** are purely additive defaults — safe to implement first, in any order.

**Phases 16–18** (Locator Families) build on Phase 4's display-text transform algorithm and must come before Phase 11.  
Implement in sequence: 16 → 17 → 18.

**Phase 11** (constructor removal) now depends on Phases 1–4 AND Phases 16–17 being complete, because `AdvancedLocatorFamily` is the only permitted home for constructors after cleanup.

**Phases 5, 8, 9** form the locator resolution chain — implement in that sequence.  
Resolve Open Decision 1 before Phase 5.

**Phase 6** depends on Phase 5 (the convention must exist before the generator can target it).  
Resolve Open Decisions 2 and 5 before Phase 6.

**Phase 13** introduces `LocatorContext` — resolve Open Decisions 3 and 4 first.  
**Phase 14** (caching) depends on Phase 13.

**Phase 12** (simplify capability interfaces) is independent and can run at any point.

**Phase 19** (capability locator key defaults) fully-qualifies every locator key as `PageClass.EnumClass.CONSTANT.role`. Both single-role and multi-role interfaces use the same format — the role suffix disambiguates multi-role without special-casing. Must run before Phase 11 (removes remaining locator overrides) and informs Phase 6 (template generator must emit this format).

**Phase 15** is a validation pass, not an implementation phase — run last.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
