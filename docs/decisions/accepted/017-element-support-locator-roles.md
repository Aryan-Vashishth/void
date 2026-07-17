# ADR-017 -- ElementSupport and LocatorRoles Utility Scope

**Date:** 2026-07-17
**Status:** Accepted

---

## Context

### Scattered (Enum<?>) casts

Four methods in `Element.java` and one in `ElementAction.java` contain direct
`(Enum<?>) this` casts:

```java
// Element.java -- getExternalFileName, getPrimaryLocator, getDisplayText, qualifiedLocatorKey
Enum<?> e = (Enum<?>) this;
Class<?> enumClass = e.getDeclaringClass();

// ElementAction.java -- elementLabel
if (this.element() instanceof Enum<?> e) { return e.name(); }
```

These casts encode a convention (all Element implementations are enums) as unguarded
runtime assumptions. The framework enforces this convention through `ElementStructureRulesTest`
but the casts themselves have no compile-time guarantee. If a non-enum ever implements
`Element`, the cast throws `ClassCastException` with no actionable message.

There is also duplication: `getDeclaringClass()` and `e.name()` are derived independently
in each method rather than delegated to a shared utility.

### Equality chains in locator role methods

`SearchableDropdown.getAllLocatorRoles()` uses a chain of equality checks to assign
`RoleEntry` labels to four locator roles, producing O(n^2) comparison growth as roles
are added:

```java
if (!btn.equals(input) && !btn.equals(searchResult) && !btn.equals(option)) {
    // btn is trigger
}
```

`SearchField.getAllLocatorRoles()` has a similar but smaller pattern. Both methods
grow proportionally in complexity with every new role, and both encode the same structural
idea (assign semantic labels to a set of roles) without a shared abstraction.

---

## Decision

**Introduce two focused utility classes: `ElementSupport` and `LocatorRoles`.**

### ElementSupport

`ElementSupport` is a **package-private final utility class** in `elements.api`. Its scope
is strictly enum-reflection helpers for Element implementations.

```java
final class ElementSupport {
    private ElementSupport() {}

    static String nameOf(Element e) {
        return ((Enum<?>) e).name();
    }

    static Class<?> declaringClassOf(Element e) {
        return ((Enum<?>) e).getDeclaringClass();
    }

    static int ordinalOf(Element e) {
        return ((Enum<?>) e).ordinal();
    }
}
```

Callers inside `elements.api` replace their direct casts with delegating calls:

```java
// Before
Enum<?> e = (Enum<?>) this;
Class<?> enumClass = e.getDeclaringClass();

// After
Class<?> enumClass = ElementSupport.declaringClassOf(this);
```

### LocatorRoles

`LocatorRoles` is a **package-accessible utility class** in `elements.api`. It provides
a `roleMap()` builder that eliminates equality chains in `getAllLocatorRoles()` implementations:

```java
public final class LocatorRoles {
    private LocatorRoles() {}

    public record RoleEntry(ElementRole role, String key) {}

    public static Map<ElementRole, String> roleMap(RoleEntry... entries) {
        LinkedHashMap<ElementRole, String> map = new LinkedHashMap<>();
        for (RoleEntry entry : entries) {
            map.put(entry.role(), entry.key());
        }
        return Collections.unmodifiableMap(map);
    }
}
```

Capability interfaces use `LocatorRoles.roleMap()` in `getAllLocatorRoles()`:

```java
// Before -- SearchableDropdown
@Override
default Map<ElementRole, String> getAllLocatorRoles() {
    String trigger = getPrimaryLocator();
    String input = ...;
    String result = ...;
    String option = ...;
    // four equality checks to build map
}

// After
@Override
default Map<ElementRole, String> getAllLocatorRoles() {
    return LocatorRoles.roleMap(
        new LocatorRoles.RoleEntry(ElementRole.TRIGGER, getPrimaryLocator()),
        new LocatorRoles.RoleEntry(ElementRole.SEARCH_INPUT, ...),
        new LocatorRoles.RoleEntry(ElementRole.SEARCH_RESULT, ...),
        new LocatorRoles.RoleEntry(ElementRole.LIST, ...)
    );
}
```

