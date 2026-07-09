# Element API Simplification & Boilerplate Reduction

## Overview

> **For the overall architecture, design principles, terminology, and usage of VOID, refer to `README.md`.**

This phase improves the Element API and the surrounding developer workflow by establishing a principle that reaches beyond reducing boilerplate:

> **Developer-authored code should remain the single source of truth.**
>
> Whenever VOID or its tooling can deterministically derive runtime artifacts from that source, those artifacts should be generated rather than manually maintained.

> **Developers should never manually maintain runtime artifacts that can be deterministically regenerated.**

This applies to every redundant piece of information in the current API: locator keys, display text, argument defaults, properties templates, JSON repositories, and repository locations.

Developers should author **intent** — page structure, capability groupings, locator values.

VOID should generate **everything else**.

---

# Non-Goals

This phase does **not** aim to:

- Change the overall architecture of VOID.
- Modify the execution engine.
- Change Flow or Action APIs.
- Replace capability interfaces.
- Remove support for hardcoded locators.
- Introduce a new JSON generation tool (the existing CLI already serves this role).

The focus of this phase is to simplify the Element API and developer workflow while preserving the existing programming model and all advanced escape hatches.

---

# Before and After

The following comparison shows the cumulative effect of all changes in this phase.

It is placed here so the reader can evaluate any individual improvement against the concrete outcome.

### Before

```java
public interface DemoLoginPage {

    String LOCATOR_FILE = "demo-login-elements.json";

    enum Credentials implements Typeable {

        USERNAME_INPUT("USERNAME_INPUT"),
        PASSWORD_INPUT("PASSWORD_INPUT");

        private final String key;
        Credentials(String k) { this.key = k; }

        @Override public String getInputLocator()     { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    enum Button implements Clickable {

        LOGIN_BUTTON("LOGIN_BUTTON", "Login");

        private final String key;
        private final String label;
        Button(String k, String l) { this.key = k; this.label = l; }

        @Override public String getTriggerLocator()   { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[]{label}; }
        @Override public String getDisplayText()      { return label; }
    }
}
```

Paired with a manually maintained locator file at a path that each enum must declare explicitly.

### After

```java
public interface DemoLoginPage {

    enum Credentials implements Typeable {
        USERNAME_INPUT,
        PASSWORD_INPUT
    }

    enum Button implements Clickable {
        LOGIN_BUTTON
    }
}
```

Paired with a locator file at a deterministic location VOID discovers automatically.

No duplicated keys. No constructors. No locator filenames. No page annotations. No runtime plumbing.

The nested enum structure, capability interfaces, and compile-time discoverability are fully preserved.

---

# Motivation

Over time, the Element API has accumulated repetitive implementation code that does not describe the UI — it satisfies runtime contracts that VOID could satisfy itself.

As applications grow, this repetition scales proportionally. A page containing 60–100 elements may require hundreds of lines of infrastructure code nearly identical across every project.

The deeper problem is not just volume. It is that developers are forced to manually maintain artifacts the runtime could generate deterministically.

Locator keys follow directly from enum constant names. Display text follows directly from those names. Repository locations follow directly from page names. Properties templates follow directly from enum declarations.

None of these require human judgment. All of them create human error.

> The purpose of automation is not to shift repetitive work to developers, but to eliminate it entirely.

This phase moves that responsibility into the runtime and its tooling.

---

# Generated, Not Maintained

This is the core principle of the proposal. Every individual change described in this document is a consequence of it.

> **Developers should never manually maintain runtime artifacts that can be deterministically regenerated.**

The following table makes the boundary explicit.

| What developers author               | What VOID derives or generates                        |
|--------------------------------------|-------------------------------------------------------|
| Page interface                       | Repository location (from locator repository convention) |
| Capability enum declarations         | Properties template (all keys pre-filled)             |
| Locator values in properties file    | JSON repository (via Runtime Repository Generation)   |
| Dynamic args when genuinely needed   | Locator key from enum constant name                   |
| Custom display text when needed      | Display text from enum constant name                  |
| Hardcoded locators when needed       | Default empty args                                    |

Developers should never manually maintain anything in the right column.

