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
9. [Versioning](#versioning)

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
| **IDE**   | IntelliJ IDEA recommended |

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
- **No compile-time code generation** — do not introduce Lombok, AutoValue, or similar annotation processors. All constructors, getters, setters, and utility methods must be explicit in the source for transparency and debuggability.

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
- [ ] `CHANGELOG.md` updated under `## [Unreleased]` for any user-facing change (see [Versioning](#versioning)).

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

## Versioning

This project follows [Semantic Versioning](https://semver.org/) and
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

### Version numbers

| Change | Bump | Example |
|--------|------|---------|
| Breaking API change (removal, rename, incompatible signature) | Major (`X.0.0`) | `0.2.0` to `1.0.0` |
| New feature, backwards-compatible | Minor (`0.X.0`) | `0.2.0` to `0.3.0` |
| Bug fix or internal correction, backwards-compatible | Patch (`0.0.X`) | `0.2.0` to `0.2.1` |

While the project is below `1.0.0`, a minor bump (`0.X.0`) may include breaking changes
if they are documented in the changelog under `### Removed` or `### Changed`.

### Updating CHANGELOG.md

Every user-facing change must have an entry in `CHANGELOG.md` under `## [Unreleased]`
before it is merged. Purely internal changes (refactors with no API impact, test-only
changes, build tooling) do not require a changelog entry.

**Sections -- use only what applies to your change:**

| Section | What belongs here |
|---------|-------------------|
| `Added` | New public API, new capabilities, new config options |
| `Changed` | Modified behaviour in existing features |
| `Deprecated` | Features marked for removal in a future version |
| `Removed` | API or behaviour removed in this release |
| `Fixed` | Bug fixes |
| `Security` | Vulnerability fixes |

**Entry format:**

```markdown
## [Unreleased]

### Added

- **FeatureName** -- one-line summary of what was added
  - Sub-bullet for additional detail if needed

### Changed

- **ClassName.method()** -- what changed and why (link an ADR if a decision was made)

### Removed

- **`OldClass`** -- removed; use `NewClass` instead (see Migration table below)
```

Keep entries in the same style as the existing changelog: bold the subject, follow with
a double-hyphen and a short description.

### Cutting a release

1. Replace `## [Unreleased]` with the new version and today's date:
   ```
   ## [0.3.0] - 2026-xx-xx
   ```
2. Add a new empty `## [Unreleased]` section above it.
3. Update the comparison links at the bottom of `CHANGELOG.md`:
   ```
   [Unreleased]: .../compare/v0.3.0...HEAD
   [0.3.0]: .../compare/v0.2.0...v0.3.0
   ```
4. Tag the release commit: `git tag v0.3.0`.

### What does NOT go in the changelog

- Commits with type `chore`, `test`, `style`, or `refactor` that have no public API
  impact -- let the commit message carry that information.
- In-progress work -- entries belong in `[Unreleased]` only when the change is complete
  and merged. Use the `docs/plan/` directory for pre-implementation planning.

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).

