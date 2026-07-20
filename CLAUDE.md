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

## OOP Principles

Apply OOP principles in all production code. These are not style preferences -- violations
are tracked as architectural debt in `docs/plan/draft/oop-violations-remediation/index.md`
and addressed through a dedicated remediation initiative.

The violation map uses IDs P1-P11. When a violation is referenced, use that ID.

---

### Open/Closed Principle (highest priority)

Classes and interfaces must be **open for extension, closed for modification**. Adding a new
engine, capability, or action type should require adding a new class -- not editing an
existing one. A method that must be changed every time a new subtype is added is closed for
extension.

#### Violation: instanceof dispatch chain (P1, P2)

`Action.java` checks the runtime type of `this` to decide behavior:

```java
// core/actions/Action.java -- P1 violation (4 sites)
default Action before(@Nullable BeforeActionHandler... hooks) {
    if (this instanceof HookChainAction chain) {          // type check on self
        return chain.withAdditionalHooks(toList(hooks), null);
    }
    return new HookChainAction(this, toList(hooks), null);
}

default Action withProfile(ActionProfile profile) {
    Action profiled = profile.apply(this);
    if (profiled instanceof HookChainAction chain) {      // type check again
        profiled = chain.withProfileName(profile.name());
    }
    return profiled;
}
```

`VoidDSL.java` branches on the runtime type of a method argument:

```java
// dsl/VoidDSL.java -- P2 violation
if (first instanceof MultiSelectable multiDropdown) {
    engine.triggerDropdown(multiDropdown, dropdownIndex);
} else if (first instanceof Selectable singleDropdown) {
    engine.triggerDropdown(singleDropdown);
} else {
    throw new IllegalArgumentException(...);
}
```

Both are OCP violations: adding a new type requires modifying existing methods.

**The fix:** Put the behavior on the type. `HookChainAction` should expose an extension hook
method (`mergeHooks`) that `Action.before()` calls directly, with no type check needed.
`VoidDSL` should call `element.triggerAction()` and let each capability interface dispatch.

#### Violation: switch-on-string type selector (P8)

`UIEngineFactory.java` selects an engine implementation by comparing a string:

```java
// core/engine/UIEngineFactory.java -- P8 violation
UIEngine engine = switch (engineName) {
    case "selenium" -> {
        if (bootstrap instanceof EngineBootstrap.FromProfile fp) {
            yield new SeleniumEngine(fp.profile());
        } else {
            throw new IllegalStateException(...);
        }
    }
    // Adding "playwright" requires editing this method
    default -> throw new IllegalStateException(
            "Unsupported engine: '" + engineName + "'. Supported: selenium");
};
```

Every new engine requires modifying this switch. The factory is closed.

**The fix:** An open registration map -- `Map<String, Function<EngineBootstrap, UIEngine>>`.
Registering a new engine adds an entry without modifying the factory body.

#### Violation: switch on enum value for label (P3)

`HookChainAction.java` derives an operation label by switching on `ActionCapability`:

```java
// core/actions/HookChainAction.java -- P3 violation
@Override
public String operationLabel() {
    if (delegate instanceof ActionLabeled l) return l.operationLabel();
    return switch (capability()) {
        case CLICKABLE  -> "click";
        case TYPEABLE   -> "type";
        case SELECTABLE -> "select";
        default         -> "perform";
    };
}
```

Adding a new `ActionCapability` value requires editing this method.

**The fix:** `operationLabel()` should be a default method on `Action` itself, or each
capability interface should provide it. The switch disappears.

#### Correct pattern: capability interfaces

`Clickable.java` uses no type checks. Behavior is declared directly on the interface:

```java
// elements/api/capability/Clickable.java -- correct OCP pattern
public interface Clickable extends Element, ActionCapabilityProvider {
    default ActionCapability capability() { return ActionCapability.CLICKABLE; }
    default ClickAction click() { return new ClickAction(this); }
    // adding a new capability = adding a new interface, no existing code changes
}
```

---

### Liskov Substitution Principle

Subtypes must be substitutable for their base type without changing program correctness.
Do not override a method in a way that narrows the contract, throws where the base does not,
or relies on a runtime cast that only works for specific implementors.

#### Violation: unguarded (Enum<?>) this cast (P5)

`Element.java` default methods cast `this` to `Enum<?>` without verifying the type:

```java
// elements/api/Element.java -- P5 violation (4 sites)
default String getExternalFileName() {
    Enum<?> e = (Enum<?>) this;            // throws ClassCastException for any non-enum Element
    Class<?> enumClass = e.getDeclaringClass();
    ...
}

default String getPrimaryLocator() {
    ...
    Enum<?> e = (Enum<?>) this;            // same assumption, no guard
    Class<?> enumClass = e.getDeclaringClass();
    ...
}
```

A non-enum implementor of `Element` would throw `ClassCastException` at runtime when any
default method is called. The interface cannot be safely subtyped.

**The fix:** `ElementSupport` (planned, not yet written) centralises the cast with a clear
scope: `ElementSupport.nameOf(e)`, `ElementSupport.declaringClassOf(e)`. The cast moves to
one place and is accompanied by documentation of the invariant it assumes.

#### Violation: instanceof ActionLabeled fallback (P4)

`HookChainAction.java` checks whether its delegate satisfies a secondary interface before
calling a method:

```java
// core/actions/HookChainAction.java -- P4 violation
@Override
public String elementLabel() {
    if (delegate instanceof ActionLabeled l) return l.elementLabel();
    return "ACTION";    // silent fallback for implementors that don't satisfy ActionLabeled
}
```

This means `HookChainAction` behaves differently based on the runtime type of `delegate`.
Callers cannot predict what `elementLabel()` returns without knowing the concrete type.

