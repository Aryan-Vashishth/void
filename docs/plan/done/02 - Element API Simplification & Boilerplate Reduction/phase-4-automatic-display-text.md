# Phase 4 — Automatic Display Text

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — additive default; existing overrides are unaffected

---

## Objective

Derive a human-readable display label from the enum constant name automatically, eliminating repeated `getDisplayText()` overrides for elements with predictable names.

---

## Context

Many elements override `getDisplayText()` with a value that is mechanically derivable from the constant name:

```java
LOGIN_BUTTON  →  "Login Button"
USERNAME_INPUT  →  "Username Input"
```

Implementing this manually:
- duplicates information
- introduces inconsistency when names change
- produces no output that tooling cannot generate

---

## Transformation Rules

```
USERNAME_INPUT  →  Username Input
LOGIN_BUTTON    →  Login Button
SAVE_AS_DRAFT   →  Save As Draft
PASSWORD        →  Password
```

Algorithm:
1. Split the constant name on underscores.
2. Capitalise only the first character of each token (lowercase the rest).
3. Join with a single space.

---

## Change

Add a default implementation to `Element`:

```java
default String getDisplayText() {
    String name = ((Enum<?>) this).name();
    String[] tokens = name.split("_");
    StringBuilder sb = new StringBuilder();
    for (String token : tokens) {
        if (!sb.isEmpty()) sb.append(' ');
        sb.append(Character.toUpperCase(token.charAt(0)));
        sb.append(token.substring(1).toLowerCase());
    }
    return sb.toString();
}
```

Custom labels remain fully supported:

```java
@Override
public String getDisplayText() {
    return "Submit Application";
}
```

---

## Affected Files

- `src/main/java/elements/api/Element.java` — add default `getDisplayText()`

---

## Checklist

### Implementation
- [ ] Add default `getDisplayText()` to `Element` using the documented algorithm
- [ ] Verify the algorithm handles single-token names (e.g. `PASSWORD` → `Password`)
- [ ] Verify the algorithm handles multi-token names (e.g. `SAVE_AS_DRAFT` → `Save As Draft`)
- [ ] Verify elements with explicit `getDisplayText()` overrides are unaffected

### Tests
- [ ] Unit test: single-token name produces correctly capitalised label
- [ ] Unit test: multi-token name produces space-separated capitalised tokens
- [ ] Unit test: element with explicit override returns the override value
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `Element` has a working default `getDisplayText()` derived from the enum constant name
- Existing overrides are unaffected
- All tests pass

---

## What NOT to Do

- Do not remove existing `getDisplayText()` overrides in this phase — that is Phase 11
- Do not change the transformation algorithm mid-implementation — document it first, then implement

---

*MIT License Copyright (c) 2025-2026 VOID Project*
