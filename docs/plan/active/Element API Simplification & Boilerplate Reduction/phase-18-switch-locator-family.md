# Phase 18 — SwitchLocatorFamily

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Low — additive alternative to Phase 17; leverages Java language features, no custom tooling

---

## Objective

Introduce `SwitchLocatorFamily` for projects that prefer a centralised, compiler-validated semantic mapping over per-constant constructors.

---

## Dependencies

- Phase 16 (`LocatorFamily`) must be implemented first
- Phase 17 (`AdvancedLocatorFamily`) should be implemented first (same conceptual layer)

---

## Context

`AdvancedLocatorFamily` uses constructors to carry explicit semantic values. Some teams prefer all mappings to be visible in one place — a centralised switch — so that no constant is silently missed when values are added or changed.

`SwitchLocatorFamily` provides this via a required `getSemanticValue()` method. The Java compiler enforces exhaustiveness. IntelliJ generates and maintains the switch automatically.

---

## Interface

```java
public interface SwitchLocatorFamily extends LocatorFamily {
    String getSemanticValue();
}
```

`getSemanticValue()` is abstract. Any enum implementing `SwitchLocatorFamily` must provide an exhaustive switch.

---

## Workflow

### Step 1 — Declare the enum

```java
public interface VendorPage {

    enum Filters implements Clickable, SwitchLocatorFamily {
        COUNTRY,
        PROGRAM_NAME,
        HQ_STATE_PROVINCE,
        SAVE_AND_CONTINUE,
        CRM
    }
}
```

IDE immediately reports: `Class 'Filters' must implement abstract method 'getSemanticValue()'`

### Step 2 — Generate the switch (Implement Methods quick fix)

```java
@Override
public String getSemanticValue() {
    return switch (this) {
        case COUNTRY -> throw new UnsupportedOperationException();
        case PROGRAM_NAME -> throw new UnsupportedOperationException();
        case HQ_STATE_PROVINCE -> throw new UnsupportedOperationException();
        case SAVE_AND_CONTINUE -> throw new UnsupportedOperationException();
        case CRM -> throw new UnsupportedOperationException();
    };
}
```

### Step 3 — Fill in the mappings

```java
@Override
public String getSemanticValue() {
    return switch (this) {
        case COUNTRY -> "Country";
        case PROGRAM_NAME -> "Program Name";
        case HQ_STATE_PROVINCE -> "HQ State/Province";
        case SAVE_AND_CONTINUE -> "Save & Continue";
        case CRM -> "CRM";
    };
}
```

### Step 4 — Adding a new constant

Adding `VENDOR_TYPE` to the enum causes a compile error: switch is not exhaustive.  
IntelliJ's **Add missing branches** quick fix inserts the new case automatically.

---

## Properties Key

Unchanged from all other family interfaces:

```properties
VendorPage.Filters=
```

---

## When to Use

Prefer `SwitchLocatorFamily` when:

- Most or all constants require custom semantic values.
- A centralised mapping is preferred over scattered constructors.
- Compile-time exhaustiveness for every new constant is desirable.

Prefer `AdvancedLocatorFamily` when:

- Most constants can use automatic derivation and only a few need custom values.

---

## Affected Files

- `src/main/java/elements/api/SwitchLocatorFamily.java` (new interface extending `LocatorFamily`)
- `src/main/java/elements/locator/LocatorResolver.java` — call `getSemanticValue()` for runtime arg

---

## Checklist

### Interface
- [ ] Create `SwitchLocatorFamily` extending `LocatorFamily` with abstract `getSemanticValue()`
- [ ] Confirm the abstract method triggers IDE quick fix correctly

### Resolver
- [ ] Detect `SwitchLocatorFamily` at resolution time
- [ ] Call `getSemanticValue()` for the runtime arg
- [ ] Confirm the key lookup remains `PageName.EnumName`

### Tests
- [ ] Unit test: `getSemanticValue()` return value is used as the runtime arg
- [ ] Unit test: adding a constant without updating the switch causes a compile error
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `SwitchLocatorFamily` interface exists with abstract `getSemanticValue()`
- Runtime arg comes from `getSemanticValue()` return value
- Properties key format is unchanged
- All tests pass

---

## What NOT to Do

- Do not provide a default implementation of `getSemanticValue()` — exhaustiveness requires it to be abstract
- Do not use `SwitchLocatorFamily` as the default recommendation — it is an advanced option for teams that need centralised mapping

---

*MIT License Copyright (c) 2025-2026 VOID Project*
