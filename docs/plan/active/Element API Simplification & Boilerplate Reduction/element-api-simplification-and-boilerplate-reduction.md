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

Locator keys follow directly from enum constant names. Display text follows directly from those names. Repository locations follow directly from page types. Properties templates follow directly from enum declarations.

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
- **Deterministic structure.** Repository locations follow from page types. No path configuration is required in the common case.
- **Framework intelligence over developer boilerplate.** Defaults handle the common case. Overrides handle exceptions.
- **Disposable generated artifacts.** The runtime never treats generated artifacts as authoritative. They are disposable outputs that can be regenerated from the source of truth at any time.
- **Strong compile-time guarantees.** No runtime discovery of elements. No string-typed identifiers at call sites.
- **Preserve escape hatches for advanced scenarios.** Every convention can be overridden. No capability is removed.
- **Preserve readability over abstraction.** Defaults should reduce code, not require understanding a new abstraction to use an element.

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

Java source files remain in `src/main/java`. Locator resources live in `src/main/resources` under a structure that mirrors the full package path of the page type.

```
src/main/java/tests/demo/pages/
    DemoLoginPage.java

src/main/resources/tests/demo/pages/
    DemoLoginPage/
        locators.properties
        locators.json
```

The runtime derives the resource path from the page's fully qualified type:

```
tests.demo.pages.DemoLoginPage  →  tests/demo/pages/DemoLoginPage/locators.json
```

Deriving from the fully qualified type rather than just the class name eliminates collisions. Two pages named `LoginPage` in different packages (`admin.LoginPage`, `customer.LoginPage`) produce distinct repository paths and never conflict.

No path configuration is required. No `LOCATOR_FILE` constant. No annotation. No `getExternalFileName()` override for the common case.

`getExternalFileName()` is preserved as an advanced override for pages with shared repositories, generated repositories, or custom locations. See Part 8.

Benefits:

- Zero path configuration in common usage.
- Collision-free — repository paths are globally unique within the project.
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

The runtime derives the locator key from the fully qualified element path — page name, enum group name, and constant name — joined with dots.

```java
default String getPrimaryLocator() {
    Enum<?> e = (Enum<?>) this;
    Class<?> enumClass = e.getDeclaringClass();
    Class<?> pageClass = enumClass.getEnclosingClass();
    if (pageClass != null) {
        return pageClass.getSimpleName() + "." + enumClass.getSimpleName() + "." + e.name();
    }
    return enumClass.getSimpleName() + "." + e.name();
}
```

Examples:

```
DemoLoginPage.Credentials.USERNAME_INPUT
DemoLoginPage.Credentials.PASSWORD_INPUT
DemoLoginPage.Button.LOGIN_BUTTON
```

The namespace `PageName.GroupName` is derived from the Java type hierarchy — no configuration required.

### Benefits

- Eliminates duplicate strings.
- Rename-safe — IDE renaming updates the lookup key automatically.
- Prevents key collisions across pages with same-named constants.
- Keys are self-documenting — page and group context is visible in the properties file.
- Improves discoverability — searching for a key in the properties file immediately reveals its origin.
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

VOID discovers each page's repository by deriving the resource path from the page's fully qualified type — no declaration required.

```
src/main/resources/tests/demo/pages/DemoLoginPage/locators.json
```

Derived from:

```
tests.demo.pages.DemoLoginPage  →  tests/demo/pages/DemoLoginPage/locators.json
```

The package is included in the path so that pages with identical class names in different packages never produce the same repository path.

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

DemoLoginPage.Credentials.USERNAME_INPUT=
DemoLoginPage.Credentials.PASSWORD_INPUT=
DemoLoginPage.Button.LOGIN_BUTTON=
```

Keys are namespaced as `PageName.GroupName.CONSTANT_NAME`, derived from the Java type hierarchy. The developer fills the values. Nothing else.

This eliminates:

- Typing locator keys by hand.
- Spelling mistakes.
- Casing inconsistencies.
- Keys missing because a constant was added but the properties file was not updated.
- Key collisions between pages that share the same constant name.

**Regeneration behavior.**

When a properties file already exists, the template generator follows these rules:

- Enum constants with no corresponding key in the file → key added with an empty value.
- Keys already present in the file → preserved exactly with their current value.
- Keys in the file with no matching enum constant → flagged as stale (warning only; no automatic deletion).

This means the generator is safe to run at any point in development. Running it after adding or renaming an enum constant adds the new key and flags the old one as stale, without touching any value the developer has already provided.

Enum constants are the authoritative source of locator identity. Renaming a constant produces a new key. The old key becomes stale and can be cleaned up explicitly. Existing values are never silently discarded.

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
         derive path from page type via LocatorContext
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
           (derives path from page type)
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
Derive repository path from page type
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

# Part 16 — Locator Families

Not every enum constant represents a unique locator. Many logical elements share a single locator template and differ only by their runtime argument — the text or identifier that identifies a specific instance at runtime.

For these elements, maintaining one properties key per constant is wasteful. The constant name already carries the semantic value; the locator template is shared.

`LocatorFamily` addresses this directly.

## Marker Interface

```java
public interface LocatorFamily extends Element {}
```

An enum that implements `LocatorFamily` declares that all its constants share a single locator template.

```java
public interface AdminHome {

