# Phase 1 — Action Layer: Extension Hooks and Label Promotion

Violations: **P1**, **P3**, **P4**
Deletes: `ActionLabeled.java`, `HookedAction.java`

---

## Goal

Remove every `instanceof` and `switch`-on-type from `core/actions/`. After this phase:
- `Action` default methods never reference a concrete class.
- Wrappers delegate label methods directly without a type check.
- Adding a new `Action` wrapper or a new `ActionCapability` constant requires zero changes to
  existing classes.

---

## P1 — `Action.java`: `instanceof HookChainAction` in four default methods

### Problem

`before()`, `after()`, `using()`, and `withHooks()` all contain:
```java
if (this instanceof HookChainAction chain) {
    return chain.withAdditionalHooks(...);
}
return new HookChainAction(this, ...);
```
`Action` — the root abstraction — imports and type-checks a concrete implementation. Any new
composable wrapper (`RetryAction`, `TimedAction`) requires modifying all four methods.

### Fix

Introduce **two extension hooks** — one for hook merging, one for profile attachment.
`Action` itself never references `HookChainAction` again.

**`Action.java` — hook-merging extension point:**
```java
default Action mergeHooks(List<BeforeActionHandler> before, List<AfterActionHandler> after) {
    return new HookChainAction(this, before, after);
}
```

**`Action.java` — profile-attachment extension point:**
```java
default Action attachProfile(Profile profile) {
    return new HookChainAction(this).withProfileName(profile.name());
}
```

**`HookChainAction.java` — override both to avoid re-wrapping:**
```java
@Override
public Action mergeHooks(List<BeforeActionHandler> before, List<AfterActionHandler> after) {
    return withAdditionalHooks(before, after);
}

@Override
public Action attachProfile(Profile profile) {
    return withProfileName(profile.name());
}
```

**`Action.java` — rewrite the four methods using empty lists, not null:**
```java
default Action before(BeforeActionHandler... hooks) {
    return mergeHooks(toList(hooks), Collections.emptyList());
}

default Action after(AfterActionHandler... hooks) {
    return mergeHooks(Collections.emptyList(), toList(hooks));
}

default Action using(Profile profile) {
    return attachProfile(profile);
}

default Action withHooks(List<BeforeActionHandler> before, List<AfterActionHandler> after) {
    return mergeHooks(before, after);
}
```

No casts. No `instanceof`. No implementation knowledge in the base interface.
`mergeHooks` implementations never need a null-guard because the callers pass
`Collections.emptyList()` instead.

**Why two hooks instead of one?**

`mergeHooks` owns hook list composition — a generic wrapping concern any action type could
participate in. `attachProfile` owns profile storage — a detail specific to wrapper state that
different implementations may store differently. Collapsing them into one method would force
a `HookChainAction` assumption into `mergeHooks`'s contract.

**Extension test:** `RetryAction` wraps any action. It overrides `mergeHooks` to preserve
its retry count when hooks are added:
```java
@Override
public Action mergeHooks(List<BeforeActionHandler> before, List<AfterActionHandler> after) {
    return new RetryAction(delegate.mergeHooks(before, after), retryCount);
}
```
Zero changes to `Action`.

---

## P3 + P4 — Label method promotion (do together)

### Problem — P4

`HookChainAction` and `HookedAction` both contain:
```java
public String elementLabel() {
    if (delegate instanceof ActionLabeled l) return l.elementLabel();
    return "ACTION";
}
public String operationLabel() {
    if (delegate instanceof ActionLabeled l) return l.operationLabel();
    return switch (capability()) { ... }; // P3
}
```
Any action that provides a label through a mechanism other than `ActionLabeled` gets `"ACTION"` /
`"perform"` silently. The two-tier label system degrades trace output for new action types.

### Problem — P3

The `switch (capability())` inside `operationLabel()` is the fallback when `delegate` is not
`ActionLabeled`. Every new `ActionCapability` constant falls through to `"perform"`. There is
no compile-time warning.

### Root cause

`elementLabel()` and `operationLabel()` are not part of the `Action` contract, so wrappers
cannot call them on `delegate` without a cast.

