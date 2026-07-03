# Phase 8 — Deprecated Code Removal

**Status:** Ongoing (blocked on Phases 4–6)  
**Architecture Version:** 2.3  
**Branch:** New branch per removal batch  
**Risk:** High — removal is permanent; requires full migration verification before each batch

---

## Objective

Remove deprecated public APIs cleanly and safely after all call sites have migrated to current replacements. Leave the codebase with no publicly-visible `@Deprecated(forRemoval = true)` markers in core paths. Document every removal with a changelog entry and a migration mapping.

---

## Preconditions

This phase may not begin until ALL of the following are true:

- [ ] Phase 4 (Capability-Driven Hook Selection) is complete and merged.
- [ ] Phase 5 (Execution Pipeline Boundary) is complete and merged.
- [ ] Phase 6 (Engine Portability Controls) is complete and merged — the deprecated symbol inventory and UIContext audit must exist as documented artifacts.
- [ ] All migration guides for deprecated symbols are published.
- [ ] CI shows zero usages of deprecated symbols in production code paths.

---

## Deprecated Symbol Inventory

The full inventory is produced in Phase 6. This section summarizes the removal plan for each symbol. Phase 8 works from the Phase 6 audit document.

---

## Permanent Decision: `Action.withHooks(List, List)` Is Not Removed

`withHooks(List, List)` is currently marked `@Deprecated(forRemoval = true, since = "2.0")`. This deprecation is reversed in Phase 8.

**Rationale:** `withHooks` is the power-user escape hatch for ad-hoc hook injection in a single expression. While `action.using(ActionProfile.builder().before(...).after(...).build())` covers the same use case, it requires naming and constructing a profile object. `withHooks` is more ergonomic for one-off compositions in infrastructure or framework code. It does not conflict with the profile-oriented API — it complements it.

**Action:** Remove `@Deprecated(forRemoval = true)` from `Action.withHooks`. Update Javadoc:

```java
/**
 * Wraps this action with before/after hooks, returning a new {@link Action}.
 *
 * <p><b>Advanced API.</b> For standard use cases prefer the profile shorthands:
 * {@link #safely()}, {@link #debug()}, {@link #raw()}, or
 * {@link #using(ActionProfile)}. Use this method when ad-hoc hook injection
 * is more ergonomic than constructing a named profile.</p>
 */
default Action withHooks(@Nullable List<ActionHandler> before,
                         @Nullable List<ActionHandler> after) { ... }
```

This closes Batch 4 — no removal work needed.

---

## Removal Strategy

### Rule 1 — Never Remove an API Without a Published Replacement
Before removing anything, verify the replacement exists, is tested, is documented, and is in the Phase 6 inventory.

### Rule 2 — Replace Internal Usages Before Removing the API
Order:
1. Migrate all internal framework usages to the replacement.
2. Migrate demo code.
3. Remove the deprecated symbol.
4. Update changelog.

### Rule 3 — Remove in Small Batches, Not One Large PR
Each PR targets one symbol or one related group. Small PRs are reviewable and reversible.

### Rule 4 — Use a Separate Branch Per Removal Batch
```
feature/remove-hookedaction-public-api
feature/restrict-deprecated-ui-context
feature/remove-deprecated-interactions
feature/deprecate-get-engine
```

---

## Removal Batches

### Batch 1 — `HookedAction` Public API (Medium Risk)

**Precondition:** Phase 4 complete. No code in `tests.*` or external packages constructs `HookedAction` directly or calls `HookedAction.wrap()`.

**What to remove:**
- The public deprecated constructor `HookedAction(Action, LocatorDescriptor, List, List)`
- The public deprecated static factory `HookedAction.wrap(...)`

**What to keep:**
- `HookedAction` as a class — it is used internally by `HookChainAction` and carries the tracing implementation (`LAST_TRACE`, `performAndTrace()`). Remove `public` from the class declaration if it is currently public; make it package-private.
- The package-private constructor `HookedAction(Action, LocatorDescriptor, List, List, String)` — used by `HookChainAction`. It stays.
- Remove `@Deprecated(forRemoval = true)` from the class declaration once the public API is gone (the class is no longer deprecated — it is simply internal).

**Checklist:**
- [ ] Grep entire codebase for `new HookedAction(` with 4 arguments (public constructor) outside `core.actions` package.
- [ ] Grep for `HookedAction.wrap(`.
- [ ] Confirm zero usages in `tests.*`, `demo.*`, and production code.
- [ ] Remove the 4-argument public constructor.
- [ ] Remove `HookedAction.wrap(...)` static method.
- [ ] Change `public class HookedAction` to package-private (`class HookedAction`) — or add `@Internal` if already annotated.
- [ ] Remove `@Deprecated(forRemoval = true)` from the class declaration.
- [ ] Verify `HookChainAction` compiles (uses the package-private constructor — no change needed).
- [ ] Verify `mvn -DskipTests compile` passes with no deprecation warnings for `HookedAction`.
- [ ] Add `CHANGELOG.md` entry:

```markdown
### Removed
- `HookedAction` public constructor and `HookedAction.wrap()` static factory.
  Use `action.before(...).after(...)` or profile shorthands (`safely()`, `debug()`, `using(profile)`).
```

---

### Batch 2 — `UIContext` Deprecated Methods (Low-Medium Risk)

**Precondition:** Phase 6 UIContext audit is complete. The audit document identifies every usage and its replacement path. This batch implements the audit's findings.

