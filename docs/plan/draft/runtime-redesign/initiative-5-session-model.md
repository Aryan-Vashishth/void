# I5 -- Session Model

Objective: Session becomes the first-class, domain-neutral unit of lifetime,
identity, and isolation; bootstrap and configuration stop being Selenium-gated.

---

## Phase 5.1 -- Session contract

- **Objective**: the session concept (today: `VOID` facade + `SessionContext`)
  gains a neutral definition per ADR-021: identity, lifecycle
  (create/active/shutdown), its bound execution owner (cardinality per AD1), and
  its environment (the Context/Environment fold from the ontology). `VOID` holds
  and delegates to it; external API unchanged.
- **Motivation**: ontology recommendation 2; audit Session-runtime context overlap.
- **Scope / files**: `core/context/SessionContext.java` evolution or successor;
  `core/runtime/VOID.java`, `VOIDBuilder.java` internals; `EngineBootstrap`'s
  remaining role absorbed here (its ledger closure).
- **Dependencies**: 4.3 (sessions bind the neutral contract), 0.1 (AD1 ruling).
- **Risks**: (arch) facade-to-session responsibility smearing -- the phase defines
  exactly which lifecycle state lives on Session vs facade, per ADR-021; (compat)
  none external (facade surface frozen this phase).
- **Rollback**: revert internals; facade unchanged makes this low-risk.
- **Validation**: suite green including multi-session demo; session identity
  appears in traces/logs.
- **Exit criteria**: one place owns session lifecycle; ExecutionContext remains
  deprecated-untouched (deleted 9.3).
- **ADR / docs**: system-overview session section.
- **Migration notes**: none.

## Phase 5.2 -- Bootstrap de-Seleniumization

- **Objective**: `FrameworkBootstrap` no longer hard-fails on `driver.properties`
  (audit C4); framework-level init validates only neutral configuration; domain
  and engine configuration is validated by the registered domain/executor at
  session creation.
- **Motivation**: a non-browser domain cannot currently start the framework;
  bootstrap is the outermost boundary and must be neutral first.
- **Scope / files**: `core/bootstrap/FrameworkBootstrap.java`; config validation
  relocation toward the selenium registration path; `ConfigLoader` call sites.
- **Dependencies**: 5.1, 4.1 (registration exists to relocate validation into).
- **Risks**: (compat) HIGH visibility failure-mode change: a missing
  `driver.properties` currently fails at init, after this phase it fails at web
  session creation -- same run, later moment, different message; the phase
  preserves fail-fast-ness (still before any interaction executes) and documents
  the new failure point; (arch) mutable static Properties exposure (H6) is NOT
  fixed here -- scoped out to keep one objective, tracked in backlog.
- **Rollback**: revert; the old gate returns.
- **Validation**: new test: bootstrap succeeds with no driver.properties when no
  web session is requested; existing tests green (driver.properties present).
- **Exit criteria**: `grep -n "driver.properties" src/main/java/core/bootstrap`
  empty; failure message parity test for the web path.
- **ADR / docs**: configuration-reference.md bootstrap section.
- **Migration notes**: CHANGELOG: failure point moved, same guarantees.

## Phase 5.3 -- Session as interaction subject

- **Objective**: session-level operations (navigate, current-url query, refresh)
  are expressible as kernel interactions whose subject is the Session (ontology
  O4); the `VOID` facade methods remain as conveniences delegating into the
  pipeline, unifying the currently out-of-band facade executions with the single
  execution model.
- **Motivation**: ontology failure-analysis row "Subjectless Interactions"; "never
  introduce a second execution model" -- today facade methods bypass the pipeline,
  which IS a second model in miniature.
- **Scope / files**: kernel subject-binding allowance (from 8.2 or minimal
  precursor here -- exact split decided at activation, one owner only); session
  operation types on the Web-domain side; facade delegation.
- **Dependencies**: 5.1, 4.4. Coordinate with 8.2 (multi-subject binding) --
  whichever activates first carries the subject-binding groundwork; the other
  consumes it.
- **Risks**: (arch) hook semantics for session-subject interactions (no locator
  descriptor exists) must be defined, not defaulted -- explicit no-descriptor hook
  contract; (compat) facade behavior byte-identical; verified by comparing
  before/after logs for the demo suite.
- **Rollback**: facade reverts to direct engine calls (single commit).
- **Validation**: demo suite behavior parity; traces now show session operations as
  occurrences.
- **Exit criteria**: every facade operation routes through the one pipeline;
  no direct engine invocation from the facade except via delegation types.
- **ADR / docs**: system-overview execution flow updated.
- **Migration notes**: none external.

## Phase 5.4 -- Configuration identity split

- **Objective**: neutral runtime configuration (logging, profiles, session
  defaults) is separated in documentation and loading precedence from web/driver
  configuration (`driver.properties` becomes explicitly the Web domain's file);
  no new config framework -- ownership and naming clarity only.
- **Motivation**: audit "configuration identity" risk: the framework's ignition
  file is named after one platform.
- **Scope / files**: `ConfigLoader` documented layering, configuration-reference
  doc restructure by owner, key-prefix conventions recorded; file renames are OUT
  of scope (compat).
- **Dependencies**: 5.2.
- **Risks**: (compat) none -- documentation and precedence clarification; any key
  behavior change is out of scope.
- **Rollback**: revert docs.
- **Validation**: config-resolution tests unchanged and green.
- **Exit criteria**: every config key has a documented owner (runtime / web domain /
  logging); no neutral component documents a driver key.
- **ADR / docs**: configuration-reference.md is the deliverable.
- **Migration notes**: none.
