# ADR-012 — ElementActions Factory Scope

**Date:** 2026-07-07  
**Status:** Accepted

---

## Context

`ElementActions.of(Element, ElementRole, BiConsumer<UIEngine, LocatorDescriptor>)` was the
primary action creation mechanism before Phase 14/15 introduced concrete action subclasses:

```java
// Old pattern (pre-Phase 14)
default Action click() {
    return ElementActions.of(this, ElementRole.TRIGGER, (engine, d) -> engine.click(d));
}

// New pattern (Phase 14/15)
default ClickAction click() {
    return new ClickAction(this);
}
```

After Phase 15, all 16 capability methods emitted concrete subclasses. Phase 19 audited what
remained of `ElementActions.of()`.

---

## Investigation Results

**Total call sites found:** 15  

| Location | Count | Category |
|----------|-------|----------|
| `src/main/java/` (production) | 1 | `ReadOnly.readText()` — missed in Phase 15 |
| `src/test/java/` (test infrastructure) | 14 | Custom-operation lambdas (descriptor capture, execution logging, hook-pipeline tests) |
| `src/main/java/core/actions/Action.java` | 1 | Error message string only (not a call site) |
| `src/main/java/core/actions/README.md` | 1 | Documentation example (stale) |

**Production call site:** `ReadOnly.readText()` used `ElementActions.of()` to invoke
`engine.getText()`. This was the only remaining production usage. It was migrated to a new
`ReadTextAction` concrete subclass (consistent with Phase 14/15 pattern).

**Test call sites:** All 14 test usages supply custom operation lambdas:
- Descriptor capture: `(engine, d) -> receivedDescriptor.set(d)`
- Execution logging: `(engine, d) -> executionLog.add("action")`
- Hook-pipeline verification: various recording lambdas

These cannot be replaced by concrete action constructors. `ClickAction` always calls
`engine.click()`, `TypeAction` always calls `engine.type()` — there is no concrete subclass
for arbitrary custom operations. The test infrastructure needs a way to create element-bound
actions with programmable behavior.

---

## Options Considered

**Option A — Delete entirely**  
Not viable. Test infrastructure needs a factory for custom operations.

**Option B — Keep as `@Internal` utility**  
Viable. The factory remains accessible within the framework and tests (same package or
accessible via annotation convention), with Javadoc and the `@Internal` marker making the
limited scope explicit.

**Option C — Move to test-support package**  
Viable, but adds maintenance overhead (a separate test-support module or directory). The
class is small and the audience is limited — moving it adds indirection without benefit.

---

## Decision

**Option B: Keep `ElementActions` as `@Internal`.**

Rationale:

1. The production need is gone. After `ReadTextAction`, no capability interface calls
   `ElementActions.of()`. The public API is entirely concrete-subclass-based.

2. Test infrastructure legitimately needs a custom-operation factory. Hook-pipeline tests
   verify that descriptors, engines, and hook ordering work correctly — they require actions
   with programmable execute behavior that concrete subclasses cannot provide.

3. `@Internal` communicates the scope precisely. The class is small, stable, and already
   documented as an implementation detail. Adding the annotation formalizes what the Javadoc
   already stated.

4. Moving to a test-support package adds indirection with no benefit. The class's scope is
   already narrowed by the annotation; test authors who need it can use it.

---

## Consequences

- `ElementActions` is annotated `@Internal` and its Javadoc updated to reflect its narrowed scope.
- `ReadOnly.readText()` returns `ReadTextAction` — consistent with all other 15 concrete action subclasses.
- All 16 production capability methods now emit typed concrete subclasses. No production code calls `ElementActions.of()`.
- Test infrastructure continues to use `ElementActions.of()` for custom-operation test actions.
- `ElementActions.of()` remains the escape hatch for framework contributors who need a one-off element-bound action without a dedicated subclass.
