Write `locators.json` for a page class to a custom output path.

Usage: `/write <ClassName> [outputFile]`

- Without `outputFile` — writes to the default conventional path under `src/main/resources/`
- With `outputFile` — writes to the specified path

Run the following command, replacing `$ARGUMENTS` with `<ClassName>` or `<ClassName> path/to/output.json`.

```
mvn compile -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--write $ARGUMENTS"
```

Show the full command output to the user.
