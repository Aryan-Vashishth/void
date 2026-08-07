# `core.adapters` — External Framework Integration

Adapter layers that wire VOID's core capabilities to external test frameworks.

---

## Overview

Adapters translate between VOID's internal API and the conventions of external frameworks (e.g., Cucumber BDD). Each adapter is independently optional — VOID's core functions without any adapter present.

---

## Sub-Packages

| Package | Framework | Description |
|---------|-----------|-------------|
| `core.adapters.cucumber` | Cucumber BDD | Step definition classes mapping Gherkin steps to VOID's `Interactions` layer |

---

## `core.adapters.cucumber`

### Purpose

Provides Cucumber step definitions that wire BDD feature files to VOID's interaction layer. This allows teams using Gherkin-based test specifications to leverage VOID's element model and execution engine.

### Structure

```
cucumber/
├── CommonStepDef/
│   └── CommonStepDef.java    ← Shared step definitions (click, type, select, etc.)
└── package-info.java
```

### Key Points

- **Optional dependency** — Cucumber JARs are only needed if this package is used ([ADR-002](../../../../docs/decisions/accepted/002-cucumber-optional-dependency.md))
- Maps Gherkin steps like `When I click on "Submit"` to `Interactions.clickOn(element)`
- Uses VOID's enum-driven element model for type-safe step definitions

---

## Design Rules

| Rule | Rationale |
|------|-----------|
| Adapters are `@Internal` | Framework plumbing — may change without notice |
| No business logic in adapters | Only translation between frameworks |
| Each adapter is independently optional | Core works without any adapter |
| Adapters depend on `core.interactions` | They are consumers, not providers |

---

## Adding a New Adapter

To integrate a new framework:

1. Create a sub-package: `core.adapters.<framework>/`
2. Add a `package-info.java` documenting the integration
3. Implement translation classes that delegate to `Interactions` or `UIEngine`
4. Mark all classes `@Internal`
5. Ensure the framework dependency is `<optional>true</optional>` in `pom.xml`

---

## See Also

- `core.interactions.Interactions` — the interaction layer adapters delegate to
- ADR-002: Cucumber as Optional Dependency

