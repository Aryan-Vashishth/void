# Phase 3 — DSL: Capability-Driven Dispatch

Violations: **P2**
Depends on: Phase 1 and Phase 2 complete (capability interfaces stable)

---

## Goal

After this phase, `VoidDSL` contains no sequential `instanceof` chains for dispatching to
engine operations. Adding a new capability interface requires zero changes to `VoidDSL`.

---

## P2 — `VoidDSL.java`: sequential `instanceof` chains

### Problem

Multiple DSL entry points resolve an element then dispatch to an engine operation via ordered
`instanceof` checks:

```java
if (resolved instanceof MultiSelectable ms) {
    engine.selectFromDropdown(dropdownIndex, ms);
    return;
}
if (resolved instanceof Selectable s) {
    engine.selectFromDropdown(s);
    return;
}
```

The `MultiSelectable` check must come before `Selectable` because `MultiSelectable extends
Selectable` — ordering is load-bearing and undocumented. Every new capability requires:
1. Finding every relevant chain in `VoidDSL`.
2. Inserting at the correct position relative to existing checks.
3. Not missing any overloaded DSL method that also does the same dispatch.

There is no compile-time enforcement that a chain is exhaustive.

---

## Fix strategy: typed signatures first, dispatch table as fallback

### Step 1 — Audit DSL method signatures

For each method in `VoidDSL` that contains an `instanceof` chain, check whether the public
signature already accepts a specific capability type (e.g., `void click(Clickable element)`).

If yes: the `instanceof` chain is unnecessary. The element is already the right type. Call the
action factory method directly through the typed parameter. No dispatch at all.

If no (signature accepts `Element` for a generic entry point): proceed to Step 2.

### Step 2 — Prefer typed overloads over a dispatch table

For each DSL verb that currently accepts a bare `Element` and branches on capability, add
typed overloads:

```java
// Before: one method branching on instanceof
public void interact(Element element, String value) {
    if (element instanceof Typeable t) { t.type(value).execute(engine); return; }
    if (element instanceof Selectable s) { s.selectOption(value).execute(engine); return; }
    throw new UnsupportedOperationException(...);
}

// After: two methods, compiler enforces correct usage
public void type(Typeable element, String value) {
    element.type(value).execute(engine);
}

public void select(Selectable element, String value) {
    element.selectOption(value).execute(engine);
}
```

**Why typed overloads over a dispatch table:** a dispatch table (`Map<Class<?>, Function<...>>`)
looks open/closed but still requires an explicit `put(...)` registration when a new capability
is added — the same modification problem in a different form. Typed overloads move the
dispatch entirely to the compiler. The DSL caller already knows what type it has.

### Step 3 — Dispatch table only for genuinely dynamic entry points

If a DSL method truly cannot know the element type at compile time (e.g., a scripting API or
a step that resolves elements by string name at runtime), use a dispatch table as a last resort:

```java
private static final Map<Class<? extends Element>, BiConsumer<Element, String>> DISPATCH =
    new LinkedHashMap<>(); // order matters: subtype before supertype

static {
    DISPATCH.put(MultiSelectable.class, (e, v) -> ((MultiSelectable) e).selectOptions(v.split(",")).execute(engine));
    DISPATCH.put(Selectable.class,      (e, v) -> ((Selectable) e).selectOption(v).execute(engine));
    DISPATCH.put(Typeable.class,        (e, v) -> ((Typeable) e).type(v).execute(engine));
}

private void dispatch(Element element, String value) {
    for (Map.Entry<Class<? extends Element>, BiConsumer<Element, String>> entry : DISPATCH.entrySet()) {
        if (entry.getKey().isInstance(element)) {
            entry.getValue().accept(element, value);
            return;
        }
    }
    throw new UnsupportedOperationException("No dispatch registered for " + element.getClass());
}
```

**Key difference from `instanceof` chain:** adding `DraggableElement` is one `DISPATCH.put`
call in one location — not a search through `VoidDSL` methods for every chain that needs updating.
The subtype-before-supertype ordering is still required, but it is explicit data in one map, not
implicit control flow scattered across methods.

**Prefer Step 2 wherever feasible.** Use Step 3 only if a genuinely dynamic entry point exists
after the Step 2 audit.

---

## Audit checklist (complete before writing any code)

For each `instanceof` occurrence in `VoidDSL.java`:

- [ ] Which public DSL method contains it?
- [ ] What is the parameter type at that method's signature?
- [ ] Can the signature be narrowed to the specific capability type without breaking callers?
- [ ] Does the dispatch need to handle `MultiSelectable` before `Selectable` (subtype ordering)?
- [ ] Is the dispatch reachable from more than one public DSL method?

Fill in this table before starting:

| DSL method | Line | Element param type | Can narrow? | Subtype ordering needed? |
|------------|------|-------------------|-------------|--------------------------|
| (fill in)  |      |                   |             |                          |

---

## Files changed

| File               | Change                                                             |
|--------------------|--------------------------------------------------------------------|
| `dsl/VoidDSL.java` | Replace `instanceof` chains with typed overloads or a single dispatch map |

---

## Commit

```
refactor(dsl): replace instanceof dispatch chains with typed capability calls
```

If a dispatch map was needed:
```
refactor(dsl): replace instanceof chains with capability dispatch map (single registration point)
```

---

## Verification

```
mvn compile -q
```

Run the demo login test end-to-end (exercises `Typeable`, `Clickable`, `ReadOnly` through the DSL):
```
mvn test -Dtest=DemoLoginTest -q
```

Confirm no remaining `instanceof` in `VoidDSL`:
```
grep -n "instanceof" src/main/java/dsl/VoidDSL.java
```
Expected: zero results, or only marker checks unrelated to capability dispatch.