    enum Tiles implements Clickable, LocatorFamily {
        AUDIT_INFO,
        MANAGE_USERS,
        MANAGE_DOCS,
        MANAGE_VENDORS
    }
}
```

## Key Format

The repository key for a locator family is derived from the page type and the enum type — without the constant name.

```
AdminHome.Tiles=
```

The developer supplies one locator template that applies to all constants in the enum:

```properties
AdminHome.Tiles=//button[preceding-sibling::p[normalize-space(.)='%s']]
```

## Automatic Runtime Arguments

VOID derives the runtime argument from the enum constant name using the same word-transform algorithm as `getDisplayText()` (Part 4):

```
MANAGE_USERS  →  "Manage Users"  →  Object[]{"Manage Users"}
```

At the call site:

```java
AdminHome.Tiles.MANAGE_USERS.click();
```

VOID internally resolves:

```xpath
//button[preceding-sibling::p[normalize-space(.)='Manage Users']]
```

The developer never implements `getArgs()` for family constants.

## Automatic Argument Boundary

Automatic argument derivation applies only when VOID can deterministically produce the full argument list — that is, when the locator template contains exactly one `%s` token whose value maps to the constant's semantic label.

Multi-argument templates are not automatically resolved:

```properties
Products.TableActions=//tr[td='%s']//button[@title='%s']
```

These continue to use explicit runtime arguments:

```java
Products.TableActions.DELETE.with("Laptop", "Delete");
```

---

# Part 17 — Advanced Locator Families

Some locator families require semantic values that cannot be derived automatically — acronyms, symbols, punctuation, or domain-specific UI labels.

`AdvancedLocatorFamily` extends `LocatorFamily`. Constants that need a custom semantic value carry it in a constructor. Constants without a constructor continue to use automatic derivation.

```java
public interface VendorPage {

    enum Filters implements Clickable, AdvancedLocatorFamily {

        COUNTRY,

        HQ_STATE_PROVINCE("HQ State/Province"),

        SAVE_AND_CONTINUE("Save & Continue"),

        CRM("CRM");
    }
}
```

Generated properties remain:

```properties
VendorPage.Filters=
```

Only exceptional values require explicit metadata. Everything else continues to be generated automatically.

---

# Part 18 — Switch Locator Families

For projects that prefer a centralised semantic mapping over per-constant constructors, `SwitchLocatorFamily` provides a switch-based alternative.

```java
public interface VendorPage {

    enum Filters implements Clickable, SwitchLocatorFamily {
        COUNTRY,
        PROGRAM_NAME,
        HQ_STATE_PROVINCE,
        SAVE_AND_CONTINUE,
        CRM
    }
}
```

Because `SwitchLocatorFamily` declares `getSemanticValue()` as an abstract method, the IDE immediately reports that the class must implement it. Using IntelliJ's **Implement Methods** quick fix generates an exhaustive switch:

```java
@Override
public String getSemanticValue() {
    return switch (this) {
        case COUNTRY -> throw new UnsupportedOperationException();
        case PROGRAM_NAME -> throw new UnsupportedOperationException();
        case HQ_STATE_PROVINCE -> throw new UnsupportedOperationException();
        case SAVE_AND_CONTINUE -> throw new UnsupportedOperationException();
        case CRM -> throw new UnsupportedOperationException();
    };
}
```

The developer replaces only the required mappings. Whenever a new constant is added, the compiler reports an incomplete switch and IntelliJ's **Add missing branches** quick fix updates it automatically.

Generated properties remain:

```properties
VendorPage.Filters=
```

## Runtime Argument Rules

All three family interfaces share the same runtime behavior. They differ only in how semantic values are authored.

| Interface | Semantic value source |
|---|---|
| `LocatorFamily` | Word-transform of constant name (automatic) |
| `AdvancedLocatorFamily` | Constructor value, or word-transform if no constructor |
| `SwitchLocatorFamily` | Return value of `getSemanticValue()` |

## Recommended Progression

```
Static Elements
      ↓
LocatorFamily          (most common)
      ↓
AdvancedLocatorFamily  (when some labels cannot be derived)
      ↓
SwitchLocatorFamily    (rare — prefer centralised mapping + exhaustive switch)
```

---

# Expected Benefits

After this phase:

- Locator keys are never typed by hand — derived from enum constants.
- Display text is never typed by hand — derived from enum constants.
- Repository locations are never configured — derived from page types via convention.
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

Typical page definitions should shrink by **60–80%** because repetitive runtime plumbing is eliminated.

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

## 5. Regeneration Strategy

When the template generator runs against an existing properties file, the exact merge behavior requires explicit agreement.

Decisions required:

- Whether the generator overwrites the file completely, merges with existing values, or refuses if the file already exists.
- Whether inline comments in the properties file are preserved during regeneration.
- Whether stale keys (present in the file but with no matching enum constant) are automatically removed, flagged with a warning, or silently retained.
- Whether a dry-run mode is provided to preview what would change before applying it.

The recommended default is merge-with-preserve: add new keys, retain existing values, flag stale keys as warnings. This makes the generator safe to invoke at any point in development without risk of data loss. Automatic deletion of stale keys should require an explicit flag.
