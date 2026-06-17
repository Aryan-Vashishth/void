# Documentation Index

This directory contains only public, stable documentation for VOID (Virtual Object Interaction Domain), an interaction runtime for modeling and executing interaction workflows.

Current execution narrative:
- `Element → Action → Flow → FlowExecutor → UIEngine`
- Test code describes intent; the runtime handles execution.
- Selenium today, Playwright-ready by contract, engine-agnostic by design.

## Structure

- `architecture/` - execution model, configuration, hooks, locator resolution, logging
- `decisions/` - ADRs and accepted design decisions
- `audits/` - architecture and project quality audits
- `images/` - diagrams and report screenshots referenced by docs

## Quick links

- `architecture/quick-start.md`
- `architecture/system-overview.md`
- `architecture/core-packages.md` — detailed reference for every `core/` sub-package
- `architecture/configuration-reference.md`
- `architecture/logging-reference.md`
- `architecture/locator-resolution.md`
- `architecture/hooks-pipeline.md`
- `decisions/accepted/`
- `audits/architecture-audit-2026-05.md`
- `audits/2026-05-external-readiness-audit.md`

## Notes

Internal planning material was moved out of this public surface.

