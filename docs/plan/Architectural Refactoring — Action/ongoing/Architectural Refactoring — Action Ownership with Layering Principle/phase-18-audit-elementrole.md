# Phase 18 — Audit ElementRole for Necessity

**Status:** Pending Investigation  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — investigation only, no changes yet  
**Depends on:** Phases 13–17 complete (Phases 13–16 are Done; Phase 17 is In Progress)

---

## Objective

Determine whether `ElementRole` is still necessary in the new architecture. Audit every action subclass to verify it can resolve its locator without an explicit role parameter. Document findings and make a final decision on ElementRole's future.

---

## Context

`ElementRole` exists to tell the action which locator field to resolve:

```java
// Current usage
LocatorDescriptor resolve() {
    return engine.resolve(element, ElementRole.TRIGGER);
}
```

With concrete action subclasses, each action knows its semantic meaning. ClickAction uses the trigger. TypeAction uses the input. Does it still need to ask "which role?"

---

## Investigation

### Step 1: Audit Each Action Type

For each action subclass created in Phase 14, determine: **Does this action always target the same locator role?**

```
ClickAction
  → element.getTriggerLocator()
  → Always? YES ✓
  → ElementRole needed? NO

TypeAction
  → element.getInputLocator()
  → Always? YES ✓
  → ElementRole needed? NO

SelectAction (more complex)
  → open(): element.getTriggerLocator()
  → selectByText(): element.getListLocator()
  → select(): element.getTriggerLocator()
  → Always same? NO ⚠️
  → ElementRole needed? DEPENDS ON VARIANT

HoverAction
  → element.getTriggerLocator()
  → Always? YES ✓
  → ElementRole needed? NO

... audit others ...
```

### Step 2: Check for Edge Cases

Search for element types or capabilities that might have unusual locator requirements:

- **Autocomplete:** typing (input), then selecting from popup (different locators)
- **SearchField + SearchableDropdown:** similar pattern
- **EditableTable:** cell editing (depends on row/column)
- **Custom elements with multiple interactive surfaces**

### Step 3: Document Findings

Create brief summary:

```markdown
### ElementRole Audit Results

**Actions that always use one locator role:**
- ClickAction: TRIGGER
- CheckAction: TRIGGER
- HoverAction: TRIGGER
- TypeAction: INPUT
- ClearAction: INPUT
- AppendTypeAction: INPUT
- UploadAction: INPUT
- ... count: 15 actions, no ElementRole needed

**Actions that may use multiple locators:**
- SelectAction: TRIGGER (open) and LIST (select) — needs strategy
- OpenAction: TRIGGER only (if refactored from SelectAction)
- SearchField actions: INPUT and LIST
- SearchableDropdown actions: TRIGGER and LIST
- EditableTable actions: depends on cell coordinates
- ... count: 8 actions, ElementRole OR strategy needed

**Decision:** ElementRole can be removed from public Element/Capability interfaces.
Actions that need multiple locators use constructor parameters or internal strategy pattern.
```

### Step 4: Propose Solution

**Option A: Remove ElementRole entirely**

If all actions work without it, delete it from public contracts and move to internal utilities only.

**Option B: Keep ElementRole, pass to select actions**

If SelectAction and others truly need it, add `ElementRole` as a constructor parameter to those actions.

**Option C: Strategy pattern**

Actions that need multiple locators define an internal enum:

```java
public final class SelectAction extends ElementAction {
    enum Strategy { TRIGGER, LIST }
    
    private final Strategy strategy;
    
    public SelectAction(Selectable element, Strategy strategy) {
        super(element);
        this.strategy = strategy;
    }
    
    @Override
    protected LocatorDescriptor resolve() {
        return switch (strategy) {
            case TRIGGER -> engine.resolve(element, ElementRole.TRIGGER);
            case LIST -> engine.resolve(element, ElementRole.LIST);
        };
    }
}
```

---

## Implementation (Post-Audit)

### If Decision is "Remove ElementRole"

Delete from public contracts:
- `Element` interface (no change, it doesn't reference role)
- `ElementRole` enum (move to `core.actions` internal package if needed)

Update action subclass constructor documentation.

### If Decision is "Keep ElementRole"

Add to select action constructors:

```java
public SelectAction(Selectable element, ElementRole roleHint) {
    // Use roleHint to decide which locator
}
```

Document which actions expect which roles.

### If Decision is "Internal Strategy"

Actions define minimal strategy enums instead of using `ElementRole`.

---

## Tests

Write tests to verify audit assumptions:

```java
@Test
public void clickAction_doesNotNeedElementRole() {
    ClickAction action = new ClickAction(clickable);
    action.execute(engine, descriptor);
    // Verify it resolved TRIGGER without being told
}

@Test
public void selectAction_canSelectByText_withoutExplicitRole() {
    SelectAction action = new SelectAction(selectable);
    action.execute(engine, descriptorForList);
    // Verify it can work with list descriptor
}
```

---

## Exit Criteria

- [ ] Every action subclass audited for ElementRole necessity
- [ ] Edge cases identified and documented
- [ ] Decision made: Remove / Keep / Internal Strategy
- [ ] Decision documented in final ADR-015 or architecture note

---

## Next Phase

Phase 19 — ElementActions Factory Role (create ADR-015 post-audit)

---

## Notes

This phase is investigation-driven. No code changes are expected until the decision is made and documented. The output is a single summary document that explains the finding and guides Phase 19.

