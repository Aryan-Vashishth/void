## Plan: Architectural Refactoring — Action Ownership with Layering Principle (Final v2)

**TL;DR:** Implement a clean architectural foundation where capabilities describe targets, actions encapsulate execution behavior, and execution infrastructure never leaks into capability interfaces. Use Template Method pattern with final lifecycle methods, immutable action instances, and concrete return types. Establish foundational principles as ADRs that will guide all future architectural decisions.

---

## Foundational Principles (The Heart of This Redesign)

These principles guide every decision. Document them as ADRs—they are not refactoring tactics, they are architectural law.

1. **Capabilities describe targets. Actions describe execution.**
2. **Metadata never drives behavior.** (`ActionCapability` is for tracing, never for dispatch)
3. **Actions encapsulate execution behavior.** They never query their own capability to decide how to execute.
4. **Execution concerns flow downward** through the pipeline (Action → ExecutionPipeline → Engine), never upward into capabilities.
5. **Extension happens by introducing new action types**, not by modifying central dispatch logic.

---

## Phase 0: Establish Foundational Principles (ADRs)

### ADR-013: Architectural Layering Principle

**File:** `docs/decisions/accepted/013-architectural-layering-principle.md`

```markdown
Status: Foundational Architecture Decision

Capabilities describe the structural properties and target of elements.
Actions describe the execution intent and policies applied to those targets.
Execution infrastructure (hooks, retries, timeouts, profiles, tracing) 
belongs to actions and execution pipelines, never to capability interfaces.

This layering keeps concerns separated and allows multiple actions to emit 
from the same capability with independent execution policies.

### Derived rules

- Metadata ≠ behavior: ActionCapability is read-only metadata for logging/tracing, never for behavioral dispatch.
- An action never queries its own capability to decide how to execute. The action already knows what it is.
- Extension happens by introducing new action types (DoubleClickAction, RightClickAction), not by modifying central dispatch.
```

### ADR-014: Concrete Actions over Anonymous Lambdas (Status: Future Decision)

**File:** `docs/decisions/accepted/014-concrete-actions-over-lambdas.md`

```markdown
Status: Accepted (will be created after Phase 2 is complete)

Originally, actions were anonymous lambdas created by ElementActions.of().
The framework is evolving to first-class action types (ClickAction, TypeAction, etc.).

This change makes actions explicit, testable, and able to own their execution policies.
It consolidates behavior in one place rather than spreading it across factories and dispatchers.
```

---

## Phase 1: Foundation — ElementAction Base Class

**Create:** `src/main/java/core/actions/ElementAction.java`

```java
public abstract class ElementAction implements Action {
    protected final Element element;
    protected final ActionCapability capability;
    // tracing, hook infrastructure owned here
    
    // ── Template Method ──────────────────────────────────────────
    
    /** Template method — orchestrates action lifecycle. FINAL. */
    public final void perform(UIEngine engine) {
        LocatorDescriptor descriptor = resolve();
        // before hooks
        execute(engine, descriptor);
        // after hooks
    }
    
    /** Primitive operation — subclasses implement unit of behavior. */
    protected abstract void execute(UIEngine engine, LocatorDescriptor descriptor);
    
    /** Shared locator resolution. FINAL. */
    protected final LocatorDescriptor resolve() {
        // resolution logic here
    }
    
    // ── Fluent APIs — all FINAL ──────────────────────────────────
    
    public final Action safely() {
        return using(defaultSafeProfile());
    }
    
    public final Action debug() {
        return using(defaultDebugProfile());
    }
    
    public final Action reliable() {
        return using(defaultReliableProfile());
    }
    
    public final Action raw() {
        return this;  // no hooks
    }
    
    // ── Profile defaults — overridable by subclasses ──────────────
    
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.DEFAULT_SAFE;
    }
    
    protected ActionProfile defaultDebugProfile() {
        return ActionProfiles.DEFAULT_DEBUG;
    }
    
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.DEFAULT_RELIABLE;
    }
    
    // ── Immutability rule ────────────────────────────────────────
    
    /** Every fluent method returns a new action. Never mutates this. */
    @Override
    public final Action using(ActionProfile profile) {
        // returns new HookChainAction, not this
    }
}
```

**Key rules:**
- `perform()`, `resolve()`, `safely()`, `debug()`, `reliable()`, `raw()` are final. No subclass override.
- Only extension points: `execute()`, `defaultSafeProfile()`, `defaultDebugProfile()`, `defaultReliableProfile()`
- Immutability: Every fluent call returns a new action instance.
- Immutability rule in Javadoc: "This action is immutable. All fluent methods return new instances."

---

## Phase 2: Concrete Action Subclasses

Create one subclass per action type. Example: `ClickAction`, `TypeAction`, `SelectAction`, etc.