---

## Scope constraints

### ElementSupport scope invariant

`ElementSupport` contains exactly three methods: `nameOf`, `declaringClassOf`, `ordinalOf`.
It must never be extended beyond enum reflection. It is not a general-purpose element
utility, not a capability resolver, and not a locator helper.

Additions to `ElementSupport` require an explicit architectural review. The class is
package-private by design -- callers outside `elements.api` must not be added without
justification in a new ADR.

### LocatorRoles scope invariant

`LocatorRoles` is for building `getAllLocatorRoles()` return values. It is not a
replacement for `ElementRole`, not an extension of the locator resolution pipeline, and
not a factory for `LocatorDescriptor` objects. Its `roleMap()` method produces an
unmodifiable map -- callers must not cast to mutable.

### Separation between ElementSupport and LocatorRoles

These two classes exist for different purposes and must not be merged:

| Utility | Purpose | Callers |
|---|---|---|
| `ElementSupport` | Enum reflection for Element implementations | `Element.java`, `ElementAction.java`, `Listable.java` |
| `LocatorRoles` | Locator role map construction | `SearchableDropdown.java`, `SearchField.java` |

A call site that uses `ElementSupport` is resolving enum identity. A call site that uses
`LocatorRoles` is building a locator role map. These are distinct concerns; keeping them
in separate classes preserves that distinction for readers.

---

## Reasoning

### Package-private for ElementSupport

`ElementSupport` is package-private because its `(Enum<?>) this` cast is a framework
invariant that should not be visible or callable outside the element API. External code
that needs to determine an element's name or declaring class should work with the `Element`
interface methods (`getDisplayText()`, etc.) or enum polymorphism, not through a raw
reflection helper.

### Not a single "ElementUtils" class

Merging `ElementSupport` and `LocatorRoles` into one class would obscure the two
separate concerns. `ElementSupport` is about what an element IS (its enum identity).
`LocatorRoles` is about what an element EXPOSES (its locator map). Keeping them separate
means a future reader can understand each class's purpose from its name alone.

### ordinalOf and Listable.getIndex()

`ElementSupport.ordinalOf()` enables a non-trivial default on `Listable.getIndex()`. The
default uses ordinal to assign a zero-based list index -- consistent with natural enum
ordering. This replaces the hardcoded `return 0` previously in `Selectable.getIndex()`,
but only after a deliberate decision (see post-plan audit Phase 2 gap note on
`Selectable.getIndex()` hardcoded `return 0`). The resolution of that decision must be
documented in the Phase 2 post-implementation validation.

---

## Consequences

- `ElementSupport.java` created in `elements.api` (package-private)
- `LocatorRoles.java` created in `elements.api` (public)
- Four `(Enum<?>) this` casts in `Element.java` replaced with `ElementSupport` calls
- `ElementAction.elementLabel()` `instanceof Enum<?>` check replaced with
  `ElementSupport.nameOf()` (or delegated through `element.getDisplayText()`)
- `SearchableDropdown.getAllLocatorRoles()` and `SearchField.getAllLocatorRoles()` use
  `LocatorRoles.roleMap()`
- `Listable.getIndex()` has a default using `ElementSupport.ordinalOf()`
- `ElementSupport` scope: exactly three methods, package-private, not callable from
  action, DSL, or engine layers
- `LocatorRoles` scope: one `roleMap()` method plus `RoleEntry` record, no execution logic

---

## Related

- [ADR-008 -- Capability Interfaces](008-capability-interfaces.md) -- element and capability structure
- [ADR-016 -- capability() Ownership Migration](016-capability-ownership-migration.md) -- related Phase 2 decision
- [OOP Violations Remediation Phase 2](../../plan/draft/oop-violations-remediation/phase-2-element-interface.md)
- [OOP Violations Remediation Phase 4](../../plan/draft/oop-violations-remediation/phase-4-infrastructure.md)
