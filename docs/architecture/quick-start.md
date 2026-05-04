# VOID — Quick Start Guide

Get up and running with VOID in under 10 minutes.

---

## Prerequisites

| Tool       | Version  |
|------------|----------|
| **Java**   | 17+      |
| **Maven**  | 3.x      |
| **Browser**| Chrome (default), Firefox, or Edge |

> WebDriver binaries are managed automatically by Selenium Manager (built into Selenium 4.6+) — no manual driver downloads needed.

---

## 1 — Clone & Build

```bash
git clone <https://github.com/Aryan-Vashishth/void-framework.git> void-framework
cd void-framework
mvn clean install -DskipTests
```

---

## 2 — Configure the Driver

Edit `src/main/resources/core/driver/config/driver.properties` to match your environment:

```properties
# Core
browser=chrome          # chrome | firefox | edge
headless=false
remote=false
gridUrl=                # e.g. http://localhost:4444/wd/hub (when remote=true)
maximize=true
width=                  # explicit window width (ignored when maximize=true)
height=                 # explicit window height

# Timeouts & Strategy (seconds)
implicitWait=5
pageLoadTimeout=60
scriptTimeout=30
pageLoadStrategy=NORMAL # NORMAL | EAGER | NONE

# Certificates / Security
acceptInsecureCerts=true

# Downloads
downloadsDir=           # custom download directory (optional)

# Mobile emulation (Chrome only)
mobileEmulationDevice=  # e.g. Pixel 7

# Binary overrides (optional)
chromeBinary=
firefoxBinary=
edgeBinary=

# Proxy (optional)
proxy.http=
proxy.ssl=
proxy.socks=
proxy.socksVersion=

# Browser arguments (CSV or numbered)
args=                   # e.g. --no-sandbox,--disable-dev-shm-usage
arg.1=
arg.2=

# Browser preferences
pref.download.prompt_for_download=

# Extra capabilities
cap.someCapability=

# Engine selection (Phase 2+)
engine=selenium         # selenium | playwright (default: selenium)
engine.timeout=10       # default element interaction timeout (seconds)
engine.pollingMs=200    # polling interval for explicit waits (milliseconds)
engine.baseUrl=         # base URL for navigation
```

All keys have sensible defaults — the file works out of the box for a local Chrome run.

---

## 3 — Define Your Elements

Create an interface with nested enums for each page / component.
Each enum constant maps to a locator key in an external `.properties` or `.json` file.

### Capability interfaces define what an element CAN DO:

```java
package elements;

import elements.api.capability.*;

public interface LoginPageElements {

    /** Shared properties file for all enums in this interface. */
    String PROPS = "login-page-elements.properties";

    // --- Text fields use Typeable (role: INPUT) ---
    enum Credentials implements Typeable {
        USERNAME_INPUT("USERNAME_INPUT"),
        PASSWORD_INPUT("PASSWORD_INPUT");

        private final String key;
        Credentials(String k) { this.key = k; }

        @Override public String getInputLocator()     { return key; }
        @Override public String getExternalFileName() { return PROPS; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    // --- Buttons use Clickable (role: TRIGGER) ---
    enum Actions implements Clickable {
        SIGN_IN_BUTTON("SIGN_IN_BUTTON", "Sign In");

        private final String key;
        private final String label;
        Actions(String k, String l) { this.key = k; this.label = l; }

        @Override public String getTriggerLocator()   { return key; }
        @Override public String getExternalFileName() { return PROPS; }
        @Override public Object[] getArgs()           { return new Object[]{label}; }
    }
}
```

> **Why separate enums?** Each enum implements exactly one capability interface matching its UI role.
> Text inputs implement `Typeable` (provides `getInputLocator()`), buttons implement
> `Clickable` (provides `getTriggerLocator()`). This keeps role maps clean and lets the
> resolver pipeline pick the correct `ElementRole` automatically.

### Capability Interfaces: What to Implement

