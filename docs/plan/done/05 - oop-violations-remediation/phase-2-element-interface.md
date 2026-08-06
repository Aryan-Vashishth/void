# Phase 2 — Element Interface: Safety, Capability Colocation, Forced Abstract Removal

Violations: **P5**, **P6**, **P7**, **P10**
Deletes: `ActionCapabilityProvider.java`
Depends on: Phase 1 complete (`elementLabel()`/`operationLabel()` must be on `Action`)

---

## Goal

After this phase:
- `Element` defaults never crash on a non-enum implementation.
- `capability()` is part of the `Element` contract — no separate interface required.
- Label derivation in `ElementAction` and `LocatorResolver` is polymorphic, not cast-based.
- `Listable` implementors get index-from-ordinal for free.

---

## P5 — `Element.java`: `(Enum<?>) this` hard casts

### Problem

`Element` interface defaults cast `this` to `Enum<?>` in at least four methods:
`getDisplayText()`, `getPrimaryLocator()`, `getExternalFileName()`, `qualifiedLocatorKey()`.
Any non-enum implementor gets a `ClassCastException` at the cast site, not at the design
decision that produced the wrong type.

### Fix

**Important visibility constraint:** Java interface static methods are implicitly `public
static` and cannot be narrowed. Adding the helpers directly to `Element.java` would expose
them as framework API forever, even though they are implementation details. To keep them
package-private, introduce a dedicated utility class:

**New file: `elements/api/ElementSupport.java`** (package-private, same package as `Element`):
```java
final class ElementSupport {
    private ElementSupport() {}

    static String nameOf(Element e) {
        return e instanceof Enum<?> en ? en.name() : e.getClass().getSimpleName();
    }

    static Class<?> declaringClassOf(Element e) {
        if (e instanceof Enum<?> en) {
            Class<?> dc = en.getDeclaringClass();
            return dc != null ? dc : en.getClass();
        }
        return e.getClass();
    }

    static int ordinalOf(Element e) {
        if (e instanceof Enum<?> en) return en.ordinal();
        throw new UnsupportedOperationException(
            e.getClass().getSimpleName() + " implements Listable but has no ordinal semantics. Override Listable.getIndex()."
        );
    }
}
```

`ordinalOf` throws rather than returning `0` for non-enum elements because ordinal is a
semantic value: `0` is indistinguishable from a real first-position index and would silently
produce wrong list offsets rather than failing visibly. Any non-enum `Listable` implementor
must override `getIndex()` explicitly.

Replace every `((Enum<?>) this).name()` → `ElementSupport.nameOf(this)`.
Replace every `((Enum<?>) this).getDeclaringClass()` / `.getEnclosingClass()` →
`ElementSupport.declaringClassOf(this)`.
Replace every `((Enum<?>) this).ordinal()` → `ElementSupport.ordinalOf(this)`.

**Why three helpers and not one value-object:** a `EnumInfo` wrapper would need a factory call
and a field access per use. Three static one-liners are inline and disappear at the call site.
`ElementSupport` is not part of the public API -- it exists only to centralise enum-specific
reflection with the visibility the design actually requires. Its scope is intentionally
narrow: structural enum facts (`name`, `declaring class`, `ordinal`). It must not accumulate
presentation helpers, resolver-specific formatting, or any logic that belongs to a call site.
Utility classes expand by default; the constraint here is deliberate.

**Non-enum behaviour after fix:** `nameOf` returns `getClass().getSimpleName()`,
`declaringClassOf` returns the class itself. Implementors that need precise control override
the default methods that call these helpers.

---

## P7 — Move `capability()` to `Element`, delete `ActionCapabilityProvider`

### Problem

`ElementActions.capabilityFor(Element element)` does:
```java
if (element instanceof ActionCapabilityProvider p) return p.capability();
return ActionCapability.UNKNOWN;
```
An element that provides its capability through a different mechanism returns `UNKNOWN` silently.
`Element` and `ActionCapabilityProvider` are parallel interfaces that should be one.

### Fix

**`Element.java` — add default:**
```java
default ActionCapability capability() { return ActionCapability.UNKNOWN; }
```

All nine capability interfaces already declare:
```java
@Override
default ActionCapability capability() { return ActionCapability.CLICKABLE; /* etc. */ }
```
These declarations currently satisfy `ActionCapabilityProvider`. After the fix they satisfy
`Element.capability()` instead — no change to the method bodies, only to which interface they
override.

**`ElementActions.java` — simplify:**
```java
private static ActionCapability capabilityFor(Element element) {
    return element.capability();
}
```
The `instanceof` check is gone. If `capabilityFor` is only called in one place, inline it.

**Remove `implements ActionCapabilityProvider` from all nine capability interfaces** —
`Clickable`, `Typeable`, `ReadOnly`, `Selectable`, `MultiSelectable`, `Listable`, `Uploadable`,
`Table`, `EditableTable`. Each already extends `Element`, which now carries `capability()`.

**Delete `ActionCapabilityProvider.java`** — the interface is now empty. Any external code
doing `element instanceof ActionCapabilityProvider` should migrate to
`element.capability() != ActionCapability.UNKNOWN`.

**Architectural invariant -- one capability family per element:** `capability()` returns a
single `ActionCapability`. This is a deliberate constraint: each element enum constant
represents one interaction kind (click a button, type into a field), and the framework routes
it to a single action family. An element that is simultaneously `Clickable` and `Typeable`
is a design error at the element-modelling level, not something the API should accommodate.
Document this constraint wherever `capability()` is discussed so future contributors do not
attempt to make one element satisfy multiple capability interfaces and then discover the
method cannot represent that model.

