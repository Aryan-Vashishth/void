# Phase 6 — Properties Template Generation

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — new CLI command; no changes to the runtime

---

## Objective

Introduce a CLI command that generates a pre-populated properties template from a page's enum declarations, so developers never type a locator key by hand.

---

## Open Decisions Required

**Open Decision 2** and **Open Decision 5** must be resolved before implementation begins:

> **Decision 2:** Command name and invocation style; scope (single page vs directory vs project); merge behavior when a file already exists.

> **Decision 5:** Exact regeneration strategy — recommended: merge-with-preserve (add new keys, retain existing values, flag stale keys as warnings; require explicit flag for deletion).

---

## Context

Currently, developers type locator keys into properties files by hand. A typo produces a runtime resolution failure, not a compile-time error.

Enum constant names are the authoritative source of locator identity. The keys are fully derivable. A generator should write them.

---

## Input / Output

Given:

```java
public interface DemoLoginPage {

    enum Credentials implements Typeable {
        USERNAME_INPUT,
        PASSWORD_INPUT
    }

    enum Button implements Clickable {
        LOGIN_BUTTON
    }
}
```

Generator produces at `src/main/resources/tests/demo/pages/DemoLoginPage/locators.properties`:

```properties
# DemoLoginPage — locators
# Generated from enum declarations. Do not edit keys. Fill values only.

USERNAME_INPUT=
PASSWORD_INPUT=
LOGIN_BUTTON=
```

---

## Regeneration Behavior (Recommended)

When a properties file already exists:

- Enum constants with no corresponding key → key added with empty value
- Keys already present → preserved exactly with current value
- Keys with no matching enum constant → flagged as stale (warning only; never auto-deleted)

The generator is safe to run at any point in development without risk of data loss.

---

## Affected Files

- New CLI command / entry point in the existing CLI tool
- No changes to the runtime locator resolution

---

## Checklist

### Design
- [ ] Resolve Open Decision 2 — confirm command name and invocation
- [ ] Resolve Open Decision 5 — confirm merge behavior
- [ ] Document the output format (header comment, key ordering, blank lines)

### Implementation
- [ ] Implement enum constant discovery from a compiled class or source file
- [ ] Implement key generation from constant names
- [ ] Implement merge-with-preserve behavior for existing files
- [ ] Implement stale-key warning output
- [ ] Wire up CLI command entry point

### Tests
- [ ] Unit test: page with no existing file produces a fully pre-populated template
- [ ] Unit test: page with an existing file — new constants are added, existing values preserved
- [ ] Unit test: stale keys produce a warning and are not deleted
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- Running the command on a page produces a correct properties template
- Re-running on an existing file merges correctly without data loss
- Stale keys are flagged, not silently removed

---

## What NOT to Do

- Do not overwrite existing locator values — always preserve them
- Do not auto-delete stale keys without an explicit flag
- Do not introduce a new JSON generation tool — the existing CLI (Phase 7) handles that step

---

*MIT License Copyright (c) 2025-2026 VOID Project*
