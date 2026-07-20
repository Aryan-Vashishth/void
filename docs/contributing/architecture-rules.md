# Architecture Rules

## Architecture Invariants

These are non-negotiable. They reflect decisions that have been through full initiative
cycles and ADR review. Do not work around them. If a task seems to require violating one,
stop and raise it explicitly.

| Invariant | Rule |
|---|---|
| `UIEngine` is the single execution authority | No code outside `UIEngine` implementations should call `WebDriver` methods directly. ADR-007. |
| Engine-agnostic layers must not import Selenium | `DriverContext`, `WebDriver`, `By` must not appear in `core.runtime`, `core.interactions`, or `dsl` except in `@Deprecated` bridge paths. ADR-018. |
| `LocatorDescriptor` is Selenium-free | Must not depend on `org.openqa.selenium.By`. ADR-019. |
| `ElementSupport` scope is frozen | Package-private in `elements.api`. Exactly three methods: `nameOf`, `declaringClassOf`, `ordinalOf`. Do not add to it without an ADR. ADR-017. |
| `Target` carries no enum-specific defaults | `Target` (in `core.target`) must not carry default methods that assume enum implementors. |
| `VOIDBuilder` is single-use | Each session requires a new `VOID.builder()` call. Calling `start()` twice throws. ADR-018. |
| `SessionContext` not `ExecutionContext` | `VOID` holds a `SessionContext`. `ExecutionContext` is deprecated. ADR-018. |

---

## Architectural Stability Rules

Before introducing a new interface, abstraction, generic type, factory, registry, or
lifecycle object, answer these questions:

1. Does an equivalent abstraction already exist?
2. Can the current abstraction be extended rather than a new one introduced?
3. Does this reduce coupling, or merely relocate it?
4. Will this still make sense after a second engine implementation is added?
5. Is this an implementation detail or a permanent architectural concept?

If the answer to question 1 or 2 is "yes", do not introduce the new abstraction. If the
answer to question 3 is "merely relocates", reconsider. If the answer to question 4 is
"unclear", the abstraction is premature.

**Architecture should emerge from repeated requirements, not anticipated ones.** Avoid
speculative abstractions. Three concrete cases that need the same behavior justify an
abstraction; one hypothetical future case does not.

---

## OOP Violation Protocol

When work surfaces an OOP violation, apply this decision tree. Do not silently work around
it.

**Step 1 -- Identify.**
Name the principle, file, line, and runtime risk. If tracked in
`docs/plan/draft/oop-violations-remediation/index.md`, use the P-ID.

**Step 2 -- Assess fix cost.**

| Fix cost | Criteria |
|---|---|
| Minimal | A few lines; no new classes; no ripple across interfaces |
| Dedicated | New classes, interface changes, or cross-file edits |

**Step 3 -- Decide.**

| Fix cost | Action |
|---|---|
| Minimal -- cheaper now | Fix inline. Dedicated commit. If inside an initiative, record it in the phase doc under "Incidental fixes". |
| Dedicated -- costly now | Log in `docs/audits/backlog/violations/`. Update the index. Do not fix it in the current initiative. |

**Never** introduce a new `instanceof` dispatch chain, `switch`-on-string type selector, or
unguarded `(Enum<?>) this` cast without either fixing it or logging it.

---

## Violation File Format

Files in `docs/audits/backlog/violations/` use this structure:

```
# <short title>

**Principle:** OCP / LSP / ISP / SRP / DIP
**File:** src/main/java/...
**Discovered:** <date>
**Risk:** Critical / High / Medium / Low

## What it does

One paragraph describing the code and why it is a violation.

## Code

<code snippet showing the violation>

## Recommended fix

One paragraph describing the correct approach.
```

Update `docs/audits/backlog/violations/README.md` index after adding any file.

---

## Full OOP Reference

For full principle descriptions, all tracked violations (P1-P11), code examples, and
planned fixes: `docs/architecture/oop-principles.md`.
