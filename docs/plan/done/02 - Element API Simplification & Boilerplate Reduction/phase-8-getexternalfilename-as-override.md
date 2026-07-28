# Phase 8 — `getExternalFileName()` as an Override

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — changes the semantic role of an existing method; must not break any current usage

---

## Objective

Reposition `getExternalFileName()` from a required declaration to an advanced override, while preserving all existing behavior for elements that already use it.

---

## Context

Currently, every element must implement `getExternalFileName()` to declare the path to its locator file:

```java
@Override
public String getExternalFileName() {
    return LOCATOR_FILE;
}
```

With the deterministic repository convention in place (Phase 5), this declaration is only needed for exceptions — pages with shared repositories, external sources, or custom structures.

---

## New Role

`getExternalFileName()` becomes an advanced override. When it returns a non-null value, that path takes precedence over the convention. When it returns `null`, the convention applies.

Override use cases:
- Shared repositories used by multiple pages
- Generated repositories from external sources
- Plugin or integration-supplied repositories
- Custom project structures

---

## Change (as implemented — diverges from original spec)

The original spec proposed `return null` and deferring convention-based resolution to Phase 9.
The implemented default instead encodes the convention directly, probing the classpath in
priority order: `.json` first (under `locators/json/`), then `.properties` (under
`locators/properties/`), falling back to `.json` if no file is found.

```java
default String getExternalFileName() {
    Enum<?> e = (Enum<?>) this;
    Class<?> enumClass = e.getDeclaringClass();
    Class<?> pageClass = enumClass.getEnclosingClass();
    Class<?> target = pageClass != null ? pageClass : enumClass;
    return resolveLocatorFileName(target.getSimpleName());
}

// private static — candidates kept internal, not yet configurable
```

The priority list is internal to a `private static` method inside `Element` (interface fields
are implicitly public, so the list cannot be a field and remain internal). Adding a new format
requires one entry in the candidates array plus a matching `LocatorSource` registration.

Explicit overrides on existing elements continue to take precedence unchanged.

---

## Affected Files

- `src/main/java/elements/api/Element.java` (or the interface that declares `getExternalFileName()`) — add default returning `null`
- Verify that no element currently relies on `getExternalFileName()` returning a non-null value as a signal — all should now rely on the resolution order in Phase 9

---

## Checklist

### Analysis
- [ ] Identify all current implementations of `getExternalFileName()` across the codebase
- [ ] Confirm all non-null returns are intentional overrides (not just satisfying a required contract)

### Implementation
- [ ] Add `default String getExternalFileName() { return null; }` to the appropriate interface
- [ ] Confirm existing non-null overrides still take precedence

### Tests
- [ ] Unit test: element with no override — `getExternalFileName()` returns `null`
- [ ] Unit test: element with an explicit override — returns the declared path
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `getExternalFileName()` has a default implementation returning `null`
- All existing explicit overrides continue to work
- No element is broken by the addition of the default

---

## What NOT to Do

- Do not remove `getExternalFileName()` from the API — it is the escape hatch
- Do not change the resolution logic here — that is Phase 9
- Do not auto-migrate existing implementations — leave them in place; Phase 11 handles cleanup

---

*MIT License Copyright (c) 2025-2026 VOID Project*
