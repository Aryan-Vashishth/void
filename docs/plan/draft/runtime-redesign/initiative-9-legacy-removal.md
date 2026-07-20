# I9 -- Legacy Removal & Public API

Objective: the strangler completes: the DSL leaves the legacy pipeline, the legacy
surface is deleted, the ontology vocabulary is reclaimed, and the stability tiers
are re-declared for the new major release.

---

## Phase 9.1 -- DSL re-founded on the kernel pipeline

- **Objective**: `dsl.VoidDSL` stops delegating to the frozen `Interactions`
  orchestrator and rides the kernel pipeline (session + interactions), removing
  By/WebElement from its surface (audit C6/D5). Behavior parity for its
  BDD-facing methods.
- **Motivation**: the legacy zone cannot shrink while a living, test-facing layer
  depends on it; D5 is the gravitational-pull risk.
- **Scope / files**: `dsl/VoidDSL.java`; `core/adapters/cucumber/*` call sites;
  its tests. Depends on oop-remediation Phase 3 (P2 dispatch fix) being merged --
  the DSL should be refactored once, not twice; if P2 is not yet done, it merges
  INTO this phase (one owner).
- **Dependencies**: 4.4, 5.3, 8.2 (the kernel surface it lands on is final);
  never parallel with 9.3.
- **Risks**: (compat) DSL is user-facing for BDD users; parity is verified by
  behavior tests per method before/after; (arch) partial migration would leave a
  dual dependency -- the phase completes or reverts, no half state.
- **Rollback**: revert (legacy path still exists until 9.3, so rollback is safe).
- **Validation**: DSL behavior-parity suite green; `grep -rn "core.interactions"
  src/main/java/dsl` empty; no By/WebElement in DSL surface.
- **Exit criteria**: dsl depends only on kernel, session, and web-domain
  vocabulary; fitness check added.
- **ADR / docs**: DSL section of system-overview.
- **Migration notes**: CHANGELOG; DSL users unaffected functionally.

## Phase 9.2 -- Utilities dismantling

- **Objective**: `core.utils` stops being the unowned multi-domain package (D6):
  the ADR-020 graveyard (`DOMUtils`, `WaitUtils` By-surface, `TableHandler`) is
  deleted on its schedule; `EnumResolver` loses its Selenium imports (its By/
  WebElement members either move to the web domain or are deleted with their dead
  callers); `UIContext` is prepared for 9.3 deletion (remaining readers
  inventoried); config loading's future home documented (move optional).
- **Motivation**: audit D6, D14 (undeprecated Selenium in utils), priorities 7
  and 11.
- **Scope / files**: `core/utils/web/*` deletions; `core/utils/EnumResolver.java`;
  inventory doc for UIContext readers; per the 0.3.0 deprecated-removal notes,
  the known blocking call sites are resolved here.
- **Dependencies**: 4.5 (driver edges already cut), ADR-020 accepted; before 9.3.
- **Risks**: (compat) deletions -- everything deleted is already
  `@Deprecated(forRemoval=true)` with documented replacements per ADR-020; the
  phase verifies zero non-deprecated callers before each deletion (the audit noted
  active callers already commented out); (arch) none.
- **Rollback**: restore from history; deletions are grouped by class, one commit
  each.
- **Validation**: suite green after each deletion commit; fitness check: zero
  Selenium imports under `core.utils`.
- **Exit criteria**: `core.utils` contains only genuinely neutral leaves; check
  green.
- **ADR / docs**: core-packages.md utils section; CHANGELOG removal list.
- **Migration notes**: removal notices with replacements (all pre-documented in
  ADR-020).

## Phase 9.3 -- Legacy surface deletion

- **Objective**: delete `Interactions`, `Via` (absorbing oop-remediation P11),
  `UIContext`, `ExecutionContext`, `HookedAction` (if not already deleted by
  oop-remediation P1), `SeleniumLocatorBridge`, the deprecated
  `Interactions(WebDriver)`/`SeleniumEngine(WebDriver)` constructors,
  `VOID.getDriver()/getContext()/interactions()/start()`, and the 2.1 hook
  bridges. The strangler's final cut.
