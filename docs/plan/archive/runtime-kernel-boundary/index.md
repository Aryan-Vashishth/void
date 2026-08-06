# Runtime Kernel Boundary

> **SCRAPPED (2026-07-20)** -- superseded in full by
> [`../../draft/runtime-redesign/`](../../draft/runtime-redesign/index.md).
> **Reason**: this draft was created as a holding initiative for the 2026-07 audit's
> Critical findings before the master roadmap existed; every one of its four phases
> was absorbed by runtime-redesign (phase 1 -> 0.1; phase 2 -> 4.1 + 4.2;
> phase 3 -> 2.1; phase 4 -> 3.1 + 3.2), so implementing it would duplicate that
> work under a second source of truth. Archived for traceability only.
> **Do not implement from here.**

Identified: 2026-07-20 domain model architecture audit and ontology review
(`docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`, findings D1-D18 and O1-O9).
Branch target: cut from `feature/engine-decoupling` once merged, as
`initiative/runtime-kernel-boundary`.

---

## Problem statement

VOID's long-term identity is a domain-neutral interaction runtime, but the boundary of
that runtime is defined nowhere in code or docs. The 2026-07 audit found four Critical
issues that must be resolved before any second domain (REST, CLI, Playwright-as-engine)
is introduced:

1. Engine neutrality and domain neutrality are conflated; invariants do not state which
   axis they govern.
2. The interaction kernel and the UI domain model are entangled (D1): the kernel/UI
   boundary runs invisibly through the middle of `core.actions`.
3. The engine contract package depends on the Selenium implementation and on
   `core.driver` (D2, D3).
4. The runtime's extension vocabularies are closed enums (`ActionCapability`,
   `LocatorStrategy`) with a silent `UNKNOWN` fallback (D18).

This initiative addresses the definitional and boundary-hardening portion of those
findings: adopt the seven-concept ontology as the target conceptual model, state the
axes explicitly, sever the engine contract from the platform, give the hook system a
non-deprecated home, and open the extension vocabularies. It deliberately does NOT
attempt kernel/UI package separation, facade redesign, or multi-module split (see
"Future watch").

---

## Concern map

| ID | Concern | Layer | Location |
|----|---------|-------|----------|
| D2 | `UIEngineFactory` compile-time depends on `SeleniumEngine` (switch-on-string, P8) | Engine contract | `core/engine/UIEngineFactory.java` |
| D3 | `EngineBootstrap` carries `DriverFactory.Profile`; `core.engine` imports `core.driver` | Engine contract | `core/engine/EngineBootstrap.java` |
| D4 | Stable hook API lives inside the deprecated `core.interactions` package; the kernel imports through the legacy zone | Hooks | `core/interactions/hooks/` |
| D18 | `ActionCapability` and `LocatorStrategy` are closed enums; `UNKNOWN` silently applies browser wait hooks | Kernel / engine | `core/actions/ActionCapability.java`, `core/engine/LocatorStrategy.java` |
| O1-O9 | The target conceptual model (ontology) is undocumented; execution ownership and session concept unstated | Docs / decisions | `docs/decisions/`, `docs/architecture/` |

---

## Phase overview

| Phase | Goal | Risk | Key changes |
|-------|------|------|-------------|
| 1 | Define the kernel boundary and adopt the ontology | Low (docs only) | ADR-021 draft; invariants updated to name their neutrality axis; no code |
| 2 | Sever engine contract from platform | Medium | `UIEngineFactory` registry (absorbs/depends on P8); `EngineBootstrap` no longer carries `DriverFactory.Profile`; `core.engine` has zero `core.driver` and zero `core.engine.selenium` imports |
| 3 | Hook system ownership | Medium | Hook contract moves to a kernel-owned package; deprecated bridges remain in `core.interactions.hooks` |
| 4 | Open the extension vocabularies | Medium-High | `ActionCapability` and `LocatorStrategy` become open sets; `UNKNOWN` silent fallback removed |

Phase docs:

- [Phase 1 -- Define kernel boundary](phase-1-define-kernel-boundary.md)
- [Phase 2 -- Engine contract decoupling](phase-2-engine-contract-decoupling.md)
- [Phase 3 -- Hooks ownership](phase-3-hooks-ownership.md)
- [Phase 4 -- Extension vocabularies](phase-4-extension-vocabularies.md)

---

## Dependency rationale

Phase 1 before everything: the ADR is the authority the later phases implement.
Without it, Phases 2-4 are taste; with it, they are enforcement.

Phase 2 coordinates with `oop-violations-remediation` Phase 4 (P8, factory switch to
registry). If that initiative lands first, this Phase 2 shrinks to the
`EngineBootstrap` decoupling; if not, this phase absorbs P8 and the other initiative
marks it done. One owner per fix; never both.

Phase 3 is independent of Phase 2 but depends on Phase 1 (the ADR names the hook
system as kernel-owned).

Phase 4 last: it touches `ActionCapability`, which `oop-violations-remediation`
Phases 1-3 also touch (P3 dispatch removal, P7 capability ownership). Those must be
merged first so the capability surface is stable before it is opened.

**Rule**: nothing in Phase N depends on Phase N+1. Each phase compiles and passes
`mvn compile -q` and existing examples on its own before the next phase begins. Never mix
phases in one commit.

---

## Cross-initiative coordination

| Initiative | Relationship |
|---|---|
| `oop-violations-remediation` | P8 overlaps Phase 2; P3/P7 must precede Phase 4. |
| `generalize-element-into-target` | Independent but vocabulary-aligned: its `Target` root is the ontology's Target concept. Phase 1's ADR should cite it. No file overlap. |
| `locator-sync-trigger` | No overlap. |

---

## What does NOT change

- `Action.perform(UIEngine)` signature -- domain-axis generalization is future watch
- `VOID` facade surface (`navigateTo`, `getCurrentUrl`, etc.) -- future watch
- The `elements.api` / `core.actions` mutual dependency (D1) -- requires the ontology
  ADR to be accepted and a second domain to validate against; future watch
- Maven module count -- single artifact remains; build-level enforcement (D17) is its
  own future initiative
- `Interactions`, `Via`, `UIContext`, `ExecutionContext` -- legacy removal is a
  separate workstream
- Locator resolution pipeline internals
- All page object enums and capability interfaces

---

## Commit sequence

```
# Phase 1
docs(decisions): add ADR-021 interaction kernel boundary and ontology adoption
docs(architecture): state neutrality axis for each architecture invariant

# Phase 2
feat(engine): replace UIEngineFactory switch with engine registry
refactor(engine): EngineBootstrap carries engine name and opaque settings, drops DriverFactory.Profile

# Phase 3
refactor(hooks): move hook contract to kernel-owned package, keep deprecated bridges

# Phase 4
feat(actions): open ActionCapability to an extensible capability set
feat(engine): open LocatorStrategy to an extensible strategy set
fix(actions): remove UNKNOWN silent hook fallback, require explicit default profile
```

All commits follow Conventional Commits format. No em dashes. Imperative present tense.

---

## Future watch (do not act on these now)

Gated on a concrete second domain or second engine entering active development:

- Generalize `Action.perform(UIEngine)` toward the execution-owner concept that
  ADR-021 names (audit open decision AD2)
- Split the VOID facade's session-level operations into Interactions on the Session
  (ontology O4) instead of out-of-band facade methods
- Separate kernel and UI-domain content of `core.actions` (D1)
- Multi-module build enforcement of purity invariants (D17)
- Configuration identity: replace `driver.properties` as the bootstrap gate (C4)

These require two concrete implementations to validate against. Do not design them
speculatively.

---

## Versioning (CHANGELOG.md)

Scrapped -- no release of its own, ever. Changelog entries for its content ship
with the runtime-redesign releases that absorbed the phases: phase 1 (ADR-021) is
docs-only, no entry; phases 3 and 4 (hooks, vocabularies) under **0.5.0** (M2);
phase 2 (engine contract) under **0.6.0** (M3).
