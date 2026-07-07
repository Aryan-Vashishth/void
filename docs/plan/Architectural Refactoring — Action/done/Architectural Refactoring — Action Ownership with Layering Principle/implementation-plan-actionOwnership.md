## Implementation Plan: Action Ownership with Layering Principle

**Reference:** `plan-actionOwnershipLayering.prompt.md` (architecture design)  
**Active branch:** `feature/action-package-refactor`

This document provides step-by-step implementation guidance, testing strategy, and migration notes for executing the architectural refactoring.

---

## Progress Summary (as of 2026-07-06)

| Phase | Title | Status |
|-------|-------|--------|
| 13 (Plan Phase 1) | ElementAction Base Class | ✅ Done |
| 14 (Plan Phase 2) | Concrete Action Subclasses | ✅ Done |
| 15 (Plan Phase 3) | Capability Refactoring (covariant returns) | ✅ Done |
| 16 (Plan Phase 4) | Delete Execution Policy from Capabilities | ✅ Done |
| 17 (Plan Phase 5) | Eliminate Capability-Based Profile Dispatch | 🔄 In Progress |
| 18 (Plan Phase 6) | Audit ElementRole for Necessity | ⏳ Pending |
| 19 (Plan Phase 7) | ElementActions Factory Decision | ⏳ Pending |
| 20 (Plan Phase 8) | Update Documentation | ⏳ Pending |

---

## Pre-Implementation Checklist

- [x] Both ADR-013 and ADR-014 are created and reviewed
- [x] Team understands the five foundational principles
- [x] Feature branch `feature/action-package-refactor` is created from current `main`
- [x] Local environment compiles on current `main` with no errors

---

## Phase 1: ElementAction Base Class

### Step 1.1: Create ElementAction skeleton

**File:** `src/main/java/core/actions/ElementAction.java`

Start minimal to establish Template Method pattern:

```java
public abstract class ElementAction implements Action {
    protected final Element element;
    protected final ActionCapability capability;
    
    protected ElementAction(Element element) {
        this.element = Objects.requireNonNull(element);
        this.capability = element instanceof ActionCapabilityProvider p 
            ? p.capability() 
            : ActionCapability.UNKNOWN;
    }
    
    @Override
    public final void perform(UIEngine engine) {
        LocatorDescriptor descriptor = resolve();
        execute(engine, descriptor);
    }
    
    protected abstract void execute(UIEngine engine, LocatorDescriptor descriptor);
    
    protected final LocatorDescriptor resolve() {
        // implementation deferred to Phase 1.2
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**Compile check:** `mvn -DskipTests compile`

### Step 1.2: Implement resolve() and fluent APIs

Add to ElementAction:

```java
protected final LocatorDescriptor resolve() {
    // Use role to determine which locator to resolve
    // This will be refined in Phase 6 audit
    return engine.resolve(element, role);
}

@Override
public final Action safely() {
    return using(defaultSafeProfile());
}

@Override
public final Action debug() {
    return using(defaultDebugProfile());
}

@Override
public final Action reliable() {
    return using(defaultReliableProfile());
}

@Override
public final Action raw() {
    return this;
}

protected ActionProfile defaultSafeProfile() {
    return ActionProfiles.DEFAULT_SAFE;
}

protected ActionProfile defaultDebugProfile() {
    return ActionProfiles.DEFAULT_DEBUG;
}

protected ActionProfile defaultReliableProfile() {
    return ActionProfiles.DEFAULT_RELIABLE;
}

@Override
public final Action using(ActionProfile profile) {
    // Delegate to HookChainAction, maintaining immutability
    Objects.requireNonNull(profile);
    List<BeforeActionHandler> before = profile.before(this);
    List<AfterActionHandler> after = profile.after(this);
    if ((before == null || before.isEmpty()) && (after == null || after.isEmpty())) {
        return this;
    }
    return new HookChainAction(this, before, after);
}
```

**Compile check:** `mvn -DskipTests compile`

### Step 1.3: Add immutability documentation

Update class Javadoc:

```java
/**
 * Template Method base class for all concrete action types.
 * 
 * <h3>Immutability</h3>
 * <p>ElementAction instances are immutable. Every fluent call 
 * ({@code safely()}, {@code debug()}, {@code using(...)}) returns a new action 
 * instance rather than mutating this one. Subclasses must not override 
 * immutability guarantees.</p>
 * 
 * <h3>Extension</h3>
 * <p>Subclasses implement only {@link #execute} and optionally override 
 * profile defaults ({@code defaultSafeProfile()}, etc.). 
 * All lifecycle methods are final.</p>
 */
public abstract class ElementAction implements Action {
    // ...
}
```

### Step 1.4: Write unit tests for ElementAction

**File:** `src/test/java/core/actions/ElementActionTest.java`

Key tests:
- Template method orchestration (perform → resolve → execute)
- Final methods can't be overridden
- Immutability: safely() / debug() / using() return new instances
- Profile default delegation

```java
@Test
public void elementAction_safely_returnsDifferentInstance() {
    ElementAction action = new TestClickAction(element);
    Action safe = action.safely();
    assertNotSame(safe, action, "safely() must return new instance");
}

