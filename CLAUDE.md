# VOID Framework -- Claude Instructions

Project-level instructions for Claude Code. These override default behavior.

---

## Development Philosophy

Every significant change begins with exploration, not implementation. Before writing code,
audit the current state, write a plan, and validate the plan. The architecture should be
understood before it is changed.

**Stages in order:**

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

**Do not skip stages.** If the user asks to implement something without an existing plan,
ask whether to audit first or proceed directly -- do not silently skip the audit step.

---

## Workflow Rules

- Audit before implementation. When a significant architectural change is requested, start
  with an audit document in `docs/plan/draft/<initiative>/audit/` before writing any plan.
- Write the plan before writing code. Implementation phases live in
  `docs/plan/draft/<initiative>/`. Move to `docs/plan/done/` when all phases are merged.
- One commit per phase step. Never mix changes from two phases in a single commit.
- Each phase must compile and pass `mvn compile -q` before the next phase begins.
- Plans are written in `docs/plan/draft/`. Completed plans move to `docs/plan/done/`.
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

Use `initiative/` not `feature/` for architectural work. The word "feature" implies additive
work; an initiative may restructure, consolidate, or remove.

---

## Commit Format

Conventional Commits. Imperative present tense. No em dashes.

```
<type>(<scope>): <short summary>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

Examples:
```
feat(engine): add SeleniumEngine(Profile) constructor
refactor(runtime): replace ExecutionContext with SessionContext in VOID
docs(decisions): add ADR-018 engine lifecycle ownership
test(runtime): add VOIDBuilderTest covering fluent API and single-use guard
```

Never use `--` (em dash) in commit messages. Use a plain hyphen or semicolon.

---

## Reading Docs Before Proceeding

**Always read relevant documentation before starting any task.** The docs directory is the
authoritative record of architectural intent. Code without its context is incomplete
information.

### What to read, and when

| Situation | Read first |
|---|---|
| Starting or continuing an initiative | `docs/plan/draft/<initiative>/index.md` + the relevant phase doc |
| Implementing any architectural change | `docs/decisions/accepted/` -- check if an ADR already governs this area |
| Touching engine, runtime, or session code | `docs/decisions/accepted/007-uiengine-execution-authority.md`, `ADR-011`, `ADR-018` |
| Touching element or capability code | `docs/decisions/accepted/008-capability-interfaces.md`, `ADR-016`, `ADR-017` |
| Writing or reviewing a plan | The pre-plan audit in `docs/plan/draft/<initiative>/audit/` |
| Checking if a plan is still active | `docs/plan/draft/README.md` and `docs/plan/done/README.md` |
| Any change that may affect architecture | `docs/audits/ongoing/` for open findings |

If a relevant plan, ADR, or audit exists and has not been read, stop and read it before
proposing or implementing anything. Do not rely on memory of a prior session alone -- the
docs may have changed.

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
  architecture/                -- user-facing guides (quick-start, core-packages, etc.)
  audits/
    ongoing/                   -- audits with open findings; check before touching related code
    fulfilled/                 -- audits whose findings have been fully addressed
```

### How each directory is used

**`docs/plan/draft/`** -- The working space for active initiatives. Every initiative has a
subdirectory containing an `audit/` folder, an `index.md` overview, and one `phase-N-*.md`
file per implementation phase. When all phases are complete and the initiative is merged,
the entire directory moves to `docs/plan/done/` with its status updated.

**`docs/plan/done/`** -- Historical record of completed work. Do not modify these docs
except to correct factual errors or update status fields. They are reference material, not
living documents.

**`docs/decisions/accepted/`** -- The canonical record of architectural decisions that are
live in `main`. Before making any change to a governed subsystem, read the relevant ADR.
The index in `accepted/README.md` maps ADR numbers to titles and areas.

**`docs/decisions/pending-review/`** -- ADRs for initiatives that are implemented but not
yet merged. Treat these with the same authority as accepted ADRs when working on the
corresponding initiative branch. When the initiative merges, move these to `accepted/` and
update the index.

**`docs/architecture/`** -- User-facing guides. These must stay current with the public API.
If an initiative changes a public API, update the relevant guide in the same initiative.
Do not let these fall behind.

**`docs/audits/ongoing/`** -- Open audit findings. Any unresolved finding here is a known
architectural debt item. Check this directory before touching areas it covers; do not
introduce changes that would deepen an open finding.

---

## Project Structure

```
docs/
  plan/
    draft/           -- active initiative plans (audit + phase docs)
    done/            -- completed initiative plans (all phases merged)
  decisions/
    pending-review/  -- ADRs for implemented but unmerged initiatives
    accepted/        -- ADRs for decisions in main
  architecture/      -- user-facing architecture guides
  audits/            -- ongoing and fulfilled architecture audits
src/
  main/java/         -- production source
  test/java/         -- unit tests (TestNG; no Mockito; no live browser in unit tests)
```

---

## Code Rules

- Java 17. Use records, sealed classes, pattern matching where appropriate.
- No Lombok, no compile-time code generation.
- No wildcard imports (except `static` imports from `CustomLogger` and `AnsiColors`).
- Logging: `CustomLogger` (`info`, `debug`, `warn`, `error`). Never `System.out.println`.
- Initialize logger in test classes: `CustomLogger.initialize(MyClass.class)` in `@BeforeClass`.
- Test naming: `methodUnderTest_scenario_expectedOutcome`.
- Unit tests do not open a browser. Use reflection for private fields/methods when needed.
- Static utility classes must have a `private` constructor.

---

## Architectural Constraints (Do Not Violate)

- `UIEngine` is the single execution authority. No code outside `UIEngine` implementations
  should call `WebDriver` methods directly.
- `DriverContext` is Selenium-specific. Do not reference it from engine-agnostic layers.
- `LocatorDescriptor` must not depend on `org.openqa.selenium.By`.
- `ElementSupport` (package-private in `elements.api`) contains exactly three methods:
  `nameOf`, `declaringClassOf`, `ordinalOf`. Do not add to it without an ADR.
- `Target` (in `core.target`) must not carry enum-specific default methods.
- `VOIDBuilder` is single-use. Each session requires a new `VOID.builder()` call.

---

## Memory

Project memory lives at:
`C:\Users\AryanVashishth\.claude\projects\D--void-framework\memory\`

Check `MEMORY.md` in that directory for the index of saved context.
