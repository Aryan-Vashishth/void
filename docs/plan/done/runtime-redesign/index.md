# runtime-redesign -- Completed Initiatives Index

Initiatives completed on `initiative/runtime-redesign` and merged to `main` as version 0.5.0.
Listed in implementation sequence. See `docs/plan/draft/runtime-redesign/index.md` for the
full roadmap including I4-I9.

---

## M1 -- Foundations (merged earlier, no version bump)

| Initiative | File | Key deliverable |
|---|---|---|
| I0 -- Foundations | [initiative-0-foundations.md](initiative-0-foundations.md) | ADR-021; `KernelBoundaryRulesTest` baseline; workflow docs |

---

## M2 -- Neutral Vocabulary (0.5.0)

| Initiative | File | Key deliverable |
|---|---|---|
| I1 -- Target Model | [initiative-1-target-model.md](initiative-1-target-model.md) | `core.target.Target` root; `Element` renamed `UIElement` |
| I2 -- Kernel Extraction | [initiative-2-kernel-extraction.md](initiative-2-kernel-extraction.md) | Hook contracts to `core.actions.hooks`; `ElementAction` family to `elements.api.actions`; kernel purity gate |
| I3 -- Capability Model | [initiative-3-capability-model.md](initiative-3-capability-model.md) | `ActionCapability` opened to extensible interface; UNKNOWN silent fallback removed; Web-domain ownership declared |

**Full-system audit:** [audit/m2-full-system-audit.md](audit/m2-full-system-audit.md)

---

## Pending (draft)

I4-I9 remain in `docs/plan/draft/runtime-redesign/`. See that directory's `index.md`.
