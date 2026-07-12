# Phase 7 — Runtime Repository Generation

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — repositioning of existing CLI; no new tool introduced

---

## Objective

Formally position the existing JSON Migration CLI as the "Runtime Repository Generation" step in the developer workflow, completing the pipeline from enum declarations to a runtime-ready JSON repository.

---

## Open Decision Consideration

**Open Decision 4** is relevant here:

> How far `LocatorRepository` abstracts the underlying source — and whether future repository types (YAML, remote) can be introduced at that level without touching the CLI or the runtime.

This phase does not need to resolve it, but the CLI's output should conform to whatever the `LocatorRepository` interface expects.

---

## Context

The existing CLI converts a filled properties file into a JSON repository:

```
locators.properties  →  CLI  →  locators.json
```

This capability already exists. This phase:
- Names it officially as "Runtime Repository Generation" in documentation, workflow descriptions, and developer guides
- Confirms it integrates correctly with the convention introduced in Phase 5
- Does not introduce a new tool

---

## Pipeline Position

```
Enum constants
      │
      ▼
Generated properties template  (Phase 6)
      │
Developer fills locator values
      │
      ▼
Runtime Repository Generation (this phase — existing CLI)
      │
      ▼
locators.json  →  Runtime
```

---

## Affected Files

- CLI documentation / README
- Developer workflow documentation
- No changes to the runtime itself

---

## Checklist

### Verification
- [ ] Confirm the existing CLI correctly reads a properties file at the conventional path
- [ ] Confirm the CLI writes the JSON file at the path the runtime will look up (Phase 5 convention)
- [ ] Confirm no code changes to the CLI are required for Phase 5 compatibility

### Documentation
- [ ] Update CLI documentation to name this step "Runtime Repository Generation"
- [ ] Add the 7-step developer workflow to the appropriate guide

### Tests
- [ ] Integration test: properties file at conventional path → CLI → JSON at conventional path → runtime resolves correctly

---

## Exit Criteria

- The CLI is documented as the Runtime Repository Generation step
- It reads from and writes to the conventional paths established in Phase 5
- The full pipeline (enum → properties → CLI → JSON → runtime) works end-to-end

---

## What NOT to Do

- Do not introduce a new JSON generation tool — the existing CLI is the tool for this step
- Do not change the CLI's internal implementation unless required for Phase 5 compatibility

---

*MIT License Copyright (c) 2025-2026 VOID Project*
