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
