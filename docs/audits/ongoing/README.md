# Audits -- Ongoing

Audits with open findings, partially addressed items, or work currently in progress.

Files here represent active architectural questions. Move to `fulfilled/` only when all blocking findings are resolved and verified.

## Contents

- `architecture-audit-2026-05.md` -- full codebase audit scoped to engine-swap readiness, coupling, and migration risks; some findings addressed, structural coupling questions remain open pending engine-decoupling plan
- `2026-05-external-readiness-audit.md` -- external readiness review from a senior-engineer perspective; portfolio and documentation gaps partially addressed, some remaining
- `architecture-audit-2026-07-domain-model.md` -- two-part audit: Part I covers domains, bounded contexts, package structure, dependency direction, and vocabulary (findings D1-D18); Part II is a first-principles ontology review of the Runtime/Interaction/Capability/Target/Domain model (findings O1-O9, invariants I1-I12, open decisions AD1-AD3; recommends adding Session and explicitly modeling the execution-ownership gap, shape deferred to ADR-021). Seeds `docs/plan/draft/runtime-kernel-boundary/`
