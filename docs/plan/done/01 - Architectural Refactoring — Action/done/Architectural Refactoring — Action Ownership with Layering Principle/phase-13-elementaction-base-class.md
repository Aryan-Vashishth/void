# Phase 13 — ElementAction Base Class

**Status:** Done (implemented as part of Phase 5 SoC correction, `feature/action-package-refactor`)  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — foundational class, isolated from existing code

> `ElementAction.java` was created during the Phase 5 SoC correction on
> `feature/action-package-refactor`. The class matches this plan's target design:
> Template Method pattern, final fluent APIs, protected overridable profile defaults,
> 3-arg constructor (element, role, capability). `ElementActionTest.java` also exists.
> This phase is complete. Phases 14+ continue from this state.

---

## Objective

Establish the `ElementAction` abstract base class that implements the Template Method pattern. This class owns the action lifecycle (perform → resolve → execute), fluent APIs (safely, debug, reliable, raw), and immutability guarantees. All concrete action subclasses (ClickAction, TypeAction, etc.) extend this base.

---

## Context

Currently, actions are created via `ElementActions.of()` factory with an anonymous lambda operation. The factory captures metadata (capability, profile) at construction time. This phase replaces that with a first-class base class that makes inheritance and behavior ownership explicit.

---

## Target Design

### ElementAction — Template Method Pattern

```java
public abstract class ElementAction implements Action {
    protected final Element element;
    protected final ActionCapability capability;
    
    // Template method — orchestrates lifecycle
    public final void perform(UIEngine engine) {
        LocatorDescriptor descriptor = resolve();
        execute(engine, descriptor);
    }
    
    // Primitive operation — subclasses implement behavior
    protected abstract void execute(UIEngine engine, LocatorDescriptor descriptor);
    
    // Shared resolution
    protected final LocatorDescriptor resolve() { ... }
    
    // Final fluent APIs
    public final Action safely() { return using(defaultSafeProfile()); }
    public final Action debug() { return using(defaultDebugProfile()); }
    public final Action reliable() { return using(defaultReliableProfile()); }
    public final Action raw() { return this; }
    
    // Overridable profile defaults
    protected ActionProfile defaultSafeProfile() { ... }
    protected ActionProfile defaultDebugProfile() { ... }
    protected ActionProfile defaultReliableProfile() { ... }
}
```

### Key Invariants

1. **Template Method:** `perform()` is final. It orchestrates: resolve → execute. No subclass override.
2. **Primitive operation:** `execute(UIEngine, LocatorDescriptor)` is abstract. Subclasses implement only this.
3. **Final fluent APIs:** `safely()`, `debug()`, `reliable()`, `raw()` are final. No subclass override.
4. **Profile defaults:** `defaultSafeProfile()`, `defaultDebugProfile()`, `defaultReliableProfile()` are protected and overridable. Subclasses override only when their behavior differs from the default.
5. **Immutability:** Every fluent call returns a new action instance (via HookChainAction wrapper). Never mutates `this`.

---

## Implementation

### File

`src/main/java/core/actions/ElementAction.java`

### Steps

1. **Create ElementAction skeleton** with Template Method structure and constructor.
2. **Implement resolve()** — obtain locator descriptor from element using role.
3. **Implement fluent APIs** — safely(), debug(), reliable(), raw().
4. **Add profile defaults** — defaultSafeProfile(), defaultDebugProfile(), defaultReliableProfile().
5. **Add immutability guarantees** — using() returns HookChainAction, not this.
6. **Write Javadoc** — explain Template Method, immutability, extension points.

### Tests

**File:** `src/test/java/core/actions/ElementActionTest.java`

**Key test cases:**

- Template method orchestration: perform() calls resolve() then execute()
- Finality: safely(), debug(), reliable(), raw() cannot be overridden
- Immutability: safely() / debug() / using() return different instances
- Profile defaults: subclasses inherit defaults correctly
- Capability capture: element's capability is stored at construction

### Compilation Checkpoint

```bash
mvn -DskipTests compile
mvn test -Dtest=ElementActionTest
```

---

## Affected Files

**Create:**
- `src/main/java/core/actions/ElementAction.java`
- `src/test/java/core/actions/ElementActionTest.java`

**No changes to existing files** — this is purely additive.

---

## Exit Criteria

- [ ] ElementAction compiles without errors
- [ ] Template Method pattern is correct (perform → resolve → execute)
- [ ] All lifecycle methods are final
- [ ] Immutability is guaranteed (using() returns new instance)
- [ ] All unit examples pass
- [ ] No changes to existing Action interface

---

## Next Phase

Phase 14 — Concrete Action Subclasses (ClickAction, TypeAction, SelectAction, etc.)

