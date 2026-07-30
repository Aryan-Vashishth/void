# I7 Pre-Implementation Audit -- Locator Generalization

**Initiative:** I7 Locator Generalization (phases 7.1, 7.2, 7.3)
**Branch:** `initiative/locator-generalization`
**Status:** READY
**Audited against:** main @ v0.7.0 (commit 20cf57f)

---

## Scope

Three phases:

| Phase | Objective |
|---|---|
| 7.1 | Open the strategy set (`LocatorStrategy` extensible; `ByParser` prefix table generalized) |
| 7.2 | Descriptor ownership moved to web/UI area (out of `core.engine`) |
| 7.3 | Delete the By-returning resolution path from `LocatorResolver` |

---

## Findings

### A1: `LocatorStrategy` is a closed enum -- audit D18 (7.1)

`core/engine/LocatorStrategy.java`

```java
public enum LocatorStrategy { XPATH, CSS, ID, NAME; }
```

Four constants only. Adding a strategy (e.g., LINK_TEXT, ACCESSIBILITY_ID for Appium) requires
editing a framework-owned type. Audit D18 ("second closed vocabulary") is this finding.

`infer(String)` only recognises XPath-shaped strings; all others default to CSS. ID
shorthand (`#`) is documented in the Javadoc but not implemented -- `#id` falls through to
CSS, which is correct behavior (`By.cssSelector("#id")` works); the Javadoc is misleading.

**Action (7.1):** Replace the enum with an open, extensible type. See D1 below.

---

### A2: Strategy/prefix coverage gap (7.1 observation)

`ByParser.DEFAULT_STRATEGIES` registers 8 prefix strategies:

```
id=, name=, class=, tag=, linktext=, partiallinktext=, css=, xpath=
```

`LocatorStrategy` has 4 constants. The four extra prefixes (`class=`, `tag=`, `linktext=`,
`partiallinktext=`) exist only in the By-returning pipeline. In the descriptor pipeline
(`resolveDescriptor`), `LocatorResolver.inferStrategy()` recognises only `xpath=`, `css=`,
`id=`, `name=` -- so `class=my-class` enters `inferStrategy` without matching any explicit
prefix, falls through to `LocatorStrategy.infer()`, returns CSS, and
`stripPrefix` strips `class=` -- producing descriptor(`my-class`, CSS). SeleniumEngine
then calls `By.cssSelector("my-class")` instead of `By.className("my-class")`.

This is a latent correctness gap in the descriptor pipeline for the four Selenium-specific
prefixes. It is not user-visible today because production locator files use XPath/CSS
directly, not `class=` / `tag=` / `linktext=` / `partiallinktext=` prefix form.

**Action (7.1):** Decide whether these four prefixes need constants in the open set, or
whether they are Selenium-specific aliases that belong exclusively inside
`SeleniumEngine.toBy()`. Given the "open set" goal the correct answer is the latter:
`class=`, `tag=`, `linktext=`, `partiallinktext=` are Selenium capability aliases, not
strategy constants. The descriptor pipeline's gap closes by documenting that those
prefix forms are unsupported in the descriptor path (and removing them from
`LocatorResolver.stripPrefix` accordingly).

---

### A3: `LocatorDescriptor` and `LocatorStrategy` live in `core.engine` (7.2)

```
core/engine/LocatorDescriptor.java   -- UI/DOM concept: locator value, strategy, parent scope, label
core/engine/LocatorStrategy.java     -- UI/DOM concept: XPATH, CSS, ID, NAME
```

`core.engine` is the neutral engine contract package: `Executor`, `UIEngine`,
`UIEngineFactory`, `EngineBootstrap`, `EngineConfig`, `EngineRegistrar`. Both locator
types describe DOM-scoped, web-domain concepts housed in the wrong package.

`LocatorDescriptor` is referenced in approximately 50 files. Key callers:

| Category | Files |
|---|---|
| UIEngine contract | `core.engine.UIEngine` (resolve methods) |
| Engine impl | `core.engine.selenium.SeleniumEngine` (resolve, toBy, findElement) |
| Actions | all `elements.api.actions.*` action classes |
| Utilities | `WaitUtils`, `KeyValuePairHandler`, `TableHandler`, `Upload`, `DOMUtils` |
| DSL | `core.dsl.VoidDSL` |
| Hooks | `core.interactions.hooks.Before`, `After`, `DemoHooks` |
| Resolvers | `LocatorResolver`, `LocatorTemplate`, source chain |

