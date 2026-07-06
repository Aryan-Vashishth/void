# ElementRole Necessity Audit

**Date:** 2026-07-06  
**Scope:** `elements/meta/ElementRole` — all usages across the action, engine, and element layers  
**Goal:** Determine whether `ElementRole` is still necessary after the action ownership refactor (Phases 13–17), or whether it can be removed, internalized, or replaced  
**Outcome:** No changes required — architecture is sound

---

## Audit Questions

1. Is ElementRole still required by the public API?
2. Do action subclasses need to be told their role, or can it be inferred?
3. Are composite actions using ElementRole correctly?
4. Is there redundancy or misuse anywhere?

---

## Findings

### 1. ElementRole is a public API contract

ElementRole appears as a required parameter in four independent public interfaces:

| Contract | Signature |
|----------|-----------|
| `UIEngine.resolve()` | `resolve(Element, ElementRole, Object...)` |
| `LocatorResolver.resolve()` | `resolve(Element, ElementRole, Object...)` |
| `LocatorResolver.resolveDescriptor()` | `resolveDescriptor(Element, ElementRole, Object...)` |
| `Element.getAllLocatorRoles()` | Returns `Map<ElementRole, String>` |
| `Via.descriptor()` | `descriptor(Element, ElementRole, Object...)` |

Removing or replacing ElementRole would require binary-breaking changes to all of the above and every Element implementation in the codebase. It is not an internal detail — it is the type-safe locator key exposed across the entire framework surface.

### 2. Action subclasses correctly hardcode their role

All 16 concrete action subclasses declare their role at construction time, not at runtime. The role is a compile-time constant that names which locator key the action targets:

| Action | Role | Locator semantic |
|--------|------|-----------------|
| `ClickAction` | `TRIGGER` | clickable activator |
| `ToggleAction` | `TRIGGER` | same interaction model as click |
| `CheckAction` | `TRIGGER` | same interaction model as click |
| `HoverAction` | `TEXT` | hover target, not a click trigger |
| `TypeAction` | `INPUT` | text entry field |
| `ClearAction` | `INPUT` | same field |
| `AppendTypeAction` | `INPUT` | same field |
| `TypeAndPressAction` | `INPUT` | same field |
| `UploadAction` | `INPUT` | file input field |
| `OpenAction` | `TRIGGER` | dropdown/menu opener |
| `SelectByTextAction` | `LIST` | options panel |
| `SelectByValueAction` | `LIST` | options panel |
| `TypeSearchAction` | `SEARCH_INPUT` | search text field |
| `SubmitSearchAction` | `SEARCH_BUTTON` | search submit control |
| `SelectAction` | `TRIGGER` (primary) | composite — see §3 |
| `SearchAndSelectAction` | `TRIGGER` (primary) | composite — see §3 |

This is correct. Each action's role is a stated invariant, not a dispatch decision — the role documents which locator the action resolves, eliminating ambiguity for callers and future maintainers.

### 3. Composite actions use ElementRole as named keys

`SelectAction` and `SearchAndSelectAction` resolve multiple locators inside `execute()`:

**SelectAction:** resolves `TRIGGER` (open dropdown) then `LIST` (select option)  
**SearchAndSelectAction:** resolves `TRIGGER` (open) → `SEARCH_INPUT` (type term) → `SEARCH_RESULT` (pick result)

Each call to `engine.resolve(element, ElementRole.X)` is a named lookup — equivalent to a typed map key. This is the correct usage pattern. There is no dispatch here; the roles are used to fetch specific locators, not to branch execution paths.

### 4. No redundancy or misuse found

- `getAllLocatorRoles()` on capability interfaces returns only the roles that interface exposes — no excess values
- `Interactions.java` calls `resolveDescriptor(element, ElementRole.X)` directly for 10+ operations — consistent with the contract
- `EnumLocatorScanner` iterates `getAllLocatorRoles()` to build JSON from element enums — ElementRole is the key schema
- No action class uses ElementRole in a `switch` to branch execution logic

---

## Verdict

| Question | Finding |
|----------|---------|
| Still required by public API? | **Yes** — UIEngine, LocatorResolver, Element, Via all require it |
| Can role be inferred instead of stated? | **No** — the element exposes multiple roles; the action must name which one it targets |
| Composite action usage correct? | **Yes** — named locator key, not execution dispatch |
| Any redundancy or misuse? | **None found** |

**Decision: Keep ElementRole unchanged.**

ElementRole is a fundamental semantic type that provides type-safe access to role-specific locators across the framework. Replacing it with strings or positional arguments would remove the only compile-time guarantee that a requested locator role is valid. No action is required.

---

## Related

- Phase 18 plan doc: `docs/plan/Architectural Refactoring — Action/ongoing/.../phase-18-audit-elementrole.md`
- `elements/meta/ElementRole.java` — 24 constants
- `core/engine/UIEngine.java` — `resolve(Element, ElementRole, Object...)`