@Test
public void elementAction_perform_callsExecuteWithDescriptor() {
    AtomicReference<LocatorDescriptor> received = new AtomicReference<>();
    ElementAction action = new ElementAction(element) {
        @Override
        protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
            received.set(descriptor);
        }
    };
    action.perform(engine);
    assertNotNull(received.get(), "execute() must receive descriptor");
}
```

**Compile & test:** `mvn test -Dtest=ElementActionTest`

---

## Phase 2: Concrete Action Subclasses (Parallel)

### Step 2.1: Create ClickAction

**File:** `src/main/java/core/actions/ClickAction.java`

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

### Step 2.2: Create TypeAction, ClearAction, AppendTypeAction

Follow same pattern as ClickAction.

### Step 2.3: Create SelectAction and variant actions

SelectAction may need ElementRole parameter for Phase 6 audit. Start without it; add if needed during audit.

```java
public final class SelectAction extends ElementAction {
    // May need to accept mode parameter
    public SelectAction(Selectable element) {
        super(element);
    }
    
    // ...
}
```

### Step 2.4: Test action subclasses

**File:** `src/test/java/core/actions/ClickActionTest.java` (and others)

Key tests:
- Action type performs correct engine operation
- Profile defaults are correct
- Immutability works through inheritance chain

```java
@Test
public void clickAction_execute_callsEngineClick() {
    AtomicReference<LocatorDescriptor> received = new AtomicReference<>();
    UIEngine mockEngine = mock(UIEngine.class);
    doAnswer(inv -> { received.set(inv.getArgument(0)); return null; })
        .when(mockEngine).click(any());
    
    new ClickAction(clickable).execute(mockEngine, descriptor);
    assertSame(received.get(), descriptor);
    verify(mockEngine).click(descriptor);
}
```

---

## Phase 3: Refactor Capabilities (Alongside Phase 2)

### Step 3.1: Update Clickable

**File:** `src/main/java/elements/api/capability/Clickable.java`

```java
// ...existing code...

/**
 * Emits a click action. Returns the concrete action type to enable 
 * potential future action-specific fluent APIs, while remaining polymorphic 
 * as an Action via covariant return typing.
 * 
 * @return a new ClickAction targeting this element
 */
default ClickAction click() {
    return new ClickAction(this);
}
```

Remove ElementActions.of() call and any profile constants.

### Step 3.2: Update Typeable, Selectable, Checkable, etc.

Same pattern: return concrete action types.

### Step 3.3: Test capability methods

Write tests that verify capability methods return correct action types:

```java
@Test
public void clickable_click_returnsClickAction() {
    Clickable el = stubClickable();
    Action action = el.click();
    assertInstanceOf(ClickAction.class, action);
}
```

---

## Phase 4: Delete Execution Policy from Capabilities

### Step 4.1: Remove profile methods from ActionCapabilityProvider

Delete:
- `safeProfile()`
- `reliableProfile()`
- `fastProfile()`
- `debugProfile()`

Keep:
- `capability()`

**File:** `src/main/java/core/actions/ActionCapabilityProvider.java`

### Step 4.2: Remove profile constants from capability interfaces

Delete from Clickable, Typeable, Selectable, SearchField, SearchableDropdown:
- `*_SAFE_PROFILE`
- `*_RELIABLE_PROFILE`
- etc.

### Step 4.3: Update tests

Remove tests that verify capability-owned profiles. Profile tests now verify ElementAction subclasses own profiles.

---

## Phase 5: Delete Central Dispatch

### Step 5.1: Refactor Profiles class

Delete switch statements from SAFE, RELIABLE, FAST, DEBUG profiles.

Keep as data-only:

```java
public final class Profiles {
    
    public static final ActionProfile RAW = new ActionProfile() {
        @Override public String name() { return "RAW"; }
    };
    
    public static final ActionProfile DEBUG = new ActionProfile() {
        @Override public String name() { return "DEBUG"; }
        @Override
        public List<BeforeActionHandler> before() {
            return List.of(Before.LOG_INTENT, Before.HIGHLIGHT_ELEMENT);
        }
        @Override
        public List<AfterActionHandler> after() {
            return List.of(After.HIGHLIGHT_ELEMENT);
        }
    };
    
