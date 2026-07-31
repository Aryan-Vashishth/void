# I6 -- Domain Registration

Objective: Domain becomes the unit of extension in code, not only in docs: a
registration contract, the Web domain assembled as its first instance with its
own physical package root, and a standing machine-checked proof that a new
domain integrates with zero runtime modification.

**Scope expanded 2026-07-24** (ADR-021 addendum, "Physical Package Topology"):
Domain assembly now includes physically relocating web-owned code under
`domain.automation.web.*`. The expansion is ownership-audit-driven -- 6.2
produces the migration matrix, 6.4 executes it -- not a mechanical move of
existing packages. See the ADR addendum for the full decision and rationale.

## Program context

**Why this initiative exists.** Domain is the ontology's extension unit, and
"extensibility without modifying the runtime" is the roadmap's headline claim --
but a claim with no registration seam and no second instance is a slogan. This
initiative gives the claim a mechanism (6.1), a first real occupant (6.2, the Web
domain), and a permanent falsifier (6.3, the probe): after M4 the claim is a CI
check, not a sentence in a README.

**Why it is sequenced here.** It consumes nearly everything before it: open
capabilities (I3), neutral dispatch (I4), neutral bootstrap (I5). Sequencing it
earlier would force the registration contract to be designed against surfaces
still in motion; sequencing it later would delay the M4 gate that tells us the
preceding five initiatives actually worked.

**What architectural boundary it owns.** The runtime/extension boundary: how a
domain announces its vocabulary and execution owners, and the standing proof that
crossing that boundary requires no runtime edits (ontology invariants I1, I2). As
of the 2026-07-24 expansion, it also owns the physical package boundary between
domain-owned code (`domain.automation.<domain>.*`) and kernel-neutral code (stays
outside that tree, exact root decided by the 6.2 audit).

**What it deliberately does not own.** Any production second domain -- REST or CLI
built for real would double the roadmap (cross-initiative risk #7) and becomes its
own initiative after M4. No plugin-discovery machinery (classloaders, SPI, module
scanning): explicit registration only, until a real ecosystem demands more. And no
new content or behavior change -- 6.2's audit and 6.4's relocation move existing
types to their ownership-correct package; they invent nothing and change no method
body (pure relocation, per guardrail rule 1). Kernel-neutral code is not forced
into `domain.automation` merely for tree symmetry -- a package existing only to
mirror the domain tree's shape is the "empty speculative abstraction" the
stability rules already forbid.

---

## Phase 6.1 -- Domain registration contract

- **Objective**: a neutral contract through which a domain announces itself at
  bootstrap: its name, its execution-owner factory/binding, its configuration
  validation hook (relocated there by 5.2), and its vocabulary declaration point.
  Builds on the 4.1 engine registry, generalizing registration from "engine by
  name" to "domain shipping executors."
- **Motivation**: ontology relationship "Domain registers into Runtime (additive,
  at bootstrap)"; invariant I2.
- **Scope / files**: new registration contract in the neutral area; 4.1 registry
  evolves underneath it; `VOIDBuilder` selection path reads domain+engine.
- **Dependencies**: 4.1-4.4, 5.1, 5.2.
- **Risks**: (arch) inventing a plugin framework -- the contract is a Java-level
  registration seam, not a classloader/SPI system; discovery mechanics beyond
  explicit registration are out of scope until a real second domain exists
  (stability rule 4); (compat) none -- additive.
- **Rollback**: delete contract; registry remains as in 4.1.
- **Validation**: suite green; registration of the web domain (6.2) is the real
  test.
- **Exit criteria**: runtime bootstrap consults only the registration surface to
  learn what domains exist.
- **ADR / docs**: system-overview gains the Domain section.
- **Migration notes**: none.

## Phase 6.2 -- Web domain assembly and ownership audit

- **Objective**: everything UI-specific that previous initiatives pushed out of the
  kernel is declared and registered as the Web domain -- the first Domain instance.
  Wiring and declaration for the content already moved in I1-I4, I7, plus (2026-07-24
  expansion) the ownership audit that decides physical package placement for 6.4.
  The declaration distinguishes the domain's two ownership layers explicitly
  (guardrail rule 8): **logical ownership** -- the vocabulary the Web domain
  defines (UIElement model, UI capabilities, concrete UI interactions, roles) --
  versus **implementation ownership** -- the realizations it contains (the web
  execution contract and its Selenium executor, locator resolution, driver
  internals). The distinction is load-bearing: a future Playwright executor is a
  second entry in the Web domain's implementation layer against the same
  vocabulary, not a new domain.
- **Motivation**: the ontology's Domain concept must have one real instance before
  the probe (6.3) can prove the second costs no runtime edits. The ownership audit
  is motivated by the ADR-021 addendum: physical relocation must follow discovered
  ownership, not be inferred during implementation.
