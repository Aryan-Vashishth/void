# Contributing to VOID

Thank you for considering contributing to VOID! This document outlines the standards, workflow, and expectations for contributions.

---

## 📋 Table of Contents

1. [Getting Started](#getting-started)
2. [Development Setup](#development-setup)
3. [Coding Standards](#coding-standards)
4. [Branching & Workflow](#branching--workflow)
5. [Commit Messages](#commit-messages)
6. [Pull Requests](#pull-requests)
7. [Testing](#testing)
8. [Documentation](#documentation)

---

## Getting Started

1. Fork the repository (or create a feature branch if you have write access).
2. Clone your fork and set up the development environment.
3. Make your changes following the guidelines below.
4. Submit a pull request.

---

## Development Setup

### Prerequisites

| Tool      | Version |
|-----------|---------|
| **Java**  | 17+     |
| **Maven** | 3.x     |
| **IDE**   | IntelliJ IDEA recommended (Lombok plugin required) |

### Build & Test

```bash
# Full build (skip tests for speed)
mvn clean install -DskipTests

# Run all tests
mvn clean test

# Run a specific test class
mvn test -Dtest=CustomLoggerTest

# Run a specific test method
mvn test -Dtest=InteractionsTest#clickOn_clickable_delegatesToSelenium
```

### IDE Setup

- Install the **Lombok** plugin and enable annotation processing.
- Set the project SDK to **Java 17**.
- Mark `src/main/java` and `src/test/java` as source/test roots.
- The TestNG suite is at `src/testNgXml/testng.xml`.

---

## Coding Standards

### General

- **Java 17** — use records, sealed classes, pattern matching, and text blocks where appropriate.
- Follow existing package conventions — don't create new top-level packages without discussion.
- Prefer composition over inheritance.
- Keep classes focused (Single Responsibility Principle).

### Enum-Driven Design

- UI elements **must** be modeled as enum constants implementing the appropriate `elements.api` interface.
- Each enum should carry its own locator key, external file reference, and dynamic args.
- Use `ElementRole` for locator role semantics — don't use raw strings for roles.

### Naming Conventions

| Entity          | Convention                              | Example                          |
|-----------------|-----------------------------------------|----------------------------------|
| Classes         | `PascalCase`                            | `LocatorResolver`                |
| Methods         | `camelCase`                             | `resolveLocator()`               |
| Constants       | `UPPER_SNAKE_CASE`                      | `WAIT_FOR_ELEMENT_VISIBLE`       |
| Packages        | `lowercase`, dot-separated              | `core.resolvers.locator.api`     |
| Test methods    | `methodName_scenario_expectedBehavior`  | `clickOn_nullElement_throwsNPE`  |

### Code Style

- **No wildcard imports** (`import foo.*`) except for `static` imports from `CustomLogger` and `AnsiColors`.
- **Max line length**: 120 characters (soft limit).
- Use `@Nullable` / `@NotNull` annotations on public API parameters and return types.
- Prefer `final` on fields, parameters, and local variables when the value is not reassigned.
- Static utility classes must have a `private` constructor.

### Logging

- Use `CustomLogger` (`info`, `debug`, `warn`, `error`) — never `System.out.println`.
- Use semantic action methods (`info.click(...)`, `debug.resolved(...)`) over generic `log(...)`.
- Initialize the logger in test classes: `CustomLogger.initialize(MyClass.class)`.

---

## Branching & Workflow

| Branch               | Purpose                                    |
|----------------------|--------------------------------------------|
| `main`               | Stable release branch                      |
| `develop`            | Integration branch for upcoming release    |
| `feature/<name>`     | New feature work                           |
| `bugfix/<name>`      | Bug fixes                                  |
| `chore/<name>`       | Refactors, dependency updates, tooling     |
| `docs/<name>`        | Documentation-only changes                 |

### Workflow

1. Branch from `develop` (or `main` for hotfixes).
2. Keep commits atomic and well-described.
3. Rebase on `develop` before submitting a PR.
4. Ensure all tests pass (`mvn clean test`).
5. Request a review.

---

## Commit Messages

Use the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short summary>

<optional body>

<optional footer>
```

### Types

| Type       | When to Use                              |
|------------|------------------------------------------|
| `feat`     | New feature                              |
| `fix`      | Bug fix                                  |
| `docs`     | Documentation only                       |
| `refactor` | Code change with no behavior change      |
| `test`     | Adding or updating tests                 |
| `chore`    | Build, CI, dependency changes            |
| `perf`     | Performance improvement                  |

### Examples

```
feat(interactions): add clickOnWithin for scoped clicks

fix(locator): handle null fileName in LocatorRequest

docs(readme): add prerequisites section and badges

refactor(logging): extract AnsiEscape from AnsiColors

test(hooks): add unit tests for Before.CLEAR_FIELD

chore(deps): bump selenium to 4.38.0
```

---

## Pull Requests

### Before Submitting

- [ ] All existing tests pass (`mvn clean test`).
- [ ] New code has unit tests (aim for >80% coverage on new classes).
- [ ] Public APIs have Javadoc (see [Documentation](#documentation)).
- [ ] No compiler warnings introduced.
- [ ] Changelog updated (if user-facing changes).

### PR Description

Include:
1. **What** — a brief summary of the change.
2. **Why** — the motivation or issue being resolved.
3. **How** — a high-level description of the approach.
4. **Testing** — how you verified the change.

---

## Testing

### Test Location

- **Unit tests**: `src/test/java/` — mirrors the main source structure.
- **Test resources**: `src/test/resources/` — test configs, locator files, uploads.
- **TestNG suite**: `src/testNgXml/testng.xml` — runs via `mvn test`.

### Test Guidelines

- Use **TestNG** (`@Test`, `@BeforeClass`, `@DataProvider`).
- Test method naming: `methodUnderTest_scenario_expectedOutcome`.
- Mock `WebDriver` and `WebElement` for unit tests — don't require a live browser.
- Use `@DataProvider` for parameterized tests.
- Tests must be idempotent and independent — no ordering dependencies.

### Test Categories

| Package        | What It Tests                              |
|----------------|-------------------------------------------|
| `core.*`       | Driver, logging, resolvers, utilities      |
| `elements.*`   | Element interface contracts, role maps     |

---

## Documentation

### Javadoc Requirements

All **public** classes, interfaces, methods, and constants must have Javadoc:

```java
/**
 * Scrolls a {@link WebElement} into the visible viewport using JavaScript.
 *
 * <p>Uses {@code scrollIntoView} with {@code block: 'center'} for optimal positioning.
 * Falls back gracefully if the element is stale or detached.</p>
 *
 * @param element the element to scroll into view
 * @see DOMUtils#hoverOnElement(WebElement)
 */
public static void scrollToElement(WebElement element) { ... }
```

### Package-Level Docs

Every package should have a `package-info.java` with a brief description.

### Markdown Docs

User-facing guides live in `/docs`. When adding a new feature:

1. Update the relevant guide (or create a new one).
2. Add a link in the README documentation table.
3. Keep code samples up to date.

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).

