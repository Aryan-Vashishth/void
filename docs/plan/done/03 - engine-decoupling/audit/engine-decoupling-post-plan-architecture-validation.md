# Post-Plan Audit: Engine Decoupling

**Scope:** `docs/plan/draft/engine-decoupling/` (index + all four phase documents)
**Codebase scanned:** `src/main/java` (key files in `core/runtime`, `core/engine`, `core/interactions`, `core/bootstrap`, `core/context`, `core/driver`)
**Audit date:** 2026-07-17

---

## Purpose

This document audits all four engine-decoupling phase plans against the actual codebase.
For each phase it answers three questions:

1. Do the violations described in the plan actually exist?
2. Are the "Files changed" tables complete and accurate?
3. What gaps, risks, or underdescribed details would cause friction during implementation?

---

## Package structure (actual, relevant files only)

```
core/
  runtime/        VOID.java (293 lines)
  engine/         UIEngineFactory.java (93 lines), SeleniumEngine.java (603 lines)
  context/        ExecutionContext.java (84 lines), SessionContext.java (95 lines)
  interactions/   Interactions.java (833 lines)
  bootstrap/      FrameworkBootstrap.java (99 lines)
  driver/         DriverManager.java (61 lines)

NOT YET CREATED:
  engine/         EngineBootstrap.java (Phase 1 creates it)
  runtime/        VOIDBuilder.java (Phase 2 creates it)
  bridge/selenium/ SeleniumLocatorBridge.java (Phase 3 creates it)
```

---

## Phase 1 -- Factory Contract

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `UIEngineFactory.create()` accepts `WebDriver` | Line 41: `public static UIEngine create(Properties config, WebDriver driver)` | CONFIRMED |
| Switch dispatches on engine name string | Lines 45-50: `switch (engineName) { case "selenium" -> new SeleniumEngine(driver); }` | CONFIRMED |
| Playwright line is commented out | Line 47: `// case "playwright" -> new PlaywrightEngine();` | CONFIRMED |
| `SeleniumEngine` has a single WebDriver constructor | Lines 44-47: only one constructor, no `Profile` constructor | CONFIRMED |
| `EngineBootstrap.java` does not exist | Absent from codebase | CONFIRMED |
| `SeleniumEngine.initialize()` is simple (no driver creation) | Lines 54-58: only sets `config` and `defaultTimeout`, logs | CONFIRMED |
| `SeleniumEngine` has no `DriverContext` references | Zero `DriverContext` imports or calls in `SeleniumEngine.java` | CONFIRMED |

### Gap: VOID.java was missing from Phase 1 Files Changed -- fixed

Phase 1's DriverManager section shows that `VOID.start()` must change in Phase 1:

```java
// VOID.start() — Phase 1 (before inversion)
WebDriver driver = DriverManager.createDriver(profile);
UIEngine engine = UIEngineFactory.create(
        config,
        EngineBootstrap.fromDriver(driver));  // ← factory call changes here
```

`UIEngineFactory.create()` no longer accepts a raw `WebDriver` after Phase 1 -- it
accepts `EngineBootstrap`. The call in `VOID.start()` at line 143 currently reads
`UIEngineFactory.create(FrameworkBootstrap.getUtilsConfig(), driver)`. That call must
change in the Phase 1 commit to wrap the driver in `EngineBootstrap.fromDriver(driver)`.

`VOID.java` was absent from the Phase 1 Files Changed table. The row has been added.

### Gap: SeleniumEngine driver field mutability

The plan introduces a `SeleniumEngine(DriverFactory.Profile profile)` constructor that
sets `this.driver = null`, with `initialize()` later assigning the field. This requires
`driver` to be a non-final field. The current constructor is `public SeleniumEngine(WebDriver driver) { this.driver = driver; ... }` -- if `driver` is declared `private final WebDriver driver`, Phase 1 must remove the `final` modifier. Verify before implementation:

```
grep -n "final WebDriver driver" src/main/java/core/engine/selenium/SeleniumEngine.java
# if this returns a result, remove final as part of Phase 1
```

### Files changed accuracy

Phase 1 table lists `UIEngineFactory.java`, `EngineBootstrap.java`, `SeleniumEngine.java`, `VOID.java`. Complete -- `VOID.java` row was added as part of this audit.

---