The neutral dispatch path (`Executor`) carries no locator type -- that invariant already
holds. The problem is ownership: these are web-domain nouns housed in the neutral package.

**Intermediate target for 7.2:** `elements.locator` (new sibling to `elements.api`). This
keeps the type visible to everything in `elements.*` and allows `UIEngine` (in `core.engine`)
to import them as web-domain vocabulary without a cycle. The final physical relocation to
`domain.automation.web.*` is I6.4's job and is out of scope here.

Moving to `elements.api` directly creates a coupling: `elements.api.UIElement` already
imports `core.engine.LocatorDescriptor`. Reversing this (UIElement in elements.api,
LocatorDescriptor also in elements.api) removes the cross-module import but tightly couples
the element API with the locator type. A sibling package `elements.locator` isolates
the concern.

See D2 below.

**Action (7.2):** Relocate `LocatorDescriptor` and `LocatorStrategy` to `elements.locator`.
Update all import sites. Add fitness check: `core.engine` (excluding UIEngine's own class
file) must not declare a type named `Locator*`.

---

### A4: By-returning pipeline -- consumer inventory (7.3)

`LocatorResolver` exposes five By-returning methods:

```java
By resolve(LocatorRequest)
By resolve(String fileName, String key, Object... args)
By resolve(UIElement)
By resolve(UIElement, ElementRole, Object...)
By resolveBest(UIElement, Object...)
```

Consumer inventory (non-deprecated, non-legacy):

| Call site | File | Line | Migration |
|---|---|---|---|
| `LocatorResolvers.strict().resolve(element)` | `WaitUtils.java` | 161 | `resolveDescriptor` + `SeleniumEngine.toBy()` |
| `LocatorResolvers.strict().resolve(LocatorRequest...)` | `KeyValuePairHandler.java` | 44 | `resolveDescriptor` + `SeleniumEngine.toBy()` |
| `LocatorResolvers.strict().resolve(fileElement)` | `Upload.java` | 51 | `resolveDescriptor` + `SeleniumEngine.toBy()` |

All three are in `core.utils.web`. All three have a direct migration path: replace
`resolve(...)` with `resolveDescriptor(...)` + `SeleniumEngine.toBy(descriptor)`. This
is the same pattern `TableHandler` already uses (lines 53-54, 85-86, 116-117).

**Deprecated consumers** (not blocking 7.3):
`Via.java` lines 247, 255, 263, 283, 301 -- all `@Deprecated(forRemoval = true)`.
`Via` is part of the frozen compatibility surface deleted in 9.3. Its calls to
`LocatorResolver.resolve()` are frozen, not new callers, and do not block 7.3.

**Verdict:** 7.3 is in scope for this initiative. Three call sites, three identical
migrations. No deferral to 9.x required.

**Action (7.3):** Migrate the three call sites first (one commit), then delete the five
By-returning methods from `LocatorResolver` (second commit). Add fitness check:
`core.resolvers.locator.api.LocatorResolver` has no method returning `org.openqa.selenium.By`.

---

### A5: `ByParser` post-7.3 fate (7.1 / 7.3 concern)

`core/resolvers/locator/parser/ByParser.java`

Currently: prefix recognition table + heuristic → `By`. After 7.3 removes the By-returning
public pipeline, `ByParser` loses its external role. Its remaining consumer is
`SeleniumEngine.toBy()`, which calls `ByParser` implicitly through the old resolve path
today and will call it directly (or equivalently) post-migration.

Options:

| Option | Consequence |
|---|---|
| Move `ByParser` + `ByPrefixStrategy` to `core.engine.selenium` | Acknowledges its web-domain nature; removes Selenium import from `core.resolvers` |
| Keep in `core.resolvers.locator.parser`, retain as SeleniumEngine dependency | Cross-domain import in the resolver package |
| Absorb into `SeleniumEngine` directly | Removes the indirection; ByParser tests go away |

The correct choice aligns with the 7.2 ownership principle: `ByParser` produces Selenium
`By` objects and belongs in the Selenium platform layer, not in the neutral resolver
infrastructure. Option 1 is correct. `ByPrefixStrategy` follows.

