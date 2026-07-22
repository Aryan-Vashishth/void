# Phase 4 — Infrastructure: Registry, Dedup Helper, Via Cleanup

Violations: **P8**, **P9**, **P11**
No upstream dependencies within this plan -- can be done after any phase or in parallel with Phase 3.

> **External dependency (P8 only)**: `initiative/engine-decoupling` Phases 1 and 2 must be merged
> before P8 is applied. Phase 1 changes `UIEngineFactory.create()` to accept `EngineBootstrap`
> instead of `WebDriver`; Phase 2 deletes `EngineBootstrap.FromDriver`. By the time P8 runs,
> the factory input is `EngineBootstrap` (containing only `FromProfile`). See the P8 section
> below for the impact on the registry creator signature.

---

## Goal

After this phase:
- Supporting a new `UIEngine` requires no modification to `UIEngineFactory`; only registration of the new engine.
- `SearchableDropdown`/`SearchField` can add new locator roles with one line, no equality checks.
- `Via.java` no longer acts as a growing catalogue of per-capability static methods.

---

## P8 — `UIEngineFactory.java`: switch on engine name string

### Problem

```java
UIEngine engine = switch (engineName) {
    case "selenium"   -> new SeleniumEngine(engineHost);
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

    private static final Map<String, Function<Object, UIEngine>> REGISTRY =
        new LinkedHashMap<>(); // insertion order preserved for readable exception messages

    static {
        REGISTRY.put("selenium",
            engineHost -> new SeleniumEngine((WebDriver) engineHost));
    }

    public static void register(String engineName, Function<Object, UIEngine> creator) {
        String key = engineName.trim().toLowerCase(Locale.ROOT);
        if (REGISTRY.containsKey(key))
            throw new IllegalStateException("Engine '" + engineName + "' is already registered.");
        REGISTRY.put(key, creator);
    }

    public static UIEngine create(String engineName, Object engineHost) {
        String key = engineName.trim().toLowerCase(Locale.ROOT);
        Function<Object, UIEngine> creator = REGISTRY.get(key);
        if (creator == null) {
            throw new IllegalStateException(
                "No UIEngine registered for '" + engineName + "'.\nRegistered engines: " + REGISTRY.keySet());
        }
        return creator.apply(engineHost);
    }

    private UIEngineFactory() {}
}
```

Adding Playwright in a future module:
```java
UIEngineFactory.register(
    "playwright",
    host -> new PlaywrightEngine((Page) host)
);
```
Zero changes to `UIEngineFactory`.

After engine-decoupling Phases 1-2 are merged, the Selenium creator casts to `EngineBootstrap.FromProfile`
rather than `WebDriver`:
```java
static {
    REGISTRY.put("selenium",
        host -> new SeleniumEngine(((EngineBootstrap.FromProfile) host).profile()));
}
```
`EngineBootstrap` is passed as `Object` -- the registry type is unchanged. Only the cast inside
the lambda changes.

`Function<Object, UIEngine>` is sufficient because the creator already knows which engine it
constructs from the registration itself. Passing the engine name into the creator (as
`BiFunction<String, Object, UIEngine>` would) duplicated information that was never used.

**EngineHost abstraction:** an EngineHost is the bootstrap object supplied when creating a
`UIEngine`. Each engine implementation requires a different concrete EngineHost type -- for
Selenium this is a `WebDriver`; for Playwright it may be a `Page` or `BrowserContext`. The
factory resolves the registered engine creator and passes the EngineHost through unchanged.
The factory treats the EngineHost as opaque; only the registered engine implementation interprets its concrete type.

EngineHost is represented as `Object` because each engine implementation requires a different
concrete host type. Introducing a common host interface would unnecessarily couple unrelated
engine implementations. Each registered engine creator is therefore responsible for validating
and casting its own EngineHost
(for example `(WebDriver) engineHost`). Callers supplying the wrong host type will receive
a `ClassCastException` at the creator site rather than a compile-time error. This is an
inherent constraint of supporting heterogeneous engine implementations and is acceptable
because engine creation is an internal framework bootstrap operation, not user-facing API.

**Why not `ServiceLoader`:** `ServiceLoader` is the correct long-term answer for a modular
build. It requires a `module-info.java` or `META-INF/services` file. The map achieves OCP now
without that infrastructure cost. When the project modularises, `register(...)` calls in module
initializers replace the `static {}` block -- the public API is unchanged.

**Future consideration -- registry lifecycle:** registration is expected during framework
bootstrap, not during execution. If runtime engine registration is never required, the registry
can later be frozen after initialization (e.g., wrapping with `Collections.unmodifiableMap`)
to prevent accidental mutation. Not necessary for this phase, but worth tracking: registries
are typically populated during framework bootstrap. If runtime registration is never required,
making the registry immutable after initialization is the natural next step.

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

