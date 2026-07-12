# Element API Simplification & Boilerplate Reduction

## Overview

> **For the overall architecture, design principles, terminology, and usage of VOID, refer to `README.md`.**

This phase improves the Element API and the surrounding developer workflow by establishing a principle that reaches beyond reducing boilerplate:

> **Developer-authored code should remain the single source of truth.**
>
> Whenever VOID or its tooling can deterministically derive runtime artifacts from that source, those artifacts should be generated rather than manually maintained.

Developers should author **intent** — page structure, capability groupings, locator values.

VOID should generate **everything else** — locator keys, display text, argument defaults, properties templates, JSON repositories, and repository locations.

---

# Non-Goals

This phase does **not** aim to:

- Change the overall architecture of VOID.
- Modify the execution engine.
- Change Flow or Action APIs.
- Replace capability interfaces.
- Remove support for hardcoded locators.
- Introduce a new JSON generation tool (the existing JSON Migration CLI already serves this role).

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

The deeper problem is not just the volume of repetition. It is that the runtime forces developers to maintain artifacts it could generate deterministically.

Locator keys follow directly from enum constant names. Display text follows directly from those names. Repository locations follow directly from page names. Properties templates follow directly from enum declarations.

None of these require human judgment. All of them create human error.

> The purpose of automation is not to shift repetitive work to developers, but to eliminate it entirely.

This phase moves that responsibility into the runtime and its tooling.

---

# Design Philosophy

The refactoring follows these principles:

- **Single source of truth.** Developer-authored code is the authoritative source. Generated artifacts derive from it, not the reverse.
- **Generated artifacts over manually maintained artifacts.** If VOID can deterministically produce something, it should.
- **Convention over repetition.** A consistent project layout eliminates the need to configure what can be inferred.
- **Deterministic project structure.** Repository locations follow from page names. No path configuration is required in the common case.
- **Runtime intelligence over developer boilerplate.** Defaults handle the common case. Overrides handle exceptions.
- **Strong compile-time guarantees.** No runtime discovery of elements. No string-typed identifiers at call sites.
- **Preserve escape hatches for advanced scenarios.** Every convention can be overridden. No capability is removed.

---

# Generated, Not Maintained

The following table distinguishes what developers author from what VOID generates.

| What developers author               | What VOID generates or infers                         |
|--------------------------------------|-------------------------------------------------------|
| Page interface                       | Repository location (from deterministic layout)       |
| Capability enum declarations         | Properties template (all keys pre-filled)             |
| Locator values in properties file    | JSON repository (via existing JSON Migration CLI)     |
| Dynamic args when genuinely needed   | Locator key from enum constant name                   |
| Custom display text when needed      | Display text from enum constant name                  |
| Hardcoded locators when needed       | Default empty args                                    |

Developers should never manually maintain anything in the right column.

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

Several capability interfaces contain forwarding implementations that delegate to parent interfaces without adding capability-specific behavior. These increase maintenance cost without contributing to element modeling.

---

## 8. Repeated Display Text Implementations

Many elements override display text even though a sensible value can be derived from the enum constant name.

```
LOGIN_BUTTON  →  Login Button
```

---

# Deterministic Project Layout

Rather than requiring developers to declare locator file paths, VOID should adopt a convention for where each page's repository lives.

```
pages/
    DemoLoginPage/
        DemoLoginPage.java
        locators.properties
        locators.json

    DashboardPage/
        DashboardPage.java
        locators.properties
        locators.json
```

The runtime discovers the repository by deriving the path from the page name.

No path configuration is required. No `LOCATOR_FILE` constant. No `@LocatorFile` annotation. No `getExternalFileName()` override for the common case.

Benefits:

- Zero path configuration in common usage.
- Consistent navigation — every page follows the same structure.
- Easier onboarding — the layout is self-explanatory.
- Reliable IDE navigation — all page assets live in one place.
- LLM-friendly — predictable structure is far easier to generate and read correctly.

`getExternalFileName()` is preserved as an advanced override mechanism for pages with shared repositories, generated repositories, or custom locations. See Part 8.

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
(the only manual step)
      │
      ▼
Existing JSON Migration CLI
      │
      ▼
Generated JSON repository
      │
      ▼
Runtime
```

At no point does a developer type a locator key. The enum constant name is the key. Tooling writes it. The developer provides only the XPath or CSS value that no tool can infer.

---

# Developer Workflow

The intended end-to-end workflow after this phase:

```
1.  Create the page interface.
2.  Add capability enums with constants.
3.  Run the properties template generator.
4.  Open the generated template — all keys are already present.
5.  Fill in the locator values (XPath, CSS, ID, etc.).
6.  Run the existing JSON Migration CLI.
7.  Execute tests.
```

Step 3 and step 6 are the only tool invocations. Steps 1, 2, and 5 are the only places where developer judgment is required.

Everything else is generated.

---

# Properties as the Authoring Format

Properties files are the preferred format for authoring locators because:

- They are simple flat key-value structures — no nesting, no syntax complexity.
- Humans edit them reliably.
- LLMs generate them reliably.
- Diffs are clean and readable — one line per locator.

Properties are not a fallback or a compatibility layer. They are intentionally the best format for the task of recording locator values by hand.

JSON is the canonical runtime format. Users should generally not edit JSON directly.

---

# JSON as the Runtime Repository

JSON is the format VOID reads at runtime.

It is structured and typed, which makes it well-suited for the runtime. It is not optimized for human editing.

The JSON Migration CLI is the bridge between the authoring format (properties) and the runtime format (JSON). It is already implemented. This phase documents the intended workflow around it rather than introducing a replacement.

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

# Part 5 — Deterministic Project Layout

Introduce a fixed convention for page directory structure.

VOID discovers each page's repository by deriving the path from the page class name — no declaration required.

Example layout:

```
pages/
    DemoLoginPage/
        DemoLoginPage.java
        locators.properties
        locators.json
