# Phase 19 — ElementActions Factory Role (Deferred Decision)

**Status:** Done  
**Architecture Version:** 2.4  
**Branch:** `feature/action-package-refactor`  
**Risk:** Low — investigation only, decision deferred  
**Depends on:** Phases 13–17 complete (Phase 18 can run in parallel)

---

## Objective

Audit all remaining call sites of `ElementActions.of()` in the codebase. Determine whether the factory should be: (A) deleted, (B) kept as internal utility, or (C) moved to test-support package only. Document the decision in ADR-012.

---

## Context

`ElementActions.of()` was the primary action creation mechanism:

```java
// OLD
default Action click() {
    return ElementActions.of(this, ElementRole.TRIGGER, (engine, d) -> engine.click(d));
}
```

With concrete action subclasses, capability methods now create actions directly:

```java
// NEW
default ClickAction click() {
    return new ClickAction(this);
}
```

But `ElementActions.of()` may still exist in:
- Test support utilities
- Legacy code paths
- Demo examples
- Internal utilities

This phase surveys what remains.

---

## Investigation

### Step 1: Search All Call Sites

```bash
grep -r "ElementActions\.of(" src/main/java/
grep -r "ElementActions\.of(" src/test/java/
grep -r "ElementActions\.of(" docs/
```

Categorize by context:

```
src/main/java/               □ count
src/test/java/               □ count
docs/                        □ count
demo/                        □ count
Total:                       □
```

### Step 2: Analyze Each Call Site

For each match, ask:

1. **Is this reachable code?** (Not dead/commented out?)
2. **What does it do?** (What capability, what role, what operation?)
3. **Can it be replaced by concrete action constructor?** (e.g., `new ClickAction(element)`)
4. **Is there a reason it can't be?** (Unusual use case?)

Example analysis:

```
File: src/test/java/core/actions/ElementActionsSafeProfileTest.java:107
Code: ElementActions.of(stubClickable(), ElementRole.TRIGGER, (e, d) -> {})
Type: Test utility creating a stub action
Can replace: YES → new ClickAction(stubClickable())
Reason: Straightforward

File: src/test/java/core/actions/HookPipelineTest.java:220
Code: ElementActions.of(element, role, (engine, d) -> receivedDescriptor.set(d))
Type: Test capturing a descriptor
Can replace: Partially — needs test utility that accepts custom operation
Reason: Test infrastructure may need a factory for custom behavior
```

### Step 3: Categorize Results

```markdown
## ElementActions.of() Call Sites

### Test infrastructure (replace with test utility factory)
- Count: X
- Examples: hook examples, descriptor capture

### Implementation code (replace with direct constructors)
- Count: Y
- Examples: capability methods (already done in Phase 15)

### Demo/docs (replace with direct constructors)
- Count: Z
- Examples: examples, README

### Dead code (delete)
- Count: N
- Examples: commented out, unreachable

### Unclear / needs refactoring
- Count: M
- Description: ...
```

### Step 4: Propose Three Options

**Option A: Delete entirely**

If all call sites can be replaced with direct constructors or test utilities.

**Pros:**
- Simpler codebase
- No factory abstraction
- Capabilities create actions directly

**Cons:**
- Test support may need custom action factory for edge cases

**Option B: Keep as internal utility**

If core.actions needs a helper for test infrastructure or internal use cases.

**Pros:**
- Maintains test infrastructure patterns
- Minimal surface area (not part of public API)

**Cons:**
- One more class to maintain
- Adds abstraction layer

**Option C: Move to test-support package**

If ElementActions is used only for testing.

**Pros:**
- Clear separation (only examples use it)
- Easier to remove later

**Cons:**
- Test-only class in main source (usually avoided)

---

## Implementation (Post-Audit)

### If Decision is "Delete"

1. Verify no remaining call sites
2. Delete `src/main/java/core/actions/ElementActions.java`
3. Create test factory in test-support if needed:
   ```java
   // src/test/java/core/actions/TestActionFactory.java
   class TestActionFactory {
       static Action withCustomOperation(Element element, 
                                        BiConsumer<UIEngine, LocatorDescriptor> op) {
           // Create a test-only action for custom scenarios
       }
   }
   ```
4. Update examples to use TestActionFactory

### If Decision is "Keep as Internal"

1. Mark `ElementActions` as `@Internal`
2. Move to `core.actions.internal` package (if desired)
3. Update Javadoc to discourage external use
4. Document limited scope in ADR-015

### If Decision is "Move to Test-Support"

1. Create `src/test/java/core/actions/support/ElementActionFactory.java`
2. Move ElementActions.of() logic there
3. Delete from main source
4. Update all test call sites to import from test-support

---

## Tests

No additional examples needed—this phase validates audit assumptions through compilation and existing test suite.

After refactoring:

```bash
mvn -DskipTests compile  # Verify no references to deleted class
mvn test                 # Verify test infrastructure still works
```

---

## Audit Results

**15 call sites found:**
- 1 production (`ReadOnly.readText()`) — migrated to `ReadTextAction` (Option B prerequisite)
- 14 test infrastructure — custom-operation lambdas that cannot be replaced by concrete constructors

**Decision: Option B — Keep as `@Internal`.**  
See ADR-012 for full reasoning.

**Code changes:**
- `ReadTextAction` created — 17th concrete action subclass, consistent with Phase 14/15
- `ReadOnly.readText()` returns `ReadTextAction` directly — no more `ElementActions.of()` in production
- `ElementActions` annotated `@Internal`, Javadoc updated
- `README.md` example updated to direct-constructor pattern
- `Action.java` error message updated

## Exit Criteria

- [x] All ElementActions.of() call sites audited
- [x] Categorized by context (test, impl, demo, dead)
- [x] One of three options chosen with evidence
- [x] ADR-012 created documenting decision and rationale
- [x] Code refactored according to decision
- [x] All examples pass
- [x] Demo code compiles

---

## ADR-015 Template

```markdown
# ADR-015 — ElementActions Factory Scope

| Field       | Value |
|-------------|-------|
| Status      | Accepted |
| Decision    | [Option A / B / C] |

## Context

ElementActions.of() was the primary action creation mechanism. With concrete 
action subclasses, capability methods now create actions directly.

## Investigation Results

Found X call sites:
- Y in examples (classified as...)
- Z in implementation (classified as...)
- N dead code

## Decision

[Option chosen and reasoning]

## Consequences

...
```

---

## Notes

This phase runs in parallel with Phases 13–17. Its output (ADR-015 and refactored code) is a refinement, not a blocker.

If code is unclear or unusual, document it in ADR-015 with a note for future consideration.

