<!-- Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/Java-17-007396?logo=java" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Selenium-4-43B02A?logo=selenium" alt="Selenium 4"/>
  <img src="https://img.shields.io/badge/TestNG-parallel-FF6C37" alt="TestNG"/>
  <img src="https://img.shields.io/badge/Allure-reporting-orange" alt="Allure"/>
  <a href="https://github.com/Aryan-Vashishth/void/actions/workflows/sauce-demo.yml"><img src="https://github.com/Aryan-Vashishth/void/actions/workflows/sauce-demo.yml/badge.svg" alt="SauceDemo Tests"/></a>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License"/>
</p>

<h1 align="center">SauceDemo Automation</h1>

Automation suite for [saucedemo.com](https://www.saucedemo.com) -- reference implementation for the VOID interaction runtime.

---

## Assignment Summary

| | |
|---|---|
| Assignment 1 | Completed |
| Automated scenarios | 46 |
| Manual test cases | [Google Sheet](https://docs.google.com/spreadsheets/d/1YE2avgbrKymS8T463X9Bkfrhy6aXMyeTp-pLMGpPFNk/edit?usp=sharing) |
| CI | GitHub Actions |
| Parallel execution | Yes |
| Reporting | Allure with screenshot-on-failure |
| Stack | Selenium 4 + TestNG |

---

## Quick start

> Requires JDK 17+, Maven 3.8+, Chrome. Selenium Manager auto-downloads ChromeDriver.

**Terminal (PowerShell)**

```powershell
git clone https://github.com/Aryan-Vashishth/void.git
cd void
mvn clean test "-Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml" allure:serve
```

> Quote `-D` arguments in PowerShell to prevent the shell from stripping the flag (`"-Dproperty=value"`).

<p style="text-align: center;">or</p>

**IntelliJ** -- right-click `src/testNgXml/saucedemo.xml` in the Project view and select **Run**.

<p style="text-align: center;">or</p>

**Headless (CI / bash)**

```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/testNgXml/saucedemo.xml \
               -Dheadless=true \
               -Dargs=--no-sandbox,--disable-dev-shm-usage,--disable-gpu,--window-size=1920,1080
```

---

## Test coverage

| Module | Positive | Negative | E2E |
|--------|--------:|---------:|----:|
| Login | 5 | 7 | - |
| Products | 10 | 2 | - |
| Cart | 8 | 2 | 2 |
| Checkout | 6 | 2 | 2 |
| **Total** | **29** | **13** | **4** |

**[View the test case sheet →](https://docs.google.com/spreadsheets/d/1YE2avgbrKymS8T463X9Bkfrhy6aXMyeTp-pLMGpPFNk/edit?usp=sharing)**

Covers all 46 scenarios with Severity, Priority, Steps, Expected Result, Actual Result, and Status columns.

---

## Reporting

![Allure report overview](../../../../../../docs/images/screenshots/SauceDemo-allure-report-sample.png)

Allure generates an interactive HTML report after every run:

- Pass / fail / skip breakdown with duration
- Per-test timeline across parallel threads
- Screenshot attached to every failed test
- Full interaction log per test

```bash
mvn allure:serve
```

---

## CI

![GitHub Actions -- SauceDemo suite passing](../../../../../../docs/images/screenshots/SauceDemo-ci-passing.png)

Every push and pull request runs the full suite headlessly. Three artifacts are uploaded after each run:

- `allure-report` -- interactive HTML report with timeline, screenshots, and per-test breakdown
- `surefire-report` -- raw TestNG XML output
- `void-logs` -- VOID runtime execution trace, one log file per JVM process (all parallel threads write into the same file). Each file records session startup, every interaction dispatched (element, operation, hook chain, duration, pass/fail status), and teardown. Three verbosity levels are written per run: `partial-trace` (clean messages), `debug-trace` (messages + immediate caller), `full-trace` (messages + complete call chain to test root).

---

## Project structure

```
src/main/
 ├── java/
 │    ├── core/                                       — VOID runtime engine (collapsed)
 │    ├── domain/                                     — domain registration and registry (collapsed)
 │    └── examples/
 │         ├── pages/
 │         │    └── saucedemo/
 │         │         ├── LoginPage.java            ←┐
 │         │         ├── ProductsPage.java          │  page contracts
 │         │         ├── CartPage.java              │  (enum-driven)
 │         │         ├── CheckoutStepOnePage.java   │
 │         │         ├── CheckoutStepTwoPage.java   │
 │         │         └── CheckoutCompletePage.java ←┘
 │         └── tests/
 │              └── SauceDemoTest.java              — all 46 test methods
 └── resources/examples/
      └── pages/
           └── saucedemo/
                ├── LoginPage/                ←┐
                │    ├── locators.json         │  mirrors the page contract above;
                │    └── locators.properties   │  one folder per page class,
                ├── ProductsPage/              │  named identically
                ├── CartPage/                  │
                ├── CheckoutStepOnePage/       │
                ├── CheckoutStepTwoPage/       │
                └── CheckoutCompletePage/     ←┘
                     ├── locators.json
                     └── locators.properties
```

---

## The VOID approach

Rather than using a standard Page Object framework, this project is built on **VOID (Virtual Object Interaction-Domain Runtime)** -- an interaction runtime I designed to separate interaction modeling from execution.

**Conventional Page Object**

```java
loginPage.login("standard_user", "secret_sauce");
```

**VOID**

```java
Flow loginFlow = Flow.of(
    USERNAME.type("standard_user"),
    PASSWORD.type("secret_sauce"),
    LOGIN.click()
);

app.run(loginFlow);
```

Both approaches keep WebDriver out of the test. The difference is architectural. A Page Object method executes immediately. A Flow is a reusable interaction model that can be passed around, composed with other flows, and executed by the runtime whenever needed.

Unlike a page method, `loginFlow` is a value. It can be passed between methods, composed into larger workflows, reused across tests, or executed by any engine implementing the `UIEngine` contract.

---

## Why I built VOID

Most Selenium projects eventually accumulate:

- Wait logic duplicated across page objects and tests
- Page objects that both model the UI and execute browser actions
- Locators commonly coupled to page object implementation, mixing locator management with interaction logic
- Browser execution logic spread across the project rather than owned in one place

I wanted to design a different architecture where tests describe **intent** while a dedicated runtime owns execution. That idea evolved into VOID -- an interaction runtime that treats UI automation as one execution domain rather than the system itself.

VOID doesn't replace Selenium, Playwright, Appium, or other automation engines -- it provides a unified interaction model that sits above them, separating interaction modeling from execution.

| Conventional Page Objects | VOID |
|---|---|
| Methods execute browser interactions immediately | Elements emit immutable deferred actions |
| Browser logic distributed across page objects | Runtime owns all execution |
| Flows implemented as Java methods | Flows are immutable objects that can be stored, composed, reused, and executed independently |
| Locators commonly coupled to page object implementation | Locators externalized into runtime contracts |
| Selector updates coupled to page object source | Selector updates require no Java changes |
| IDE completion for page methods only | IDE completion for every element, capability, and interaction |
| Runtime errors for wrong interaction types | Compile-time safety via typed capabilities (`Clickable`, `Typeable`, `ReadOnly`) |

### Why this design?

- **Readable tests** -- interactions read like user intent rather than browser commands.
- **Compile-time safety** -- invalid interactions are caught at compile time through typed capability interfaces. Only `Typeable` elements expose `type()`, only `Clickable` elements expose `click()`.
- **Composable flows** -- interaction sequences are immutable objects that can be stored, combined, reused, and executed across different scenarios.
- **Rich IDE support** -- enum-driven page contracts provide discoverable auto-completion for pages, elements, capabilities, and interactions.
- **Externalized locators** -- selector updates are made outside Java source, keeping test and page code unchanged.
- **Runtime-owned execution** -- waits, retries, logging, screenshots, and browser-specific behaviour are implemented once in the runtime rather than duplicated across the project.

---

<details open>
<summary><span style="font-size: 1.15em; font-weight: 600;">Architecture</span></summary>

```
    Test Method
        │
        ▼
    UI Elements     enum constants implementing Clickable / Typeable / ReadOnly
        │
        ▼
      Actions       deferred execution intents — nothing runs yet
        │
        ▼
       Flow         immutable ordered sequence of Actions
        │
        ▼
     Runtime        dispatches to the engine, runs hooks
        │
        ▼
    UI Engine       waits, retries, locator resolution, browser interaction
        │
        ▼
     Selenium       WebDriver execution
```

Elements declare intent. Flows compose actions. The runtime executes.

Full architecture documentation: [`docs/architecture/`](../../../../docs/architecture/)

</details>

<details open>
<summary><span style="font-size: 1.15em; font-weight: 600;">Locator system</span></summary>

Locators live outside Java in external JSON contracts:

```
Update locator value in locators.json
          ↓
Runtime immediately uses the new locator
          ↓
No Java changes. No recompile.
```

Each page class maps to a JSON file by convention — no configuration needed.

</details>

<details open>
<summary><span style="font-size: 1.15em; font-weight: 600;">Adding a new page</span></summary>

**1. Define the page contract** -- `src/main/java/examples/pages/saucedemo/LoginPage.java`

```java
public interface LoginPage {

    interface LoginForm {
        enum Credentials implements Typeable  { USERNAME_FIELD, PASSWORD_FIELD; }
        enum Buttons    implements Clickable  { LOGIN_BUTTON; }
    }

    interface ErrorMessage {
        enum Labels   implements ReadOnly  { ERROR_BANNER; }
        enum Buttons  implements Clickable { ERROR_DISMISS; }
    }
}
```

**2. Run sync to generate the `.properties` template**

```powershell
mvn compile -q && mvn exec:java "-Dexec.mainClass=core.resolvers.locator.json.JsonMigratorCli" "-Dexec.args=--sync examples.pages.saucedemo.LoginPage"
```

<p style="text-align: center;">or</p>

> **Claude Code CLI** (if installed): `/sync-locators examples.pages.saucedemo.LoginPage` -- or sync an entire package at once: `/sync-locators-package examples.pages.saucedemo`

The runtime creates the template at the path that mirrors the contract's package structure under `src/main/resources/`:

```
src/
 └── main/
      ├── java/examples/pages/saucedemo/LoginPage.java        ← contract
      └── resources/examples/pages/saucedemo/LoginPage/
               ├── locators.properties                        ← generated template
               └── locators.json                              ← generated after fill
```

Generated template -- keys are derived from the enum hierarchy, values are blank:

```properties
# LoginPage — locators
# Generated by LocatorSyncCli. Fill XPath values only. Do not edit keys.

# --- Credentials ---
LoginForm.Credentials.USERNAME_FIELD.INPUT=
LoginForm.Credentials.PASSWORD_FIELD.INPUT=

# --- Buttons ---
LoginForm.Buttons.LOGIN_BUTTON.TRIGGER=

# --- Labels ---
ErrorMessage.Labels.ERROR_BANNER.TEXT=

# --- Buttons ---
ErrorMessage.Buttons.ERROR_DISMISS.TRIGGER=
```

**3. Fill in the locator values**

Values support the following strategies -- all can be mixed freely in the same file:

| Prefix | Strategy | Notes |
|---|---|---|
| `//` or `(//` | XPath | Inferred automatically -- no prefix needed |
| `xpath=` | XPath | Explicit form, same result |
| `css=` | CSS selector | Required for CSS -- not inferred |
| `id=` | By ID | Shorthand for `css=[id='...']` |
| `name=` | By name attribute | Shorthand for `css=[name='...']` |
| `class=` | By class name | Single class only |
| `tag=` | By tag name | |

```properties
# XPath -- inferred from //
LoginForm.Credentials.USERNAME_FIELD.INPUT=//input[@data-test='username']
LoginForm.Credentials.PASSWORD_FIELD.INPUT=//input[@data-test='password']

# CSS
LoginForm.Buttons.LOGIN_BUTTON.TRIGGER=css=input[data-test='login-button']

# id= shorthand
ErrorMessage.Labels.ERROR_BANNER.TEXT=id=error-banner

# name= shorthand
ErrorMessage.Buttons.ERROR_DISMISS.TRIGGER=name=dismiss-error
```

**4. Re-run sync to produce `locators.json`**

Re-run the step 2 command. The JSON mirrors the nested structure of the contract exactly:

```json
{
  "LoginPage": {
    "LoginForm": {
      "Credentials": {
        "USERNAME_FIELD": { "INPUT":  "//input[@data-test='username']" },
        "PASSWORD_FIELD": { "INPUT":  "//input[@data-test='password']" }
      },
      "Buttons": {
        "LOGIN_BUTTON":  { "TRIGGER": "css=input[data-test='login-button']" }
      }
    },
    "ErrorMessage": {
      "Labels":  { "ERROR_BANNER":  { "TEXT":    "id=error-banner" } },
      "Buttons": { "ERROR_DISMISS": { "TRIGGER": "name=dismiss-error" } }
    }
  }
}
```

**5. Use in a test** -- no locator references in test code

```java
app.run(Flow.of(
    LoginPage.LoginForm.Credentials.USERNAME_FIELD.type("standard_user"),
    LoginPage.LoginForm.Credentials.PASSWORD_FIELD.type("secret_sauce"),
    LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()
));

assertEquals(
    app.reader().query(LoginPage.ErrorMessage.Labels.ERROR_BANNER.getText()),
    "Epic sadface: Username is required"
);
```

</details>

---

## About VOID

VOID is my personal open-source project exploring a different architecture for automation.

Its interaction model is independent of the underlying execution engine, allowing the same programming model to target Selenium today while remaining extensible to Playwright, Appium, REST APIs, and other domains.

---

## Project roadmap

**Current**

- Selenium engine
- Parallel execution with per-thread session isolation
- External JSON locator contracts
- Allure reporting with screenshot-on-failure
- GitHub Actions CI

**Next**

- Playwright engine (no test-code changes required by design)
- LLM-assisted page generation pipeline

**Future**

- Mobile engine (Appium)
- REST API engine
- Desktop engine

---

*Tests describe intent. The runtime owns execution.*
