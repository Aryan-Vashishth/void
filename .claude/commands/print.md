Print the resolved `locators.json` for a page class to stdout (does not write any files).

Run the following command, replacing `$ARGUMENTS` with the fully-qualified class name (e.g. `tests.demo.pages.DemoLoginPage`).

```
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--print $ARGUMENTS"
```

Show the full command output to the user.
