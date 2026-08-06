# Phase 11 — Session API Completion

**Status:** Ongoing (blocked on Phase 6, Phase 8 Batch 4)  
**Architecture Version:** 2.3  
**Branch:** `feature/session-api-completion` (new branch)  
**Risk:** Medium — new public API additions; `getEngine()` removal is permanent

---

## Objective

Add missing session-level methods to the `VOID` session API so that every known `app.getEngine()` use case has a first-class alternative. This phase is the prerequisite for Phase 8 Batch 4's `getEngine()` removal: callers must never lose capability when `getEngine()` is removed.

---

## Context

`app.getEngine()` exposes the concrete `UIEngine` type to callers. This violates the engine-agnostic contract and was flagged as SESSION-001 in Phase 6. However, removing it before providing alternatives would break callers who have no other way to access the functionality they need.

The correct migration strategy:
1. **Audit** — find every use of `app.getEngine()` and understand what it is used for.
2. **Cover** — add a session-level method for each distinct use case.
3. **Migrate** — replace each `app.getEngine().doX()` call with `app.doX()`.
4. **Remove** — Phase 8 Batch 4 removes `getEngine()` after zero usages remain.

Phase 11 owns steps 1–3. Phase 8 Batch 4 owns step 4.

---

## Audit Phase

Before adding any new session methods, audit every current usage of `getEngine()`:

```bash
grep -rn "getEngine()" src/ --include="*.java"
```

For each usage, document:
- Which file and line
- What the caller does with the engine reference (e.g., `engine.navigate(url)`, `engine.executeScript(...)`)
- Whether a session-level equivalent already exists
- If not, what the new session-level method signature should be

Deliver the audit as a table in this document or in `docs/architecture/deprecated-symbol-audit.md`.

---

## Coverage Principle

When adding session-level methods, follow these rules:

**Rule 1 — Semantics, not mechanics.**  
Name methods by what they accomplish, not by what the engine does. `app.navigateTo(url)` not `app.get(url)`.

**Rule 2 — Engine-agnostic signatures.**  
Method signatures must contain no Selenium/Playwright types. `By`, `WebElement`, `WebDriver` must not appear in any new public `VOID` method.

**Rule 3 — Only cover confirmed use cases.**  
Do not add speculative session methods. Add exactly the methods needed to replace confirmed `getEngine()` usages.

**Rule 4 — Document portability.**  
Every new method that has Semantic Equivalence: NO (different Selenium vs Playwright semantics) must be added to `docs/architecture/engine-portability-exceptions.md`.

---

## Known Use Cases (Seed — Complete During Audit)

| Use case | Current call | Proposed session method |
|----------|-------------|------------------------|
| Navigate to URL | `app.getEngine().navigate(url)` | `app.navigateTo(url)` (may already exist) |
| Get current URL | `app.getEngine().getCurrentUrl()` | `app.getCurrentUrl()` (may already exist) |
| Execute JavaScript | `app.getEngine().executeScript(script, args)` | `app.executeScript(script, args)` |
| Switch to frame | `app.getEngine().switchToFrame(...)` | `app.switchToFrame(...)` |
| Accept alert | `app.getEngine().acceptAlert()` | `app.acceptAlert()` |
| Get page title | `app.getEngine().getPageTitle()` | `app.getPageTitle()` |

This table is a seed. Complete it during the audit phase by running the grep command above.

---

## Escape Hatch

There will be use cases where the engine capability is genuinely too low-level for a session-level abstraction — or too rarely needed to justify a permanent public API. For these, an explicit escape hatch is acceptable:

```java
/**
 * Returns the underlying native driver for direct engine access.
 *
 * <p><b>Last resort only.</b> Prefer session-level methods. Use this only when
 * the needed operation has no session equivalent and cannot be reasonably added.</p>
 *
 * @see <a href="docs/architecture/engine-portability-exceptions.md">SESSION-001</a>
 */
Object getNativeDriver();
```

`getNativeDriver()` returns `Object` (not `WebDriver`) to preserve compile-time engine independence. Callers cast at their own portability risk.

This is different from `getEngine()`: it exposes the raw driver, not the `UIEngine` abstraction. Callers cannot call VOID's hook infrastructure through `getNativeDriver()` — they get the raw engine object only.

---

## Affected Files

Depends on audit findings. Expected:
- `src/main/java/core/runtime/VOID.java` — new session-level methods
- `src/main/java/core/engine/UIEngine.java` — verify all needed operations are on the interface (not just in Selenium impl)
- `docs/architecture/engine-portability-exceptions.md` — new entries for any NO-equivalence methods
- Demo and test code — replace `getEngine()` call sites

---

## Checklist

### Audit
- [ ] Run `grep -rn "getEngine()" src/` and document every usage.
- [ ] For each usage: identify the operation, check for existing session equivalent.
- [ ] Produce the completed audit table.

### Coverage
- [ ] For each unaddressed use case: design the session-level method signature.
- [ ] Confirm all new method signatures are engine-agnostic (no Selenium/Playwright types).
- [ ] Add each new method to `VOID.java` with Javadoc.
- [ ] For NO-equivalence methods: add an entry to `engine-portability-exceptions.md`.

### Migration
- [ ] Replace every `app.getEngine().doX()` call site with the new `app.doX()` method.
- [ ] Replace demo code.
- [ ] Replace internal framework usages.
- [ ] Run `grep -rn "getEngine()" src/` — confirm zero remaining usages in production code.

### Pre-removal handoff
- [ ] Document audit findings and migration outcome for Phase 8 Batch 4.
- [ ] Confirm Phase 8 Batch 4 preconditions are met.

---

## Tests

- [ ] Each new session-level method has at least one test covering its basic behavior.
- [ ] `app.getEngine()` usages replaced — examples that previously called through `getEngine()` now call session methods and pass.
- [ ] Integration: existing end-to-end flow examples pass without modification.

---

## Exit Criteria

- Audit complete: every `getEngine()` usage is documented with a replacement path.
- Every confirmed use case has a session-level alternative.
- Zero `app.getEngine()` calls remain in production code.
- Phase 8 Batch 4 preconditions are satisfied and documented.

---

## What NOT to Do

- Do not remove `getEngine()` in this phase — that is Phase 8 Batch 4.
- Do not add speculative session methods — only add what confirmed usages require.
- Do not use Selenium-specific types in new method signatures.
- Do not skip the audit and add methods you assume are needed.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
