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
- **Multi-Engine Execution** (`2026-05-01`) — Phases 1–2 complete (UIEngine, Action/Flow/FlowExecutor, SeleniumEngine, UIEngineFactory). Phase 3 (Playwright prototype) next.

### Completed Outcomes
- **Interaction–Execution Separation** (`2026-06`) — **Implemented**. `Interactions` refactored to pure orchestrator. `UIEngine` is the single execution authority. All legacy methods deprecated.

### Prospects
_(empty — all current ideas are in active development)_

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
