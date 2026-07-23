# Phase 3 — DSL: Capability-Driven Dispatch

Violations: **P2**
Depends on: Phase 1 and Phase 2 complete (capability interfaces stable)

---

## Goal

After this phase, `VoidDSL` contains no sequential `instanceof` chains for dispatching to
engine operations. Adding a new capability interface requires zero changes to the typed DSL
API. Dynamic runtime entry points require a single centralized registration.

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

**Why typed overloads over a dispatch table:** a dispatch table still requires an explicit
registration entry when a new capability is added -- the same modification problem in a
different form. Typed overloads move the dispatch entirely to the compiler.

In the majority of the public DSL API, the capability is already known statically: a method
accepting `Clickable` will only be called with something `Clickable`. Where the public
signature already accepts a specific capability interface, there is no dispatch to do -- call
the action factory method directly through the typed parameter.

### Step 3 — Capability dispatch for genuinely dynamic entry points

**This step is the exception, not an equal alternative to Step 2.** It applies only to DSL
methods that resolve elements from string keys at runtime and therefore cannot express a
specific capability type in their signatures. All other methods belong in Step 2.

Phase 2 established `element.capability()` as the canonical way to ask an element what it
does. Runtime dispatch should use the same model -- keyed on `ActionCapability` (an enum),
not on interface types (which would reintroduce `isInstance()` and duplicate Phase 2's model
in a different form):

```java
private static final EnumMap<ActionCapability, BiConsumer<Element, String>> DISPATCH =
    new EnumMap<>(ActionCapability.class);

static {
    DISPATCH.put(ActionCapability.MULTI_SELECTABLE, (e, v) -> ((MultiSelectable) e).selectOptions(v.split(",")).execute(engine));
    DISPATCH.put(ActionCapability.SELECTABLE,       (e, v) -> ((Selectable) e).selectOption(v).execute(engine));
    DISPATCH.put(ActionCapability.TYPEABLE,         (e, v) -> ((Typeable) e).type(v).execute(engine));
}

private void dispatch(Element element, String value) {
    BiConsumer<Element, String> handler = DISPATCH.get(element.capability());
    if (handler == null) throw new UnsupportedOperationException(
        "No dispatch registered for capability " + element.capability());
    handler.accept(element, value);
}
```

**Why `EnumMap<ActionCapability, ...>` over `Map<Class<?>, ...>`:** Phase 2 guarantees
one capability family per element and makes `capability()` the canonical query. Using
interface types as keys would maintain two parallel runtime-dispatch models. Using the
enum key aligns dispatch with the model already documented, eliminates `isInstance()`,
and removes the subtype-ordering requirement (enum keys have no inheritance relationship).

**Key difference from `instanceof` chain:** the modification point is centralized -- one
`DISPATCH.put` entry per capability, in one place, rather than a search through every DSL
method for every chain that needs updating. There is still one modification when a new
capability is added; it no longer requires touching existing code in multiple locations.
Adding a new `ActionCapability` constant will produce an unused-entry warning from static
analysis tools if a handler is not registered -- a near-compile-time enforcement the old
chain could not provide.

**Type-safe registration:** the `DISPATCH.put` calls above cast `e` inside the lambda, which
the compiler cannot verify. To eliminate this unchecked association, route registration
through a typed helper:

```java
private static <T extends Element> void register(
    ActionCapability capability,
    Class<T> type,
    BiConsumer<T, String> handler
) {
    DISPATCH.put(capability, (e, v) -> handler.accept(type.cast(e), v));
}

// Registration site is now type-safe:
register(ActionCapability.SELECTABLE, Selectable.class, (s, v) -> s.selectOption(v).execute(engine));
register(ActionCapability.TYPEABLE,   Typeable.class,   (t, v) -> t.type(v).execute(engine));
```

The helper guarantees that `Class<T>` and the `BiConsumer<T, String>` handler remain
type-consistent with each other -- the compiler links the two via `T`. The association
between an `ActionCapability` enum constant and its capability interface is not enforced by
the type system (Java cannot express that relationship generically) and remains a
registration-time responsibility.

**This registry is the canonical runtime dispatch mechanism for capability-based routing.** `switch` on
`ActionCapability` and `isInstance` checks outside this map are prohibited in `VoidDSL`
and should not appear in other classes that could route through the DSL instead.

**Prefer Step 2 wherever feasible.** Use Step 3 only for entry points confirmed dynamic by
the audit.

---

## Audit checklist (complete before writing any code)

For each `instanceof` occurrence in `VoidDSL.java`:

- [ ] Which public DSL method contains it?
- [ ] What is the parameter type at that method's signature?
- [ ] Can the signature be narrowed to the specific capability type without breaking callers?
- [ ] Does the dispatch need to handle `MultiSelectable` before `Selectable` (subtype ordering)?
- [ ] Is the dispatch reachable from more than one public DSL method?

**For every method where "Can narrow?" is No, add a "Reason if not" entry.** Every remaining
`Element` parameter must justify its existence -- a blank reason means the audit is incomplete.

Pre-filled from the current `VoidDSL.java` audit (verify line numbers before implementing):

| DSL method | Element param type | Can narrow? | Reason if not |
|---|---|---|---|
| `selectFromDropdownByContext` | resolved from `String unresolvedEnumName` at runtime | No | Element type not known until runtime resolution |
| `triggerDropdownByContext` | resolved from `String keySuffix` at runtime | No | Element type not known until runtime resolution |
| `getSearchedElementByContext` | resolved from `String unresolvedEnumName` at runtime | No | Element type not known until runtime resolution |
| `clickSearchableElementByContext` | resolved from `String unresolvedEnumName` at runtime | No | Element type not known until runtime resolution |
| `setCheckboxByContext` | resolved from runtime context | No | Element type not known until runtime resolution |
| `verifyElementsAreVisible` | cast to bare `Element` via `(Element) resolved` | Investigate | Check whether `ResolvableEnum` cast site can be narrowed instead |

`resolveEnumConstant` uses `instanceof ResolvableEnum` to determine resolution strategy
rather than to dispatch to a capability-specific engine operation. It is not part of this
phase unless the audit determines it participates in runtime capability routing.

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

Confirm no remaining dispatch-style `instanceof` or `switch` in `VoidDSL`:
```
grep -n "instanceof" src/main/java/dsl/VoidDSL.java
grep -n "switch.*ActionCapability" src/main/java/dsl/VoidDSL.java
```
Expected: zero results for both, or only non-dispatch marker checks with a comment explaining why.
