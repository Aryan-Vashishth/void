# Phase 2 -- Engine Contract Decoupling

Touches: `core/engine/UIEngineFactory.java`, `core/engine/EngineBootstrap.java`,
`core/engine/selenium/SeleniumEngine.java`, `core/runtime/VOIDBuilder.java`.

Coordinates with: `oop-violations-remediation` Phase 4 (P8). Whichever initiative
lands first owns the registry change; the other cites it as done.

---

## Goal

`core.engine` becomes a pure contract package: zero imports of `core.engine.selenium`
and zero imports of `core.driver`. This is the code-level enforcement of audit
findings D2 and D3 and of ADR-021 invariant I1 on the engine axis, and it retires the
`EngineBootstrap` design debt acknowledged in ADR-018.

Two changes:

1. **Engine registry (D2 / P8).** `UIEngineFactory` no longer `switch`es on the engine
   name string with a compile-time `new SeleniumEngine(...)`. Engines register into a
   registry map keyed by engine name; the Selenium registration lives with
   `core.engine.selenium`, not with the factory. Unknown names fail fast with the
   list of registered engines in the error message.

2. **EngineBootstrap decoupling (D3).** `EngineBootstrap` stops carrying
   `DriverFactory.Profile`. It carries the engine name plus opaque, engine-owned
   settings (`Properties` view). `SeleniumEngine` derives its own
   `DriverFactory.Profile` from those settings internally; the profile type never
   crosses the contract boundary. This also removes the ADR-018 noted debt where
   `resolvedConfig()` injects the engine name into a Properties copy.

Design details (registration mechanism, static vs instance registry, how
`VOIDBuilder` triggers Selenium registration without importing it eagerly) are decided
at implementation time within the constraint above; if the decision is non-obvious it
goes into ADR-021's consequences section, not a new ADR.

---

## What does NOT change in this phase

- `UIEngine` interface methods
- `EngineConfig`
- `LocatorDescriptor`, `LocatorStrategy` (Phase 4)
- `DriverFactory`, `DriverContext`, `DriverManager` internals -- only who references
  their types
- `SeleniumEngine` execution logic -- constructor/bootstrap path only

---

## Files changed

| File | Change |
|------|--------|
| `core/engine/UIEngineFactory.java` | switch removed; registry lookup |
| `core/engine/EngineBootstrap.java` | drops `DriverFactory.Profile`; carries name + opaque settings |
| `core/engine/selenium/SeleniumEngine.java` | owns profile derivation; hosts/performs its registration |
| `core/runtime/VOIDBuilder.java` | passes engine name + settings; no behavior change for callers |

---

## Commits

```
feat(engine): replace UIEngineFactory switch with engine registry
refactor(engine): EngineBootstrap carries engine name and opaque settings, drops DriverFactory.Profile
```

---

## Verification

```
mvn compile -q
mvn test -q

# Contract package purity
grep -rn "core.driver" src/main/java/core/engine --include=*.java | grep -v "engine\\selenium"
# expected: empty

grep -rn "engine.selenium" src/main/java/core/engine/UIEngineFactory.java src/main/java/core/engine/EngineBootstrap.java
# expected: empty

grep -n "switch" src/main/java/core/engine/UIEngineFactory.java
# expected: empty
```

---

## Phase complete when

- [ ] `core.engine` (excluding the `selenium` subpackage) imports neither
      `core.engine.selenium` nor `core.driver`.
- [ ] Registering a hypothetical second engine requires no edit to any file in
      `core.engine`.
- [ ] `mvn compile -q` and existing tests pass.
- [ ] P8 marked resolved in the violations index (coordinated with
      `oop-violations-remediation`).
