# Phase 5 — Deterministic Locator Repository Convention

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** High — changes how the runtime locates repositories; must coexist with existing explicit paths

---

## Objective

Introduce a fixed convention for where each page's locator repository lives, so the runtime can discover it automatically without any declaration from the developer.

---

## Open Decision Required

**Open Decision 1** must be resolved before implementation begins:

> Whether the convention root is fixed or configurable per project.
> Whether it applies to the classpath root or a specific source root.

The phase design below assumes the fully qualified type approach (recommended).

---

## Convention

Java source and locator resources follow the standard Maven layout, with the resource path mirroring the page's full package path:

```
src/main/java/tests/demo/pages/DemoLoginPage.java

src/main/resources/tests/demo/pages/
    DemoLoginPage/
        locators.properties
        locators.json
```

The runtime derives the resource path from the page's fully qualified type:

```
tests.demo.pages.DemoLoginPage  →  tests/demo/pages/DemoLoginPage/locators.json
```

Package inclusion prevents collisions: two pages named `LoginPage` in different packages produce distinct paths.

---

## Why No Annotation

An annotation approach (e.g. `@LocatorFile("...")`) still requires a manual declaration. The convention approach requires nothing — the path is deterministic from information already present in the type system.

---

## Affected Files

- `src/main/java/elements/locator/LocatorResolver.java` (or equivalent) — implement convention-based path derivation
- Supporting infrastructure as needed by Phase 13 (`LocatorContext`)

> **Note:** This phase defines the convention. Phase 9 wires it into the resolution order. Phase 13 encapsulates it behind `LocatorContext`.

---

## Checklist

### Design
- [ ] Resolve Open Decision 1 — confirm root path approach
- [ ] Document the exact derivation algorithm (package + class name → resource path)
- [ ] Confirm the resource path is classpath-relative

### Implementation
- [ ] Implement path derivation from `Class.getName()` or equivalent
- [ ] Confirm the derived path loads from the classpath correctly
- [ ] Confirm pages outside the conventional root still work via `getExternalFileName()` override

### Tests
- [ ] Unit test: page type produces the correct conventional resource path
- [ ] Unit test: two pages with the same class name in different packages produce distinct paths
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- Given a page type, the runtime can derive the conventional repository path without any developer declaration
- The derivation is collision-free across packages
- Existing explicit-path pages are unaffected

---

## What NOT to Do

- Do not wire the convention into the resolution order here — that is Phase 9
- Do not encapsulate behind `LocatorContext` here — that is Phase 13
- Do not remove `getExternalFileName()` — that is repositioned in Phase 8

---

*MIT License Copyright (c) 2025-2026 VOID Project*
