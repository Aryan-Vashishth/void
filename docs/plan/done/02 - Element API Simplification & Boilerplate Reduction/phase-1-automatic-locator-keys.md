# Phase 1 — Automatic Locator Keys

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — additive default; no existing behavior removed

---

## Objective

Eliminate duplicate locator key strings by deriving the locator key directly from the enum constant name.

---

## Context

Every element enum currently passes its own name as a string argument to its constructor:

```java
USERNAME_INPUT("USERNAME_INPUT"),
PASSWORD_INPUT("PASSWORD_INPUT");
```

The enum constant already uniquely identifies the key. The string is pure duplication. Renaming a constant without updating the string produces a silent runtime mismatch.

---

## Change

Add a default implementation to `Element` that derives the key from the fully qualified element path:

```java
default String getPrimaryLocator() {
    Enum<?> e = (Enum<?>) this;
    Class<?> enumClass = e.getDeclaringClass();
    Class<?> pageClass = enumClass.getEnclosingClass();
    if (pageClass != null) {
        return pageClass.getSimpleName() + "." + enumClass.getSimpleName() + "." + e.name();
    }
    return enumClass.getSimpleName() + "." + e.name();
}
```

The namespace `PageName.GroupName` is derived from the Java type hierarchy — no configuration required.

Examples:

```
DemoLoginPage.Credentials.USERNAME_INPUT
DemoLoginPage.Credentials.PASSWORD_INPUT
DemoLoginPage.Button.LOGIN_BUTTON
```

Capability interfaces (`Typeable`, `Clickable`, `Selectable`, etc.) that currently override `getPrimaryLocator()` by forwarding to this default can have those overrides removed in Phase 12.

---

## After

```java
enum Credentials implements Typeable {
    USERNAME_INPUT,
    PASSWORD_INPUT
}
```

No constructor. No duplicate string. IDE rename of `USERNAME_INPUT` updates the lookup key automatically. The key in the properties file (`DemoLoginPage.Credentials.USERNAME_INPUT`) updates with it.

---

## Affected Files

- `src/main/java/elements/api/Element.java` — add default `getPrimaryLocator()`

---

## Checklist

### Implementation
- [ ] Add `default String getPrimaryLocator()` returning `((Enum<?>) this).name()` to `Element`
- [ ] Confirm the cast is safe — `Element` is only implemented by enums in this project
- [ ] Verify existing elements that override `getPrimaryLocator()` are unaffected (override takes precedence)

### Tests
- [ ] Unit test: nested enum constant returns namespaced key (`PageName.GroupName.CONSTANT`)
- [ ] Unit test: top-level enum constant (no enclosing class) returns `EnumName.CONSTANT`
- [ ] Unit test: enum constant with an explicit override still returns the overridden value
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `Element` has a working default `getPrimaryLocator()` derived from `name()`
- Existing elements with explicit overrides are unaffected
- All tests pass

---

## What NOT to Do

- Do not remove any existing constructors or overrides in this phase — that is Phase 11
- Do not change capability interface signatures in this phase — that is Phase 12
- Do not add the `NO_ARGS` constant here — that is Phase 2 and 3

---

*MIT License Copyright (c) 2025-2026 VOID Project*
