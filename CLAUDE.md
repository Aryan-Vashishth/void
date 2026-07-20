# VOID Framework -- Claude Instructions

Project-level instructions for Claude Code. These override default behavior.

Before starting any task, read the relevant docs. Code without its architectural context is
incomplete information.

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

Non-negotiable. Do not work around these. If a task seems to require violating one, raise
it explicitly before proceeding.

| Invariant | Rule |
|---|---|
| `UIEngine` is the single execution authority | Nothing outside `UIEngine` implementations calls `WebDriver` methods directly. ADR-007. |
| Engine-agnostic layers are Selenium-free | `DriverContext`, `WebDriver`, `By` must not appear in `core.runtime`, `core.interactions`, or `dsl` except in `@Deprecated` bridge paths. ADR-018. |
| `LocatorDescriptor` is Selenium-free | No `org.openqa.selenium.By` dependency. ADR-019. |
| `ElementSupport` scope is frozen | Exactly three methods: `nameOf`, `declaringClassOf`, `ordinalOf`. No additions without an ADR. ADR-017. |
| `Target` carries no enum-specific defaults | `core.target.Target` must not assume enum implementors. |
| `VOIDBuilder` is single-use | Each session requires a new `VOID.builder()` call. ADR-018. |

---

## Coding Conventions

- Java 17. Use records, sealed classes, pattern matching where appropriate.
- No Lombok, no compile-time code generation.
- No wildcard imports (except `static` from `CustomLogger` and `AnsiColors`).
- Logging: `CustomLogger` only. Never `System.out.println`.
- Test naming: `methodUnderTest_scenario_expectedOutcome`.
- Unit tests do not open a browser. Use reflection for private state.
- Static utility classes must have a `private` constructor.

Full details: `docs/contributing/coding-standards.md`.

---

## Architectural Stability Rules

Before introducing a new interface, abstraction, factory, registry, or lifecycle object:

1. Does an equivalent abstraction already exist?
2. Can the current abstraction be extended instead of replaced?
3. Does this reduce coupling, or merely relocate it?
4. Will this still make sense after a second engine is added?
5. Is this an implementation detail or a permanent architectural concept?

If the answer to (1) or (2) is yes, do not introduce the new abstraction. If (3) is
"relocates only", reconsider. If (4) is unclear, the abstraction is premature.

Architecture should emerge from repeated requirements, not anticipated ones.

---

## OOP Principles

Violations are tracked as P-IDs. When referencing or logging a violation, use the ID.

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

### When you encounter a violation

1. Name it (P-ID if tracked, or describe if new).
2. Assess fix cost: minimal (few lines, no ripple) or dedicated (new classes, interface changes).
3. **Minimal** -- fix inline, dedicated commit; record in the phase doc under "Incidental fixes".
4. **Dedicated** -- log in `docs/audits/backlog/violations/`, update the index. Do not fix in the current initiative.
5. Never introduce a new `instanceof` dispatch chain, `switch`-on-string type selector, or unguarded `(Enum<?>) this` cast without either fixing it or logging it.

Violation file format and full protocol: `docs/contributing/architecture-rules.md`.

---

## Further Reading

| Doc | Contents |
|---|---|
| `docs/contributing/workflow.md` | Full initiative lifecycle, audit stages, merge policy, reading docs guide, docs/ directory guide |
| `docs/contributing/coding-standards.md` | Java conventions, logging, testing, naming |
| `docs/contributing/architecture-rules.md` | Architecture invariants, stability rules, violation protocol and file format |
| `docs/architecture/oop-principles.md` | Full SOLID reference with P1-P11 code examples and planned fixes |
| `docs/decisions/accepted/README.md` | Index of all accepted ADRs |
| `docs/plan/draft/README.md` | Active initiatives |
| `docs/audits/ongoing/` | Open findings -- check before touching governed areas |

---

## Memory

Project memory: `C:\Users\AryanVashishth\.claude\projects\D--void-framework\memory\`

Check `MEMORY.md` in that directory for the index of saved context.
