# Post-Plan Audit: OOP Violations Remediation

**Scope:** `docs/plan/draft/oop-violations-remediation/` (all four phase documents)
**Codebase scanned:** `src/main/java` (188 Java files)
**Audit date:** 2026-07-16

---

## Purpose

This document audits the four remediation phase plans against the actual codebase state.
For each phase it answers three questions:

1. Do the violations described in the plan actually exist?
2. Are the "Files changed" tables complete and accurate?
3. What gaps, risks, or underdescribed details would cause friction during implementation?

"Claim incorrect" in the findings table means a claim in the plan does not match current
source state. In most cases this is expected: the plan describes what needs to be
created, not what already exists. True discrepancies are flagged separately.

---

## Package structure (actual)

```
core/
  actions/      35 files -- Action, ElementAction, HookChainAction, ActionLabeled,
                            HookedAction, ClickAction, TypeAction, SelectAction,
                            UploadAction, ReadTextAction, ElementActions, ...
  engine/       UIEngineFactory, SeleniumEngine, LocatorDescriptor, LocatorStrategy
  interactions/ Via.java, hooks/
  resolvers/locator/ LocatorResolver, LocatorRequest, LocatorResolvers
  driver/
  bootstrap/
  ...
elements/
  api/          Element.java, LocatorFamily.java
  api/capability/ Clickable, Typeable, Selectable, ReadOnly, Searchable,
                  SearchableDropdown, SearchField, Listable, MultiSelectable, ...
  meta/         ElementRole, EnumClassRegistry
dsl/
  VoidDSL.java
examples/
```

---

## Phase 1 -- Action Layer

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `before()`, `after()`, `using()`, `withHooks()` all contain `if (this instanceof HookChainAction chain)` | Found in all four methods | CONFIRMED |
| `HookChainAction.withAdditionalHooks()` exists | Exists, not deprecated | CONFIRMED |
| `ActionLabeled` interface declares `elementLabel()` and `operationLabel()` | Confirmed at lines 13/16 | CONFIRMED |
| `HookChainAction` delegates labels through `instanceof ActionLabeled` cast | Lines 60-62 and 66-67 | CONFIRMED |
| `HookChainAction.operationLabel()` contains `switch (capability())` | Lines 67-72, cases: CLICKABLE, TYPEABLE, SELECTABLE, default "perform" | CONFIRMED |
| `HookedAction` is `@Deprecated(forRemoval = true, since = "0.2")` | Line 44, also `@Internal` | CONFIRMED |
| `mergeHooks()` and `withProfile()` do not yet exist on `Action` | Absent -- correctly described as the fix | CONFIRMED |

### Gap: operationLabel() override table

The plan specifies explicit `operationLabel()` overrides for five concrete action classes:

| Class | Plan specifies | Actual current state |
|---|---|---|
| `ClickAction` | `return "click"` | No override; inherits `ElementAction.operationLabel()` |
| `TypeAction` | `return "type"` | No override; inherits `ElementAction.operationLabel()` |
| `SelectAction` | `return "select"` | No override; inherits `ElementAction.operationLabel()` |
| `UploadAction` | `return "upload"` | No override; inherits `ElementAction.operationLabel()` |
| `ReadTextAction` | `return "read"` | No override; inherits `ElementAction.operationLabel()` |

`ElementAction.operationLabel()` (lines 226-232) currently derives the label by stripping
the `"Action"` suffix from the class name and lowercasing the first character. This already
produces `"click"`, `"type"`, `"select"` etc. for the five listed classes.

**Risk:** The plan does not state whether `ElementAction.operationLabel()`'s class-name
derivation logic is being removed. If it is kept, the explicit overrides in the plan are
redundant but harmless. If it is removed (in favour of the `getClass().getSimpleName()`
diagnostic default on `Action`), then the explicit overrides become mandatory.

**Recommendation:** Clarify in Phase 1 whether `ElementAction.operationLabel()`'s existing
derivation logic is kept, removed, or superseded by the new defaults.

### Gap: elementLabel defaults already exist on ElementAction

