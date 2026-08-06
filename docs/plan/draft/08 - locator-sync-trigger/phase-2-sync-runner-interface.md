# Phase 2 -- LocatorSyncRunner Interface

Touches: `core/resolvers/locator/sync/LocatorSyncRunner.java` (new),
`LocatorSyncOrchestrator.java` (implements interface).

---

## Goal

Introduce a library API that build tools (Maven plugin, TestNG listener) can call
without forking a JVM subprocess. After this phase:

- `LocatorSyncRunner` is a stable interface in `core.resolvers.locator.sync`.
- `LocatorSyncOrchestrator` implements it.
- The Maven plugin (Phase 3) and Picocli CLI (Phase 4) both call
  `LocatorSyncRunner.run()` as a library method -- no `exec:java` fork needed.
- Future sync procedure changes (new output formats, incremental mode) only require
  updating the implementation, not the callers.

---

## Problem: no library seam

`LocatorSyncOrchestrator.sync()` is a public static method. There is no interface.
A Maven plugin Mojo that wants to call it must either:

- Add a compile dependency on `core.resolvers.locator.sync` (tight coupling), or
- Fork `JsonMigratorCli` as a subprocess via `exec-maven-plugin` (slow, fragile)

The interface decouples the call site from the implementation and enables alternative
implementations without touching the trigger.

---

## New interface: `LocatorSyncRunner`

```java
package core.resolvers.locator.sync;

/**
 * Entry point for triggering a locator sync from build tools or test listeners.
 *
 * <p>Implementations scan the given page class, generate or update the
 * {@code .properties} template, validate all keys are filled, and write
 * {@code locators.json}. The sync procedure may evolve (incremental mode,
 * additional output formats) without changing this interface.</p>
 *
 * @see LocatorSyncOrchestrator
 */
public interface LocatorSyncRunner {

    /**
     * Runs a full locator sync for the given page class.
     *
     * @param pageClass the root page class whose nested element enums are scanned
     * @param prune     whether to remove orphan keys from the .properties template
     * @return sync result; never null
     */
    SyncResult run(Class<?> pageClass, boolean prune);
}
```

---

## New value type: `SyncResult`

`SyncResult` captures the outcome so callers can decide how to handle failures -- fail
the build, log a warning, or collect results across multiple page classes.

```java
package core.resolvers.locator.sync;

/**
 * Outcome of a single locator sync run.
 */
public sealed interface SyncResult
        permits SyncResult.Success, SyncResult.EmptyKeys, SyncResult.IOFailure {

    record Success(Class<?> pageClass) implements SyncResult {}

    record EmptyKeys(Class<?> pageClass, java.util.List<EmptyKeyError> errors)
            implements SyncResult {}

    record IOFailure(Class<?> pageClass, Exception cause) implements SyncResult {}
}
```

This replaces the current exit-code pattern (`0`, `1`, `3`) with a typed value that
callers can pattern-match rather than interpreting numeric codes.

> **Note**: if introducing a sealed interface is too large a change for this phase,
> a simple enum `SyncStatus { SUCCESS, EMPTY_KEYS, IO_FAILURE }` with a `cause` field
> is an acceptable interim. Seal it in a follow-up.

---

## `LocatorSyncOrchestrator` -- implements `LocatorSyncRunner`

Minimal change: add `implements LocatorSyncRunner` to the class declaration and
adapt the existing `sync()` method to return `SyncResult` instead of relying on
`System.exit()`.

```java
public class LocatorSyncOrchestrator implements LocatorSyncRunner {

    @Override
    public SyncResult run(Class<?> pageClass, boolean prune) {
        try {
            // existing steps 1-3 unchanged ...

            List<EmptyKeyError> emptyKeys = EmptyKeyValidator.validate(...);
            if (!emptyKeys.isEmpty()) {
                return new SyncResult.EmptyKeys(pageClass, emptyKeys);
            }

            // step 4: write locators.json ...
            return new SyncResult.Success(pageClass);

        } catch (IOException e) {
            return new SyncResult.IOFailure(pageClass, e);
        }
    }
}
```

`JsonMigratorCli` continues to call `LocatorSyncOrchestrator` directly and translates
`SyncResult` back to exit codes for the subprocess path. No change to the CLI contract.

---

## What does NOT change

- `JsonMigratorCli` -- still the subprocess entry point; adapts to use `SyncResult`
  exit-code translation internally
- The four sync steps in `LocatorSyncOrchestrator` -- procedure unchanged
- `FrameworkBootstrap` -- untouched
- All page object enums, locator files, resolution pipeline -- untouched

---

## Files changed

| File | Change |
|------|--------|
| NEW `core/resolvers/locator/sync/LocatorSyncRunner.java` | New interface |
| NEW `core/resolvers/locator/sync/SyncResult.java` | Sealed result type |
| `core/resolvers/locator/sync/LocatorSyncOrchestrator.java` | Implements `LocatorSyncRunner`; returns `SyncResult` instead of calling `System.exit()` |
| `core/resolvers/locator/json/JsonMigratorCli.java` | Translates `SyncResult` to exit codes; no behavioral change |

---

## Commit

```
refactor(sync): introduce LocatorSyncRunner interface in core.resolvers.locator.sync
```

---

## Verification

```
mvn compile -q

# LocatorSyncOrchestrator implements the interface
grep -n "implements LocatorSyncRunner" \
  src/main/java/core/resolvers/locator/sync/LocatorSyncOrchestrator.java
# expected: one result

# CLI still exits with the correct codes
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync examples.pages.DemoLoginPage"
# expected: exit 0 on success

mvn test -q
# expected: all existing examples pass
```

---

## Phase complete when

- [ ] `LocatorSyncRunner` interface exists in `core.resolvers.locator.sync`.
- [ ] `SyncResult` sealed type exists and covers success, empty-keys, and IO-failure.
- [ ] `LocatorSyncOrchestrator` implements `LocatorSyncRunner`.
- [ ] `JsonMigratorCli` still exits with correct codes (0, 1, 3).
- [ ] `mvn compile -q` and `mvn test -q` both pass.
