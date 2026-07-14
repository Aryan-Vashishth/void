Write `locators.json` for a page class to the conventional path derived from the FQCN (e.g. `src/main/resources/tests/demo/pages/DemoLoginPage/locators.json`).

Run the following command, replacing `$ARGUMENTS` with the fully-qualified class name (e.g. `tests.demo.pages.DemoLoginPage`).

```
mvn compile -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--write-conventional $ARGUMENTS"
```

Show the full command output to the user.
