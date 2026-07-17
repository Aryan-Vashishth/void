Sync every page class in the project. Generates/updates `.properties` and `locators.json` for all discovered page classes.

Steps to follow:
1. Find all `.java` files under `src/main/java/tests/` and `src/test/java/tests/` recursively.
2. Filter to files that contain at least one `implements Element` declaration (these are page classes with element enums).
3. For each matching file, derive the FQCN from its path relative to the source root (e.g. `src/main/java/tests/demo/pages/DemoLoginPage.java` → `tests.demo.pages.DemoLoginPage`).
4. Run `mvn compile -q` once.
5. For each FQCN, run: `mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--sync <FQCN>"` and show the output.
6. Print a summary table: class name, status (synced / errored), and path of generated JSON.
