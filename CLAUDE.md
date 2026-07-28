# VOID Runtime -- Claude Instructions

Project-level instructions for Claude Code. These override default behavior.

## Core Principles

- Documentation is the source of truth.
- Architecture before implementation.
- Prefer extension over modification.
- Preserve engine neutrality.
- Keep abstractions minimal and intentional.

---

## Instruction Precedence

When guidance conflicts, follow this order:

1. Current initiative plan (`docs/plan/draft/<initiative>/`)
2. Accepted ADRs, or pending-review ADRs when working on their initiative branch
3. Architecture invariants (this file)
4. Coding conventions

If two documents conflict, report the inconsistency before proceeding rather than choosing
arbitrarily.

---

## Before Modifying Architecture

Understand the current design before changing it.

- Read the governing ADR(s) for the subsystem being touched.
- Read the active initiative plan if the subsystem is mid-initiative.
- Check `docs/audits/ongoing/` for open findings in the affected area.

Do not design from code alone.

### Documentation Scope

Do not load additional documentation for:

- isolated bug fixes
- formatting or documentation-only changes
- test-only changes with no architectural impact

Only read documents relevant to the current task.

---

## Development Philosophy

Every significant change begins with exploration, not implementation.

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
Plan Validation           (post-plan audit)
        |
        v
Implementation (Phased)   (one commit per phase step; compile + tests pass between each)
        |
        v
Full-System Audit
        |
        v
Hotfix Initiative         (if needed: hotfix/<initiative>-final-audit)
        |
        v
Architecture Decision     (docs/decisions/pending-review/)
        |
        v
Merge to main
```

Do not skip stages. Full details: `docs/contributing/workflow.md`.

---

## Execution Mode

Unless the user requests otherwise:

- Complete a coherent unit of work before responding.
- Do not stop after every small edit to ask for confirmation.
- Batch related changes into a single implementation step.
- Keep responses focused on decisions, verification, and remaining risks.

---

## Branch Naming

| Branch | When |
|---|---|
| `initiative/<name>` | Multi-phase architectural work |
| `hotfix/<name>` | Scoped corrections from a final audit |
| `bugfix/<name>` | Isolated bug fixes |
| `docs/<name>` | Documentation-only changes |

Use `initiative/` not `feature/`.

**Never commit or push directly to `main`.** Every change -- including single-line
documentation fixes, changelog updates, and version bumps -- must go through a branch
and be merged via `--no-ff`. `main` is the integration point only.

---

## Commit Format

Conventional Commits. Imperative present tense. No em dashes.

```
<type>(<scope>): <short summary>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

```
feat(engine): add SeleniumEngine(Profile) constructor
refactor(runtime): replace ExecutionContext with SessionContext in VOID
docs(decisions): add ADR-018 engine lifecycle ownership
test(runtime): add VOIDBuilderTest covering fluent API and single-use guard
```

---

## Release Checklist

All version-bump changes must go through a branch (e.g. `docs/release-x.y.z`) and be
merged via `--no-ff` before tagging. Never commit version files directly to `main`.

When cutting a new version:

1. On a release branch, bump all three files atomically:
   - `pom.xml` -- `<version>x.y.z</version>`
   - `version.json` -- `{ "version": "x.y.z" }` (README badge reads from this via shields.io)
   - `CHANGELOG.md` -- promote `## [Unreleased]` to `## [x.y.z] - YYYY-MM-DD`; add a new
     empty `## [Unreleased]` above it
2. Update the comparison links at the bottom of `CHANGELOG.md`.
3. Merge the release branch to `main` via `--no-ff`.
4. Tag the merge commit on `main` and push the tag:
   ```
   git tag vx.y.z
   git push origin vx.y.z
   ```
5. Create the GitHub release:
   ```
   gh release create vx.y.z --title "vx.y.z" --notes-file <notes-file>
   ```

---

## Architecture Invariants

Non-negotiable. Raise a violation explicitly before working around one.

Axis follows ADR-021's two neutrality axes (**engine**: does this seam prevent swapping
Selenium for Playwright/Appium; **domain**: does this seam prevent adding REST/CLI/Database
alongside Web/UI) plus **scope**, for invariants that are about API/lifecycle discipline
rather than either neutrality axis.

| Invariant | Axis | Rule |
|---|---|---|
| `UIEngine` is the single execution authority | engine | Nothing outside `UIEngine` implementations calls `WebDriver` methods directly. ADR-007. |
| Engine-agnostic layers are Selenium-free | engine | `DriverContext`, `WebDriver`, `By` must not appear in `core.runtime`, `core.interactions`, or `dsl` except in `@Deprecated` bridge paths. ADR-018. |
| `LocatorDescriptor` is Selenium-free | engine | No `org.openqa.selenium.By` dependency. ADR-019. |
| `ElementSupport` scope is frozen | scope | Exactly three methods: `nameOf`, `declaringClassOf`, `ordinalOf`. No additions without an ADR. ADR-017. |
| `Target` carries no enum-specific defaults | scope | `core.target.Target` must not assume enum implementors. |
| `VOIDBuilder` is single-use | scope | Each session requires a new `VOID.builder()` call. ADR-018. |
| Kernel purity: the kernel depends only on JDK, `core.logging`, `core.annotations`, `core.target`, itself, and a short, explicitly documented list of temporary exceptions | domain | `core.actions`, `core.actions.trace`, `core.actions.hooks`, `core.flow`, `core.executor`, `core.context` (minus the legacy `ExecutionContext`), `core.runtime`, `core.bootstrap` must never depend on `elements.*` or Selenium. Consolidates I2.1-I2.3; enforced by `KernelBoundaryRulesTest.kernelPurity`, with every temporary exception named and cross-referenced to its closing phase. ADR-021, runtime-redesign I2.4. |

