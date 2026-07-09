# Element API Simplification & Boilerplate Reduction

## Overview

> **For the overall architecture, design principles, terminology, and usage of VOID, refer to `README.md`.**

This phase focuses exclusively on improving the Element API by reducing repetitive implementations, centralizing common behavior, and simplifying how page elements are defined.

The primary objective is to shift repetitive framework plumbing from page definitions into the framework itself, allowing developers to focus on describing UI elements rather than implementing infrastructure code.

Every change follows the same principle:

> *The framework already knows this information, so the user should not have to repeat it.*

The proposed changes aim to:

- Reduce boilerplate across page definitions.
- Preserve compile-time safety.
- Maintain capability-driven element modeling.
- Preserve existing locator resolution behavior.
- Improve long-term maintainability.
- Keep the public API clean and intuitive.

---

# Non-Goals

This phase does **not** aim to:

- Change the overall architecture of VOID.
- Modify the execution engine.
- Change Flow or Action APIs.
- Replace capability interfaces.
- Remove support for hardcoded locators.

The focus of this phase is solely to simplify the Element API while preserving the existing programming model.

---

# Before and After

The following comparison illustrates the cumulative effect of all changes in this phase.

It is placed here so the reader can evaluate any individual improvement against the concrete goal.

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

### After

```java
@LocatorFile("demo-login-elements.json")
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

The nested enum structure, capability interfaces, and compile-time discoverability are all preserved.

---

# Motivation

Over time, the Element API has accumulated a considerable amount of repetitive implementation code.

Most of this code does not describe the UI itself — it simply satisfies framework contracts.

As applications grow, this repetition scales proportionally.

A page containing 60–100 elements may require hundreds of lines of infrastructure code that is nearly identical across every project.

The framework already possesses enough information to infer much of this behavior automatically.

This phase moves that responsibility into the framework.

---

# Current Pain Points

## 1. Duplicate Locator Keys

Current implementation:

```java
enum Credentials implements Typeable {

    USERNAME_INPUT("USERNAME_INPUT"),
    PASSWORD_INPUT("PASSWORD_INPUT");
}
```

The enum constant already uniquely identifies the locator key.

Maintaining duplicate strings:

- increases typing
- introduces rename risks
- creates opportunities for mistakes
- provides no additional information

---

## 2. Repeated Empty Arguments

Most elements require no locator arguments.

Yet nearly every enum implements:

```java
@Override
public Object[] getArgs() {
    return new Object[0];
}
```

This implementation is identical throughout the project.

---

## 3. Repeated Locator File Definitions

Every enum repeats:

```java
@Override
public String getExternalFileName() {
    return LOCATOR_FILE;
}
```

Although every element belongs to the same page and therefore shares the same locator repository.

This metadata naturally belongs to the page rather than individual elements.

---

## 4. Constructors Without Meaning

Many constructors exist only to store duplicated locator keys.

Example:

```java
USERNAME_INPUT("USERNAME_INPUT"),
PASSWORD_INPUT("PASSWORD_INPUT");
```

These constructors carry no meaningful runtime state.

---

## 5. Capability Interfaces Contain Framework Plumbing

Several capability interfaces primarily forward inherited behavior.

Example:

```java
@Override
default String getPrimaryLocator() {
    return Typeable.super.getPrimaryLocator();
}
```

Such methods increase maintenance cost without adding capability-specific behavior.

---

## 6. Repeated Display Text Implementations

Many elements override display text even though a sensible value can be derived from the enum constant.

Example:

```
LOGIN_BUTTON  →  Login Button
```

---

# Design Principles

The refactoring should follow these principles:

- Convention over repetition.
- Framework intelligence over developer boilerplate.
- Strong compile-time guarantees.
- Explicit object-oriented modeling.
- Backward compatibility wherever practical.
- Preserve readability over excessive abstraction.

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

The framework derives the locator key directly from the enum constant name.

Example implementation:

```java
default String getPrimaryLocator() {
    return ((Enum<?>) this).name();
}
```

### Benefits

- Eliminates duplicated strings.
- Safe IDE renaming — rename the constant and the lookup key follows automatically.
- Prevents locator key mismatches.
- Smaller page definitions.

---

# Part 2 — Default Empty Arguments

Provide a default implementation within `Element`.

```java
default Object[] getArgs() {
    return NO_ARGS;
}
```

Dynamic elements continue overriding the method as before.

Example:

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

`EMPTY_ARGS` describes the state of the array.

`NO_ARGS` communicates the intent: this element requires no arguments.

Names should reflect intent, not implementation.

### Benefits

- More readable at every call site.
- Clearer meaning without requiring context.

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

The algorithm:

1. Split the constant name on underscores.
2. Capitalise only the first character of each token.
3. Join tokens with a single space.

Custom labels remain supported through overrides.

```java
@Override
public String getDisplayText() {
    return "Submit Application";
}
```

Documenting the transformation explicitly avoids ambiguity and ensures consistent display across all log output, reporting, and tooling.

### Benefits

- Better log readability.
- Less repetitive code.
- Consistent behavior across the project.

---

# Part 5 — Page-Level Locator Metadata

## The Annotation Option

Introduce a page annotation:

```java
@LocatorFile("demo-login-elements.json")
public interface DemoLoginPage {
}
```

The annotation becomes the default locator repository for all nested elements, removing the need for every enum to implement `getExternalFileName()` individually.

## The Interface Option

Alternatively, the page could implement a `LocatorRepositoryProvider` interface:

```java
public interface DemoLoginPage extends LocatorRepositoryProvider {

