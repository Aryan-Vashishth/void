# Audits -- Backlog -- OOP Violations

New OOP violations discovered during development that are not yet assigned to a remediation
initiative phase.

Each file documents one violation: where it was found, which principle it breaks, the risk
level, and the recommended fix. Violations here are candidates for inclusion in the next
phase of `docs/plan/draft/oop-violations-remediation/` or a new initiative.

## When a file is added here

A violation is logged here when it is found incidentally during unrelated work -- not during
a dedicated audit pass. If a dedicated audit finds multiple violations at once, they go into
the remediation plan directly.

## Index

| File | Principle | Area | Risk |
|---|---|---|---|
| [i1-domutils-direct-webdriver-calls.md](i1-domutils-direct-webdriver-calls.md) | ADR-007 | `core/utils/web/DOMUtils.java` | High |
| [i1-tablehandler-direct-webdriver-calls.md](i1-tablehandler-direct-webdriver-calls.md) | ADR-007 | `core/utils/web/TableHandler.java` | High |
| [i1-waitutils-direct-webdriver-calls.md](i1-waitutils-direct-webdriver-calls.md) | ADR-007, ADR-018 | `core/utils/web/WaitUtils.java` | High |
| [oop-driverfactory-instanceof-preference-dispatch.md](oop-driverfactory-instanceof-preference-dispatch.md) | OCP | `core/driver/DriverFactory.java:722` | Low |
