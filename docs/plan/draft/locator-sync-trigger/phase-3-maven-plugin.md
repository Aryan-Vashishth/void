# Phase 3 -- Maven Plugin (void-maven-plugin)

Touches: new Maven module `void-maven-plugin`; `pom.xml` (parent + plugin registration).

Requires: Phase 2 (`LocatorSyncRunner`, `SyncResult`).

---

## Goal

Deliver two things in one module:

1. **Auto-sync**: sync runs automatically at `process-classes` on every build. A
   developer who adds a new capability role gets a build failure rather than a
   silent runtime failure. Developers never need to remember to run sync manually.

2. **Manual goals**: `mvn void:sync -Dclass=DemoLoginPage` replaces the full
   `exec:java` command. Short class names work via classpath scanning.

After this phase:
- `locators.json` is always up to date before tests run.
- Empty keys fail the build at `process-classes`, not at test execution.
- Developers can trigger any sync mode from the command line with a short name.
- The shell wrapper (`void.bat` / `void.sh`) is still useful for the subset of
  operations that are interactive; the plugin handles the automated path.

---

## Module structure

```
void-maven-plugin/
  pom.xml
  src/main/java/
    io.github.aryan_vashishth.void_maven_plugin/
      SyncMojo.java          (void:sync goal)
      PrintMojo.java         (void:print goal)
      WriteMojo.java         (void:write goal)
      AutoSyncMojo.java      (void:auto-sync goal, bound to process-classes)
      ClassNameResolver.java (short name -> FQCN via classpath scan)
```

The plugin depends on `void-framework` (the main module) to get access to
`LocatorSyncRunner` and `LocatorSyncOrchestrator` on its classpath.

---

## Auto-sync binding

`AutoSyncMojo` is bound to `process-classes` (after main classes are compiled, before
test compile). It reads a list of page classes from plugin config, resolves short names,
calls `LocatorSyncRunner.run()`, and fails the build on `SyncResult.EmptyKeys` or
`SyncResult.IOFailure`.

```xml
<!-- pom.xml of the project using the plugin -->
<plugin>
  <groupId>io.github.aryan-vashishth</groupId>
  <artifactId>void-maven-plugin</artifactId>
  <version>0.1.0</version>
  <executions>
    <execution>
      <goals><goal>auto-sync</goal></goals>
      <!-- binds to process-classes by default; no phase needed -->
    </execution>
  </executions>
  <configuration>
    <pageClasses>
      <pageClass>DemoLoginPage</pageClass>   <!-- short name; resolved by plugin -->
    </pageClasses>
  </configuration>
</plugin>
```

Build lifecycle with auto-sync:

```
mvn test
  compile              (page object enums compiled)
  process-classes      <- AutoSyncMojo fires
    resolve DemoLoginPage -> tests.demo.pages.DemoLoginPage
    LocatorSyncRunner.run(DemoLoginPage.class, false)
    SyncResult.EmptyKeys? -> BUILD FAILURE (shows which keys are blank)
    SyncResult.Success?   -> continue
  test-compile
  test                 (locators.json is current; tests run)
```

---

## Manual goals

```
# Sync a single class by short name
mvn void:sync -Dclass=DemoLoginPage

# Print resolved JSON to stdout
mvn void:print -Dclass=DemoLoginPage

# Write JSON to default output directory
mvn void:write -Dclass=DemoLoginPage

# List available goals
mvn void:help
```

---

## Short class name resolution (`ClassNameResolver`)

`DemoLoginPage` is resolved to its FQCN by scanning compiled classes in
`target/classes` (and `target/test-classes`):

```java
public class ClassNameResolver {
    public static Class<?> resolve(String nameOrFqcn, ClassLoader cl) throws AmbiguousNameException {
        if (nameOrFqcn.contains(".")) {
            return Class.forName(nameOrFqcn, true, cl);
        }
        List<Class<?>> matches = scanForSimpleName(nameOrFqcn, cl);
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty()) throw new ClassNotFoundException(nameOrFqcn);
        throw new AmbiguousNameException(nameOrFqcn, matches);
    }
}
```

On ambiguity, the plugin prints the candidate FQCNs and exits with a helpful error.

---

## `SyncResult` -> Maven build outcome translation

```java
switch (result) {
    case SyncResult.Success s    -> getLog().info("Sync OK: " + s.pageClass().getSimpleName());
    case SyncResult.EmptyKeys e  -> {
        e.errors().forEach(err -> getLog().error("Empty key: " + err.key() + " (" + err.lineNumber() + ")"));
        throw new MojoFailureException("Sync failed: " + e.errors().size() + " empty key(s)");
    }
    case SyncResult.IOFailure f  -> throw new MojoExecutionException("Sync I/O error", f.cause());
}
```

---

## Open questions for this phase

1. Does the plugin live in this repo (as a Maven module) or a sibling repo?
   Keeping it in-repo simplifies version alignment; a sibling repo separates the
   plugin release cycle.
2. Should `auto-sync` skip page classes whose `.properties` file timestamp is newer
   than all enum source files? (Incremental mode -- deferred to Future Watch.)
3. Opt-in TestNG `@BeforeSuite` sync path (flagged in the audit): deliver here or
   as a separate initiative?

---

## Files changed

| File | Change |
|------|--------|
| NEW `void-maven-plugin/pom.xml` | Maven module declaration |
| NEW `void-maven-plugin/src/.../AutoSyncMojo.java` | `process-classes` binding |
| NEW `void-maven-plugin/src/.../SyncMojo.java` | `void:sync` goal |
| NEW `void-maven-plugin/src/.../PrintMojo.java` | `void:print` goal |
| NEW `void-maven-plugin/src/.../WriteMojo.java` | `void:write` goal |
| NEW `void-maven-plugin/src/.../ClassNameResolver.java` | Short name -> FQCN scan |
| `pom.xml` (parent) | Add `void-maven-plugin` module |

---

## Commit

```
feat(plugin): create void-maven-plugin with auto-sync and manual sync/print/write goals
```

---

## Verification

```
# Auto-sync fires on build
mvn process-classes -q
# expected: sync runs for each configured page class; no errors if all keys are filled

# Empty key detection
# (temporarily blank a locators.properties value)
mvn process-classes
# expected: BUILD FAILURE with the key name and line number

# Manual goals with short name
mvn void:sync -Dclass=DemoLoginPage
# expected: resolves to FQCN, runs sync, writes locators.json

# Ambiguous name
mvn void:sync -Dclass=LoginPage
# expected: error listing candidate FQCNs if LoginPage exists in multiple packages

mvn test -q
# expected: all tests pass with auto-sync enabled
```

---

## Phase complete when

- [ ] `void-maven-plugin` module compiles and installs (`mvn install -pl void-maven-plugin`).
- [ ] `auto-sync` goal fires at `process-classes` and fails the build on empty keys.
- [ ] `void:sync DemoLoginPage` (short name) runs correctly.
- [ ] `void:print` and `void:write` goals work.
- [ ] `mvn test -q` passes with the plugin configured.