    // Remove switches entirely
}
```

### Step 5.2: Update tests

Tests on `Profiles.SAFE.before(action)` no longer make sense. Delete them. Profile behavior is now tested through action subclasses.

### Step 5.3: Compile & test

`mvn -DskipTests compile && mvn test`

---

## Phase 6: Audit ElementRole for Necessity

### Step 6.1: Analyze each action type

For each action (ClickAction, TypeAction, SelectAction, HoverAction, etc.), ask:

- Does this action **always** target the same locator?
- Are there edge cases (Autocomplete, complex composites)?
- Can the role be inferred from the action type?

```
ClickAction       always getTriggerLocator()      ✓ no role needed
TypeAction        always getInputLocator()        ✓ no role needed
SelectAction      may use both (trigger + list)   ? might need role
HoverAction       always getTriggerLocator()      ✓ no role needed
```

### Step 6.2: Document findings

Create brief note in implementation PR comment or as an TODO in code.

### Step 6.3: Decision

- **If no action needs ElementRole:** Remove from public Element/Capability interfaces; keep internal if useful.
- **If SelectAction or others need it:** Add ElementRole as action constructor parameter or strategy enum.

Document decision in Phase 7 (ElementActions) ADR or separate architecture note.

---

## Phase 7: ElementActions Factory (Decision Deferred)

After all above phases complete:

### Step 7.1: Audit ElementActions.of() usage

Search codebase for all calls to `ElementActions.of()`:

```bash
grep -r "ElementActions\.of\(" src/
```

Count by context:
- Test files: ?
- Demo code: ?
- Implementation files: ?
- Disabled/dead code: ?

### Step 7.2: Document findings

Create brief summary in PR comments:

- X calls in tests
- Y calls in implementation
- Z calls in demo

### Step 7.3: Create ADR-015

`docs/decisions/accepted/015-elementactions-factory-scope.md`

Based on evidence, decide:
- **Option A:** Delete entirely (if no calls found)
- **Option B:** Keep as internal utility (if implementation files use it)
- **Option C:** Move to test-support utilities (if only tests use it)

Document chosen option with rationale.

---

## Phase 8: Update Documentation

### Step 8.1: Update core architecture docs

Edit `docs/architecture/core-packages.md` and `docs/architecture/system-overview.md`:

Replace all ElementActions.of() examples with direct action construction.

Show new dependency flow:
```
Element → Capability → Concrete Action → Pipeline → Engine
```

### Step 8.2: Add ADR references

In `docs/architecture/` guide, reference ADR-013 and ADR-014 early and often.

### Step 8.3: Document covariant return types

Add note explaining that returning ClickAction instead of Action is standard Java covariant return typing.

### Step 8.4: Update example code

Demo and test code should use new pattern exclusively.

---

## Testing Strategy Throughout Implementation

### Unit tests
- ElementAction base class (template method, finality, immutability)
- Each action subclass (execute() behavior, profile defaults)
- Capability methods (return correct action types)

### Integration tests
- Action through hook pipeline (before/after/using)
- Action through ExecutionPipeline (if Phase 5 already exists)
- Profile resolution end-to-end

### Regression tests
- All existing action behavior preserved
- Demo code still compiles and runs
- Hook behavior unchanged

### Audit tests
- ElementRole: verify every action can resolve its locator
- ElementActions.of(): catalog all remaining call sites

---

## Compilation Checkpoints

After each phase:

```bash
# Quick compile check
mvn -DskipTests compile

# Full test suite
mvn test

# Specific test class
mvn test -Dtest=ElementActionTest

# Demo compilation
javac src/main/java/tests/demo/VoidDemo.java
```

Never push without: `mvn -DskipTests compile` passing on both `main` and feature branch.

---

## Common Pitfalls to Avoid

1. **Don't make profile methods public in subclasses** — keep as protected `defaultSafeProfile()` only. Public API stays in ElementAction.

2. **Don't duplicate Template Method logic** — every action subclass implements ONLY `execute()`. Never override `perform()` or `resolve()`.

3. **Don't mutate action instances** — every fluent call must return a new instance via HookChainAction.

4. **Don't use capability for dispatch** — if you write a switch statement on `capability()` inside an action, you're violating the architecture.

5. **Don't forget immutability tests** — verify that `action.safely()` returns a different object.

---

## Rollback Plan

If issues arise that can't be fixed within scope:

1. Keep feature branch; push to origin
2. Document blockers in PR comments
3. Create separate issue for follow-up
4. Mark ADRs as "In Discussion" rather than "Accepted"
5. Do NOT merge incomplete implementation to main

---

## Success Criteria

Implementation is complete when:

- [ ] All 8 phases are complete
- [ ] ElementAction base class compiles and tests pass
- [ ] All capability methods return concrete action types
- [ ] All action subclasses implement execute() correctly
- [ ] ActionCapabilityProvider contains only capability() method
- [ ] Profile dispatch is 100% polymorphic (no switches)
- [ ] ElementRole usage audited and documented
- [ ] ElementActions decision documented in ADR-015
- [ ] Architecture docs updated
- [ ] Demo code compiles and runs
- [ ] All tests pass: `mvn test`
- [ ] No compile errors: `mvn -DskipTests compile`
- [ ] Feature branch ready for review against ADR-013 and ADR-014