**Action (7.1 or 7.3):** Decide and record. Moving `ByParser`/`ByPrefixStrategy` to
`core.engine.selenium` during 7.1 is safe because 7.1 is reworking the strategy surface
anyway; doing it in 7.3 is also valid since that is when the By-returning path is deleted.
Record the decision in the phase doc.

---

### A6: `inferStrategy` / `stripPrefix` coupling in `LocatorResolver` (7.1 observation)

`LocatorResolver` lines 193-211:

- `inferStrategy`: checks `xpath=`, `css=`, `id=`, `name=` (4 prefixes)
- `stripPrefix`: strips `xpath=`, `css=`, `id=`, `name=`, `class=`, `tag=`, `linktext=`,
  `partiallinktext=` (8 prefixes)

These two methods are implicitly paired but handle different prefix sets. After 7.1 opens
the strategy set, this logic needs to become extensible: each strategy registers its own
prefix, and inference + stripping are driven from that registry rather than being
two separate hardcoded tables.

This is a 7.1 internal design decision, not a blocker.

---

## Pre-Implementation Decisions

### D1: Extensibility mechanism for `LocatorStrategy`

Three candidates:

| Candidate | Tradeoff |
|---|---|
| Interface + constants (same pattern as `ActionCapability` in I3) | Open set; constants survive as typed references; no enum methods to lose |
| String-keyed registry (strategies as named strings) | Maximum openness; loses type safety at call sites |
| Sealed interface + well-known implementations | Halfway; still closed to external additions |

**Recommendation:** Interface with constants, identical to how `ActionCapability` was opened
in I3. This preserves the existing named constants (XPATH, CSS, ID, NAME) as typed
references while allowing engines to introduce domain-specific strategies (e.g.,
`AppiumStrategy.ACCESSIBILITY_ID`) without editing framework code. The fitness check
for 7.1 ensures no exhaustive switch/if-chain over strategies exists outside deprecated
paths.

**Must be decided before 7.1 implementation.**

### D2: Intermediate landing zone for `LocatorDescriptor` and `LocatorStrategy` (7.2)

Three candidates:

| Location | Tradeoff |
|---|---|
| `elements.locator` (new package) | Clean isolation; UIEngine imports elements.locator; no cycle |
| `elements.api` (alongside UIElement) | Minimal package count; tighter coupling with UIElement API |
| `core.resolvers.locator.descriptor` | Stays near the resolver; still a cross-domain import for UIEngine |

**Recommendation:** `elements.locator`. It separates the locator noun from both the engine
contract (`core.engine`) and the element API (`elements.api`), allows a clean import in
`UIEngine` without cycle risk, and leaves a clean relocation target for I6.4. The package
contains only `LocatorDescriptor` and `LocatorStrategy` (plus their package-info).

**Must be decided before 7.2 implementation.**

---

## Risks

### R1 -- HIGH: `LocatorDescriptor` move touches ~50 files

The import migration for 7.2 is mechanical but large. One wrong edit silently compiles
if the old class still exists (e.g., via a temporary alias). The old `core.engine`
location must be deleted in the same commit, not deprecated-and-retained, to prevent
stale imports from slipping through.

**Mitigation:** Single commit that moves both files and updates all imports; compile gate
(`mvn compile -q`) verifies no stale reference survives. No deprecated alias.

### R2 -- MEDIUM: `ByParser` move crosses a package boundary

`ByParser` currently lives in `core.resolvers.locator.parser`. Moving it to
`core.engine.selenium` relocates a tested class. Its tests (`ByParserTest`) must follow
or be updated.

**Mitigation:** Move test class in the same commit. The fitness check added in 7.1
(`core.resolvers.locator.parser` must not import `org.openqa.selenium.By` after 7.3)
enforces the cleanup.

### R3 -- LOW: Fitness check for strategy openness is non-trivial to write

"No exhaustive iteration over strategies" is harder to express as an ArchUnit rule than
a simple import check. The ArchUnit expression must detect `instanceof`-chain or
`switch` on `LocatorStrategy` constants specifically (not the interface).

**Mitigation:** Write the check in 7.1 against the concrete change; if ArchUnit can't
express it cleanly, a comment in `CLAUDE.md` Architecture Invariants is the fallback.
The PR audit catches violations in review.

