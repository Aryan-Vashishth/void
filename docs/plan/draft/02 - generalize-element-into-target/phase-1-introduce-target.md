# Phase 1 -- Introduce Target

Touches: `core/target/Target.java` (new file only)

---

## Goal

Create `core.target.Target` -- the domain-neutral root above `UIElement`.

After this phase:
- `Target` exists and compiles in isolation. No existing file changes.
- `Target` carries the three methods with zero UI or enum-specific semantics:
  `getDisplayText()`, `getArgs()`, `effectiveArgs()`, and the `NO_ARGS` constant.
- `UIElement` does not yet extend `Target`. That wiring happens in Phase 2.
- All existing page object enums, capability interfaces, `UIEngine`, and locator
  infrastructure are completely unaffected.

Phase 1 is low-risk by design: it adds one file and changes nothing else.

---

## New file: `core/target/Target.java`

```java
package core.target;

/**
 * Domain-neutral root abstraction for anything the framework can operate on.
 *
 * <p>A Target is any describable, potentially-parameterizable subject of an
 * engine action -- a web element, an API endpoint, a mobile component, etc.
 * This interface carries only the concepts that are independent of the
 * interaction medium:</p>
 * <ul>
 *   <li>A human-readable display label ({@link #getDisplayText()}) for logging
 *       and reporting.</li>
 *   <li>Optional template arguments ({@link #getArgs()}) for parameterized
 *       targets (e.g., a row index or a dynamic label).</li>
 * </ul>
 *
 * <p>UI-specific concerns (locator keys, locator roles, external locator files)
 * are defined on {@link elements.api.UIElement}, which extends this interface.</p>
 *
 * <p>Implementations are expected to be immutable value descriptors -- typically
 * enum constants. The framework does not enforce this at the type level.</p>
 *
 * @see elements.api.UIElement
 */
public interface Target {

    /** Shared empty-args sentinel -- signals this target requires no template arguments. */
    Object[] NO_ARGS = new Object[0];

    /**
     * Returns a human-readable label for this target.
     * Used in log output and assertion messages.
     *
     * @return non-null display text
     */
    String getDisplayText();

    /**
     * Returns dynamic arguments used to format parameterized locator or address templates.
     * Returns {@link #NO_ARGS} by default (no arguments required).
     *
     * @return argument array; never null
     */
    default Object[] getArgs() {
        return NO_ARGS;
    }

    /**
     * Returns {@code overrides} when non-null and non-empty; otherwise returns
     * {@link #getArgs()}.
     *
     * <p>Centralises the "override args take precedence over the target's own args" rule
     * that would otherwise be repeated as a ternary at every call site.</p>
     *
     * @param overrides caller-supplied argument overrides
     * @return effective argument array; never null
     */
    default Object[] effectiveArgs(Object... overrides) {
        return (overrides != null && overrides.length > 0) ? overrides : getArgs();
    }
}
```

---

## What does NOT change in this phase

- `elements/api/Element.java` -- unchanged; no `extends Target` yet
- All capability interfaces -- unchanged
- `UIEngine.java` -- unchanged
- All page object enums -- unchanged
- `LocatorFamily` variants -- unchanged
- `ElementRole`, `LocatorDescriptor`, `LocatorStrategy` -- unchanged
- All production and test behavior -- unchanged

---

## Files changed

| File | Change |
|------|--------|
| NEW `core/target/Target.java` | New interface with `getDisplayText()`, `getArgs()`, `effectiveArgs()`, `NO_ARGS` |

---

## Commit

```
feat(core): introduce Target domain root in core.target
```

---

## Verification

```
mvn compile -q

# Confirm no existing file was touched
git diff HEAD --name-only
# expected: only core/target/Target.java

# Confirm Target compiles in isolation with no framework imports
grep -n "import" src/main/java/core/target/Target.java
# expected: zero results
```

---

## Phase complete when

- [ ] `core/target/Target.java` exists and compiles.
- [ ] `mvn compile -q` passes with no other file changes.
- [ ] `Target` has no imports from `core.engine`, `elements`, or Selenium.
