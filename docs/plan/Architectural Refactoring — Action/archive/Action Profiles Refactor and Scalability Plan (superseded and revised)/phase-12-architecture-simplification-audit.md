# Phase 12 — Architecture Simplification Audit

**Status:** Ongoing (blocked on Phase 8, Phase 9, Phase 10, Phase 11)  
**Architecture Version:** 2.3  
**Branch:** Per-finding feature branch  
**Risk:** Medium — any removal is permanent; proceed finding by finding

---

## Objective

After all architecture phases are complete, audit the codebase for complexity that became unnecessary during the evolution. Delete abstractions that are now redundant, collapse trivial wrappers, inline delegators, and remove temporary fallbacks and compatibility shims. Verify that no duplicate concepts remain.

Every long-lived framework needs this cleanup after major evolution. The goal is not to add functionality — it is to reduce the surface area to what the architecture actually requires.

---

## When to Run This Phase

This phase begins only when all of the following are complete:

- [ ] Phase 8 — Deprecated Code Removal (all batches)
- [ ] Phase 9 — Profile Completion
- [ ] Phase 10 — ExecutionPipeline Implementations (at least two implemented and in use)
- [ ] Phase 11 — Session API Completion

Running this phase early — before deprecated code is removed or before abstractions prove themselves through real usage — risks removing things that are still needed.

---

## Audit Framework

For each finding, answer four questions before acting:

1. **Is this still used?** Grep. Do not rely on memory.
2. **Did we add this temporarily?** Look at the git blame and commit message. Temporary shims and fallbacks have a limited legitimate lifetime.
3. **Does this serve a unique purpose?** If two abstractions cover the same concept, one is probably redundant. Identify which one is the authoritative version.
4. **What breaks if we remove it?** If the answer is "nothing that a one-line change wouldn't fix," removal is probably correct.

---

## Questions to Answer for Each Area

### `ActionProfile` and `ActionProfile.Builder`

- Does `ActionProfile` still need to be an interface, or can it be a record?
- Is `ProfileBuilder` still a separate class, or was it collapsed into `ActionProfile`?
- Are there any profiles constructed inline (`.builder().before(...).build()`) that appear more than twice and should be named constants?
- Does `ActionProfiles` need any constants that were added but never referenced?

### `HookedAction` class

- After Phase 8 removes the public constructor and `wrap()` factory, is `HookedAction` still the right vehicle for hook tracing?
- Can `HookedAction`'s remaining logic (`performAndTrace()`, `LAST_TRACE`) be inlined into `HookChainAction`?
- Is the `ActionTrace` / `LAST_TRACE` `ThreadLocal` mechanism still the best tracing approach after Phase 10's `TracingExecutionPipeline` exists?

### `Profiles` enum

- After Phase 9, how often is `Profiles.SAFE.before(action)` actually called through its switch?
- Is the switch still the primary path for any code, or is it now a compatibility path only?
- Could `Profiles` be simplified to constants that delegate to `ActionProfiles.DEFAULT_*`?
- Are `Profiles.RELIABLE`, `Profiles.FAST`, `Profiles.DEBUG` switch arms still exercised by tests?

### `ElementRole` fallback in `ElementActions.capabilityFor()`

- After Phase 4, the fallback is: `if (role == ElementRole.INPUT) return TYPEABLE; ...`
- Is this fallback still reachable? Are there elements that reach `capabilityFor()` without being `ActionCapabilityProvider`?
- If zero elements reach the fallback in practice, can it be removed or replaced with a strict assertion?

### `ExecutionPipeline` composition

- After Phase 10, are any two pipeline implementations always paired together in every usage?
- If `TracingExecutionPipeline` + `MetricsExecutionPipeline` are always combined, should they be merged or pre-composed?
- Are there any `ExecutionPipeline` implementations added in Phase 10 that were never actually used after initial integration?

### `ElementBoundAction` size