- **Scope / files**: web-domain registration implementation; package-info ownership
  declarations; bootstrap default (web registered by default so existing user
  experience is unchanged); a Class Migration Matrix (current type, current
  package, target package, visibility, reason) covering every type the
  full-assignment sweep classifies as web-domain, plus Current/Target Architecture
  Maps and a dependency-change list (remain / disappear / introduced edges) --
  the planning artifacts 6.4 executes against. No file is moved in this phase.
- **Dependencies**: 6.1, and the content phases: 2.3, 3.3, 4.5, 7.2.
- **Risks**: (compat) zero behavior change required -- existing tests are the
  proof; default registration preserves the "it just works" experience; (arch)
  leftover strays -- anything that cannot be assigned to web-domain or kernel at
  this point is a missed finding; the phase includes a full-assignment sweep
  (every main-tree package maps to kernel / web domain / observability /
  tooling / legacy-pending-deletion, and web-domain rows are further classified
  as vocabulary vs implementation per guardrail rule 8); (arch) matrix
  incompleteness -- 6.4 may not begin against a matrix with unassigned rows.
- **Rollback**: revert wiring; content stays where I1-I4 put it. The matrix is a
  doc artifact; reverting it blocks 6.4, nothing else.
- **Validation**: full suite green with web as a registered domain; assignment
  sweep table and Class Migration Matrix committed to docs.
- **Exit criteria**: the sweep table has no "unassigned" row; the migration matrix
  is complete for every web-domain-classified type; startup path: bootstrap ->
  registration -> session(web) -> pipeline, all under existing tests.
- **ADR / docs**: core-packages.md restructured by ownership; migration matrix
  published as its own doc, referenced by 6.4.
- **Migration notes**: none (declaration only; physical moves are 6.4).

## Phase 6.3 -- Probe domain (the neutrality regression test)

- **Objective**: a minimal, test-scope, non-UI domain (for example an in-memory
  key-value "store" domain with a handful of targets, two capabilities, and three
  interactions) registered and executed entirely from test code, proving: new
  domain, zero edits to runtime-owned files. It stays in the repo permanently as
  CI's neutrality regression test.