- **Motivation**: audit domain 9 exists only to die; D5/D11/H-series lineage; the
  0.3.0 deprecated-removal inventory (five known blocking call sites) is the
  checklist seed.
- **Scope / files**: the legacy classes and every remaining reference (tests
  pinning them are deleted or migrated); `core.bridge.selenium` package removal;
  `core.context` reduced to the session type.
- **Dependencies**: 9.1 (DSL off), 9.2 (utils off), 7.3 consumer gate resolved;
  external-consumer deprecation window per stability policy (the release notes
  window, judged at activation).
- **Risks**: (compat) THE breaking phase of the roadmap, by design, aligned to the
  major release boundary; everything deleted has carried
  `@Deprecated(forRemoval=true)` and a documented replacement for multiple
  releases; (arch) discovery of hidden dependents late -- mitigated by a
  pre-deletion reference inventory committed as the phase's first artifact.
- **Rollback**: git revert of deletion commits (grouped per class family); no
  schema/data implications.
- **Validation**: suite green; grep sweeps: `Interactions|UIContext|
  ExecutionContext|SeleniumLocatorBridge` zero hits in src; Selenium imports now
  confined to the web domain's platform edge -- fitness checks tightened to their
  final form.
- **Exit criteria**: legacy bounded context empty; Migration Ledger rows closed
  for all pre-existing artifacts.
- **ADR / docs**: CHANGELOG major-release removal section; system-overview legacy
  path section deleted.
- **Migration notes**: the major-release migration guide (started here, finished
  9.5).

## Phase 9.4 -- Vocabulary reclaim (executes 8.3's mapping)

- **Objective**: ontology names land in code per the 8.3 mapping table: the
  central intent concept takes the Interaction name (freed by 9.3); Beta-tier
  types rename outright; stable-tier surfaces gain aliases with a deprecation
  window into the next major; the 4.4 bridge overloads die here.
- **Motivation**: audit D11; ubiquitous language is the point of the ontology --
  shipping the new architecture under the old names would preserve the confusion
  the redesign exists to end.
- **Scope / files**: renames across kernel and web domain per mapping; all docs;
  all tests; Migration Ledger rows for new aliases (die next major).
- **Dependencies**: 9.3 (name freed), 8.3 (mapping approved); green baseline
  commit before (pure-rename discipline, cross-risk #3).
- **Risks**: (compat) largest mechanical diff of the roadmap -- mitigated by
  aliasing policy per tier and by being semantically empty (rename only, enforced
  by review rule: this phase changes no behavior and no signature shapes);
  (arch) none if 8.3's table is complete.
- **Rollback**: revert rename commits.
- **Validation**: suite green; alias-compatibility tests; docs grep for old names
  returns only migration-guide mentions.
- **Exit criteria**: code speaks the ontology; mapping table fully executed.
- **ADR / docs**: every architecture doc; the migration guide's rename table.
- **Migration notes**: rename table published.

## Phase 9.5 -- Stability re-declaration and closing audit

- **Objective**: re-declare the stability tiers for the new major (what is Stable,
  Beta, Internal in the redesigned surface); delete any remaining Migration Ledger
  artifacts scheduled for this milestone (1.2 Element alias); run the workflow's
  full-system audit; convert its findings into the standard hotfix initiative if
  needed; move this roadmap and absorbed drafts to their archived/fulfilled
  locations; promote ADR-021 (and any successors) through review.
- **Motivation**: the workflow's mandated final stage; a redesign that ends
  without a closing audit violates the project's own process.
- **Scope / files**: annotations pass for tiers; docs; audits; plan folder
  archival moves.
- **Dependencies**: everything.
- **Risks**: audit findings large enough to warrant a hotfix initiative -- that is
  the designed outcome path, not a failure.
- **Rollback**: n/a (release gate).
- **Validation**: full-system audit document produced; probe domain (6.3) and all
  fitness checks green; CHANGELOG and migration guide complete.
- **Exit criteria**: main-merge criteria of the workflow satisfied; M5 declared.
- **ADR / docs**: final state everywhere; MEMORY/plan housekeeping.
- **Migration notes**: the migration guide is the release deliverable.