| Interface | Role(s) | Purpose | Key Method |
|-----------|---------|---------|------------|
| `ReadOnly` | `TEXT` | Static labels, badges, headings | `getTextLocator()` |
| `Clickable` | `TRIGGER` | Buttons, links, any clickable | `getTriggerLocator()` |
| `Typeable` | `INPUT` | Text fields, date pickers | `getInputLocator()` |
| `Checkable` | `TRIGGER` | Toggle / checkbox controls | inherits from `Clickable` |
| `Selectable` | `TRIGGER`, `LIST` | Single-value dropdowns | `getTriggerLocator()`, `getListLocator()` |
| `SearchableDropdown` | `TRIGGER`, `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT` | Dropdowns with inline search | all four locators |
| `SearchField` | `SEARCH_INPUT`, `SEARCH_BUTTON` | Standalone search bars | `getSearchInputLocator()`, `getSearchButtonLocator()` |
| `Searchable` | `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT` | Search with result selection | adds `getSearchResultLocator()` |
| `MultiSelectable` | `MULTI_TRIGGER`, `MULTI_LIST` | Repeated dropdowns (e.g., three-dot menus) | `getTriggerLocator()`, `getListLocator()` |
| `Hoverable` | `TEXT`, `TOOLTIP_CONTENT` | Truncated text with hover tooltip | `getToolTipContentLocator()`, `getEndsWith()` |
| `Uploadable` | `INPUT` | `<input type="file">` for uploads | `getInputLocator()` |
| `Table` | `TABLE`, `ROW`, `COLUMN`, `CELL`, `HEADER` | Read-only tables | `getTableLocator()` + optional sub-locators |
| `EditableTable` | + `ADD_ROW_BUTTON`, `REMOVE_ROW_BUTTON`, `FOOTER_INPUT_ROW` | Editable tables | adds row management locators |
| `Listable` | `LIST` | Ordered/unordered list items | `getListLocator()`, `getIndex()` |

> **Dynamic locators** — use `%s` placeholders in the locator template and supply runtime values via `getArgs()`.

### Action Emission: What Capabilities Provide

Every capability interface emits **deferred Action objects** — intent, not execution:

| Capability | Actions Emitted | Description |
|------------|----------------|-------------|
| `Clickable` | `click()` | Deferred click |
| `Typeable` | `type(text)`, `clear()`, `append(text)`, `typeAndPress(text, key)` | Text input actions |
| `Checkable` | `toggle()`, `set(boolean)` | Checkbox control |
| `Selectable` | `open()`, `select()`, `selectByText(text)`, `selectByValue(value)` | Dropdown selection |
| `MultiSelectable` | `open()`, `selectAtIndex(index)` | Indexed dropdown selection |
| `SearchField` | `typeSearch(text)`, `submitSearch()` | Search actions |
| `SearchableDropdown` | `searchAndSelect(term)` | Composite search+select |
| `Hoverable` | `hover()` | Tooltip trigger |
| `ReadOnly` | `getText()` | Text retrieval |
| `Uploadable` | `upload(path)` | File upload |
| `EditableTable` | `clickAddRow()`, `clickRemoveRow()` | Table row management |

### Adding BDD / Step-Definition Support

If you need string-to-enum resolution (for Cucumber step definitions), add `ResolvableEnum` as a second interface.
It adds **zero** boilerplate — only label/name resolution, no locator methods:

```java
import elements.api.capability.*;
import core.utils.ResolvableEnum;

enum UserCards implements ReadOnly, ResolvableEnum {
    FULL_NAME("FULL_NAME"),
    EMAIL("EMAIL");

    private final String key;
    UserCards(String k) { this.key = k; }

    @Override public String getExternalFileName() { return "user-cards.properties"; }
    @Override public String getTextLocator()      { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}
```

---

## 4 — Add Locators

### Option A — `.properties` file

Create `src/main/resources/locators/properties/login-page-elements.properties`:

```properties
USERNAME_INPUT  = //input[@id='username']
PASSWORD_INPUT  = //input[@id='password']
SIGN_IN_BUTTON  = //button[@type='submit']
```

