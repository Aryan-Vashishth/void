# Phase 4 — Remove Selenium Leakage from Public Surface

> **Status:** Prospect.
>
> **Phase number:** 4 (audit numbering, stable).  
> **Execution order:** 6 of 10 — runs after Phase 3 (runtime wiring).  
> **Precedes:** Phase 5 (exec order 7).  
> **Succeeds:** Phase 3 (exec order 5).
>
> **Goal:** Remove Selenium types (`By`, `WebElement`, `WebDriver`) from public
> framework APIs (`VoidDSL`, `Via`, runtime escape hatches).
>
> **Proposed:** 2026-05-05  
> **Last updated:** 2026-05-05

---

## Full Execution Sequence (for reference)

> Phase numbers are stable audit identifiers. Execution order reflects
> dependency-driven sequence — Phase 2 runs before Phase 1 by design
> (see active architecture doc §8).

| Exec | Phase # | Title | Depends On |
|:---:|:---:|---|---|
| 1 | 0 | Lock Architecture Rules | — |
| 2 | 2 | Resolution Unification | Phase 0 |
| 3 | 1 | Fix Bootstrap & Startup Ownership | Phase 2 |
| 4 | 1.5 | Lock Execution Model in Code | Phase 1 |
| 5 | 3 | Wire `FlowExecutor` into Runtime | Phase 1, 2 |
| **6** | **4** | **Remove Selenium Leakage from Public Surface ← you are here** | Phase 3 |
| 7 | 5 | Convert `Interactions` to Strict Adapter | Phase 3, 4 |
| 8 | 6 | Remove `UIContext` | Phase 5 |
| 9 | 7 | Finalize Hook Model | Phase 5, 6 |
| 10 | 8 | Add Playwright Engine | Phases 1–7 |

---

## 1. In scope

- Remove Selenium imports from public `VoidDSL` signatures and implementation paths.
- Retire public usage of `Via.locator(...)` and `Via.webElement(...)`.
- Remove runtime `getDriver()` escape hatch from `VOID` surface.
- Replace DSL visibility checks with descriptor-based engine calls.

## 2. Out of scope

- Interactions adapter rewrite (Phase 5).
- Playwright implementation (Phase 8).

---

## 3. Code touchpoints

- `src/main/java/dsl/VoidDSL.java`
- `src/main/java/core/interactions/Via.java`
- `src/main/java/core/runtime/VOID.java`
- `src/main/java/core/resolvers/locator/api/LocatorResolver.java` (public API review)

---

## 4. Implementation sequence

1. Refactor DSL internals from `By` to `LocatorDescriptor`.
2. Replace Selenium-returning helper usage in DSL/runtime.
3. Deprecate/remove public Selenium-based helpers in `Via`.
4. Remove `VOID.getDriver()` from runtime surface (or make non-public transitional bridge).
5. Update docs and examples to descriptor-only public APIs.

---

## 5. Risks and mitigations

- Step-definition compatibility break.
  - Mitigation: short-lived deprecated adapters with explicit phase tags.
- Hidden Selenium dependencies in tests/utilities.
  - Mitigation: architecture tests for public signature purity.
- Mixed API state.
  - Mitigation: phase checklist requires signature audit before merge.

---

## 6. Test strategy

- API compile checks for DSL public surface (no Selenium types).
- Regression tests for existing DSL behavior.
- Architecture rule for R5 on public DSL contracts.

---

## 7. Exit criteria

- Public `VoidDSL` signatures contain no Selenium types.
- `Via` public recommended path is descriptor-only.
- `VOID` public/runtime surface has no `WebDriver` escape hatch.
- Docs and quick-start samples are engine-agnostic on public API paths.

