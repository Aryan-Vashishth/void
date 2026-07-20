# Architecture Audit: Generalize Element into Target

Audit date: 2026-07-20
Conducted after: `initiative/engine-decoupling` completion

---

## 1. Context

VOID started as Selenium-only. The engine-decoupling workstream removed the forced
`WebDriver` dependency from the startup pipeline. The next natural question: should the
*domain model* also generalize?

`Element` is the root abstraction for everything the framework can interact with. As
written it is implicitly a *UI element descriptor* -- it encodes:

- Locator keys and external locator files (`getExternalFileName`, `getPrimaryLocator`)
- Role-based locator maps (`getAllLocatorRoles`, `locatorKeyForRole`)
- Enum-only default implementations (`(Enum<?>) this` casts throughout)

The question is whether to introduce a domain-neutral `Target` root above `Element`,
making VOID's domain model independent of the interaction medium.

---

## 2. Current state snapshot

**`Element`** (`elements/api/Element.java`) declares nine methods:

| Method | UI-specific | Enum-specific |
|--------|-------------|---------------|
| `getExternalFileName()` | yes | yes |
| `getPrimaryLocator()` | yes | yes |
| `getSecondaryLocator()` | yes | no (default null) |
| `getArgs()` | no | no |
| `effectiveArgs()` | no | no |
| `getDisplayText()` | no | yes (splits enum name on `_`) |
| `locatorKeyForRole()` | yes | yes |
| `templateFamilyKey()` | yes | no (default null) |
| `getAllLocatorRoles()` | yes | no |
| `NO_ARGS` constant | no | no |

**`UIEngine.resolve(Element, ElementRole, Object...)`** -- takes `Element` directly.

**Capability interfaces** (`Clickable`, `Typeable`, `Selectable`, ...) do NOT extend
`Element`. They are standalone mixins declared alongside it:
`enum Button implements Element, Clickable { ... }`.

**Constraint**: every `Element` default method that derives state from the implementing
type uses `(Enum<?>) this`. `Element` is, in practice, an **enum-only interface** today.

---

## 3. Audit questions and findings

### Q1 -- Naming: is "Target" right?

Yes.

A Target is what the engine acts upon -- the thing being operated on. The name:
- Is generic enough for web elements, API endpoints, mobile components, or file paths
- Is directional: you *target* it, you *resolve* it, you *interact with* it
- Does not collide with existing VOID vocabulary (`Element`, `Action`, `Flow`, `Engine`)

Rejected alternatives:

| Name | Rejection reason |
|------|-----------------|
| `Subject` | Implies test subject, reads as assertive not imperative |
| `Addressable` | Adjective form; adds noise |
| `Entity` | DDD collision -- entity has identity + lifecycle, a descriptor does not |
| `Node` | Collides with DOM/tree vocabulary |
| `Actor` | BDD term for a test participant, not a target |

**Verdict**: `Target` is correct.

---

### Q2 -- Method placement: what belongs on Target?

Only the methods that have zero UI or enum-specific semantics:

| Method | Move to Target? | Reason |
|--------|----------------|--------|
| `getDisplayText()` | Yes -- declared, no default | Every target needs a human-readable label. The enum-underscore-split default stays on `UIElement`. `Target` declares the method with no default, keeping it free of enum assumptions. |
| `getArgs()` | Yes -- with default | Every parameterizable target can carry args. No UI semantics. |
| `effectiveArgs()` | Yes -- with default | Delegates to `getArgs()`; safe anywhere. |
| `NO_ARGS` constant | Yes | Zero semantics; purely a convenience sentinel. |
| `getExternalFileName()` | No | Classpath locator file lookup; UI infrastructure. |
| `getPrimaryLocator()` | No | Locator key string; UI-specific. |
| `getSecondaryLocator()` | No | Fallback locator key; UI-specific. |
| `getAllLocatorRoles()` | No | Role map; UI-specific. |
| `locatorKeyForRole()` | No | Per-role key generation; UI-specific. |
| `templateFamilyKey()` | No | Family locator template; UI-specific. |
| `qualifiedLocatorKey()` (static) | No | Static builder; UI-specific. |

`Target` interface body:

