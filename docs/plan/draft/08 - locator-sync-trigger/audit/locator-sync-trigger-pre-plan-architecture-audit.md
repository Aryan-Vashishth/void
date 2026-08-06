# Architecture Audit: Locator Sync Trigger

Audit date: 2026-07-20
Question: where should locator sync be initiated -- build level, `FrameworkBootstrap`,
or `VOID.java`? Does a future-proof sync procedure require a separate bootstrap?

---

## 1. What the sync actually does

Understanding the sync's nature is the prerequisite for placing it correctly.

`LocatorSyncOrchestrator.sync(pageClass, prune)` performs four steps:

| Step | Operation | I/O |
|------|-----------|-----|
| 1 | Load enum constants via reflection; extract role keys | Reads compiled `.class` files |
| 2 | Create or merge a `.properties` template at `src/main/resources/<pkg>/locators.properties` | **Writes to source tree** |
| 3 | Validate all expected keys have non-blank values; exit(1) if any are empty | Reads `.properties` |
| 4 | Build and write `locators.json` at `src/main/resources/<pkg>/locators.json` | **Writes to source tree** |

**Critical observation**: steps 2 and 4 write files into `src/main/resources` -- the source
tree, not a temp directory. This is code generation: the output files are checked in,
read at runtime from the classpath, and consumed by `JsonTreeBuilder` / `EnumLocatorScanner`
to resolve locators during test execution.

The sync is **not an initializer**. It does not configure runtime state. It generates
resource files that later become classpath artifacts.

**Classpath requirement**: step 1 uses `Class.forName()`. The page class must already be
compiled. Sync cannot run before `mvn compile` completes.

**Write permission requirement**: steps 2 and 4 write to `src/main/resources`. In a
packaged JAR or a read-only CI workspace, this is impossible. Sync must run against a
checked-out source tree with write access.

---

## 2. Candidate trigger points

### A -- Maven build phase (`exec-maven-plugin`)

Sync bound to a Maven lifecycle phase via `exec-maven-plugin` or a custom Mojo.

**Earliest valid phase**: `process-classes` (after `compile`, main classes compiled and
available via reflection; before test compile so generated files land before examples run).

Execution chain:

```
mvn test
  compile              (page object enums compiled)
  process-classes      ← sync fires here
    foreach page class: JsonMigratorCli --sync <className>
    writes locators.properties + locators.json into src/main/resources
  test-compile
  test                 (examples find locators.json on classpath)
```

**Pros**:
- Architecturally correct: code generation in the build, not at runtime
- Declarative: configured in `pom.xml`, visible to the team
- Build fails cleanly if any empty keys remain (exit code 1 propagates)
- Generated files are stable across sessions; no file-write I/O during test execution
- Works in CI without any framework-level configuration

**Cons**:
- Requires listing page classes in `pom.xml` (or a discovery mechanism that scans
  for `Element` implementors -- could be a custom Mojo)
- Runs on every build even when no enum changed; needs change detection for speed
- `exec-maven-plugin` forks a JVM per invocation; a custom Maven plugin Mojo avoids that

---

### B -- `FrameworkBootstrap.init()`

Sync called once per JVM inside the existing `FrameworkBootstrap.init()` synchronized
block, alongside the existing config init steps.

**Execution chain**:

```
VOIDBuilder.start()
  FrameworkBootstrap.init()
    1. verify driver.properties
    2. load utils/test config
    3. ← sync would go here
       foreach page class: LocatorSyncOrchestrator.sync(...)
       writes locators.properties + locators.json
  UIEngineFactory.create(...)
```

**Pros**:
- Zero Maven config; works automatically for any developer
- One-time per JVM (idempotency guard already exists)

**Cons**:
- **Writes to the source tree during test execution**. A developer running examples in a
  read-only workspace (e.g., a CI runner without source checkout write access) gets an
  `IOException` before the first test starts.
- **File I/O on the hot path of session creation**. Sync reads all page classes via
  reflection and writes multiple files. Even if cached, the first call adds latency.
- **Validation failures blow up the session**. Empty key (exit code 1) crashes
  `FrameworkBootstrap.init()`, killing the session before any test runs. A developer
  who adds a new locator role but forgets to fill its `.properties` value cannot run
  any test at all.
- **Mixes concerns**: `FrameworkBootstrap` is currently free of driver logic, engine
  logic, and locator logic -- its Javadoc explicitly states this. Adding sync
  contradicts the single-responsibility established there.
- **Sync procedure is not idempotent in all states**: if a previous sync wrote a
  `.properties` with blank values, this run will exit(1) mid-init. The guard
  `initialized = true` never fires. Subsequent `init()` calls re-attempt and re-fail.

---

### C -- `VOID.java` (per-session, inside `VOIDBuilder.start()`)

Sync called at the start of every `VOID.builder().start()`.

**Pros**: none that B does not also have; B at least runs only once per JVM.

**Cons**: all of B's cons, magnified:
- Runs per session -- including in parallel sessions on CI (concurrent writes to the
  same `locators.json` file from multiple threads)
- Multiple `VOID` instances in a single test class trigger multiple syncs
- Completely wrong scope: session creation should not touch the source tree

**Verdict**: ruled out immediately.

---

## 3. The fundamental constraint