**Symbols:** `UIContext.setLastLocatorDescriptor(...)`, `UIContext.getLastElement()`

**Checklist:**
- [ ] Locate the Phase 6 UIContext audit document and review replacement paths.
- [ ] For each usage of `setLastLocatorDescriptor`: migrate to replacement identified in audit.
- [ ] For each usage of `getLastElement`: migrate to replacement identified in audit.
- [ ] If no external replacement exists (methods are framework-internal state), make them package-private rather than removing.
- [ ] Remove or restrict the methods per the audit's recommendation.
- [ ] Verify `mvn -DskipTests compile` produces no deprecation warnings for `UIContext`.
- [ ] Add `CHANGELOG.md` entry with before/after migration mapping.

---

### Batch 3 — `Interactions.isAnyDisplayed(By)` (Low Risk)

**Precondition:** Confirm that `VoidDSL.java` is the only caller.

**Checklist:**
- [ ] Grep for `isAnyDisplayed`.
- [ ] Confirm only one usage exists (expected: `VoidDSL.java`).
- [ ] Migrate `VoidDSL.java` usage to `searchAndGetResults()`.
- [ ] Remove `isAnyDisplayed(By)` from `Interactions`.
- [ ] Verify `mvn test` passes.
- [ ] Add `CHANGELOG.md` entry:

```markdown
### Removed
- `Interactions.isAnyDisplayed(By)` — use `searchAndGetResults(field, term)`.
```

---

### Batch 4 — `VOID.getEngine()` / `app.getEngine()` (Medium Risk)

**Precondition:** SESSION-001 documented in Phase 6. Engine-portability exceptions document confirms this is a High-risk portability gap. Every known usage has been audited.

**Step 1 — Deprecate first (this batch)**

This API may have usages in test infrastructure or extension code that cannot be migrated immediately. Deprecate before removing:

```java
/**
 * @deprecated Exposes the concrete engine type, violating the engine-agnostic contract.
 *             Use engine-level methods on the VOID session ({@code app.navigateTo()},
 *             {@code app.getCurrentUrl()}, etc.) or request a new session-level method
 *             if the needed operation is not yet exposed.
 *             This method will be removed in a future release (see SESSION-001).
 */
@Deprecated(forRemoval = true)
UIEngine getEngine();
```

**Step 2 — Remove in a follow-up PR (one release cycle later)**

- [ ] Grep all projects for `app.getEngine()` and `.getEngine()` calls.
- [ ] For each: confirm a VOID session method covers the need, or add one.
- [ ] Remove the `getEngine()` method from `VOID`.
- [ ] Add `CHANGELOG.md` entry:

```markdown
### Removed
- `VOID.getEngine()` — use session-level methods (`app.navigateTo()`, `app.getCurrentUrl()`,
  etc.) or `app.getEngine().getNativeDriver()` as a last-resort escape hatch if the
  underlying engine must be accessed directly.
```

**Checklist (this batch):**
- [ ] Add `@Deprecated(forRemoval = true)` and Javadoc to `VOID.getEngine()`.
- [ ] CI passes with deprecation warning visible.
- [ ] Schedule removal for the next release cycle.

---

## Cross-Cutting Checklist (Apply Before Every Batch)

- [ ] All preconditions above are satisfied.
- [ ] Phase 6 deprecated symbol inventory exists and is current.
- [ ] Migration guides for the symbols being removed are published.
- [ ] Zero deprecated usages of the target symbol in CI compiler output for production code.

## Per-Removal Steps (Apply to Each Batch)

- [ ] Grep codebase for all usages of the symbol.
- [ ] Confirm the replacement is stable, tested, and documented.
- [ ] Migrate internal framework usages.
- [ ] Migrate demo code.
- [ ] Remove or restrict the deprecated symbol.
- [ ] Run `mvn -DskipTests compile` — zero deprecation warnings for this symbol in production packages.
- [ ] Update Javadoc on any types that referenced the removed symbol.
- [ ] Add `CHANGELOG.md` entry with before/after migration mapping.

## After All Batches

- [ ] Run `mvn test` — all tests pass.
- [ ] Verify compiler output contains no `@Deprecated` warnings in core production packages.
- [ ] Update architecture docs to remove references to removed symbols.

---

## Tests

No new test code in this phase. Each batch verifies via:
- `mvn -DskipTests compile` producing no deprecation warnings for the removed symbol.
- `mvn test` passing fully.
- CI clean build.

---

## Exit Criteria

- Zero `@Deprecated(forRemoval = true)` annotations remain in core production packages.
- `Action.withHooks` retains `@Deprecated` removed — it is documented as an advanced API.
- All removed APIs have changelog entries with before/after migration mappings.
- `app.getEngine()` is at minimum deprecated, with a committed removal timeline.
- CI passes cleanly with no deprecation warnings in production code.

---

## What NOT to Do

- Do not remove `Before.*` or `After.*` hook constants — they are stable public API and used by `ActionProfile.builder()` and direct hook users.
- Do not remove `Action.withHooks(List, List)` — this deprecation was reversed. It is an advanced API, not a removed one.
- Do not rush Batch 4 (`app.getEngine()`) — deprecate first, remove in a follow-up release cycle.
- Do not begin any batch without the Phase 6 audit document as input.
- Do not merge any removal batch without a passing CI run.
- Do not remove `HookedAction` as a class — only remove its public constructor and static factory. The class itself is internal infrastructure for hook tracing and must remain.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