Each implements:
- `protected void execute(UIEngine engine, LocatorDescriptor descriptor)` — the unit of behavior
- `protected ActionProfile defaultSafeProfile()` (override only if different from ElementAction default)
- `protected ActionProfile defaultDebugProfile()` (override only if different)
- `protected ActionProfile defaultReliableProfile()` (override only if different)

**Profile constants are owned by the subclass:**

```java
public class ClickAction extends ElementAction {
    
    static final ActionProfile CLICK_ACTION_SAFE_PROFILE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();
    
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

**No public API methods in subclasses.** All fluent APIs are inherited and final.

---

## Phase 3: Refactor Capabilities to Emit Concrete Actions

Update every capability method to return the concrete action type (covariant return):

```java
// Clickable.java
public interface Clickable extends Element {
    
    /** Returns the trigger locator for clicking this element. */
    String getTriggerLocator();
    
    // ... other locators ...
    
    /**
     * Emits a click action. Returns the concrete subtype for potential 
     * future action-specific fluent methods while remaining polymorphic 
     * as an Action.
     * 
     * (Note: Uses covariant return typing—ClickAction is an Action.)
     */
    default ClickAction click() {
        return new ClickAction(this);
    }
}

// Typeable.java
public interface Typeable extends Element {
    
    default TypeAction type(String text) {
        return new TypeAction(this, text);
    }
    
    default TypeAction clear() {
        return new ClearAction(this);
    }
    
    default TypeAction append(String text) {
        return new AppendTypeAction(this, text);
    }
}

// Selectable.java
public interface Selectable extends Element {
    
    default SelectAction select() {
        return new SelectAction(this, SelectAction.Mode.TRIGGER);
    }
    
