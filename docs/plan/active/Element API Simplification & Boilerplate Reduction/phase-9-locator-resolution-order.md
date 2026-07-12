# Phase 9 — Locator Resolution Order

**Status:** Complete — all three steps already implemented; no resolver changes needed  
**Branch:** `feature/element-api-simplification`  
**Risk:** High — changes the core runtime resolution path; must handle all three strategies correctly

---

## Objective

Wire the three-step locator resolution order into the runtime so it correctly falls through from element override to convention to hardcoded fallback.

## Implementation Note (as built)

All three steps are satisfied by the existing infrastructure — no changes to LocatorResolver were needed:

- **Step 1 (override):** LocatorResolver already passes `getExternalFileName()` to the source pipeline. Any non-null return (including explicit overrides like `"shared/common.json"`) routes to the matching `LocatorSource`.
- **Step 2 (convention):** Embedded in the Phase 8 default — `getExternalFileName()` probes the classpath and derives the conventional file name automatically. There is no separate "convention path" in the resolver; the default makes Step 1 = Step 2 for normal elements.
- **Step 3 (hardcoded):** `HardcodedLocatorSource.supports(null)` returns `true` and `readRaw` returns `request.key()` verbatim. Elements that override `getExternalFileName()` to return `null` (e.g. LocatorFamily) fall through to this path automatically.

---

## Dependencies

- Phase 5 (convention) must be implemented
- Phase 8 (`getExternalFileName()` default) must be implemented

---

## Resolution Order

```
Step 1 — Element override
         getExternalFileName() returns non-null
         → use the declared path directly

Step 2 — Deterministic convention
         derive path from page type via LocatorContext (Phase 13)
         → use the resolved repository

Step 3 — Hardcoded fallback
         no external source found
         → treat the locator returned by the element as final XPath / CSS
```

---

## Resolution Flow

```text
Element
      │
      ▼
getExternalFileName() != null?
      │
 ┌────┴────┐
 │         │
Yes        No
 │         │
 ▼         ▼
Use file   Derive path from page type
           (convention — Phase 5)
               │
        ┌──────┴──────┐
        │             │
     Found          Missing
        │             │
        ▼             ▼
External lookup   Treat locator
                  as hardcoded
```

---

## Affected Files

- `src/main/java/elements/locator/LocatorResolver.java` (or equivalent) — implement the three-step fallthrough
- Any resolver infrastructure touched by the convention path

> **Note:** Phase 13 (`LocatorContext`) will encapsulate Step 2 behind an abstraction. For now, the convention path may be inline — Phase 13 extracts it.

---

## Checklist

### Implementation
- [ ] Step 1: if `getExternalFileName()` is non-null, load that file; short-circuit
- [ ] Step 2: derive the conventional path; if a repository exists there, use it
- [ ] Step 3: if no external repository found, return the locator as a hardcoded value

### Tests
- [ ] Unit test: element with explicit `getExternalFileName()` → uses that path (Step 1)
- [ ] Unit test: element without override, repository at conventional path → uses convention (Step 2)
- [ ] Unit test: element without override, no repository at conventional path → treats locator as hardcoded (Step 3)
- [ ] Unit test: element that explicitly overrides `getExternalFileName()` to return `null` + has a hardcoded locator → Step 3 applies
- [ ] Regression: all existing demo and test pages resolve correctly
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- All three resolution paths work correctly
- No existing element is silently broken by the new fallthrough logic
- All tests pass

---

## What NOT to Do

- Do not skip Step 1 or reorder the steps — the order is the contract
- Do not remove the hardcoded fallback (Step 3) — it is an intentional feature
- Do not encapsulate the convention behind `LocatorContext` here — that is Phase 13

---

*MIT License Copyright (c) 2025-2026 VOID Project*
