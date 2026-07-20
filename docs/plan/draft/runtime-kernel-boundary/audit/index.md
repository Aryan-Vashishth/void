# Runtime Kernel Boundary -- Audit

Pre-plan architecture audit for the runtime-kernel-boundary initiative.

## Contents

The audit lives in the shared audits tree rather than being duplicated here:

- [`docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`](../../../../audits/ongoing/architecture-audit-2026-07-domain-model.md)
  - Part I -- implementation audit: domains, bounded contexts, package structure,
    runtime purity, dependency direction, vocabulary (findings D1-D18, priorities
    Critical through Low)
  - Part II -- architecture ontology review: first-principles validation of the
    Runtime/Interaction/Capability/Target/Domain model (findings O1-O9, invariants
    I1-I12, open decisions AD1-AD3; recommends adding Session and explicitly
    modeling the execution-ownership gap, with the concept's name and shape
    deferred to ADR-021)

This initiative implements the Critical findings that are actionable today
(axes definition, D2, D3, D4, D18). D1 (kernel/UI separation) and D17
(build-level enforcement) are deliberately deferred; see "Future watch" in
[index.md](../index.md).

## Note

A post-implementation validation document belongs here after all four phases are
implemented and verified. It does not exist yet.