- **Motivation**: milestone M4 gate; the only honest proof of "extensibility
  without modifying the runtime" is an extension; the probe is deliberately
  trivial so it cannot become a maintenance burden or a half-built product domain
  (cross-risk #7).
- **Scope / files**: test-scope sources only; a CI-visible test named for its gate
  role.
- **Dependencies**: 6.1, 6.2, 3.1 (open capabilities), 4.4 (neutral dispatch),
  5.2 (bootstrap without driver.properties).
- **Risks**: (arch) probe realism -- too trivial and it proves nothing (it must
  exercise: registration, session creation, capability declaration+validation,
  interaction dispatch, hooks, tracing); too rich and it becomes cross-risk #7;
  the five listed exercises are the exact scope; (compat) none.
- **Rollback**: delete test sources (nothing depends on them).
- **Validation**: the probe test IS the validation; plus a guard: the probe's
  build asserts `git diff`-level that no `src/main/java` runtime-owned file is
  touched by probe enablement.
- **Exit criteria**: probe green in CI; documented as a permanent invariant check.
- **ADR / docs**: architecture-rules.md: the probe as standing gate; README claim
  "domain-neutral" may now cite it.
- **Migration notes**: none.

## Phase 6.4 -- Physical domain package relocation (2026-07-24 addition)

- **Objective**: execute 6.2's Class Migration Matrix -- every type it assigns to
  the Web domain physically relocates to `domain.automation.web.*`, sub-packaged
  by logical vs. implementation ownership (guardrail rule 8: vocabulary --
  `UIElement`, capabilities, concrete UI interactions, roles -- vs. implementation
  -- `UIEngine`, `SeleniumEngine`, locator resolution, driver internals). Kernel-
  neutral types (`Executor`, `Session`, `Flow`/`FlowExecutor`, `Interaction`/
  `Action`, `ActionCapability`, `ActionTrace`, hooks, the `VOID`/`VOIDBuilder`
  facade) are explicitly out of scope -- they stay outside `domain.automation` per
  the ADR-021 addendum. No type moves that the matrix does not name.
- **Motivation**: ADR-021 addendum ("Physical Package Topology"); the roadmap's
  own instruction that the proposed `domain.automation.*` tree "is a direction,
  not a mandatory literal package layout" and must be "derived from actual
  responsibilities," which is exactly what 6.2's audit produces and this phase
  executes.
- **Scope / files**: every file the matrix lists; import updates across every
  caller (kernel, tests, demo pages); `package-info.java` ownership declarations
  moved with their packages; a full visibility re-audit per relocated type (Java
  subpackages do not inherit package-private access -- a member visible only
  because two types shared a package before the move must be re-justified, not
  silently widened to `public`).
- **Dependencies**: 6.2 (matrix complete, zero unassigned rows), 6.3 (probe proves
  the registration contract before the heaviest content move lands on top of it).
  Sequenced with, not strictly after, I9.4 (vocabulary reclaim) -- both are
  mechanical, breaking, major-boundary renames; landing them in the same release
  wave avoids two separate compatibility breaks. Never parallel with any phase
  still writing to `core.actions`, `elements.*`, or `core.engine` (cross-risk #2).
- **Risks**: (arch) THE highest-blast-radius phase in I6 -- mitigated by the
  matrix precondition (6.2) and by doing the move as a single-purpose, no-logic-
  change commit per the roadmap's mechanical-rename discipline (cross-risk #3);
  (arch) visibility widening-by-accident -- explicitly forbidden (guardrail
  addition below); a compile error caused by the move is resolved by fixing the
  dependency direction or narrowing scope, never by reflexively adding `public`;
  (compat) this is a breaking change by design, aligned to the 1.0.0 boundary;
  package-level bridges are not provided (unlike class-level renames elsewhere in
  the ledger) -- the migration guide documents the FQN mapping instead.
- **Rollback**: revert the relocation commit(s); grouped so a partial revert
  cannot leave a type split across old and new packages.
- **Validation**: suite green; fitness checks tightened to their final physical
  form (Selenium imports confined to `domain.automation.web.selenium`; kernel
  packages import nothing from `domain.automation.*`); visibility audit checklist
  complete for every moved type; zero remaining imports of the pre-move FQNs.
- **Exit criteria**: `domain.automation.web.*` contains exactly the matrix's
  entries; no kernel package references it; CHANGELOG FQN-mapping table published.
- **ADR / docs**: core-packages.md and system-overview.md updated to the physical
  topology; ADR-021 addendum marked executed.
- **Migration notes**: 1.0.0 migration guide gains the package FQN mapping table
  (old path -> new path per relocated type), authored here and finalized in 9.5.

### Absorbed backlog findings (checked 2026-07-24 per CLAUDE.md's "check
docs/audits/ongoing for open findings" rule, missed in the initial expansion)

- **`core-driver-package-selenium-coupling.md`** (Medium, ADR-018 + package
  cohesion): `core/driver/` is Selenium-only content misleadingly placed at the
  top level. Its recommended fix is absorbed into 6.4 verbatim rather than
  spawned as its own `initiative/selenium-driver-relocation`: relocate to
  `domain.automation.web.selenium.driver`; rename `DriverContext` ->
  `SeleniumDriverContext`, `DriverFactory` -> `SeleniumDriverFactory`,
  `DriverManager` -> `SeleniumDriverManager`; rename `driver.properties` ->
  `selenium-webdriver.properties` and update `ConfigPaths`. The backlog file's
  own precondition applies as a hard gate here: **6.4 may not begin** until the
  API-surface decision on `DriverFactory.Profile` (currently public via
  `VOIDBuilder.profile(DriverFactory.Profile)`) is resolved -- either re-exposed
  via a stable neutral type (e.g. `SessionProfile` in the kernel) or accepted as
  a breaking change under the normal deprecation window. This decision is
  recorded in the ADR-021 addendum, not inferred during 6.4.
- **`oop-driverfactory-instanceof-preference-dispatch.md`** (Low, OCP,
  `DriverFactory.java:722-724`): per CLAUDE.md's violation protocol, a minimal-
  cost fix touched incidentally during a phase is fixed inline in a dedicated
  commit and recorded under "Incidental fixes." 6.4 touches `DriverFactory` for
  the rename/relocation anyway, so this is fixed inline as part of that same
  initiative, in its own commit -- not folded into the relocation commit itself
  (relocation stays pure-rename per guardrail rule 1).
- **`waiter-returns-webdriverwait.md`** (Medium, ADR-007, `Waiter.java`): **not**
  fixed in 6.4 -- it is a behavior change (three non-deprecated callers migrate
  off `WebDriverWait` to `UIEngine` wait methods), and 6.4 is relocation-only.
  The file moves with its package; its violation-report path reference updates
  in the same commit so the tracker does not go stale. Two of its three callers
  (`Upload.java`, `KeyValuePairHandler.java` in `core/utils/web`) are **not**
  covered by I9.2's ADR-020 graveyard deletion (only `DOMUtils`, `WaitUtils`'s
  By-surface, and `TableHandler` are on that list) -- this violation remains open
  after 6.4 and is not silently resolved by this roadmap; it stays logged for a
  dedicated fix.
- `uiengine-sendkeys-javadoc-selenium-reference.md` is already resolved
  (2026-07-22); no action, only its file path updates when `UIEngine` relocates.
- **Status**: COMPLETE -- commit `3d83c1d`, 1100 tests green, 2026-07-31. 179 files
  changed; 6 selenium domain files had wrong package declarations (fixed); 30+
  test files needed new domain imports or package-declaration changes for
  package-private access; `ElementStructureRulesTest` importPackages scope extended
  to include `"domain"` and `"dsl"`. Exit criteria met: kernel purity gate green,
  zero remaining pre-move FQN imports in non-deprecated code.
