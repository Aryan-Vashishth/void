# Phase 20 — Update Documentation

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — documentation only  
**Depends on:** Phases 13–19 complete

---

## Objective

Update all architecture documentation, examples, and README to reflect the new action ownership model. Ensure the codebase tells a consistent story about capabilities, actions, and execution.

---

## Context

The refactoring changes several key concepts:

- **OLD:** Capabilities emit anonymous actions via factory. Profiles live on capabilities.
- **NEW:** Capabilities emit concrete action types. Actions own profiles. No factory layer.

---

## Documentation to Update

### 1. Architecture Overview

**File:** `docs/architecture/system-overview.md`

Update dependency flow diagram:

```
BEFORE:
Element
    ↓
Capability
    ↓
Factory
    ↓
Profiles
    ↓
Switch
    ↓
Hooks

AFTER:
Element
    ↓
Capability (emits concrete action)
    ↓
Concrete Action (ClickAction, TypeAction, etc.)
    ↓
Action profile (safely(), debug(), using())
    ↓
ExecutionPipeline
    ↓
Engine
```

Add text explaining the new model:

```markdown
### Action Ownership

Each action type owns its execution policy.

- ClickAction knows how to execute a click safely
- TypeAction knows how to execute typing safely  
- SelectAction knows how to execute selection safely

Capabilities emit these concrete types. The action knows itself.

User code sees the same fluent API:
  element.click().safely()          // returns ClickAction, then wraps with profile
  element.type("text").debug()      // returns TypeAction, then wraps with profile
```

### 2. Core Packages Reference

**File:** `docs/architecture/core-packages.md`

Update examples to use new pattern:

```
BEFORE:
  LoginPage.USERNAME.type("admin")
    .withHooks(Before.CLEAR_FIELD, After.HIGHLIGHT)

AFTER:
  LoginPage.USERNAME.type("admin")        // returns TypeAction
    .before(Before.CLEAR_FIELD)           // fluent API
    .after(After.HIGHLIGHT)               // fluent API
```

Add section on action types:

```markdown
## Action Types

Each capability emits a concrete action type:

- `Clickable` → `ClickAction`
- `Typeable` → `TypeAction`, `ClearAction`, `AppendTypeAction`
- `Selectable` → `SelectAction`, `SelectByTextAction`, etc.
- `Hoverable` → `HoverAction`
- ...

Action types own:
- Execution logic (execute() method)
- Profile defaults (defaultSafeProfile(), etc.)
- Metadata (capability, element reference)

Users interact through the fluent Action interface, which is the same across all types.
```

### 3. Design Principles

**File:** NEW — `docs/architecture/design-principles.md` or update existing architecture guide

Add section on Layering Principle (ADR-013):

```markdown
## Architectural Invariants

These principles guide all decisions and must be enforced in code review:

### Layering Principle (ADR-013)

Capabilities describe *what* an element is.
Actions describe *how* to execute operations on that element.
Execution infrastructure lives in actions and pipelines, never in capabilities.

This keeps concerns separated and allows multiple actions from the same 
capability to have independent execution policies.

### Derived Rules

1. **Metadata ≠ Behavior:** ActionCapability is metadata for logging/tracing. 
   Never use it for dispatch.

2. **Self-aware Execution:** Actions never query their own capability to decide 
   how to execute. They already know what they are.

3. **Extension via Types:** Add behavior by introducing new action types 
   (DoubleClickAction, RightClickAction), not by modifying central dispatch.

4. **Immutable Actions:** Action instances are immutable. Fluent calls return 
   new instances.
```

### 4. Hook Evolution (if separate doc)

**File:** `docs/architecture/hooks-reference.md` (if exists)

Update to clarify that hooks are owned by actions, not capabilities:

```markdown
## Hook Profiles

Hooks are applied through action profiles. Each action type defines its own 
safe/debug/reliable profiles.

Example:

  ClickAction.CLICK_ACTION_SAFE_PROFILE contains:
    Before: WAIT_FOR_ELEMENT_CLICKABLE
    After: WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT
    
  TypeAction.TYPE_ACTION_SAFE_PROFILE contains:
    Before: CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE
    After: HIGHLIGHT_ELEMENT

User code doesn't care about these constants—it just calls safely():

  element.click().safely()         // Uses ClickAction's safe profile
  element.type("x").safely()       // Uses TypeAction's safe profile
```

### 5. Example Code and Demo

**File:** `src/main/java/tests/demo/VoidDemo.java`

Update to use new action pattern:

```
BEFORE:
  return ElementActions.of(loginPage.USERNAME, ElementRole.INPUT, 
           (engine, d) -> engine.type(d, username))
    .safely();

AFTER:
  return loginPage.USERNAME.type(username)
    .safely();
```

