# Phase 14 — Cache the LocatorContext Resolution

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — concurrency-sensitive; must not introduce stale caches or race conditions

---

## Objective

Cache the resolved `LocatorRepository` per page type so that repository resolution happens at most once per page during a test session.

---

## Dependencies

- Phase 13 (`LocatorContext`) must be implemented

---

## Context

Without a cache, every element lookup triggers:
1. `getExternalFileName()` check
2. Conventional path derivation
3. Classpath resource load
4. `LocatorRepository` construction

On pages with 60–100 elements, this repeated work adds up. The data never changes within a test run — the repository for a given page type is identical for every lookup.

---

## Cache Design

Cache the resolved `LocatorRepository`, not the path string:

```java
ConcurrentHashMap<Class<?>, LocatorRepository>
```

Keyed by the page's `Class<?>` (the enclosing class of the element enum).

**Why cache the repository, not the path:**
- All subsequent lookups are constant-time regardless of format
- The cache remains correct if the underlying source changes format
- Future repository types (YAML, remote) require no change to the cache layer

---

## Placement

The cache lives inside `LocatorContext` or its default implementation — not in the resolver. This keeps the cache co-located with the resolution logic it accelerates.

---

## Cache Invalidation

Within a test session, the cache is never invalidated. Repository contents do not change mid-session. If live-reload behavior is needed in the future, it belongs in a separate `ReloadableLocatorContext` implementation, not in `DefaultLocatorContext`.

---

## Affected Files

- `src/main/java/elements/locator/DefaultLocatorContext.java` — add `ConcurrentHashMap` cache
- No changes to `LocatorResolver` or `LocatorContext` interface

---

## Checklist

### Implementation
- [ ] Add `ConcurrentHashMap<Class<?>, LocatorRepository>` to `DefaultLocatorContext`
- [ ] On first lookup for a page type — resolve and cache
- [ ] On subsequent lookups — return cached value directly
- [ ] Confirm `computeIfAbsent` or equivalent is used to avoid duplicate resolution under concurrent access

### Tests
- [ ] Unit test: first lookup for a page triggers resolution
- [ ] Unit test: second lookup for the same page returns cached value without re-resolving
- [ ] Unit test: two different page types have independent cache entries
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- Repository resolution occurs at most once per page type per session
- No race condition under concurrent element lookups from the same page
- Cache is keyed by page type, not by element or path string
- All tests pass

---

## What NOT to Do

- Do not cache the path string — cache the resolved `LocatorRepository`
- Do not implement cache invalidation in this phase — it is not needed for the current scope
- Do not put the cache in `LocatorResolver` — it belongs in `LocatorContext` alongside the resolution logic

---

*MIT License Copyright (c) 2025-2026 VOID Project*
