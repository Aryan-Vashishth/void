# I6 Pre-Implementation Audit

Conducted: 2026-07-30
Branch: initiative/domain-registration
Plan: docs/plan/draft/06 - runtime-redesign/initiative-6-domain-registration.md
ADRs: ADR-021, ADR-021 addendum (Physical Package Topology)

---

## Precondition Gate

All prior-phase gates checked against current main (v0.8.0).

| Phase | Requirement | Status |
|---|---|---|
| I2.2 / I2.4 | Kernel purity gate established; `elements.*` zero kernel imports | PASS |
| I3.1 | `ActionCapability` is an open interface with name-based equality | PASS |
| I4.1 | `EngineRegistrar` SPI; `UIEngineFactory` uses `ServiceLoader` -- no hard `switch` | PASS |
| I4.2 | `EngineBootstrap` carries opaque settings; `core.engine` has zero `core.driver` imports | PASS |
| I5.2 | `FrameworkBootstrap` is domain-neutral (loads utils config; no driver validation) | PASS |
| I7.2 | `LocatorDescriptor` / `LocatorStrategy` in `elements.locator` | PASS |

---

## Findings

### F1 -- DomainRegistrar / EngineRegistrar relationship (design decision, 6.1)

`EngineRegistrar` (I4.1) is an engine-level SPI: one registrar per execution engine
(`SeleniumEngineRegistrar`). `DomainRegistrar` (6.1) is the domain-level contract. The
relationship must be resolved before implementing 6.1.

Candidate: `DomainRegistrar` is a higher-level contract; the Web domain's registrar
delegates internally to `UIEngineFactory` / the existing `EngineRegistrar` chain for
engine selection. This keeps engine multiplicity (Selenium, Playwright) inside the Web
domain's implementation and out of the kernel. The probe domain (6.3) implements
`DomainRegistrar` directly with a trivial in-memory executor, with no engine registrar
involved. This design is consistent with the plan's "generalizing from engine by name to
domain shipping executors" and requires no changes to the existing `EngineRegistrar` SPI.

Resolution: adopt this layering in 6.1. Record in ADR-023 addendum or 6.1 commit note.

### F2 -- `DomainRegistrar` physical location (design decision, 6.1)

`core.engine` currently holds both kernel concepts (`Executor`) and engine-layer SPIs
(`EngineRegistrar`, `UIEngineFactory`). `DomainRegistrar` is at the same SPI layer.
Placing it in `core.engine` is consistent with the existing pattern and avoids a new
package that exists only to hold one interface.

Resolution: place `DomainRegistrar` in `core.engine` for 6.1; it relocates with
`core.engine`'s neutral contracts during 6.4 if the Class Migration Matrix assigns it
outside `domain.automation.web.*` (which it should, being kernel-adjacent neutral
infrastructure).

### F3 -- VOIDBuilder domain selection path (design decision, 6.1)

`VOIDBuilder.start()` currently resolves engine name only (from `.engine()`, System
property, ENV, config, or default "selenium") and calls `UIEngineFactory.create()`.
Post-6.1, it must resolve domain name and pass domain selection to a `DomainRegistrar`
lookup, with the domain's registrar then responsible for executor creation. The engine
name remains a parameter passed into the domain's executor creation path (relevant for
Web, irrelevant for Probe).

Required change: `VOIDBuilder` gains `.domain(String)` method (default `"web"`);
`start()` looks up `DomainRegistrar` by domain name and calls
`DomainRegistrar.createExecutor(EngineBootstrap)`. The existing explicit-engine path
becomes: domain="web" domain registrar internally resolves engine from the same
System/ENV/config chain currently in `UIEngineFactory`.

### F4 -- HARD GATE for 6.4: `DriverFactory.Profile` API surface (unresolved)

`VOIDBuilder.profile(DriverFactory.Profile)` is public API. `DriverFactory.Profile` is
a Selenium-specific type. 6.4 relocates `DriverFactory` to
`domain.automation.web.selenium.driver`, which would break any caller of `.profile()`.

The ADR-021 addendum records two options: (a) re-expose via a stable neutral type (e.g.
`SessionProfile` in kernel), or (b) accept as a breaking change at the 1.0.0 boundary.
This decision is NOT required for 6.1-6.3.

Gate: **6.4 must not begin until this decision is recorded in the ADR-021 addendum.**

### F5 -- D6: `core.utils` cross-domain coupling (classification input for 6.2)

`core.utils.web.*` (`WaitUtils`, `TableHandler`, `KeyValuePairHandler`, `Upload`,
`DOMUtils`, `ElementHighlighter`) imports Selenium and `elements.*`. The 6.2 ownership
audit must classify these explicitly. Per ADR-020 ("managed graveyard") and the
I6 plan's "observability / tooling / legacy-pending-deletion" sweep categories,
`core.utils.web` types are expected to land in `legacy-pending-deletion` (I9.2 target)
rather than relocating to `domain.automation.web.*`.

Note for 6.2: confirm this classification; do not treat the graveyard contents as
web-domain vocabulary requiring physical relocation.

### F6 -- D7: `UIEngine.resolve(Element, role)` leaks element model into engine contract (open finding, feeds 6.2)

`UIEngine` (currently in `core.engine`) has resolution methods that take
`elements.api.UIElement` -- this means `core.engine.UIEngine` depends on `elements.api`.
This is an engine-neutrality concern: the engine-level contract carries web-domain
vocabulary.

The 6.2 ownership audit must classify `UIEngine` and note this dependency. The expected
outcome is that `UIEngine` relocates to `domain.automation.web.*` in 6.4 (as a web-domain
execution contract, not a kernel interface), with `Executor` remaining as the kernel
interface. D7 is not fixed in 6.1-6.3; it is tracked and resolved by correct
classification in 6.2 and relocation in 6.4.

### F7 -- Absorbed 6.4 backlog findings (confirmed, carry to 6.4 planning)

Three findings are already absorbed into 6.4 per the plan (checked 2026-07-24):
- `core-driver-package-selenium-coupling.md` -- `core.driver` relocates to
  `domain.automation.web.selenium.driver` in 6.4.
- `oop-driverfactory-instanceof-preference-dispatch.md` -- fixed inline in 6.4 (low
  cost, touched incidentally).
- `waiter-returns-webdriverwait.md` -- NOT fixed in 6.4 (behavior change); file moves
  with package, violation tracker path updated.

No action needed for 6.1-6.3. Confirm these are still open (not silently resolved by
I7 or I8) before starting 6.4.

---

## Phase Readiness

| Phase | Gate | Status |
|---|---|---|
| 6.1 | All I4.1 / I5.2 preconditions satisfied | READY |
| 6.2 | Awaiting 6.1 completion | NOT YET |
| 6.3 | Awaiting 6.1 and 6.2 completion | NOT YET |
| 6.4 | Awaiting 6.2 matrix (no unassigned rows), 6.3 (probe green), F4 hard gate resolved | NOT YET |

---

## Verdict

**READY TO BEGIN 6.1.**

F1, F2, F3 are design decisions resolved here; they are not blockers. F4 is a hard gate
for 6.4 only. F5 and F6 are classification inputs for 6.2.