Introduce a new package-private utility class `LocatorRoles` (same package as
`SearchableDropdown` and `SearchField`). This is a locator-role concern, not an element
reflection concern, so it belongs in its own class rather than growing `ElementSupport`:

```java
final class LocatorRoles {

    static record RoleEntry(ElementRole role, String key) {}

    static RoleEntry role(ElementRole role, String key) {
        return new RoleEntry(role, key);
    }

    static Map<ElementRole, String> roleMap(RoleEntry... roles) {
        Map<ElementRole, String> result = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RoleEntry r : roles) {
            if (r.key() != null && !r.key().isBlank() && seen.add(r.key())) {
                result.put(r.role(), r.key());
            }
        }
        return result;
    }

    private LocatorRoles() {}
}
```

`RoleEntry` is nested inside `LocatorRoles` so it never escapes the class. It is an
implementation detail of the deduplication helper, not a standalone framework type.

`SearchableDropdown.getAllLocatorRoles()` becomes:
```java
@Override
default Map<ElementRole, String> getAllLocatorRoles() {
    return LocatorRoles.roleMap(
        LocatorRoles.role(ElementRole.TRIGGER,       getTriggerLocator()),
        LocatorRoles.role(ElementRole.SEARCH_INPUT,  getSearchInputLocator()),
        LocatorRoles.role(ElementRole.SEARCH_BUTTON, getSearchButtonLocator()),
        LocatorRoles.role(ElementRole.LIST,          getListLocator())
    );
}
```

Adding `SEARCH_CLEAR_BUTTON` is one new `role(...)` line at the end. No overload to add,
no arity limit, one implementation forever. Dedup is automatic.

`SearchField.getAllLocatorRoles()` -- same pattern.

**Why a record over `Object...`:** the varargs element is now `RoleEntry`, not `Object`.
The compiler guarantees that every varargs element is a `RoleEntry`. Invalid argument
ordering (such as alternating `ElementRole` and `String` values) is no longer possible
because the `(role, key)` pairing is encapsulated by the record. No runtime casts.

**Why a record over typed overloads:** overloads bound arity. Adding a fifth role would
require either a new overload or switching to varargs anyway. The record keeps the call site
equally readable, stays compile-time safe, and supports any number of roles with one
implementation.

**Why not a builder:** a builder is a new public type with its own lifecycle. The record +
varargs combination is package-private, inline at the call site, and reads as a declaration.
If a builder is ever needed (conditional inclusion, dynamic role sets), this is the prototype.

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

Classify each call site into one of four categories:

| Category | Example | Replacement |
|----------|---------|-------------|
| Boolean check | `Via.isClickable(e)` | `e instanceof Clickable` or `e instanceof Clickable c` (pattern) |
| Cast for immediate use | `Via.clickable(e).click()` | `((Clickable) e).click()` or pattern match -- whichever is more readable at the call site |
| Dynamic / unknown at compile time | `Via.cast(e, capabilityClass)` | keep one generic helper |
| Locator descriptor | `Via.descriptor(e)` | **out of scope -- do not inline or remove** |

`Via` currently contains three active `descriptor(...)` methods that resolve
`LocatorDescriptor` objects. These are not capability dispatchers and are not part of this
phase. Leave them untouched. Category 1, 2, and 3 apply only to the capability-related
methods (`isXxx`, `xxx(element)`). Deprecated Selenium helpers (`locator(...)`,
`webElement(...)`) may be removed separately in a future cleanup phase but are also out of
scope here.

**Step 2 — eliminate category 1 and 2 call sites** — inline the `instanceof` or cast directly.
These are one-liners; the `Via` wrapper adds no value.

**Step 3 — reduce `Via` to at most one generic method** (or delete the capability section):

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
| `elements/api/capability/LocatorRoles.java`             | **NEW** -- package-private `RoleEntry` record, `role()` factory, `roleMap()` helper |
| `elements/api/capability/SearchableDropdown.java`       | `getAllLocatorRoles()` uses `LocatorRoles.roleMap(…)` |
| `elements/api/capability/SearchField.java`              | `getAllLocatorRoles()` uses `LocatorRoles.roleMap(…)` |
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

Confirm no leftover `Via.*` call sites across the codebase:
```
grep -rn "Via\." src/
# must return zero results (or only the generic cast method if category-3 sites exist)
```

Confirm no per-capability methods remain in `Via` (if kept):
```
grep -n "Clickable\|Typeable\|Selectable\|Uploadable\|ReadOnly\|Listable" \
  src/main/java/core/interactions/Via.java
# must return zero results
```
