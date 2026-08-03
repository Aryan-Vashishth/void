# ADR-024 -- Domain Registration: Contract, Web Assembly, Probe Gate, and Physical Relocation

**Date:** 2026-07-31
**Status:** Pending Review (`initiative/domain-registration`)

---

## Context

ADR-021 mandated that Domain become the runtime's extension unit: a registration contract,
the Web domain assembled as its first instance, and a standing machine-checked proof that a
new domain integrates with zero runtime modification. ADR-021's addendum (2026-07-24) further
required that web-owned code physically relocate to a `domain.automation.*` package tree
derived from an audited ownership matrix, not from inference during implementation.

Before I6, four structural gaps remained:

**No registration seam.** Bootstrap knew only one domain -- the web platform -- by construction.
`UIEngineFactory`, `FrameworkBootstrap`, and `VOID` all made implicit assumptions about the web
domain being present. Adding a second domain required editing runtime-owned files.

**No first Domain instance.** Every concept in the model (Target, Interaction, Capability,
Domain) had design; only Domain had no code instance. Without a first instance, the probe
(the falsifier) could not be written, and "domain-neutral" was a claim with no evidence.

**No machine-checked neutrality proof.** Milestone M4's gate was stated as "a second domain
exists and proves extension cost zero edits to the runtime." Without a permanent, CI-visible
test, the claim was only as good as the last manual audit.

**Misplaced physical ownership.** Web-domain code lived in `elements.*`, `core.engine.selenium`,
and `core.driver` -- packages whose names implied neutrality or generality. A reader following
the package layout could not determine which code the web domain owned without consulting
documentation.

---

## Decision

### D1 -- Domain registration contract (I6.1)

A `DomainRegistrar` SPI and `DomainRegistry` factory are introduced in `core.engine`. A domain
announces itself at bootstrap by implementing `DomainRegistrar` and registering via
`ServiceLoader`. `VOIDBuilder` reads the active domain through `DomainRegistry` and wires
vocabulary and execution owner without referencing any concrete domain type.

The Web domain self-registers as the default, preserving the existing `VOID.builder()...start()`
user experience with no required changes. The registration contract is Java-level only (no
classloader plugin machinery); discovery beyond explicit registration is out of scope until a
production second domain exists.

### D2 -- Web domain as first Domain instance (I6.2)

A full ownership sweep of all main-tree packages classifies every type as one of:
kernel / web-domain vocabulary / web-domain implementation / observability / tooling /
legacy-pending-deletion. Web-domain types are further split into two ownership layers per
ADR-021 guardrail rule 8:

- **Vocabulary (logical ownership):** `UIElement` model, capability interfaces, deferred
  actions, roles. This is what the domain defines.
- **Implementation (execution ownership):** `UIEngine` contract, Selenium executor, locator
  resolution, driver internals. This is how the domain executes.

The distinction is load-bearing: a future Playwright executor is a second entry in the
implementation layer against the same vocabulary, not a new domain.

The output is the Class Migration Matrix -- a committed document naming every type to be
relocated in I6.4 with its current FQN, target FQN, visibility, and classification. No file
is moved in this phase; the matrix is the authoritative gate for I6.4.

### D3 -- Probe domain as permanent CI gate (I6.3)

A minimal, test-scope, non-UI store domain (three targets, two capabilities, two
interactions) is registered and executed entirely from test code. The probe exercises the
full integration path: registration, session creation, capability declaration and validation,
interaction dispatch, hook pipeline, and tracing. It was not possible to implement the probe
before I6.1 (no registration seam) and before I5 (bootstrap required `driver.properties`).

The probe is permanently in CI. Removing it or causing it to fail is a build failure, not
a code-review concern. Its test name identifies it as a neutrality regression gate. No
runtime-owned `src/main/java` file is edited to enable the probe -- this is the gate's own
falsification criterion, checked by inspection at each green run.

### D4 -- Physical package relocation (I6.4)

Every type in the Class Migration Matrix physically relocates to `domain.automation.web.*`,
sub-packaged by ownership layer:

