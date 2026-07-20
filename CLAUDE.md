# VOID Framework -- Claude Instructions

Project-level instructions for Claude Code. These override default behavior.

## Core Principles

- Architecture before implementation.
- Documentation is the source of truth.
- Prefer extension over modification.
- Preserve engine neutrality.
- Keep abstractions minimal and intentional.

---

## Instruction Precedence

When guidance conflicts, follow this order:

1. Current initiative plan (`docs/plan/draft/<initiative>/`)
2. Accepted ADRs -- and pending-review ADRs on the current initiative branch
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

## Branch Naming

| Branch | When |
|---|---|
| `initiative/<name>` | Multi-phase architectural work |
| `hotfix/<name>` | Scoped corrections from a final audit |
| `bugfix/<name>` | Isolated bug fixes |
| `docs/<name>` | Documentation-only changes |

Use `initiative/` not `feature/`.

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

## Architecture Invariants

Non-negotiable. Raise a violation explicitly before working around one.

| Invariant | Rule |
|---|---|
| `UIEngine` is the single execution authority | Nothing outside `UIEngine` implementations calls `WebDriver` methods directly. ADR-007. |
| Engine-agnostic layers are Selenium-free | `DriverContext`, `WebDriver`, `By` must not appear in `core.runtime`, `core.interactions`, or `dsl` except in `@Deprecated` bridge paths. ADR-018. |
| `LocatorDescriptor` is Selenium-free | No `org.openqa.selenium.By` dependency. ADR-019. |
| `ElementSupport` scope is frozen | Exactly three methods: `nameOf`, `declaringClassOf`, `ordinalOf`. No additions without an ADR. ADR-017. |
| `Target` carries no enum-specific defaults | `core.target.Target` must not assume enum implementors. |
| `VOIDBuilder` is single-use | Each session requires a new `VOID.builder()` call. ADR-018. |

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

Architecture should emerge from repeated requirements, not anticipated ones.

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

| ID | Principle | Summary |
|---|---|---|
| P1 | OCP, DIP | `instanceof HookChainAction` in 4 `Action` default methods |
| P2 | OCP | Sequential `instanceof` chains in `VoidDSL` dispatch |
| P3 | OCP | `switch (ActionCapability)` in `HookChainAction.operationLabel` |
| P4 | LSP, DIP | `instanceof ActionLabeled` fallback in `HookChainAction` |
| P5 | LSP | `(Enum<?>) this` hard cast in `Element` interface defaults |
| P6 | LSP | Duplicated `instanceof Enum<?>` in `ElementAction` + `LocatorResolver` |
| P7 | ISP, OCP | `instanceof ActionCapabilityProvider` in `ElementActions.capabilityFor` |
| P8 | OCP | `switch` on engine name string in `UIEngineFactory` |
| P9 | OCP | O(n) dedup in `SearchableDropdown`/`SearchField.getAllLocatorRoles` |
| P10 | ISP | Forced abstract `getIndex()` in `Listable` with no default |
| P11 | OCP | Per-capability static helpers in `Via` growing with capability count |

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
