# Element API Simplification & Boilerplate Reduction

## Overview

> **For the overall architecture, design principles, terminology, and usage of VOID, refer to `README.md`.**

This phase focuses exclusively on improving the Element API by reducing repetitive implementations, centralizing common behavior, and simplifying how page elements are defined.

The primary objective is to shift repetitive framework plumbing from page definitions into the framework itself, allowing developers to focus on describing UI elements rather than implementing infrastructure code.

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
- Introduce annotation-driven element definitions.
- Remove support for hardcoded locators.

The focus of this phase is solely to simplify the Element API while preserving the existing programming model.

---

# Motivation

Over time, the Element API has accumulated a considerable amount of repetitive implementation code.

Most of this code does not describe the UI itself—it simply satisfies framework contracts.

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
USERNAME("USERNAME"),
PASSWORD("PASSWORD");
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
LOGIN_BUTTON

↓

Login Button
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

The framework derives the locator key directly from the enum constant.

Example implementation:

```java
default String getPrimaryLocator() {
    return ((Enum<?>) this).name();
}
```

### Benefits

- Eliminates duplicated strings.
- Safe IDE renaming.
- Prevents locator mismatches.
- Smaller page definitions.

---

# Part 2 — Default Empty Arguments

Provide a default implementation within `Element`.

```java
default Object[] getArgs() {
    return EMPTY_ARGS;
}
```

Dynamic elements continue overriding the method.

Example:

```java
PRODUCT_ROW.with("Laptop")
```

or another supported dynamic API.

### Benefits

- Eliminates repetitive implementations.
- Reduces allocations.
- Cleaner page definitions.

---

# Part 3 — Shared EMPTY_ARGS Constant

Replace repeated allocations of

```java
new Object[0]
```

with

```java
Element.EMPTY_ARGS
```

### Benefits

- Zero repeated allocations.
- Cleaner code.
- Consistent implementation.

---

# Part 4 — Automatic Display Text

Provide a default implementation that derives a human-readable label.

Example:

```
LOGIN_BUTTON

↓

Login Button
```

Custom labels remain supported through overrides.

### Benefits

- Better logging.
- Better reporting.
- Less repetitive code.

---

# Part 5 — Page-Level Locator Metadata

Introduce a page annotation.

Example:

```java
@LocatorFile("demo-login-elements.json")
public interface DemoLoginPage {
}
```

The page annotation becomes the default locator repository for all nested elements.

This removes the need for every enum to repeatedly implement:

```java
@Override
public String getExternalFileName() {
    return LOCATOR_FILE;
}
```

---

# Part 6 — Preserve Hardcoded Locator Support

The existing locator resolution behavior must remain unchanged.

If

```java
getExternalFileName()
```

returns

```java
null
```

the resolver must assume that the locator returned by the element is already the final XPath/CSS locator.

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

No external lookup should occur.

This behavior is an intentional feature and must be preserved.

---

# Part 7 — Locator Resolution Order

The resolver determines the locator source using the following order.

## Step 1

Element override.

```java
element.getExternalFileName()
```

If non-null,

use that locator file.

---

## Step 2

Otherwise inspect the enclosing page.

```java
element.getClass().getEnclosingClass()
```

If

```java
@LocatorFile(...)
```

exists,

use the annotated locator file.

---

## Step 3

If neither exists,

assume the locator is hardcoded.

---

# Resolution Flow

```text
Element
      │
      ▼
Element override?
      │
 ┌────┴────┐
 │         │
Yes        No
 │         │
 ▼         ▼
Use file   Check @LocatorFile
               │
        ┌──────┴──────┐
        │             │
      Found         Missing
        │             │
        ▼             ▼
External lookup   Treat locator
                  as hardcoded
```

---

# Part 8 — Mixed Locator Strategies

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
SAVE
↓

Resolved from users.json

-----------------------

DELETE_ROW
↓

Uses hardcoded XPath
```

---

# Part 9 — Remove Constructors From Static Elements

Current:

```java
enum Credentials {

    USERNAME("USERNAME"),
    PASSWORD("PASSWORD");
}
```

Proposed:

```java
enum Credentials {

    USERNAME,
    PASSWORD;
}
```

Constructors remain only when elements genuinely require metadata.

---

# Part 10 — Simplify Capability Interfaces

Move common implementations into the base `Element` interface.

Capability interfaces should primarily define:

- Supported actions.
- Locator roles.
- Capability-specific behavior.
- Action emission.

They should no longer contain forwarding implementations whose only purpose is delegating to parent interfaces.

This reduces maintenance effort while making capability interfaces easier to understand.

---

# Part 11 — Cache Page Annotation Lookup

Reading the enclosing page annotation requires reflection.

Although inexpensive, this lookup should occur only once.

Introduce a cache.

Example:

```java
ConcurrentHashMap<Class<?>, String>
```

The resolver performs reflection only during the first lookup for each page.

Subsequent resolutions become constant-time operations.

---

# Part 12 — Preserve Nested Enum Organization

The existing organization remains unchanged.

Example:

```java
DemoLoginPage.Credentials.USERNAME

DemoLoginPage.Buttons.LOGIN

DemoLoginPage.Messages.INVALID_LOGIN
```

Benefits include:

- Logical grouping.
- Better IDE autocomplete.
- Capability-based organization.
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
- Large applications experience substantial reductions in repetitive code.

---

# Estimated Impact

Typical page definitions are expected to shrink by **60–80%**.

The public programming model remains familiar while the amount of required framework plumbing is dramatically reduced.

The result is an Element API that is cleaner, easier to author, easier to maintain, and better aligned with VOID's long-term design philosophy.