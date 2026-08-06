# Phase 14 — Cache the LocatorContext Resolution

**Status:** Complete  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — concurrency-sensitive; must not introduce stale caches or race conditions

---

## Objective

Cache the resolved file name per element class so that the classpath probes in
`getExternalFileName()` run at most once per element class per session.

---

## Dependencies

- Phase 13 (`LocatorContext`) must be implemented

---

## Context

Without a cache, every element lookup triggers:
1. `getExternalFileName()` check
2. Conventional path derivation
3. Classpath resource load (4 `ClassLoader.getResource()` probes per call)
4. JSON re-parsing on every key lookup (`ObjectMapper.readTree()`)

On pages with 60–100 elements this repeated work adds up. The data never changes
within a test run — the file name for a given element class is identical for every lookup.

---

## Implementation Notes

`LocatorRepository` was deferred (Open Decision 4) — no such class exists yet.
Phase 14 instead applies two targeted caches:

### Cache 1 — file-name resolution in `DefaultLocatorContext`

```java
private static final ConcurrentHashMap<Class<?>, Optional<String>> FILE_NAME_CACHE =
        new ConcurrentHashMap<>();

@Override
public String resolveFileName(Element element) {
    return FILE_NAME_CACHE
            .computeIfAbsent(element.getClass(),
                             k -> Optional.ofNullable(element.getExternalFileName()))
            .orElse(null);
}
```

**Key: `element.getClass()`** — not the enclosing page class. Using the page class caused
cache poisoning on pages with mixed strategies: a file-backed element cached its file name
under the page class, then a hardcoded sibling element (whose `getExternalFileName()` returns
null) inherited that cached value and resolved to the wrong source.

`element.getClass()` is safe because enum constants without a body all share their enum class
(one entry covers all constants in the enum), while constants that override `getExternalFileName()`
per-body each get their own anonymous class — and thus their own entry.

### Cache 2 — parsed JSON nodes in `JsonLocatorReader`

```java
private static final ConcurrentHashMap<String, Optional<JsonNode>> NODE_CACHE =
        new ConcurrentHashMap<>();

private static JsonNode load(String cp) {
    return NODE_CACHE.computeIfAbsent(cp, key -> {
        try (InputStream in = ...) {
            if (in == null) return Optional.empty();
            return Optional.of(MAPPER.readTree(in));
        } catch (Exception e) {
            return Optional.empty();
        }
    }).orElse(null);
}
```

Eliminates `ObjectMapper.readTree()` on every key lookup. Makes the `JsonLocatorSource`
comment ("JSON reader holds its own cache") accurate.

---

## Placement

Both caches are static fields in their respective classes — not in `LocatorResolver`. No
changes to the `LocatorContext` interface or `LocatorResolver`.

---

## Cache Invalidation

Within a test session, the caches are never invalidated. If live-reload is needed in the
future it belongs in a separate `ReloadableLocatorContext`, not in `DefaultLocatorContext`.

---

## Affected Files

- `src/main/java/core/resolvers/locator/api/DefaultLocatorContext.java`
- `src/main/java/core/resolvers/locator/json/JsonLocatorReader.java`
- `src/test/java/core/resolvers/locator/api/LocatorContextTest.java` — 3 new caching examples

---

## Checklist

### Implementation
- [x] Add `ConcurrentHashMap<Class<?>, Optional<String>>` to `DefaultLocatorContext`
- [x] On first lookup for an element class — resolve and cache
- [x] On subsequent lookups — return cached value directly
- [x] `computeIfAbsent` used to avoid duplicate resolution under concurrent access
- [x] Add `ConcurrentHashMap<String, Optional<JsonNode>>` to `JsonLocatorReader`

### Tests
- [x] Unit test: first lookup for an element class triggers resolution
- [x] Unit test: second lookup for the same class returns cached value without re-resolving
- [x] Unit test: two different element classes have independent cache entries
- [x] Regression: `mvn test` passes — 1065 examples, 0 failures

---

## Exit Criteria — Met

- File-name resolution occurs at most once per element class per session
- No race condition under concurrent element lookups (ConcurrentHashMap + computeIfAbsent)
- Cache is keyed by element class, not by path string
- All 1065 examples pass

---

*MIT License Copyright (c) 2025-2026 VOID Project*
