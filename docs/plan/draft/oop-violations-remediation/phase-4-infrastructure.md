# Phase 4 — Infrastructure: Registry, Dedup Helper, Via Cleanup

Violations: **P8**, **P9**, **P11**
No upstream dependencies — can be done after any phase or in parallel with Phase 3.

---

## Goal

After this phase:
- Adding a new `UIEngine` requires zero changes to `UIEngineFactory`.
- `SearchableDropdown`/`SearchField` can add new locator roles with one line, no equality checks.
- `Via.java` no longer acts as a growing catalogue of per-capability static methods.

---

## P8 — `UIEngineFactory.java`: switch on engine name string

### Problem

```java
UIEngine engine = switch (engineName) {
    case "selenium"   -> new SeleniumEngine(driver);
    // case "playwright" -> new PlaywrightEngine();
    default -> throw new IllegalStateException("Unsupported engine: ...");
};
```

A new engine (`playwright`, `appium`, `cdp`) requires modifying the switch. The commented-out
Playwright line is direct evidence this has already been anticipated and deferred.

### Fix

Replace the `switch` with a registry map. Keep the API surface identical — only the internals change.

**`UIEngineFactory.java`:**
```java
public final class UIEngineFactory {

    private static final Map<String, BiFunction<String, Object, UIEngine>> REGISTRY =
        new LinkedHashMap<>();

    static {
        REGISTRY.put("selenium", (name, driver) -> new SeleniumEngine((WebDriver) driver));
    }

    public static void register(String name, BiFunction<String, Object, UIEngine> creator) {
        REGISTRY.put(name.trim().toLowerCase(Locale.ROOT), creator);
    }

    public static UIEngine create(String name, Object driver) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        BiFunction<String, Object, UIEngine> creator = REGISTRY.get(key);
        if (creator == null) {
            throw new IllegalStateException(
                "Unsupported engine: '" + name + "'. Registered: " + REGISTRY.keySet());
        }
        return creator.apply(key, driver);
    }

    private UIEngineFactory() {}
}
```

Adding Playwright in a future module:
```java
UIEngineFactory.register("playwright", (name, driver) -> new PlaywrightEngine());
```
Zero changes to `UIEngineFactory`.

**Why not `ServiceLoader`:** `ServiceLoader` is the correct long-term answer for a modular
build. It requires a `module-info.java` or `META-INF/services` file. The map achieves OCP now
without that infrastructure cost. When the project modularises, `register(...)` calls in module
initializers replace the `static {}` block — the public API is unchanged.

---

## P9 — `SearchableDropdown`/`SearchField`: O(n²) role deduplication

### Problem

`getAllLocatorRoles()` in both interfaces deduplicates locator keys with growing equality chains:
```java
if (input != null && !input.isBlank() && !input.equals(trigger)) roles.put(...);
if (button != null && !button.isBlank() && !button.equals(trigger) && !button.equals(input)) roles.put(...);
```
Four roles → 6 comparisons. Five roles → 10. Adding a fifth role requires modifying the method
to add new equality checks against all four existing roles.

### Fix

Add a package-private static helper to `Element.java` (or a package-private `LocatorRoleMap`
utility if the method feels out-of-place on `Element`):

```java
static Map<ElementRole, String> roleMap(Object... pairs) {
    Map<ElementRole, String> result = new LinkedHashMap<>();
    Set<String> seen = new LinkedHashSet<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
        ElementRole role = (ElementRole) pairs[i];
        String key  = (String) pairs[i + 1];
        if (key != null && !key.isBlank() && seen.add(key)) {
            result.put(role, key);
        }
    }
    return result;
}
```

`SearchableDropdown.getAllLocatorRoles()` becomes:
```java
@Override
default Map<ElementRole, String> getAllLocatorRoles() {
    return Element.roleMap(
        ElementRole.TRIGGER,       getTriggerLocator(),
        ElementRole.SEARCH_INPUT,  getSearchInputLocator(),
        ElementRole.SEARCH_BUTTON, getSearchButtonLocator(),
        ElementRole.LIST,          getListLocator()
    );
}
```

