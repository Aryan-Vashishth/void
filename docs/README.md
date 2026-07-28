# Documentation Index

This directory contains all documentation for VOID (Virtual Object Interaction Domain), an interaction runtime for modeling and executing UI interaction workflows.

Current execution narrative:
- `UIElement -> Action -> Flow -> FlowExecutor -> UIEngine`
- Test code describes intent; the runtime handles execution.
- Selenium today, Playwright-ready by contract, engine-agnostic by design.

## Structure

- `architecture/` -- execution model, element layer, actions, hooks, locator resolution, logging, configuration
- `decisions/` -- Architecture Decision Records (ADRs) for accepted design decisions
- `audits/` -- architecture and project quality audits (backlog, ongoing, fulfilled)
- `plan/` -- implementation plans (done and draft)
- `images/` -- diagrams and report screenshots referenced by docs

## Quick links

### Architecture
- `architecture/quick-start.md` -- getting started with elements, actions, and flows
- `architecture/system-overview.md` -- full system description, capability table, execution flow
- `architecture/elements.md` -- element layer: UIElement interface, capability hierarchy, LocatorFamily
- `architecture/actions.md` -- action layer: concrete action types, profiles, hooks, extension guide
- `architecture/locator-resolution.md` -- locator resolution pipeline and LocatorContext
- `architecture/hooks-pipeline.md` -- before/after hook engine
- `architecture/core-packages.md` -- detailed reference for every `core/` sub-package
- `architecture/configuration-reference.md`
- `architecture/logging-reference.md`

### Decisions
- `decisions/accepted/` -- all 17 accepted ADRs (001 through 017)

### Audits
- `audits/ongoing/architecture-audit-2026-05.md`
- `audits/ongoing/2026-05-external-readiness-audit.md`
- `audits/backlog/domain-agnostic-runtime-audit-2026-06.md`

### Plans
- `plan/draft/oop-violations-remediation/` -- next: OOP violations remediation (4 phases)
- `plan/draft/engine-decoupling/` -- next: engine decoupling
- `plan/done/Element API Simplification & Boilerplate Reduction/` -- completed v0.3.0 work