---

## Fitness Checks to Add

| Phase | Check | Mechanism |
|---|---|---|
| 7.1 | No exhaustive `switch`/`instanceof` chain on `LocatorStrategy` outside deprecated paths | ArchUnit or convention note |
| 7.2 | `core.engine` package has no class named `Locator*` (except in `UIEngine` method signatures referencing the new location) | ArchUnit class-name check |
| 7.2 | Kernel purity gate continues to pass (LocatorDescriptor is no longer in a kernel-adjacent package) | Existing `KernelBoundaryRulesTest.kernelPurity` |
| 7.3 | `LocatorResolver` has no method returning `org.openqa.selenium.By` | ArchUnit return-type check |
| 7.3 | `core.resolvers.locator.parser` has no `org.openqa.selenium.By` import | ArchUnit (if ByParser moves in 7.3) |

---

## Phase-by-Phase Implementation Notes

### 7.1 -- Open the strategy set

1. Replace `enum LocatorStrategy` with an interface + four named constant implementations
   (XPATH, CSS, ID, NAME). Public API surface of each constant is identical to the current
   enum constant (name, toString).
2. Migrate all exhaustive consumers (switch, EnumSet, values() iteration) within the same
   commit. Identified sites: `LocatorResolver.inferStrategy()`, `LocatorResolver.stripPrefix()`,
   `SeleniumEngine.toBy()` switch (verify location), `ByParser` (if moving in this phase).
3. Decide and record `ByParser` fate (move to `core.engine.selenium` now or defer to 7.3).
4. Close the `inferStrategy` / `stripPrefix` coupling: move both to the new strategy type
   as interface methods, or drive them from a registered prefix on each constant.
5. Add fitness check.
6. Commit: `refactor(locators): open LocatorStrategy set (I7.1)`.

### 7.2 -- Descriptor ownership

1. Create `elements/locator/` package with `LocatorDescriptor.java`, `LocatorStrategy.java`,
   and `package-info.java`.
2. Delete `core/engine/LocatorDescriptor.java` and `core/engine/LocatorStrategy.java` in
   the same commit.
3. Update all ~50 import sites (IDE-assisted; compile gate confirms completeness).
4. Add ArchUnit check: `core.engine` contains no `Locator*` class.
5. Verify kernel purity gate still passes.
6. Commit: `refactor(locators): move LocatorDescriptor and LocatorStrategy to elements.locator (I7.2)`.

### 7.3 -- Delete the By-returning path

1. Migrate the three non-deprecated call sites:
   - `WaitUtils:161` → `resolveDescriptor(element)` + `SeleniumEngine.toBy()`
   - `KeyValuePairHandler:44` → `resolveDescriptor(LocatorRequest...)` + `SeleniumEngine.toBy()`
   - `Upload:51` → `resolveDescriptor(fileElement)` + `SeleniumEngine.toBy()`
2. Delete the five By-returning methods from `LocatorResolver`.
3. Move `ByParser` + `ByPrefixStrategy` to `core.engine.selenium` (if deferred from 7.1).
   Move `ByParserTest` with it.
4. Add fitness checks (return-type check, import check).
5. Verify `grep -rn "openqa.selenium.By" src/main/java` hits only: `SeleniumEngine`,
   `SeleniumLocatorBridge`, `ByParser` (if still in old location), `SeleniumEngine`-adjacent
   utilities, and pending-deletion legacy (`Via`, `Interactions`, `UIContext`).
6. Commit: `refactor(locators): delete By-returning resolution path (I7.3)`.

---

## Not In Scope

- Locator file formats (JSON/properties) -- stable, user-facing, owned by `locator-sync-trigger`
- `TargetDescriptor` or neutral address abstraction for future domains -- no second domain
  yet; deferred per initiative plan
- Physical relocation to `domain.automation.web.*` -- I6.4
- `SeleniumLocatorBridge` removal -- Migration Ledger item, 9.3
- Deprecated By-returning methods in `Via` -- frozen surface, 9.3

---

## Verdict: READY

All three phases have clear scope, known call site counts, and no unresolved blockers.
Decisions D1 and D2 must be recorded at the start of their respective phase commits.
7.3's consumer inventory is complete: 3 sites, all migratable in-phase.