```java
package core.target;

public interface Target {
    Object[] NO_ARGS = new Object[0];

    String getDisplayText();

    default Object[] getArgs() { return NO_ARGS; }

    default Object[] effectiveArgs(Object... overrides) {
        return (overrides != null && overrides.length > 0) ? overrides : getArgs();
    }
}
```

`UIElement` extends `Target` and restores the enum-split `getDisplayText()` default:

```java
public interface UIElement extends Target {
    @Override
    default String getDisplayText() {
        String[] tokens = ((Enum<?>) this).name().split("_");
        // ... existing logic ...
    }
    // all current locator methods remain here unchanged
}
```

---

### Q3 -- Locator concept ownership: does Target know about locators?

No.

`LocatorDescriptor`, `LocatorStrategy`, and `ElementRole` are UI concepts. They encode
how a browser engine finds a DOM node -- XPATH, CSS, ID, NAME are Document Object Model
mechanisms. A future API engine has no CSS selector. A file-system engine has no locator
role.

The resolution pipeline (`UIEngine.resolve(UIElement, ElementRole, Object...)`) is cleanly
scoped to UIEngine + UIElement. It must not migrate upward into Target.

**Verdict**: locators live at the `UIElement` level. `Target` carries no locator semantics.

---

### Q4 -- Hierarchy options

Three options considered:

**Option A: `Target > UIElement`** (two-tier, recommended)

```
Target          (domain-neutral: displayText, args)
  └── UIElement (UI-specific: locators, roles, external file lookup)
```

Clean boundary; each layer has a single responsibility. Capability interfaces remain
standalone mixins on `UIElement` enums. This is the right option given the current codebase.

**Option B: `Target > UITarget > UIElement`** (three-tier, premature)

```
Target
  └── UITarget    (marks UI-addressable; adds getExternalFileName, getPrimaryLocator)
        └── UIElement   (adds role-based locator system)
```

`UITarget` is justified only if a non-role-based UI target exists (e.g., a raw locator
string without a page object enum). No such case exists today. Introduce this tier when
the first concrete use case appears.

**Option C: Marker-only Target**

```
Target (no methods; pure taxonomy anchor)
  └── UIElement   (current Element, renamed)
```

Establishes the taxonomy with zero contract commitment. Acceptable but incomplete: a
marker with no methods provides no polymorphism benefit and defers the contract question
without resolving it.

**Verdict**: Option A.

---

### Q5 -- Package structure

| Type | Package | Note |
|------|---------|------|
| `Target` | `core.target` | Domain-neutral; no dependency on elements, locators, or engines |
| `UIElement` | `elements.api` | Existing package; interface renamed in place |
| Capability interfaces | `elements.api.capability` | No change |
| `ElementRole` | `elements.meta` | No change |
| `LocatorFamily` variants | `elements.api` | No change |

`core.target` has no imports from Selenium, locators, or any engine artifact. It is
the innermost ring in the dependency graph.

The `elements` package name remains appropriate -- `elements.api.UIElement` is
unambiguous. The package scopes it to UI elements; the interface name makes the
contract explicit.

---

### Q6 -- Engine compatibility: how does UIEngine change?

`UIEngine.resolve()` currently takes `Element`. After the rename it takes `UIElement`.
This is a pure symbol rename with no behavioral change.

The signature stays:

```java
LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args);
```

`UIEngine` must NOT be generalized to take `Target` directly, because `ElementRole`
(which it also takes) is UI-specific. Pairing `Target` + `ElementRole` in the same
signature would be a leaky abstraction -- it would expose `ElementRole` at a level above
UI concerns.

If a second domain engine (API, file system) is introduced later:
- It defines its own resolution interface, e.g., `ApiEngine.resolve(ApiTarget, ...)`
  where `ApiTarget extends Target`
- A shared `Engine` superinterface (not `UIEngine`) would hold lifecycle only:
  `initialize()`, `shutdown()`
- `UIEngine` remains scoped to `UIElement`

**Verdict**: `UIEngine` gets `UIElement` in the `resolve()` signature. No other changes.

---

### Q7 -- Clean Architecture alignment

| Layer | Type | Location |
|-------|------|----------|
| Domain | `Target` | `core.target` (no outward imports) |
| Interface adapter | `UIElement` | `elements.api` (imports `ElementRole` from `elements.meta`) |
| Interface adapter | `UIEngine` | `core.engine` (imports `UIElement`, `ElementRole`) |
| Application | `VOID`, `VOIDBuilder` | `core.runtime` |

