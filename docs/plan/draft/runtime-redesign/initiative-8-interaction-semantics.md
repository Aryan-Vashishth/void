# I8 -- Interaction Semantics

Objective: the kernel's interaction concept gains the semantics the ontology
requires: descriptions vs occurrences, one-or-more subjects, and an explicit Result
contract. The vocabulary reclaim (Action -> Interaction naming) is planned here but
executes with I9 because the name is occupied.

---

## Phase 8.1 -- Description / occurrence split

- **Objective**: an interaction description (today: an Action instance, immutable,
  reusable) is formally distinguished from an execution occurrence (today:
  ActionTrace, informally); occurrence identity is minted at dispatch by the
  runtime; retries, timing, failure, and hooks attach to occurrences.
- **Motivation**: ontology O2; traces and results need something to attach to;
  re-running a Flow twice must yield two occurrence streams, not mutated state.
- **Scope / files**: `core/actions/trace/*` promotion to the formal occurrence
  role; dispatch path in FlowExecutor/runtime; logging call sites reference
  occurrence identity.
- **Dependencies**: 4.4 (occurrences minted at the neutral dispatch edge),
  5.1 (session identity in the occurrence).
- **Risks**: (arch) state creep into descriptions -- the phase includes an
  immutability assertion test over description types; (compat) trace-consuming
  test helpers may need updates; Beta tier.
- **Rollback**: revert; trace returns to informal role.
- **Validation**: suite green; new test: same description executed twice yields two
  occurrences with distinct identities and independent hook runs.
- **Exit criteria**: every executed interaction has exactly one occurrence record;
  descriptions verified immutable.
- **ADR / docs**: actions.md gains description/occurrence vocabulary.
- **Migration notes**: none external.

## Phase 8.2 -- Multi-subject binding and Result contract

- **Objective**: interaction descriptions may bind one or more subjects (Targets,
  or the Session per 5.3's groundwork), requiring capabilities per subject; and an
  interaction may declare a Result as part of its contract (reading interactions
  stop being modeled as side effects with out-of-band returns).
- **Motivation**: ontology O3, O5; existing read-type actions (ReadTextAction)
  already smuggle results informally.
- **Scope / files**: kernel description contract; validation path (per AD3
  timing); read-family UI actions adopt the Result contract; FlowExecutor result
  propagation.
- **Dependencies**: 8.1, 3.3 (validation against open capabilities), 5.3
  coordination (single owner for subject-binding groundwork).
- **Risks**: (arch) result typing must not turn Flow into a dataflow language --
  scope is "an interaction may yield a value", not inter-step piping; piping is
  explicitly out of scope (stability rule: no speculation); (compat) read-action
  callers may see signature refinement; Beta tier, in-repo migration in-phase.
- **Rollback**: revert; single-subject implicit model returns.
- **Validation**: suite green; tests: two-subject interaction validates both
  subjects' capabilities; a read interaction's result is observable through the
  runtime path (not via engine escape hatch).
- **Exit criteria**: no kernel assumption of exactly-one-Target remains; result
  path documented.
- **ADR / docs**: actions.md; system-overview execution flow.
- **Migration notes**: CHANGELOG for read-family signatures.

## Phase 8.3 -- Vocabulary reclaim plan (executes as 9.4)

- **Objective**: the mapping table for the ubiquitous-language repair: which
  code names align to ontology names (Action -> Interaction, and any others
  ADR-021 mandates), what becomes a deprecated alias, and what never renames
  (stable-tier surfaces get aliases, Beta surfaces rename outright).
- **Motivation**: audit D11 -- the framework's central noun is squatted on by the
  frozen legacy class until 9.3 deletes it; renaming earlier would create two
  `Interaction*` meanings in one codebase, the worst of all worlds.
- **Scope / files**: a mapping document in this initiative's folder; no code.
- **Dependencies**: 8.2 (the semantics being named are final), 0.1.
- **Risks**: none (planning artifact); the execution risk lives in 9.4.
- **Rollback**: n/a.
- **Validation**: review that every renamed concept's tier and alias policy is
  stated.
- **Exit criteria**: mapping table approved; 9.4 can execute mechanically from it.
- **ADR / docs**: the mapping table itself.
- **Migration notes**: prepared here, shipped with 9.4.
