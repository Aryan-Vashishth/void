# I3 -- Capability Model

Objective: capabilities become an open, declarative vocabulary owned by domains;
the runtime validates against them as opaque requirements; no silent fallback
behavior remains.

---

## Phase 3.1 -- Open the capability set

- **Objective**: `ActionCapability` stops being a closed enum; it becomes an
  extensible set (existing constants preserved with identical identities, logging
  labels, and profile bindings).
- **Motivation**: audit D18; June audit "what cracks first"; ontology I11.
- **Scope / files**: `core/actions/ActionCapability.java` (or successor location
  after 2.2), every capability-typed field and map key, profile-selection lookups.
- **Dependencies**: 2.2 (the type has its final home first); oop-remediation P3/P7
  merged (dispatch and ownership stabilized). Never parallel with I2 phases.
- **Risks**: (arch) reintroducing closure by accident -- any exhaustive iteration or
  ordinal use over the set must be removed in-phase; (compat) enum-specific caller
  code (switch, values(), EnumMap) breaks -- in-repo callers migrated in-phase;
  external callers get CHANGELOG migration notes (Beta-adjacent surface).
- **Rollback**: revert; constants' identities are preserved, so revert is clean.
- **Validation**: suite green; new test: a test-scope custom capability registers,
  binds a profile, and executes with zero edits to runtime-owned files.
- **Exit criteria**: extension test green; no `switch`/`values()` over capabilities
  outside deprecated legacy paths; fitness check added.
- **ADR / docs**: ADR-021 consequences appendix updated; actions.md.
- **Migration notes**: CHANGELOG entry with before/after for callers.

## Phase 3.2 -- Remove the UNKNOWN silent fallback

- **Objective**: an unrecognized capability no longer inherits browser wait hooks
  silently; profile resolution for an unconfigured capability fails fast with an
  actionable message, or uses a profile the action/config names explicitly.
- **Motivation**: June audit failure mode -- wrong behavior applied invisibly to a
  future domain's actions; ontology I10/I12 spirit.
- **Scope / files**: `ActionProfiles` default-selection path; configuration key
  documentation; affected tests.
- **Dependencies**: 3.1.
- **Risks**: (compat) HIGH relative to its size: any existing action relying on the
  silent default changes behavior from "silently hooked" to "explicit or fail". The
  phase inventories every in-repo action's effective profile BEFORE the change and
  asserts the same effective profile AFTER via explicit configuration, making the
  change behavior-neutral for the current codebase.
- **Rollback**: revert commit restores the fallback.
- **Validation**: before/after effective-profile inventory test; suite green.
- **Exit criteria**: no code path selects hooks for a capability it does not know;
  failure message names the capability and the configuration remedy.
- **ADR / docs**: configuration-reference.md updated.
- **Migration notes**: explicit-config requirement documented for custom actions.

## Phase 3.3 -- Neutral capability contract; UI capabilities as domain vocabulary

- **Objective**: the kernel-facing capability contract is neutral (a requirement the
  runtime can validate opaquely, per AD3's decided timing); the fifteen UI
  capability interfaces are declared Web-domain vocabulary. Their action-emitting
  convenience methods remain (they are UI-side API sugar), but the ontology rule is
  encoded in docs and fitness checks: capabilities never execute, and the kernel
  never inspects concrete capabilities.
- **Motivation**: ontology O6 (capability one convenience away from factoryhood),
  Concept Reality Tests corollary (capability = contract, never entity).
- **Scope / files**: capability contract type placement; javadoc contracts;
  package-info declarations of domain ownership; fitness check: kernel does not
  reference concrete capability types.
- **Dependencies**: 3.1, 2.3.
- **Risks**: (arch) minimal code motion, mostly declaration -- the risk is doing too
  much here; generalizing the emission mechanism belongs to a future second-domain
  initiative, not this phase.
- **Rollback**: revert.
- **Validation**: suite green; fitness check green.
- **Exit criteria**: capability ownership documented per interface; kernel
  capability references are contract-typed only.
- **ADR / docs**: elements.md capability table gains an ownership column.
- **Migration notes**: none external.