## Phase 2 -- VOID Startup Pipeline

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `VOID.start()` calls `DriverManager.createDriver()` before engine selection | Lines 135-143: `WebDriver driver = DriverManager.createDriver(profile);` then `UIEngineFactory.create(...)` | CONFIRMED |
| `VOID.java` holds `ExecutionContext` field | Line 80: `private final ExecutionContext context;` | CONFIRMED |
| `ExecutionContext` holds `WebDriver` | Line 33: `private final WebDriver driver;` | CONFIRMED |
| `SessionContext` exists and holds `UIEngine` | Lines 33-36: class confirmed, `private final UIEngine engine;` | CONFIRMED |
| `SessionContext` is dead code (not instantiated) | No `new SessionContext(...)` found anywhere in codebase | CONFIRMED |
| `VOID.shutdown()` calls `DriverContext.removePrimary()` directly | Lines 162-166 confirmed | CONFIRMED |
| `ExecutionContext` is not yet `@Deprecated` | No `@Deprecated` annotation on class | CONFIRMED |
| `VOIDBuilder.java` does not exist | Absent from codebase | CONFIRMED |

### Gap: SessionContext constructor signature unverified

Phase 2 creates `new SessionContext(config, engine)` in `VOIDBuilder.start()`. The plan
assumes `SessionContext` has a constructor matching `(Properties config, UIEngine engine)`.
`SessionContext` exists and holds `UIEngine`, but the constructor signature was not
confirmed during the audit. Verify before implementation:

```
grep -n "public SessionContext" src/main/java/core/context/SessionContext.java
# confirm: SessionContext(Properties config, UIEngine engine)
```

If the constructor signature differs, `VOIDBuilder.start()` will not compile.

### Gap: VOID.getDriver() implementation change

Currently `VOID.getDriver()` at line 290 delegates to `context.getDriver()` (where
`context` is `ExecutionContext`). After Phase 2, `context` is `SessionContext` which has
no `getDriver()` method. The plan re-routes this deprecated method through
`(WebDriver) engine.getNativeDriver()`. This is a behavior change for a deprecated method
-- safe, but requires attention during the `VOID.java` field-type swap to avoid a
compilation gap between swapping the field type and updating `getDriver()`.

### Gap: Index.md "What does NOT change" was stale -- fixed

`index.md` previously said "`DriverManager.createDriver()` -- deprecated in Phase 2".
Phase 2 no longer deprecates `DriverManager` (deferred -- `SeleniumEngine` will still use
`DriverFactory` internally after Phase 1, and `DriverManager` may still have direct callers).
The line has been updated to: "deprecation deferred to a future workstream once no internal
or external callers remain."

The Phase 3 commit sequence in `index.md` was also stale (two entries, not three). Updated
to match the three-commit sequence in the Phase 3 document.

### Files changed accuracy

Phase 2 table: `VOID.java`, `VOIDBuilder.java`, `EngineBootstrap.java`,
`UIEngineFactory.java`, `SeleniumEngine.java`, `ExecutionContext.java`. All confirmed correct.
`DriverManager.java` intentionally absent (deprecation deferred). No missing entries.

---

## Phase 3 -- Interactions Cleanup

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `Interactions(UIEngine)` constructor casts `getNativeDriver()` to `WebDriver` | Line 68: `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver());` | CONFIRMED |
| Cast is at construction time, before any interaction runs | Yes -- in the constructor body | CONFIRMED |
| `Interactions.java` imports `SeleniumEngine` | Line 5: `import core.engine.selenium.SeleniumEngine;` | CONFIRMED |
| `SeleniumEngine.fromBy()` is called in deprecated bridge methods | 6 call sites: lines 161, 254, 384 (×2), 621, 821, 830 | CONFIRMED |
| `SeleniumLocatorBridge.java` does not exist | Absent from codebase | CONFIRMED |
| `Interactions(WebDriver)` deprecated constructor uses `new SeleniumEngine(driver)` | Lines 78-81 confirmed; `@Deprecated(forRemoval = true)` | CONFIRMED |

### Gap: Phase 3 implementation dependency on Phase 2

**Implementation dependency**: This phase assumes Phase 2 has already moved `DriverContext`
registration into `SeleniumEngine.initialize()`. Applying Phase 3 before Phase 2 breaks
legacy `DriverContext` consumers.

Specifically: Phase 3 removes `DriverContext.setPrimaryDriver((WebDriver) engine.getNativeDriver())`
from `Interactions(UIEngine)`. After Phase 2 this registration is redundant -- `SeleniumEngine.initialize()`
already wrote to `DriverContext` before any `Interactions` instance is created. Before Phase 2,
this constructor is the only place the primary driver is registered. Removing it early leaves
`DriverContext` without a primary driver. Any subsequent call to `DriverContext.getPrimary()` or
`DriverContext.get()` returns `null` or throws. The failure is at runtime, not compile time.

This dependency is documented in Phase 3 directly. Do not apply Phase 3 ahead of Phase 2.

### Note: SeleniumEngine.fromBy() -- future cleanup

After Phase 3, `SeleniumLocatorBridge` replicates the `fromBy()` logic and `SeleniumEngine.fromBy()`
loses all its `Interactions` call sites. This is not a Phase 3 action item. Leave the method,
deprecate it later, and delete it during the deprecated API cleanup pass. Phase 3 is already
scoped correctly.

