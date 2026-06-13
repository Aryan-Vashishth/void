# Phase 8 — Deprecated Code Removal

**Status:** Ongoing (blocked on Phases 1-4)  
**Architecture Version:** 2.3  
**Branch:** new branch per removal batch  
**Risk:** High — removal is permanent, requires full migration verification

---

## Objective

Remove deprecated APIs cleanly and safely after all call sites have migrated to replacements. Leave the codebase with no dead `@Deprecated` markers in core paths. Document every removal with a changelog entry and a migration mapping.

---

## Preconditions

This phase may not begin until ALL of the following are true:

- [ ] Phase 1 (Profile API Consolidation) is complete and merged.
- [ ] Phase 3 (Capability Resolution Hardening) is complete and merged.
- [ ] Phase 4 (Hook Strategy Layer) is complete and merged.
- [ ] All migration guides for deprecated symbols are published.
- [ ] CI shows zero usages of deprecated symbols in production code paths.

---

## Deprecated Symbol Inventory

### Current Known Deprecated Items

| Symbol | Location | Since | Replacement | Risk |
|---|---|---|---|---|
| `HookedAction` class | `core.actions.HookedAction` | 2.0 | `action.withHooks(...)` | Medium |
| `HookedAction(...)` constructor | `core.actions.HookedAction` | 2.0 | `action.withHooks(...)` | Medium |
| `HookedAction.wrap(...)` static method | `core.actions.HookedAction` | 2.0 | fluent hook API | Medium |
| `Action.withHooks(List, List)` | `core.actions.Action` | 2.0 (on feature branch) | `safely()`, `debug()`, `using(profile)` | High — public API |
| `setLastLocatorDescriptor(...)` | `core.utils.UIContext` | unknown | unknown — needs audit | Low-Medium |
| `getLastElement()` | `core.utils.UIContext` | unknown | unknown — needs audit | Low-Medium |
| `isAnyDisplayed(By)` | `core.interactions.Interactions` | unknown | unknown — needs audit | Low |

> Note: `Action.withHooks(List, List)` is shown as deprecated on the feature branch's warnings output. Verify its actual deprecation status before removing — it may need to stay as an advanced escape hatch.

---

## Removal Strategy

### Rule 1: Never Remove an API That Has No Published Replacement

Before removing anything, verify the replacement exists, is tested, and is documented.

### Rule 2: Replace Internal Usages Before Removing the API

Order:
1. Migrate all internal framework usages to the replacement.
2. Migrate demo code.
3. Remove the deprecated symbol.
4. Update changelog.

### Rule 3: Remove in Small Batches, Not One Big PR

Each PR targets one symbol or one related group. Small PRs are reviewable and reversible.

### Rule 4: Use a Separate Branch Per Removal Batch

```
feature/remove-hookedaction
feature/remove-deprecated-ui-context
feature/remove-deprecated-interactions
```

---

## Removal Plan (Ordered by Risk)

### Batch 1 — `HookedAction` (Low-Medium Risk)

**Precondition:** Phase 1 complete. All call sites using `HookedAction.wrap(...)` or constructing it directly have been migrated.

- [ ] Grep entire codebase for `new HookedAction(` and `HookedAction.wrap(`.
- [ ] Migrate each usage to `action.withHooks(...)` or the appropriate profile shorthand.
- [ ] Verify no compilation warnings reference `HookedAction`.
- [ ] Delete `src/main/java/core/actions/HookedAction.java`.
- [ ] Update `core/actions/README.md` — remove all references.
- [ ] Add to `CHANGELOG.md`:

```markdown
### Removed
- `HookedAction` — use `action.withHooks(List, List)` or profile shorthands (`safely()`, `debug()`, `raw()`).
```

### Batch 2 — `UIContext` Deprecated Methods (Low-Medium Risk)

**Precondition:** Understand what `setLastLocatorDescriptor` and `getLastElement` are used for and what replaces them.

- [ ] Audit all usages of `UIContext.setLastLocatorDescriptor(...)` across codebase.
- [ ] Audit all usages of `UIContext.getLastElement()` across codebase.
- [ ] Identify replacement or decide to make them package-private.
- [ ] Migrate all usages.
- [ ] Remove or restrict methods.
- [ ] Add changelog entry.

### Batch 3 — `Interactions.isAnyDisplayed(By)` (Low Risk)

**Precondition:** Confirm no test code uses this directly.

- [ ] Grep for `isAnyDisplayed`.
- [ ] Confirm only one usage exists (in `VoidDSL.java`).
- [ ] Migrate `VoidDSL` usage to replacement.
- [ ] Remove `isAnyDisplayed(By)` from `Interactions`.
- [ ] Add changelog entry.

### Batch 4 — `Action.withHooks(List, List)` (High Risk — Decide First)

> This requires a decision before any removal. `withHooks` may need to remain as the power-user / advanced API even if profiles are the primary path.

- [ ] Evaluate: is `withHooks(List, List)` an escape hatch or a deprecated API?
- [ ] If escape hatch: remove `@Deprecated`, keep the method, update Javadoc.
- [ ] If truly deprecated: confirm `using(customProfile)` covers all its use cases.
- [ ] If removing: grep all usages including demo, tests, and docs.
- [ ] Migrate all usages to `using(customProfile)`.
- [ ] Remove method.
- [ ] Add changelog entry.

---

## Checklist (Cross-Cutting)

### Before Any Removal
- [ ] All phases 1-4 are marked complete.
- [ ] Zero deprecated usages in CI compiler output for production code.
- [ ] Migration guides for each symbol are published.

### Per-Removal Steps (Apply to Each Batch)
- [ ] Grep codebase for all usages of the symbol.
- [ ] Confirm replacement is stable and tested.
- [ ] Migrate internal framework usages.
- [ ] Migrate demo code.
- [ ] Remove deprecated symbol.
- [ ] Verify `mvn -DskipTests compile` passes with zero deprecation warnings for that symbol.
- [ ] Update Javadoc on any types that referenced the removed symbol.
- [ ] Add `CHANGELOG.md` entry with before/after migration mapping.

### After All Removals
- [ ] Run `mvn test` in CI — all tests pass.
- [ ] Verify compiler output contains no `@Deprecated` warnings in core production packages.
- [ ] Update architecture docs to remove references to removed symbols.

---

## Exit Criteria

- Zero `@Deprecated(forRemoval = true)` symbols remain in core packages.
- All removed APIs have changelog entries with migration mappings.
- CI passes cleanly with no deprecation warnings in production code.

---

## What NOT to Do

- Do not remove `Before.*` or `After.*` hook constants — they are stable public API.
- Do not remove `@Deprecated` annotations without removing the actual code.
- Do not rush Batch 4 (`withHooks`) — make the escape-hatch decision carefully.
- Do not merge any removal batch without a passing CI run.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