The only artifact that genuinely requires human judgment is the locator value itself — the XPath, CSS selector, or ID that maps to an element in the real UI.

Everything else is mechanical repetition that tooling should perform.

---

# Design Philosophy

The refactoring follows these principles:

- **Single source of truth.** Developer-authored code is the authoritative source. Generated artifacts derive from it, not the reverse.
- **Generated artifacts over manually maintained artifacts.** If VOID can deterministically produce something, it should.
- **Convention over repetition.** A consistent repository convention eliminates the need to configure what can be inferred.
- **Deterministic structure.** Repository locations follow from page names. No path configuration is required in the common case.
- **Runtime intelligence over developer boilerplate.** Defaults handle the common case. Overrides handle exceptions.
- **Strong compile-time guarantees.** No runtime discovery of elements. No string-typed identifiers at call sites.
- **Preserve escape hatches for advanced scenarios.** Every convention can be overridden. No capability is removed.

---

# Current Pain Points

## 1. Duplicate Locator Keys

```java
USERNAME_INPUT("USERNAME_INPUT"),
PASSWORD_INPUT("PASSWORD_INPUT");
```

The enum constant already uniquely identifies the locator key. The string argument is pure duplication.

Maintaining it introduces rename risks, spelling mistakes, and casing inconsistencies without providing any additional information.

---

## 2. Repeated Empty Arguments

Most elements require no locator arguments. Yet nearly every enum implements:

```java
@Override
public Object[] getArgs() {
    return new Object[0];
}
```

This implementation is identical throughout every project.

---

## 3. Repeated Locator File Declarations

Every enum repeats:

```java
@Override
public String getExternalFileName() {
    return LOCATOR_FILE;
}
```

Even though every element on the same page shares the same repository. This belongs to the page, not to each element individually.

---

## 4. Constructors Without Meaning

Many constructors exist only to store duplicated locator keys and carry no meaningful runtime state.

---

## 5. Manually Maintained Repository Locations

Developers currently declare locator file paths as string constants:

```java
String LOCATOR_FILE = "demo-login-elements.json";
```

This path is a derived artifact. It follows from the page name. The runtime could locate it automatically.

---

## 6. Manually Maintained Locator Keys in Properties Files

Properties files are currently populated entirely by hand. Every key must be typed correctly to match the enum constant it represents.

A typo in a key produces a runtime resolution failure, not a compile-time error.

Since the keys derive deterministically from enum declarations, they should be generated, not typed.

---

## 7. Capability Interfaces Contain Runtime Plumbing

Several capability interfaces contain forwarding implementations that delegate to parent interfaces without adding capability-specific behavior. These increase maintenance cost and make capability interfaces harder to read.

---

## 8. Repeated Display Text Implementations

Many elements override display text even though a sensible value can be derived from the enum constant name.

```
LOGIN_BUTTON  →  Login Button
```

---

# Deterministic Locator Repository Convention

Rather than requiring developers to declare locator file paths, VOID adopts a convention for where each page's repository lives within the standard Maven source layout.

Java source files remain in `src/main/java`. Locator resources live in `src/main/resources` under a mirrored structure.

```
src/main/java/pages/
    DemoLoginPage.java

src/main/resources/pages/
    DemoLoginPage/
        locators.properties
        locators.json
```

The runtime derives the resource path from the page class name:

```
DemoLoginPage  →  pages/DemoLoginPage/locators.json
```

No path configuration is required. No `LOCATOR_FILE` constant. No annotation. No `getExternalFileName()` override for the common case.

`getExternalFileName()` is preserved as an advanced override for pages with shared repositories, generated repositories, or custom locations. See Part 8.

Benefits:

- Zero path configuration in common usage.
- Maven-compatible build layout — Java and resources stay in their respective source roots.
- Consistent navigation — every page follows the same structure.
- Easier onboarding — the layout is self-explanatory.
- Reliable IDE navigation — all page assets co-locate predictably.
- Deterministic structure improves both human and tool-assisted development.

---

# Repository Abstraction

Properties and JSON are both representations of the same underlying data. Neither is the abstraction — `LocatorRepository` is.

