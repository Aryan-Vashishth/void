# Phase 6 — Engine Portability Controls

**Status:** Ongoing  
**Architecture Version:** 2.3  
**Branch:** `main` (documentation only — safe to land directly)  
**Risk:** Low — documentation and source tagging only; no API changes

---

## Objective

Prevent silent Playwright incompatibilities from accumulating in the codebase. Make every engine-specific assumption explicit, documented, reviewable, and tracked before the framework adds a second engine. Surface the complete list of deprecated symbols that Phase 8 will remove.

---

## Context

VOID's architecture is engine-agnostic at the DSL level. The abstraction chain:

```
Element → Action → Flow → FlowExecutor → UIEngine
```

hides Selenium completely from test code.

However, some hooks assume Selenium-specific semantics, and two framework APIs expose the engine directly to callers. These assumptions are currently implicit — they are not visible in code reviews or PR checklists. Undocumented engine assumptions become invisible migration debt.

### Known Engine Assumptions

```java
// HOOK-001: Before.WAIT_FOR_ELEMENT_CLICKABLE
engine.waitForClickable(descriptor, timeout);
// Selenium: ExpectedConditions.elementToBeClickable — checks visibility AND enabled state.
// Playwright: locator.waitFor(VISIBLE) — checks visibility only. Not semantically equivalent.

// HOOK-002: Before.HIGHLIGHT_ELEMENT / After.HIGHLIGHT_ELEMENT
engine.highlight(descriptor, "red");
// Selenium: JavascriptExecutor.executeScript(). Universal across JS-capable browsers.
// Playwright: page.evaluate(). Functionally equivalent; syntax differs.

// HOOK-003: After.WAIT_FOR_ANGULAR_LOADER
// Hardcoded CSS selector: "app-loader". Application-specific — not portable.

// HOOK-004: After.WAIT_FOR_SPIN_SPINNER_LOADER
// Hardcoded XPath: "//span[contains(@class, 'spin spinner')]". Application-specific.

// SESSION-001: VOID.getEngine() / app.getEngine()
// Exposes the concrete UIEngine type to callers. Direct engine access leaks
// the abstraction boundary. Tracked for deprecation and removal in Phase 8.
```

### Deprecated Symbol Inventory (Pre-Phase-8 Preparation)

Phase 8 requires a complete audit of deprecated symbols before removal begins. This audit is performed as part of Phase 6 so Phase 8 can begin without a discovery phase.

---

## Engine Portability Exception Format

Create and maintain: `docs/architecture/engine-portability-exceptions.md`

Each entry uses the following format:

```markdown
## [ID] HookOrAPIName

**Type:** Hook | Session API | Engine Method  
**Selenium behavior:** [specific behavior, class or method name]  
**Playwright equivalent:** [equivalent approach]  
**Semantic Equivalence:** YES | NO
- YES: Both engines produce equivalent outcomes; only syntax differs.
- NO: Semantics differ; a per-engine implementation is required.
**Risk:** Low | Medium | High  
**Mitigation:** [what must happen when adding a second engine]  
**Status:** Open | Resolved  
```

---

## Known Items to Document

| ID | Area | Item | Semantic Equivalence | Risk |
|----|------|------|---------------------|------|
| HOOK-001 | Before hooks | `WAIT_FOR_ELEMENT_CLICKABLE` — visibility vs. enabled state | **NO** | High |
| HOOK-002 | Before/After hooks | `HIGHLIGHT_ELEMENT` — JS execution syntax | YES | Low |
| HOOK-003 | After hooks | `WAIT_FOR_ANGULAR_LOADER` — hardcoded CSS selector `app-loader` | **NO** | Medium |
| HOOK-004 | After hooks | `WAIT_FOR_SPIN_SPINNER_LOADER` — hardcoded XPath | **NO** | Medium |
| SESSION-001 | VOID session | `app.getEngine()` exposes concrete engine type | **NO** | High |
| SESSION-002 | VOID session | Engine name appears in log output (Selenium-specific string) | YES | Low |

`Semantic Equivalence: NO` items require per-engine implementations when Playwright support is added. These are the items to prioritize first in any future engine portability project.

---

## Deprecated Symbol Inventory (for Phase 8)

Document every deprecated symbol with its replacement and risk level. This document becomes the input to Phase 8's removal batches.

| Symbol | Package | Since | Replacement | Risk |
|--------|---------|-------|-------------|------|
| `HookedAction` (public constructor) | `core.actions` | 2.0 | `action.before(...).after(...)` or profile shorthands | Medium |
| `HookedAction.wrap(...)` (static method) | `core.actions` | 2.0 | `action.before(...).after(...)` | Medium |
| `Action.withHooks(List, List)` | `core.actions` | 2.0 | Decided in Phase 8 — see Batch 4 decision | High |
| `VOID.getEngine()` / `app.getEngine()` | `core.runtime` | (to be deprecated) | Engine-level abstractions; `UIEngine` methods via VOID session | High |
| `UIContext.setLastLocatorDescriptor(...)` | `core.utils` | unknown | Audit required — see below | Low-Medium |
| `UIContext.getLastElement()` | `core.utils` | unknown | Audit required — see below | Low-Medium |
| `Interactions.isAnyDisplayed(By)` | `core.interactions` | unknown | `searchAndGetResults()` | Low |

