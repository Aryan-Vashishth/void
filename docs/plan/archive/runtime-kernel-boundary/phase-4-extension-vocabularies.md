# Phase 4 -- Extension Vocabularies

Touches: `core/actions/ActionCapability.java`, `core/actions/ActionProfiles.java`,
`core/engine/LocatorStrategy.java`, `core/resolvers/locator/parser/*` (strategy
consumers), profile-selection call sites.

Depends on: `oop-violations-remediation` Phases 1-3 merged (P3 dispatch removal and
P7 capability ownership must stabilize the capability surface first). This is the last
phase for that reason.

---

## Goal

Resolve audit finding D18 and ontology invariant I11: the two vocabularies the runtime
currently hardcodes become open sets that a domain or engine can extend without
editing runtime-owned types. This is what the June 2026 domain-agnostic audit ranked
as "what cracks first" when a second domain arrives.

Three changes:

1. **`ActionCapability` opens.** The closed enum
   (CLICKABLE, TYPEABLE, SELECTABLE, ..., UNKNOWN) becomes an extensible capability
   set (interface-with-constants shape, per the June audit's migration note). Existing
   constants keep their identities so profiles and logs are unchanged for current
   callers.

2. **`UNKNOWN` silent fallback removed.** Today an unrecognized capability silently
   inherits browser wait hooks via the default profile: wrong behavior made invisible.
   After this phase, profile selection for a capability with no configured default is
   explicit: either the action declares its profile or configuration names one;
   otherwise fail fast with a clear message. No silent inheritance across capability
   families.

3. **`LocatorStrategy` opens.** Same treatment: the closed enum (XPATH, CSS, ID, NAME)
   becomes extensible so a future engine or domain can register strategies without
   editing `core.engine`. `ByParser` prefix handling maps onto the open set;
   unrecognized prefixes keep failing fast.

Each change follows the Architectural Stability Rules checklist: these are not new
abstractions, they are existing abstractions having their closed-set constraint
removed, driven by three prior audits rather than speculation.

---

## What does NOT change in this phase

- Existing capability and strategy constant names and semantics
- `ActionProfile` interface
- Concrete action classes (they keep declaring the same capabilities)
- Locator file formats and prefix tokens
- Hook contract (Phase 3 owns that)

---

## Files changed

| File | Change |
|------|--------|
| `core/actions/ActionCapability.java` | Enum to open set; constants preserved |
| `core/actions/ActionProfiles.java` | Default-profile lookup keyed by capability; no UNKNOWN fallback |
| `core/engine/LocatorStrategy.java` | Enum to open set; constants preserved |
| `core/resolvers/locator/parser/*` | Strategy consumers use the open set |
| Profile-selection call sites | Explicit failure path for unconfigured capabilities |

---

## Commits

```
feat(actions): open ActionCapability to an extensible capability set
feat(engine): open LocatorStrategy to an extensible strategy set
fix(actions): remove UNKNOWN silent hook fallback, require explicit default profile
```

---

## Verification

```
mvn compile -q
mvn test -q

# No switch/enumeration over the vocabularies survives in runtime-owned code
grep -rn "switch (ActionCapability" src/main/java --include=*.java
grep -rn "switch (LocatorStrategy" src/main/java --include=*.java
# expected: empty (or confined to @Deprecated legacy paths)

# UNKNOWN fallback gone
grep -rn "UNKNOWN" src/main/java/core/actions --include=*.java
# expected: no profile-selection use

# Extension test: a test-scope custom capability + custom strategy register and
# round-trip without editing core.actions or core.engine
mvn test -Dtest=*VocabularyExtension* -q
```

---

## Phase complete when

- [ ] A test-scope custom capability with its own profile works with zero edits to
      runtime-owned files.
- [ ] A test-scope custom locator strategy parses and resolves with zero edits to
      `core.engine`.
- [ ] Unconfigured capability profile selection fails fast instead of silently
      inheriting browser hooks.
- [ ] `mvn compile -q` and existing examples pass.