```
Developer edits
      │
      ▼
PropertiesRepository
      │
      ▼
Runtime Repository Generation (CLI)
      │
      ▼
JsonRepository
      │
      ▼
LocatorRepository  ◄──  runtime reads only this interface
      │
      ▼
Runtime
```

This means:

- Properties and JSON are implementation details beneath the repository interface.
- The runtime is not coupled to either format.
- Future repository types (YAML, remote, database) slot in at the `LocatorRepository` level without touching the runtime.
- The cache stores a resolved `LocatorRepository`, making it format-agnostic.

---

# Properties and JSON — Distinct Roles

**Properties are the developer-facing representation.** They are the preferred authoring format because:

- Flat key-value structures are easy for both humans and automated tooling to generate and maintain.
- Diffs are clean and readable — one line per locator.
- There is no nesting, no syntax, no structural complexity to manage.

**JSON is the runtime representation.** It is structured and typed, which makes it well-suited for the runtime. It is intentionally not optimized for human editing.

Developers should edit properties. The runtime reads JSON. The CLI bridges them.

---

# Single Source of Truth Pipeline

Enum declarations are the authoritative source of locator identity within a page.

The complete pipeline from declaration to runtime execution:

```
Enum constants
      │
      ▼
Generated properties template
(all locator keys pre-filled by tooling)
      │
      ▼
Developer fills locator values
(XPath, CSS, ID — the only manual step)
      │
      ▼
Runtime Repository Generation (CLI)
      │
      ▼
Generated JSON repository
      │
      ▼
Runtime
```

At no point does a developer type a locator key. The enum constant name is the key. Tooling writes it. The developer provides only the locator value that no tool can infer.

---

# Developer Workflow

The intended end-to-end workflow after this phase:

```
1.  Create the page interface.
2.  Add capability enums with constants.
3.  Run the properties template generator.
4.  Open the generated template — all keys are already present.
5.  Fill in the locator values (XPath, CSS, ID, etc.).
6.  Run Runtime Repository Generation.
7.  Execute tests.
```

Steps 3 and 6 are the only tool invocations. Steps 1, 2, and 5 are the only places where developer judgment is required.

Everything between those steps is generated.

**Future direction.** Steps 3 and 6 could eventually collapse into a single command — something like `void sync-locators` — that generates missing keys, preserves existing values, and regenerates the JSON repository in one pass. This phase should keep the design open enough that this consolidation is a straightforward next step rather than a rearchitecting.

---

# Proposed Improvements

---

# Part 1 — Automatic Locator Keys

## Current

```java
USERNAME_INPUT("USERNAME_INPUT")
```

## Proposed

```java
USERNAME_INPUT
```

The runtime derives the locator key directly from the enum constant name.

```java
default String getPrimaryLocator() {
    return ((Enum<?>) this).name();
}
```

### Benefits

- Eliminates duplicate strings.
- Rename-safe — IDE renaming updates the lookup key automatically.
- Prevents locator key mismatches.
- Smaller page definitions.

---

# Part 2 — Default Empty Arguments

Provide a default implementation within `Element`:

```java
default Object[] getArgs() {
    return NO_ARGS;
}
```

Dynamic elements override as before:

```java
PRODUCT_ROW.with("Laptop")
```

### Benefits

- Eliminates repetitive implementations.
- Reduces object allocations.
- Cleaner page definitions.

---

# Part 3 — Rename `EMPTY_ARGS` to `NO_ARGS`

Replace:

```java
Object[] EMPTY_ARGS = new Object[0];
```

With:

```java
Object[] NO_ARGS = new Object[0];
```

`EMPTY_ARGS` describes state. `NO_ARGS` communicates intent.

Names should reflect what a thing means, not what it contains.

---

# Part 4 — Automatic Display Text

Provide a default implementation that derives a human-readable label from the enum constant name.

Transformation rules:

```
USERNAME_INPUT  →  Username Input
LOGIN_BUTTON    →  Login Button
SAVE_AS_DRAFT   →  Save As Draft
PASSWORD        →  Password
```

Algorithm:

1. Split on underscores.
2. Capitalise only the first character of each token.
3. Join with a single space.

Custom labels remain fully supported through overrides:

```java
@Override
public String getDisplayText() {
    return "Submit Application";
}
```

