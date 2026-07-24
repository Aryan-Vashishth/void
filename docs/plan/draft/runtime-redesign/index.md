# Runtime Redesign -- Master Roadmap

Identified: 2026-07-20, from the two-part domain-model audit and ontology review
(`docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`, findings D1-D18,
O1-O9, open decisions AD1-AD3).
Status: draft roadmap. This is the implementation strategy for the next major runtime
release: the migration from the current UI-oriented, engine-neutral framework to a
Domain-Neutral Runtime built on the Runtime / Interaction / Capability / Target /
Domain conceptual model.

This roadmap is a planning document. It contains no API designs, no Java, and no ADR
content. Each phase is scoped so it can later become its own implementation prompt.
When an initiative is activated, it is expanded into the standard
`index.md` + `phase-N.md` structure on its own branch.

Terminology note: the audit's open decision AD2 (the name and shape of the concept
that performs interactions) is resolved in Phase 0.1 (ADR-021). Until then this
roadmap uses the placeholder **execution owner**. Every later phase that references
it substitutes the chosen name mechanically; no phase's scope depends on which
candidate wins.

---

## Executive Summary

**Strategy: strangle, never rewrite.** The modern pipeline
(Element -> Action -> Flow -> FlowExecutor -> UIEngine) is architecturally sound and
survives domain substitution in six of seven primitives (June 2026 audit). The
redesign therefore does not replace the pipeline; it (1) draws the kernel boundary
around it, (2) re-types its edges against neutral concepts (Target, execution owner,
Session), (3) repackages everything UI-specific as the first Domain, and (4) deletes
the legacy surface that anchors Selenium vocabulary in the core. At every milestone
the framework builds, tests pass, and existing test suites written against the
stable tiers keep compiling.

Ten initiatives, 38 phases, five milestones:

| Milestone | Meaning | Initiatives |
|---|---|---|
| M1 Boundary locked | Decisions made, boundaries encoded as automated checks | I0 |
| M2 Neutral vocabulary | Target root exists, kernel free of UI imports, capabilities open | I1, I2, I3 |
| M3 Neutral execution | Engine contract sealed, execution-owner contract live, kernel retyped | I4 |
| M4 Neutral lifecycle | Session first-class, bootstrap de-Seleniumized, Web is a registered Domain, probe domain proves extension | I5, I6 |
| M5 Clean surface | Locator generalization done, legacy deleted, vocabulary reclaimed, tiers re-declared | I7, I8, I9 |

Guardrail mechanics (not just principles): Phase 0.2 introduces automated
architecture fitness checks that encode each boundary as it is won. Every subsequent
phase tightens the checks in the same commit that earns the tightening. Boundaries
are therefore ratcheted: once a package is Selenium-free or elements-free, the build
fails if it regresses. This is how "runtime remains domain-neutral" survives 37
phases without depending on reviewer memory.

Every temporary artifact created by this roadmap (bridge overloads, deprecated
aliases, compatibility shims) is listed in the Migration Ledger below with the phase
that deletes it. No shim ships without a scheduled death.

---

## Existing Draft Assessment (mandatory review)

