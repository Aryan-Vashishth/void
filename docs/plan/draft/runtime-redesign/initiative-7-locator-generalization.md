# I7 -- Locator Generalization

Objective: the locator subsystem's open questions are settled the same way as the
rest: strategy set open, descriptor ownership on the domain side, Selenium's By
confined to the platform edge and then removed from the resolution surface.

Runs parallel to I5/I6 (disjoint files) after I4.

---

## Phase 7.1 -- Open the strategy set

- **Objective**: `LocatorStrategy` stops being a closed enum (XPATH, CSS, ID, NAME);
  becomes an extensible set with constants preserved; `ByParser` prefix handling
  maps onto the open set; unknown prefixes keep failing fast.
- **Motivation**: audit D18 (second closed vocabulary); June audit crack #5.
- **Scope / files**: `core/engine/LocatorStrategy.java` (or post-7.2 home),
  `core/resolvers/locator/parser/*`, descriptor construction sites.
- **Dependencies**: 0.2; independent of I5/I6. Not parallel with 7.2.
- **Risks**: (compat) enum-typed caller code breaks as in 3.1; same in-phase
  migration rule; (arch) strategies are engine-interpreted -- the open set must not
  imply every engine supports every strategy; unsupported-strategy failure is the
  executor's explicit error, tested.
- **Rollback**: revert; constants preserved.
- **Validation**: suite green; test: custom strategy registers and round-trips to
  the engine's unsupported-strategy error without editing `core.engine`.
- **Exit criteria**: no exhaustive iteration over strategies outside deprecated
  paths; fitness check added.
- **ADR / docs**: locator-resolution.md.
- **Migration notes**: CHANGELOG for enum-typed callers.

## Phase 7.2 -- Descriptor ownership

- **Objective**: `LocatorDescriptor`'s ownership moves to the Web-domain side of
  the line (it is UI vocabulary: locator, DOM-scoped parent), and the kernel/neutral
  contract handles target descriptions opaquely; the neutral dispatch path (4.4)
  carries no locator-shaped type.
- **Motivation**: audit priority 13 (descriptor housed in the engine contract
  package while produced by resolvers and carried by actions); June audit
  "LocatorDescriptor evolves" verdict -- structure survives, naming/DOM semantics
  are domain-bound.
- **Scope / files**: descriptor and strategy relocation to the web/UI area;
  `UIEngine` signatures (already UI-side); hook signature interplay resolved by
  the 4.4 bridge decision; resolver imports.
- **Dependencies**: 4.3, 4.4 (the neutral edge must exist so the descriptor can
  leave it), 7.1.
- **Risks**: (arch) hooks -- `(engine, descriptor)` is the stable hook signature;
  after this phase the descriptor type is domain-owned, which is correct (hooks as
  currently shipped are web-domain hooks); the neutral hook contract from 4.4
  carries the opaque form; getting this split wrong re-couples the kernel;
  (compat) import moves for anyone naming the descriptor type; CHANGELOG.
- **Rollback**: revert relocation.
- **Validation**: suite green; fitness check: neutral/kernel packages do not import
  the descriptor type.
- **Exit criteria**: descriptor and strategy live with the web domain; kernel
  purity check still green.
- **ADR / docs**: locator-resolution.md, elements.md.
- **Migration notes**: import table in CHANGELOG.

## Phase 7.3 -- Delete the By-returning resolution path

- **Objective**: `LocatorResolver.resolve() -> By` and its remaining deprecated
  consumers are removed; `By` handling is confined to `ByParser` internals and the
  Selenium executor; the ADR-020 invariant ("no new By-returning resolver calls")
  becomes "no By-returning resolver exists."
- **Motivation**: audit D14/H1 lineage; dual resolution returns are the last
  Selenium type on the resolution surface.
- **Scope / files**: `core/resolvers/locator/api/LocatorResolver.java`, deprecated
  consumers (legacy Interactions call sites are deleted in 9.3 -- THIS phase may
  only proceed once its non-legacy consumers are zero; legacy consumers may force
  ordering after 9.1/9.2 -- resolved at activation with a consumer inventory).
- **Dependencies**: 7.2; consumer inventory gate (possibly after 9.1).
- **Risks**: (compat) any external caller of resolve() breaks -- it has been
  deprecated with a replacement (`resolveDescriptor`) since ADR-020; removal is
  CHANGELOG'd with the replacement; (arch) none, pure deletion.
- **Rollback**: restore method from history (isolated deletion commit).
- **Validation**: suite green; `grep -rn "openqa.selenium.By" src/main/java` hits
  only parser internals, the Selenium executor, and pending-deletion legacy.
- **Exit criteria**: grep result as above; Migration Ledger row closed.
- **ADR / docs**: locator-resolution.md; ADR-020 invariant superseded note in
  ADR-021 consequences.
- **Migration notes**: CHANGELOG removal entry.