### Option B — `.json` file ⭐ Recommended

Create `src/main/resources/locators/json/login-page-elements.json`:

```json
{
  "LoginPageElements": {
    "Credentials": {
      "USERNAME_INPUT": "//input[@id='username']",
      "PASSWORD_INPUT": "//input[@id='password']"
    },
    "Actions": {
      "SIGN_IN_BUTTON": "//button[@type='submit']"
    }
  }
}
```

### Option C — Migrate from Properties → JSON automatically

Use the built-in CLI migrator:

```bash
# Preview
java core.resolvers.locator.json.JsonMigratorCli --print  elements.LoginPageElements

# Write to default directory
java core.resolvers.locator.json.JsonMigratorCli --write  elements.LoginPageElements
```

Or programmatically:

```java
import core.resolvers.locator.json.JsonLocatorMigrator;

String json = JsonLocatorMigrator.buildResolvedJson(LoginPageElements.class);
Path file = JsonLocatorMigrator.writeResolvedJson(LoginPageElements.class);
```

---

## 5 — Write a Test

### Modern: Action / Flow / Runner (Preferred)

```java
import core.engine.UIEngine;
import core.engine.UIEngineFactory;
import core.flow.Flow;
import core.runner.Runner;
import core.driver.DriverFactory;
import org.testng.annotations.*;

public class LoginTest {

    private UIEngine engine;
    private Runner runner;

    @BeforeClass
    public void setUp() {
        var driver = DriverFactory.build();
        engine = UIEngineFactory.create(new java.util.Properties(), driver);
        runner = new Runner(engine);
    }

    @Test
    public void userCanLogIn() {
        runner.run(Flow.of(
            LoginPageElements.Credentials.USERNAME_INPUT.type("admin@example.com"),
            LoginPageElements.Credentials.PASSWORD_INPUT.type("secret"),
            LoginPageElements.Actions.SIGN_IN_BUTTON.click()
        ));
    }

    @AfterClass
    public void tearDown() {
        if (engine != null) engine.shutdown();
    }
}
```

### Legacy: Interactions (Backward Compatible)

```java
import core.bootstrap.VOID;
import core.interactions.hooks.Before;
import core.interactions.hooks.After;
import org.testng.annotations.*;

public class LoginTest {

    private VOID app;

    @BeforeClass
    public void setUp() {
        app = new VOID();
    }

    @Test
    public void userCanLogIn() {
        app.interaction().typeInto(LoginPageElements.Credentials.USERNAME_INPUT, "admin@example.com");
        app.interaction().typeInto(LoginPageElements.Credentials.PASSWORD_INPUT, "secret");
        app.interaction().clickOn(LoginPageElements.Actions.SIGN_IN_BUTTON);
    }

    @Test
    public void clickWithHooks() {
        app.interaction().clickOn(
            java.util.List.of(Before.WAIT_FOR_ANGULAR_LOADER),
            LoginPageElements.Actions.SIGN_IN_BUTTON,
            java.util.List.of(After.DO_NOTHING)
        );
    }
}
```

### DSL Layer (BDD / Step Definitions)

```java
import dsl.VoidDSL;

VoidDSL dsl = new VoidDSL(app.interaction());
dsl.clickOnFrom("tiles", "admin_home", "Account Mapping");
dsl.selectFromDropdownByContext("filters", "Status Dropdown");
dsl.setCheckboxByContext("settings", "options", "Enable Notifications", true);
```

---

## 6 — Run Tests

### Via Maven (uses `src/testNgXml/testng.xml`)

```bash
mvn clean test
```

### Engine Selection at Runtime

```bash
# Default (Selenium)
mvn clean test

# Explicitly select engine
mvn clean test -Dengine=selenium

# Via environment variable
$env:ENGINE = "selenium"
mvn clean test
```

### Override Suite

```bash
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/my-suite.xml
```

---

## 7 — View Reports & Logs

