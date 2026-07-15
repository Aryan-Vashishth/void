# Phase 3 — Interactions: Remove Unsafe Cast and SeleniumEngine Import

Violations: **V4**
Touches: `Interactions.java`

---

## Goal

`Interactions` is a frozen legacy class. Its constructor currently registers a `WebDriver` in
`DriverContext` by casting `engine.getNativeDriver()` — a cast that throws `ClassCastException`
for any non-Selenium engine. After this phase:
- The `Interactions(UIEngine)` constructor is safe for any engine.
- `Interactions.java` does not import `SeleniumEngine` (a concrete implementation).
- All remaining `By`/`WebElement` bridge methods are left in place — they are deprecated but
  their removal is not in scope here.

---

## V4 — `Interactions(UIEngine)` casts `getNativeDriver()` to `WebDriver`

### Problem

```java
// Interactions.java:65–69
public Interactions(UIEngine engine) {
    this.engine = engine;
    // Backward compat: register native driver in DriverContext for legacy paths
    DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver());
}
```

`getNativeDriver()` returns `Object`. The cast to `WebDriver` is correct for
`SeleniumEngine` but throws `ClassCastException` for any other engine at the moment
`new Interactions(engine)` is called — before any interaction runs. The comment says
"backward compat", but it is actually a correctness hazard that would block hotswapping.

### Why this registration is no longer needed

After Phase 2, `SeleniumEngine.initialize()` calls `DriverContext.setPrimaryDriver(driver)`.
By the time `VOID.interaction()` creates an `Interactions` instance, the driver is already
registered. The `Interactions` constructor is doing redundant double-registration work.

The only scenario where this mattered was the pre-Phase-1 path where `VOID` held a raw
`WebDriver` and `Interactions` was constructed before `DriverContext` had a primary driver.
That construction path no longer exists after Phase 2.

### Fix

Remove the cast entirely. The `Interactions(UIEngine)` constructor becomes:

```java
public Interactions(UIEngine engine) {
    this.engine = engine;
}
```

No `DriverContext` call. No cast. Safe for any `UIEngine` implementation.

**`VOID.java` — `interaction()` is unaffected:**
```java
@Deprecated(since = "0.1", forRemoval = true)
public Interactions interaction() {
    if (interactions == null) {
        interactions = new Interactions(engine);   // now safe for any engine
    }
    return interactions;
}
```

---

## Remove `SeleniumEngine` import from `Interactions.java`

### Problem

```java
// Interactions.java:5
import core.engine.selenium.SeleniumEngine;
```

`Interactions` imports a concrete engine implementation. This appears in two call sites:

**1. `Interactions(WebDriver)` deprecated constructor (line 79–81):**
```java
@Deprecated(forRemoval = true)
public Interactions(WebDriver driver) {
    this(new SeleniumEngine(driver));
}
```

**2. `SeleniumEngine.fromBy()` calls in deprecated bridge methods (lines 161, 254, 384, 622, 821, 829):**
```java
LocatorDescriptor descriptor = SeleniumEngine.fromBy(locator);
```

### Fix — `SeleniumEngine.fromBy()` moves to a neutral location

`SeleniumEngine.fromBy(By)` converts a Selenium `By` into a `LocatorDescriptor`. It is a
pure conversion utility — it does not touch any engine instance. Its current home in
`SeleniumEngine` is convenient but conceptually wrong: a conversion from `By` to
`LocatorDescriptor` should not require importing the engine implementation.

Move it to `LocatorDescriptor` as a static factory, gated behind a Selenium-specific import:

**`LocatorDescriptor.java` — add static bridge (only for deprecated paths):**
```java
/**
 * Creates a descriptor from a Selenium {@link org.openqa.selenium.By} locator.
 * For use in deprecated By-based bridge methods only. Do not add new call sites.
 *
 * @deprecated Selenium-specific. Use string-based descriptors or element-based resolution.
 */
@Deprecated(forRemoval = true)
public static LocatorDescriptor fromBy(org.openqa.selenium.By by) {
    String raw = by.toString();
    // By.toString() format: "By.cssSelector: .foo" or "By.xpath: //div"
    int colon = raw.indexOf(':');
    if (colon < 0) return LocatorDescriptor.of(raw);
    String type  = raw.substring(0, colon).trim().toLowerCase(Locale.ROOT);
    String value = raw.substring(colon + 1).trim();
    LocatorStrategy strategy = switch (type) {
        case "by.id"              -> LocatorStrategy.ID;
        case "by.cssselector"     -> LocatorStrategy.CSS;
        case "by.xpath"           -> LocatorStrategy.XPATH;
        case "by.name"            -> LocatorStrategy.NAME;
        default                   -> LocatorStrategy.CSS;
    };
    return new LocatorDescriptor(value, strategy);
}
```

