# ElementRole Necessity Audit

**Date:** 2026-07-06  
**Scope:** `elements/meta/ElementRole` — all usages across the action, engine, and element layers  
**Goal:** Determine whether `ElementRole` is still necessary after the action ownership refactor (Phases 13–17), or whether it can be removed, internalized, or replaced  
**Outcome:** No changes required — architecture is sound

---

## Core Distinction: Dispatch vs Lookup

The earlier refactor phases (13–17) removed `ActionCapability` from execution paths because it was being used to infer behavior — "if capability is CLICKABLE, apply these hooks." That is runtime dispatch. It was the right thing to remove.

ElementRole is different in kind. It is used to identify data:

```java
engine.resolve(element, ElementRole.INPUT);
```

This is no different from:

```java
map.get(Key.INPUT);
```

No behavior changes. No execution path is selected. The caller is simply naming which locator it wants from an element that may expose several. ElementRole answers *which locator* — not *which code should execute*. Those are fundamentally different architectural concerns.

---

## Audit Questions

1. Is ElementRole still required by the public API?
2. Do action subclasses need to declare their role, or can it be inferred?
3. Are composite actions using ElementRole correctly?
4. Is there redundancy or misuse anywhere?

---

## Findings

### 1. ElementRole is a public API contract

ElementRole appears as a required parameter in five independent public contracts:

| Contract | Signature |
|----------|-----------|
| `UIEngine.resolve()` | `resolve(Element, ElementRole, Object...)` |
| `LocatorResolver.resolve()` | `resolve(Element, ElementRole, Object...)` |
| `LocatorResolver.resolveDescriptor()` | `resolveDescriptor(Element, ElementRole, Object...)` |
| `Element.getAllLocatorRoles()` | Returns `Map<ElementRole, String>` |
| `Via.descriptor()` | `descriptor(Element, ElementRole, Object...)` |

Removing or replacing ElementRole would require binary-breaking changes to all of the above and every Element implementation in the codebase. It is not an internal detail — it is the type-safe locator key at the center of the resolution pipeline.

### 2. Each action declares the locator role it operates on

All 16 concrete action subclasses define their role at construction time as a compile-time invariant. This is not arbitrary coupling — it is a statement of which part of the element the action targets:

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

`ClickAction` does not hardcode `TRIGGER` as an arbitrary constant. It defines that it operates on the trigger locator. That is what the role represents — a semantic identifier for a part of an element's structure.

Consider a login component:

```
LoginComponent
  USERNAME      → exposes INPUT
  PASSWORD      → exposes INPUT
  LOGIN_BUTTON  → exposes TRIGGER
  ERROR_MESSAGE → exposes TEXT
```

These are not implementation details — they are semantics. They answer *which locator do you want?*, not *which code should execute?* That is exactly what a role should represent.

### 3. Composite actions justify ElementRole even more

`SelectAction` and `SearchAndSelectAction` resolve multiple locators inside `execute()`:

**SelectAction:** `TRIGGER` (open dropdown) → `LIST` (select option)  
**SearchAndSelectAction:** `TRIGGER` (open) → `SEARCH_INPUT` (type term) → `SEARCH_RESULT` (pick result)

Without ElementRole, the alternatives are worse:

- Dedicated engine methods: `resolveTrigger()`, `resolveSearchInput()`, `resolveSearchResult()` — the engine interface grows unboundedly
- String keys: `resolve(element, "trigger")` — compile-time safety is gone

Each call to `engine.resolve(element, ElementRole.X)` is a named lookup. There is no dispatch — the roles are used to fetch specific locators, not to select execution paths.

### 4. Engine portability is preserved

The engine receives the same semantic request regardless of technology:

```java
resolve(element, TRIGGER)
```

Whether the engine underneath uses XPath, CSS, ARIA, or any future locator strategy, the caller expresses what it needs. The engine decides how to satisfy it. This is correct separation of concerns.

### 5. No redundancy or misuse found

- `getAllLocatorRoles()` on capability interfaces returns only the roles that interface exposes — no excess values
- `Interactions.java` calls `resolveDescriptor(element, ElementRole.X)` directly — consistent with the contract
- `EnumLocatorScanner` iterates `getAllLocatorRoles()` to build JSON — ElementRole is the key schema
- No action class uses ElementRole in a `switch` to branch execution logic

---

## Architectural Layer Placement

ElementRole belongs to the element model, not the action model:

```
Element
  ├── ElementRole        ← which part is being addressed
  ├── Locator map        ← where that part lives
  └── Metadata           ← element-level context

Action
  ├── ActionProfile      ← how it executes
  ├── Hooks              ← surrounding behavior
  └── Execution          ← what it does
```

Actions describe *what happens*. Elements describe *where it happens*. ElementRole is the bridge — it lets an action name a part of an element without knowing how the element is implemented. The two layers should not merge.

---

## Future Consideration: Enum Size

The enum currently has 24 constants. This is appropriate as long as each constant represents a universal locator semantic — something any engine or UI technology could reasonably understand.

Watch for drift if application-specific roles begin to appear (e.g., `CHECKOUT_CONFIRM_BUTTON`, `PROFILE_AVATAR`). Those would indicate the enum is being used as an application-level registry rather than a framework-level semantic type. Based on the current audit, no such drift is present.

---

## Verdict

| Question | Finding |
|----------|---------|
| Still required by public API? | **Yes** — UIEngine, LocatorResolver, Element, Via all require it |
| Can role be inferred instead of declared? | **No** — the element exposes multiple roles; the action must name which one it targets |
| Composite action usage correct? | **Yes** — named locator key, not execution dispatch |
| Any redundancy or misuse? | **None found** |
| Engine-agnostic? | **Yes** — the engine receives a semantic request and decides how to fulfill it |

**Decision: Keep ElementRole unchanged.**

ElementRole is a semantic identifier, not a behavior dispatcher. It names parts of an element rather than selecting execution paths — the same architectural distinction that drove the removal of capability-based dispatch in Phases 13–17. Keeping it is consistent with that direction.

---

## Related

- Phase 18 plan doc: `docs/plan/Architectural Refactoring — Action/ongoing/.../phase-18-audit-elementrole.md`
- `elements/meta/ElementRole.java` — 24 constants
- `core/engine/UIEngine.java` — `resolve(Element, ElementRole, Object...)`
