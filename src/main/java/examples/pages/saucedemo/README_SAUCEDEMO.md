# SauceDemo Automation -- Assignment Submission

Automated test suite for [saucedemo.com](https://www.saucedemo.com/) built on the **VOID Runtime**. Covers 46 positive, negative, and end-to-end scenarios across Login, Product Listing, Cart, and Checkout flows.

Manual test cases (Google Sheet with Severity, Priority, Steps, Expected, Actual, Status):
**[View Test Case Sheet](https://docs.google.com/spreadsheets/d/your-sheet-id)**

---

## Quick Start

> Requires JDK 17+, Maven 3.8+, and Chrome installed.

```bash
# 1. Clone
git clone https://github.com/Aryan-Vashishth/void.git
cd void

# 2. Run the suite
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml

# 3. Open the report
mvn allure:serve
```

That's it. The report opens in your browser automatically.

---

## What is VOID?

> **VOID (Virtual Object Interaction-Domain) Runtime System** -- an interaction runtime for modeling and executing interaction workflows.

```
UIElement → Action → Flow → FlowExecutor → UIEngine
```

VOID separates interaction modeling from execution. Elements emit actions. Actions compose flows. Flows are executed by the VOID Runtime through interchangeable engines that own waits, retries, locator resolution, synchronization, and all native automation concerns.

**Test code describes intent. The runtime handles execution.**

Selenium today. Playwright-ready by contract. Engine-agnostic by design.

| Layer | Responsibility |
|---|---|
| `UIElement` | Typed enum implementing a capability interface (Clickable, Typeable, ReadOnly...) and declaring a locator key |
| `Action` | Deferred intent -- describes what should happen, touches nothing |
| `Flow` | Composes actions into a named workflow |
| `VOID` | Session object -- navigate, run flows, shutdown |
| `FlowExecutor` | Dispatches flows to the engine |
| `UIEngine` | Browser executor -- waits, retries, scroll, highlight, click, type, screenshot |

Elements do not execute. They emit `Action`. There is no alternative path. No direct `WebDriver` calls anywhere in test or page code.

```java
// Elements declare intent
LoginPage.LoginForm.Credentials.USERNAME_FIELD.type("standard_user")

// Flows compose actions
app.run(Flow.of(
    LoginPage.LoginForm.Credentials.USERNAME_FIELD.type("standard_user"),
    LoginPage.LoginForm.Credentials.PASSWORD_FIELD.type("secret_sauce"),
    LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()
));
```

Locators live in external JSON files and are resolved by the runtime -- never embedded in test or page code.

---

## Framework Choice

| Concern | Choice | Reason |
|---|---|---|
| Language | Java 17 | Records, sealed classes, pattern matching -- expressive and type-safe |
| Driver | Selenium 4 | Industry standard, broad browser support, mature ecosystem |
| Test runner | TestNG | First-class parallel execution, flexible suite XML, rich listener API |
| Locator strategy | JSON + Properties | Locators live outside test code; update without recompile |
| Reporting | Allure | Interactive HTML report, screenshot-on-failure, timeline view |
| CI | GitHub Actions | Native to the repo, runs on every push and PR |

---

## Project Layout

```
src/main/java/
  examples/demo/
    SauceDemoTest.java              ← all 46 test methods
    pages/saucedemo/
      LoginPage.java                ← element enums for login page
      ProductsPage.java             ← element enums for product listing
      CartPage.java                 ← element enums for cart
      CheckoutStepOnePage.java      ← element enums for checkout step 1
      CheckoutStepTwoPage.java      ← element enums for checkout step 2
      CheckoutCompletePage.java     ← element enums for order confirmation
  examples/listeners/
    ScreenshotListener.java         ← attaches PNG to Allure on failure
    ScreenshotCapable.java          ← interface implemented by SauceDemoTest

src/main/resources/examples/demo/pages/saucedemo/
  LoginPage/
    locators.properties             ← raw XPath/CSS values
    locators.json                   ← resolved locator tree (generated)
  ProductsPage/
    locators.properties
    locators.json
  ... (one folder per page)

src/testNgXml/
  saucedemo.xml                     ← suite config: parallel=methods, 2 threads

.github/workflows/
  sauce-demo.yml                    ← CI: Chrome headless, Allure report artifact
```

---

## File Reference

Every file that makes up the SauceDemo suite, in one place.

### Test and Page Code

| File | Role |
|---|---|
| `src/main/java/examples/demo/SauceDemoTest.java` | All 46 test methods; `ThreadLocal<VOID>` session isolation |
| `src/main/java/examples/demo/pages/saucedemo/LoginPage.java` | Element enums for the login page |
| `src/main/java/examples/demo/pages/saucedemo/ProductsPage.java` | Element enums for the product listing page |
| `src/main/java/examples/demo/pages/saucedemo/CartPage.java` | Element enums for the cart page |
| `src/main/java/examples/demo/pages/saucedemo/CheckoutStepOnePage.java` | Element enums for checkout step 1 (personal info) |
| `src/main/java/examples/demo/pages/saucedemo/CheckoutStepTwoPage.java` | Element enums for checkout step 2 (order overview) |
| `src/main/java/examples/demo/pages/saucedemo/CheckoutCompletePage.java` | Element enums for the order confirmation page |

### Listeners

| File | Role |
|---|---|
| `src/main/java/examples/listeners/ScreenshotCapable.java` | Interface -- test classes implement this to expose a screenshot byte array |
| `src/main/java/examples/listeners/ScreenshotListener.java` | TestNG `ITestListener` -- attaches PNG to Allure on every test failure |

### Locators

Each page has three resource files. `locators.properties` is the source of truth; the other two are generated.

| File | Role |
|---|---|
| `src/main/resources/examples/demo/pages/saucedemo/LoginPage/locators.properties` | Raw XPath / CSS for login page |
| `src/main/resources/examples/demo/pages/saucedemo/LoginPage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/LoginPage.json` | Page-level metadata (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/ProductsPage/locators.properties` | Raw XPath / CSS for products page |
| `src/main/resources/examples/demo/pages/saucedemo/ProductsPage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/ProductsPage.json` | Page-level metadata (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CartPage/locators.properties` | Raw XPath / CSS for cart page |
| `src/main/resources/examples/demo/pages/saucedemo/CartPage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CartPage.json` | Page-level metadata (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepOnePage/locators.properties` | Raw XPath / CSS for checkout step 1 |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepOnePage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepOnePage.json` | Page-level metadata (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepTwoPage/locators.properties` | Raw XPath / CSS for checkout step 2 |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepTwoPage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutStepTwoPage.json` | Page-level metadata (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutCompletePage/locators.properties` | Raw XPath / CSS for confirmation page |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutCompletePage/locators.json` | Resolved locator tree (generated) |
| `src/main/resources/examples/demo/pages/saucedemo/CheckoutCompletePage.json` | Page-level metadata (generated) |

### Config and CI

| File | Role |
|---|---|
| `src/testNgXml/saucedemo.xml` | TestNG suite -- `parallel=methods`, `thread-count=2`, wires `ScreenshotListener` |
| `src/main/resources/allure.properties` | Points Allure results to `target/allure-results` |
| `.github/workflows/sauce-demo.yml` | CI pipeline -- headless Chrome, runs suite, uploads Allure + surefire + VOID logs |
| `pom.xml` | Declares `allure-testng` dependency and `allure-maven` plugin with report paths |

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| Chrome | Any recent stable |
| ChromeDriver | Matching Chrome version (or let Selenium Manager auto-resolve) |

---

## Setup

```bash
git clone https://github.com/Aryan-Vashishth/void.git
cd void
mvn compile
```

That's it. Selenium Manager auto-downloads a matching ChromeDriver if one is not on `PATH`.

---

## Running the Tests

**All 46 SauceDemo examples (headed):**
```bash
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml
```

**Headless (same as CI):**
```bash
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml \
         -Dheadless=true \
         -Dmaximize=false \
         -Dargs=--no-sandbox,--disable-dev-shm-usage,--disable-gpu,--window-size=1920,1080
```

**Single test by name:**
```bash
mvn test -Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml \
         -Dtest=SauceDemoTest#login01_validLogin
```

Tests run **2 in parallel** by default (configured in `saucedemo.xml`). Each thread gets its own browser session via `ThreadLocal<VOID>`.

---

## Viewing the Allure Report

After any test run, raw results land in `target/allure-results/`. Generate and open the report:

```bash
mvn allure:serve
```

This spins up a local server and opens the report in your browser automatically. The HTML report is also saved to `logs/allure-report/index.html` -- open it via your IDE's built-in HTTP server (right-click > Open In > Browser in IntelliJ).

**What you get:**
- Pass / fail / skip breakdown with duration
- Per-test timeline across parallel threads
- PNG screenshot attached to every failed test
- Full VOID interaction log per test (Navigate, Click, Type, etc.)

---

## CI -- GitHub Actions

Every push and pull request runs the full suite headlessly:

```
.github/workflows/sauce-demo.yml
```

After the run, three artifacts are available under the workflow run's **Artifacts** section:

| Artifact | Contents |
|---|---|
| `allure-report` | Full interactive HTML report (download, open `index.html` via IDE) |
| `surefire-report` | Raw XML / text surefire output |
| `void-logs` | VOID session logs |

---

## Locator Management

Each page has two locator files:

| File | Purpose |
|---|---|
| `locators.properties` | Source of truth -- raw XPath / CSS written by hand |
| `locators.json` | Generated resolved tree -- read at runtime by VOID |

The CLI command is `--sync`. It reads the page class, generates or updates the `.properties` template with keys derived from the enum constants, then writes `locators.json`. The locator file path is resolved automatically from the class name -- it mirrors the Java package structure, so no path argument is needed.

```
src/main/java/examples/demo/pages/saucedemo/CartPage.java
                                     ↕ mirrors
src/main/resources/examples/demo/pages/saucedemo/CartPage/locators.properties
```

**Update an existing page** -- edit the `.properties` file, then sync:

```bash
mvn compile -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync examples.demo.pages.saucedemo.CartPage"
```

Append `--prune` to also remove keys that no longer have a matching enum constant.

### Sync via Claude (in this repo)

Open Claude Code in the project root and say:

```
/sync-locators examples.demo.pages.saucedemo.CartPage
```

Claude runs the sync command and shows the full output.

---

## Adding a New Test

### Step 1 -- Define the element contract

Create the page interface in `src/main/java/examples/demo/pages/saucedemo/`. Declare nested enums implementing the appropriate capability interfaces (`Clickable`, `Typeable`, `ReadOnly`, etc.) with one constant per interactive element.

### Step 2 -- Generate the locator template

Run `--sync` against the new class. The CLI reads the enum constants and writes a `.properties` file with every key pre-populated and empty values:

```bash
mvn compile -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync examples.demo.pages.saucedemo.MyNewPage"
```

The generated file lands at `src/main/resources/examples/demo/pages/saucedemo/MyNewPage/locators.properties`.

### Step 3 -- Fill in the locator values

Open the generated `.properties` file and add an XPath or CSS selector for each key. Keys map 1:1 to enum constants -- no guessing required.

### Step 4 -- Generate locators.json

Re-run the same sync command. With values now present in `.properties`, the CLI writes the resolved `locators.json` alongside it. The runtime reads this file -- no recompile needed after locator changes.

```bash
mvn compile -q && mvn exec:java \
  -Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli \
  -Dexec.args="--sync examples.demo.pages.saucedemo.MyNewPage"
```

### Step 5 -- Write the test

Add a test method to `SauceDemoTest.java`. Naming convention: `module##_shortDescription` (e.g. `products03_sortByPriceHighToLow`).

---

## Parallelisation

The suite runs parallel by method out of the box (2 threads in CI, 4 locally). Scaling options:

- **More threads** -- increase `thread-count` in `saucedemo.xml`; 4 works locally, CI uses 2 to stay within the 7 GB runner memory limit
- **Cross-browser** -- parameterise the engine profile (`-Dbrowser=firefox`); VOID's engine-neutral contract means no test code changes at all
- **Remote grid** -- point `remote=true` and `remoteUrl` in `driver.properties` to run against Selenium Grid, BrowserStack, or Sauce Labs

---

## Reporting

Allure is wired in with screenshot-on-failure. Enhancements:

- Add `@Description`, `@Severity`, and `@Link` Allure annotations to test methods for richer report metadata
- Publish the report to GitHub Pages (`peaceiris/actions-gh-pages`) for a permanent public URL on every CI run
- Add a Slack or email notification step with `if: failure()` in the workflow so failures surface immediately

---

## Extension Plan

### Playwright Engine Support

Introduce a Playwright execution engine alongside the existing Selenium engine while preserving the current runtime architecture entirely. Because VOID's `UIEngine` contract owns all browser interaction -- waits, retries, locator resolution, synchronization -- adding Playwright requires only a new engine implementation. Existing element contracts, flows, and test logic execute against either engine without modification.

### LLM-Assisted Automation Pipeline

Integrate Large Language Models into the automation generation workflow. Given an application URL, VOID will automatically scan the application's DOM, accessibility tree, and structural heuristics before performing semantic analysis using an LLM. Rather than generating source code directly, the LLM will produce structured metadata describing page boundaries, UI components, semantic element names, and interaction models. The VOID `JsonMigrationCli` will then consume this metadata to deterministically generate page contracts, enums, interfaces, property files, and migration JSON -- allowing developers to review and refine the generated artifacts before they become part of the production automation suite. This pipeline significantly reduces the effort required to bootstrap and maintain large-scale automation projects.

### Domain-Neutral Runtime

Evolve VOID from a UI automation runtime into a domain-neutral interaction runtime. The core interaction model -- `UIElement → Action → Flow → FlowExecutor → UIEngine` -- will remain independent of any specific automation technology, enabling interchangeable execution engines for web UI (Selenium, Playwright), mobile automation, desktop applications, REST APIs, and command-line interfaces, while preserving a single consistent programming model across all domains.
