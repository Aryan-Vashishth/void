# VOID Framework — External Readiness Audit

**Subject**: `Aryan-Vashishth/void-framework` (branch `feature/engine-abstraction`)
**Auditor lens**: Senior engineer / hiring manager opening this repo for the first time
**Goal**: Identify what's blocking the project from doing its real job — being a credible portfolio anchor in interviews and recruiter screens
**Audit date**: May 2026
**Version reviewed**: `2.0-SNAPSHOT`

---

## 1. Executive Summary

### TL;DR

**The substance is real. The packaging is not.**

VOID is materially more developed than the resume implies: ~101 main Java files, 39 test files, an internal architecture audit, ADR-style decision records, a working CHANGELOG with migration notes, and a thoughtfully designed execution pipeline. Internal engineering quality is genuinely above average for a solo project at this YOE.

But a first-time visitor — recruiter, hiring engineer, future teammate — lands on a repo that has:
- No CI signal (no `.github/workflows/`, no status badge)
- A personal classpath dump (`cp.txt`) leaking your Windows username at the repo root
- A `2.0-SNAPSHOT` version with no tagged releases
- A "Playwright Ready" badge that the code does not back up
- A confusing 8-folder docs structure that mixes drafts, ideas, and archives into the public surface
- Stale package-info references and a `StepDefinition/` directory outside any sensible package

The fix is not more code. The fix is **two weekends of packaging polish**.

### Verdict

| Dimension | Rating |
|---|---|
| Internal engineering quality | Strong |
| Public-facing presentation | Weak |
| Interview-narrative readiness | Medium — strong substance, weak visible artifacts |
| Distance from "portfolio-ready" | 2 weekends of focused work |

---

## 2. Methodology

