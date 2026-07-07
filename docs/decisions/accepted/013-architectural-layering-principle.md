# ADR-013 — Architectural Layering Principle

**Date:** 2026-07-07  
**Status:** Accepted

---

## Context

During the action ownership refactor (Phases 13–19), a recurring question arose: where does
execution policy belong? Specifically: should capabilities know how to execute safely, or should
actions own that responsibility?

The original design had capability interfaces expose `ActionProfile` constants and `safeProfile()`
methods. A central `Profiles.SAFE` used a `switch(action.capability())` to select behavior at
execution time. This made execution policy a capability concern and required central dispatch.

---

## Statement of Principle

**Capabilities describe. Actions execute.**

| Layer | Responsibility |
|-------|---------------|
| **Element** | Declares what locators it exposes and what capabilities it has |
| **Capability interface** | Describes the structural contract — what operations the element supports |
| **Action** | Owns execution: locator role, engine method, profile defaults, hook wiring |
| **UIEngine** | Owns browser interaction: scroll, waits, retries, fallback |

---

## Derived Rules

These rules follow directly from the principle and must be enforced in code review:

1. **No execution policy in capability interfaces.** Capability interfaces must not contain
   `ActionProfile` constants, `safeProfile()` methods, or hook configurations. They describe
   structure; actions describe execution.

2. **ActionCapability is metadata, not dispatch.** The `ActionCapability` enum identifies an
   action for logging, tracing, and diagnostics. It must never be used in a `switch` to select
   execution paths (e.g., "if CLICKABLE, do X; if TYPEABLE, do Y").

3. **Extension via new types, not via central modification.** Adding a new interaction (e.g.,
   `DoubleClickAction`) means creating a new `ElementAction` subclass with its own
   `defaultSafeProfile()`. It does not mean adding a case to `ActionProfiles.safeProfileFor()`
   unless the new type genuinely needs a new profile shape.

4. **Actions are self-aware.** A concrete action knows its locator role, its capability, and its
   safe/reliable/debug profile defaults. It does not query a registry or central dispatcher to
   discover these.

5. **Profile dispatch lives in one place.** `ActionProfiles` (package-private) owns the mapping
   from capability to profile constants. No other class should contain such a mapping.

---

## Why This Matters

Before this principle was articulated, execution policy leaked in multiple directions:

- `Profiles.SAFE` had a `before(Action action)` and `after(Action action)` that switched on
  `action.capability()` — central dispatch in a profile object
- `Clickable`, `Typeable`, `Selectable` each defined their own `ActionProfile` constants —
  execution policy in structural contracts
- Adding a new hook required changing both the capability interface and the profile switch

After applying the principle:

- `ActionProfiles` owns all profile constants
- Each `ElementAction` subclass calls `ActionProfiles.safeProfileFor(capability)` via
  `defaultSafeProfile()` — one call, no switch in the action
- Capability interfaces contain no profile, hook, or execution logic

---

## Consequences

- Capability interfaces are pure structural contracts — safe to evolve, extend, or implement
  without touching execution infrastructure
- Adding a new action type is local: create the class, declare the profile override if needed
- The execution policy is visible and auditable in one package (`core.actions`)
- `ActionCapability` remains safe to add values to — no switch needs updating elsewhere
