# UIElement Layer Architecture

The `elements` package is VOID's **structural contract layer** -- it describes what UI elements
exist and what capabilities they support without executing anything. Elements emit typed
`Action` objects; they never perform browser interaction themselves.

---

## Table of Contents

1. [Overview](#overview)
2. [UIElement Interface Contract](#uielement-interface-contract)
3. [Capability Hierarchy](#capability-hierarchy)
4. [Enum-Driven UIElement Model](#enum-driven-uielement-model)
5. [LocatorFamily -- Shared Locator Patterns](#locatorfamily----shared-locator-patterns)
6. [ElementSupport -- Enum Reflection Utility](#elementsupport----enum-reflection-utility)
7. [LocatorRoles -- Role Map Construction](#locatorroles----role-map-construction)
8. [Capability Identity](#capability-identity)
9. [Invariants](#invariants)
10. [Extension Guide](#extension-guide)

---

## Overview

Elements separate declaration from execution:

- **Declaration** -- an element enum constant declares its locator keys, display text,
  dynamic args, and the capability interfaces it implements.
- **Emission** -- capability interface default methods return typed `Action` objects
  representing deferred intent.
- **Execution** -- `FlowExecutor` calls `action.perform(engine)`, at which point locators
  are resolved and browser interaction occurs.

Elements NEVER resolve locators eagerly. They NEVER call engine methods. The only output
of an element is an `Action` object.

---

## UIElement Interface Contract

`UIElement` (in `domain.automation.web.vocabulary.element`) is the web-domain root contract for all UI element
descriptors. It extends the domain-neutral `core.target.Target`, which carries the
members with zero UI semantics; `UIElement` adds locator-specific structure on top.

### Members inherited from `Target`

| Method | Purpose | Default behavior |
|---|---|---|
| `getDisplayText()` | Human-readable label for logging | Overridden on `UIElement`, derived from `ElementSupport.nameOf(this)` -- capitalized, underscores to spaces |
| `getArgs()` | Dynamic `%s` substitution arguments | `NO_ARGS` (empty array) |
| `effectiveArgs(Object... overrides)` | Returns `overrides` if non-null/non-empty, else falls back to `getArgs()` | Default composition helper on `Target` |
| `NO_ARGS` | Shared empty-args constant | `core.target.Target.NO_ARGS` |

### Core methods (`UIElement`-specific)

| Method | Purpose | Default behavior |
|---|---|---|
| `getExternalFileName()` | Classpath path to the locator resource file | Probes `PageClass/locators.json` then `PageClass/locators.properties`; returns the json path as preferred target |
| `getPrimaryLocator()` | Namespaced locator key for the element | Delegates to first value of `getAllLocatorRoles()`; falls back to enum-name-derived key |
| `getSecondaryLocator()` | Fallback locator key | `null` by default |
| `getAllLocatorRoles()` | Ordered map of `ElementRole` to locator key | Empty map in the root default; capability interfaces populate this |
| `capability()` | Identity of the capability this element implements | `ActionCapability.UNKNOWN` by default; overridden by each capability interface |

### Locator key derivation

The default `getPrimaryLocator()` reads the first key from `getAllLocatorRoles()`. Each
capability interface populates `getAllLocatorRoles()` with one or more role-to-key entries.
For example, `Clickable` returns `{TRIGGER: "PageClass.GroupName.CONSTANT_NAME"}`.

The fully-qualified locator key format is `PageClass.GroupName.CONSTANT_NAME`, derived from
the element enum's enclosing class hierarchy. `LocatorResolvers` uses this key to look up
the locator value in the JSON or properties file.

---

## Capability Hierarchy

All capability interfaces extend `UIElement`. Each interface:

1. Adds role-based locator keys to `getAllLocatorRoles()`
2. Returns a non-`UNKNOWN` `ActionCapability` constant from `capability()`
3. Emits one or more typed `Action` subclasses via default methods

### Full hierarchy

```
Target (core.target -- domain-neutral root)
└── UIElement (root)
├── Clickable              capability=CLICKABLE;   emits click()
│   ├── Checkable          capability=CHECKABLE;   emits toggle(), set(boolean)
│   └── (extended by Selectable, SearchField)
├── Typeable               capability=TYPEABLE;    emits type(), clear(), append(), typeAndPress()
│   └── (extended by SearchField)
├── ReadOnly               capability=READ_ONLY;   emits readText()
│   └── Hoverable          capability=HOVERABLE;   emits hover()
├── Selectable             capability=SELECTABLE;  extends Clickable + Listable; emits open(), select(), selectByText(), selectByValue()
│   └── SearchableDropdown capability=SEARCHABLE_DROPDOWN; extends Selectable + Searchable; emits searchAndSelect()
├── MultiSelectable        capability=MULTI_SELECTABLE; emits open(), selectAtIndex()
├── Uploadable             capability=UPLOADABLE;  emits upload(path)
├── Listable               capability=LISTABLE;    provides getIndex() default via ordinal
├── SearchField            capability=SEARCH_FIELD; extends Typeable + Clickable; emits typeSearch(), submitSearch()
│   └── Searchable         capability=SEARCHABLE;  adds SEARCH_RESULT role
├── Table                  capability=TABLE;       declares row/column/cell/header roles
│   └── EditableTable      capability=EDITABLE_TABLE; adds add/remove row buttons; emits clickAddRow()
└── KeyValuePair           standalone contract; no capability enum entry
```

### Capability ownership table

All 15 capability interfaces are **Web-domain vocabulary** (ADR-021, runtime-redesign I3.3).
They reside in `domain.automation.web.vocabulary.capability` and are never referenced by kernel packages.
The kernel uses only the neutral `ActionCapability` contract.

| Interface | `ActionCapability` constant | Domain | Package |
|---|---|---|---|
| `Clickable` | `CLICKABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Typeable` | `TYPEABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Selectable` | `SELECTABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Hoverable` | `HOVERABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Checkable` | `CHECKABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Uploadable` | `UPLOADABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Searchable` | `SEARCHABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `SearchField` | `SEARCH_FIELD` | Web | `domain.automation.web.vocabulary.capability` |
| `SearchableDropdown` | `SEARCHABLE_DROPDOWN` | Web | `domain.automation.web.vocabulary.capability` |
| `ReadOnly` | `READ_ONLY` | Web | `domain.automation.web.vocabulary.capability` |
| `Table` | `TABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `EditableTable` | `EDITABLE_TABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `Listable` | `LISTABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `MultiSelectable` | `MULTI_SELECTABLE` | Web | `domain.automation.web.vocabulary.capability` |
| `KeyValuePair` | (none -- standalone contract) | Web | `domain.automation.web.vocabulary.element` |

The fitness check `KernelBoundaryRulesTest.kernelCapabilityReferencesAreContractTypedOnly`
enforces this boundary automatically.

### Diamond inheritance

`Selectable` extends both `Clickable` and `Listable`, each of which returns a different
`ActionCapability` constant. `Selectable` resolves the diamond with an explicit
`@Override default ActionCapability capability()` returning `SELECTABLE`.

The same explicit override is required for any derived interface that extends two or more
parents with conflicting `capability()` defaults.

---

## Enum-Driven UIElement Model

Every element in VOID is declared as a Java enum constant. This is an architectural
invariant enforced by `ElementStructureRulesTest`.

### Defining elements

```java
public enum LoginPageElements {

    // Plain element -- no capability
    ;

    public enum Credentials implements Typeable {
        USERNAME_INPUT,
        PASSWORD_INPUT;

        @Override
        public String[] getArgs() { return UIElement.NO_ARGS; }
    }

    public enum Actions implements Clickable {
        SIGN_IN_BUTTON;
    }

    public enum Labels implements ReadOnly {
        ERROR_MESSAGE,
        SUCCESS_MESSAGE;
    }
}
```

### Nesting convention

Elements nest by page, then by functional group:

```java
AccountMappingElements.FilterPanel.StatusDropdown
AccountMappingElements.FilterPanel.DateRangeInput
AccountMappingElements.Grid.ActionButton
```

The outer enum is the page class. The inner enums are functional groups. This reflects UI
structure in the type hierarchy without any runtime cost.

### Locator key derivation from nesting

Given `AccountMappingElements.FilterPanel.StatusDropdown`, the derived locator key for the
`TRIGGER` role is:

```
AccountMappingElements.FilterPanel.StatusDropdown
```

The `locators.json` file for `AccountMappingElements` must contain this key:

```json
{
  "FilterPanel.StatusDropdown.TRIGGER": "xpath=//div[@data-testid='status-dropdown']"
}
```

`LocatorResolvers.strict()` performs the key lookup. `LocatorResolvers.legacyPadded()`
supports older `.properties` format with padding conventions.

---

## LocatorFamily -- Shared Locator Patterns

`LocatorFamily` (in `domain.automation.web.vocabulary.element`) addresses the case where multiple enum constants in a
group share the same locator key with different dynamic argument values. Instead of each
constant having its own unique key, they share one key and differ only in their `getArgs()`.

```java
public enum StatusDropdown implements Selectable, LocatorFamily {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PENDING("Pending");

    private final String label;

    StatusDropdown(String label) { this.label = label; }

    @Override
    public Object[] getArgs() { return new Object[]{label}; }
}
```

The `LocatorFamily` marker causes `getPrimaryLocator()` to return the shared family key
(e.g., `FilterPanel.StatusDropdown`) without appending the constant suffix. All three
enum constants resolve to the same template locator, substituting their own label at
resolve time.

---

## ElementSupport -- Enum Reflection Utility

`ElementSupport` (package-private in `domain.automation.web.vocabulary.element`) provides three static helpers that
consolidate the `(Enum<?>) this` casts required for enum-based element implementations.

### Methods

| Method | Returns | Use case |
|---|---|---|
| `nameOf(UIElement e)` | `String` -- enum constant name | Display text derivation, debug logging |
| `declaringClassOf(UIElement e)` | `Class<?>` -- the enum class | Locator key construction, file path derivation |
| `ordinalOf(UIElement e)` | `int` -- zero-based enum ordinal | `Listable.getIndex()` default |

### Scope constraint

`ElementSupport` is package-private and contains exactly these three methods. It is not a
general-purpose element utility. Callers outside `domain.automation.web.vocabulary.element` must not use it. Additions
require a new ADR (see ADR-017).

---

## LocatorRoles -- Role Map Construction

`LocatorRoles` (public, in `domain.automation.web.vocabulary.capability`) provides a `roleMap()` builder for constructing
`getAllLocatorRoles()` return values without equality chain boilerplate.

```java
@Override
default Map<ElementRole, String> getAllLocatorRoles() {
    return LocatorRoles.roleMap(
        new LocatorRoles.RoleEntry(ElementRole.TRIGGER,       getPrimaryLocator()),
        new LocatorRoles.RoleEntry(ElementRole.SEARCH_INPUT,  getPrimaryLocator() + ".SEARCH_INPUT"),
        new LocatorRoles.RoleEntry(ElementRole.SEARCH_RESULT, getPrimaryLocator() + ".SEARCH_RESULT"),
        new LocatorRoles.RoleEntry(ElementRole.LIST,          getPrimaryLocator() + ".LIST")
    );
}
```

The returned map is unmodifiable. Insertion order is preserved (backed by `LinkedHashMap`).
`getPrimaryLocator()` returns the first key in the map, so the first `RoleEntry` should
be the primary locator for the element.

`LocatorRoles` is for building role maps only. It does not resolve locators, produce
`LocatorDescriptor` objects, or interact with `LocatorResolvers`.

---

## Capability Identity

Each capability interface returns a canonical `ActionCapability` constant from
`capability()`. This is metadata -- it is used for observability, logging, tracing, and
as the dispatch key in `VoidDSL`'s EnumMap-based capability dispatch (see ADR-013 and the
OOP violations remediation Phase 3 plan).

`capability()` must never be used to select execution behavior. Behavioral execution goes
through the capability interface method directly via polymorphism:

```java
// Correct -- polymorphic dispatch
element.click().perform(engine);

// Forbidden -- capability-enum behavioral dispatch (ADR-013 rule 2)
switch (element.capability()) {
    case CLICKABLE -> ((Clickable) element).click().perform(engine);
}
```

The VoidDSL EnumMap dispatch pattern is a bounded exception: it maps `ActionCapability`
constants to `BiConsumer<UIElement, String>` lambdas for string-key-resolved DSL methods
where the element type is genuinely unknown at compile time. This is metadata-keyed
routing, not behavioral dispatch -- the lambda is the same regardless of capability value.

---

## Invariants

These invariants are enforced by `ElementStructureRulesTest` and must not be violated:

1. **Every UIElement implementation is an enum.** Plain class implementations of `UIElement`
   are prohibited. The `(Enum<?>) this` cast in `ElementSupport` is a framework invariant,
   not a runtime check.

2. **Elements never execute.** An element must not call engine methods, resolve locators
   eagerly, or perform browser interaction. All execution is deferred to `Action.perform()`.

3. **One capability family per element.** An enum constant implements at most one leaf
   capability interface. Implementing both `Clickable` and `Typeable` on the same enum is
   prohibited -- use a composed interface (like `SearchField`) instead.

4. **Capability interfaces contain no execution logic.** Capability default methods return
   `Action` objects. They must not contain waits, retries, conditions, or calls to engine
   methods (ADR-013 rule 1).

5. **`capability()` returns a stable constant.** The return value of `capability()` must be
   the same for every instance of the capability interface. It must not depend on runtime
   state, arguments, or configuration.

6. **`getAllLocatorRoles()` returns an ordered map.** The first entry is the primary locator.
   `getPrimaryLocator()` relies on iteration order. Use `LinkedHashMap`-backed maps only.

7. **The kernel never depends on `elements.*` or `domain.automation.web.*`.** The vocabulary
   packages (`domain.automation.web.vocabulary.*`) and action layer depend on the kernel
   (`core.actions.Action`, `ActionCapability`, `ActionProfile`, `ActionProfiles`) -- never
   the reverse. This was audit finding D1 (the two packages' mutual dependency was proof
   they were one bounded context); `KernelBoundaryRulesTest`'s `kernelPurity`
   (runtime-redesign I2.3, consolidated I2.4) makes it a permanent, enforced ratchet rather
   than a one-time cleanup that could silently regress.

---

## Extension Guide

### Adding a new capability interface

1. Create the interface in `domain.automation.web.vocabulary.capability`.
2. Extend `UIElement` (not `ActionCapabilityProvider` -- see ADR-016).
3. Override `capability()` to return the new `ActionCapability` constant.
4. If the new interface extends two parents with `capability()`, add an explicit
   `@Override default ActionCapability capability()` resolving the diamond.
5. Add default methods that return typed `Action` subclasses.
6. Add the new `ActionCapability` constant to the enum.
7. Add the capability to the hierarchy table in this document.
8. Write a `CapabilityRolesTest` verifying `getAllLocatorRoles()` output.

### Adding a new element to a page

1. Declare the enum constant in the appropriate group enum.
2. Implement the correct capability interface.
3. Add the locator key to the page's `locators.json`.
4. If the element shares a locator template with siblings, implement `LocatorFamily` and
   supply `getArgs()`.
5. Run the locator sync slash command to regenerate the `.properties` template.

---

## Related

- [ADR-008 -- Capability Interfaces](../decisions/accepted/008-capability-interfaces.md) -- why capability interfaces were introduced
- [ADR-013 -- Architectural Layering Principle](../decisions/accepted/013-architectural-layering-principle.md) -- capabilities describe, actions execute
- [ADR-016 -- capability() Ownership Migration](../decisions/accepted/016-capability-ownership-migration.md) -- why ActionCapabilityProvider was removed
- [ADR-017 -- ElementSupport and LocatorRoles Utility Scope](../decisions/accepted/017-element-support-locator-roles.md) -- utility class boundaries
- [Action Layer Architecture](actions.md) -- how actions consume elements
- [Locator Resolution Architecture](locator-resolution.md) -- how locator keys become locators
- [System Overview](system-overview.md) -- full capability table and execution flow