**Extension test:** `DraggableElement` is a new capability interface.
```java
public interface DraggableElement extends Element {
    @Override
    default ActionCapability capability() { return ActionCapability.DRAGGABLE; }
}
```
Zero changes to `ElementActions`, `Action`, or any existing class.

---

## P6 — `ElementAction` + `LocatorResolver`: duplicated `instanceof Enum<?>` label checks

### Problem

Two independent classes both derive a label from an element by checking `instanceof Enum<?>`:

```java
// ElementAction.elementLabel():
if (element instanceof Enum<?> e) return e.name();
return element.getClass().getSimpleName();

// LocatorResolver.labelOf():
if (!(element instanceof Enum<?> en)) return null;
String prefix = page != null ? page.getSimpleName() + " > " : "";
return prefix + en.getClass().getSimpleName() + " > " + en.name();
```

`Element.getDisplayText()` already produces a human-readable label via word-transform. Fixing
one call site without the other leaves inconsistent label formats in action traces vs. resolver
traces for the same element.

### Fix

**`ElementAction.java`:**
```java
@Override
public String elementLabel() {
    return element.getDisplayText();
}
```

**`LocatorResolver.labelOf()`:**

`getDisplayText()` returns the word-transformed constant name (e.g., `"Username"`). If the
`Page > Enum > CONSTANT` format is needed in resolver traces, build it from the helpers added
in P5:

```java
private static String labelOf(Element element) {
    Class<?> declaring = ElementSupport.declaringClassOf(element);
    Class<?> page = declaring.getEnclosingClass();
    String prefix = page != null ? page.getSimpleName() + " > " + declaring.getSimpleName() + " > " : "";
    return prefix + element.getDisplayText();
}
```

No `instanceof`. Non-enum elements get `ClassName > DisplayText` automatically.

**Why not a single `Element.qualifiedLabel()` method:** the two call sites want different
formats. Centralising the format string onto `Element` would couple the element abstraction to
display concerns specific to two internal resolvers. The P5 static helpers provide the
structural facts (`declaringClassOf`); each call site assembles the format it needs.

---

## P10 — `Listable.getIndex()`: forced abstract with no default

### Problem

`Listable` declares `int getIndex();` as abstract. Every enum implementing `Listable` must
provide an override even when ordinal-based indexing is correct. When a new constant is
inserted in the middle of an enum, all manually-maintained index overrides below it are wrong
with no compile error.

### Fix

**`Listable.java` — change abstract to default:**
```java
default int getIndex() {
    return ElementSupport.ordinalOf(this);   // uses the P5 helper
}
```

Enums that need non-ordinal indices (zero-based vs. one-based, non-contiguous, externally
defined) override explicitly. All existing overrides that return their own ordinal value can
be deleted — they are now redundant.

**Audit:** before deleting any override, verify it does not do arithmetic on the ordinal
(e.g., `ordinal() + 1` for one-based indexing) — those must be kept.

`Selectable.getIndex()` currently returns a hardcoded `0`, not an ordinal value. This is not
redundant with the new default and must not be silently deleted. Determine whether `0` is
intentional (a fixed index for a specific engine API contract) or a placeholder before
removing or replacing this override.

---

## Files changed

| File                                             | Change                                                      |
|--------------------------------------------------|-------------------------------------------------------------|
| `elements/api/ElementSupport.java`               | **NEW** — package-private utility: `nameOf`, `declaringClassOf`, `ordinalOf` |
| `elements/api/Element.java`                      | Replace casts with `ElementSupport` calls; add `capability()` default |
| `elements/api/capability/Listable.java`          | `getIndex()` becomes `default`                              |
| `elements/api/capability/Clickable.java`         | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/Typeable.java`          | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/ReadOnly.java`          | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/Selectable.java`        | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/MultiSelectable.java`   | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/Uploadable.java`        | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/Table.java`             | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/EditableTable.java`     | Remove `implements ActionCapabilityProvider`                |
| `elements/api/capability/SearchField.java`       | Remove `implements ActionCapabilityProvider`                |
| `core/actions/ElementActions.java`               | `capabilityFor` → `element.capability()` (or inline)        |
| `core/actions/ElementAction.java`                | `elementLabel()` → `element.getDisplayText()`               |
| `core/resolvers/locator/api/LocatorResolver.java`| `labelOf()` uses `declaringClassOf` + `getDisplayText()`    |
| `core/actions/ActionCapabilityProvider.java`     | **DELETE**                                                  |

---

## Commits

```
feat(elements): add enum-safe static helpers to Element, remove Enum casts from defaults
feat(elements): move capability() to Element, delete ActionCapabilityProvider
fix(actions): ElementAction.elementLabel delegates to element.getDisplayText()
fix(resolvers): LocatorResolver.labelOf uses Element helpers, removes Enum cast
feat(elements): default getIndex() on Listable from ordinal
```

---

## Verification

Before removing `ActionCapabilityProvider`, confirm every implementation site:
```
grep -rn "ActionCapabilityProvider" src/main/java
# review each result -- remove implements clause from each interface found
```

Then verify deletion is complete:
```
mvn compile -q
grep -r "ActionCapabilityProvider" src/
# must return zero results
```

Run locator sync to confirm element resolution is unaffected:
```
mvn compile -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli ^
  "-Dexec.args=--sync examples.pages.DemoLoginPage --prune"
```
Expected: `[sync] Done — DemoLoginPage is in sync.`