| Draft | Verdict | Rationale |
|---|---|---|
| `generalize-element-into-target/` | **MERGE** | Becomes Initiative I1 Phases 1.1-1.3 verbatim (its three phases are already correctly scoped). Its "future watch" items (Engine superinterface, role generalization) are absorbed by I4.3 and I7. Draft index gains a merged-into notice; no content is lost. |
| `runtime-kernel-boundary/` | **SUPERSEDE** | Created as the Critical-findings holding initiative before this roadmap existed. Its four phases map to 0.1 (ADR-021), 2.1 (hooks), 4.1+4.2 (engine contract), 3.1+3.2 (vocabularies). This roadmap is its superset; keeping both would create two sources of truth for the same phases. Scrapped and moved to `docs/plan/archive/runtime-kernel-boundary/`. |
| `oop-violations-remediation/` | **SPLIT** | Phases 1-3 (P1-P7, P10): KEEP as an independent prerequisite work stream; they stabilize the Action and Element surfaces this roadmap re-types, and nothing here may run against an unstable capability surface. Phase 4: P8 is MERGED into 4.1 (engine registry) -- one owner, this roadmap; P11 (Via helpers) is absorbed by 9.3 (Via deletion); P9 stays in the original draft as an independent low-priority fix. |
| `locator-sync-trigger/` | **KEEP** | Developer tooling around locator file sync. Orthogonal: it consumes locator file formats, which this roadmap does not change (I7 changes ownership and strategy sets, not file formats). Only coordination point: its Maven/CLI phases should not land in the same window as 7.3 (By-path deletion) to avoid rebase churn. |

Absorption rule: where a phase below duplicates an existing draft's phase, the
existing draft's text is the authoritative starting point and is lifted, not
rewritten.

---

## Initiative Dependency Graph

```
                         [oop-remediation P1-P3]  (external prerequisite)
                                   |
  I0 Foundations ------------------+----------------------------------------+
      |                                                                     |
      v                                                                     |
  I1 Target Model ----> I2 Kernel Extraction ----> I3 Capability Model      |
                              |                          |                  |
                              v                          v                  |
                        I4 Execution Boundary  <---------+   (4.1 absorbs P8)
                              |
              +---------------+----------------+
              v                                v
        I5 Session Model                 I7 Locator Generalization
              |                                |
              v                                |
        I6 Domain Registration                 |
              |                                |
              +---------------+----------------+
                              v
                    I8 Interaction Semantics
                              |
                              v
                    I9 Legacy Removal & Public API
                        (9.4 vocabulary reclaim depends on 9.3 deletion)
```

Sequencing rules:

- **Unblocks everything:** 0.1 (ADR-021). No re-typing phase may start before the
  execution-owner name (AD2), session cardinality (AD1), and validation timing (AD3)
  are decided. This is the single decision bottleneck; see Cross-Initiative Risks.
- **Strict order:** I1 before I2 (the cycle break needs Target to exist); I2 before
  I3 (capabilities open only after kernel/UI action split, or the open set would be
  defined against a package about to move); I3+I2 before I4.4 (kernel retyping);
  I4 before I5.3 and I6 (sessions and domains bind execution owners); I6.2 before
  6.3 (probe domain needs the registration contract); 9.1 before 9.3 (DSL must leave
  the legacy pipeline before the pipeline is deleted); 9.3 before 9.4 (the
  Interaction name is occupied until the legacy class dies).
- **May run in parallel:** I5 and I7 (disjoint files); I7 and I6; oop-remediation
  P1-P3 with I0-I1; locator-sync-trigger with anything except 7.3.
- **Must never run in parallel:** any two of I1/I2/I3/I4.4 (all churn
  `core.actions` and `elements.api`; interleaving guarantees merge conflicts and,
  worse, semantic conflicts in the capability surface); 9.3 with anything touching
  `dsl` or `core.interactions`.

---

## Initiative Breakdown

