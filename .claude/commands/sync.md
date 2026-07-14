Generate/update the `.properties` template and write `locators.json` for a page class.

Run the following command, replacing `$ARGUMENTS` with the fully-qualified class name (e.g. `tests.demo.pages.DemoLoginPage`). Append `--prune` to remove orphan keys.

```
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--sync $ARGUMENTS"
```

Show the full command output to the user.
