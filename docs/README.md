# Docs Workflow (State-Driven)

This directory is organized by **state**, not by document type.

## Directory states

- `0-ideas/` - raw thinking, loose notes, no structured plan
- `1-drafts/` - structured plan exists, implementation not started
- `2-active/` - implementation has started (code/work in progress)
- `3-review/` - implementation complete, waiting for formal decision
- `4-decisions/accepted/` - approved decisions
- `4-decisions/rejected/` - rejected decisions
- `5-architecture/` - stable truth projected from accepted decisions
- `6-audits/` - system evaluations and assessments
- `7-archive/` - historical or retired material

## Strict lifecycle

`0-ideas -> 1-drafts -> 2-active -> 3-review -> 4-decisions -> (5-architecture or 7-archive)`

## Hard constraints

1. No skipping `3-review`.
2. No direct move to `5-architecture`.
3. No auto-promotion from `3-review`; decision is mandatory.
4. No backward move without a reason recorded in the header.

## Deterministic classification rules

### Prospects split (no wording heuristics)

- Move to `1-drafts/` if the document has a scoped plan (for example scope, steps, boundaries, success criteria).
- Move to `0-ideas/` if it is loose thinking without scoped execution.
- Nothing moves to `2-active/` unless implementation has started in code/work artifacts.

### Review and architecture gating

- `3-review/` means decision pending.
- Everything in `3-review/` must move to `4-decisions/accepted` or `4-decisions/rejected` explicitly.
- `5-architecture/` is optional promotion from `4-decisions/accepted` only.

## Naming convention

Use:

`YYYY-MM-DD-[type]-[slug].md`

Examples:

- `2026-05-05-exp-exec-pipeline.md`
- `2026-05-06-dec-ui-engine-authority.md`
- `2026-05-07-audit-dependency-graph.md`

## Required header

```md
Doc-ID: EXP-2026-05-05-exec-flow
Status: DRAFT | ACTIVE | REVIEW | ACCEPTED | REJECTED | ARCHIVED
State: 0-IDEAS | 1-DRAFTS | 2-ACTIVE | 3-REVIEW | 4-DECISIONS | 5-ARCHITECTURE | 6-AUDITS | 7-ARCHIVE
Owner: Void
Created: YYYY-MM-DD
Updated: YYYY-MM-DD
Related: <decision-id or linked docs>
Original-Path: <legacy path when migrated>
Reason: <required when moving backward>
```

## Link safety net for moves

When a document moves, leave a temporary stub in the old location:

```md
# Moved

This document has moved to:
-> ../../2-active/2026-05-05-exp-exec-flow.md
```

Remove stubs only after links are updated and validated.

## Phased migration checklist

1. Lock transition rules and Doc-ID format.
2. Create state folders and a migration manifest (`old_path -> new_path`).
3. Move low-risk sets (`architecture -> 5`, `audits -> 6`, `archive -> 7`).
4. Split decisions into `accepted`/`rejected` without renaming.
5. Move experiments in order: `active -> 2`, `prospects -> 0/1`, `outcomes -> 3`, `archive -> 7`.
6. Add redirect stubs, update links, then remove stubs in cleanup.

