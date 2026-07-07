# Phase 18 — Audit ElementRole for Necessity

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — investigation only, no code changes  
**Depends on:** Phases 13–17 complete

---

## Objective

Determine whether `ElementRole` is still necessary in the new architecture. Audit every action
subclass to verify whether explicit role parameters are justified. Document findings and make a
final decision.

---

## Audit Findings

### ElementRole Enum

**File:** `src/main/java/elements/meta/ElementRole.java`  
**Size:** 24 constants

```
PRIMARY, SECONDARY               — Generic locator resolution fallback positions
TRIGGER                          — Clickable trigger (button, icon, dropdown handle)
INPUT                            — Text / value input field
LIST                             — Generic list container or options panel
TEXT                             — Static textual element
SEARCH_INPUT                     — Search text input field
SEARCH_BUTTON                    — Search action button
SEARCH_RESULT                    — Search result list / panel
TOOLTIP_CONTENT                  — Tooltip content element
TABLE                            — Table root element
ROW, COLUMN, CELL, HEADER        — Table structural roles
ADD_ROW_BUTTON, REMOVE_ROW_BUTTON, FOOTER_INPUT_ROW   — Editable table controls
MULTI_TRIGGER, MULTI_LIST        — Multi-select dropdown roles
```

### Public API Presence

ElementRole is a required parameter in **core public contracts**:

| Contract | Signature | Location |
|----------|-----------|----------|
| `UIEngine.resolve()` | `resolve(Element, ElementRole, Object...)` | Engine interface |
| `LocatorResolver.resolve()` | `resolve(Element, ElementRole, Object...)` | Locator resolver API |
| `LocatorResolver.resolveDescriptor()` | `resolveDescriptor(Element, ElementRole, Object...)` | Locator resolver API |
| `Element.getAllLocatorRoles()` | Returns `Map<ElementRole, String>` | Base element interface |
| `Via.descriptor()` | `descriptor(Element, ElementRole, Object...)` | Interactions API |

ElementRole is **not** a private implementation detail — it is the type-safe contract at the
center of the locator resolution pipeline.

### Usage by All 16 Concrete Action Classes

Every `ElementAction` subclass hard-codes its role in the constructor:

| Action | Role Passed to Constructor | Notes |
|--------|---------------------------|-------|
| `ClickAction` | `TRIGGER` | single locator |
| `ToggleAction` | `TRIGGER` | single locator |
| `CheckAction` | `TRIGGER` | single locator |
| `HoverAction` | `TEXT` | single locator |
| `TypeAction` | `INPUT` | single locator |
| `ClearAction` | `INPUT` | single locator |
| `AppendTypeAction` | `INPUT` | single locator |
| `TypeAndPressAction` | `INPUT` | single locator |
| `UploadAction` | `INPUT` | single locator |
| `OpenAction` | `TRIGGER` | single locator |
| `SelectByTextAction` | `LIST` | single locator |
| `SelectByValueAction` | `LIST` | single locator |
| `TypeSearchAction` | `SEARCH_INPUT` | single locator |
| `SubmitSearchAction` | `SEARCH_BUTTON` | single locator |
| `SelectAction` | `TRIGGER` (primary) + calls `engine.resolve(element, LIST)` internally | composite — two roles |
| `SearchAndSelectAction` | `TRIGGER` (primary) + calls `engine.resolve(element, SEARCH_INPUT)` and `engine.resolve(element, SEARCH_RESULT, term)` internally | composite — three roles |

**12 actions use exactly one role. 4 composite actions use multiple roles explicitly.**

The composite actions (`SelectAction`, `SearchAndSelectAction`) call `engine.resolve()` directly
inside `execute()` for their secondary locators — they use `ElementRole` as a named locator key,
not as a dispatch mechanism.

### Broader Ecosystem

- **`getAllLocatorRoles()` contract:** Every capability interface implements this method,
  returning a `Map<ElementRole, String>` mapping each semantic role to its locator key. This is
  the primary way the locator-resolution infrastructure discovers which XPath/CSS to load.
- **`Interactions.java`:** Uses `resolveDescriptor(element, ElementRole.X)` in 10+ methods.
- **`ElementActions.of(Element, ElementRole, BiConsumer)`:** The legacy factory takes an explicit
  role parameter.
- **`EnumLocatorScanner`:** Iterates `getAllLocatorRoles()` to build JSON from Element enums.
- **`SeleniumEngine.resolve()`:** Implements the UIEngine contract, takes ElementRole.
- **`VoidDemo`:** Uses `engine.resolve(element, ElementRole.TEXT)` directly in a hook.

---

## Decision: Keep ElementRole Unchanged

**Verdict: Keep.** ElementRole is not a candidate for removal or internalization.

**Reasoning:**

1. **It is a public interface parameter.** `UIEngine.resolve()`, `LocatorResolver.resolveDescriptor()`,
   and `Element.getAllLocatorRoles()` all reference it. Removing it would require binary-breaking
   API changes across the entire framework.

2. **It is the semantic locator key.** Every element implementation maps role constants to
   locator strings (XPath/CSS). Without ElementRole there is no type-safe way to request a
   specific locator from an element that exposes multiple (e.g., Selectable exposes TRIGGER and
   LIST; SearchableDropdown exposes four roles).

3. **Composite actions need it explicitly.** `SelectAction` and `SearchAndSelectAction` resolve
   multiple locators during `execute()`. They name each locator by role — that is the correct
   design. There is nothing to fix here.

4. **It provides compile-time safety and discoverability.** An enum over raw strings prevents
   typos, enables IDE navigation, and makes it obvious which locator semantic is intended.

5. **The Phase 18 concern was pre-empted by Phase 14.** The concern was: "does each action
   still need to be *told* which role?" The answer is yes — but the role is hardcoded in the
   action constructor (known at compile time), not inferred at runtime. `ClickAction` always
   resolves `TRIGGER`; `TypeAction` always resolves `INPUT`. ElementRole is a stated invariant,
   not a runtime dispatch.

**No code changes required.**

---

## Audit per Action Subclass

```
ClickAction       → TRIGGER     always  ✓ role hardcoded in constructor
ToggleAction      → TRIGGER     always  ✓
CheckAction       → TRIGGER     always  ✓
HoverAction       → TEXT        always  ✓ (uses TEXT locator, not TRIGGER)
TypeAction        → INPUT       always  ✓
ClearAction       → INPUT       always  ✓
AppendTypeAction  → INPUT       always  ✓
TypeAndPressAction→ INPUT       always  ✓
UploadAction      → INPUT       always  ✓
OpenAction        → TRIGGER     always  ✓
SelectByTextAction→ LIST        always  ✓
SelectByValueAction→ LIST       always  ✓
TypeSearchAction  → SEARCH_INPUT always ✓
SubmitSearchAction→ SEARCH_BUTTON always ✓
SelectAction      → TRIGGER + LIST         composite, both needed ✓
SearchAndSelectAction → TRIGGER + SEARCH_INPUT + SEARCH_RESULT  composite ✓
```

All 16 actions correctly declare their locator role. No role is unnecessary or redundant.

---

## Exit Criteria

- [x] Every action subclass audited for ElementRole necessity
- [x] Edge cases (composite actions) identified and documented
- [x] Decision made: **Keep**
- [x] Decision documented (this document)

---

## Next Phase

Phase 19 — ElementActions Factory Decision (ADR-015)