### Fix

**`Action.java` — add defaults:**
```java
default String elementLabel()   { return "ACTION"; }
default String operationLabel() { return getClass().getSimpleName(); }
```

`elementLabel()` keeps `"ACTION"` — the existing fallback — so no logging changes for callers
that already see this value.

`operationLabel()` uses `getClass().getSimpleName()` rather than `"perform"`. If a developer
adds a new action class and forgets to override `operationLabel()`, seeing `"TimedAction"` in
the trace immediately reveals the missing override. `"perform"` would have hidden the gap.

**Concrete action classes — add one-liner overrides:**

| Class                | `operationLabel()` return |
|----------------------|--------------------------|
| `ClickAction.java`   | `"click"`                |
| `TypeAction.java`    | `"type"`                 |
| `SelectAction.java`  | `"select"`               |
| `UploadAction.java`  | `"upload"`               |
| `ReadTextAction.java`| `"read"`                 |

`ElementAction.java` already has `elementLabel()` implemented — no change needed.

**`HookChainAction.java` — remove both casts and the switch:**
```java
@Override
public String elementLabel() {
    return delegate.elementLabel();
}

@Override
public String operationLabel() {
    return delegate.operationLabel();
}
```
No `instanceof ActionLabeled`. No `switch`. Delegating directly through the `Action` contract.

**Delete `ActionLabeled.java`** — the interface is now empty of purpose. No class should
implement it going forward.

**Delete `HookedAction.java`** — it is already `@Deprecated(forRemoval = true, since = "0.2")`.
Phase 1 removes the last non-trivial pattern it contained (`instanceof ActionLabeled`). Remove
any `@deprecated` Javadoc cross-references in `Action.java` that point to `HookedAction`.

**Extension test:** `TimedAction` wraps any action and adds duration tracking. It is not
`ActionLabeled`. After Phase 1:
```java
@Override public String elementLabel()   { return delegate.elementLabel(); }
@Override public String operationLabel() { return delegate.operationLabel(); }
```
Labels flow through the chain with no interface to implement, no cast, no fallback logic.

---

## Future watch — is `HookChainAction` becoming `ActionDecorator`?

After Phase 1, `HookChainAction` holds hooks and a profile name. If over time it accumulates
retry state, timeout, metrics, or other cross-cutting concerns, it will no longer be a
hook chain — it will be the universal wrapper. Worth monitoring. If that happens, rename it
and make the decorator role explicit rather than letting the name lie.

Not something to change now, but a signal to watch.

---

## Files changed

| File                               | Change                                                   |
|------------------------------------|----------------------------------------------------------|
| `core/actions/Action.java`         | Add `mergeHooks`, `attachProfile`; add `elementLabel`/`operationLabel` defaults; rewrite 4 hook methods |
| `core/actions/HookChainAction.java`| Override `mergeHooks`, `attachProfile`; rewrite `elementLabel`/`operationLabel` (no cast, no switch) |
| `core/actions/ClickAction.java`    | `operationLabel() { return "click"; }`                   |
| `core/actions/TypeAction.java`     | `operationLabel() { return "type"; }`                    |
| `core/actions/SelectAction.java`   | `operationLabel() { return "select"; }`                  |
| `core/actions/UploadAction.java`   | `operationLabel() { return "upload"; }`                  |
| `core/actions/ReadTextAction.java` | `operationLabel() { return "read"; }`                    |
| `core/actions/ActionLabeled.java`  | **DELETE**                                               |
| `core/actions/HookedAction.java`   | **DELETE**                                               |

---

## Commits

```
feat(actions): add mergeHooks and attachProfile extension hooks, remove instanceof HookChainAction from Action defaults
feat(actions): promote elementLabel/operationLabel to Action, remove ActionLabeled
chore(actions): delete HookedAction (deprecated since 0.2, superseded by HookChainAction)
```

---

## Verification

```
mvn compile -q
```

Then grep for residual references:
```
grep -r "ActionLabeled" src/
grep -r "HookedAction"  src/
```
Both must return zero results.
