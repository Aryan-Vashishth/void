# VOID

Structured UI automation around a single execution path:

**Element → Action → Flow → FlowExecutor → UIEngine**

VOID is engine-agnostic (Selenium today, Playwright-ready by contract), and not a Page Object framework.

Test code does not touch WebDriver, locators, or wait logic.

Elements emit actions, actions form flows, and `FlowExecutor` runs those flows through a `UIEngine` that handles everything underneath.

You describe what should happen. VOID handles how — and tells you exactly when it can’t.

![Java](https://img.shields.io/badge/Java-17+-blue?logo=openjdk)
![Selenium](https://img.shields.io/badge/Selenium-4.38-green?logo=selenium)
![Playwright](https://img.shields.io/badge/Playwright-Ready-45ba63?logo=playwright)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Architecture

![VOID execution pipeline](docs/images/architecture.png)

## Run the demo

```bash
git clone https://github.com/Aryan-Vashishth/void-framework.git
cd void-framework
mvn -B test -Dtest=VoidDemo
```

Expected output:

```text
[VOID] Engine: SeleniumEngine
[VOID] Flow start: login
[VOID] -> type(USERNAME_INPUT, "tomsmith")
[VOID] -> type(PASSWORD_INPUT, "******")
[VOID] -> click(LOGIN_BUTTON)
[VOID] Flow end: login (3 actions, 1.2s)
```

Sample report snapshot:

![Allure sample report](docs/images/allure-report-sample.png)

[View sample Allure report](docs/images/allure-report-sample.png)

---

## TL;DR

```java
import core.executor.FlowExecutor;
import core.flow.Flow;
import core.runtime.VOID;
import tests.demo.pages.DemoLoginPage;

VOID app = VOID.start();
FlowExecutor executor = new FlowExecutor(app.getEngine());

executor.run(Flow.of(
    DemoLoginPage.Credentials.USERNAME_INPUT.type("tomsmith"),
    DemoLoginPage.Credentials.PASSWORD_INPUT.type("SuperSecretPassword!"),
    DemoLoginPage.Button.LOGIN_BUTTON.click()
));

app.shutdown();
```

---

## Single Execution Path

VOID enforces one execution path:

```text
Element → Action → Flow → FlowExecutor → UIEngine
```

There is no alternative path for new code.
No direct WebDriver calls.
No bypassing the system.

If you bypass this path, you are outside the system.

This constraint is what makes behavior predictable and debuggable.

---

## Mental Model

| Layer | Responsibility | What it should do | What it should not do |
|---|---|---|---|
| `Element` | typed UI contract | declare roles and locator keys | execute browser actions |
| `Action` | deferred intent | describe what should happen | touch WebDriver / `By` |
| `Flow` | composition | group actions into a workflow | execute anything |
| `FlowExecutor` | flow executor | execute actions in order | resolve locators outside action flow |
| `UIEngine` | browser executor | waits, retries, scroll, click, type, screenshot | act as test API |

```text
Element → Action → Flow → FlowExecutor → UIEngine
```

This is the only execution path new code should use.

---

## What VOID is / is not

### VOID is
- a structured automation system
- enum-based element modeling with capability interfaces
- deferred execution through actions and flows
- execution through an engine that owns waits, retries, and browser interaction
- externalized, role-based locator resolution

### VOID is not
- a Selenium-only or Playwright-only wrapper API for direct test code
- a Page Object Model framework centered around mutable page classes
- a place to call `By.xpath(...)`, `WebDriver`, or raw DOM helpers from tests

---

## Elements

Elements are defined as enums implementing capability interfaces.
The capability determines which kind of `Action` an element can emit.

Real example from `tests.demo.pages.DemoLoginPage`:

```java
public interface DemoLoginPage {

    String LOCATOR_FILE = "demo-login-elements.json";

    enum Credentials implements Typeable {
        USERNAME_INPUT("USERNAME_INPUT"),
        PASSWORD_INPUT("PASSWORD_INPUT");

        private final String key;
        Credentials(String k) { this.key = k; }

        @Override public String getInputLocator()     { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    enum Button implements Clickable {
        LOGIN_BUTTON("LOGIN_BUTTON", "Login");

        private final String key;
        private final String label;
        Button(String k, String l) { this.key = k; this.label = l; }

        @Override public String getTriggerLocator()   { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[]{label}; }
    }
}
```

Common capability interfaces live under:
- `elements/api/capability/Clickable.java`
- `elements/api/capability/Typeable.java`
- `elements/api/capability/Selectable.java`
- `elements/api/capability/ReadOnly.java`
- `elements/api/capability/Hoverable.java`

---

## Actions

Elements do not execute.
They emit `Action`.

Examples from the current API:

```java
DemoLoginPage.Button.LOGIN_BUTTON.click();
DemoLoginPage.Credentials.USERNAME_INPUT.type("tomsmith");
DemoLoginPage.Credentials.PASSWORD_INPUT.typeAndPress("secret", "ENTER");
```

Key idea:
- `Action` = deferred intent
- no browser work happens when the action is created
- execution happens later when `FlowExecutor` calls `perform(UIEngine)`

---

## Flow + FlowExecutor

Use `Flow` to compose actions and `FlowExecutor` to run them.

```java
import core.executor.FlowExecutor;
import core.flow.Flow;
import core.runtime.VOID;
import tests.demo.pages.DemoLoginPage;

VOID app = VOID.start();
FlowExecutor executor = new FlowExecutor(app.getEngine());

Flow login = Flow.of(
    DemoLoginPage.Credentials.USERNAME_INPUT.type("tomsmith"),
    DemoLoginPage.Credentials.PASSWORD_INPUT.type("SuperSecretPassword!"),
    DemoLoginPage.Button.LOGIN_BUTTON.click()
);

executor.run(login);
```

Single action execution also works:

```java
executor.run(DemoLoginPage.Button.LOGIN_BUTTON.click());
```

> Legacy note: `core.interactions.Interactions` still exists for compatibility, but it is deprecated and should not be the main API for new code.

---

## Hooks

Hooks are applied at the action layer via `Action.withHooks(...)`.

Hooks wrap actions. They do not change execution — they add behavior around it.
They operate on the same action pipeline, not outside it.
They are part of execution, not a separate layer.

```java
import core.flow.Flow;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;

Flow login = Flow.of(
    DemoLoginPage.Credentials.USERNAME_INPUT.type("tomsmith")
        .withHooks(
            java.util.List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
            java.util.List.of(After.HIGHLIGHT_ELEMENT)
        ),
    DemoLoginPage.Credentials.PASSWORD_INPUT.type("SuperSecretPassword!")
        .withHooks(
            java.util.List.of(Before.CLEAR_FIELD),
            java.util.List.of(After.HIGHLIGHT_ELEMENT)
        ),
    DemoLoginPage.Button.LOGIN_BUTTON.click()
        .withHooks(
            java.util.List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE),
            java.util.List.of(After.HIGHLIGHT_ELEMENT)
        )
);
```

Current codebase supports:
- action-level hook composition via `withHooks(...)`
- hook execution through the normal action/flow pipeline

FlowExecutor-level hooks are not implemented as a separate public API in the current codebase.

---

## UIEngine

`UIEngine` is where browser complexity lives.

It owns behavior such as:
- waits
- retries
- scroll into view
- click/type execution
- screenshots
- hover/highlight
- native-driver integration

You don’t deal with that complexity.

Test code should think in terms of:
- elements
- actions
- flows
- `FlowExecutor`

Not:
- `WebDriver`
- `By`
- raw engine calls for ordinary UI interactions

---

## Locators

Locators are externalized and role-based.

Tests never use `By`.
Elements declare logical locator roles (`TRIGGER`, `INPUT`, `TEXT`, etc.).
Locator values live in external `.json` / `.properties` files.
Resolution happens inside the framework, not in test code.

Example from the demo page:
- `DemoLoginPage.Credentials.USERNAME_INPUT` → role `INPUT`
- `DemoLoginPage.Button.LOGIN_BUTTON` → role `TRIGGER`
- locator file: `demo-login-elements.json`

---

## Project Structure

```text
void-framework/
├── src/main/java/
│   ├── core/
│   │   ├── actions/                # Action contract + action factories
│   │   ├── flow/                   # Flow composition
│   │   ├── executor/               # FlowExecutor
│   │   ├── engine/                 # UIEngine + engine implementations
│   │   ├── runtime/                # VOID facade / startup
│   │   ├── interactions/hooks/     # Action hook library (legacy package location)
│   │   ├── resolvers/locator/      # Locator resolution infrastructure
│   │   └── driver/                 # Driver lifecycle and bootstrap support
│   ├── elements/
│   │   ├── api/                    # Element contracts
│   │   └── meta/                   # ElementRole and metadata
│   └── tests/demo/
│       ├── VoidDemo.java           # Current Action/Flow/FlowExecutor demo
│       └── pages/                  # Demo element enums
├── src/main/resources/
│   └── locators/                   # External locator definitions
└── docs/
    ├── architecture/              # Architecture docs
    └── audits/                    # Audit reports
```

Legacy compatibility remains under `core/interactions/`, but it is not the primary model.

---

## Current setup path

The execution model is already in place.
The runtime convenience layer is still catching up.

Today, the typical setup is:

```java
VOID app = VOID.start();
FlowExecutor executor = new FlowExecutor(app.getEngine());
```

That is current codebase reality.
The recommended mental model remains:

```text
Element → Action → Flow → FlowExecutor → UIEngine
```

---

## Minimal example with the demo page

```java
import core.executor.FlowExecutor;
import core.flow.Flow;
import core.runtime.VOID;
import tests.demo.pages.DemoLoginPage;

public class Example {
    public static void main(String[] args) {
        VOID app = VOID.start();
        FlowExecutor executor = new FlowExecutor(app.getEngine());

        try {
            app.getEngine().navigateTo("https://the-internet.herokuapp.com/login");

            executor.run(Flow.of(
                DemoLoginPage.Credentials.USERNAME_INPUT.type("tomsmith"),
                DemoLoginPage.Credentials.PASSWORD_INPUT.type("SuperSecretPassword!"),
                DemoLoginPage.Button.LOGIN_BUTTON.click()
            ));
        } finally {
            app.shutdown();
        }
    }
}
```

This mirrors `src/main/java/tests/demo/VoidDemo.java`.

---

## Documentation

| Document | Description |
|----------|-------------|
| `docs/architecture/quick-start.md` | Getting started walkthrough |
| `docs/architecture/system-overview.md` | Architecture and execution flow |
| `docs/architecture/configuration-reference.md` | Config keys and behavior |
| `docs/architecture/logging-reference.md` | Log channels, folder layout, and trace depth |
| `docs/architecture/locator-resolution.md` | Locator roles and resolution pipeline |
| `docs/architecture/hooks-pipeline.md` | Hook behavior and composition |
| `docs/audits/architecture-audit-2026-05.md` | Current architecture audit |
| `CHANGELOG.md` | Version history |
| `CONTRIBUTING.md` | Contribution guide |

---

## License

MIT License © 2025–2026