| Sub-tree | Contents |
|---|---|
| `domain.automation.web.vocabulary.element` | UIElement, locator families, ElementSupport |
| `domain.automation.web.vocabulary.capability` | Clickable, Typeable, Selectable, and 12 others |
| `domain.automation.web.vocabulary.actions` | ElementAction, ElementActions, 21 concrete actions |
| `domain.automation.web.vocabulary.role` | ElementRole, EnumClassRegistry |
| `domain.automation.web.locator` | LocatorDescriptor, LocatorStrategy, NamedStrategy |
| `domain.automation.web.engine` | UIEngine, UIEngineFactory, EngineRegistrar |
| `domain.automation.web.resolve.api` | LocatorResolver, LocatorResolvers, LocatorContext, LocatorRequest, ConventionalLocatorPath, LocatorPaths |
| `domain.automation.web.resolve.json` | JsonLocatorReader, JsonTreeBuilder, PropertiesIndex, JsonNodeLookup |
| `domain.automation.web.resolve.parser` | ByParser, ByPrefixStrategy |
| `domain.automation.web.resolve.properties` | PropertiesFileLocatorReader |
| `domain.automation.web.resolve.source` | LocatorSource, LocatorSourceRegistry, HardcodedLocatorSource, JsonLocatorSource, PropertiesLocatorSource, LayeredPropertiesLocatorSource |
| `domain.automation.web.selenium` | SeleniumEngine, SeleniumEngineRegistrar |
| `domain.automation.web.selenium.driver` | SeleniumDriverFactory, SeleniumDriverContext, SeleniumDriverManager, Waiter |

The move is pure relocation: no method body is modified, no visibility is widened to resolve
a compile error caused by the move. Any compile error from the relocation is resolved by
fixing the dependency direction, not by adding `public`.

### D5 -- `SessionProfile` as the stable session configuration type (I6.4 preamble)

`DriverFactory.Profile` was on the public surface via `VOIDBuilder.profile(DriverFactory.Profile)`
and `VOID.start(DriverFactory.Profile)`. Relocating `DriverFactory` to a domain-implementation
package while keeping these bridge methods would either: (a) expose a domain-implementation
type through the kernel facade, or (b) require a deprecated re-export shim. Neither is
acceptable.

`SessionProfile` is introduced in `core.runtime` as a plain, kernel-owned value object
carrying the same fields (`Properties props`, `String name`). The two deprecated bridge
methods are removed entirely; callers migrate to `VOIDBuilder.profile(SessionProfile)` or
the new `SessionProfile.fromProperties(...)` factory.

### D6 -- Selenium driver classes renamed to reflect Selenium scope (I6.4)

`DriverFactory`, `DriverContext`, and `DriverManager` are renamed to `SeleniumDriverFactory`,
`SeleniumDriverContext`, and `SeleniumDriverManager`. The old names implied generality ("any
driver") while the implementations were exclusively Selenium. The rename makes the scope
visible at the call site; it is a breaking change aligned to the 1.0.0 boundary.

---

## Consequences

**Positive:**

- Adding a second domain (REST, CLI, Database) requires implementing `DomainRegistrar` and
  creating a services file entry -- no runtime-owned file is modified. The probe demonstrates
  this property holds; the CI gate enforces it permanently.
- Web-domain code is physically separated from kernel and cross-concern code. The package
  layout now mirrors the ownership model: a reader following the imports can determine which
  code the web domain owns without consulting documentation.
- Fitness checks enforcing the kernel/domain boundary are now in their final post-relocation
  form: `domain.automation.web.vocabulary.*` is provably Selenium-free; kernel packages are
  provably free of web-vocabulary and Selenium imports.
- `LocatorDescriptor` and `LocatorStrategy` complete their journey from `core.engine`
  (I7.2 intermediate) to their canonical domain package. The I7 audit gap G3 is closed.

**Negative / tracked:**

- This is a breaking change by design, aligned to the 1.0.0 boundary. No package-level
  bridges are provided (the migration guide in CHANGELOG.md documents the FQN mapping).
  User page-object code referencing `elements.*`, `core.driver.*`, or `core.engine.selenium.*`
  must update imports.
- Two kernel purity exceptions remain after I6.4: `domain.automation.web.engine.UIEngine`
  (kernel bridge methods, closes I9.4) and `domain.automation.web.locator.LocatorDescriptor`
  (kernel bridge methods, closes I9.4). Both are named and cross-referenced in
  `KERNEL_PURITY_TEMPORARY_EXCEPTIONS`.

---

## Alternatives considered

**Incremental relocation (move packages one at a time over multiple releases).** The
ADR-021 addendum explicitly chose a single-release wave for web-domain relocation to avoid
two separate compatibility breaks. Splitting the move would have forced users to update
imports twice and complicated the fitness-check transition (rules would need to accept both
old and new paths during the intermediate period).

**Keep `DriverFactory.Profile` on the public API surface.** Keeping the bridge would have
exposed `domain.automation.web.selenium.driver.SeleniumDriverFactory.Profile` -- a
domain-implementation nested type -- through `VOIDBuilder`, the kernel facade. This would
have re-entrenched the exact coupling the relocation was designed to remove.

**Make `elements.*` a permanent alias package.** Re-exporting types from `domain.*` through
`elements.*` via deprecated aliases would have preserved compile compatibility but would have
left the `elements` name permanently on the public surface. The 1.0.0 boundary is the right
time to break it cleanly; the migration guide covers the update.
