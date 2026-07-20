# I2 -- Kernel Extraction

Objective: the interaction kernel (Action, Flow, FlowExecutor, ActionProfile, hooks,
trace) becomes physically separable: no imports from the legacy zone, no imports
from the UI element model, and its boundary is machine-enforced.

---

## Phase 2.1 -- Hooks ownership (from superseded runtime-kernel-boundary phase 3)

- **Objective**: the stable hook contract leaves the deprecated
  `core.interactions` package for a kernel-owned package; old types remain as
  deprecated bridges (old interface extends new; constant classes delegate).
- **Motivation**: audit D4 -- the frozen package currently owns the living API and
  the kernel imports through the legacy zone.
- **Scope / files**: new kernel hook package (5 files); `core/interactions/hooks/*`
  become bridges; import updates in `core/actions/*`, engine implementation, demo
  and test sources.
- **Dependencies**: 0.1, 0.2. Independent of I1.
- **Risks**: (arch) none, relocation only; (compat) hook API is Stable tier -- the
  bridge pattern (old extends new) keeps lambdas and implementations compiling; a
  dedicated test implements the old type and passes it to new-typed call sites.
- **Rollback**: revert; bridges make this a two-commit change at most.
- **Validation**: suite green; fitness check added: kernel packages import nothing
  from `core.interactions.*`.
- **Exit criteria**: `grep -rn "core.interactions" src/main/java/core/actions` empty;
  bridge compatibility test green.
- **ADR / docs**: hooks-pipeline.md updated; Migration Ledger row (bridges die 9.3).
- **Migration notes**: CHANGELOG note; old imports keep working until 9.3.

## Phase 2.2 -- Kernel/UI action split

- **Objective**: within the action layer, separate the neutral contracts (Action,
  profile contracts, hook-chain wrapper, trace) from the 17 concrete UI actions and
  their three abstract family bases. The concrete actions are declared UI-domain
  content and housed accordingly; the kernel package retains only neutral types.
- **Motivation**: audit Part I bounded-context finding: "the kernel/UI boundary runs
  through the middle of `core.actions`, invisible in the package structure."
- **Scope / files**: package relocation of ~20 concrete/abstract action classes plus
  `ActionProfiles`' capability-specific constants (which are UI vocabulary); import
  updates in `elements.api.capability` (the emitters) and tests. No behavior change.
- **Dependencies**: 1.4; oop-remediation P1-P3 merged (their deletions --
  HookedAction, ActionLabeled -- shrink what must move). Never parallel with I1/I3.
- **Risks**: (arch) misclassification -- a type that looks neutral but encodes UI
  policy (for example capability-specific profile constants) must go UI-side; the
  ADR-021 kernel list is the arbiter; (compat) concrete action types are Beta tier;
  external code rarely names them (capability interfaces return them opaquely), so
  breakage is limited to imports, covered by CHANGELOG.
- **Rollback**: revert relocation commit; no signatures change.
- **Validation**: suite green; fitness check tightened: kernel action package
  contains no type referencing `UIElement`, `ElementRole`, or capability constants.
- **Exit criteria**: two package populations with the boundary between them equal to
  the ADR-021 kernel list, exactly.
- **ADR / docs**: core-packages.md and actions.md updated.
- **Migration notes**: import-change table in CHANGELOG.

## Phase 2.3 -- Cycle break

- **Objective**: the mutual dependency `elements.api` <-> kernel becomes
  one-directional: UI capabilities and UI actions may reference each other freely
  (one bounded context), and both reference the kernel; the kernel references
  neither.
- **Motivation**: audit D1 -- the cycle is proof the two packages are one context;
  after 2.2 the remaining kernel->elements edges are enumerable and removable.
- **Scope / files**: residual kernel references to `elements.meta`
  (`ElementRole` in resolve paths -- moves behind the UI-domain edge), `EnumClassRegistry`
  references if any; `ElementActions` factory (`@Internal`) re-homed with the UI
  actions.
- **Dependencies**: 2.2, 1.4.
- **Risks**: (arch) the resolve path is the hard edge -- `UIEngine.resolve(UIElement,
  role)` is UI-domain vocabulary and stays on the UI side of the line; the kernel
  must not need it (it doesn't: resolution happens inside concrete actions, which
  are UI-side after 2.2); (compat) none expected beyond imports.
- **Rollback**: revert.
- **Validation**: suite green; fitness check: zero imports from `elements.*` in
  kernel packages -- the check that makes D1 unrecurrable.
- **Exit criteria**: dependency direction UI-domain -> kernel only.
- **ADR / docs**: elements.md and actions.md updated.
- **Migration notes**: none external.

## Phase 2.4 -- Boundary ratchet and kernel purity gate

- **Objective**: consolidate 2.1-2.3 into a named, tested boundary: a "kernel
  purity" fitness group asserting the kernel imports only JDK, logging, annotations,
  and `core.target`; document it as a standing invariant with its axis.
- **Motivation**: milestone M2 gate; guardrail mechanics.
- **Scope / files**: verification classes; architecture-rules.md.
- **Dependencies**: 2.1-2.3.
- **Risks**: false-pass risk as in 0.2; same mutation-demo mitigation.
- **Rollback**: n/a (additive checks).
- **Validation**: mutation demo recorded.
- **Exit criteria**: kernel purity check green and demonstrated to fail on
  violation; invariant added to CLAUDE.md table with axis = domain.
- **Migration notes**: none.