Documenting the transformation rules explicitly ensures consistent display across log output, reporting, and tooling.

---

# Part 5 — Deterministic Locator Repository Convention

Introduce a fixed convention for where page repositories live within the Maven project layout.

VOID discovers each page's repository by deriving the resource path from the page class name — no declaration required.

```
src/main/resources/pages/DemoLoginPage/locators.json
```

Derived from:

```
DemoLoginPage  →  pages/DemoLoginPage/locators.json
```

Pages that require a different source override `getExternalFileName()`. See Part 8.

---

# Part 6 — Properties Template Generation

Introduce a CLI command that generates a pre-populated properties template from a page's enum declarations.

Given:

```java
public interface DemoLoginPage {

    enum Credentials implements Typeable {
        USERNAME_INPUT,
        PASSWORD_INPUT
    }

    enum Button implements Clickable {
        LOGIN_BUTTON
    }
}
```

The generator produces:

```properties
# DemoLoginPage — locators
# Generated from enum declarations. Do not edit keys. Fill values only.

USERNAME_INPUT=
PASSWORD_INPUT=
LOGIN_BUTTON=
```

Every key is already present and correctly named. The developer fills the values. Nothing else.

This eliminates:

- Typing locator keys by hand.
- Spelling mistakes.
- Casing inconsistencies.
- Keys missing because a constant was added but the properties file was not updated.

---

# Part 7 — Runtime Repository Generation

The step that converts a filled properties file into the runtime JSON repository is performed by the existing JSON Migration CLI.

```
locators.properties  →  CLI  →  locators.json
```

This phase positions that tool as a named step in the generation pipeline — Runtime Repository Generation — rather than treating it as a standalone migration utility.

No new generation tool is needed or proposed. The CLI is already implemented.

---

# Part 8 — `getExternalFileName()` as an Override

`getExternalFileName()` is not removed.

Its role changes. It becomes an advanced override for cases where the deterministic convention does not apply.

Override use cases:

- Shared repositories used by multiple pages.
- Generated repositories from external sources.
- Plugin or integration-supplied repositories.
- Custom project structures.

When `getExternalFileName()` returns a non-null value, it takes precedence over the convention.

This preserves full flexibility while eliminating manual path declarations from the common case.

---

# Part 9 — Locator Resolution Order

```
Step 1 — Element override
         getExternalFileName() returns non-null
         → use the declared path directly

Step 2 — Deterministic convention
         derive path from page name via LocatorContext
         → use the resolved repository

Step 3 — Hardcoded fallback
         no external source found
         → treat the locator returned by the element as final XPath / CSS
```

---

# Resolution Flow

```text
Element
      │
      ▼
getExternalFileName() != null?
      │
 ┌────┴────┐
 │         │
Yes        No
 │         │
 ▼         ▼
Use file   LocatorContext.resolve(element)
           (derives path from page name)
               │
        ┌──────┴──────┐
        │             │
     Found          Missing
        │             │
        ▼             ▼
 External lookup   Treat locator
                   as hardcoded
```

---

# Part 10 — Mixed Locator Strategies

Pages can mix conventional and hardcoded locators freely.

```java
public interface UsersPage {

    // Resolved from convention: src/main/resources/pages/UsersPage/locators.json
    enum Buttons implements Clickable {
        SAVE,
        CANCEL
    }

    // Hardcoded — returns null to bypass external lookup
    enum Dynamic implements Clickable {

        DELETE_ROW;

        @Override
        public String getExternalFileName() {
            return null;
        }

        @Override
        public String getTriggerLocator() {
            return "//tr[td='%s']//button";
        }
    }
}
```

Result:

```
SAVE        →  resolved from pages/UsersPage/locators.json
DELETE_ROW  →  uses hardcoded XPath
```

---

# Part 11 — Remove Constructors From Static Elements

### Current

```java
enum Credentials {

    USERNAME_INPUT("USERNAME_INPUT"),
    PASSWORD_INPUT("PASSWORD_INPUT");

    private final String key;
    Credentials(String k) { this.key = k; }

    @Override public String getInputLocator() { return key; }
}
```

### Proposed

```java
enum Credentials implements Typeable {
    USERNAME_INPUT,
    PASSWORD_INPUT
}
```

