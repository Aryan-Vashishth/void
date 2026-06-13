# Phase 7 — Documentation Realignment

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `main` (docs-only, safe to land directly)  
**Risk:** None — documentation only

---

## Objective

Ensure contributors learn the current architecture, not legacy patterns. Every architecture doc, guide, and example should reflect the reality of `v2.3` — not the state from six months ago.

---

## Context

Architecture is evolving faster than docs. This is visible:

- Hooks pipeline doc still references old `before(...).after(...)` chaining.
- Quick-start guide shows patterns that have been superseded.
- Some pages have no version marker so contributors cannot tell if they are current.
- The audit noted: "future contributors learn obsolete architecture."

---

## What an Architecture Version Header Looks Like

Add to every core architecture doc:

```markdown
> **Architecture Version:** 2.3  
> **Last Updated:** 2026-06-13  
> **Status:** Current
```

For legacy sections that must remain for historical context:

```markdown
> **Architecture Version:** 1.x (Legacy)  
> **Status:** Superseded — see [current doc]
```

---

## Documents to Update

### Core Architecture Docs (`docs/architecture/`)

| File | Action Required |
|---|---|
| `system-overview.md` | Add version header. Verify element-action-flow diagram is current. |
| `core-packages.md` | Add version header. Verify `core.actions` package list matches source. |
| `hooks-pipeline.md` | Add version header. Replace old chaining examples with `safely()`/`using()`. Add strategy layer diagram. |
| `quick-start.md` | Add version header. Update code examples to current API. |
| `configuration-reference.md` | Add version header. |
| `locator-resolution.md` | Add version header. |
| `logging-reference.md` | Add version header. |

### Source-Level Docs

| File | Action Required |
|---|---|
| `src/main/java/core/actions/README.md` | Update with current API: `perform`, `resolve`, `withHooks`, `safely`, `debug`, `raw`, `using`. |
| `src/main/java/core/executor/README.md` | Add `ExecutionPipeline` when Phase 5 lands. |
| `src/main/java/core/interactions/hooks/Before.java` | Verify all Javadoc is accurate and complete. |
| `src/main/java/core/interactions/hooks/After.java` | Verify all Javadoc is accurate and complete. |

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
- [ ] Replace any `before(...).after(...)` examples with `safely()` or `using(Profiles.SAFE)`.
- [ ] Add at least one `withHooks(List, List)` example as the advanced/custom path.
- [ ] Verify `quick-start.md` demo code matches `VoidDemo.java` exactly.
- [ ] Add hook strategy layer to `hooks-pipeline.md` diagram.

### Source README Updates
- [ ] Update `core/actions/README.md` to show all current public Action methods.
- [ ] Add note on deprecation of `HookedAction` with migration path.

### Contributing Guide
- [ ] Add to `CONTRIBUTING.md`: all PRs must use current API style in examples.
- [ ] Add to `CONTRIBUTING.md`: branching rule — 3+ conditionals requires a note explaining why.
- [ ] Add to `CONTRIBUTING.md`: Action method count guardrail — alert at 12, redesign at 15.
- [ ] Add to `CONTRIBUTING.md`: engine-specific code must be logged in portability exceptions doc.

---

## Exit Criteria

- All architecture docs have `Architecture Version` headers.
- No doc shows deprecated or non-existent API patterns.
- `quick-start.md` demo code compiles cleanly when copy-pasted.
- `CONTRIBUTING.md` contains the four new guardrail rules.

---

## What NOT to Do

- Do not remove historical context from docs — use "Legacy" sections instead.
- Do not update docs that describe future plans as if they are current.
- Do not land doc changes that reference Phase 5 or later before those phases are implemented.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