### Files changed accuracy

Phase 3 table: `Interactions.java`, `SeleniumLocatorBridge.java`. Correct and complete.

---

## Phase 4 -- Bootstrap Cleanup

### Violation verification

| Claim | Finding | Status |
|---|---|---|
| `FrameworkBootstrap.init()` suppresses Selenium JUL logger | Line 47: `Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);` | CONFIRMED |
| Suppression runs unconditionally for all engine configurations | Yes -- inside `init()` with no engine check | CONFIRMED |
| Class Javadoc claims to be "intentionally free of driver logic" | Present in class-level Javadoc | CONFIRMED |
| `SeleniumEngine.initialize()` does not yet suppress the logger | Absent from `initialize()` body | CONFIRMED |
| `SeleniumEngine.java` does not yet import `java.util.logging.*` | Not in current imports | CONFIRMED |

### Gap: Phase 4 is safe to implement independently

V6 has no code dependency on Phases 1-3. The logger suppression move is purely cosmetic
and does not interact with driver creation, context handling, or any runtime state. Phase 4
can be implemented in any order relative to Phases 1-3 without risk of conflict.

No gaps or risks found in Phase 4.

### Files changed accuracy

Phase 4 table: `FrameworkBootstrap.java`, `SeleniumEngine.java`. Both correct and complete.

---

## Cross-phase concerns

### 1. VOID.java is modified in Phase 1 AND Phase 2

`VOID.java` must change in Phase 1 (factory call site: `driver` -> `EngineBootstrap.fromDriver(driver)`)
and again in Phase 2 (builder introduction, ExecutionContext -> SessionContext, shutdown cleanup).
Both phases must be independently compilable and `mvn compile -q` must pass after each commit.
The Phase 1 change to `VOID.start()` is the minimal change to make the factory signature compile.
The Phase 2 change inverts the construction order. These are additive changes that don't conflict,
but the implementor must not accidentally apply the Phase 2 inversion during Phase 1.

### 2. DriverContext registration ownership progression

Phase 1: `VOID` registers the driver (via `DriverManager.createDriver()`) -- still true during Phase 1.
Phase 2: `SeleniumEngine.initialize()` registers the driver; `VOID.start()` no longer calls `DriverManager.createDriver()`.
Phase 3: `Interactions(UIEngine)` no longer re-registers the driver -- safe because Phase 2 already moved that responsibility.
Phase 4: No change to DriverContext ownership.

The chain is correct. The Phase 3 dependency on Phase 2 is documented in Phase 3 directly.

### 3. SeleniumEngine field introduction order

Phase 1 adds `profile` field (null on legacy path) and makes `driver` mutable (null on primary
path until `initialize()`). Phase 2 adds `ID` constant and updates `shutdown()`. These are
additive and non-conflicting, but the implementor must apply Phase 1's field changes before
Phase 2's `ID` addition (which goes in the same file).

### 4. `index.md` stale entries -- fixed in-place

Two stale entries in `index.md` were corrected as part of this audit:
- "deprecated in Phase 2" for `DriverManager` updated to "deprecation deferred to a future workstream"
- Phase 3 commit sequence updated from the old two-line entry to the correct three-commit sequence matching the Phase 3 document

---

## Implementation risk summary

| Risk | Severity | Phase | Status |
|---|---|---|---|
| `VOID.java` missing from Phase 1 Files Changed | High | 1 | Fixed -- added to Phase 1 Files Changed table |
| `SeleniumEngine.driver` field may be `final` | High | 1 | Verify with grep before coding |
| `SessionContext` constructor signature unverified | Medium | 2 | Verify with grep before coding |
| `index.md` stale DriverManager / Phase 3 commit entries | Low | 2-3 | Fixed in-place |
| Phase 3 dependency on Phase 2 for DriverContext safety | Medium | 3 | Documented in Phase 3 directly |
| `SeleniumEngine.fromBy()` unused after Phase 3 | Low | 3 | Future cleanup -- not a Phase 3 action item |

---

## Verdict by phase

| Phase | Violations confirmed | Plan accuracy | Implementation ready |
|---|---|---|---|
| Phase 1 | All confirmed | High -- VOID.java omission fixed | Yes, after verifying driver field mutability |
| Phase 2 | All confirmed | High -- one unverified assumption | Yes, after verifying SessionContext constructor signature |
| Phase 3 | All confirmed | High -- dependency note added to phase doc | Yes |
| Phase 4 | All confirmed | High -- no gaps | Yes, unconditionally |

All four phases are ready to implement. The two pre-coding verification steps are:

```
# Phase 1 -- confirm driver field is non-final
grep -n "final WebDriver driver" src/main/java/core/engine/selenium/SeleniumEngine.java

# Phase 2 -- confirm SessionContext constructor signature
grep -n "public SessionContext" src/main/java/core/context/SessionContext.java
```