What was inspected:
- Repository top-level: `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `pom.xml`, `.gitignore`, `cp.txt`
- Source tree: `src/main/java/` (full traversal), `src/test/java/`
- Build configuration: Maven POM, surefire/testng setup, `src/testNgXml/testng.xml`
- CI/CD configuration: absent — confirmed no `.github/`, no `Jenkinsfile`, no `*.yml` workflows
- Documentation tree: `docs/` (all 8 numbered subdirectories)
- Architecture artifacts: `docs/5-architecture/system-overview.md`, `docs/6-audits/architecture-audit-2026-05.md`
- Branch state: `feature/engine-abstraction` (only branch fetched; `main` state unknown from this snapshot)

What was not inspected:
- Runtime behavior (no `mvn test` execution)
- Test coverage metrics
- Static analysis output (SpotBugs, PMD, SonarLint)

---

## 3. Strengths (Do Not Change These)

These are working. The audit recommendations below should preserve all of these.

### 3.1 README is genuinely above average

`README.md` includes things most solo-project READMEs lack:
- Clear positioning statement in the first three lines
- A working code example in the TL;DR section
- A **"What VOID is / is not"** framing — excellent for interview articulation
- A **layer responsibility table** with explicit "what it should do / not do" columns
- Real code examples from `examples.pages.DemoLoginPage`, not pseudocode
- A documentation index at the bottom

### 3.2 The execution-model design is coherent

`Element → Action → Flow → FlowExecutor → UIEngine` is a real architectural commitment, not a slogan. The code follows it:
- `core/actions/` contains the action contracts
- `core/flow/Flow.java` is the composition layer
- `core/executor/FlowExecutor.java` is the runner
- `core/engine/UIEngine.java` is the interface; `core/engine/selenium/SeleniumEngine.java` is the only implementation
- `elements/api/capability/` holds the capability interfaces (`Clickable`, `Typeable`, etc.)

This is not "I wrote a wrapper around WebDriver." It is a designed system. Defend it as such.

### 3.3 ADR-style decision records

`docs/4-decisions/accepted/` (and the archived copies referenced from system-overview.md) contains numbered decision records:
- 001 — Remove WebDriverManager
- 002 — Cucumber as Optional Dependency
- 003 — No Compile-Time Code Generation (no Lombok)
- 004 — Dependency Philosophy
- 005 — Logging Architecture
- 006 — Replace JavaFaker with Datafaker
- 007 — UIEngine as Single Execution Authority
- 008 — Capability Interfaces
- 009 — Action / Flow / FlowExecutor Execution Model
- 010 — Hook Evolution

ADRs at a solo-project level are unusually mature. Hiring managers notice this. Keep doing it.

### 3.4 Self-authored architecture audit

`docs/6-audits/architecture-audit-2026-05.md` honestly flags the legacy `Interactions` class, Selenium leakage in `VoidDSL`, dual-return locator resolver, and incomplete refactor. **Self-critique at this level is rare.** It will read as engineering maturity in interviews — *if you address the items in the audit and can speak to it.*

### 3.5 CHANGELOG with real migration notes

`CHANGELOG.md` includes a binary-breaking-changes section, a migration table (old call → new call), and traceable rationale. This is professional-grade behavior.

### 3.6 Test coverage exists

39 test files under `src/test/java/`, organized parallel to main sources (`core/`, `elements/`, `resolvers/`, `interactions/`, `logging/`). Not a token "Hello World" test suite.

### 3.7 Modern dependency choices

- Java 17 (current LTS-appropriate)
- Selenium 4.43 (current)
- Jackson via BOM (best practice)
- Datafaker (replaced legacy JavaFaker per ADR-006)
- Log4j 2.25
- JSR-305 for nullability annotations
- TestNG 7.11 (current)

No outdated dependency rot. Pom hygiene is good.

---

## 4. Findings — Critical Severity

> "Critical" means: a first-time visitor's first impression collapses before they read your code.

### 4.1 ❌ `cp.txt` at the repository root — DELETE TODAY

**Location**: `/cp.txt` (8KB, committed to repo)

**What it is**: A Windows classpath dump showing fully-qualified jar paths from your local Maven repository.

**Sample line** (paraphrased): `C:\Users\AryanVashishth\.m2\repository\org\seleniumhq\selenium\selenium-java\4.38.0\selenium-java-4.38.0.jar;...`

**Why this is critical**:

1. **Personal information disclosure**: The file leaks your local Windows username (`AryanVashishth`). Acceptable in isolation, but it signals "I committed something without reading what I committed."
2. **Dependency-drift evidence**: The classpath references `OpenTelemetry 1.55.0`, `snakeyaml 2.3`, `libphonenumber 8.13.50`, `jspecify 1.0.0`, `rgxgen 2.0`, and `Selenium 4.38.0` — none of which appear in the current `pom.xml` (which declares Selenium 4.43.0). This means either (a) the classpath dump is stale, or (b) the project has transitive deps that warrant deliberate review. Either way, a reviewer asks: "why is this file here?"
3. **Engineering hygiene signal**: Top-level `cp.txt` files are the kind of artifact a careful engineer never commits. It's not destructive, just unprofessional. In a recruiter or hiring-manager skim, it costs more than its size suggests.

**Fix**:
```bash
git rm cp.txt
echo "cp.txt" >> .gitignore
echo "*.classpath.txt" >> .gitignore
git commit -m "chore: remove local classpath dump from repo root"
```

**Severity**: Critical (visible at the root listing, before any code is read).

---

### 4.2 ❌ No CI/CD pipeline exists

**Evidence**: No `.github/workflows/` directory. No `Jenkinsfile`. No `*.yml` or `*.yaml` outside `target/` builds (none found).

**Why this is critical**:

In 2026, a public Java project of this scope **without** a green CI badge reads as either abandoned or experimental. A hiring engineer's mental model:
- Sees a CI badge → "this project actually compiles and examples pass" → proceeds to look at code
- Sees no CI badge → "is this even buildable?" → leaves

This is the single highest-ROI fix on the entire project. It also unlocks every downstream improvement (publishing Allure reports, releasing artifacts, status visibility).

**Fix**: See Section 7.1 for a drop-in workflow.

**Severity**: Critical (first 5 seconds of repo visit).

---

### 4.3 ❌ "Playwright Ready" badge overclaims

**Location**: `README.md` line 17

**Current claim**: `![Playwright](https://img.shields.io/badge/Playwright-Ready-45ba63?logo=playwright)`

**Reality**:
- `src/main/java/core/engine/` contains only `selenium/SeleniumEngine.java`
- No `playwright/` package
- No `PlaywrightEngine` class
- No Playwright dependency in `pom.xml`

The README body softens it correctly: *"engine-agnostic (Selenium today, Playwright-ready by contract)"* — that's honest. But the badge does not match the body.

**Why this matters**:

Interview probe sequence (likely):
- Interviewer: "I see Playwright-ready. Can you walk me through the Playwright engine?"
- You: "It's not implemented yet — the architecture supports it."
- Interviewer: "But your badge says 'Ready.'"

This is a controllable wound. Overclaiming on a small badge undermines the genuinely strong architectural work everywhere else in the README.

**Fix options** (pick one):

**A. Soften the badge**:
```markdown
![Playwright](https://img.shields.io/badge/Playwright-pluggable-yellow?logo=playwright)
```

**B. Remove the badge** until a `PlaywrightEngine` ships.

**C. Ship a minimal `PlaywrightEngine`** in 1–2 weekends — this is actually feasible given the abstraction quality. Then the badge becomes truthful and the resume gets its best single bullet.

**Recommendation**: B in the short term, C as a Month 4 deliverable.

**Severity**: Critical (because it's the easiest claim to fact-check in an interview).

---

## 5. Findings — High Severity

> "High" means: visible during a 60-second skim of the repo, hurts perception, fixable in <2 hours each.

### 5.1 `pom.xml` placeholder identity

**Location**: `pom.xml` lines 7–9

```xml
<groupId>org.example</groupId>
<artifactId>VOID</artifactId>
<version>2.0-SNAPSHOT</version>
```

**Issues**:
- `org.example` is the IntelliJ default placeholder. Reads as "never finished setup."
- `artifactId: VOID` (uppercase) violates Maven convention. Standard is lowercase-hyphenated.
- `2.0-SNAPSHOT` with **no tagged releases ever** signals perpetual WIP.

**Fix**:
```xml
<groupId>io.github.aryan-vashishth</groupId>
<artifactId>void-framework</artifactId>
<version>0.1.0</version>
```

Use `io.github.<your-username>` even if you never publish to Maven Central — it's the convention for OSS projects and instantly removes the "placeholder" feel.

Then tag the release:
```bash
git tag -a v0.1.0 -m "Initial public release: Selenium engine, action/flow execution model, locator resolution, hook pipeline"
git push origin v0.1.0
```

**Severity**: High (sub-30-second skim).

---

### 5.2 README missing visual proof

The README has shields.io badges (Java/Selenium/Maven/License) but lacks:
- ❌ A CI status badge (will exist after Section 7.1)
- ❌ An architecture diagram image — currently uses ASCII pipeline notation only
- ❌ A screenshot of a passing run / Allure report
- ❌ A "Run the demo in 30 seconds" section with clone → command → expected output

ASCII pipelines are fine, but a real diagram (Excalidraw export to PNG, committed to `docs/images/architecture.png`) significantly raises perceived professionalism.

**Fix**: Add to README, near the top:

```markdown
## Architecture

![VOID execution pipeline](docs/images/architecture.png)

## Run the demo

```bash
git clone https://github.com/Aryan-Vashishth/void-framework.git
cd void-framework
mvn -B test -Dtest=VoidDemo
```

Expected output:
```
[VOID] Engine: SeleniumEngine
[VOID] Flow start: login
[VOID] → type(USERNAME_INPUT, "tomsmith")
[VOID] → type(PASSWORD_INPUT, "******")
[VOID] → click(LOGIN_BUTTON)
[VOID] Flow end: login (3 actions, 1.2s)
```

[View sample Allure report →](https://aryan-vashishth.github.io/void-framework/)
```

**Severity**: High.

---

### 5.3 `docs/` numeric-prefix structure is confusing on a public repo

**Current state**:
```
docs/
├── 0-ideas/
├── 1-drafts/
├── 2-active/
├── 3-review/
├── 4-decisions/
├── 5-architecture/
├── 6-audits/
└── 7-archive/         (472KB — largest subfolder)
```

**Problem**: This is a personal knowledge-management system (Zettelkasten / PARA-like). It works for *your* note-taking. It confuses *every* first-time visitor.

A visitor wants to find: "where's the architecture overview?" — currently in `docs/5-architecture/system-overview.md`. The number prefix is meaningless to them.

**Worse**: `docs/0-ideas/`, `docs/1-drafts/`, `docs/3-review/`, `docs/7-archive/` are all visible at the same level as polished architecture docs. Visitors see "drafts" and "ideas" as public-facing, which downgrades perception.

**Fix**: Restructure for public consumption:
```
docs/
├── architecture/          # System overview, locator resolution, hooks, etc.
├── decisions/             # ADRs (currently 4-decisions/accepted)
├── audits/                # Architecture + dependency audits
├── images/                # Diagrams, screenshots
└── README.md              # Index
```

Move `0-ideas/`, `1-drafts/`, `2-active/`, `3-review/`, `7-archive/` to a **private notes repo** or to a `.notes/` directory in `.gitignore`. The public docs surface should only show finished work.

Also: the README references `docs/architecture/quick-start.md` but the file is at `docs/5-architecture/quick-start.md`. **Broken link in the README's documentation index.** Fix either the path or the structure.

**Severity**: High (perception + broken links).

---

### 5.4 Stale `package-info.java` references

**Location**: `src/main/java/StepDefinition/package-info.java`

```java
/**
 * Cucumber step-definition classes that wire BDD feature files to the
 * {@link interactions.Interactions} and {@link automation.interactions.VoidDSL}
 * layers.
 */
package StepDefinition;
```

**Issues**:
1. **`@link interactions.Interactions`** — that package does not exist. Actual location is `core.interactions.Interactions`.
2. **`@link automation.interactions.VoidDSL`** — that package does not exist. Actual location is `dsl.VoidDSL`.
3. **`package StepDefinition;`** — Java convention is lowercase, hyphen-free. Capitalized package names are non-idiomatic and break some tooling.
4. Located at `src/main/java/StepDefinition/` — outside any sane package hierarchy. Should be under `core/adapters/cucumber/` or similar.

**Fix**:
- Either delete this directory (Cucumber is declared `optional` in pom.xml — does it actually need step definitions in core?)
- Or move to `src/main/java/core/adapters/cucumber/` and fix the package declaration + javadoc links.

**Severity**: High (broken javadoc + naming-convention violation visible in directory listing).

---

## 6. Findings — Medium Severity

> "Medium" means: noticed by a careful reviewer; not deal-breakers but accumulate.

### 6.1 Legacy `Interactions.java` is 833 lines and still present

**Location**: `src/main/java/core/interactions/Interactions.java` (833 LOC), plus `Via.java` (303 LOC).

Your own architecture audit (`docs/6-audits/architecture-audit-2026-05.md`) correctly flags this as:
> *"a large (~834-line) legacy `Interactions` class and supporting utilities (`Via`, `UIContext`, `DriverContext`, `VoidDSL`) that still reference Selenium types directly."*

The audit calls it `⚠️ Selenium leakage`.

**Two acceptable resolutions**:

A. **Commit to deletion** with a CHANGELOG entry: *"Removed in v0.2.0: legacy Interactions API. Migration: use Element → Action → Flow."* Then schedule deletion in 1–2 minor versions.

B. **Delete now** if the new pipeline is feature-complete enough to cover all current call sites.

What's not acceptable for a portfolio piece: leaving the audit's critical finding unaddressed indefinitely. Interviewers who read the audit will ask.

**Severity**: Medium (interview liability if someone reads your audit).

---

### 6.2 `VoidDSL` is `⚠️ Delegates to Interactions, imports By/WebElement`

Same audit. `src/main/java/dsl/VoidDSL.java` couples the DSL layer to Selenium types, contradicting the "engine-agnostic" core thesis.

Decision required: is `VoidDSL` part of the system, or part of the legacy migration path? Either keep and decouple, or deprecate alongside `Interactions`.

**Severity**: Medium.

---

### 6.3 `src/main/java/StepDefinition/` violates package conventions

Already covered in 5.4. The directory itself is non-idiomatic placement; it sits as a top-level Java sibling of `core/`, `elements/`, `dsl/`, `examples/`. Top-level directories should map to packages in `groupId` namespace (or at minimum follow lowercase convention).

**Severity**: Medium (visible in source tree).

---

### 6.4 README badges include no CI status

Once Section 7.1 is implemented, the README needs:
```markdown
[![CI](https://github.com/Aryan-Vashishth/void-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/Aryan-Vashishth/void-framework/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Aryan-Vashishth/void-framework)](https://github.com/Aryan-Vashishth/void-framework/releases)
```

**Severity**: Medium (depends on 4.2 being fixed first).

---

### 6.5 No `LICENSE` file at root (only mentioned in README)

The README states *"MIT License © 2025–2026"* but `ls -la` shows no `LICENSE` file at the root. GitHub's License detection relies on the file being present.

**Fix**: Add `LICENSE` (standard MIT template) at the repo root. GitHub will then surface "MIT License" in the sidebar, increasing perceived legitimacy.

**Severity**: Medium.

---

### 6.6 Single branch visible: `feature/engine-abstraction`

The clone shows only `feature/engine-abstraction` as the active branch. If the default branch on GitHub is also this feature branch (and not `main`), visitors land on a feature branch by default — which signals "main is stale" or "default branch isn't set."

**Action**:
- Verify GitHub default branch.
- Once `feature/engine-abstraction` is stable, merge to `main`, tag `v0.1.0`, and set `main` as default.

**Severity**: Medium.

---

## 7. Drop-In Fixes (Copy / Adapt / Commit)

### 7.1 GitHub Actions CI Workflow

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main, "feature/**"]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    strategy:
      fail-fast: false
      matrix:
        java: [17, 21]

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: temurin
          cache: maven

      - name: Build & run unit examples
        run: mvn -B -ntp clean test

      - name: Upload surefire reports on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports-jdk${{ matrix.java }}
          path: target/surefire-reports/
          if-no-files-found: ignore

      - name: Upload logs on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: void-logs-jdk${{ matrix.java }}
          path: logs/
          if-no-files-found: ignore
```

This:
- Runs on every push and PR.
- Tests against Java 17 and 21 (proves the "Java 17+" claim in the README).
- Caches Maven deps (faster runs).
- Uploads diagnostic artifacts on failure (showcases your existing logging discipline).

After committing this, add the badge to README line 15-ish:

```markdown
[![CI](https://github.com/Aryan-Vashishth/void-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/Aryan-Vashishth/void-framework/actions/workflows/ci.yml)
```

---

### 7.2 `pom.xml` identity fix

Replace lines 7–9:

```xml
<groupId>io.github.aryan-vashishth</groupId>
<artifactId>void-framework</artifactId>
<version>0.1.0</version>
<name>VOID Framework</name>
<description>Engine-agnostic Java UI automation framework with a deterministic action/flow execution model.</description>
<url>https://github.com/Aryan-Vashishth/void-framework</url>

<licenses>
    <license>
        <name>MIT License</name>
        <url>https://opensource.org/licenses/MIT</url>
    </license>
</licenses>

<developers>
    <developer>
        <name>Aryan Vashishth</name>
        <url>https://github.com/Aryan-Vashishth</url>
    </developer>
</developers>

<scm>
    <connection>scm:git:git://github.com/Aryan-Vashishth/void-framework.git</connection>
    <developerConnection>scm:git:ssh://git@github.com/Aryan-Vashishth/void-framework.git</developerConnection>
    <url>https://github.com/Aryan-Vashishth/void-framework</url>
</scm>
```

This block is what any Maven Central or polished OSS project carries. Even without publishing, the metadata signals "this is a real artifact."

---

### 7.3 `.gitignore` additions

Append to `.gitignore`:

```
# Local classpath dumps
cp.txt
*.classpath.txt

# IDE
.vscode/
*.swp
.idea/inspectionProfiles/
.idea/sonarlint/

# OS
.DS_Store
Thumbs.db

# Local notes (if you move personal docs out of public docs/)
.notes/

# Allure
allure-results/
allure-report/
```

---

### 7.4 LICENSE file

Create `LICENSE` at the repo root with the standard MIT template (replace `[year]` and `[fullname]`):

```
MIT License

Copyright (c) 2025-2026 Aryan Vashishth

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 8. Sequenced Action Plan

### Today (1–2 hours)

1. ✅ `git rm cp.txt` + commit. Add to `.gitignore`.
2. ✅ Add `LICENSE` file at repo root.
3. ✅ Fix `pom.xml` identity block (groupId, artifactId, version, metadata).
4. ✅ Soften or remove the "Playwright Ready" badge in README.

### This weekend (4–6 hours)

5. ✅ Add `.github/workflows/ci.yml` from Section 7.1. Push, verify green build.
6. ✅ Add CI badge to README.
7. ✅ Tag `v0.1.0`, write release notes (use the existing CHANGELOG as the source).
8. ✅ Create the architecture diagram (Excalidraw → PNG → commit to `docs/images/`). Embed in README.
9. ✅ Add the "Run the demo" section in README with the expected output snippet.
10. ✅ Fix the `StepDefinition/` directory: either delete it or move to `core/adapters/cucumber/`. Fix `package-info.java` javadoc links.

### Next weekend (4–6 hours)

11. ✅ Restructure `docs/`: flatten to `docs/architecture/`, `docs/decisions/`, `docs/audits/`, `docs/images/`. Move `0-ideas/`, `1-drafts/`, `2-active/`, `3-review/`, `7-archive/` out of the public surface.
12. ✅ Fix broken doc links in the README's documentation index.
13. ✅ Decide on `Interactions` and `VoidDSL`: deletion timeline or deprecation policy. Document in CHANGELOG.
14. ✅ Verify GitHub default branch is `main` and that `main` reflects current shipped state.

### Within 30 days

15. ✅ Wire Allure (if not already integrated in the test runs).
16. ✅ Publish a sample Allure report to GitHub Pages. Link from README.
17. ✅ Either ship a minimal `PlaywrightEngine` OR commit to "engine-pluggable" wording everywhere.
18. ✅ Write a blog post: *"Designing an Engine-Agnostic UI Automation Framework"* — link from README and LinkedIn.

---

## 9. Interview Talking Points (Convert Findings into Stories)

Each weakness becomes a strength if you can talk about it well.

### "Walk me through your architecture audit"

You wrote one. That's already differentiating. Specific talking points:
- *"I noticed the new pipeline coexisted with an 833-line legacy class that still leaked Selenium types. I documented the migration in the audit and committed to a removal timeline."*
- *"The audit identified the `VoidDSL` layer as a transitional artifact — it imports `By` and `WebElement` directly, contradicting the engine-agnostic core. The decision was to deprecate it rather than retrofit, because retrofitting would expand the engine-abstraction contract beyond what consumers need."*

### "How do you handle architectural decisions in a solo project?"

- *"I use ADRs. Every non-trivial decision — removing WebDriverManager, making Cucumber optional, choosing Datafaker over JavaFaker, defining the UIEngine as the single execution authority — has a numbered decision record with context, options considered, and consequences. It's the same practice teams use; I do it solo to keep myself honest."*

### "What's the hardest part of an engine-agnostic design?"

- *"The locator layer. Selenium uses `By`; Playwright uses string selectors with engine semantics. My resolver produces a `LocatorDescriptor` — an engine-agnostic value object — and each engine adapter consumes it. The hardest sub-problem was role-based locator templates with dynamic substitution (`%s` args in JSON), because that pushes resolution timing from compile-time to action-execution-time."*

### "Why no Playwright engine yet?"

(After softening the badge.)
- *"The abstraction is engine-pluggable, but I haven't shipped the Playwright implementation because the priority sequence was: first, prove the architecture by completing Selenium; second, write the audit; third, ship CI; fourth, ship Playwright. Playwright is the next planned engine, with the test suite running against both via a config flag."*

### "What would you change if starting over?"

- *"Three things. One — I'd start with CI/CD on day one instead of as a packaging step. Two — I'd keep documentation private until a doc was finished, instead of mixing drafts and finished work in the same tree. Three — I'd resist the temptation to keep the legacy `Interactions` class compatible; it became a 833-line liability that pulls Selenium imports into places that should be engine-agnostic."*

---

## 10. Closing Assessment

The substance is real. Treat the next two weekends as **release engineering**, not feature engineering.

The exact gap between this repo today and a portfolio piece that opens interview doors is:

- 1 file deleted (`cp.txt`)
- 1 file added (`.github/workflows/ci.yml`)
- 1 file added (`LICENSE`)
- ~30 lines edited (`pom.xml` identity + README badges)
- 1 directory restructured (`docs/`)
- 1 git tag (`v0.1.0`)
- 1 image committed (architecture diagram)

That's it. Total scope ≤ 200 lines of changes. The resulting perception shift is disproportionate.

After this audit's items are closed, the framework articulation script in your acceleration plan (`Section 8.6 of SDET_ACCELERATION_3_TO_6_MONTHS.md`) will land convincingly — because the repository will back up every claim it makes.

Stop building. Start shipping.

---

*Audit version 1.0 — May 2026. Re-audit recommended at v0.2.0 / Playwright engine shipped.*
