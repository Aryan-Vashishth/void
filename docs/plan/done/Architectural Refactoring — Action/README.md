# Architectural Refactoring — Action

Completed plans for the full action layer refactoring. Two sub-projects were executed in sequence.

## Sub-projects

- `done/Action Profiles Refactor and Scalability Plan/` -- phases 0-5: introduced `ActionProfile`, `safely()` / `reliable()` / `debug()` / `raw()`, capability-aware profile defaults, `Profiles` preset library, custom profile builder, and config-driven default profile
- `done/Architectural Refactoring — Action Ownership with Layering Principle/` -- phases 13-20: introduced `ElementAction` base class, 17 concrete action subclasses, `ClickableElementAction` / `TypeableElementAction` / `SelectableElementAction` abstract intermediaries, established the Architectural Layering Principle (ADR-013), deleted central dispatch and capability-owned execution policy
- `archive/` -- superseded drafts from earlier iterations of the Action Profiles plan

## Outcome

ADRs produced: 012 (ElementActions Factory Scope), 013 (Architectural Layering Principle), 014 (Concrete Actions over Anonymous Lambdas), 015 (Capability Self-Description).
