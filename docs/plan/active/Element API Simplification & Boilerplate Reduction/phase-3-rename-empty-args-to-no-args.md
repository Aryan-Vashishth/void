# Phase 3 — Rename `EMPTY_ARGS` to `NO_ARGS`

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — API rename; all call sites must be updated in the same commit

---

## Objective

Replace the `EMPTY_ARGS` constant name with `NO_ARGS` throughout the codebase.

`EMPTY_ARGS` describes state — what the array contains (nothing).  
`NO_ARGS` communicates intent — what the element needs (no arguments).

Names should reflect meaning, not contents.

---

## Context

The current constant in `Element`:

```java
Object[] EMPTY_ARGS = new Object[0];
```

Phase 2's default `getArgs()` references this constant. The rename should happen before or alongside Phase 2 so the default is introduced under the correct name.

---

## Change

In `Element`:

```java
// Before
Object[] EMPTY_ARGS = new Object[0];

// After
Object[] NO_ARGS = new Object[0];
```

All references to `EMPTY_ARGS` across the codebase must be updated in the same commit.

---

## Affected Files

- `src/main/java/elements/api/Element.java` — rename the constant
- All files that reference `Element.EMPTY_ARGS` or `EMPTY_ARGS` directly — update to `NO_ARGS`

Run a global search for `EMPTY_ARGS` before committing.

---

## Checklist

### Preparation
- [ ] Search codebase for all references to `EMPTY_ARGS` and list them
- [ ] Confirm no external consumers of this constant exist outside the project

### Implementation
- [ ] Rename `EMPTY_ARGS` → `NO_ARGS` in `Element`
- [ ] Update every call site found in the search above

### Tests
- [ ] Confirm compilation succeeds — no remaining references to `EMPTY_ARGS`
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `EMPTY_ARGS` does not appear anywhere in the codebase
- `NO_ARGS` is the single canonical empty-args constant
- All tests pass

---

## What NOT to Do

- Do not introduce a deprecated alias for `EMPTY_ARGS` — this is an internal constant, not a public API
- Do not split the rename across multiple commits — partial rename leaves the codebase in an inconsistent state

---

*MIT License Copyright (c) 2025-2026 VOID Project*