| Output | Location |
|--------|----------|
| **TestNG Reports**     | `target/surefire-reports/`           |
| **Console Logs**       | ANSI-colored, timestamped, with call-site tracing |

Example log line:

```
2026-04-24 13:15:37.672 │ DEBUG │ [LOCATOR] Resolving:          │ LocatorResolver.resolve ← LocatorResolver.resolveBest
                        │       │           ├─ File  : login-page-elements.properties
                        │       │           ├─ Key   : USERNAME_INPUT
                        │       │           └─ Args  : []
```

---

## Key Concepts at a Glance

| Concept | What It Means |
|---------|---------------|
| **Enum-driven elements** | Every UI element is an enum constant implementing a capability interface (`Clickable`, `Selectable`, `Searchable`, etc.). |
| **Capability interfaces** | Located in `elements.api.capability.*`. Define what an element CAN DO. Emit deferred `Action` objects. |
| **Action / Flow / Runner** | `element.click()` returns `Action` (deferred intent). `Flow.of(...)` composes. `Runner` executes via `UIEngine`. |
| **UIEngine** | Single execution authority. Owns scroll, waits, retries, fallback. `SeleniumEngine` is the current implementation. |
| **LocatorDescriptor** | Engine-agnostic locator record. Contains value, strategy, args, optional parent scope. |
| **External locators** | Locators live in `.properties` or `.json` — never in Java code. |
| **Role-based resolution** | `LocatorResolvers.strict()` (recommended) resolves locators by `ElementRole`. |
| **`ResolvableEnum`** | Mixin for name↔label resolution. Add alongside a capability interface for BDD string-to-enum matching. |
| **Hook pipeline** | `Before.*` / `After.*` hooks for composable pre/post behavior (used with `Interactions`). |
| **VOID façade** | Legacy entry point. `VoidDSL` for BDD. `Runner` + `Flow` for new code. |
| **`Via` helper** | Static utility. Descriptor-based methods preferred over `By`-based (deprecated). |

---

## Common Cheat Sheet

### Action / Flow / Runner

```java
Runner runner = new Runner(engine);

// Single action
runner.execute(LoginPage.SUBMIT.click());

// Flow of actions
runner.run(Flow.of(
    LoginPage.USERNAME.type("user@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));

// Dropdown
runner.execute(MyPage.STATUS_DROPDOWN.select());

// Search dropdown
runner.execute(MyPage.GLOBAL_SEARCH.searchAndSelect("Deal Registration"));

// Checkbox
runner.execute(MyPage.NOTIFICATIONS.set(true));

// Type and press Enter
runner.execute(MyPage.SEARCH_INPUT.typeAndPress("query", "ENTER"));

// File upload
runner.execute(MyPage.AVATAR_INPUT.upload("/path/to/image.png"));

// Table
runner.execute(MyPage.DATA_TABLE.clickAddRow());
```

### Legacy Interactions

```java
VOID app = new VOID();

app.interaction().clickOn(MyElements.SUBMIT);
app.interaction().typeInto(MyElements.EMAIL_FIELD, "user@example.com");
app.interaction().selectFromDropdown(MyElements.AppSwitcher.ADMIN);
app.interaction().searchFor(MyElements.GlobalSearch.SEARCH, "Deal Registration");
String name = app.interaction().getText(MyElements.UserCards.FULL_NAME);
```

---

## Next Steps

- 📖 [System Overview](system-overview.md) — full architecture and execution flow
- 🪝 [Hooks Pipeline](hooks-pipeline.md) — composable before/after action hooks
- 📍 [Locator Resolution](locator-resolution.md) — full resolution pipeline
- ⚙️ [Configuration Reference](configuration-reference.md) — all config keys
- 🧩 Browse `src/main/java/elements/api/capability/` for all capability interfaces
- 🔧 See `core/engine/UIEngine.java` for the execution contract
- 📦 See [`core/resolvers/locator/json/README.md`](../../src/main/java/core/resolvers/locator/json/README.md) for the JSON migration tool

---

*MIT License © 2025–2026 VOID Project*
