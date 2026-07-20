# Development Workflow

## Initiative Lifecycle

Every significant change begins with exploration, not implementation. The stages are fixed
and must not be skipped. If asked to implement something without a plan, ask whether to
audit first or proceed directly.

```
Architectural Conversation
        |
        v
Architecture Audit        (docs/plan/draft/<initiative>/audit/)
        |
        v
Implementation Plan       (docs/plan/draft/<initiative>/index.md + phase-N.md)
        |
        v
Plan Validation           (post-plan audit; confirm plan is internally consistent)
        |
        v
Implementation (Phased)   (one commit per phase step; each step must compile + pass tests)
        |
        v
Full-System Audit         (audit entire initiative as a coherent system)
        |
        v
Hotfix Initiative         (if audit finds problems: hotfix/<initiative>-final-audit branch)
        |
        v
Architecture Decision     (docs/decisions/pending-review/)
        |
        v
Merge to main
```

| Stage | Purpose |
|---|---|
| Architecture Audit | Understand the current state; identify coupling, violations, and risks before any plan is written |
| Implementation Plan | Define phases, file changes, and commit sequence; make architectural decisions explicit |
| Plan Validation | Verify the plan is internally consistent and addresses the identified violations |
| Implementation | Execute phases in order; each phase must compile and pass tests independently |
| Full-System Audit | After all phases, audit the initiative as a coherent system |
| Hotfix Initiative | If the full-system audit finds integration issues, address them on a scoped hotfix branch |
| Architecture Decision | Record the decision in `docs/decisions/pending-review/` after implementation and review |

---

## Workflow Rules

- Audit before implementation. When a significant architectural change is requested, start
  with an audit document in `docs/plan/draft/<initiative>/audit/`.
- Write the plan before writing code. Phases live in `docs/plan/draft/<initiative>/`.
  Move to `docs/plan/done/` when all phases are merged.
- One commit per phase step. Never mix changes from two phases in a single commit.
- Each phase must compile and pass `mvn compile -q` before the next phase begins.
- ADRs go in `docs/decisions/pending-review/` after implementation; move to
  `docs/decisions/accepted/` after merge to main.

---

## Branch Naming

| Branch | When |
|---|---|
| `initiative/<name>` | Multi-phase architectural work |
| `hotfix/<name>` | Scoped corrections found during a final audit |
| `bugfix/<name>` | Isolated bug fixes that do not require an initiative |
| `docs/<name>` | Documentation-only changes |

Use `initiative/` not `feature/`. The word "feature" implies additive work; an initiative
may restructure, consolidate, or remove.

---

## Phase-Based Development

An initiative is divided into phases. A phase is a self-contained implementation milestone:
it leaves the codebase in a compilable, test-passing state and moves the initiative
measurably forward. Phases are not merge points.

```
initiative/<name>
    |-- Phase 1  (committed, compilable, tests pass)
    |-- Phase 2  (committed, compilable, tests pass)
    |-- ...
    |-- Full-System Audit
    `-- Merge to main
```

Each phase has a corresponding plan document in `docs/plan/draft/<initiative>/`. One commit
per phase step -- never mix phase changes in a single commit.

---

## Merge Policy

An initiative is merged to `main` only when:

- All planned phases are complete.
- The full regression suite passes (`mvn clean test`).
- The full-system audit has been conducted and any findings addressed.
- No architectural violations remain from the original violation map.
- ADRs are written and placed in `docs/decisions/pending-review/`.

`main` receives only production-ready architectural changes.

---

## Reading Docs Before Proceeding

Always read relevant documentation before starting any task. The docs directory is the
authoritative record of architectural intent.

### What to read, and when

| Situation | Read first |
|---|---|
| Starting or continuing an initiative | `docs/plan/draft/<initiative>/index.md` + the relevant phase doc |
| Implementing any architectural change | `docs/decisions/accepted/` -- check if an ADR already governs this area |
| Touching engine, runtime, or session code | ADR-007, ADR-011, ADR-018 |
| Touching element or capability code | ADR-008, ADR-016, ADR-017 |
| Writing or reviewing a plan | The pre-plan audit in `docs/plan/draft/<initiative>/audit/` |
| Checking if a plan is still active | `docs/plan/draft/README.md` and `docs/plan/done/README.md` |
| Any change that may affect architecture | `docs/audits/ongoing/` for open findings |

### Minimum reads for common tasks

**New initiative:**
1. `docs/plan/draft/README.md` -- confirm the initiative is listed and active
2. `docs/plan/draft/<initiative>/audit/` -- read the pre-plan audit
3. `docs/plan/draft/<initiative>/index.md` -- read the full plan

**Continuing an initiative:**
1. `docs/plan/draft/<initiative>/index.md` -- re-read the phase overview
2. The specific phase doc for the current phase
3. Any open audit findings in `docs/audits/ongoing/`

**Adding a method or class to an existing subsystem:**
1. The ADR(s) that govern that subsystem (`docs/decisions/accepted/`)
2. The relevant plan phase if the subsystem is mid-initiative

**Writing an ADR:**
1. `docs/decisions/accepted/README.md` -- check the existing index and numbering
2. The two or three most recent accepted ADRs to match tone and structure

---

## docs/ Directory Guide

```
docs/
  plan/
    draft/                     -- initiatives that are planned or in progress
      README.md                -- index of all active draft initiatives
      <initiative>/
        audit/                 -- pre-plan and post-plan architecture audits
        index.md               -- initiative overview: problem, phases, dependency rationale
        phase-N-<name>.md      -- one file per phase: goal, changes, commits, verification
    done/
      README.md                -- index of all completed initiatives
      <initiative>/            -- plan docs preserved after merge; status updated to Complete
  decisions/
    README.md                  -- explains accepted/ and pending-review/ directories
    accepted/
      README.md                -- index table of all accepted ADRs
      NNN-<slug>.md            -- one ADR per architectural decision
    pending-review/
      README.md                -- index of ADRs awaiting merge
      NNN-<slug>.md            -- ADR implemented on an initiative branch, not yet in main
  architecture/                -- user-facing guides (quick-start, core-packages, oop-principles)
  audits/
    ongoing/                   -- audits with open findings; check before touching related code
    fulfilled/                 -- audits whose findings have been fully addressed
    backlog/                   -- planned audits not yet started
      violations/              -- individual OOP violations found incidentally during development
  contributing/                -- this directory; detailed workflow and standards docs
```

**`docs/plan/draft/`** -- Working space for active initiatives. When all phases are complete
and merged, the entire directory moves to `docs/plan/done/`.

**`docs/plan/done/`** -- Historical record. Do not modify except to correct factual errors.

**`docs/decisions/accepted/`** -- Canonical record of architectural decisions live in `main`.
Read the relevant ADR before touching any governed subsystem.

**`docs/decisions/pending-review/`** -- ADRs for implemented but unmerged initiatives.
Treat with the same authority as accepted ADRs on the corresponding branch.

**`docs/architecture/`** -- User-facing guides. Must stay current with the public API.

**`docs/audits/ongoing/`** -- Open findings. Do not introduce changes that deepen them.

**`docs/audits/backlog/violations/`** -- New OOP violations found incidentally. Log here
immediately; triage into a remediation phase during the next planning session.
