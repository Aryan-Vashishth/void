# I4 -- Execution Boundary

Objective: the engine contract is sealed against its implementations and against
driver infrastructure; a neutral execution-owner contract (name per ADR-021/AD2)
sits above UIEngine; kernel edges are retyped against it with bridged compatibility.

## Program context

**Why this initiative exists.** The execution seam is the reason the ontology
review happened: execution ownership was the five-concept model's orphaned
responsibility (O1), and in code the seam is soft in both directions -- the
contract package depends on its Selenium implementation and on driver
infrastructure (audit D2, D3), and the kernel's central signature names the UI
domain (`perform(UIEngine)`). Every promise the word "runtime" makes routes
through this initiative.

**Why it is sequenced here.** It needs I2's kernel purity (so the neutral contract
has a clean place to sit) and I3's capability contracts (so dispatch validation
has neutral types to reference). It unblocks everything downstream: sessions bind
execution owners (I5), domains ship them (I6), and the descriptor can only leave
the neutral edge once that edge exists (7.2).

**What architectural boundary it owns.** Two lines: contract vs implementation
(engine registry, bootstrap decoupling, driver re-homing) and the kernel's
execution-facing edge (the retyped signatures). Phase 4.3 carries the roadmap's
single highest architectural risk -- one UI method on the neutral contract poisons
neutrality permanently -- and that risk is owned here, nowhere else.

**What it deliberately does not own.** Session lifecycle (I5), domain
registration (I6), and the deaths of the bridges it creates (9.4). It never
promotes a UIEngine method to the neutral contract speculatively: promotion
requires a second domain's demand (extension before modification).

---

## Phase 4.1 -- Engine registry (absorbs oop-remediation P8; from superseded runtime-kernel-boundary phase 2, part 1)

- **Objective**: `UIEngineFactory` stops compile-time-referencing `SeleniumEngine`;
  engines register by name into a registry; unknown names fail fast listing what is
  registered.
- **Motivation**: audit D2; P8; DIP at the seam whose purpose is inversion.
- **Scope / files**: `core/engine/UIEngineFactory.java`; registration point beside
  `SeleniumEngine`; `VOIDBuilder` trigger path.
- **Dependencies**: 0.1, 0.2. Coordination: this phase OWNS P8; the
  oop-remediation draft marks it absorbed.
- **Risks**: (arch) eager vs lazy registration affects startup ordering -- the phase
  must preserve current single-session and multi-session startup behavior exactly;
  (compat) none for callers (factory signature unchanged).
- **Rollback**: revert to switch (single-commit change).
- **Validation**: suite green including multi-session demo; fitness check: no
  `engine.selenium` import in the contract package's factory.
- **Exit criteria**: registering a second engine requires zero edits in
  `core.engine`; P8 closed in the violations index.
- **ADR / docs**: oop-principles.md P8 status; core-packages.md.
- **Migration notes**: none.

## Phase 4.2 -- EngineBootstrap decoupling (part 2 of the same seam)

- **Objective**: `EngineBootstrap` (ADR-018's acknowledged migration debt) stops
  carrying `DriverFactory.Profile`; it carries the engine name plus opaque,
  engine-owned settings; `SeleniumEngine` derives its own profile internally.
  `core.engine` reaches zero `core.driver` imports.
- **Motivation**: audit D3; ADR-018 debt note; contract packages must not require
  WebDriver-world on the classpath.
- **Scope / files**: `core/engine/EngineBootstrap.java`,
  `core/engine/selenium/SeleniumEngine.java`, `core/runtime/VOIDBuilder.java`.
- **Dependencies**: 4.1.
- **Risks**: (arch) settings-passing must not become a stringly-typed dumping
  ground -- the opaque settings are engine-owned by contract, documented as such;
  (compat) `VOIDBuilder` public surface unchanged; deprecated `VOID.start(Profile)`
  path keeps working through the builder delegation.
- **Rollback**: revert; ADR-018 shape restored.
- **Validation**: suite green; fitness check: zero `core.driver` imports in
  `core.engine` excluding the selenium subpackage.
- **Exit criteria**: check green; EngineBootstrap's ledger row closed (its debt
  retired; the type itself survives only until the Session contract of 5.1 absorbs
  its role, tracked there).
- **ADR / docs**: ADR-018 addendum note in ADR-021 consequences.
- **Migration notes**: none external.

## Phase 4.3 -- Neutral execution-owner contract

