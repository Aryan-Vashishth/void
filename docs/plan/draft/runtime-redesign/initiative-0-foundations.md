# I0 -- Foundations

Objective: make the decisions that gate everything else, and turn architectural
boundaries from prose into automated checks before any code moves.

---

## Phase 0.1 -- ADR-021: kernel boundary, ontology, open decisions

- **Objective**: produce the single decision document the roadmap executes:
  neutrality axes (engine vs domain) assigned to every existing invariant; kernel
  membership list (Action, Flow, FlowExecutor, ActionProfile, hook contract, trace);
  ontology adoption (Runtime, Session, Interaction, Capability, Target, Domain,
  plus the execution-owner concept this ADR must name); resolutions for AD1
  (session-to-domain cardinality), AD2 (execution-owner name/shape), AD3
  (validation timing); the discrete-operations scope limit.
- **Motivation**: audit Part II; 30+ later phases substitute AD2's chosen name.
- **Scope / files**: `docs/decisions/pending-review/021-*.md` (new), `CLAUDE.md`
  invariants table, `docs/architecture/system-overview.md` philosophy section.
  No Java.
- **Dependencies**: none. Unblocks everything.
- **Risks**: (arch) scope creep into a design document -- the ADR decides, it does
  not design APIs; (compat) none.
- **Rollback**: revert docs commits.
- **Validation / tests**: none executable; review against audit Part II
  recommendations 1-9.
- **Exit criteria**: AD1-AD3 each have a written resolution with rationale; every
  invariant names its axis; kernel membership is a closed list.
- **ADR / docs**: this phase IS the ADR; docs updated in-phase.
- **Migration notes**: none.

## Phase 0.2 -- Architecture fitness checks (the ratchet)

- **Objective**: automated dependency verification running in the standard test
  phase, encoding the boundaries that are ALREADY true so they cannot regress:
  `elements.*`, `core.flow`, `core.executor`, `core.logging`, `core.actions` are
  Selenium-free; `core.runtime` imports WebDriver only in its deprecated bridge;
  logging imports nothing domain-specific.
- **Motivation**: audit D17 -- every invariant is currently enforced by grep and
  memory; 37 phases cannot ride on reviewer vigilance. Later phases tighten these
  checks as they win new boundaries.
- **Scope / files**: new test-scope verification module/classes; `pom.xml` test
  dependency if a library is chosen (library choice is an implementation decision,
  not made here); no production code.
- **Dependencies**: 0.1 (checks reference the axis vocabulary in their names/docs).
- **Risks**: (arch) checks written looser than reality, creating false confidence --
  each check must be demonstrated to FAIL when a forbidden import is temporarily
  added; (compat) none.
- **Rollback**: delete the verification classes.
- **Validation / tests**: the checks themselves; mutation demo (add forbidden
  import, watch it fail) recorded in the phase doc.
- **Exit criteria**: suite green; each listed boundary has one check; CI-executable
  via `mvn test`.
- **ADR / docs**: `docs/contributing/architecture-rules.md` gains a section: how to
  tighten the ratchet when a phase wins a boundary.
- **Migration notes**: none.

## Phase 0.3 -- Documentation baseline

- **Objective**: align living docs with the decided direction so subsequent phases
  edit docs incrementally instead of confronting drift: system overview marks
  legacy-vs-kernel paths with their scheduled fates; the roadmap is linked from
  `docs/plan/draft/README.md`; superseded/merged drafts carry their notices.
- **Motivation**: "documentation is the source of truth" fails if the truth is
  scattered at the start of a 37-phase campaign.
- **Scope / files**: `docs/architecture/*.md` touch-ups, draft README, notices in
  absorbed drafts. No Java.
- **Dependencies**: 0.1.
- **Risks**: none beyond doc-link rot; verified by link check.
- **Rollback**: revert.
- **Validation**: manual link pass; no broken references to renamed sections.
- **Exit criteria**: no doc describes the pre-ADR-021 direction as current intent.
- **Migration notes**: none.
