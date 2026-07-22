# Locator Sync -- Build Integration and Developer CLI

Identified: 2026-07-20 post-engine-decoupling tooling audit.
Branch target: cut from `main` (independent of active feature branches).
Absorbs: `void-cli-simplification.md` (merged into phases 1 and 3-4 here).

---

## Problem statement

Locator sync today is a manual, verbose operation that developers must remember to run
after every page object change:

```
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.demo.pages.DemoLoginPage"
```

Three compounding problems:

1. **No build binding.** Sync never runs automatically. A developer who adds a new
   capability role and forgets to sync will get runtime failures, not a build failure.
2. **No library API.** `LocatorSyncOrchestrator` has no interface. Build tools must fork
   a JVM subprocess to call the CLI -- expensive, fragile, hard to compose.
3. **Poor ergonomics.** The CLI requires a fully-qualified class name, has no `--help`,
   and produces no short-name resolution.

See [audit/locator-sync-trigger-pre-plan-architecture-audit.md](audit/locator-sync-trigger-pre-plan-architecture-audit.md)
for the full architectural analysis of build vs bootstrap vs VOID.java trigger points.

---

## Concern map

| ID | Concern | Layer |
|----|---------|-------|
| C1 | No build-phase binding; sync is manual | Build integration |
| C2 | No library interface; callers must fork a JVM subprocess | `core.resolvers.locator.sync` |
| C3 | Verbose invocation; full FQCN required; no short-name resolution | `JsonMigratorCli` |
| C4 | No `--help` or command list; not discoverable | `JsonMigratorCli` |

---

## Phase overview

| Phase | Goal | Risk | Key changes |
|-------|------|------|-------------|
| 1 | Shell wrapper (`void.bat` / `void.sh`) | Trivial | New scripts only; no code changes |
| 2 | `LocatorSyncRunner` interface | Low | New interface; `LocatorSyncOrchestrator` implements it |
| 3 | Maven plugin (`void-maven-plugin`) | Medium | New module; auto-sync at `process-classes`; manual goals; short name resolution |
| 4 | Standalone Picocli fat-JAR CLI | Medium | New module; `void sync DemoLoginPage`; tab completion; `--help` |

Phase docs:
- [Phase 1 -- Shell wrapper](phase-1-shell-wrapper.md)
- [Phase 2 -- LocatorSyncRunner interface](phase-2-sync-runner-interface.md)
- [Phase 3 -- Maven plugin](phase-3-maven-plugin.md)
- [Phase 4 -- Standalone Picocli CLI](phase-4-picocli-cli.md)

---

## Dependency rationale

Phase 1 is independent -- no code changes, proves the command pattern before investing
in a plugin.

Phase 2 before Phase 3: the Maven plugin Mojo calls `LocatorSyncRunner` as a library
method (no JVM fork). Phase 2 must compile and be available on the plugin's classpath
before Phase 3 can use it.

Phase 2 before Phase 4: the Picocli CLI delegates to `LocatorSyncRunner` for the same
reason. Phase 4 has no hard dependency on Phase 3 -- both can proceed after Phase 2.

Phase 3 and Phase 4 are independent of each other. Phase 3 delivers build automation;
Phase 4 delivers best-in-class developer UX. Both are useful without the other.

**Rule**: nothing in Phase N depends on Phase N+1. Each phase compiles and passes
`mvn compile -q` on its own before the next phase begins.

---

## What does NOT change

- `LocatorSyncOrchestrator` internal steps -- the four-step procedure (template
  generation, orphan detection, empty key validation, JSON generation) is unchanged
- `JsonMigratorCli` -- kept as-is; the plugin and Picocli CLI wrap it or the underlying
  runner, they do not replace it
- `FrameworkBootstrap` -- sync does not enter the runtime bootstrap path (see audit)
- `VOID.java`, `VOIDBuilder` -- no changes
- All page object enums, locator files, resolution pipeline -- untouched

---

## Commit sequence

```
# Phase 1
chore(cli): add void.bat and void.sh developer convenience wrappers

# Phase 2
refactor(sync): introduce LocatorSyncRunner interface in core.resolvers.locator.sync

# Phase 3
feat(plugin): create void-maven-plugin with auto-sync and manual sync/print/write goals

# Phase 4
feat(cli): create void-picocli standalone CLI with short name resolution and tab completion
```

All commits follow Conventional Commits format. No em dashes. Imperative present tense.

---

## Open questions (carry into phase planning)

1. Does `void-maven-plugin` live in this repo or a sibling module / repo?
2. Is GraalVM AOT compilation (native image) worth pursuing for Phase 4 to eliminate
   JVM startup latency?
3. Should the opt-in TestNG `@BeforeSuite` sync path (flagged in the audit) be
   delivered as part of Phase 3 or deferred to a separate initiative?

---

## Future watch (do not act on these now)

- Incremental sync (skip page classes whose enum source has not changed since last sync)
- Sync for non-Java page object representations (Playwright TypeScript, etc.)
- IDE plugin that triggers sync on file save

---

## Versioning (CHANGELOG.md)

Target release: **0.8.0** (default slot in the pinned sequence, after
runtime-redesign M4/0.7.0 and before 1.0.0). Independent of the milestone train
(branch cut from `main`), so it may land earlier -- in that case it takes the next
free minor at merge time and later numbers shift up. Only scheduling constraint:
not alongside runtime-redesign phase 7.3. `## [Unreleased]` entries as phases
land:

- `### Added` -- **`void.bat` / `void.sh`** -- developer convenience wrappers
  (Phase 1)
- `### Added` -- **`LocatorSyncRunner`** -- library interface for locator sync,
  no JVM fork required (Phase 2)
- `### Added` -- **`void-maven-plugin`** -- auto-sync at `process-classes` plus
  manual goals (Phase 3)
- `### Added` -- **`void` CLI** -- standalone Picocli fat-JAR with short-name
  resolution and `--help` (Phase 4)

Phases 1-2 alone are user-facing enough to warrant entries; the plugin and CLI
each justify the minor bump on their own if released separately.
