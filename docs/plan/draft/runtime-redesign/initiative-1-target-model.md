# I1 -- Target Model

Objective: a domain-neutral Target root exists, the UI element model becomes an
explicit specialization of it, and kernel types stop needing UI vocabulary to refer
to the subject of an interaction.

Phases 1.1-1.3 are the merged `generalize-element-into-target` draft; its phase docs
are the authoritative text and are lifted into this initiative when activated.

## Program context

**Why this initiative exists.** Every later re-typing phase needs a neutral answer
to "what is the subject of an interaction?" Today the answer is `Element`, a
UI-named, UI-shaped type, which is the root of the kernel/UI fusion (audit D1): the
kernel cannot shed UI vocabulary while its subject type IS the UI element. Target
is the ontology's subject concept made real.

**Why it is sequenced here.** It is the cheapest structural change with the widest
downstream payoff, and it is already validated (the merged draft carries its own
pre-plan audit). I2's cycle break is literally impossible before a neutral subject
type exists to retreat to, so I1 must precede all kernel work.

**What architectural boundary it owns.** The subject-side vocabulary line: which
members are neutral (display text, args -- Target) and which are UI (locator keys,
roles, files -- UIElement). Once drawn here, that line is what I2 and I7 enforce.

**What it deliberately does not own.** The action/kernel package split (I2),
capability semantics (I3), resolution machinery (I7). It does not generalize
`ElementRole` or invent a non-UI addressing concept -- both stay UI-side until a
second domain provides real requirements (merged draft's future-watch discipline).

---

## Phase 1.1 -- Introduce Target (merged draft phase 1)

- **Objective**: `core.target.Target` exists: display text, args, effective-args --
  the members with zero UI semantics. New file only; nothing else changes.
- **Motivation**: draft concern C2; ontology Target concept.
- **Scope / files**: one new file.
- **Dependencies**: 0.1 (Target's javadoc states its ontology role).
- **Risks**: minimal by design.
- **Rollback**: delete the file.
- **Validation**: compiles in isolation; zero imports from `core.engine`,
  `elements`, Selenium (fitness check added).
- **Exit criteria**: per merged draft phase 1 checklist.
- **ADR / docs**: none new (covered by ADR-021 and the merged draft).
- **Migration notes**: none.

## Phase 1.2 -- Rename Element to UIElement, extend Target (merged draft phase 2)

- **Objective**: the UI-only scope of the element contract is explicit in its name;
  `UIElement extends Target`; all imports, implements clauses, and the
  `UIEngine.resolve()` parameter updated.
- **Motivation**: draft concerns C1, C3; audit D1 groundwork.
- **Scope / files**: `elements/api/*`, every page-object enum's implements line,
  `core/engine/UIEngine.java` parameter, imports across `core.actions`,
  `core.resolvers`, `dsl`, demo pages. Mechanical rename; no semantic change.
- **Dependencies**: 1.1; green baseline commit immediately before (cross-risk #3).
- **Risks**: (arch) none, pure rename; (compat) breaks external page objects
  compiled against `Element` -- if external consumers exist at this time, a
  deprecated `Element` alias ships and enters the Migration Ledger (deleted 9.5).
- **Rollback**: revert the rename commit (single commit, pure rename).
- **Validation**: full suite green; `grep -rn "\bElement\b"` confined to alias and
  docs per merged draft phase 2 verification.
- **Exit criteria**: per merged draft phase 2 checklist.
- **Migration notes**: CHANGELOG migration entry for external page objects.

## Phase 1.3 -- Validation and cleanup (merged draft phase 3)

- **Objective**: regression pass, documentation audit, no stray `Element`
  references.
- **Scope / files**: docs, leftover references.
- **Dependencies**: 1.2.
- **Risks / Rollback / Validation / Exit**: per merged draft phase 3.

## Phase 1.4 -- Kernel target-neutrality

- **Objective**: kernel types that only need neutral semantics (labels, args,
  identity for logging and tracing) are typed against `Target`, not `UIElement`;
  UI-only members (locator keys, roles, files) are referenced exclusively by
  UI-domain code. This is the preparatory cut for the cycle break in 2.3.
- **Motivation**: audit D1 -- the kernel currently cannot be pointed at without
  dragging the UI model along; the June audit's LocatorDescriptor/role analysis.
- **Scope / files**: `core/actions/ElementAction.java` (base-type references),
  `core/actions/trace/*` (labels), `core/executor`, log call sites. Concrete UI
  actions intentionally keep `UIElement` -- they are UI-domain content (see 2.2).
- **Dependencies**: 1.2, 1.3; must not run parallel with any I2 phase.
- **Risks**: (arch) over-generalizing -- members that look neutral but carry UI
  assumptions (role-defaulting) must stay UI-side; the phase reviews each member
  against the ADR-021 kernel list; (compat) Beta-tier signatures may change;
  in-repo callers migrate in-phase.
- **Rollback**: revert; no data or config formats involved.
- **Validation**: suite green; new fitness check: kernel trace/log paths compile
  against `Target` only.
- **Exit criteria**: no kernel type references `UIElement` except the concrete
  UI actions and their abstract family bases (relocated in 2.2).
- **ADR / docs**: `docs/architecture/actions.md` updated.
- **Migration notes**: none external (Beta tier).
