# VOID — Quick Start Guide

Get up and running with VOID in under 10 minutes.

VOID (Virtual Object Interaction Domain) is an interaction runtime for modeling and executing interaction workflows, currently configured for UI automation.

Execution pipeline:

`UIElement → Action → Flow → FlowExecutor → UIEngine`

Test code describes intent. The runtime handles execution.

> 💡 **Want to see it in action first?** Jump to the [Runnable Demo](#runnable-demo) at the end, or run
> `VoidDemo` as a TestNG test from [`src/main/java/tests/demo/VoidDemo.java`](../../src/main/java/tests/demo/VoidDemo.java) directly.

---

## Prerequisites

| Tool       | Version  |
|------------|----------|
| **Java**   | 17+      |
| **Maven**  | 3.x      |
| **Browser**| Chrome (default), Firefox, or Edge |

> Selenium runs use Selenium Manager (built into Selenium 4.6+) for driver binaries. Playwright runs use the Playwright runtime/browser install path.

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
Each enum constant maps to a locator key resolved from an external `.json` file.

### Minimal form — zero boilerplate

```java
package tests.your.pages;

import elements.api.capability.*;

public interface LoginPageElements {

    // Text fields — Typeable resolves role INPUT
    enum Credentials implements Typeable {
        USERNAME_INPUT, PASSWORD_INPUT
    }

    // Buttons — Clickable resolves role TRIGGER
    enum Actions implements Clickable {
        SIGN_IN_BUTTON
    }

    // Read-only labels — ReadOnly resolves role TEXT
    enum Labels implements ReadOnly {
        ERROR_MESSAGE, SUCCESS_BANNER
    }
}
```

No constructors, no locator keys, no `getArgs()`, no `getExternalFileName()`.

| Derived automatically | Rule |
|-----------------------|------|
| Locator key | `PageClass.EnumClass.CONSTANT.ROLE` — e.g. `LoginPageElements.Credentials.USERNAME_INPUT.INPUT` |
| Locator file | Conventional classpath path from FQCN — e.g. `tests/your/pages/LoginPageElements/locators.json` |
| Display text | Word-transform of constant name — `SIGN_IN_BUTTON` → `"Sign In Button"` |
| Args | Empty (`NO_ARGS`) |

> **Why separate enums?** Each enum implements exactly one capability interface matching its UI role.
> Text inputs implement `Typeable`, buttons implement `Clickable`. This keeps role maps clean and
> lets the resolver pick the correct `ElementRole` automatically.

### Advanced overrides

Override only what differs from the convention:

```java
enum Credentials implements Typeable {
    USERNAME_INPUT, PASSWORD_INPUT;

    // Point to a named file instead of the conventional path
    @Override public String getExternalFileName() { return "shared-login.json"; }
}
```

```java
enum Actions implements Clickable {
    SUBMIT_BUTTON;

    // Inline XPath — skips file lookup entirely
    @Override public String getTriggerLocator() { return "//button[@type='submit']"; }
}
```

Both inline (hardcoded) and file-based elements can coexist in the same page interface.

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
| `ReadOnly` | `readText()` | Text retrieval |
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

### Option A — Conventional path via `--sync` ⭐ Recommended

Run `--sync` to generate a `.properties` template, fill in XPath values, then re-run to write the JSON:

```bash
# Step 1: generate the template
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.your.pages.LoginPageElements"
```

This creates `src/main/resources/tests/your/pages/LoginPageElements/locators.properties`:

```properties
# Fill in XPath values — do not edit keys
LoginPageElements.Credentials.USERNAME_INPUT.INPUT=
LoginPageElements.Credentials.PASSWORD_INPUT.INPUT=
LoginPageElements.Actions.SIGN_IN_BUTTON.TRIGGER=
LoginPageElements.Labels.ERROR_MESSAGE.TEXT=
LoginPageElements.Labels.SUCCESS_BANNER.TEXT=
```

```bash
# Step 2: after filling values, write locators.json
mvn process-resources -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync tests.your.pages.LoginPageElements"
```

Resulting `locators.json`:

```json
{
  "LoginPageElements": {
    "Credentials": {
      "USERNAME_INPUT": { "INPUT":   "//input[@id='username']" },
      "PASSWORD_INPUT": { "INPUT":   "//input[@id='password']" }
    },
    "Actions": {
      "SIGN_IN_BUTTON": { "TRIGGER": "//button[@type='submit']" }
    },
    "Labels": {
      "ERROR_MESSAGE":  { "TEXT": "//div[@class='error']" },
      "SUCCESS_BANNER": { "TEXT": "//div[@class='success']" }
    }
  }
}
```

In Claude Code: `/sync-locators tests.your.pages.LoginPageElements` runs both steps for you.

---

### Option B — Manual `.properties` file

Create `src/main/resources/locators/properties/login-page-elements.properties` (named file, opt-in via `getExternalFileName()`):

```properties
USERNAME_INPUT  = //input[@id='username']
PASSWORD_INPUT  = //input[@id='password']
SIGN_IN_BUTTON  = //button[@type='submit']
```

### Option C — Manual `.json` file

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

### Option D — Migrate existing `.properties` → JSON

```bash
# Preview
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--print elements.LoginPageElements"

# Write to default directory
mvn exec:java -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--write elements.LoginPageElements"
```

Or programmatically:

```java
import core.resolvers.locator.json.JsonLocatorMigrator;

String json = JsonLocatorMigrator.buildResolvedJson(LoginPageElements.class);
Path file   = JsonLocatorMigrator.writeResolvedJson(LoginPageElements.class);
```

---

## 5 — Write a Test

### Modern: VOID Session Façade (Preferred)

Test code stays engine-agnostic. Select `selenium` or `playwright` at runtime via config.

```java
import core.flow.Flow;
import core.runtime.VOID;
import org.testng.annotations.*;

public class LoginTest {

    private VOID app;

    @BeforeClass
    public void setUp() {
        app = VOID.builder().start();
    }

    @Test
    public void userCanLogIn() {
        app.navigateTo("https://example.com/login");

        app.run(Flow.of(
            LoginPageElements.Credentials.USERNAME_INPUT.type("admin@example.com"),
            LoginPageElements.Credentials.PASSWORD_INPUT.type("secret"),
            LoginPageElements.Actions.SIGN_IN_BUTTON.click()
        ));

        assertTrue(app.getCurrentUrl().contains("/dashboard"));
    }

    @AfterClass
    public void tearDown() {
        if (app != null) app.shutdown();
    }
}
```

### Multi-Session Test

Each `VOID` instance is a fully independent session. `shutdown()` is session-scoped.

```java
VOID admin    = VOID.builder().start();
VOID customer = VOID.builder().start();

admin.navigateTo(ADMIN_URL);
admin.run(adminLoginFlow);

customer.navigateTo(APP_URL);
customer.run(customerLoginFlow);

admin.run(createUserFlow);

customer.run(searchUserFlow);

admin.run(approveFlow);

customer.run(verifyApprovalFlow);

admin.shutdown();    // does NOT affect customer session
customer.shutdown();
```

### Advanced: Engine Escape Hatch

When engine-specific operations are genuinely needed, use `getEngine()`. Document why.

```java
// Advanced: custom wait not yet on the facade
UIEngine engine = app.getEngine();
engine.waitForVisible(descriptor, Duration.ofSeconds(10));
```

### Hooks on Actions

Hooks wrap individual actions. They receive the engine as a parameter — this is the
designated way for hooks to interact with the engine without exposing it to test code.

```java
app.run(Flow.of(
    LoginPageElements.Credentials.USERNAME_INPUT.type("admin@example.com")
        .before(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT)
        .after(After.HIGHLIGHT_ELEMENT),
    LoginPageElements.Actions.SIGN_IN_BUTTON.click()
        .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
        .after(After.HIGHLIGHT_ELEMENT)
));
```

### Legacy: Interactions (Backward Compatible, Deprecated)

```java
// @Deprecated since 2.1 — use app.run(element.click()) instead
app.interaction().typeInto(LoginPageElements.Credentials.USERNAME_INPUT, "admin@example.com");
app.interaction().clickOn(LoginPageElements.Actions.SIGN_IN_BUTTON);
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

# Explicitly select Selenium
mvn clean test -Dengine=selenium

# Explicitly select Playwright
mvn clean test -Dengine=playwright

# Via environment variable
$env:ENGINE = "selenium"
mvn clean test

$env:ENGINE = "playwright"
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
| **Action / Flow** | `element.click()` returns `Action` (deferred intent). `Flow.of(...)` composes Actions into a sequence. |
| **`VOID` session** | Primary test object. Owns navigation (`navigateTo`, `getCurrentUrl`, `getTitle`, `refresh`), execution (`run(flow)`, `run(action)`), and lifecycle (`start`, `shutdown`). |
| **`VOID.run()`** | Preferred execution entry — delegates to the internal `FlowExecutor`. Test code never constructs `FlowExecutor` directly. |
| **UIEngine** | Single execution authority. Owns scroll, waits, retries, fallback. Engine implementations are selected at runtime (`selenium` / `playwright`). |
| **`getEngine()`** | Advanced escape hatch. Most tests never need it. Document why when used. |
| **LocatorDescriptor** | Engine-agnostic locator record. Contains value, strategy, args, optional parent scope. |
| **External locators** | Locators live in `.properties` or `.json` — never in Java code. |
| **Role-based resolution** | `LocatorResolvers.strict()` (recommended) resolves locators by `ElementRole`. |
| **`ResolvableEnum`** | Mixin for name↔label resolution. Add alongside a capability interface for BDD string-to-enum matching. |
| **Hook pipeline** | `Before.*` / `After.*` hooks composed via `.before(...).after(...)`. Hooks receive `(UIEngine, LocatorDescriptor)` — engine-agnostic. |

---

## Common Cheat Sheet

### Session Façade

```java
VOID app = VOID.builder().start();

// Navigation
app.navigateTo("https://example.com");
String url   = app.getCurrentUrl();
String title = app.getTitle();
app.refresh();

// Single action
app.run(LoginPage.SUBMIT.click());

// Flow of actions
app.run(Flow.of(
    LoginPage.USERNAME.type("user@example.com"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));

// Fluent hooks — before/after composed inline
app.run(
    LoginPage.SUBMIT.click()
        .before(Before.WAIT_FOR_ANGULAR_LOADER)
        .after(After.HIGHLIGHT_ELEMENT)
);

// Dropdown
app.run(MyPage.STATUS_DROPDOWN.select());

// Search dropdown
app.run(MyPage.GLOBAL_SEARCH.searchAndSelect("Deal Registration"));

// Checkbox
app.run(MyPage.NOTIFICATIONS.set(true));

// Type and press Enter
app.run(MyPage.SEARCH_INPUT.typeAndPress("query", "ENTER"));

// File upload
app.run(MyPage.AVATAR_INPUT.upload("/path/to/image.png"));

// Table
app.run(MyPage.DATA_TABLE.clickAddRow());

// Engine escape hatch (advanced — document why)
UIEngine engine = app.getEngine();

// Teardown
app.shutdown();
```

### Multi-Session

```java
VOID admin    = VOID.builder().start();
VOID customer = VOID.builder().start();

admin.navigateTo(ADMIN_URL);
admin.run(adminLoginFlow);

customer.navigateTo(APP_URL);
customer.run(customerLoginFlow);

admin.run(createUserFlow);
customer.run(searchUserFlow);
admin.run(approveFlow);
customer.run(verifyApprovalFlow);

admin.shutdown();    // session-scoped — does NOT affect customer
customer.shutdown();
```

### Legacy Interactions (Deprecated since 2.1)

```java
// @Deprecated — use app.run(element.click()) instead
app.interaction().clickOn(MyElements.SUBMIT);
app.interaction().typeInto(MyElements.EMAIL_FIELD, "user@example.com");
app.interaction().selectFromDropdown(MyElements.AppSwitcher.ADMIN);
String name = app.interaction().getText(MyElements.UserCards.FULL_NAME);
```

---

## Runnable Demo

A complete, self-contained demo lives in `src/main/java/tests/demo/`. It logs into [the-internet.herokuapp.com/login](https://the-internet.herokuapp.com/login) using the Action/Flow/FlowExecutor pattern.

### Files

| File | Purpose |
|------|---------|
| [`VoidDemo.java`](../../src/main/java/tests/demo/VoidDemo.java) | Main entry point — bootstraps VOID, runs login flow, verifies redirect |
| [`DemoLoginPage.java`](../../src/main/java/tests/demo/pages/DemoLoginPage.java) | UIElement definitions — `Typeable` for inputs, `Clickable` for button, `ReadOnly` for labels |
| [`demo-login-elements.json`](../../src/main/resources/locators/json/demo-login-elements.json) | Locator file — XPath locators keyed by element name |

### Running

```bash
# From IDE: run VoidDemo as a TestNG test

# From command line:
mvn test -Dtest=tests.demo.VoidDemo
```

### What It Demonstrates

1. **`VOID.builder().start()`** — full framework bootstrap (config validation → engine selection → driver creation → session init)
2. **`app.navigateTo(url)`** — session-level navigation via the façade
3. **`app.run(Flow.of(...))`** — composable Action pipeline via the session façade
4. **Capability interfaces** — `Typeable.type()`, `Clickable.click()` emitting deferred Actions
5. **Fluent hooks** — `.before(...).after(...)` for inline hook composition
6. **External JSON locators** — resolved at execution time by the engine
7. **`CustomLogger`** — color-coded, call-site-traced output
8. **`app.shutdown()`** — session-scoped teardown

---

## Next Steps

- 🚀 **[Runnable Demo](../../src/main/java/tests/demo/VoidDemo.java)** — complete working example targeting `the-internet.herokuapp.com/login`
  - [`DemoLoginPage.java`](../../src/main/java/tests/demo/pages/DemoLoginPage.java) — element definitions (Typeable, Clickable, ReadOnly)
  - [`demo-login-elements.json`](../../src/main/resources/locators/json/demo-login-elements.json) — locator file
  - Run via: `mvn test -Dtest=tests.demo.VoidDemo` or IDE TestNG runner
- 📖 [System Overview](system-overview.md) — full architecture and execution flow
- 🪝 [Hooks Pipeline](hooks-pipeline.md) — composable before/after action hooks
- 📍 [Locator Resolution](locator-resolution.md) — full resolution pipeline
- ⚙️ [Configuration Reference](configuration-reference.md) — all config keys
- 🧩 Browse `src/main/java/elements/api/capability/` for all capability interfaces
- 🔧 See `core/engine/UIEngine.java` for the execution contract
- 📦 See [`core/resolvers/locator/json/README.md`](../../src/main/java/core/resolvers/locator/json/README.md) for the JSON migration tool

---

*MIT License © 2025–2026 VOID Project*