**Verify `SeleniumEngine.fromBy()` implementation** first — if it already does the same
parsing, replicate the logic. If it delegates to `ByParser`, the `LocatorDescriptor` bridge
can parse via `ByParser` and then infer strategy from the returned `By` type.

**Alternative if moving `fromBy` is too invasive:** keep `SeleniumEngine.fromBy()` where
it is, but replace the `Interactions` import with a direct call to `ByParser.DEFAULT.parse()`
+ `LocatorDescriptor.of(by.toString())`. The deprecated bridge methods in `Interactions` are
already `@Deprecated(forRemoval=true)` — they only need to compile, not be clean.

The **minimum acceptable fix for this phase** is:

1. Remove the `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver())` line.
2. If `SeleniumEngine.fromBy()` is only used for the deprecated By-bridges and nothing else,
   leave the import in place with a note that it will be removed when the bridge methods are
   deleted. The import is an aesthetic issue; the cast is a correctness issue.

The cast is the P0. The import is cleanup. Do not block Phase 3 on the `fromBy` refactor
if it adds scope.

### Fix — `Interactions(WebDriver)` deprecated constructor

The deprecated `Interactions(WebDriver)` constructor uses `SeleniumEngine(WebDriver)` which
is itself marked `@Deprecated(forRemoval=true)` after Phase 1:

```java
@Deprecated(forRemoval = true)
public Interactions(WebDriver driver) {
    this(new SeleniumEngine(driver));   // SeleniumEngine(WebDriver) is deprecated — compiles with warning
}
```

This is acceptable. Both are deprecated, both compile. No change needed here.

---

## Remaining `By`/`WebElement` deprecated methods

`Interactions` contains approximately 12 deprecated methods with `By` or `WebElement`
parameters. They are left unchanged in this phase. They are already `@Deprecated(forRemoval=true)`.
Their removal is a future workstream, not part of engine decoupling.

Examples left intact:
- `clickOn(By locator)` — line 253
- `clickOn(WebElement element)` — line 287
- `typeInto(By locator, String text)` — line 621
- `isAnyDisplayed(By locator, Duration timeout, Duration poll)` — line 820

These do not affect engine hotswapping. They use the deprecated `SeleniumEngine.fromBy()`
bridge which is Selenium-internal. A Playwright session would never call these methods via
the `Interactions` API.

---

## Files changed

| File                                      | Change                                                                                        |
|-------------------------------------------|-----------------------------------------------------------------------------------------------|
| `core/interactions/Interactions.java`     | Remove `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver())` from constructor; optionally remove `SeleniumEngine` import if `fromBy` is relocated |
| `core/engine/LocatorDescriptor.java`      | Add `@Deprecated static fromBy(By)` if `SeleniumEngine.fromBy()` is relocated (optional for this phase) |

---

## Commits

```
fix(interactions): remove unsafe WebDriver cast from Interactions constructor
chore(interactions): remove SeleniumEngine import from Interactions
```

Second commit is conditional on whether `fromBy` is relocated. If the import is kept
temporarily, note it in the commit message:

```
chore(interactions): note SeleniumEngine import retained pending fromBy relocation
```

---

## Verification

```
mvn compile -q

grep -n "getNativeDriver" src/main/java/core/interactions/Interactions.java   # must be empty
grep -n "WebDriver.*cast\|cast.*WebDriver\|(WebDriver)" src/main/java/core/interactions/Interactions.java
# must be empty (the deprecated WebDriver constructor uses the type, not a cast — that is OK)
```

Manual check: confirm `VOID.interaction()` still returns a usable `Interactions` object
after removing the constructor side-effect:
```
mvn test -Dtest=VoidDemo -q
```