Constructors remain only when elements genuinely require runtime metadata — dynamic arguments or an explicitly custom display label.

---

# Part 12 — Simplify Capability Interfaces

Move common implementations into the base `Element` interface.

Capability interfaces should define:

- Supported actions.
- Locator roles.
- Capability-specific behavior.
- Action emission.

They should not contain forwarding implementations that delegate to parent interfaces without adding behavior.

---

# Part 13 — LocatorContext

`LocatorContext` abstracts how the resolver locates a repository for a given element.

With the deterministic repository convention, its responsibility is:

```
Resolve page from element
      │
      ▼
Derive repository path from page name
      │
      ▼
Load LocatorRepository
```

The abstraction decouples the resolver from the specific convention. If the convention or repository format evolves, the resolver is unaffected.

`LocatorContext` also handles the `getExternalFileName()` override path — the resolver delegates to it unconditionally rather than branching on null-checks directly.

---

# Part 14 — Cache the LocatorContext Resolution

Repository resolution should happen only once per page.

Cache the resolved `LocatorRepository`, not just the filename.

```java
ConcurrentHashMap<Class<?>, LocatorRepository>
```

Caching the repository rather than the path means:

- All subsequent lookups for elements on the same page are constant-time.
- The cache remains correct if the underlying repository source changes format.
- Future repository types — remote, computed, YAML — require no change to the cache layer.

---

# Part 15 — Preserve Nested Enum Organization

The existing organization is unchanged.

```java
DemoLoginPage.Credentials.USERNAME_INPUT
DemoLoginPage.Button.LOGIN_BUTTON
DemoLoginPage.Labels.SUCCESS_MESSAGE
```

Benefits:

- Logical capability-based grouping.
- Natural IDE autocomplete at the page level.
- Strong compile-time discoverability.
- Consistent navigation.

---

# Expected Benefits

After this phase:

- Locator keys are never typed by hand — derived from enum constants.
- Display text is never typed by hand — derived from enum constants.
- Repository locations are never configured — derived from page names via convention.
- Properties templates are never typed by hand — generated from enum declarations.
- JSON repositories are never typed by hand — generated by Runtime Repository Generation.
- Locator values are the only manually maintained artifact.
- Static elements require no implementation code beyond the enum declaration.
- Capability interfaces are significantly smaller.
- Hardcoded locator support is fully preserved.
- Advanced override mechanisms remain available at every level.
- Project organization is deterministic and consistent across all pages.
- Onboarding is simpler — the layout is self-explanatory.
- Deterministic structure improves both human and tool-assisted development.

---

# Expected Impact

Typical page definitions should become substantially smaller because repetitive runtime plumbing is eliminated.

The developer workflow shortens to three steps that require judgment: define the page structure, fill in locator values, and run tests. Everything between those steps is generated.

---

# Open Decisions

The following decisions should be resolved before implementation begins.

## 1. Locator Repository Convention — Root Path

The exact root path for the repository convention must be agreed upon.

Considerations:

- Whether the root is fixed (`pages/`) or configurable per project.
- Whether the convention applies to the classpath root or a specific source root.
- How to handle pages that exist outside the conventional root (the override mechanism covers this, but the convention boundary should be explicit).

## 2. Properties Template Generator — CLI Design

The template generator introduces a new CLI command.

Decisions required:

- Command name and invocation style.
- Whether it operates on a single page, a directory, or the entire project.
- Behavior when a properties file already exists — overwrite, merge, or refuse.
- Whether enum constants added after initial generation are automatically appended.
- Relationship to the future `sync-locators` concept (see Developer Workflow).

## 3. LocatorContext Contract

The `LocatorContext` interface is described conceptually in Part 13.

Before implementation, agree on:

- Exact method signatures.
- How it composes with the existing `LocatorResolver`.
- What the default implementation is permitted to assume about the project layout.
- Whether `LocatorContext` is injectable or resolved through a registry.

## 4. Repository Abstraction Boundaries

Decide how far `LocatorRepository` abstracts the underlying source.

This affects whether future repository types can be introduced at the `LocatorRepository` level without modifying the resolver, the cache, or the runtime.
