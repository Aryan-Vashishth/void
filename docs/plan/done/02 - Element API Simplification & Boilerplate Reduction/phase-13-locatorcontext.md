# Phase 13 — LocatorContext

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** High — new abstraction that decouples the resolver from the convention; must integrate cleanly with existing resolver infrastructure

---

## Objective

Introduce `LocatorContext` as the abstraction that encapsulates how the resolver locates a repository for a given element, decoupling the resolver from the specific convention or format.

---

## Open Decisions — Resolved

**Decision 3 (resolved):** `LocatorContext` is a single-method interface `resolveFileName(Element) → String` in `core.resolvers.locator.api`. Returns the classpath-relative file name (the existing seam between path resolution and `LocatorSourceRegistry.select()`). Injectable via `LocatorResolver.Builder`; defaults to `DefaultLocatorContext.INSTANCE`. Not a registry.

**Decision 4 (resolved):** `LocatorRepository` is deferred to Phase 14. Phase 13 returns a `String` file name — consistent with `LocatorRequest.fileName()` and `LocatorSourceRegistry`. Phase 14 introduces `LocatorRepository` as a pre-loaded, cached object when caching demands it.

---

## Responsibility

`LocatorContext` performs:

```
Resolve page from element
      │
      ▼
Derive repository path from page type (convention — Phase 5)
      │
      ▼
Load LocatorRepository
```

It also handles the `getExternalFileName()` override path — the resolver delegates to `LocatorContext` unconditionally rather than branching on null-checks inline.

---

## Why This Abstraction

Without `LocatorContext`, the convention logic lives directly in the resolver. If the convention or repository format evolves, the resolver must change.

With `LocatorContext`:
- The resolver is decoupled from the convention
- Future conventions or repository formats slot in at the `LocatorContext` level
- The cache (Phase 14) stores the resolved `LocatorRepository`, not the path — making it format-agnostic

---

## Proposed Interface (Skeleton — to be finalized by Open Decision 3)

```java
public interface LocatorContext {
    LocatorRepository resolve(Element element);
}
```

The default implementation:
1. Checks `element.getExternalFileName()` — non-null → load that path
2. Derives the conventional path from the page type
3. Returns the loaded `LocatorRepository`

---

## Affected Files

- `src/main/java/elements/locator/LocatorContext.java` (new interface)
- `src/main/java/elements/locator/DefaultLocatorContext.java` (new default implementation)
- `src/main/java/elements/locator/LocatorResolver.java` — delegate Step 2 of resolution to `LocatorContext`

---

## Checklist

### Design
- [ ] Resolve Open Decision 3 — confirm method signatures and composition approach
- [ ] Resolve Open Decision 4 — confirm `LocatorRepository` abstraction boundaries
- [ ] Document what the default implementation is permitted to assume about the project layout

### Implementation
- [ ] Create `LocatorContext` interface
- [ ] Create `DefaultLocatorContext` implementing the convention + override path
- [ ] Wire into `LocatorResolver` — resolver delegates Step 2 to `LocatorContext`

### Tests
- [ ] Unit test: `DefaultLocatorContext` — element with `getExternalFileName()` non-null → loads that path
- [ ] Unit test: `DefaultLocatorContext` — element with no override → derives conventional path
- [ ] Unit test: `DefaultLocatorContext` — conventional path missing → returns null / signals Step 3
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- `LocatorContext` interface exists with a documented contract
- `DefaultLocatorContext` implements the convention + override path correctly
- `LocatorResolver` delegates to `LocatorContext` for Step 2
- All examples pass

---

## What NOT to Do

- Do not implement caching inside `LocatorContext` — that is Phase 14
- Do not collapse `getExternalFileName()` handling and convention handling into a single if-else in the resolver — both must flow through `LocatorContext`
- Do not make `LocatorContext` aware of the hardcoded fallback (Step 3) — that remains in the resolver

---

*MIT License Copyright (c) 2025-2026 VOID Project*
