# Phase 1 -- Define Kernel Boundary

Touches: `docs/decisions/pending-review/021-interaction-kernel-boundary.md` (new),
`CLAUDE.md` (invariants table wording), `docs/architecture/system-overview.md`
(design philosophy section). No Java files.

---

## Goal

Give the later phases (and every future initiative) a written authority for what the
interaction kernel is, which neutrality axis each rule governs, and what conceptual
model the redesign targets.

After this phase:

- ADR-021 exists in `docs/decisions/pending-review/` and defines:
  - The two neutrality axes: engine neutrality (within a domain) vs domain neutrality
    (across interaction media). Every existing invariant is assigned an axis.
  - The interaction kernel membership list: `Action`, `Flow`, `FlowExecutor`,
    `ActionProfile`, the hook contract, and action tracing. Concrete action classes
    (`ClickAction`, `TypeAction`, ...) are explicitly UI-domain content, not kernel.
  - Adoption of the ontology (Runtime, Session, Interaction, Capability, Target,
    Domain) and invariants I1-I7 and I9-I12 as the target conceptual model, with the
    declared scope limit (discrete, bounded interactions). Source: audit Part II
    recommendations 1-9.
  - Resolution of the audit's open decisions, which is exactly where these design
    choices belong:
    - AD2: name and shape the execution-owner concept (candidates: Executor,
      Dispatcher, Interpreter, Domain Runtime, Operation Handler, per-interaction
      strategies; the audit identifies the gap and its constraints without choosing).
    - AD1: session-to-domain cardinality (single, multiple, or composite sessions).
    - AD3: capability validation timing (eager before dispatch vs lazy at execution).
- `CLAUDE.md` architecture invariants each carry an axis marker (engine / domain).
- `system-overview.md` design philosophy references the ontology by ADR number instead
  of restating it.

This phase decides; it does not implement. Phases 2-4 implement the subset of ADR-021
that is actionable today.

---

## What does NOT change in this phase

- No Java source files
- No accepted ADRs (018-020 remain pending-review on their own track)
- No package structure
- Existing initiative drafts are cited, not modified

---

## Files changed

| File | Change |
|------|--------|
| NEW `docs/decisions/pending-review/021-interaction-kernel-boundary.md` | Axes, kernel membership, ontology adoption, invariants, AD1-AD3 resolutions, scope limit |
| `CLAUDE.md` | Invariants table: axis column or per-row axis note |
| `docs/architecture/system-overview.md` | Design philosophy cites ADR-021 |

---

## Commit

```
docs(decisions): add ADR-021 interaction kernel boundary and ontology adoption
docs(architecture): state neutrality axis for each architecture invariant
```

---

## Verification

```
# No code changed
git diff HEAD --name-only
# expected: only the three docs files

# Links resolve
grep -n "021-interaction-kernel-boundary" docs/architecture/system-overview.md CLAUDE.md
```

---

## Phase complete when

- [ ] ADR-021 exists and covers axes, kernel membership, ontology, invariants, the
      AD1-AD3 resolutions (including the execution-owner concept's name and shape),
      and the scope limit.
- [ ] Every invariant in `CLAUDE.md` names its axis.
- [ ] No Java file changed.