    default SelectAction selectByText(String text) {
        return new SelectByTextAction(this, text);
    }
}
```

**Key: No ElementActions.of() factory calls. Action encapsulates its execution behavior.**

---

## Phase 4: Delete Execution Policy from Capabilities

1. **From `ActionCapabilityProvider`:** Delete only the profile methods:
   - `safeProfile()`
   - `reliableProfile()`
   - `fastProfile()`
   - `debugProfile()`
   
   Keep the interface with only `ActionCapability capability()` — this is a clean metadata contract.

2. **From all capability interfaces:** Delete profile constant fields:
   - `CLICKABLE_SAFE_PROFILE`
   - `TYPEABLE_SAFE_PROFILE`
   - `SELECTABLE_SAFE_PROFILE`
   - etc.

3. **Status of ActionCapabilityProvider:** Keep it. It's now metadata-only and clean.

---

## Phase 5: Refactor Central Dispatch

1. **Delete switch statements** from `Profiles.SAFE`, `Profiles.RELIABLE`, `Profiles.FAST`, `Profiles.DEBUG`:
   ```java
   // BEFORE (delete this)
   public static final ActionProfile SAFE = new ActionProfile() {
       @Override
       public List<BeforeActionHandler> before(Action action) {
           return switch (action.capability()) {  // ← DELETE THIS
               case CLICKABLE -> ...;
               case TYPEABLE -> ...;
               // ...
           };
       }
   };
   
   // AFTER (keep only data structures)
   public static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
           .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
           .build();
   ```

2. **Keep Profiles class** as a reusable data container for default profiles and `ActionProfile.builder()`. Don't deprecate it—it's now pure data.

3. **Profile resolution is 100% polymorphic** through `ElementAction.defaultSafeProfile()` overrides in subclasses.

---

## Phase 6: Audit ElementRole for Necessity

**Do NOT remove ElementRole yet.** Investigate first.

For each action type, determine: Does the action always target the same locator role?

- ClickAction → always `getTriggerLocator()` ✓
- TypeAction → typically `getInputLocator()` ✓
- SelectAction → **may need both** (trigger to open, list to select) ⚠️
- HoverAction → use `getTriggerLocator()` ✓

**Check edge cases:** Autocomplete, complex composites, etc.

**Decision outcome:** 
- If all actions require no role parameter: Remove ElementRole from public contracts (keep internally if helpful).
- If any action needs role selection: Keep ElementRole as action constructor parameter or locator strategy enum.
- Document findings and decision in a brief architecture note.

---

## Phase 7: ElementActions Factory Role (Decision Deferred)

**Create ADR-015 AFTER implementation:** `docs/decisions/accepted/015-elementactions-factory-scope.md`

At that point, investigate:
- Are there any remaining call sites of `ElementActions.of()`?
- If yes: Keep as deprecated bridge with 2-release removal window.
- If no: Mark for deletion.
- If only tests: Move to test-support utilities.

**Document the decision with evidence.**

---

## Phase 8: Update Documentation

1. **Update `docs/architecture/core-packages.md`:** Replace all ElementActions.of() examples with direct action construction.

2. **Update `docs/architecture/system-overview.md`:**
   ```
   Element
       ↓ (Capability)
       ↓
   Concrete Action (ClickAction, TypeAction, SelectAction, ...)
       ↓ (Fluent: safely(), debug(), using())
       ↓
   ExecutionPipeline (retry, timeout, tracing)
       ↓ (perform)
       ↓
   UIEngine
   ```

3. **Add to architecture guide:**
   ```
   Foundational Principles (ADR-013, ADR-014):
   - Capabilities describe targets.
   - Actions describe execution.
   - Metadata ≠ behavior.
   - Actions encapsulate behavior.
   - Extension via new action types, not dispatch.
   ```

4. **Update covariant return type explanation:** Document that returning concrete `ClickAction` instead of generic `Action` is standard Java covariant return typing, not a framework feature.

5. **Update test examples** and demo code to use direct action construction.

---

## Implementation Sequence

| # | Phase | Description | Owner |
|---|-------|-------------|-------|
| 0 | Create ADR-013, ADR-014 | Establish foundational principles | Documentation |
| 1 | Create ElementAction base class | Template Method, final methods | Core |
| 2 | Create concrete action subclasses | ClickAction, TypeAction, etc. (parallel) | Core |
| 3 | Refactor capabilities | Return action types | Capabilities |
| 4 | Delete execution policy | Remove profile methods, keep interface | Core |
| 5 | Delete central dispatch | Remove switches, keep data | Core |
| 6 | Audit ElementRole | Investigate, document decision | Investigation |
| 7 | Document ElementActions decision | Create ADR-015 (deferred) | Documentation |
| 8 | Update architecture docs | Align all references | Documentation |

---

## Architectural Invariants

These are non-negotiable design principles that guide all future decisions:

1. **Layering Principle (ADR-013):** Capabilities describe targets. Actions describe execution. Execution infrastructure never leaks into capability interfaces.

2. **Metadata ≠ behavior:** `ActionCapability` is read-only metadata for logging/tracing/diagnostics. Never use it for behavioral dispatch.

3. **Self-aware execution:** An action never queries its own capability to decide how to execute. The action already knows what it is.

4. **Immutable actions:** Action instances are immutable. Fluent calls return new instances.

5. **Final lifecycle:** `perform()`, `resolve()`, `safely()`, `debug()`, `reliable()`, `raw()` are final in `ElementAction`. Subclasses only implement `execute()` and profile defaults.

6. **Covariant returns:** Capability methods return concrete action types (e.g., `ClickAction`), enabling action-specific extension while maintaining `Action` polymorphism.

7. **Extension via action types:** New behavior is added by introducing new `Action` subclasses, not by modifying central dispatch logic or adding branches.

---

## Why This Architecture Works

**Problem it solves:**
- Original: Capabilities knew about execution (profiles, hooks). Tightly coupled.
- Problem: Adding new action types required modifying capability interfaces and central dispatch.
- Problem: Metadata (capability enum) was used for behavioral dispatch (the switch).
- Problem: Execution infrastructure leaked into multiple layers.

**New approach:**
- Capabilities are thin: describe structural properties only.
- Actions own execution: profiles, hooks, tracing, defaults all belong here.
- Polymorphism instead of dispatch: add DoubleClickAction, not another switch case.
- Clear responsibility: if it executes, it goes on Action. If it describes, it goes on Capability.

**Evolution:**
```
Capability → Factory → Profiles → Switch → Hooks
       ↓ (coupled, indirect, entropy grows)
       
Element → Capability → Concrete Action → Pipeline → Engine
       ↓ (layered, direct, principles guide growth)
```

**Result:**
- ✓ Easier to explain (one principle: capability describes, action executes)
- ✓ Easier to extend (new action type, not new switch case)
- ✓ Easier to test (action is first-class, not generated by factory)
- ✓ Easier to trace (action owns its metadata and behavior)
- ✓ Easier to maintain (responsibilities don't shift between layers)

---

## Readiness for Implementation

This architecture design is mature and ready for Phase 1 implementation. The foundational principles are established (ADR-013, ADR-014), the dependency flow is clear (Element → Capability → Concrete Action → Pipeline → Engine), and the extension points are well-defined (new action subclasses, not modifications to existing layers). 

First implementation will reveal edge cases (probably around ElementRole, locator resolution, or subtle inheritance hierarchy tuning) that require adjustment. That is normal and healthy. Review pull requests against the architectural invariants established above; implementation feedback will be more valuable than further redesign.

See `implementation-plan-actionOwnership.md` for step-by-step implementation guidance, testing strategy, and migration notes.

