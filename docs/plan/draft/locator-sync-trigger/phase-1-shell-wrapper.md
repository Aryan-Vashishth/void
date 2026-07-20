# Phase 1 -- Shell Wrapper (void.bat / void.sh)

Touches: project root only (`void.bat`, `void.sh`). No Java source changes.

---

## Goal

Provide an immediate ergonomic improvement with zero code changes. After this phase:

- `.\void /sync tests.demo.pages.DemoLoginPage` replaces the full Maven exec command.
- Both Windows (`void.bat`) and POSIX (`void.sh`) are covered.
- Developers have a stable command surface to learn before the Maven plugin (Phase 3)
  makes the underlying invocation invisible.

This phase proves the command pattern is useful before investing in the plugin.

---

## `void.bat` (Windows)

```bat
@echo off
rem void.bat -- VOID developer CLI wrapper
rem Usage: .\void /sync  <ClassName>
rem        .\void /print <ClassName>
rem        .\void /write <ClassName>

set CMD=%1
set CLASS=%2

if "%CMD%"=="/sync"               goto sync
if "%CMD%"=="/print"              goto print
if "%CMD%"=="/write"              goto write
if "%CMD%"=="/write-conventional" goto write_conv

echo VOID developer CLI
echo.
echo Commands:
echo   /sync  ^<ClassName^>   -- generate .properties template and write locators.json
echo   /print ^<ClassName^>   -- print resolved JSON to stdout
echo   /write ^<ClassName^>   -- write JSON to default output directory
echo   /write-conventional ^<ClassName^>  -- write to conventional locators.json path
echo.
echo Example:
echo   .\void /sync tests.demo.pages.DemoLoginPage
goto :eof

:sync
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--sync %CLASS%"
goto :eof

:print
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--print %CLASS%"
goto :eof

:write
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--write %CLASS%"
goto :eof

:write_conv
mvn process-resources -q && mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli -Dexec.args="--write-conventional %CLASS%"
goto :eof
```

---

## `void.sh` (POSIX)

```bash
#!/usr/bin/env bash
# void.sh -- VOID developer CLI wrapper
# Usage: ./void.sh /sync  <ClassName>
#        ./void.sh /print <ClassName>
#        ./void.sh /write <ClassName>

CMD=${1:-}
CLASS=${2:-}
MAIN="core.resolvers.locator.json.JsonMigratorCli"

run() {
  mvn process-resources -q && mvn exec:java \
    -Dexec.mainClass="$MAIN" \
    -Dexec.args="$1 $CLASS"
}

case "$CMD" in
  /sync)               run "--sync" ;;
  /print)              run "--print" ;;
  /write)              run "--write" ;;
  /write-conventional) run "--write-conventional" ;;
  *)
    echo "VOID developer CLI"
    echo ""
    echo "Commands:"
    echo "  /sync  <ClassName>              generate .properties template and write locators.json"
    echo "  /print <ClassName>              print resolved JSON to stdout"
    echo "  /write <ClassName>              write JSON to default output directory"
    echo "  /write-conventional <ClassName> write to conventional locators.json path"
    echo ""
    echo "Example:"
    echo "  ./void.sh /sync tests.demo.pages.DemoLoginPage"
    ;;
esac
```

---

## Limitations (addressed in later phases)

- Full FQCN still required. Short name resolution (`DemoLoginPage`) arrives in Phase 3.
- `mvn process-resources -q` prefix is still needed to avoid stale-cache issues. The
  Maven plugin (Phase 3) eliminates this by binding to `process-classes` automatically.
- No tab completion.

---

## Files changed

| File | Change |
|------|--------|
| NEW `void.bat` | Windows wrapper script |
| NEW `void.sh` | POSIX wrapper script |

---

## Commit

```
chore(cli): add void.bat and void.sh developer convenience wrappers
```

---

## Verification

```
# Windows
.\void
# expected: help text listing /sync /print /write

.\void /sync tests.demo.pages.DemoLoginPage
# expected: process-resources runs, then sync runs, then locators.json is written

# POSIX
chmod +x void.sh
./void.sh
./void.sh /sync tests.demo.pages.DemoLoginPage
```

---

## Phase complete when

- [ ] `void.bat` and `void.sh` exist at project root.
- [ ] Running with no args prints the command list.
- [ ] `.\void /sync tests.demo.pages.DemoLoginPage` produces the same output as the
      full Maven exec command.
- [ ] Scripts are committed and reviewable.