Adding `SEARCH_CLEAR_BUTTON`: one new pair at the end. Dedup is automatic.

`SearchField.getAllLocatorRoles()` — same pattern.

**Why varargs pairs and not a builder:** a builder is a new public type with its own lifecycle.
Varargs pairs are inline at the call site, read as a declaration, and the helper is
package-private — not part of the framework API. If a builder is ever needed (dynamic role
registration, conditional inclusion), the varargs helper is the prototype for it.

**Constraint:** pairs must be `(ElementRole, String, ElementRole, String, ...)` — even count,
alternating types. The helper does not validate this at compile time. If misuse is a concern,
consider an overloaded form:
```java
static Map<ElementRole, String> roleMap(
    ElementRole r1, String k1,
    ElementRole r2, String k2,
    ElementRole r3, String k3,
    ElementRole r4, String k4) { ... }
```
For four fixed roles this is more explicit. Add overloads for 2, 3, 4 roles and keep the
varargs form for ≥5.

---

## P11 — `Via.java`: per-capability static helper methods

### Problem

`Via` has one `cast(element)` + one `is(element)` method per capability interface. Adding any
new capability requires two new static methods in `Via`. The class grows indefinitely with the
capability set.

### Fix

**Step 1 — audit all `Via.*` call sites** across the codebase:
```
grep -rn "Via\." src/
```

Classify each call site into one of three categories:

| Category | Example | Replacement |
|----------|---------|-------------|
| Boolean check | `Via.isClickable(e)` | `e instanceof Clickable` |
| Cast for immediate use | `Via.clickable(e).click()` | `((Clickable) e).click()` |
| Dynamic / unknown at compile time | `Via.cast(e, capabilityClass)` | keep one generic helper |

**Step 2 — eliminate category 1 and 2 call sites** — inline the `instanceof` or cast directly.
These are one-liners; the `Via` wrapper adds no value.

**Step 3 — reduce `Via` to at most one generic method** (or delete the class):

If any category-3 call sites exist (truly dynamic capability resolution where the capability
class is a variable), keep:
```java
public final class Via {
    public static <T extends Element> T cast(Element element, Class<T> capability) {
        if (!capability.isInstance(element))
            throw new ClassCastException(element + " does not implement " + capability.getSimpleName());
        return capability.cast(element);
    }
    private Via() {}
}
```

If zero category-3 call sites exist after the audit, **delete `Via.java`** entirely.

**What not to do:** do not add a `Via.draggable(e)` method when `DraggableElement` is added.
The audit + reduction in this phase exists precisely to prevent that pattern from continuing.

---

## Files changed

| File                                                    | Change                                          |
|---------------------------------------------------------|-------------------------------------------------|
| `core/engine/UIEngineFactory.java`                      | `switch` → registry `Map`                       |
| `elements/api/Element.java`                             | add `roleMap` static helper                     |
| `elements/api/capability/SearchableDropdown.java`       | `getAllLocatorRoles()` uses `Element.roleMap(…)` |
| `elements/api/capability/SearchField.java`              | `getAllLocatorRoles()` uses `Element.roleMap(…)` |
| `core/interactions/Via.java`                            | reduce to 1 generic method **or DELETE**        |
| All `Via.*` call sites                                  | inline `instanceof` / cast                      |

---

## Commits

```
feat(engine): replace UIEngineFactory switch with registry map
refactor(elements): SearchableDropdown and SearchField use roleMap dedup helper
chore(interactions): inline Via call sites, reduce Via to generic cast helper
```

If `Via` is deleted entirely:
```
chore(interactions): delete Via, inline all call sites with direct instanceof/cast
```

---

## Verification

```
mvn compile -q
```

Confirm no string switch in factory:
```
grep -n "switch" src/main/java/core/engine/UIEngineFactory.java
# must return zero results
```

Confirm no per-capability methods remain in `Via` (if kept):
```
grep -n "Clickable\|Typeable\|Selectable\|Uploadable\|ReadOnly\|Listable" \
  src/main/java/core/interactions/Via.java
# must return zero results
```