**File:** `docs/` examples (README, quick-start, tutorials)

Replace all ElementActions.of() examples with direct capability method calls.

### 6. ADR References

**File:** `docs/architecture/README.md`

Link to new ADRs:

```markdown
## Foundational Decisions

- [ADR-013: Architectural Layering Principle](../decisions/accepted/013-architectural-layering-principle.md) 
  — Core principle guiding responsibility assignment
  
- [ADR-014: Concrete Actions over Anonymous Lambdas](../decisions/accepted/014-concrete-actions-over-lambdas.md)
  — Why actions are now first-class types

- [ADR-015: ElementActions Factory Scope](../decisions/accepted/015-elementactions-factory-scope.md)
  — Decision on factory utility (to be created in Phase 19)
```

### 7. Glossary / Terms

**File:** `docs/architecture/glossary.md` (if exists)

Update definitions:

```markdown
**Action** — A deferred execution operation on an element. Owns its own 
execution policy (profiles, hooks, defaults). Concrete types: ClickAction, 
TypeAction, SelectAction, etc.

**Capability** — A structural contract describing what an element can do. 
Does NOT describe how to execute. Emits concrete action types.

**Profile** — A set of before/after hooks applied to an action during execution. 
Each action type owns its profile constants and defaults.

**ActionCapability** — An enum identifying an action's semantic type (CLICKABLE, 
TYPEABLE, etc.). Metadata-only; never used for dispatch.

**ElementRole** — [If kept] Identifies which locator on an element to target.
[If removed] No longer used; action types choose their own locators.
```

---

## Implementation Steps

### Step 1: Review and Update Architecture Docs

- [ ] Update `docs/architecture/system-overview.md` with new flow
- [ ] Update `docs/architecture/core-packages.md` with action examples
- [ ] Create or update `docs/architecture/design-principles.md`
- [ ] Update hook/profile documentation
- [ ] Update all examples to use new action pattern

### Step 2: Update Decision Records

- [x] Ensure ADR-013 and ADR-014 are visible from architecture guide
- [x] ADR-012 created in Phase 19 (ElementActions factory scope)
- [x] ADR-013 created (Architectural Layering Principle)
- [x] ADR-014 created (Concrete Actions over Anonymous Lambdas)
- [ ] Update ADR-011 (boundary) if needed — not required, scope unchanged

### Step 3: Update Code Examples

- [ ] Demo code (VoidDemo.java)
- [ ] README examples
- [ ] Tutorial documentation
- [ ] Inline Javadoc in key classes (ElementAction, action subclasses)

### Step 4: Add Migration Guide (Optional)

If contributors have existing code using ElementActions.of(), create migration guide:

```markdown
## Migration: ElementActions.of() to Concrete Actions

OLD CODE:
  Element.of(element, role, (engine, d) -> engine.click(d))
  
NEW CODE:
  new ClickAction(element)

Or via capability method:
  element.click()  // if element implements Clickable
```

### Step 5: Compile Check

Verify all documentation examples compile:

```bash
# If docs have embedded code samples, validate they're accurate
javac docs/examples/*.java  # if samples exist
```

### Step 6: Review Against ADRs

Before finalizing, review all documentation against ADR-013 and ADR-014:

- Does it reinforce the layering principle?
- Does it explain why actions own execution policy?
- Does it clarify that capabilities describe, not execute?

---

## Affected Files

**Modify:**
- `docs/architecture/README.md`
- `docs/architecture/system-overview.md`
- `docs/architecture/core-packages.md`
- `docs/architecture/hooks-reference.md` (if exists)
- `docs/architecture/configuration-reference.md` (if needed)
- `README.md` (top-level)

**Create or update:**
- `docs/architecture/design-principles.md`
- `docs/architecture/glossary.md`
- Migration guide (optional)

**Update examples:**
- `src/main/java/tests/demo/VoidDemo.java`
- Inline Javadoc in `ElementAction`, action subclasses, capability interfaces

---

## Exit Criteria

- [x] Architecture documentation reflects new model (system-overview.md, core-packages.md)
- [x] All examples use concrete action types
- [x] ADR-012, ADR-013, ADR-014 linked from system-overview.md decision traceability
- [x] Design principles documented (ADR-013 Layering Principle; Architecture Invariants in system-overview.md)
- [x] No references to old ElementActions.of() pattern in examples or architecture docs
- [ ] Glossary — no glossary.md exists; definitions covered inline in ADRs and architecture docs
- [x] Pull request reviewable

---

## Notes

This phase happens last because it documents the final state. All changes should be complete before documentation is finalized.

If any documentation reveals inconsistencies (e.g., "this action should do X but the implementation does Y"), file issues or PRs to fix the implementation, not the documentation.