---

## Architectural Stability Rules

Before introducing a new interface, abstraction, factory, registry, or lifecycle object:

1. Does an equivalent abstraction already exist?
2. Can the current abstraction be extended instead of replaced?
3. Does this reduce coupling, or merely relocate it?
4. Will this still make sense after a second engine is added?
5. Is this an implementation detail or a permanent architectural concept?

If (1) or (2) is yes, do not introduce it. If (3) is "relocates only", reconsider. If (4)
is unclear, the abstraction is premature.

Architecture should emerge from repeated requirements, not anticipated ones. Never optimize
for hypothetical future requirements unless the active initiative explicitly requires it.

---

## Coding Conventions

- Java 17. Records, sealed classes, pattern matching where appropriate.
- No Lombok, no compile-time code generation.
- No wildcard imports except `static` from `CustomLogger` and `AnsiColors`.
- Logging: `CustomLogger` only. Never `System.out.println`.
- Test naming: `methodUnderTest_scenario_expectedOutcome`.
- Unit tests do not open a browser. Use reflection for private state.
- Static utility classes must have a `private` constructor.

Full details: `docs/contributing/coding-standards.md`.

---

## OOP Principles

Violations are tracked as P-IDs. Reference the ID when naming or logging a violation.

| ID | Principle | Summary | Status |
|---|---|---|---|
| P1 | OCP, DIP | `instanceof HookChainAction` in 4 `Action` default methods | Fixed |
| P2 | OCP | Sequential `instanceof` chains in `VoidDSL` dispatch | Fixed |
| P3 | OCP | `switch (ActionCapability)` in `HookChainAction.operationLabel` | Fixed |
| P4 | LSP, DIP | `instanceof ActionLabeled` fallback in `HookChainAction` | Fixed |
| P5 | LSP | `(Enum<?>) this` hard cast in `UIElement` interface defaults | Fixed |
| P6 | LSP | Duplicated `instanceof Enum<?>` in `ElementAction` + `LocatorResolver` | Fixed |
| P7 | ISP, OCP | `instanceof ActionCapabilityProvider` in `ElementActions.capabilityFor` | Fixed |
| P8 | OCP | `switch` on engine name string in `UIEngineFactory` | Fixed -- runtime-redesign I4.1 |
| P9 | OCP | O(n) dedup in `SearchableDropdown`/`SearchField.getAllLocatorRoles` | Fixed |
| P10 | ISP | Forced abstract `getIndex()` in `Listable` with no default | Fixed |
| P11 | OCP | Per-capability static helpers in `Via` growing with capability count | Deferred -- runtime-redesign I9.3 |

Full SOLID reference with code examples: `docs/architecture/oop-principles.md`.

### Violation protocol

Name it (P-ID or description). Then:

| Fix cost | Action |
|---|---|
| Minimal -- few lines, no ripple | Fix inline. Dedicated commit. Record in phase doc under "Incidental fixes". |
| Dedicated -- new classes or interface changes | Log in `docs/audits/backlog/violations/`. Update the index. Do not fix in the current initiative. |

Never introduce a new `instanceof` dispatch chain, `switch`-on-string selector, or
unguarded `(Enum<?>) this` cast without fixing it or logging it.

Full protocol and file format: `docs/contributing/architecture-rules.md`.

---

## Token Efficiency

- Prefer architectural reasoning over verbose narration.
- Do not restate the user's request before acting on it.
- Do not explain what obvious code does.
- Do not narrate implementation progress step by step -- report results.
- Reference existing documentation instead of reproducing it inline.
- When editing files, describe only changed sections unless asked otherwise.
- During audits, report findings only -- do not describe code that has no issues.
- Ask one concise clarification instead of listing multiple assumptions.
- Do not repeat previous phase summaries when continuing an initiative.
- Preserve context budget on long initiatives: read only the docs relevant to the current phase.
- Do not regenerate plans or documentation that already exist. Reference them instead.

---

## Further Reading

| Doc | Contents |
|---|---|
| `docs/contributing/workflow.md` | Initiative lifecycle, audit stages, merge policy, reading docs guide, docs/ directory guide |
| `docs/contributing/coding-standards.md` | Java conventions, logging, testing, naming |
| `docs/contributing/architecture-rules.md` | Architecture invariants, stability rules, violation protocol and file format |
| `docs/architecture/oop-principles.md` | Full SOLID reference with P1-P11 code examples and planned fixes |
| `docs/decisions/accepted/README.md` | Index of all accepted ADRs |
| `docs/plan/draft/README.md` | Active initiatives |
| `docs/audits/ongoing/` | Open findings -- check before touching governed areas |

---

## Memory

Project memory is stored in the project's Claude memory directory (environment-specific
path). Check `MEMORY.md` in that directory for the index of saved context before relying
on information from previous sessions.
