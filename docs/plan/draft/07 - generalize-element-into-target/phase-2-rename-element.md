# Phase 2 -- Rename Element to UIElement

Touches: `Element.java` (renamed), `UIEngine.java`, `SeleniumEngine.java`,
all page object enums, `LocatorFamily` variants, and every file importing
`elements.api.Element`.

---

## Goal

Rename `Element` to `UIElement` and wire `UIElement extends Target`. After this phase:

- `elements.api.UIElement` is the renamed interface. `elements.api.Element` no longer exists.
- `UIElement` extends `core.target.Target`. Page object enums inherit `Target` transitively.
- `getDisplayText()`, `getArgs()`, `effectiveArgs()`, and `NO_ARGS` are removed from
  `UIElement` (they now live on `Target`); `UIElement`'s `getDisplayText()` default
  override restores the enum-name-split logic.
- `UIEngine.resolve()` takes `UIElement` instead of `Element`.
- All page object `implements Element` declarations read `implements UIElement`.
- No locator methods move. No behavior changes.

This is the high-blast-radius phase. All changes are symbol renames and import updates --
no logic changes. The recommended approach is an IDE-assisted global symbol rename to
propagate all references in one pass, followed by a targeted cleanup for any edge cases
the rename misses.

---

## Changes

### `elements/api/Element.java` -- rename and restructure

Rename file to `UIElement.java`. Update the interface declaration:

```java
package elements.api;

import core.target.Target;
import elements.meta.ElementRole;
import javax.annotation.Nullable;

/**
 * Core abstraction for every UI element descriptor in the framework.
 *
 * <p>Extends {@link core.target.Target}: inherits {@code getDisplayText()},
 * {@code getArgs()}, {@code effectiveArgs()}, and the {@code NO_ARGS} constant.
 * The enum-name-split default for {@code getDisplayText()} is overridden here.</p>
 *
 * <p>Responsibilities beyond Target:</p>
 * <ul>
 *   <li>Expose an external locator file path for classpath-based locator bundles.</li>
 *   <li>Provide primary and secondary locator keys for engine resolution.</li>
 *   <li>Publish an ordered role map via {@link #getAllLocatorRoles()} used by resolution
 *       pipelines.</li>
 * </ul>
 *
 * <p><b>Enum-only contract</b>: all default method implementations call
 * {@code (Enum<?>) this}. UIElement must be implemented by enum types only.
 * Non-enum implementations will compile but throw {@link ClassCastException}
 * at the first default method call.</p>
 */
public interface UIElement extends Target {

    // -------------------------------------------------------------------------
    // Target overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable label derived from the enum constant name.
     * Transformation: {@code SAVE_AS_DRAFT} -> {@code Save As Draft}.
     * Tokens are split on underscores; each token is capitalised with the rest lowercased.
     * Capability interfaces override this to incorporate dynamic args when present.
     */
    @Override
    default String getDisplayText() {
        String[] tokens = ((Enum<?>) this).name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) sb.append(token.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Locator resolution (UI-specific)
    // -------------------------------------------------------------------------

    // ... all existing Element methods (getExternalFileName, getPrimaryLocator,
    //     getSecondaryLocator, getAllLocatorRoles, locatorKeyForRole,
    //     templateFamilyKey, qualifiedLocatorKey) remain here UNCHANGED ...
}
```

The four methods that move to `Target` (`getDisplayText` body excluded -- it stays as an
`@Override` default on `UIElement`, `getArgs`, `effectiveArgs`, `NO_ARGS`) are removed
from `UIElement` because they are now inherited from `Target`. The `getDisplayText()`
default is kept as an `@Override` to provide the enum-split behavior that `Target` cannot
(Target declares `getDisplayText()` without a default).

### `core/engine/UIEngine.java` -- parameter rename in `resolve()`

```java
// Before
LocatorDescriptor resolve(Element element, ElementRole role, Object... args);

// After
LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args);
```

Import changes: remove `import elements.api.Element`, add `import elements.api.UIElement`.

### `core/engine/selenium/SeleniumEngine.java` -- parameter rename in `resolve()`

```java
// Before (implementation)
public LocatorDescriptor resolve(Element element, ElementRole role, Object... args) { ... }

// After
public LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args) { ... }
```

Import changes: same pattern -- swap `Element` import for `UIElement`.

### `elements/api/LocatorFamily.java`, `AdvancedLocatorFamily.java`, `SwitchLocatorFamily.java`

These are marker interfaces. Audit each one:
- If it declares `extends Element` (or references `Element`), update to `extends UIElement`.
- If it is standalone with no explicit `Element` reference, no change is needed.

### All page object enums

Every enum implementing `Element` updates its declaration:

```java
// Before
enum Button implements Element, Clickable { ... }
enum Credentials implements Element, Typeable { ... }

// After
enum Button implements UIElement, Clickable { ... }
enum Credentials implements UIElement, Typeable { ... }
```

**The enum constant bodies do not change.** Only the `implements` declaration line changes.

### Capability interfaces

Capability interfaces (`Clickable`, `Typeable`, `Selectable`, etc.) are standalone mixins.
They do not extend `Element` / `UIElement`. No changes needed to their declarations or
method signatures.

If any capability interface contains an `Element` type reference in a method parameter or
return type, update it to `UIElement`. Audit with:
```
grep -rn "Element" src/main/java/elements/api/capability/
```

### All other files

Any file importing `elements.api.Element` that is not covered above must update the import
to `elements.api.UIElement`. Use the IDE rename or:
```
grep -rn "elements\.api\.Element" src/
```

---

## Method migration summary

| Method | Before | After |
|--------|--------|-------|
| `getDisplayText()` | default on `Element` (enum-split) | `@Override` default on `UIElement`; same body |
| `getArgs()` | default on `Element` | inherited from `Target` (removed from `UIElement`) |
| `effectiveArgs()` | default on `Element` | inherited from `Target` (removed from `UIElement`) |
| `NO_ARGS` | constant on `Element` | inherited from `Target` (removed from `UIElement`) |
| All locator methods | on `Element` | on `UIElement`; bodies unchanged |

---

## What does NOT change in this phase

- All locator method bodies -- untouched
- `ElementRole`, `LocatorDescriptor`, `LocatorStrategy` -- untouched
- Capability interface method bodies -- untouched
- Enum constant bodies -- untouched (only `implements` declaration line changes)
- `DriverContext`, `DriverFactory`, `SeleniumEngine` driver internals -- untouched
- `VOID`, `VOIDBuilder`, `SessionContext` -- untouched
- All test behavior -- no behavioral changes; pure symbol rename

---

## Files changed

| File | Change |
|------|--------|
| `elements/api/Element.java` | Renamed to `UIElement.java`; adds `extends Target`; removes `getArgs`, `effectiveArgs`, `NO_ARGS` (now on `Target`); keeps `getDisplayText` as `@Override` default |
| `core/engine/UIEngine.java` | `resolve()` parameter: `Element` -> `UIElement`; import swap |
| `core/engine/selenium/SeleniumEngine.java` | `resolve()` parameter: `Element` -> `UIElement`; import swap |
| `elements/api/LocatorFamily.java` | Update `Element` reference to `UIElement` if present |
| `elements/api/AdvancedLocatorFamily.java` | Same |
| `elements/api/SwitchLocatorFamily.java` | Same |
| All page object enums | `implements Element` -> `implements UIElement` |
| Any other file importing `elements.api.Element` | Import updated to `elements.api.UIElement` |

---

## Commit

```
refactor(elements): rename Element to UIElement; extend core.target.Target

UIElement replaces Element. The rename makes the UI-specific scope of the interface
explicit. UIElement extends Target; page object enums inherit Target transitively
with no changes to enum constant bodies. getArgs, effectiveArgs, and NO_ARGS are
removed from UIElement and inherited from Target. getDisplayText is kept on UIElement
as an Override default to preserve the enum-name-split behavior.

BREAKING CHANGE: Element is removed. Replace all `implements Element`
with `implements UIElement`.
```

---

## Verification

```
mvn compile -q

# No remaining Element references in production sources (except the deleted file)
grep -rn "elements\.api\.Element" src/main/java/
# expected: zero results

grep -rn "implements Element" src/main/java/
# expected: zero results

grep -rn "import elements\.api\.Element" src/test/
# expected: zero results (update test imports too if any)

# UIElement correctly extends Target
grep -n "extends Target" src/main/java/elements/api/UIElement.java
# expected: one result on the interface declaration line

# getArgs and effectiveArgs are NOT declared on UIElement
grep -n "getArgs\|effectiveArgs\|NO_ARGS" src/main/java/elements/api/UIElement.java
# expected: zero results (inherited from Target)

# getDisplayText IS declared as an Override on UIElement
grep -n "getDisplayText" src/main/java/elements/api/UIElement.java
# expected: one result (the @Override default)

mvn test -q
# expected: all examples pass; no behavior changed
```

---

## Phase complete when

- [ ] `elements.api.Element` no longer exists.
- [ ] `elements.api.UIElement` exists and compiles.
- [ ] `UIElement` extends `Target`.
- [ ] No `implements Element` declarations remain in any source file.
- [ ] `UIEngine.resolve()` takes `UIElement`.
- [ ] `mvn compile -q` passes.
- [ ] `mvn test -q` passes with no test failures.