    @Override
    default String getLocatorFile() {
        return "demo-login-elements.json";
    }
}
```

## Trade-off

| Aspect                    | `@LocatorFile` annotation          | `LocatorRepositoryProvider` interface |
|---------------------------|------------------------------------|---------------------------------------|
| Syntax                    | Minimal                            | Explicit, object-oriented             |
| Extensibility             | Limited to filename                | Can evolve to remote, YAML, DB        |
| VOID design consistency   | Annotation-based config            | Interface-based modeling              |
| Discoverability           | Requires annotation processor docs | IDE can navigate interface hierarchy  |
| Dynamic sources           | Not supported                      | Supported via method override         |

VOID has consistently favored explicit interfaces and object-oriented constructs over annotation-heavy configuration. `@LocatorFile` is appropriate when locator sources remain static JSON files permanently. If locator sources are expected to evolve, `LocatorRepositoryProvider` offers more flexibility without requiring a later migration.

**This decision should be made before implementation begins.**

---

# Part 6 — Preserve Hardcoded Locator Support

Returning `null` from `getExternalFileName()` signals to the resolver that the locator returned by the element is already the final XPath or CSS selector.

Example:

```java
enum DynamicElements implements Clickable {

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
```

No external lookup should occur in this case.

This behavior is an intentional feature and must be preserved.

---

# Part 7 — LocatorContext Abstraction

The resolution path currently relies on `getEnclosingClass()` to locate a page annotation.

That works for nested enums but ties the resolver conceptually to one particular structural pattern.

If someone later introduces a non-nested element:

```java
public final class LoginButtons implements Clickable {
    // no enclosing page
}
```

the enclosing class lookup returns `null` and resolution silently falls through to the hardcoded path.

Rather than hardcoding the "nested enum inside an annotated interface" assumption into the resolver, introduce a `LocatorContext` that abstracts how the resolver determines the locator source for a given element.

```
LocatorContext
      │
      ▼
LocatorRepository
      │
      ▼
   JSON / YAML / DB / Remote
```

Internally, the default `LocatorContext` implementation still uses `getEnclosingClass()` and reads `@LocatorFile`. But the resolver is decoupled from that mechanism — it asks `LocatorContext` for the repository, not the class hierarchy directly.

This means:

- The common case (nested enum + annotation) continues to work exactly as today.
- Future structures (non-nested elements, YAML sources, remote repositories) can be supported by providing a different `LocatorContext` without touching the resolver.
- The abstraction is thin enough to add no complexity to the common path.

---

# Part 8 — Locator Resolution Order

The resolver determines the locator source using the following order:

## Step 1 — Element Override

```java
element.getExternalFileName()
```

If non-null, use that locator file directly.

## Step 2 — LocatorContext Lookup

Delegate to `LocatorContext` to determine the repository.

In the default implementation this inspects the enclosing class for `@LocatorFile` (or a `LocatorRepositoryProvider` implementation, depending on Part 5 decision).

If a repository is found, use it.

## Step 3 — Hardcoded Fallback

If neither Step 1 nor Step 2 produces a source, treat the locator returned by the element as a hardcoded XPath or CSS selector.

---

# Resolution Flow

```text
Element
      │
      ▼
Element override?   (getExternalFileName() != null)
      │
 ┌────┴────┐
 │         │
Yes        No
 │         │
 ▼         ▼
Use file   LocatorContext.resolve(element)
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

# Part 9 — Mixed Locator Strategies

Pages should be able to mix both locator strategies.

Example:

```java
@LocatorFile("users.json")
public interface UsersPage {

    enum Buttons implements Clickable {
        SAVE,
        CANCEL
    }

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
SAVE        →  resolved from users.json
DELETE_ROW  →  uses hardcoded XPath
```

---

# Part 10 — Remove Constructors From Static Elements

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

Constructors remain only when elements genuinely require runtime metadata, such as dynamic arguments or a custom display label.

---

# Part 11 — Simplify Capability Interfaces

Move common implementations into the base `Element` interface.

Capability interfaces should primarily define:

- Supported actions.
- Locator roles.
- Capability-specific behavior.
- Action emission.

They should no longer contain forwarding implementations whose only purpose is delegating to parent interfaces.

This reduces maintenance effort while making capability interfaces easier to understand.

---

# Part 12 — Cache the LocatorContext Resolution

Reading the enclosing class and its annotations requires reflection.

The cache should store the resolved `LocatorContext` (or `LocatorRepository`), not just the filename.

```java
ConcurrentHashMap<Class<?>, LocatorRepository>
```

Rationale: caching only the filename means additional work is still done on each resolution to construct the repository from that name. Caching the repository itself means the first lookup pays the full reflection cost, and all subsequent lookups are constant-time regardless of how the repository evolves internally.

If locator repositories later become remote or computed, the cache continues to work without modification.

---

# Part 13 — Preserve Nested Enum Organization

The existing organization remains unchanged.

Example:

```java
DemoLoginPage.Credentials.USERNAME_INPUT
DemoLoginPage.Button.LOGIN_BUTTON
DemoLoginPage.Labels.SUCCESS_MESSAGE
```

Benefits include:

- Logical grouping by capability.
- Better IDE autocomplete.
- Strong compile-time discoverability.
- Cleaner navigation.

---

# Expected Benefits

After this refactoring:

- Locator keys no longer need to be duplicated.
- Static elements require little or no implementation code.
- Page-level metadata replaces repeated locator file definitions.
- Hardcoded locator support remains fully intact.
- Capability interfaces become significantly smaller.
- Framework behavior becomes more centralized.
- Page definitions become easier to read and maintain.

---

# Expected Impact

Typical page definitions should become substantially smaller because repetitive framework plumbing is eliminated.

The public programming model remains familiar. The amount of required implementation code is dramatically reduced for the common case while all escape hatches remain available.

---

# Open Decisions

Two decisions should be resolved before implementation begins:

## 1. Annotation vs. Interface for Page-Level Locator Metadata

See Part 5.

If locator sources are expected to remain static JSON files permanently, `@LocatorFile` is the cleaner API.

If locator sources may evolve, `LocatorRepositoryProvider` is more future-proof.

## 2. LocatorContext Scope

The `LocatorContext` abstraction in Part 7 is described at a conceptual level.

Before implementation, the team should agree on the exact interface contract, how it composes with the existing `LocatorResolver`, and what the default implementation is permitted to assume.