`ElementAction.elementLabel()` (lines 210-213) already implements label derivation via
`instanceof Enum<?>`. Phase 1 adds `elementLabel()` and `operationLabel()` defaults to
`Action` itself, and Phase 2 rewrites `ElementAction.elementLabel()` to use
`element.getDisplayText()`. These two phases overlap on this method -- Phase 1 adds the
default, Phase 2 replaces the body. The plan documents this correctly, but implementors
must apply Phase 1 and Phase 2 together for `ElementAction.elementLabel()` to be
consistent. If Phase 1 is applied in isolation, `ElementAction.elementLabel()` still uses
the `instanceof Enum<?>` check.

### Files changed accuracy

The Phase 1 table is accurate. No additional affected files were found.

---

## Phase 2 -- Element Interface

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `Element.java` contains `(Enum<?>) this` casts | Found in 4 places: `getExternalFileName()` line 39, `getPrimaryLocator()` line 62, `getDisplayText()` line 95, static `qualifiedLocatorKey()` line 129 | CONFIRMED |
| `ElementAction.elementLabel()` uses `instanceof Enum<?>` | Line 211 | CONFIRMED |
| `LocatorResolver.labelOf()` uses `instanceof Enum<?>` | Lines 181-185 | CONFIRMED |
| `ActionCapabilityProvider` declares `capability()` | Single method at line 39 | CONFIRMED |
| `ElementActions.capabilityFor()` has `instanceof ActionCapabilityProvider` check | Line 56 | CONFIRMED |
| `Element` does not yet have a `capability()` default | Absent from `Element.java` | CONFIRMED |
| `Listable.getIndex()` is abstract | Line 26, no default | CONFIRMED |
| `ElementSupport.java` does not yet exist | Absent -- correctly described as new | CONFIRMED |
| `LocatorRoles.java` does not yet exist | Absent -- correctly described as new | CONFIRMED |

### Gap: capability() is on Action, not Element

`Action.java` currently declares a `default ActionCapability capability()` method (line 153).
`Element.java` does not. The plan adds `capability()` to `Element` and documents that the
nine capability interfaces override it. What the plan does not address:

- Does `Action.capability()` remain after Phase 2?
- If `Action` also has `capability()`, does that create an ambiguity for any class
  implementing both `Action` and `Element`?
- The plan says `ElementActions.capabilityFor()` simplifies to `element.capability()`.
  That only works cleanly if `capability()` is on `Element` -- which is correct.

**Recommendation:** Add a note to Phase 2 that `Action.capability()` already exists and
must either be reconciled or explicitly left alone (it routes a different concern --
the action's declared capability, not the element's).

### Gap: Listable implementation landscape is shallower than described

The plan implies multiple concrete enum implementations override `getIndex()`. The actual
state is narrower:

- Only two interfaces extend `Listable`: `Selectable` and (via `Selectable`) `SearchableDropdown`.
- `Selectable.getIndex()` overrides with a hardcoded `return 0` (not ordinal arithmetic,
  just zero).
- No concrete enum class directly implementing `Listable` was found.

**The hardcoded `return 0` in `Selectable.getIndex()`** is the only override to evaluate.
This is not ordinal arithmetic -- it is a constant. The plan's guidance says "verify it does
not do arithmetic on the ordinal" before deleting. This override should not be silently
deleted: returning `0` for all selectable elements may be intentional (zero-based single-
select index for a specific engine API), or it may be a placeholder bug. This requires a
deliberate decision, not just an audit pass.

**Recommendation:** Add an explicit note to Phase 2 that `Selectable.getIndex()` returns
a hardcoded `0` and requires a conscious decision before deletion.

### Gap: nine capability interfaces vs actual inventory

The plan lists nine capability interfaces to remove `implements ActionCapabilityProvider`
from: `Clickable`, `Typeable`, `ReadOnly`, `Selectable`, `MultiSelectable`, `Listable`,
`Uploadable`, `Table`, `EditableTable`. The audit found the following interfaces in
`elements/api/capability/`: `Clickable`, `Typeable`, `Selectable`, `ReadOnly`, `Searchable`,
`SearchableDropdown`, `SearchField`, `Listable`, `MultiSelectable`.

