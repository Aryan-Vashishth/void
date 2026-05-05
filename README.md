# VOID

**Versatile Object-Oriented Interactions for DOM**

![Java](https://img.shields.io/badge/Java-17+-blue?logo=openjdk)
![Selenium](https://img.shields.io/badge/Selenium-4.38-green?logo=selenium)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Version](https://img.shields.io/badge/Version-2.0--SNAPSHOT-orange)

---

## 🧠 What This Is (and What It Isn't)

VOID is not a Selenium wrapper.  
It's not a framework in the Spring/JUnit sense — it doesn't own your test runner or project layout.  
And it's not a loose SDK you wire together however you feel like.

VOID is a **structured automation system**. It ships as a dependency, but inside it enforces a specific model for how elements are defined, how locators are resolved, how actions execute, and how failures are reported.

You adopt the model. The model gives you consistency, traceability, and debuggability in return.

This is a deliberate trade: you don't get to invent your own element abstraction or locator strategy and still benefit from what VOID provides. The system works because the parts agree on how things are done.

Most automation tooling focuses on *running* tests.  
VOID focuses on **understanding them when they fail**.

---

## ❌ The Problem (You've Seen This Before)

If you've worked with Selenium long enough, you already know:

- Page Objects start clean → end up as 2,000-line nightmares  
- Locators live in 6 different places → none of them updated  
- Failures say *"element not found"* → thanks, very helpful  
- Debugging = scroll logs + guess + retry  

At scale, automation doesn't fail because of Selenium.  
It fails because of **lack of structure and visibility**.

---

## ✅ What VOID Does Differently

VOID doesn't patch these problems.  
It replaces the way things are modeled — and expects you to follow the replacement.

### 🔹 Elements are not classes. They're enums.
Each UI element is a **first-class, typed entity** implementing a behavioral interface (`Clickable`, `Dropdown`, `TableElement`, etc.). This isn't optional — it's how the locator resolver, the interaction layer, and the logging system identify what you're working with.

### 🔹 Locators are resolved, not hardcoded
Locators live in external `.json` or `.properties` files, resolved at runtime through `LocatorResolvers` with role-based dispatch via `ElementRole`. You don't call `By.xpath(...)` in test code. The system handles it.

### 🔹 Actions are pipelines, not method calls
Every interaction runs through a **before/after hook pipeline** — wait conditions, highlights, loader guards — all composable, all declared at the call site. The pipeline is what makes actions observable and retryable.

### 🔹 Logging actually explains things
Not just *what failed*, but:
- where it failed  
- why it failed  
- what was attempted  
- what the locator resolved to  
- who called whom  

This isn't bolted on. It's structural — the interaction layer, resolver, and hooks all feed into the same tracing system.


---

## 📋 Prerequisites

| Tool        | Version | Notes                                                        |
|-------------|---------|--------------------------------------------------------------|
| **Java**    | 17+     | `JAVA_HOME` must point to a JDK 17+ installation            |
| **Maven**   | 3.x     | Used for building and running tests                          |
| **Browser** | Latest  | Chrome (default), Firefox, or Edge                           |

> WebDriver binaries are managed automatically by Selenium Manager (built into Selenium 4.6+) — no manual driver downloads needed.

---

## ⚡ Quick Start

```bash
# Clone and build
git clone <your-repo-url> void-framework
cd void-framework
mvn clean install -DskipTests

# Run all tests
mvn clean test
```

Then write your first test:

```java
VOID app = new VOID();

app.interaction().clickOn(LoginPageElements.Actions.SIGN_IN_BUTTON);
app.interaction().typeInto(LoginPageElements.Credentials.USERNAME_INPUT, "admin@example.com");
app.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);
```

> 📖 See the full [Quick Start Guide](docs/quick-start.md) for step-by-step instructions.

---

## 🚀 Core Features

### 🧩 Enum-Driven Element Model
- Elements defined as enums implementing interfaces (`Clickable`, `Dropdown`, etc.)
- Nested enums for contextual grouping
- Each element carries:
  - locator key
  - external file reference
  - dynamic arguments
  - display text

---

### 📍 Role-Based Locator Resolution
- Centralized via `LocatorResolvers` (`strict()` + `legacyPadded()` resolvers operating on `LocatorRequest`)
- Supports `.json` and `.properties`
- Uses `ElementRole`
- Dynamic `%s` substitution at runtime

> 📖 See [Locator Resolution Guide](docs/locator-resolution.md) for the full pipeline.

---

### 🪝 Hook-Based Execution Pipeline
Reusable hooks like:
- WAIT_FOR_ELEMENT_VISIBLE  
- WAIT_FOR_ELEMENT_CLICKABLE  
- HIGHLIGHT_ELEMENT  
- WAIT_FOR_ANGULAR_LOADER  

> 📖 See [Hooks Guide](docs/hooks-guide.md) for composing custom pipelines.

---

### 🧠 Debug-Oriented Logging
- Color-coded logs with 8 built-in themes  
- Call-site tracing  
- Console + persistent logs  
- Semantic action methods (`click`, `dropdown`, `success`, `fallback`, …)

> 📖 See the full [`core.logging` README](src/main/java/core/logging/README.md) for theme reference and configuration.

---

### 🧭 Centralized Interactions API

All UI actions go through `Interactions`. Not some of them. All of them.

```java
app.interaction().clickOn(element);
app.interaction().selectFromDropdown(dropdown);
app.interaction().searchFor(searchField, "text");
```

This is the single surface area for DOM interaction — one class, one contract, full hook and logging integration on every call.

---

## 🧠 Example Usage

```java
VOID app = new VOID();

app.interaction().clickOn(ManageUsersElements.UserCards.LOGIN_AS_BUTTON);

app.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);

app.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);

String name = app.interaction().getText(ManageUsersElements.UserCards.FULL_NAME);

app.interaction().searchFor(CommonElements.GlobalSearch.SEARCH, "Deal Registration");
```

---

## 🧱 Architecture

```
Test → VOID → Interactions → LocatorResolver → WebDriver
                        ↓
                   Hooks + Logging + Context
```

Every layer enforces a contract. Tests talk to `VOID`. `VOID` delegates to `Interactions`. `Interactions` resolves locators, runs hooks, executes actions, and logs the result. There's no back door.

> 📖 See the full [Architecture Deep-Dive](docs/architecture.md) for details.

---

## 📂 Project Structure

```
void-framework/
├── src/main/java/
│   ├── core/
│   │   ├── runtime/             ← Entry point (VOID façade)
│   │   ├── interactions/        ← The single interaction surface
│   │   │   ├── Interactions.java
│   │   │   ├── Via.java         ← Static casting / locator / WebElement helpers
│   │   │   └── hooks/           ← ActionHandler, Before.*, After.* hook constants
│   │   ├── driver/              ← DriverFactory, DriverContext, Waiter
│   │   ├── logging/             ← CustomLogger, ANSI themes, LogConfig
│   │   ├── resolvers/locator/   ← LocatorResolvers, LocatorRequest, JSON/properties readers
│   │   └── utils/               ← ConfigLoader, DOMUtils, WaitUtils, Upload, TableHandler, …
│   ├── dsl/
│   │   └── VoidDSL.java         ← Context-driven DSL (optional intent layer)
│   ├── elements/
│   │   ├── api/                 ← Element interfaces — the contracts you implement
│   │   ├── meta/                ← ElementRole enum, EnumClassRegistry
│   │   └── meta/                ← ElementRole enum, EnumClassRegistry
│   ├── tests/demo/              ← Runnable demos and example page elements
│   │   ├── VoidDemo.java        ← Entry point (Action/Flow/FlowExecutor demo)
│   │   └── pages/               ← Example page element enums
│   └── StepDefinition/          ← Cucumber step definitions (optional adapter layer)
├── src/main/resources/
│   ├── config/                  ← driver.properties, test.properties
│   ├── locators/                ← .properties and .json locator files
│   ├── feature/                 ← Cucumber .feature files (optional)
│   └── log4j2.xml              ← Log4j 2 configuration
├── src/test/java/               ← Unit and integration tests
├── src/testNgXml/testng.xml     ← TestNG suite definition
└── docs/                        ← Architecture, quick-start, and configuration guides
```

---

## 🧰 Driver & Config

- Chrome, Firefox, Edge  
- Local / Grid / Selenoid  
- Headless, proxy, mobile emulation  
- Config via `driver.properties`  

> 📖 See the full [Configuration Reference](docs/configuration-reference.md) for all keys and layering behavior.

---

## 🧾 Logging Example

```
2026-04-24 13:15:37.584 │ INFO │ === InteractionsEndToEndTest starting === │ InteractionsEndToEndTest.setupClass ← TestMethodWorker.run
2026-04-24 13:15:37.663 │ DEBUG │ Setting driver for key: primary │ DriverContext.setPrimaryDriver ← Interactions.(constructor)
2026-04-24 13:15:37.668 │ DEBUG │ [get] key=locator.properties.base.path src=DEFAULT val=locators/properties/ │ ConfigLoader.get ← LocatorPaths.(static init)
2026-04-24 13:15:37.672 │ DEBUG │ [LOCATOR] Resolving: │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.673 │ DEBUG │           ├─ File        : test-locators.properties │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           ├─ Args        : [username] │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.674 │ DEBUG │           └─ Hardcoded   : false │ LocatorResolver.resolve ← LocatorResolver.resolveBest
2026-04-24 13:15:37.685 │ DEBUG │ Getting driver for key: primary │ DriverContext.getDriver ← DOMUtils.scrollToElement
```

---

## 🧠 Design Philosophy

VOID is consumed like an SDK — you add it as a dependency, not a project template.  
But internally, it's an opinionated system with its own execution model, element contracts, and resolution pipeline.

You're not expected to invent your own abstractions on top of Selenium and then wire VOID in sideways. You're expected to define elements as typed enums, declare locators externally, run actions through `Interactions`, and let the hook/logging pipeline do its job.

This is the deal: adopt the model, and the model gives you **structured, observable, debuggable automation** — the kind where a failure log tells you exactly what happened, not just that something went wrong.

Fight the model, and you're back to `driver.findElement(By.xpath("//div[3]/span[2]")).click()`.  
Which... technically works. In the same way that `goto` technically works.

---

## 🧪 Tech Stack

- Java 17  
- Selenium 4  
- TestNG  
- Cucumber (optional BDD adapter)  
- Jackson (JSON)
- Log4j 2
- Datafaker (test data generation)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Quick Start Guide](docs/architecture/quick-start.md) | Get up and running in under 10 minutes |
| [System Overview](docs/architecture/system-overview.md) | Full architecture and execution flow |
| [Configuration Reference](docs/architecture/configuration-reference.md) | All configuration keys, layering, and defaults |
| [Locator Resolution Guide](docs/architecture/locator-resolution.md) | Locator pipeline, formats, roles, and migration |
| [Hooks Pipeline](docs/architecture/hooks-pipeline.md) | Composable Before/After hook pipeline |
| [Logging README](src/main/java/core/logging/README.md) | CustomLogger themes, actions, and configuration |
| [JSON Locator Package](src/main/java/core/resolvers/locator/json/README.md) | JSON migration tool internals |
| [Dependency Audit](docs/audits/dependency-audit-2026-05.md) | Point-in-time dependency analysis |
| [Multi-Engine Prospect](docs/experiments/active/2026-05-01-multi-engine-execution.md) | Selenium ↔ Playwright portability design |
| [Changelog](CHANGELOG.md) | Version history and migration notes |
| [Contributing](CONTRIBUTING.md) | How to contribute to the project |

---

## 📜 License

MIT License © 2025–2026

---

## 🧩 Final Note

VOID won't magically fix bad test design.  
But it will make bad test design *extremely* visible.  
Which, arguably, is the more useful outcome.