The sync writes to the source tree. Any trigger inside the runtime (B or C) forces file
writes during test execution. Three consequences follow directly:

1. **Read-only environments fail.** Packaged JARs, deployed environments, CI runners
   without source write access -- all fail at session creation, not at test time.

2. **Empty key validation becomes a session blocker.** A locator value left blank kills
   the framework init, not just the affected test.

3. **Classpath mismatch risk.** If sync rewrites `locators.json` while another thread is
   reading it via `PropertiesIndex`, the read may see a partial write.

**The sync belongs in the build, not in the runtime.** This is the decisive constraint.
Build level (option A) is the correct placement.

---

## 4. Does the sync need a separate Bootstrap?

Short answer: yes -- but not a runtime bootstrap. A **sync task abstraction** for the
build layer.

### Why not extend `FrameworkBootstrap`

`FrameworkBootstrap` is a runtime initializer: it verifies that compiled artifacts
(`.properties` files) are on the classpath and seeds runtime config. It is
intentionally free of generation logic. Adding sync there would:
- Add file-write I/O to a read-only initialization step
- Couple the runtime to source tree write access
- Break the single-responsibility stated in `FrameworkBootstrap`'s Javadoc

### What a sync bootstrap actually means

"Bootstrap for locator sync" in the build context means: a **entry point that the
Maven phase (or test listener) calls** to discover page classes, invoke
`LocatorSyncOrchestrator`, and handle results. Today that entry point is
`JsonMigratorCli` (a CLI tool). The question is whether to formalize it as a library
API separate from the CLI.

### Recommended: `LocatorSyncRunner` interface

Introduce a thin interface in `core.resolvers.locator.sync`:

```java
package core.resolvers.locator.sync;

public interface LocatorSyncRunner {
    SyncResult run(Class<?> pageClass, boolean prune);
}
```

`LocatorSyncOrchestrator` already implements this contract informally. Formalizing it:
- Lets a Maven plugin or TestNG listener call the sync as a library method (no
  `exec-maven-plugin` JVM fork required)
- Insulates callers from changes to the sync procedure
- Allows alternative implementations (dry-run, incremental, Playwright-specific)
  without changing the trigger point

This is NOT a runtime bootstrap. It is a **build-time service interface** whose
implementations live in the sync package. `FrameworkBootstrap` is unaffected.

### Opt-in developer convenience path

For developers who want zero Maven config: a TestNG `@BeforeSuite` listener that
calls `LocatorSyncRunner` can be registered via a system property
(`-Dvoid.sync.auto=true`). This is opt-in and explicit. It has the same write-path
risks as option B but the developer who opts in is aware of the trade-off. It is
**not the default path**.

---

## 5. Future-proofing the sync procedure

The sync procedure may change in these ways:

| Future change | Impact on trigger |
|---------------|------------------|
| Additional output formats (Playwright JSON schema) | None -- trigger calls `LocatorSyncRunner`; implementation changes internally |
| Incremental sync (skip unchanged pages) | None -- trigger passes the page class list; orchestrator decides what to skip |
| Source scanning instead of explicit class list | Trigger changes: Maven plugin Mojo scans compiled classes instead of reading a list from config |
| Sync validation rules change (new exit codes) | Trigger must handle new `SyncResult` states -- forward-compat if `SyncResult` is a sealed type |
| Non-enum `UIElement` implementations | `LocatorTemplateGenerator` changes; trigger is unaffected |

**Key insight**: the trigger point (Maven phase + `LocatorSyncRunner` call) is stable
across all likely evolution paths. The procedure (`LocatorSyncOrchestrator`) and its
output format are the parts that change. The `LocatorSyncRunner` interface is the seam
that keeps the two sides independent.

If VOID adds a Playwright engine, the sync would need to generate a different format.
`LocatorSyncRunner` can be parameterized by engine or a separate runner implementation
registered per engine -- without touching the Maven plugin binding.

---

## 6. Verdict and recommended direction

| Question | Answer |
|----------|--------|
| Build level? | Yes -- primary path |
| `FrameworkBootstrap`? | No -- wrong layer; mixes generation with runtime init |
| `VOID.java` level? | No -- wrong scope; broken in parallel and read-only environments |
| Separate Bootstrap? | No second runtime bootstrap needed; introduce `LocatorSyncRunner` interface for build-layer abstraction |

**Recommended architecture**:

```
Build trigger (Maven process-classes or TestNG @BeforeSuite opt-in)
  └── LocatorSyncRunner.run(pageClass, prune)
        └── LocatorSyncOrchestrator   (current impl; procedure changes here)
              ├── LocatorTemplateGenerator / Writer
              ├── OrphanKeyDetector
              ├── EmptyKeyValidator
              └── JsonLocatorMigrator
```

`FrameworkBootstrap` is not touched. `VOID.java` is not touched.

**Ordering relative to other initiatives**:
- Finish `void-cli-simplification.md` first (it may affect how `JsonMigratorCli` is
  invoked and may surface the `LocatorSyncRunner` interface naturally)
- This initiative follows: wire the Maven plugin + formalize `LocatorSyncRunner`
- The `generalize-element-into-target` rename (`Element` -> `UIElement`) is independent
  but must complete before any sync changes that call `element.getAllLocatorRoles()`,
  since that method moves to `UIElement`