- After Phases 4 and 9, `ElementBoundAction` holds four profile fields (safe, reliable, fast, debug).
- Is this the right place for profiles, or do they belong elsewhere?
- Can `ElementBoundAction` be simplified? Is there a better data structure?

### Capability interface hierarchy

- After all phases complete, are there any capability interfaces that have no real implementations in the test suite or production elements?
- Are there capability interfaces that were added speculatively (no real element uses them)?

### Extension points

- Are there any extension points (`@FunctionalInterface`, abstract hooks, plugin-style registries) that were added with future use in mind but have no current users?
- If an extension point has existed for two or more phases with no implementations, consider removing it.

---

## Cleanup Categories

### Category 1 — Delete (Zero Risk)
Unused code with no external callers.
- Unused `ActionProfiles` constants
- Unused `Profiles.*` switch arms
- Extension interfaces with no implementations

### Category 2 — Inline (Low Risk)
One-method wrappers and trivial delegators.
- A method that just calls another method with no transformation
- An interface with one implementation that never needs to vary

### Category 3 — Collapse (Medium Risk)
Two abstractions that cover the same concept.
- `HookedAction` tracing vs `TracingExecutionPipeline` — determine one authoritative path
- `ActionProfile` builder vs inline constants — determine the canonical construction style

### Category 4 — Restrict (Low-Medium Risk)
Change visibility from public to package-private.
- Internal classes that have no legitimate external callers after Phase 8

---

## Checklist

### Preparation
- [ ] All precondition phases are complete (see above).
- [ ] Run `mvn test` — confirm a clean baseline.
- [ ] Run deprecation scan — confirm zero `@Deprecated(forRemoval = true)` in production packages.

### Investigation
- [ ] Grep for all usages of `Profiles.SAFE.before(`, `Profiles.RELIABLE.before(`, etc. — document call count.
- [ ] Grep for `HookedAction` usages outside `core.actions` — confirm zero after Phase 8.
- [ ] Grep for `ElementRole.INPUT`, `ElementRole.LIST`, `ElementRole.TRIGGER` in `capabilityFor()` path — count reachable callers.
- [ ] Grep for `ActionProfiles.*` — confirm all constants are referenced.
- [ ] List all `ExecutionPipeline` implementations — confirm each has production usage.

### Findings
- [ ] Document each finding in this section with category (Delete / Inline / Collapse / Restrict).
- [ ] For each finding: confirm removal is safe with a targeted test run.
- [ ] For each finding: confirm the git commit message explains what was removed and why.

### Execution
- [ ] Execute each finding as a separate PR with a clear before/after demonstration.
- [ ] After each removal: `mvn test` passes.
- [ ] After all removals: `mvn test` passes.

### Verification
- [ ] No duplicate concepts remain (confirmed by reviewing the findings list).
- [ ] `CHANGELOG.md` has an entry for each removed abstraction.
- [ ] Architecture docs updated to remove any reference to removed concepts.

---

## Tests

No new test code in this phase. Each removal is verified by:
- The existing test suite continuing to pass.
- A targeted test confirming the removed feature is truly absent (where applicable).

---

## Exit Criteria

- Codebase has no unnecessary abstractions, trivial wrappers, or redundant extension points.
- No duplicate concepts exist covering the same responsibility.
- All temporary fallbacks and compatibility shims introduced during Phases 1–11 have been removed.
- `mvn test` passes cleanly.
- Every removal is documented in `CHANGELOG.md`.

---

## What NOT to Do

- Do not remove `Before.*` or `After.*` hook constants — they are stable public API.
- Do not remove `ActionProfile.builder()` unless a better construction mechanism has replaced it and all callers are migrated.
- Do not remove `Profiles.*` enum constants unless every `.using(Profiles.*)` caller has migrated to capability-declared profiles.
- Do not begin until all precondition phases are complete. Running this phase early risks removing things that are still needed.
- Do not remove things speculatively. Grep first, confirm zero usages, then remove.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
