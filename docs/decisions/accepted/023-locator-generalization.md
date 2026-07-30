# ADR-023 -- Locator Generalization: Open Strategy Set, Descriptor Ownership, and By-Returning Deprecation

**Date:** 2026-07-30
**Status:** Accepted

---

## Context

The 2026-07 architecture audit (D14, D18, priority-13) identified three locator subsystem gaps:

1. **Closed strategy set (D18):** `LocatorStrategy` was a closed enum (XPATH, CSS, ID, NAME). Adding a
   new strategy (e.g. ACCESSIBILITY_ID for mobile) required editing the enum -- the same OCP violation
   the ActionCapability work fixed in I3.

2. **Descriptor ownership (priority-13):** `LocatorDescriptor` lived in `core.engine`, the neutral
   engine contract package. It is produced by resolvers, carried by actions, and describes a DOM
   locator -- it is web-domain vocabulary, not a neutral engine contract type.

3. **By-returning surface (D14/H1):** `LocatorResolver.resolve()` returns `org.openqa.selenium.By`
   on a non-deprecated surface. The ADR-020 invariant ("no new By-returning resolver calls") could
   not be enforced once the method itself remained as a first-class API.

---

## Decision

### D1 -- LocatorStrategy becomes an open interface

`LocatorStrategy` changes from a closed enum to an interface backed by a package-private record
`NamedStrategy(String name)`. The four named constants (XPATH, CSS, ID, NAME) are preserved as
`static final` fields on the interface, so all existing call sites compile unchanged.

Equality is name-based via the record's structural equals. `LocatorStrategy.of("XPATH")` produces
a value that compares equal to `LocatorStrategy.XPATH` -- the same pattern used by `ActionCapability`
in I3.

**Pattern applied:** open-set constant interface (same as I3.1 ActionCapability).

### D2 -- LocatorDescriptor and LocatorStrategy relocate to elements.locator

Both types move from `core.engine` to `elements.locator`, an intermediate package that acknowledges
their web-domain nature while deferring commitment to a final canonical domain-package path.

Final relocation (to `domain.automation.web.*` or equivalent, once I6 settles the Web domain
package structure) is tracked but out of scope for this initiative.

The relocation introduces `elements.locator.LocatorDescriptor` as a temporary exception in
KERNEL_PURITY_TEMPORARY_EXCEPTIONS, cross-referenced to I9.4 (bridge-method deletion phase).

### D3 -- SeleniumEngine.toBy() uses an open dispatch table, not a switch

`SeleniumEngine.toBy()` is updated to use a `Map<String, Function<String, By>> BY_FACTORIES`
rather than an exhaustive switch over enum values. Any `LocatorStrategy` name not present in
the map throws `IllegalStateException` with a message pointing to the registration site.

This makes SeleniumEngine the single place where new strategies gain Selenium support -- consistent
with the engine-neutrality invariant (ADR-007).

### D4 -- LocatorResolver.resolve() deprecated, not deleted

The five By-returning methods on `LocatorResolver` are marked `@Deprecated(forRemoval = true)`.
Full deletion is deferred to I9.3 because `Via.locator()` and `Interactions` (both already
deprecated) remain callers. Deleting the methods before their callers violates compile safety.

The intent of this phase -- that no new non-deprecated code calls `resolve() -> By` -- is met.
The grep exit criterion for "no non-legacy By-returning callers" is satisfied. I9.3 closes the
method itself.

---

## Consequences

**Positive:**
- `LocatorStrategy` is extensible without editing the framework. Mobile automation layers can
  register `ACCESSIBILITY_ID` without forking core.engine.
- `LocatorDescriptor` is no longer a neutral engine contract type. The engine contract package
  (`core.engine`) is now free of locator vocabulary.
- No new production code paths go through the Selenium `By` surface at the resolver level.
- The `coreEngineHasNoLocatorTypes` ArchUnit check permanently enforces the package boundary.

**Tracked open items:**
- `elements.locator` is an intermediate home -- I6.4 finalizes the package.
- Kernel bridge methods (Action.resolve, HookChainAction) still carry `elements.locator.LocatorDescriptor`
  in their signatures -- I9.4 closes this.
- `LocatorResolver.resolve()` and `ByParser` remain as deprecated code -- I9.3 deletes them with
  the rest of the legacy resolver surface.

**Invariant change:**
ADR-020's "no new By-returning resolver calls" invariant is superseded. The stronger form --
`LocatorResolver` has no non-deprecated By-returning method -- is now the invariant goal, to be
enforced once I9.3 deletes the deprecated methods.