**Discrepancy:** The audit found `Searchable`, `SearchableDropdown`, `SearchField` in the
capability package, but the plan's removal list does not name them. Conversely, `Uploadable`,
`Table`, `EditableTable` appear in the plan's list but were not surfaced in the capability
package scan. These may exist but were not returned by the directory listing.

**Recommendation:** Before implementing Phase 2, grep for all `implements ActionCapabilityProvider`
occurrences to get the definitive list:
```
grep -rn "ActionCapabilityProvider" src/main/java/elements/
```

### Files changed accuracy

The `ElementSupport.java` and `LocatorRoles.java` new-file entries are correct. The
`ActionCapabilityProvider.java` DELETE entry is correct. The capability interface list in the
table may be incomplete (see gap above). `Selectable.java` is not listed in the "Files
changed" table despite having the only concrete `getIndex()` override that must be evaluated.

**Missing entry:** Add `elements/api/capability/Selectable.java` to the table with change
note "Evaluate hardcoded `getIndex()` return 0 before removal."

---

## Phase 3 -- DSL Dispatch

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `VoidDSL.java` contains `instanceof` chains for capability dispatch | Confirmed | CONFIRMED |
| All four named methods contain instanceof chains | Confirmed: `selectFromDropdownByContext`, `triggerDropdownByContext`, `getSearchedElementByContext`, `clickSearchableElementByContext` | CONFIRMED |
| All four are string-key-resolved (dynamic) methods | Confirmed -- all resolve via `String unresolvedEnumName` or `String keySuffix` | CONFIRMED |
| Total instanceof occurrences: audit table shows 4 methods | 10 instanceof occurrences found across more than 4 methods | GAP (see below) |

### Gap: audit table is incomplete -- two methods missing

The plan's pre-filled audit table covers four methods. The actual `VoidDSL.java` has
`instanceof` in at least six locations across five methods:

| Method | Types checked | In plan table? |
|---|---|---|
| `selectFromDropdownByContext` | `MultiSelectable`, `Selectable` | YES |
| `triggerDropdownByContext` | `MultiSelectable`, `Selectable` | YES |
| `getSearchedElementByContext` | `Searchable` (x2) | YES |
| `clickSearchableElementByContext` | `Searchable` | YES |
| `setCheckboxByContext` | `Checkable` | **NO** |
| `resolveEnumConstant` | `ResolvableEnum` | **NO** |

`setCheckboxByContext` follows the same pattern as the other four methods -- it resolves an
element from a string key and dispatches via `instanceof Checkable`. It must be classified
in the audit table and handled in Phase 3.

`resolveEnumConstant` uses `instanceof ResolvableEnum` in a different context -- it checks
whether an enum constant implements a marker interface to determine resolution strategy.
This may be legitimate (it is not dispatching to an engine operation) and may not belong in
the EnumMap dispatch table. It requires explicit classification in the audit table.

**Recommendation:** Add `setCheckboxByContext` (with classification: dynamic, needs
EnumMap entry) and `resolveEnumConstant` (with classification: investigate, may be
legitimate) to the pre-filled audit table in Phase 3.

**Impact on implementation:** Adding `setCheckboxByContext` to the EnumMap dispatch requires
an `ActionCapability.CHECKABLE` constant (or equivalent). If that constant does not yet exist
on the `ActionCapability` enum, it must be added as part of Phase 3.

### Gap: ActionCapability enum completeness

The plan assumes all required `ActionCapability` constants already exist. The EnumMap in
Step 3 adds entries for `MULTI_SELECTABLE`, `SELECTABLE`, `TYPEABLE`. The missing
`setCheckboxByContext` requires `CHECKABLE` (or `CHECKABLE_ELEMENT`). The current
`ActionCapability` enum inventory is not listed in any phase document. If constants are
missing, Phase 3 silently expands its scope to include enum constant additions.

**Recommendation:** Add a preflight step to Phase 3: enumerate all `ActionCapability`
constants and cross-reference them against all `instanceof` dispatch sites found in the
audit.

### Files changed accuracy

