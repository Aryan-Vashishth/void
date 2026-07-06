# Phase 15 — Capability Refactoring to Emit Actions

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Medium — modifies public API of capability interfaces  
**Depends on:** Phase 13 (ElementAction) — Done, Phase 14 (concrete actions) — Done  
**Parallel with:** Phase 14, Phase 16

---

## Objective

Update every capability interface method to return concrete action types instead of factory-created anonymous actions. This removes `ElementActions.of()` from capability implementations and makes action types explicit.

---

## Context

Capability methods currently look like:

```java
// OLD
default Action click() {
    return ElementActions.of(this, ElementRole.TRIGGER, (engine, d) -> engine.click(d));
}
```

They will become:

```java
// NEW
default ClickAction click() {
    return new ClickAction(this);
}
```

This exposes action ownership: the capability decides to emit a ClickAction, and the ClickAction knows how to execute itself.

---

## Target Design

### Covariant Return Types

Capability methods return concrete action types (covariant returns):

```java
public interface Clickable extends Element {
    /**
     * Emits a click action. Returns the concrete subtype for potential 
     * future action-specific fluent methods while remaining polymorphic 
     * as an Action via covariant return typing.
     * 
     * Note: ClickAction IS an Action, so polymorphism is preserved.
     */
    default ClickAction click() {
        return new ClickAction(this);
    }
}
```

This is standard Java: `ClickAction extends ElementAction implements Action`, so returning `ClickAction` where `Action` is expected is type-safe.

### By Capability

**Clickable:**
```java
default ClickAction click() { return new ClickAction(this); }
```

**Checkable:**
```java
default CheckAction toggle() { return new CheckAction(this, CheckAction.Mode.TOGGLE); }
default CheckAction set(boolean desired) { return new CheckAction(this, CheckAction.Mode.SET, desired); }
```

**Typeable:**
```java
default TypeAction type(String text) { return new TypeAction(this, text); }
default ClearAction clear() { return new ClearAction(this); }
default AppendTypeAction append(String text) { return new AppendTypeAction(this, text); }
default TypeAndPressAction typeAndPress(String text, String key) { return new TypeAndPressAction(this, text, key); }
```

**Selectable:**
```java
default OpenAction open() { return new OpenAction(this); }
default SelectAction select() { return new SelectAction(this); }
default SelectByTextAction selectByText(String text) { return new SelectByTextAction(this, text); }
default SelectByValueAction selectByValue(String value) { return new SelectByValueAction(this, value); }
```

**Hoverable:**
```java
default HoverAction hover() { return new HoverAction(this); }
```

**Uploadable:**
```java
default UploadAction upload(String filePath) { return new UploadAction(this, filePath); }
```

... and others (SearchField, SearchableDropdown, EditableTable, Listable, MultiSelectable, etc.)

---

## Implementation

### File by Capability

| Capability | File | Methods to update |
|------------|------|------------------|
| Clickable | `src/main/java/elements/api/capability/Clickable.java` | click() |
| Checkable | `src/main/java/elements/api/capability/Checkable.java` | toggle(), set() |
| Hoverable | `src/main/java/elements/api/capability/Hoverable.java` | hover() |
| Typeable | `src/main/java/elements/api/capability/Typeable.java` | type(), clear(), append(), typeAndPress() |
| Selectable | `src/main/java/elements/api/capability/Selectable.java` | open(), select(), selectByText(), selectByValue() |
| SearchField | `src/main/java/elements/api/capability/SearchField.java` | search(), clearSearch() |
| SearchableDropdown | `src/main/java/elements/api/capability/SearchableDropdown.java` | searchAndSelect(), ... |
| Uploadable | `src/main/java/elements/api/capability/Uploadable.java` | upload() |
| EditableTable | `src/main/java/elements/api/capability/EditableTable.java` | editCell(), ... |
| Listable | `src/main/java/elements/api/capability/Listable.java` | getItem(), ... |
| MultiSelectable | `src/main/java/elements/api/capability/MultiSelectable.java` | selectMultiple(), ... |
| Table | `src/main/java/elements/api/capability/Table.java` | getCell(), ... |

### Steps

For each capability interface:

1. Replace all `ElementActions.of()` calls with `new {ActionType}(this, ...)`
2. Change return type from `Action` to concrete action type (e.g., `ClickAction`)
3. Update Javadoc to mention covariant return typing
4. Remove any import of `ElementActions` from that file

### Tests

**File:** Tests already exist for capability interfaces; update them.

**What changes:**
- Tests that verify return type can now be more specific:

**Before:**
```java
@Test
public void clickable_click_returnsAction() {
    Action action = clickable.click();
    assertNotNull(action);
}
```

**After:**
```java
@Test
public void clickable_click_returnsClickAction() {
    ClickAction action = clickable.click();
    assertNotNull(action);
    assertInstanceOf(ClickAction.class, action);
}
```

### Compilation Checkpoint

```bash
mvn -DskipTests compile
mvn test  # Full suite to catch polymorphism issues
```

---

## Backward Compatibility

**Polymorphism is preserved:** Code that expects `Action` still works because `ClickAction extends ElementAction implements Action`.

```java
// Still valid code
Action action = element.click();  // Click returns ClickAction, which IS an Action
action.safely();
```

New code can be more specific:

```java
// New possibility (action-specific fluent APIs in future)
ClickAction action = element.click();
// Could eventually add ClickAction-specific methods
```

---

## Affected Files

**Modify** (return types only, no behavior changes):
- `src/main/java/elements/api/capability/Clickable.java`
- `src/main/java/elements/api/capability/Checkable.java`
- `src/main/java/elements/api/capability/Hoverable.java`
- `src/main/java/elements/api/capability/Typeable.java`
- `src/main/java/elements/api/capability/Selectable.java`
- `src/main/java/elements/api/capability/SearchField.java`
- `src/main/java/elements/api/capability/SearchableDropdown.java`
- `src/main/java/elements/api/capability/Uploadable.java`
- `src/main/java/elements/api/capability/EditableTable.java`
- `src/main/java/elements/api/capability/Listable.java`
- `src/main/java/elements/api/capability/MultiSelectable.java`
- `src/main/java/elements/api/capability/Table.java`
- ... others as needed

---

## Exit Criteria

- [x] All capability methods return concrete action types
- [x] No ElementActions.of() calls remain in capability interfaces
- [x] All tests pass
- [x] Polymorphism is preserved (Action still accepts all action types)
- [x] Javadoc mentions covariant return typing for clarity
- [x] Demo code still compiles

---

## Next Phases

- Phase 16: Delete execution policy from capabilities
- Phase 17: Refactor central dispatch (remove switches)

