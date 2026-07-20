---
name: oop-driverfactory-instanceof-preference-dispatch
description: DriverFactory uses a 3-branch instanceof chain to route Firefox preference types -- OCP violation
metadata:
  type: project
---

# OOP: DriverFactory -- instanceof Dispatch for Firefox Preference Routing

**Principle:** OCP (Open-Closed Principle)
**File:** `src/main/java/core/driver/DriverFactory.java`
**Lines:** 722-724
**Discovered:** 2026-07-20 (architecture-rules.md audit on hotfix/engine-decoupling-final-audit)
**Risk:** Low

## What it does

A three-branch `instanceof` chain dispatches a Firefox preference value to one of three
`addPreference()` overloads based on the runtime type of the value:

```java
if (v instanceof Boolean b)  opts.addPreference(k, b);
else if (v instanceof Integer i) opts.addPreference(k, i);
else if (v instanceof String s)  opts.addPreference(k, s);
```

## Why this is a violation

The chain is open-ended: if Firefox adds a new preference type (e.g. `Double`), the
framework code must be modified to handle it. This violates OCP -- the class is not
closed for modification on this axis.

The pattern is also inconsistent with the architecture's general rejection of
`instanceof` dispatch chains (CLAUDE.md architecture invariants).

## Why risk is Low

The Firefox preference API has exactly three value types (`boolean`, `int`, `String`) and
has been stable for many years. The chain will not grow in practice. No behavioral
consequence if a fourth type is silently ignored -- it simply does not set the preference.

## Recommended fix

Extract a small sealed hierarchy or use a `Map<Class<?>, BiConsumer<FirefoxOptions, Object>>`
dispatch table keyed by type. Alternatively, if the three-type constraint is a permanent
Firefox API invariant, document it explicitly and suppress the finding with a comment
referencing this file.

**Estimated cost:** Minimal. Fix inline with a dedicated commit when touching `DriverFactory`
for any other reason. Do not prioritize independently.
