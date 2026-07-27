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

## Architecture Fitness Checks (the Ratchet)

Kernel boundary rules are enforced automatically by `KernelBoundaryRulesTest` (and the
existing `FacadeBoundaryRulesTest`, `ElementStructureRulesTest`) in `src/test/java/core/architecture/`.
These run as part of `mvn test`. A check that fails means a boundary regression -- do not
suppress it, fix the regression.

### How to tighten the ratchet when a phase wins a boundary

When an initiative phase cleans a new package of a forbidden dependency:

1. Add a rule to `KernelBoundaryRulesTest` (or a new `*RulesTest` class if the boundary
   is conceptually distinct) that encodes the newly-won boundary.
2. Verify the rule FAILS when the forbidden import is temporarily added back. Commit the
   mutation test evidence in the phase document under "Validation".
3. Verify the rule PASSES on the clean state. Commit the new rule as part of the phase commit.
4. Update the relevant ADR's Consequences section to reference the new check.

The ratchet never loosens. Once a boundary is encoded, it is a non-negotiable invariant
for all future work.

### Current ratchet baseline (Phase 0.2, ADR-021; consolidated I2.4)

| Check class | Boundary encoded |
|---|---|
| `FacadeBoundaryRulesTest` | Test classes must not hold UIEngine fields or construct FlowExecutor directly |
| `ElementStructureRulesTest` | UIElement enums must be nested inside a page class |
| `KernelBoundaryRulesTest` | core.logging, core.flow, core.actions, elements.* are Selenium-free; core.runtime holds no WebDriver/DriverContext fields; LocatorDescriptor has no Selenium dependency; core.actions/.trace/.hooks/core.flow/core.executor/core.context/core.runtime have no UIElement/ElementRole/capability/elements.* dependency (I1.4, I2.2, I2.3); `core.actions.hooks` and core.actions (excluding ActionProfiles/Profiles) have no core.interactions dependency (I2.1, I2.2); **`kernelPurity`** (I2.4) consolidates all of the above into one named, positive-allowlist boundary: the kernel may depend only on JDK/javax, core.logging, core.annotations, core.target, itself, and a short list of documented temporary exceptions (each cross-referenced to its closing phase in the test's own javadoc) |

#### Mutation demo evidence (I2.4, `kernelPurity`)

Recorded per this section's own protocol ("verify the rule FAILS when the forbidden
import is temporarily added back"). Procedure: added a temporary
`default DriverContext mutationDemoOnly() { return null; }` method to
`core.actions.Action` (a disallowed dependency -- `core.driver.DriverContext` is not in
the kernel purity allowlist or its temporary-exceptions list). Ran
`mvn test -Dtest=KernelBoundaryRulesTest#kernelPurity`:

```
Architecture Violation [Priority: MEDIUM] - Rule 'classes that reside in any package
[...] should only depend on classes that reside in any package [...]' was violated (1 times):
Method <core.actions.Action.mutationDemoOnly()> has return type <core.driver.DriverContext>
in (Action.java:0)
```

The rule failed precisely, naming the offending method and type. The mutation was then
reverted (`git diff` confirmed zero remaining changes to `Action.java`) and the full
suite re-run green (1191 tests, 0 failures) before the phase commit.

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
