# Phase 6 — Engine Portability Controls

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — documentation and tagging only, no API changes

---

## Objective

Prevent silent Playwright incompatibilities from accumulating. Make every engine-specific assumption explicit, documented, and reviewable before the framework adds a second engine.

---

## Context

VOID's architecture is surprisingly portable. Most engine leakage has been removed. The core abstraction:

```text
Element → Action → Flow → FlowExecutor → UIEngine
```

hides Selenium completely from test code.

However, some hooks still assume Selenium-style semantics:

```java
// Before.WAIT_FOR_ELEMENT_CLICKABLE
engine.waitForClickable(descriptor, DEFAULT_TIMEOUT);
// ↑ Selenium: ExpectedConditions.elementToBeClickable
// ↑ Playwright: locator.waitFor(new WaitForOptions().setState(WaitForSelectorState.VISIBLE))
// These are NOT the same concept.

// Before.HIGHLIGHT_ELEMENT
engine.highlight(descriptor, "red");
// ↑ Selenium: JavascriptExecutor — works universally
// ↑ Playwright: page.evaluate() — works, but syntax differs

// app.getEngine()
// ↑ Still exposed on the VOID session — direct engine access is an engine leak
```

None of these block today. But undocumented engine assumptions make Playwright support harder later.

---

## Engine Portability Exception Format

Create and maintain: `docs/architecture/engine-portability-exceptions.md`

Format per entry:

```markdown
## [HOOK-001] Before.WAIT_FOR_ELEMENT_CLICKABLE

**Type:** Hook  
**Selenium behavior:** Uses ExpectedConditions.elementToBeClickable — checks visibility + enabled state.  
**Playwright equivalent:** locator.waitFor(WaitForSelectorState.VISIBLE) — no enabled-state check.  
**Risk:** Medium — semantics differ; clickable in Selenium ≠ clickable in Playwright.  
**Mitigation:** When adding Playwright engine, implement UIEngine.waitForClickable() with full semantics.  
**Status:** Open  
```

---

## Known Items to Document (Seed List)

| ID | Area | Item | Risk |
|---|---|---|---|
| HOOK-001 | Before hooks | `WAIT_FOR_ELEMENT_CLICKABLE` semantics | Medium |
| HOOK-002 | Before hooks | `HIGHLIGHT_ELEMENT` — JS execution | Low |
| HOOK-003 | After hooks | `WAIT_FOR_ANGULAR_LOADER` — CSS selector `app-loader` | Medium |
| HOOK-004 | After hooks | `WAIT_FOR_SPIN_SPINNER_LOADER` — XPath selector | Medium |
| SESSION-001 | VOID session | `app.getEngine()` exposes engine type | High |
| SESSION-002 | VOID session | engine name in log output is Selenium-specific | Low |

---

## Affected Files

New:
- `docs/architecture/engine-portability-exceptions.md`

Modified:
- `src/main/java/core/interactions/hooks/Before.java` — add Javadoc portability notes per hook
- `src/main/java/core/interactions/hooks/After.java` — add Javadoc portability notes per hook

---

## Checklist

### Documentation
- [ ] Create `docs/architecture/engine-portability-exceptions.md` with exception format.
- [ ] Document all seed list items from the table above.
- [ ] Add a note in `CONTRIBUTING.md`: any new engine-specific code must be added to this doc.

### Source Tagging
- [ ] Add `@EngineSpecific` note (Javadoc tag or comment) to `Before.WAIT_FOR_ELEMENT_CLICKABLE`.
- [ ] Add `@EngineSpecific` note to `Before.WAIT_FOR_ANGULAR_LOADER`.
- [ ] Add `@EngineSpecific` note to `After.WAIT_FOR_ANGULAR_LOADER`.
- [ ] Add `@EngineSpecific` note to `After.WAIT_FOR_SPIN_SPINNER_LOADER`.
- [ ] Add portability note to `app.getEngine()` Javadoc.

### Review Gate
- [ ] Add PR checklist item: does this change assume Selenium-specific behavior? If yes, document it.

---

## Exit Criteria

- `docs/architecture/engine-portability-exceptions.md` exists and contains all seed items.
- All currently known engine-specific hooks are tagged in source.
- New contributions have a documented gate for engine assumptions.

---

## What NOT to Do

- Do not add a Playwright engine in this phase.
- Do not abstract `UIEngine` further than it currently is.
- Do not remove `app.getEngine()` yet — flag it first, remove in a later phase.

---

*MIT License Copyright (c) 2025-2026 VOID Project*