The Phase 3 table is accurate for the four methods in scope. If `setCheckboxByContext`
is reclassified as in-scope (likely), `dsl/VoidDSL.java` remains the only changed file
but the scope of change is larger than described.

---

## Phase 4 -- Infrastructure

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `UIEngineFactory` contains `switch (engineName)` | Lines 45-50 | CONFIRMED |
| Commented-out Playwright line exists | Line 47: `// case "playwright" -> new PlaywrightEngine(); // Phase 3` | CONFIRMED |
| `SearchableDropdown.getAllLocatorRoles()` uses equality chains | Lines 63, 65, 67 -- triple-chained equality | CONFIRMED |
| `SearchField.getAllLocatorRoles()` uses same pattern | Line 49 -- single equality check `!btn.equals(input)` | CONFIRMED (less extensive) |
| `LocatorRoles.java` does not yet exist | Absent | CONFIRMED |
| `Via.java` exists with per-capability static methods | Confirmed -- 26 public static methods total | CONFIRMED |

### Gap: Via.java is larger than the plan describes

The plan characterises `Via` as a catalogue of per-capability cast and check methods.
The actual `Via.java` has 26 public static methods in four distinct groups:

| Group | Count | Status |
|---|---|---|
| Capability cast helpers (e.g. `clickable(Element)`) | 9 active | Target of Phase 4 fix |
| Capability check predicates (e.g. `isClickable(Element)`) | 6 active | Target of Phase 4 fix |
| Locator descriptor resolvers (`descriptor(...)`) | 3 active | **Not addressed in plan** |
| Selenium `By` locators and `WebElement` finders | 8 `@Deprecated` | Not addressed in plan |

The plan's audit grep (`grep -rn "Via\." src/`) and classification table will surface all
26 methods, but the plan's fix description ("reduce to at most one generic method or delete
the class") implicitly assumes Via is only the capability helpers. The three active
`descriptor(...)` methods resolve `LocatorDescriptor` objects -- they are not capability
dispatchers and should not be inlined or deleted unless their callers are migrating to
another resolution path.

**Risk:** Applying the plan's "inline category 1 and 2, delete or reduce Via" instruction
without distinguishing the descriptor group could inadvertently remove actively used
locator-resolution API.

**Recommendation:** Add a fourth category to the classification table in Phase 4:

| Category | Example | Replacement |
|---|---|---|
| Boolean check | `Via.isClickable(e)` | `e instanceof Clickable` or pattern match |
| Cast for immediate use | `Via.clickable(e).click()` | `((Clickable) e).click()` |
| Dynamic / unknown at compile time | `Via.cast(e, capClass)` | Keep one generic helper |
| **Locator descriptor** | `Via.descriptor(e)` | **Do not inline -- out of scope for this phase** |

### Gap: SearchField equality chain is less severe than SearchableDropdown

`SearchableDropdown.getAllLocatorRoles()` has O(n^2) growth -- four roles, six comparisons.
`SearchField.getAllLocatorRoles()` currently has only two locator roles and one equality
check. The `LocatorRoles.roleMap()` fix is still the right call for SearchField (it
future-proofs the class), but the problem statement overstates the current severity for
SearchField. The plan groups them as equivalent; they are not.

### Files changed accuracy

The `LocatorRoles.java` new-file entry is correct. The Via row says "reduce to 1 generic
method or DELETE" -- accurate but may need qualification given the descriptor group. The
locator descriptor methods in Via are not listed as out-of-scope, which risks them being
caught in a broad inline sweep.

---

## Cross-phase concerns

### 1. Phase ordering and safe intermediate states

The plan states Phases 1 and 2 as dependencies for Phase 3, and Phase 4 as independent.
One ordering risk is not called out: Phase 2 adds `capability()` to `Element`, and Phase 3
builds `EnumMap<ActionCapability, ...>` keyed on `element.capability()`. If Phase 3 is
applied before Phase 2 completes, the `element.capability()` call either does not compile
(Element has no such method) or falls through to `ActionCapability.UNKNOWN` on elements that
still implement only `ActionCapabilityProvider`. The dependency is described but the failure
mode is not.

