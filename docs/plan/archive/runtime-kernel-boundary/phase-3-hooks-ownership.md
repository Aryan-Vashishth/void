# Phase 3 -- Hooks Ownership

Touches: new kernel-owned hook package (target name decided in this phase, e.g.
`core/hooks/`), `core/interactions/hooks/*` (become deprecated bridges), import
updates in `core/actions/*`, `core/engine/selenium/SeleniumEngine.java` (if it
references hook types), demo/test sources.

---

## Goal

Resolve audit finding D4: the stable, engine-agnostic hook contract
(`ActionHandler`, `BeforeActionHandler`, `AfterActionHandler`, `Before`, `After`)
no longer lives inside the deprecated `core.interactions` package, and the kernel no
longer imports through the legacy zone.

Compatibility constraint: the hook API is Stable tier ("no breaking changes"). The
move therefore keeps the old types alive as deprecated bridges:

- New home owns the real contract and constant libraries.
- Old `core.interactions.hooks.ActionHandler` becomes
  `@Deprecated interface ActionHandler extends <new>.ActionHandler {}` -- existing
  lambdas and implementations keep compiling, and old instances are accepted anywhere
  the new type is required.
- Old `Before`/`After` constant classes delegate to the new ones and are
  `@Deprecated` (not `forRemoval` until the legacy removal workstream).

After this phase, `grep -rn "core.interactions" src/main/java/core/actions` is empty:
the kernel has no import path through the legacy package.

The hook signature itself, `(UIEngine engine, LocatorDescriptor descriptor)`, is NOT
generalized here. Its domain-axis typing is recorded in ADR-021 as future watch (it
generalizes together with `Action.perform`).

---

## What does NOT change in this phase

- Hook semantics, ordering guarantees, null-descriptor behavior in legacy paths
- `ActionProfile` / `ActionProfiles` contents (only imports)
- `Interactions` and `Via` (untouched; they may keep using the old bridge types)
- The `ActionHandler` functional signature

---

## Files changed

| File | Change |
|------|--------|
| NEW kernel hook package (5 files) | Real contract + constant libraries |
| `core/interactions/hooks/*` (5 files) | Deprecated bridges extending/delegating to new types |
| `core/actions/*` (imports) | Point at the new package |
| Demo/test sources | Import updates |

---

## Commit

```
refactor(hooks): move hook contract to kernel-owned package, keep deprecated bridges
```

---

## Verification

```
mvn compile -q
mvn test -q

grep -rn "core.interactions" src/main/java/core/actions --include=*.java
# expected: empty

grep -rln "interactions.hooks" src/main/java | grep -v "core/interactions"
# expected: empty (only legacy package and bridges reference the old path)
```

---

## Phase complete when

- [ ] Kernel (`core.actions`, `core.flow`, `core.executor`) has zero imports from
      `core.interactions.*`.
- [ ] Old hook types still compile against existing user code (bridge pattern
      verified by a test implementing the old interface and passing it to the new
      API).
- [ ] `mvn compile -q` and existing examples pass.
