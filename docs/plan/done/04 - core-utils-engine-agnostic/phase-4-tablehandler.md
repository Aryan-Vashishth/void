# Phase 4 -- TableHandler Migration and Deprecation

**Status:** Complete
**Commit:** `c7e7a06 refactor(utils): switch TableHandler to resolveDescriptor(); deprecate all public methods`

---

## Goal

Switch the internal locator resolution path from the deprecated `LocatorResolver.resolve()`
(returns `By`) to `resolveDescriptor()` (returns `LocatorDescriptor`), and deprecate all
public methods and the class. Resolves I1-B (ADR-007 violation, resolver path migration).

---

## Changes

### `core/utils/web/TableHandler.java`

**Imports added:**

```java
import core.engine.LocatorDescriptor;
import core.engine.selenium.SeleniumEngine;
```

**Internal resolution path fixed in all three methods:**

Before:
```java
By headersBy = LocatorResolvers.strict().resolve(LocatorRequest.of(...));
List<WebElement> headers = driver.findElements(headersBy);
```

After:
```java
LocatorDescriptor headersDescriptor = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(...));
By headersBy = SeleniumEngine.toBy(headersDescriptor);
List<WebElement> headers = driver.findElements(headersBy);
```

The `driver.findElements(By)` call remains -- removing it requires engine support for
element-list reads (`getTextList(LocatorDescriptor)`), which is out of scope for this
initiative (no active callers, no concrete use case yet).

**Methods and class deprecated:**

| Target | Annotation |
|---|---|
| `TableHandler` class | `@Deprecated(forRemoval = true)` |
| `insertRowInTable(Map, TableElementV1)` | `@Deprecated(forRemoval = true)` |
| `getColumnHeaders(TableElementV1)` | `@Deprecated(forRemoval = true)` |
| `getRow(TableElementV1, Integer, Map, boolean)` | `@Deprecated(forRemoval = true)` |

`TableElementV1` interface is not deprecated separately -- it is package-private to
`TableHandler` and has no independent callers.

---

## Why `SeleniumEngine.toBy()` is acceptable here

`SeleniumEngine.toBy(LocatorDescriptor)` converts a `LocatorDescriptor` to a Selenium
`By`. Using it inside a `@Deprecated(forRemoval=true)` class is consistent -- the entire
class is Selenium-coupled and scheduled for removal. The explicit bridge call makes the
Selenium dependency visible rather than hiding it inside the deprecated resolver path.

---

## What is NOT fixed

`driver.findElements(By)` remains in all three methods. Full removal requires:
1. `UIEngine.getTextList(LocatorDescriptor)` -- for header and row text reads
2. A scoped query model -- for `row.findElements(By.xpath("./td"))` cell reads

Both are deferred: no active callers exist, and the architecture rule against
premature abstractions applies (Stability Rule 4 in CLAUDE.md).

---

## Verification

```
mvn compile -q
grep -n "\.resolve(" src/main/java/core/utils/web/TableHandler.java
grep -n "@Deprecated" src/main/java/core/utils/web/TableHandler.java
```

`resolve(` returns no matches (only `resolveDescriptor(` remains).
Four `@Deprecated` annotations (one on class, three on methods). Compile passes clean.