The dependency direction is correct: adapters know the domain (`UIElement` extends
`Target`), the domain knows nothing about adapters. `Target` in `core.target` with no
framework imports satisfies CA's dependency rule.

The rename makes VOID's existing implicit layer model explicit.

---

### Q8 -- DDD fit

In DDD terms, `Target` is a **Value Object specification** -- not a full entity. It has
no identity field, no lifecycle, and no invariants to protect. It describes how to find
and label something, not what the thing is. DDD recognizes specifications and descriptors
as first-class citizens alongside entities.

The enum implementation pattern (page object enums implementing `UIElement`) is a
**Specification** pattern: each enum constant specifies *which* element on *which* page.
This is valid DDD and does not need to change.

Introducing `Target` adds vocabulary that maps to DDD terminology without forcing a
structural change to the existing specification pattern.

---

### Q9 -- Risks

| Risk | Severity | Mitigation |
|------|----------|-----------|
| Blast radius of Element rename | HIGH | Every page object enum, `UIEngine` implementation, capability interface reference, `LocatorFamily`, and test page object imports `Element`. An IDE-assisted rename propagates in one operation. All changes land in a single commit. |
| `(Enum<?>) this` casts remain in `UIElement` defaults | MEDIUM | `Target` must not carry these casts. Any attempt to implement `Target` with a non-enum class will compile but throw `ClassCastException` at the first `UIElement` default method call. Document `UIElement` as enum-only until the cast constraint is lifted. |
| Speculative abstraction | MEDIUM | No second engine type exists. `Target` hedges against a future that may arrive in a different shape. Option A's minimal scope limits the cost: one new interface, four methods, one new package. |
| Capability interfaces already standalone | LOW | They do not extend `Element`. No declaration changes needed -- only the page object `implements Element` declarations update to `implements UIElement`. |
| `LocatorFamily` subtypes | LOW | They are marker interfaces. If standalone (no `Element` reference), no change. If they reference `Element`, update to `UIElement`. Audit with a targeted grep before Phase 2. |

---

### Q10 -- Alternatives

**Alternative 1: Rename only (`Element` -> `UIElement`), defer Target**
- Pros: zero behavioral change; makes UI-specificity explicit immediately
- Cons: defers generalization; type hierarchy stays flat

**Alternative 2: Marker-only Target, rename deferred**
- Pros: establishes the taxonomy with zero contract commitment
- Cons: no polymorphic benefit; a future caller that takes `Target` cannot call
  `getDisplayText()` without casting

**Alternative 3: Wait until the second engine exists (YAGNI)**
- Pros: requirements for what Target must actually do are known only when two concrete
  engine types exist to validate against
- Cons: retrofitting Target when Playwright arrives costs the same as doing it now;
  the rename will be required regardless

**Alternative 4: Parallel Target hierarchy in a new package, no Element changes**
- Pros: zero migration cost; existing code untouched
- Cons: two hierarchies with no declared relationship; `UIEngine` remains
  `Element`-typed at the interface level indefinitely

**Recommended**: Alternative 1 (rename) and Option A (two-tier hierarchy) together in
one initiative. The rename is low-risk and necessary regardless. Adding the minimal
`Target` shell at the same time pays no additional migration cost and establishes the
taxonomy.

---

### Q11 -- Final verdict

Introduce `Target` with the minimal Option A hierarchy. Correct scope:

1. **Rename `Element` to `UIElement`** -- makes UI-specificity explicit and unambiguous
2. **Introduce `core.target.Target`** with `getDisplayText()` (declared, no default),
   `getArgs()` (default returns `NO_ARGS`), `effectiveArgs()` (default delegates to
   `getArgs()`), and `NO_ARGS` constant
3. **`UIElement extends Target`** -- all existing page object enums inherit `Target`
   transitively; no changes inside enum bodies; only the `implements Element` declaration
   becomes `implements UIElement`
4. **`UIEngine.resolve(UIElement, ElementRole, Object...)`** -- rename only; no
   behavioral change
5. **Do NOT move locator methods to Target** -- they stay on `UIElement`
6. **Do NOT change `ElementRole`, `LocatorDescriptor`, `LocatorStrategy`** -- untouched

See [initiative index](../index.md) for the phase breakdown and commit sequence.
