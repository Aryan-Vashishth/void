# Experiments

Lightweight workflow for tracking ideas → active work → outcomes.

---

## Flow

```
prospects/ → active/ → outcomes/
```

| Folder | Purpose | When to use |
|--------|---------|-------------|
| `prospects/` | New ideas, future considerations | You have an idea worth writing down |
| `active/` | Currently being explored | You're actively working on it |
| `outcomes/` | Completed experiments | Work is done — promoted or rejected |

---

## Current Status

### Active
- **Phase 0 — Lock Architecture Rules** (`2026-05-05`) — Rules defined, ArchUnit enforcement wired, freeze baselines committed. Active for the duration of the refactor (no exit criteria).
- **Multi-Engine Execution** (`2026-05-01`) — Phases 1–2 complete (UIEngine, Action/Flow/FlowExecutor, SeleniumEngine, UIEngineFactory). Phase 3 (Playwright prototype) next.

### Completed Outcomes
- **Phase 0 — Lock Architecture Rules** (`2026-05-05`) — Rules R1–R9 locked. ArchUnit enforcement live. Structural foundation delivered. Zero new violations introduced. See `outcomes/2026-05-05-phase-0-lock-architecture-rules.md`.
- **Interaction–Execution Separation** (`2026-06`) — **Implemented**. `Interactions` refactored to pure orchestrator. `UIEngine` is the single execution authority. All legacy methods deprecated.

### Prospects
- `2026-05-05-exec-01-phase-0-lock-architecture-rules.md` — Exec 1 · Phase 0 · Lock Architecture Rules (governance note, always-on).
- `2026-05-05-exec-02-phase-2-resolution-unification.md` — Exec 2 · Phase 2 · Resolution Unification.
- `2026-05-05-exec-03-phase-1-bootstrap-startup-ownership.md` — Exec 3 · Phase 1 · Fix Bootstrap & Startup Ownership.
- `2026-05-05-exec-04-phase-1-5-lock-execution-model.md` — Exec 4 · Phase 1.5 · Lock Execution Model in Code.
- `2026-05-05-exec-05-phase-3-wire-flow-executor.md` — Exec 5 · Phase 3 · Wire `FlowExecutor` into Runtime.
- `2026-05-05-exec-06-phase-4-remove-selenium-leakage.md` — Exec 6 · Phase 4 · Remove Selenium Leakage from Public Surface.
- `2026-05-05-exec-07-phase-5-interactions-strict-adapter.md` — Exec 7 · Phase 5 · Convert `Interactions` to Strict Adapter.
- `2026-05-05-exec-08-phase-6-remove-uicontext.md` — Exec 8 · Phase 6 · Remove `UIContext`.
- `2026-05-05-exec-09-phase-7-finalize-hook-model.md` — Exec 9 · Phase 7 · Finalize Hook Model.
- `2026-05-05-exec-10-phase-8-playwright-engine.md` — Exec 10 · Phase 8 · Add Playwright Engine.

---

## Rules

1. All files use: `YYYY-MM-DD-<short-title>.md`
2. Flow is one-directional: prospects → active → outcomes
3. Only **Promoted** outcomes lead to `/docs/decisions/` or `architecture/` updates
4. **Rejected** outcomes stay in `outcomes/` (or move to `archive/` if obsolete)

---

## Minimal Format

### prospects/

```markdown
# <Title>

**Date:** YYYY-MM-DD

## Idea

What's being proposed.

## Why It Matters

Why this is worth exploring.
```

### active/

Add to existing file when moved:

```markdown
## Approach

How you're exploring it.

## Notes

Observations during exploration.
```

### outcomes/

Add to existing file when moved:

```markdown
**Status:** Promoted | Rejected  
**Completed:** YYYY-MM-DD

## Conclusion

What was decided.

## What Was Learned

Key takeaways.
```