```

Derivation:

```
DemoLoginPage  →  pages/DemoLoginPage/locators.json
```

Pages that need a different layout override `getExternalFileName()`. See Part 8.

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
# DemoLoginPage locators
# Generated — do not edit keys. Fill values only.

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

# Part 7 — Existing JSON Migration CLI

The JSON Migration CLI is already implemented. Its role does not change.

It consumes the filled properties file and produces the runtime JSON repository:

```
locators.properties  →  JSON Migration CLI  →  locators.json
```

This phase documents the intended workflow and positions the CLI as the second step in the generation pipeline rather than a standalone migration utility.

No new JSON generation tool is needed or proposed.

---

# Part 8 — `getExternalFileName()` as an Override

`getExternalFileName()` is not removed.

Its role changes. It becomes an advanced override for cases where the deterministic layout does not apply.

Override use cases:

- Shared repositories used by multiple pages.
- Generated repositories from external sources.
- Plugin or integration-supplied repositories.
- Custom project structures.

When `getExternalFileName()` returns a non-null value, it takes precedence over the deterministic layout.

This preserves full flexibility while eliminating manual path declarations from the common case.

---

# Part 9 — Locator Resolution Order

```
Step 1 — Element override
         element.getExternalFileName() != null
         → use declared file directly

Step 2 — Deterministic layout
         derive path from page name via LocatorContext
         → use resolved repository

Step 3 — Hardcoded fallback
         no external source found
         → treat locator returned by the element as final XPath / CSS
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

    // Resolved from deterministic layout: pages/UsersPage/locators.json
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

They should not contain forwarding implementations that delegate to parent interfaces without adding behavior. Those increase maintenance cost and make capability interfaces harder to read.

---

# Part 13 — LocatorContext

`LocatorContext` abstracts how the resolver locates the repository for a given element.

With a deterministic project layout, its responsibility is:

```
Resolve page from element
      │
      ▼
Derive repository path from page name
      │
      ▼
Load repository
```

The abstraction remains useful because it decouples the resolver from the specific layout convention. If the layout or repository format evolves, the resolver is unaffected.

The abstraction also covers the case where `getExternalFileName()` returns a non-null value — the resolver delegates to `LocatorContext` rather than branching on this check directly.

---

# Part 14 — Cache the LocatorContext Resolution

Repository resolution involves deriving a path from the page name and loading the result. This should happen only once per page.

Cache the resolved `LocatorRepository`, not just the filename.

```java
ConcurrentHashMap<Class<?>, LocatorRepository>
```

Caching the repository means all subsequent lookups for elements on the same page are constant-time, regardless of how the repository is sourced internally. If repositories later become remote or computed, the cache continues to work without modification.

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
- Repository locations are never configured — derived from page names.
- Properties templates are never typed by hand — generated from enum declarations.
- JSON repositories are never typed by hand — generated by the existing CLI.
- Locator values are the only manually maintained artifact.
- Static elements require no implementation code beyond the enum declaration.
- Capability interfaces are significantly smaller.
- Hardcoded locator support is fully preserved.
- Advanced escape hatches remain available at every level.
- Project organization is deterministic and consistent across all pages.
- Onboarding is simpler — the layout is self-explanatory.
- LLM-assisted development is significantly more reliable — predictable structure, no hidden conventions.

---

# Expected Impact

Typical page definitions should become substantially smaller because repetitive runtime plumbing is eliminated.

The developer workflow shortens to three steps that require judgment: define the page structure, fill in locator values, and run tests. Everything between those steps is generated.

---

# Open Decisions

The following decisions should be resolved before implementation begins.

## 1. Deterministic Layout Convention

The exact directory structure must be agreed upon before implementation.

Considerations:

- Root directory name (`pages/`, `src/pages/`, or project-configured).
- Whether the convention is fixed or configurable per project.
- How to handle pages that exist outside the conventional root.

## 2. Properties Template Generator — CLI Design

The template generator introduces a new CLI command.

Decisions required:

- Command name and invocation style.
- Whether it operates on a single page, a directory, or the entire project.
- Behavior when a properties file already exists — overwrite, merge, or refuse.
- Whether missing enum constants in an existing file are appended automatically.

## 3. LocatorContext Contract

The `LocatorContext` interface is described conceptually in Part 13.

Before implementation, agree on:

- Exact method signatures.
- How it composes with the existing `LocatorResolver`.
- What the default implementation is permitted to assume about the project layout.
- Whether `LocatorContext` is injectable or resolved through a registry.

## 4. Repository Abstraction Boundaries

Decide how far `LocatorRepository` abstracts the underlying source.

This affects whether future repository types (YAML, remote, database) can be introduced without modifying the resolver or the cache.
