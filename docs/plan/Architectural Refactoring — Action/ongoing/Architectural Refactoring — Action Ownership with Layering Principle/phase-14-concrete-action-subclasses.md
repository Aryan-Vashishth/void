# Phase 14 — Concrete Action Subclasses

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — additive only, each subclass is independent  
**Depends on:** Phase 13 (ElementAction base class) — Done  
**Parallel with:** Phase 15 (capability refactoring)

---

## Objective

Create concrete action subclasses for every action type currently emitted by capability interfaces. Each subclass implements `execute()` and optionally overrides profile defaults. Profile constants move from capabilities to actions.

---

## Context

Previously, all actions were anonymous lambdas constructed by `ElementActions.of()`. The framework is evolving to first-class action types. This makes actions explicit, testable, and able to own their execution policies independently.

---

## Target Design

### What Gets Created

One subclass per action emission point:

- `ClickAction` (Clickable.click())
- `CheckAction` (Checkable.toggle(), set())
- `HoverAction` (Hoverable.hover())
- `TypeAction` (Typeable.type())
- `ClearAction` (Typeable.clear())
- `AppendTypeAction` (Typeable.append())
- `TypeAndPressAction` (Typeable.typeAndPress())
- `SelectAction` (Selectable.select())
- `SelectByTextAction` (Selectable.selectByText())
- `SelectByValueAction` (Selectable.selectByValue())
- `OpenAction` (Selectable.open())
- `UploadAction` (Uploadable.upload())
- `SearchAction` and variants (SearchField, SearchableDropdown)
- `EditTableCellAction` (EditableTable variants)
- etc.

### Template for Each Subclass

```java
public final class ClickAction extends ElementAction {
    
    static final ActionProfile CLICK_ACTION_SAFE_PROFILE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();
    
    public ClickAction(Clickable element) {
        super(element);
    }
    
    @Override
    protected ActionProfile defaultSafeProfile() {
        return CLICK_ACTION_SAFE_PROFILE;
    }
    
    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
```

**Key rules:**
- Class is `final` — no further subclassing.
- Constructor is `public` — capabilities instantiate directly.
- Only overrides `execute()` and profile defaults that differ from ElementAction base.
- Profile constants are `static final` fields on the class.

---

## Implementation

### File Layout

All action classes in: `src/main/java/core/actions/`

### Steps

1. Create each subclass following the template above
2. Implement `execute()` to call the correct `UIEngine` method
3. Define profile constants if behavior differs from default
4. Override profile defaults only when necessary (rule: no boilerplate)
5. Add Javadoc explaining the action's intent

### Tests

**File:** `src/test/java/core/actions/{ActionType}ActionTest.java`

**Key test cases per action:**

- execute() calls correct UIEngine method with descriptor
- Profile defaults are correct (compare to existing phase expectations)
- Immutability: safely() returns new instance
- Capability is populated correctly

Example:

```java
@Test
public void clickAction_execute_callsEngineClick() {
    UIEngine engine = mock(UIEngine.class);
    ClickAction action = new ClickAction(clickableElement);
    action.execute(engine, descriptor);
    verify(engine).click(descriptor);
}

@Test
public void clickAction_safely_returnsNewInstance() {
    ClickAction action = new ClickAction(clickableElement);
    Action safe = action.safely();
    assertNotSame(safe, action);
}
```

### Compilation Checkpoint

```bash
mvn -DskipTests compile
mvn test -Dtest=.*ActionTest  # All action tests
```

---

## Profile Constants Migration

Profile constants move from capability interfaces to action classes:

**Before (Clickable.java):**
```java
ActionProfile CLICKABLE_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
    .build();
```

**After (ClickAction.java):**
```java
static final ActionProfile CLICK_ACTION_SAFE_PROFILE = ActionProfile.builder()
    .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
    .build();
```

No behavior changes — only ownership changes.

---

## Decision: Action Hierarchy vs. Composition

**Considered:** Could each action be a parameter-based variant rather than a separate class?

**Decision:** Use separate classes (ClickAction, TypeAction, etc.).

**Rationale:**
- Explicit type makes intent clear
- Easier to test in isolation
- Template Method pattern works best with inheritance
- Future action-specific fluent methods are simpler

---

## Affected Files

**Create (parallel to tests):**
- `src/main/java/core/actions/ClickAction.java`
- `src/main/java/core/actions/TypeAction.java`
- `src/main/java/core/actions/ClearAction.java`
- `src/main/java/core/actions/AppendTypeAction.java`
- `src/main/java/core/actions/TypeAndPressAction.java`
- `src/main/java/core/actions/CheckAction.java`
- `src/main/java/core/actions/HoverAction.java`
- `src/main/java/core/actions/SelectAction.java`
- `src/main/java/core/actions/SelectByTextAction.java`
- `src/main/java/core/actions/SelectByValueAction.java`
- `src/main/java/core/actions/OpenAction.java`
- `src/main/java/core/actions/UploadAction.java`
- ... and capability-specific variants

---

## Exit Criteria

- [ ] All action subclasses compile without errors
- [ ] Each action implements execute() correctly
- [ ] Profile constants are owned by actions, not capabilities
- [ ] All unit tests pass
- [ ] No public methods in subclasses (only inherited fluent APIs)
- [ ] No changes to existing Action interface

---

## Next Phases

- Phase 15 (parallel): Refactor capabilities to emit concrete actions
- Phase 16: Delete execution policy from capabilities