- **Objective**: introduce the contract ADR-021 named (AD2) above `UIEngine`:
  lifecycle (initialize/shutdown) and dispatch entry, typed against neutral concepts
  only (Target descriptions opaquely, occurrence/trace, Session). `UIEngine` becomes
  the Web domain's specialization of it. No caller is forced to migrate yet.
- **Motivation**: ontology O1/AD2; the merged target-draft's future-watch item
  ("shared Engine superinterface") lands here with its trigger condition met by
  this roadmap itself.
- **Scope / files**: new contract type(s) in the kernel-adjacent neutral area;
  `UIEngine` extends/adapts; `UIEngineFactory`/registry typed to the neutral
  contract internally.
- **Dependencies**: 4.1, 4.2, 2.4 (kernel purity gate defines where the contract
  may live), 0.1 (the name).
- **Risks**: (arch) THE central risk of the roadmap -- putting one UI method on the
  neutral contract poisons domain neutrality permanently; mitigation: the contract
  starts minimal (lifecycle + dispatch + identity), and the fitness check forbids
  it importing anything UI; anything else stays on UIEngine until a second domain
  demands promotion (extension before modification). The minimality rule is also
  the LSP guard: any method a non-web executor could only implement by throwing
  does not belong on the neutral contract; (compat) none -- additive.
- **Rollback**: delete the new types; UIEngine reverts to standalone.
- **Validation**: suite green; fitness check: neutral contract imports neither
  Selenium nor `elements.*` nor UI vocabulary.
- **Exit criteria**: UIEngine is-a execution owner; registry operates on the
  neutral type; zero behavior change.
- **ADR / docs**: system-overview execution architecture section.
- **Migration notes**: none yet (4.4 migrates callers).

## Phase 4.4 -- Kernel retyping with bridges

- **Objective**: kernel edges (`Action.perform`, `FlowExecutor`, hook signature,
  profile application) accept the neutral execution-owner contract; UIEngine-typed
  overloads remain as deprecated bridges so every existing action, hook, and test
  compiles unchanged.
- **Motivation**: audit "Action.perform(UIEngine) type pin" (June audit crack #3);
  the kernel cannot be domain-neutral while its central signature names UI.
- **Scope / files**: kernel contract signatures; concrete UI actions (now UI-side
  after 2.2) migrate to the neutral parameter where their logic allows, staying on
  UIEngine methods internally via their domain typing; all in-repo tests migrated
  off bridges except bridge-verification tests.
- **Dependencies**: 4.3, 2.4, 3.3. Never parallel with I2/I3.
- **Risks**: (arch) accidental double execution model -- bridges must delegate, not
  duplicate (one pipeline, two signatures); (compat) Beta-tier churn is expected
  and allowed; Stable hook tier is protected by the bridge signature.
- **Rollback**: bridges make rollback incremental -- reverting the retyping commit
  restores UIEngine-typed edges without touching 4.3's additive types.
- **Validation**: suite green; bridge-delegation test (both signatures reach the
  same execution path); fitness check: kernel signatures reference the neutral
  contract only.
- **Exit criteria**: no in-repo caller uses a bridge except its tests; Migration
  Ledger rows added (bridges die 9.4).
- **ADR / docs**: actions.md, hooks-pipeline.md.
- **Migration notes**: CHANGELOG with signature migration table.

## Phase 4.5 -- Driver subsystem re-homed as platform internals

- **Objective**: `core.driver` (DriverFactory, DriverContext, DriverManager, Waiter)
  is declared and enforced as Selenium-executor internals: only the selenium engine
  package may import it. Physical package move is optional and decided at
  activation; the dependency rule is the deliverable.
- **Motivation**: audit D6/D14 partial, backlog finding
  `core-driver-package-selenium-coupling.md`; platform vocabulary must stop
  presenting as framework infrastructure.
- **Scope / files**: import cleanup in `core.utils` (Waiter/driver references),
  `core.runtime` (post-4.2 residue), fitness check addition; possibly package move.
- **Dependencies**: 4.2; `core.utils` cleanup coordinates with 9.2 (only the driver
  edges move here; broader utils dismantling stays in 9.2).
- **Risks**: (compat) DriverFactory/DriverManager are used directly by some test
  infrastructure -- those call sites are Selenium-specific by definition and may
  keep importing it FROM the platform side; the rule constrains direction, not
  existence.
- **Rollback**: revert; rule removal.
- **Validation**: suite green; fitness check: `core.driver` imported only from the
  selenium engine boundary (and its own tests).
- **Exit criteria**: check green; backlog violation closed.
- **ADR / docs**: core-packages.md driver section rewritten as platform-internal.
- **Migration notes**: CHANGELOG note for direct DriverFactory users.
