Sync all page classes in a package. Generates/updates `.properties` and `locators.json` for every class found.

`$ARGUMENTS` is a package name, e.g. `tests.demo.pages` or `tests.demo.pages.*` (trailing `.*` is stripped automatically).

Steps to follow:
1. Strip any trailing `.*` from `$ARGUMENTS` to get the base package, e.g. `tests.demo.pages`.
2. Convert the package to a directory path by replacing `.` with `/`.
3. Find all `.java` files directly (non-recursively) in `src/main/java/<path>/` and `src/test/java/<path>/`. List their base names without the `.java` extension.
4. Build the FQCN for each as `<basePackage>.<ClassName>`.
5. Run `mvn compile -q` once.
6. For each FQCN, run: `mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--sync <FQCN>"` and show the output.
7. Print a summary: how many classes were synced and any that errored.