| # | Initiative | Objective (one sentence) | Phases | Detail file |
|---|---|---|---|---|
| I0 | Foundations | Decide AD1-AD3, encode boundaries as automated ratchets, align docs | 3 | [initiative-0-foundations.md](initiative-0-foundations.md) |
| I1 | Target Model | Domain-neutral Target root; Element becomes UIElement (merged draft) | 4 | [initiative-1-target-model.md](initiative-1-target-model.md) |
| I2 | Kernel Extraction | Kernel packages free of UI-domain and legacy imports | 4 | [initiative-2-kernel-extraction.md](initiative-2-kernel-extraction.md) |
| I3 | Capability Model | Open, declarative capability set; no silent fallbacks | 3 | [initiative-3-capability-model.md](initiative-3-capability-model.md) |
| I4 | Execution Boundary | Sealed engine contract; neutral execution-owner contract; kernel retyped | 5 | [initiative-4-execution-boundary.md](initiative-4-execution-boundary.md) |
| I5 | Session Model | Session as first-class neutral concept; bootstrap de-Seleniumized | 4 | [initiative-5-session-model.md](initiative-5-session-model.md) |
| I6 | Domain Registration | Domain as registration unit; Web assembled as first Domain with its own physical package root; probe domain gate | 4 | [initiative-6-domain-registration.md](initiative-6-domain-registration.md) |
| I7 | Locator Generalization | Open strategy set; descriptor ownership moved domain-side; By-path deleted | 3 | [initiative-7-locator-generalization.md](initiative-7-locator-generalization.md) |
| I8 | Interaction Semantics | Description/occurrence split, multi-subject binding, Result contract | 3 | [initiative-8-interaction-semantics.md](initiative-8-interaction-semantics.md) |
| I9 | Legacy Removal & Public API | DSL re-founded, legacy deleted, vocabulary reclaimed, tiers re-declared | 5 | [initiative-9-legacy-removal.md](initiative-9-legacy-removal.md) |

---

## Migration Ledger (temporary artifacts and their deletion phases)

Every compatibility artifact this roadmap creates, with its scheduled death. Nothing
may be added to this ledger without a deletion phase.