**The fix:** Promote `elementLabel()` and `operationLabel()` to `Action` with defaults.
Every `Action` implementor provides its own label; no secondary interface or runtime check
is needed.

---

### Interface Segregation Principle

A class that implements an interface should use every method it declares. If an implementor
leaves methods empty or the only way to satisfy an interface is a runtime cast, the interface
is too broad.

#### Correct pattern: narrow interfaces

`ActionLabeled` is a package-private interface with exactly two methods, both used by every
implementor:

```java
// core/actions/ActionLabeled.java -- correct ISP (narrow, focused)
interface ActionLabeled {
    String elementLabel();    // used by every implementor
    String operationLabel();  // used by every implementor
}
```

Each capability interface (`Clickable`, `Typeable`, `Selectable`) is similarly narrow: it
declares only the methods relevant to that one capability.

#### Violation: Via grows per capability (P11)

`Via.java` has a static cast helper for every capability interface -- currently eight:

```java
// core/interactions/Via.java -- P11 violation (grows with every new capability)
public static Clickable          clickable(Element e)          { ... }
public static Typeable           typeable(Element e)           { ... }
public static Selectable         selectable(Element e)         { ... }
public static ReadOnly           readOnly(Element e)           { ... }
public static Searchable         searchable(Element e)         { ... }
public static SearchableDropdown searchableDropdown(Element e) { ... }
public static MultiSelectable    multiSelectable(Element e)    { ... }
public static Checkable          checkable(Element e)          { ... }
public static Hoverable          hoverable(Element e)          { ... }
```

Adding a new capability interface requires adding another method here. The class is not
closed for modification.

**The fix:** One generic cast helper:
`public static <T extends Element> T as(Element e, Class<T> type)`. Existing call sites
migrate once; future capabilities add nothing to `Via`.

---

### Single Responsibility Principle

Each class has one reason to change. A class that both manages driver lifecycle and
configures logging changes for two unrelated reasons.

#### Example: FrameworkBootstrap before Phase 4

Before engine decoupling Phase 4, `FrameworkBootstrap.java` suppressed the Selenium JUL
logger unconditionally:

```java
// FrameworkBootstrap.java (pre-decoupling) -- SRP violation
public static void init() {
    configureLogging();
    suppressSeleniumLogger();   // Selenium-specific; belongs in SeleniumEngine.initialize()
}
```

`FrameworkBootstrap` is engine-agnostic bootstrap. Suppressing a Selenium-specific logger
was a second responsibility that changed whenever the engine changed.

**The fix (Phase 4):** `suppressSeleniumLogger()` moved into `SeleniumEngine.initialize()`.
`FrameworkBootstrap.init()` now contains only framework-level initialization.

---

### Dependency Inversion Principle

High-level modules must not depend on low-level modules. Both should depend on abstractions.

#### Correct pattern: VOID depends on UIEngine, not SeleniumEngine

```java
// core/runtime/VOID.java -- correct DIP
private final UIEngine engine;         // depends on the abstraction
private final SessionContext context;  // engine-typed context, not WebDriver-typed

public void shutdown() {
    engine.shutdown();                 // delegates; no knowledge of SeleniumEngine
}
```

#### Violation: Interactions depended on WebDriver (fixed in Phase 3)

Before engine decoupling Phase 3, `Interactions(UIEngine)` cast the native driver to a
Selenium-specific type:

```java
// core/interactions/Interactions.java (pre-Phase-3) -- DIP violation
public Interactions(UIEngine engine) {
    this.engine = engine;
    // High-level interaction layer depending on Selenium WebDriver directly:
    DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver());
}
```

`Interactions` is a high-level interaction orchestrator. It should not depend on the
Selenium `WebDriver` type. The cast was removed in Phase 3 -- `SeleniumEngine.initialize()`
now registers the driver itself, and `Interactions` has no knowledge of WebDriver.

---

### When you encounter a violation

Do not silently work around a violation. Apply this decision tree:

**Step 1 -- Identify it.**
Name the violation explicitly: which principle, which file and line, what the runtime risk
is. If it is already tracked in `docs/plan/draft/oop-violations-remediation/index.md`,
reference the P-ID.

**Step 2 -- Assess the fix cost.**
Estimate whether the fix is minimal (a few lines, no ripple effect, no new abstractions
needed) or dedicated (requires new classes, interface changes, or cross-file edits that
belong in a planned phase).

**Step 3 -- Decide.**

| Fix cost | Action |
|---|---|
| Minimal -- cheaper to fix now than to carry the debt | Fix it inline in the current work. Commit it as a separate, clearly labelled commit. |
| Dedicated -- more than a few lines, or touches interfaces shared across the codebase | Log it in `docs/audits/backlog/violations/`. Do not fix it in the current initiative. |

**Step 4 -- If fixing inline:**
- Make it a dedicated commit with a clear message, e.g.
  `fix(elements): remove instanceof ActionLabeled fallback; use Action.elementLabel() default`
- If the current work is part of an initiative, update the initiative's phase doc
  (`docs/plan/draft/<initiative>/phase-N-*.md`) to record the fix under a "Incidental fixes"
  section. This keeps the phase doc accurate and prevents the change from being invisible
  during the final audit.

**Step 5 -- If logging in backlog:**
- Create a file in `docs/audits/backlog/violations/` using the format below.
- Update the index table in `docs/audits/backlog/violations/README.md`.
- Never introduce a new `instanceof` dispatch chain, `switch`-on-string type selector, or
  unguarded `(Enum<?>) this` cast without either fixing it or logging it -- no exceptions.

### Violation file format

New violation files in `docs/audits/backlog/violations/` follow this structure:

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
