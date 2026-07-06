# Phase 7 — Documentation Realignment

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `main` (documentation only — safe to land directly)  
**Risk:** None — documentation only

---

## Objective

Ensure contributors learn the current architecture, not legacy patterns. Every architecture document, guide, and example should reflect the reality of v2.3 — not the state from six months ago. Produce the documentation foundation that Phase 8 will depend on.

---

## Context

Architecture evolves faster than documentation. Three visible signs of drift:

- `hooks-pipeline.md` still references old `before(...).after(...)` chaining style as the primary path.
- `quick-start.md` shows patterns that have been superseded.
- No version markers on core architecture docs — contributors cannot determine whether a doc is current.
- Phase 6 confirmed: the deprecated symbol inventory exists but has no home in the docs.

### Dependency on Phase 4

Phase 7's hook pipeline diagram must reflect the Phase 4 architecture: capability-declared safe profiles via `ActionCapabilityProvider.safeProfile()`. Do not draw a diagram showing `HookStrategyResolver` — that design was rejected. The diagram should show the path:

```
element.safely()
      ↓
ElementBoundAction.safely()   ← uses capability's declared safeProfile()
      ↓
ActionCapabilityProvider.safeProfile()  ← capability declares its own behavior
      ↓
ActionProfile.before() / after()  ← resolved hook list
```

---

## Architecture Version Header Format

Add to every core architecture document:

```markdown
> **Architecture Version:** 2.3  
> **Last Updated:** 2026-06-13  
> **Status:** Current
```

For sections that must remain for historical context:

```markdown
> **Architecture Version:** 1.x (Legacy)  
> **Status:** Superseded — see [current doc link]
```

---

## Documents to Update

### Core Architecture Docs (`docs/architecture/`)

| File | Required Action |
|------|----------------|
| `system-overview.md` | Add version header. Verify element-action-flow-executor diagram is current. |
| `core-packages.md` | Add version header. Verify `core.actions` and `core.executor` package lists match source. |
| `hooks-pipeline.md` | Add version header. Replace old chaining examples with `safely()` / `using()`. Add capability-driven safe profile diagram (Phase 4). Add `ExecutionPipeline` layer reference (Phase 5). |
| `quick-start.md` | Add version header. Update all code examples to current API. Verify demo code compiles exactly as written. |
| `configuration-reference.md` | Add version header. |
| `locator-resolution.md` | Add version header. |
| `logging-reference.md` | Add version header. |
| `engine-portability-exceptions.md` | New file created in Phase 6 — no additional version header needed. |

### Source-Level Docs

| File | Required Action |
|------|----------------|
| `src/main/java/core/actions/README.md` | Update: current public `Action` API (`perform`, `resolve`, `before`, `after`, `safely`, `debug`, `raw`, `using`, `capability`, `withHooks`). Note that `withHooks` is an advanced API (not deprecated). Remove any reference to `HookStrategyResolver` or `HookStrategy` — these were never implemented. |
| `src/main/java/core/executor/README.md` | Add when Phase 5 lands: document `ExecutionPipeline`, `DefaultExecutionPipeline`, decoration composition pattern. |
| `src/main/java/core/interactions/hooks/Before.java` | Verify all Javadoc is accurate. Engine-portability notes added in Phase 6. |
| `src/main/java/core/interactions/hooks/After.java` | Same. |

---

## CONTRIBUTING.md — New Guardrail Rules

Add four rules, replacing the previous generic branching rule with the targeted version below:

### Rule 1 — Current API in examples
All PR code examples must use the current API style. No `HookedAction`, no `before(...).after(...)` chaining as a primary style — use `safely()`, `debug()`, `raw()`, or `using(ActionProfile)`.

### Rule 2 — Capability branching justification
If a method or class dispatches on capability type — via `instanceof` checks or `switch` on `ActionCapability` — the PR description must justify why the logic cannot live on the capability interface itself (`ActionCapabilityProvider.safeProfile()` or a similar method). This prevents re-introducing the central dispatcher pattern that Phases 3 and 4 eliminated.

### Rule 3 — Action method count guardrail
If the public method count on `Action` reaches 12, open a design discussion. If it reaches 15, a redesign is required before adding more. Track this in PR review.