| Artifact | Created | Deleted | Purpose while alive |
|---|---|---|---|
| Deprecated hook bridges in `core.interactions.hooks` | 2.1 | 9.3 | Old hook type identities keep compiling |
| `Element` deprecated alias for `UIElement` (if draft's rename needs one) | 1.2 | 9.5 | External page objects migrate at leisure |
| `perform(UIEngine)` bridge overloads on kernel contracts | 4.4 | 9.4 | Kernel retyping without breaking Beta callers |
| UIEngine-typed hook signature bridge | 4.4 | 9.4 | Hook ecosystem migrates with kernel |
| EngineBootstrap (already flagged migration-only in ADR-018) | pre-existing | 4.2 | Startup parameter until settings decoupling lands |
| `SeleniumLocatorBridge` (pre-existing, ADR-019) | pre-existing | 9.3 | Legacy By conversion |
| Deprecated By-returning `resolve()` on LocatorResolver | pre-existing | 7.3 | Legacy resolution path |
| Legacy `Interactions`, `Via`, `UIContext`, `ExecutionContext`, `HookedAction` | pre-existing | 9.3 | Frozen compatibility surface |
| Old `Action`/`Flow` names as deprecated aliases post-reclaim | 9.4 | next major after release | Vocabulary migration window |

---

## Cross-Initiative Risks

1. **ADR-021 bottleneck.** One document gates 30+ phases. Mitigation: 0.1 is scoped
   to decide only AD1-AD3 and kernel membership, nothing else; it must not grow into
   a full design document. If AD2 stalls, I1 and 2.1 can proceed (they do not touch
   the execution seam), buying time without breaking sequencing.
2. **`core.actions` churn collisions.** Four initiatives touch it. Mitigation: the
   never-parallel rule above, plus fitness checks that freeze each won boundary so a
   later phase cannot silently undo an earlier one.
3. **Page-object blast radius.** 1.2 (Element rename) and 9.4 (vocabulary reclaim)
   touch every page object enum and most tests. Mitigation: both are mechanical
   renames scheduled as single-purpose phases with no semantic change, and both are
   preceded by a green-baseline commit so the diff is pure rename.
4. **Test suites pinning old APIs.** 39 test files exercise current signatures.
   Mitigation: every re-typing phase (4.4, 5.3, 8.2) includes test migration in its
   own scope; a phase is not done while any test uses its bridge artifact except
   tests that exist to verify the bridge.
5. **Long-lived deprecation surface.** Between M3 and M5 the codebase carries both
   typed edges. Mitigation: the Migration Ledger is reviewed at every milestone; any
   artifact whose deletion phase slips two milestones triggers a hotfix initiative
   per the workflow.
6. **Solo-maintainer bandwidth.** 37 phases is a long campaign. Mitigation: every
   milestone is a stable, shippable state; the roadmap survives suspension at any
   milestone boundary without leaving dual models in the public surface (bridges are
   internal or deprecated-annotated).
7. **Scope magnetism toward a second real domain.** The probe domain (6.3) exists to
   prove neutrality WITHOUT building REST/CLI for real. Building a production second
   domain inside this roadmap would double its length; it is explicitly out of scope
   and becomes its own initiative after M4.
8. **Physical package relocation (6.4) landing without a complete ownership audit.**
   The ADR-021 addendum is explicit that relocation follows the 6.2 matrix, not
   inference during implementation; the mitigation is structural, not just
   discipline: 6.4's dependency on 6.2 requires zero unassigned rows before any file
   moves, and the phase forbids widening visibility to work around a compile error
   caused by the move (guardrail addition below) -- a widened-by-accident member is
   the same failure mode as risk #2's churn, one level down at the member scope.

---

## Migration Timeline (high-level ordering)

| Order | Work | Milestone |
|---|---|---|
| 1 | 0.1 -> 0.2 -> 0.3; oop-remediation P1-P3 in parallel | M1 |
| 2 | 1.1 -> 1.2 -> 1.3 -> 1.4 | |
| 3 | 2.1 -> 2.2 -> 2.3 -> 2.4 | |
| 4 | 3.1 -> 3.2 -> 3.3 | M2 |
| 5 | 4.1 -> 4.2 -> 4.3 -> 4.4 -> 4.5 | M3 |
| 6 | 5.1 -> 5.2 (parallel: 7.1 -> 7.2) | |
| 7 | 5.3 -> 5.4; 6.1 -> 6.2 -> 6.3 (parallel: 7.3) | M4 |
| 8 | 8.1 -> 8.2 | |
| 9 | 6.4 (with 9.4); 9.1 -> 9.2 -> 9.3 -> 9.4 (with 8.3, 6.4) -> 9.5 | M5 |

---

## Versioning (CHANGELOG.md mapping)

Per CONTRIBUTING.md: SemVer + Keep a Changelog; entries accrue under
`## [Unreleased]` as phases land; releases are cut at milestone boundaries. Below
1.0.0 a minor bump may carry breaking changes when documented under `### Removed`
/ `### Changed`.

| Milestone | Version | CHANGELOG sections |
|---|---|---|
| M1 (I0) | none | Docs-only; no changelog entry per CONTRIBUTING |
| M2 (I1-I3) | 0.5.0 | Added: `Target` root. Changed: `Element` renamed to `UIElement` (deprecated alias), capability set opened. Removed: UNKNOWN silent profile fallback. Deprecated: old hook package location (2.1 bridges) |
| M3 (I4) | 0.6.0 | Added: neutral execution-owner contract, engine registry. Deprecated: UIEngine-typed bridge overloads |
| M4 (I5-I7) | 0.7.0 | Added: Session, domain registration (6.1), Web domain declared with its Class Migration Matrix (6.2, docs/planning only), probe domain. Changed: bootstrap no longer requires `driver.properties`; open locator strategy set. Removed: By-returning resolver path |
| M5 (I8-I9) | 1.0.0 | Removed: `Interactions`, `Via`, `UIContext`, `ExecutionContext`, bridges, deprecated aliases. Changed: vocabulary renames (Action -> Interaction) with migration guide; web-owned code physically relocated to `domain.automation.web.*` (6.4) with published FQN mapping. Added: Result contract, multi-subject binding |

The release train is now pinned by the plan-level sequence in
`docs/plan/draft/README.md`: 0.3.0 ships `initiative/engine-decoupling`, 0.4.0
ships `oop-violations-remediation` Phases 1-3, then one minor bump per milestone
(0.5.0 = M2, 0.6.0 = M3, 0.7.0 = M4) and the major at M5 (1.0.0).
`locator-sync-trigger` holds the flexible 0.8.0 slot by default; if it lands
earlier it takes the next free minor and the later numbers shift up -- the stable
part is the mapping: one minor per milestone, major at M5.

---

## Final Architecture Check

At M5 the conceptual chain holds end to end, with enforcement, not intention:

- **Target**: a neutral root exists (`Target`, I1); the kernel references only it;
  UI-specific structure (locator keys, roles, files) lives on `UIElement` inside the
  Web domain. UI assumptions cannot re-enter: the fitness checks fail the build if
  kernel packages import UI vocabulary (2.4).
- **Interaction**: intent objects are immutable descriptions, bind one or more
  subjects including the Session, may declare a Result, and never execute (I8; I3
  guarantees capabilities stay declarative). The intent/execution split is enforced
  by the kernel's lack of any platform import.
- **Capability**: an open, declarative set; a new domain introduces capabilities
  without editing runtime-owned types; no silent UNKNOWN behavior remains (I3).
- **Domain**: a registration unit that defines vocabulary and ships execution
  owners; Web is the first registered Domain (6.2), physically rooted at
  `domain.automation.web.*` (6.4) per an audited migration matrix rather than a
  mechanical move; the probe domain (6.3) is the standing proof that a domain
  integrates with zero runtime modification -- it runs in CI forever as a
  regression test for neutrality.
- **Runtime**: orchestration, session lifecycle, validation, and observation only;
  no Selenium import, no UI vocabulary, no closed enumerations of domain concepts;
  bootstrap starts without `driver.properties` when no web domain is in play (5.2).
- **One execution model**: the legacy pipeline is deleted, not abstracted over
  (9.3); the DSL rides the kernel pipeline (9.1); no second path exists to drift.

The name "interaction" is reclaimed for the framework's central concept only after
the legacy class that squats on it is gone (9.4), completing the ubiquitous-language
repair identified as D11.

---

## Guardrails restated as phase-level rules

1. No phase mixes a boundary change with a behavior change.
2. No phase lands without the fitness-check tightening it earned.
3. No temporary artifact without a Migration Ledger row.
4. No kernel file may gain a domain or platform import, ever, in any phase.
5. Docs and ADR updates ship in the same phase as the change they describe.
6. Every phase compiles and passes `mvn compile -q` and the test suite; phases that
   consciously break a Beta-tier signature migrate all in-repo callers within the
   same phase.
7. The Runtime's responsibility list is closed: orchestration, validation,
   observation dispatch, session creation, registry. No phase may add a
   responsibility to the Runtime (configuration management, target resolution,
   execution, caching, engine management) until Session, Domain, the execution
   owner, and observability have each been considered and refused it -- and the
   refusals are recorded in the phase doc. An orchestrator that accumulates
   leftover duties becomes a central manager, which is the god-object failure
   mode this roadmap exists to prevent.
8. Domain ownership is two-layered and phases must say which layer they touch:
   logical ownership (the vocabulary a domain defines -- targets, capabilities,
   interactions) versus implementation ownership (the realizations it contains --
   executors, resolution, platform internals). A second implementation of an
   existing medium (Playwright) joins the existing domain's implementation layer;
   it never becomes a sibling domain.
9. No physical package relocation without a committed ownership audit naming
   every moved type first (the 6.2 Class Migration Matrix pattern). A phase that
   relocates files it did not first enumerate and justify is inference, not audit,
   and is out of process.
10. A compile error caused by a package move is never resolved by widening
    visibility. Widen only after tracing why the dependency crosses the new
    package boundary and confirming no narrower fix (same-package placement,
    an explicit abstraction, composition) applies -- the same discipline
    Section 6 already requires for ordinary relocation, restated because package
    moves are where it is most tempting to skip.