**Recommendation:** Note in Phase 3 that `element.capability()` returning `UNKNOWN` for
incompletely migrated elements will silently throw `UnsupportedOperationException` from the
dispatch map's null-handler rather than failing at compile time.

### 2. ActionCapabilityProvider bridge period

Phase 2 deletes `ActionCapabilityProvider`. Phase 3 routes all dispatch through
`element.capability()`. If there are any external (test or consumer) classes implementing
`ActionCapabilityProvider` directly (not via the nine capability interfaces), deleting the
interface is a breaking change. The plan's verification step greps `ActionCapabilityProvider`
in `src/` -- which covers the framework source. It does not cover `src/test/` or any
consumer module.

**Recommendation:** Extend the Phase 2 verification grep to `src/test/` and document the
expected zero-result scope.

### 3. ElementSupport scope discipline

The plan is precise: `ElementSupport` holds only `nameOf`, `declaringClassOf`, `ordinalOf`.
Phase 4 correctly places `LocatorRoles` in its own class rather than growing `ElementSupport`.
This boundary is architecturally sound and consistent across phases. No action needed --
flagged here as a cross-phase invariant to preserve during implementation.

### 4. Via and Phase 3 interaction

Phase 3's EnumMap dispatch inlines the capability-cast lambdas. Phase 4 reduces Via's
capability cast helpers to zero (inline them at call sites). These are complementary but
must not conflict: a `Via.clickable(e).click()` call site that is also a candidate for
Phase 3's typed overload should be handled by Phase 3 (as a typed overload), not by Phase 4
(as a raw cast inline). If Phase 4 is applied first, the inlined cast becomes a
`((Clickable) e).click()` -- which Phase 3 could then further promote to a typed parameter.
Both orderings produce the same end state, but a combined sweep prevents double-touching
the same call site.

---

## Implementation risk summary

| Risk | Severity | Phase | Notes |
|---|---|---|---|
| `operationLabel()` override necessity unclear | Medium | 1 | Does Phase 1 remove or keep ElementAction's class-name derivation? |
| `Selectable.getIndex()` hardcoded `return 0` | Medium | 2 | Not ordinal arithmetic -- requires deliberate decision before removal |
| Capability interface inventory may be incomplete | Low | 2 | `grep "implements ActionCapabilityProvider"` before writing code |
| `setCheckboxByContext` missing from Phase 3 audit table | High | 3 | Dynamic dispatch site; needs EnumMap entry and possibly new ActionCapability constant |
| `resolveEnumConstant` instanceof unclassified | Low | 3 | May be legitimate non-dispatch instanceof; needs explicit classification |
| `ActionCapability` enum may be missing constants | Medium | 3 | `CHECKABLE` needed for `setCheckboxByContext`; audit enum before implementing |
| Via descriptor group not separated from capability group | High | 4 | Risk of inlining active locator-resolution API |
| `capability()` on Action vs Element ambiguity | Low | 2 | No conflict today; reconcile or document intentional coexistence |
| Phase 3 applied before Phase 2 silent failure | Low | 3 | Documents dependency but not the silent-failure failure mode |

---

## Verdict by phase

| Phase | Violations confirmed | Plan accuracy | Implementation ready |
|---|---|---|---|
| Phase 1 | All confirmed | High -- minor gap on operationLabel() derivation | Yes, with operationLabel() clarification |
| Phase 2 | All confirmed | Medium -- Selectable.getIndex() and capability inventory gaps | Yes, with preflight greps |
| Phase 3 | Confirmed, but incomplete audit table | Medium -- setCheckboxByContext and resolveEnumConstant missing | Needs audit table update before coding |
| Phase 4 | All confirmed | Medium -- Via descriptor group not scoped out | Needs Via classification table update before coding |

Phase 1 and Phase 2 are ready to implement with minor clarifications. Phase 3 requires
the audit table to be completed (add `setCheckboxByContext`, classify `resolveEnumConstant`)
before any code is written. Phase 4 requires the Via classification table to explicitly
exclude descriptor methods from the inline/delete sweep.
