# Phase 2 — Default Empty Arguments

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — additive default; no existing behavior removed

---

## Objective

Eliminate the repetitive `getArgs()` override that returns an empty array by providing a shared default in `Element`.

---

## Context

Nearly every element enum implements an identical method:

```java
@Override
public Object[] getArgs() {
    return new Object[0];
}
```

This implementation carries no meaning. It satisfies a contract that the framework could satisfy automatically. The allocation of a new `Object[0]` on every call is also wasteful when a shared constant exists.

---

## Change

Add a default implementation to `Element`:

```java
default Object[] getArgs() {
    return NO_ARGS;
}
```

Where `NO_ARGS` is the constant introduced in Phase 3. The default references `NO_ARGS` directly.

Dynamic elements override as before:

```java
PRODUCT_ROW {
    @Override
    public Object[] getArgs() {
        return new Object[]{ dynamicValue };
    }
}
```

Or via a `.with(...)` helper if one exists.

---

## Affected Files

- `src/main/java/elements/api/Element.java` — add `NO_ARGS` constant and default `getArgs()`

> **Note:** `NO_ARGS` is renamed from `EMPTY_ARGS` in Phase 3. Coordinate with Phase 3 — implement together or ensure Phase 3 lands first.

---

## Checklist

### Implementation
- [ ] Add `Object[] NO_ARGS = new Object[0]` constant to `Element` (or confirm Phase 3 added it)
- [ ] Add `default Object[] getArgs()` returning `NO_ARGS` to `Element`
- [ ] Confirm dynamic elements that override `getArgs()` are unaffected

### Tests
- [ ] Unit test: element without a `getArgs()` override returns an empty array
- [ ] Unit test: element with an explicit `getArgs()` override returns its own array
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `Element` has a working default `getArgs()` returning `NO_ARGS`
- Dynamic elements with overrides are unaffected
- All tests pass

---

## What NOT to Do

- Do not remove existing `getArgs()` overrides from enums in this phase — that is Phase 11
- Do not rename `EMPTY_ARGS` here — that is Phase 3; coordinate to avoid duplication

---

*MIT License Copyright (c) 2025-2026 VOID Project*
