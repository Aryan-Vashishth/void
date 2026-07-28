# Phase 4 -- Standalone Picocli CLI

Touches: new module `void-cli`; no changes to core framework or Maven plugin.

Requires: Phase 2 (`LocatorSyncRunner`, `SyncResult`).
Independent of: Phase 3 (Maven plugin). Both can coexist.

---

## Goal

Provide the best developer experience for interactive use: `void sync DemoLoginPage`
from any terminal, with tab completion, `--help`, and structured error messages.

After this phase:
- `void sync DemoLoginPage` works from the command line (no `mvn` prefix, no FQCN).
- `void --help` lists all commands and flags.
- Shell tab completion works on POSIX and PowerShell.
- The Maven plugin (Phase 3) handles automated build-time sync; the Picocli CLI
  handles interactive developer invocation.

---

## Module structure

```
void-cli/
  pom.xml                    (fat JAR via maven-shade-plugin; Picocli dependency)
  src/main/java/
    core.cli/
      VoidCli.java           (@Command root; entry point)
      SyncCommand.java       (void sync <ClassName>)
      PrintCommand.java      (void print <ClassName>)
      WriteCommand.java      (void write <ClassName>)
      CliClassNameResolver.java  (short name -> FQCN; reuses ClassNameResolver logic)
```

---

## Command surface

```
$ void --help

Usage: void [-hV] <command>

Commands:
  sync    Generate .properties template and write locators.json
  print   Print resolved JSON to stdout
  write   Write JSON to the default output directory
  help    Display help for a command

$ void sync --help

Usage: void sync [--prune] <className>

      <className>   Page class to sync. Short name (DemoLoginPage) or
                    fully qualified (tests.demo.pages.DemoLoginPage).
      --prune       Remove orphan keys from the .properties template.
```

---

## Usage examples

```bash
# Short name (resolved automatically)
void sync DemoLoginPage

# Fully qualified name
void sync tests.demo.pages.DemoLoginPage

# With prune
void sync DemoLoginPage --prune

# Print JSON to stdout
void print DemoLoginPage

# Write JSON to default output
void write DemoLoginPage
```

---

## Implementation notes

### Entry point

```java
@Command(
    name = "void",
    description = "VOID developer CLI",
    subcommands = { SyncCommand.class, PrintCommand.class, WriteCommand.class,
                    CommandLine.HelpCommand.class },
    mixinStandardHelpOptions = true,
    versionProvider = VoidVersionProvider.class
)
public class VoidCli implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new VoidCli()).execute(args);
        System.exit(exitCode);
    }
    @Override public void run() { new CommandLine(this).usage(System.out); }
}
```

### SyncCommand delegates to LocatorSyncRunner

```java
@Command(name = "sync", description = "Generate .properties template and write locators.json")
public class SyncCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Page class (short or fully qualified)")
    private String className;

    @Option(names = "--prune", description = "Remove orphan keys")
    private boolean prune;

    @Override
    public Integer call() throws Exception {
        Class<?> pageClass = CliClassNameResolver.resolve(className);
        LocatorSyncRunner runner = new LocatorSyncOrchestrator();
        SyncResult result = runner.run(pageClass, prune);
        return switch (result) {
            case SyncResult.Success s    -> { System.out.println("Synced: " + s.pageClass().getSimpleName()); yield 0; }
            case SyncResult.EmptyKeys e  -> { e.errors().forEach(err -> System.err.println("Empty key: " + err.key())); yield 1; }
            case SyncResult.IOFailure f  -> { System.err.println("I/O error: " + f.cause().getMessage()); yield 3; }
        };
    }
}
```

### Fat JAR packaging

```xml
<!-- void-cli/pom.xml -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>core.cli.VoidCli</mainClass>
          </transformer>
        </transformers>
        <finalName>void-cli</finalName>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Install to PATH:
```bash
mvn package -pl void-cli -q
cp void-cli/target/void-cli.jar ~/bin/void-cli.jar
alias void="java -jar ~/bin/void-cli.jar"
```

### Tab completion

Picocli generates completion scripts automatically:

```bash
# POSIX (bash/zsh)
source <(java -cp void-cli.jar picocli.AutoComplete core.cli.VoidCli)

# PowerShell
java -cp void-cli.jar picocli.AutoComplete core.cli.VoidCli --name=void
. .\void_completion.ps1
```

---

## Open question: GraalVM native image

A GraalVM native compilation eliminates JVM startup (~200 ms). Worth pursuing if
developers run `void sync` many times per session. Deferred to a follow-up; the
fat JAR path is correct first. GraalVM requires reflection configuration for
`Class.forName()` in `CliClassNameResolver` -- non-trivial to configure.

---

## Files changed

| File | Change |
|------|--------|
| NEW `void-cli/pom.xml` | Module declaration; Picocli + shade plugin |
| NEW `void-cli/src/.../VoidCli.java` | Root command; entry point |
| NEW `void-cli/src/.../SyncCommand.java` | `void sync` |
| NEW `void-cli/src/.../PrintCommand.java` | `void print` |
| NEW `void-cli/src/.../WriteCommand.java` | `void write` |
| NEW `void-cli/src/.../CliClassNameResolver.java` | Short name resolution |
| `pom.xml` (parent) | Add `void-cli` module |

---

## Commit

```
feat(cli): create void-picocli standalone CLI with short name resolution and tab completion
```

---

## Verification

```
mvn package -pl void-cli -q

java -jar void-cli/target/void-cli.jar --help
# expected: usage text with subcommand list

java -jar void-cli/target/void-cli.jar sync DemoLoginPage
# expected: resolves, syncs, exits 0

java -jar void-cli/target/void-cli.jar sync UnknownPage
# expected: error message, exits non-zero
```

---

## Phase complete when

- [ ] `void-cli` module packages as a fat JAR.
- [ ] `void --help` lists all commands.
- [ ] `void sync DemoLoginPage` (short name) runs and exits 0.
- [ ] `void sync tests.demo.pages.DemoLoginPage` (FQCN) runs and exits 0.
- [ ] Tab completion script is generated without errors.
- [ ] Exit codes (0, 1, 3) match `JsonMigratorCli` contract.
