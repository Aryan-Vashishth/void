# Architecture

Reference documentation for VOID's execution model and subsystems.

## Contents

| Document | Covers |
|---|---|
| `quick-start.md` | Defining elements, emitting actions, composing flows, running sessions |
| `system-overview.md` | Full system description, capability table, architecture invariants, execution flow |
| `elements.md` | Element interface contract, capability hierarchy, LocatorFamily, ElementSupport, LocatorRoles |
| `actions.md` | ElementAction hierarchy, concrete action types, ActionProfiles, extension guide |
| `locator-resolution.md` | Three-step locator resolution, LocatorContext, mixed strategies, caching |
| `hooks-pipeline.md` | Before/after hook engine, ActionHandler types, hook composition |
| `core-packages.md` | Detailed reference for every sub-package under `core/` |
| `configuration-reference.md` | `driver.properties`, `test.properties`, config keys |
| `logging-reference.md` | CustomLogger channels, ANSI themes, log format |

## Decision traceability

All architectural choices are backed by ADRs in `../decisions/accepted/`. The decision list in `system-overview.md` links each claim to its ADR.
