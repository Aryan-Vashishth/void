# VOID — Quick Start Guide

Get up and running with VOID in under 10 minutes.

---

## Prerequisites

| Tool       | Version  |
|------------|----------|
| **Java**   | 17+      |
| **Maven**  | 3.x      |
| **Browser**| Chrome (default), Firefox, or Edge |

> WebDriver binaries are managed automatically by [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) — no manual driver downloads needed.

---

## 1 — Clone & Build

```bash
git clone <https://github.com/Aryan-Vashishth/void-framework.git> void-framework
cd void-framework
mvn clean install -DskipTests
```

---

## 2 — Configure the Driver

Edit `src/main/resources/config/driver.properties` to match your environment:

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
```

All keys have sensible defaults — the file works out of the box for a local Chrome run.

---

## 3 — Define Your Elements

Create an interface with nested enums for each page / component.  
Each enum constant maps to a locator key in an external `.properties` or `.json` file.

```java
package elements;

import elements.api.*;

public interface LoginPageElements {

    /** Shared properties file for all enums in this interface. */
    String PROPS = "login-page-elements.properties";

    // --- Text fields use TextInputField (role: INPUT) ---
    enum Credentials implements TextInputField {
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

> **Why separate enums?** Each enum implements exactly one element interface matching its UI role.
> Text inputs implement `TextInputField` (provides `getInputLocator()`), buttons implement
> `Clickable` (provides `getTriggerLocator()`). This keeps role maps clean and lets the
> resolver pipeline pick the correct `ElementRole` automatically.

### Adding BDD / Step-Definition Support

If you need string-to-enum resolution (for Cucumber step definitions), add `ResolvableEnum` as a second interface.  
It adds **zero** boilerplate — only label/name resolution, no locator methods:

```java
import elements.api.*;
import core.utils.ResolvableEnum;

enum UserCards implements ReadOnlyElement, ResolvableEnum {
    FULL_NAME("FULL_NAME"),
    EMAIL("EMAIL");

    private final String key;
    UserCards(String k) { this.key = k; }

    @Override public String getExternalFileName() { return "user-cards.properties"; }
    @Override public String getTextLocator()      { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}
```

### Available Element Interfaces

| Interface | Roles | Purpose |
|-----------|-------|---------|
| `ReadOnlyElement` | `TEXT` | Static labels, badges, headings |
| `Clickable` | `TRIGGER` | Buttons, links, any clickable |
| `TextInputField` | `INPUT` | Text fields, date pickers |
| `Checkbox` | `TRIGGER` | Toggle / checkbox controls |
| `Dropdown` | `TRIGGER`, `LIST` | Single-value dropdowns |
| `SearchableDropdown` | `TRIGGER`, `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT` | Dropdowns with an inline search |
| `SearchField` | `SEARCH_INPUT`, `SEARCH_BUTTON` | Standalone search bars |
| `Searchable` | `SEARCH_INPUT`, `SEARCH_RESULT` | Generic search with result selection |
| `MultipleIdenticalDropdowns` | `MULTI_TRIGGER`, `MULTI_LIST` | Repeated dropdowns (e.g. three-dot menus in table rows) |
| `ToolTipElement` | `TEXT`, `TOOLTIP_CONTENT` | Truncated text with hover tooltip |
| `FileInputElement` | `INPUT` | `<input type="file">` for uploads |
| `TableElement` | `TABLE`, `ROW`, `COLUMN`, `CELL`, `HEADER` | Read-only tables |
| `WritableTableElement` | `TABLE`, `ROW`, `COLUMN`, `CELL`, `HEADER`, `ADD_ROW`, `REMOVE_ROW`, `FOOTER_INPUT` | Editable tables |
| `ListElement` | (varies) | Ordered/unordered list items |

> **Dynamic locators** — use `%s` placeholders in the locator template and supply runtime values via `getArgs()`.

---

## 4 — Add Locators

### Option A — `.properties` file

Create `src/main/resources/locators/properties/login-page-elements.properties`:

```properties
USERNAME_INPUT  = //input[@id='username']
PASSWORD_INPUT  = //input[@id='password']
SIGN_IN_BUTTON  = //button[@type='submit']
```

### Option B — `.json` file

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

Use the built-in CLI migrator to generate JSON files from your enum element descriptors:

```bash
# Print resolved JSON to stdout
java core.resolvers.locator.json.JsonMigratorCli --print  elements.LoginPageElements

# Write to the default output directory (src/main/resources/locators/json/)
java core.resolvers.locator.json.JsonMigratorCli --write  elements.LoginPageElements

# Write to a specific file
java core.resolvers.locator.json.JsonMigratorCli --write  elements.LoginPageElements  path/to/output.json
```

Or programmatically:

```java
import core.resolvers.locator.json.JsonLocatorMigrator;

// Build JSON string
String json = JsonLocatorMigrator.buildResolvedJson(LoginPageElements.class);

// Build and write to default directory
Path file = JsonLocatorMigrator.writeResolvedJson(LoginPageElements.class);
```

> See [`core/resolvers/locator/json/README.md`](../src/main/java/core/resolvers/locator/json/README.md) for full details on the migration pipeline.

---

## 5 — Write a Test

```java
import WebApplication.VOID;
import interactions.hooks.Before;
import interactions.hooks.After;
import org.testng.annotations.*;

public class LoginTest {

    private VOID app;

    @BeforeClass
    public void setUp() {
        // DriverFactory reads driver.properties and starts the browser
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
        // Before: wait for Angular overlay to clear
        // After:  no-op
        app.interaction().clickOn(
            java.util.List.of(Before.WAIT_FOR_ANGULAR_LOADER),
            LoginPageElements.Actions.SIGN_IN_BUTTON,
            java.util.List.of(After.DO_NOTHING)
        );
    }
}
```

---

## 6 — Run Tests

### Via Maven (uses `src/testNgXml/testng.xml`)

```bash
mvn clean test
```

### Via TestNG XML directly

The default suite is at `src/testNgXml/testng.xml`:

```xml
<suite name="VoidFrameworkSuite" verbose="1">
    <test name="UnitTests">
        <packages>
            <package name="core.*"/>
            <package name="elements.*"/>
        </packages>
    </test>
</suite>
```

Override the suite in Maven if needed:

```bash
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/my-suite.xml
```

---

## 7 — View Reports & Logs

| Output | Location |
|--------|----------|
| **Extent HTML Report** | `target/ExtentReports/SparkReports/` |
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
| **Enum-driven elements** | Every UI element is an enum constant implementing a behavioral interface (`Clickable`, `Dropdown`, `Searchable`, etc.). |
| **External locators** | Locators live in `.properties` or `.json` — never in Java code. |
| **Role-based resolution** | `LocatorResolvers.strict()` (recommended) and `LocatorResolvers.legacyPadded()` (backward compat) resolve locators by `ElementRole`. |
| **`ResolvableEnum`** | Standalone mixin (`core.utils.ResolvableEnum`) for name↔label resolution (used by `EnumResolver`). **Not** a locator interface — add it alongside a role interface when you need step-definition string-to-enum matching. |
| **Hook pipeline** | Pass `Before.*` / `After.*` hooks to any interaction for composable pre/post behavior. |
| **VOID façade** | `new VOID()` is the entry point; call `interaction()` for all UI actions. |
| **AutomationVOID** | Extends `VOID` with `stepDefInteraction()` for BDD / Cucumber step-definition helpers. |
| **`Via` helper** | Static utility for casting elements, resolving locators, and finding `WebElement`s without touching resolvers directly. |
| **JSON migration** | `JsonLocatorMigrator` and `JsonMigratorCli` auto-generate JSON locator files from enum + `.properties` definitions. |

---

## Common Interactions Cheat Sheet

```java
VOID app = new VOID();

// Click
app.interaction().clickOn(MyElements.SUBMIT);

// Click with hooks
app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyElements.SUBMIT,
    List.of(After.DO_NOTHING)
);

// Type into a text field
app.interaction().typeInto(MyElements.EMAIL_FIELD, "user@example.com");

// Type with hooks
app.interaction().typeInto(Before.WAIT_FOR_ANGULAR_LOADER, MyElements.EMAIL_FIELD, "user@example.com");

// Append to existing text (no clear)
app.interaction().appendTo(MyElements.NOTES_FIELD, " — additional note");

// Clear a field
app.interaction().clearField(MyElements.NOTES_FIELD);

// Type and press Enter
app.interaction().typeIntoAndPress(MyElements.SEARCH_INPUT, "query", Keys.ENTER);

// Dropdown (single-value)
app.interaction().selectFromDropdown(MyElements.AppSwitcher.ADMIN);

// Dropdown (three-dots by row index)
app.interaction().selectFromDropdown(2, MyElements.RowMenu.VIEW_REGISTRATION);

// Search + click first result
app.interaction().searchFor(MyElements.GlobalSearch.SEARCH, "Deal Registration");

// Search without clicking
app.interaction().searchForWithoutClick(MyElements.GlobalSearch.SEARCH, "Partnership");

// Read text
String name = app.interaction().getText(MyElements.UserCards.FULL_NAME);

// Tooltip text
String tip = app.interaction().getTextViaToolTip(null, MyElements.EMAIL, null, true);

// Click within a scoped parent
app.interaction().clickOnWithin(parentElement, MyElements.CHILD_LINK);
```

### Using `Via` for direct locator access

```java
import interactions.Via;

// Cast to a specific interface
Clickable  btn = Via.clickable(MyElements.SAVE_BUTTON);
Dropdown   ddl = Via.dropdown(MyElements.STATUS_DROPDOWN);

// Resolve a By locator directly
By locator     = Via.locator(MyElements.SAVE_BUTTON);
By roleLocator = Via.locator(MyElements.STATUS_DROPDOWN, ElementRole.LIST, "Active");
By rawLocator  = Via.locator("common-elements.json", "searchInput");

// Find a live WebElement
WebElement el = Via.webElement(MyElements.SAVE_BUTTON);

// Type-check before casting
if (Via.isDropdown(element)) { ... }
```

---

## Next Steps

- 📖 [Architecture deep-dive](architecture.md)
- 🧩 Browse `src/main/java/elements/api/` for all element interfaces
- 🪝 Explore `interactions/hooks/Before.java` & `After.java` for built-in hooks
- 🧰 Check `core/utils/` for `ConfigLoader`, `DOMUtils`, `WaitUtils`, `UIContext`, and more
- 🔧 See `core/resolvers/locator/` for the full locator resolution pipeline
- 📦 See [`core/resolvers/locator/json/README.md`](../src/main/java/core/resolvers/locator/json/README.md) for the JSON migration tool

---

*MIT License © 2025–2026 VOID Project*