### Rule 4 — Engine-specific code gate
Any new engine-specific behavior must be logged in `docs/architecture/engine-portability-exceptions.md` before the PR is merged. (Introduced in Phase 6 — reiterate here for visibility.)

---

## Affected Files

Modified (architecture docs):
- `docs/architecture/system-overview.md`
- `docs/architecture/core-packages.md`
- `docs/architecture/hooks-pipeline.md`
- `docs/architecture/quick-start.md`
- `docs/architecture/configuration-reference.md`
- `docs/architecture/locator-resolution.md`
- `docs/architecture/logging-reference.md`

Modified (source README):
- `src/main/java/core/actions/README.md`

Modified (contributing guide):
- `CONTRIBUTING.md`

---

## Migration Strategy

Documentation only. No migration required.

---

## Checklist

### Version Headers
- [ ] Add version header to `system-overview.md`.
- [ ] Add version header to `core-packages.md`.
- [ ] Add version header to `hooks-pipeline.md`.
- [ ] Add version header to `quick-start.md`.
- [ ] Add version header to `configuration-reference.md`.
- [ ] Add version header to `locator-resolution.md`.
- [ ] Add version header to `logging-reference.md`.

### Example Updates
- [ ] Replace any `before(...).after(...)` usages in docs with `safely()` or `using(ActionProfile)` as the primary style.
- [ ] Keep one `before(...).after(...)` example in docs as the advanced/manual path.
- [ ] Verify `quick-start.md` demo code compiles exactly when copy-pasted into a project.
- [ ] Update `hooks-pipeline.md` diagram to show capability-driven safe profile path (Phase 4 design).
- [ ] Add `ExecutionPipeline` layer to execution flow diagram in `hooks-pipeline.md` (Phase 5 design). Do not add before Phase 5 is merged.

### Source README Updates
- [ ] Update `core/actions/README.md` to list all current public `Action` methods including `withHooks` (documented as advanced, not deprecated).
- [ ] Remove any reference to `HookStrategyResolver` or `HookStrategy` — these designs were evaluated and not implemented.

### CONTRIBUTING.md
- [ ] Add Rule 1 (current API in examples).
- [ ] Add Rule 2 (capability branching justification — replaces the previous "3+ conditionals" rule).
- [ ] Add Rule 3 (Action method count guardrail: alert at 12, redesign at 15).
- [ ] Add Rule 4 (engine-specific code gate — reiterate from Phase 6).

### Pre-Phase-8 Preparation
- [ ] Verify that Phase 6's deprecated symbol inventory document is complete and available.
- [ ] Verify that the UIContext audit (performed in Phase 6) has a documented replacement path for each deprecated method.
- [ ] Confirm that `HookedAction.wrap()` and the public `HookedAction(...)` constructor have no usages outside of `HookChainAction` in the current codebase. Document the finding.
- [ ] Confirm current usage count of `action.withHooks(List, List)` in test code and production code. Document.

---

## Tests

No new test code. All deliverables are documentation and CONTRIBUTING.md changes.

- [ ] Verify `quick-start.md` demo code compiles as written.
- [ ] Review: no doc references `HookStrategyResolver`, `HookStrategy`, or superseded patterns as current.

---

## Exit Criteria

- All core architecture docs have version headers.
- No document teaches deprecated or non-existent API patterns as current.
- `quick-start.md` demo code compiles exactly as written.
- `CONTRIBUTING.md` contains all four new guardrail rules, replacing the previous generic branching rule.
- Pre-Phase-8 preparation checklist is complete: deprecated symbol inventory reviewed and usage audit findings documented.

---

## What NOT to Do

- Do not remove historical context from docs — mark it with `(Legacy)` headers instead.
- Do not update docs that describe future plans as if they are current — mark them as `(Planned)`.
- Do not land the `ExecutionPipeline` diagram in `hooks-pipeline.md` before Phase 5 is merged.
- Do not land the capability-driven safe profile diagram before Phase 4 is merged.
- Do not create a `HookStrategyResolver` section in any doc — this design was evaluated and rejected.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