### UIContext Audit

`setLastLocatorDescriptor` and `getLastElement` have unknown replacement paths. Before Phase 8 Batch 2 can proceed, this audit must be complete:

1. Find all usages: `grep -r "setLastLocatorDescriptor\|getLastElement" src/`.
2. For each usage, determine whether the caller can be rewritten using `ActionTrace`, `LocatorDescriptor`, or engine-level methods instead.
3. Document the replacement path, or decide to make the methods package-private (not removed) if they are internal framework state.

Deliver the audit result as a section in `docs/architecture/engine-portability-exceptions.md` or as a separate `docs/architecture/deprecated-symbol-audit.md`.

---

## Affected Files

New:
- `docs/architecture/engine-portability-exceptions.md` — full exception registry with all seed items

Modified:
- `src/main/java/core/interactions/hooks/Before.java` — add `@EngineSpecific` Javadoc note to HOOK-001, HOOK-003, HOOK-004
- `src/main/java/core/interactions/hooks/After.java` — add `@EngineSpecific` Javadoc note to HOOK-003, HOOK-004
- `CONTRIBUTING.md` — add review gate for engine-specific code

---

## Migration Strategy

This phase is additive. No existing behavior changes. The exception registry and deprecated symbol inventory are new documentation artifacts. Source tagging adds Javadoc notes only — no logic changes.

---

## Checklist

### Engine Portability Registry
- [ ] Create `docs/architecture/engine-portability-exceptions.md` with the exception format.
- [ ] Document HOOK-001 (`WAIT_FOR_ELEMENT_CLICKABLE`) with Semantic Equivalence: NO.
- [ ] Document HOOK-002 (`HIGHLIGHT_ELEMENT`) with Semantic Equivalence: YES.
- [ ] Document HOOK-003 (`WAIT_FOR_ANGULAR_LOADER`) with Semantic Equivalence: NO.
- [ ] Document HOOK-004 (`WAIT_FOR_SPIN_SPINNER_LOADER`) with Semantic Equivalence: NO.
- [ ] Document SESSION-001 (`app.getEngine()`) with Semantic Equivalence: NO, Risk: High.
- [ ] Document SESSION-002 (engine name in log output) with Semantic Equivalence: YES, Risk: Low.

### Source Tagging
- [ ] Add Javadoc portability note to `Before.WAIT_FOR_ELEMENT_CLICKABLE` (HOOK-001): "Semantic equivalence: NO — Playwright does not check enabled state."
- [ ] Add Javadoc portability note to `Before.WAIT_FOR_ANGULAR_LOADER` (HOOK-003): "Engine-specific: CSS selector `app-loader` is application-coupled. Playwright support requires a configurable selector."
- [ ] Add Javadoc portability note to `After.WAIT_FOR_SPIN_SPINNER_LOADER` (HOOK-004): same pattern.
- [ ] Add Javadoc note to `VOID.getEngine()`: "Engine access escape hatch. Deprecated for removal in a future release (see SESSION-001). Prefer engine-level abstractions on the VOID session."

### Deprecated Symbol Inventory
- [ ] Produce the deprecated symbol inventory table (above or as a standalone doc).
- [ ] Perform UIContext audit: grep usages, identify replacement paths, document findings.
- [ ] Deliver UIContext audit as part of `docs/architecture/deprecated-symbol-audit.md` or as a section in the portability exceptions doc.

### Review Gate
- [ ] Add to `CONTRIBUTING.md`: "Any new engine-specific code must add an entry to `docs/architecture/engine-portability-exceptions.md` before the PR is merged."

---

## Tests

No new test code in this phase. All deliverables are documentation and Javadoc.

- [ ] Verify `docs/architecture/engine-portability-exceptions.md` exists and is parseable.
- [ ] Verify all six seed items are present with all required fields filled in.
- [ ] Verify Javadoc notes appear in rendered IDE Javadoc for each tagged hook.

---

## Exit Criteria

- `docs/architecture/engine-portability-exceptions.md` exists and contains all six seed items with `Semantic Equivalence` classification.
- All Semantic Equivalence: NO hooks are tagged in source Javadoc.
- `app.getEngine()` Javadoc marks it for future removal with reference to SESSION-001.
- `CONTRIBUTING.md` contains the engine-portability review gate.
- Deprecated symbol inventory is complete, including UIContext audit findings.
- Phase 8 can begin without a discovery phase.

---

## What NOT to Do

- Do not add a Playwright engine in this phase.
- Do not abstract `UIEngine` further — the current interface is the portability boundary.
- Do not remove `app.getEngine()` yet — flag it here, remove it in Phase 8 Batch 5.
- Do not begin Phase 8 removal work here — Phase 6 is discovery and documentation only.
- Do not create a dynamic annotation or runtime tag for engine-specific code — Javadoc notes and the exception registry are sufficient.

---

*MIT License Copyright (c) 2025-2026 VOID Project*
