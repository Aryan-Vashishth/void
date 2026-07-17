# Developer Experience Audit — Action Hook Layer

**Date:** 2026-06-13  
**Scope:** `core.actions`, `core.interactions.hooks`, VOID public API surface  
**Status:** Fulfilled — all 7 phases implemented in v0.3.0 (feature/element-api-simplification)

---

## Summary

The hook engine is architecturally sound. `before(...)` / `after(...)` directional APIs are correct low-level plumbing.

The gap identified is the **developer experience layer** sitting on top of the engine.

---

## Findings

### ✅ What Is Working Well

| Area | Finding |
|---|---|
| Hook execution | `before → action → after` ordering is reliable and well-tested |
| Type safety | `BeforeActionHandler` / `AfterActionHandler` marker types prevent direction mistakes at compile time |
| Descriptor lifecycle | Descriptor resolved once, shared across all hooks — correct |
| Hook composition | `HookChainAction` merges hook lists cleanly across `.before(...).after(...)` chains |
| Deprecation | `withHooks(...)` and `HookedAction` correctly deprecated; `@Internal` applied |
| Constants | `Before.*` / `After.*` libraries are comprehensive and engine-agnostic |

---

### ⚠️ Gap: Developer Experience

| Gap | Impact |
|---|---|
| Test writers must know `Before`, `After`, `ActionHandler` | Cognitive overhead for common patterns |
| No built-in shorthand for standard safety patterns | Every team re-invents hook combinations |
| Hook ordering awareness required | Framework internals leak into daily usage |
| Docs currently show hooks as primary API | Raises barrier to entry |
| No concept of capability-aware defaults | A click and a type need different safety strategies — no abstraction for this |
| No app-level or config-driven defaults | Every action requires explicit hook composition |

---

### ✅ Gap NOT Present

| Item | Status |
|---|---|
| Hook engine correctness | Not a concern — no redesign needed |
| Descriptor resolution | Correct and deferred |
| Type safety of `before` vs `after` | Enforced by compiler |
| Backward compatibility | `withHooks` and `Interactions` both preserved with deprecation path |

---

## Recommended Action

See: [`docs/plan/ongoing/action-profiles-roadmap.md`](../plan/ongoing/action-profiles-roadmap.md)

### Summary of recommended changes:

| Phase | Change | Priority |
|---|---|---|
| 1 | Add `safely()`, `debug()`, `raw()` to `Action` | Very High |
| 2 | Introduce `ActionProfile` interface + `using(profile)` on `Action` | High |
| 3 | Capability-aware profile resolution per `Clickable`, `Typeable`, etc. | Very High |
| 4 | `Profiles` preset library (`SAFE`, `DEBUG`, `FAST`, `VISUAL`, `RELIABLE`) | High |
| 5 | Custom profile builder (`Profile.builder()...build()`) | Very High |
| 6 | Global default profiles via `void.profile.default` config key | Extremely High |
| 7 | Docs shift — profiles as primary API, hooks as advanced extension | High |

---

## Stability Impact

| Layer | Change Required | Breaking? |
|---|---|---|
| `Action` interface | New default methods (`safely`, `debug`, `raw`, `using`) | No |
| `HookChainAction` | No change | No |
| `Before` / `After` | No change — used internally by profiles | No |
| `BeforeActionHandler` / `AfterActionHandler` | No change | No |
| Public docs | Update primary examples to profile API | No |

All changes are additive. Existing `before(...)` / `after(...)` API remains intact.

---

## Conclusion

VOID is transitioning from a **Selenium wrapper** to a **UI execution platform**.

Hooks are the plumbing.  
Profiles would be the language people actually speak.

That transition is the marker of a mature framework.

---

*MIT License © 2025–2026 VOID Project*

