# I6 -- Domain Registration

Objective: Domain becomes the unit of extension in code, not only in docs: a
registration contract, the Web domain assembled as its first instance, and a
standing machine-checked proof that a new domain integrates with zero runtime
modification.

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
crossing that boundary requires no runtime edits (ontology invariants I1, I2).

**What it deliberately does not own.** Any production second domain -- REST or CLI
built for real would double the roadmap (cross-initiative risk #7) and becomes its
own initiative after M4. No plugin-discovery machinery (classloaders, SPI, module
scanning): explicit registration only, until a real ecosystem demands more. And no
web-domain content -- everything it registers in 6.2 was moved by earlier
initiatives; 6.2 is wiring, not migration.

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

## Phase 6.2 -- Web domain assembly

- **Objective**: everything UI-specific that previous initiatives pushed out of the
  kernel is declared and registered as the Web domain -- the first Domain instance.
  Primarily wiring and declaration; the content already moved in I1-I4, I7.
  The declaration distinguishes the domain's two ownership layers explicitly
  (guardrail rule 8): **logical ownership** -- the vocabulary the Web domain
  defines (UIElement model, UI capabilities, concrete UI interactions, roles) --
  versus **implementation ownership** -- the realizations it contains (the web
  execution contract and its Selenium executor, locator resolution, driver
  internals). The distinction is load-bearing: a future Playwright executor is a
  second entry in the Web domain's implementation layer against the same
  vocabulary, not a new domain.
- **Motivation**: the ontology's Domain concept must have one real instance before
  the probe (6.3) can prove the second costs no runtime edits.
- **Scope / files**: web-domain registration implementation; package-info ownership
  declarations; bootstrap default (web registered by default so existing user
  experience is unchanged).
- **Dependencies**: 6.1, and the content phases: 2.3, 3.3, 4.5, 7.2.
- **Risks**: (compat) zero behavior change required -- existing tests are the
  proof; default registration preserves the "it just works" experience; (arch)
  leftover strays -- anything that cannot be assigned to web-domain or kernel at
  this point is a missed finding; the phase includes a full-assignment sweep
  (every main-tree package maps to kernel / web domain / observability /
  tooling / legacy-pending-deletion, and web-domain rows are further classified
  as vocabulary vs implementation per guardrail rule 8).
- **Rollback**: revert wiring; content stays where I1-I4 put it.
- **Validation**: full suite green with web as a registered domain; assignment
  sweep table committed to docs.
- **Exit criteria**: the sweep table has no "unassigned" row; startup path:
  bootstrap -> registration -> session(web) -> pipeline, all under existing tests.
- **ADR / docs**: core-packages.md restructured by ownership.
- **Migration notes**: none.

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
