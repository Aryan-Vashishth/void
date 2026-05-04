# VOID — Dependency Audit


> Generated: 2026-05-01 (updated June 2026 — reflects UIEngine/Action/Flow/Runner architecture)
> Scope: All runtime and provided-scope dependencies declared in `pom.xml`
> Philosophy lens: transparency, traceability, debuggability, self-containment, engine portability

---

## Summary Matrix

| # | Dependency | Status | Hidden Behavior Risk | Action Taken |
|---|---|---|---|---|
| 1 | `selenium-java` | **KEPT** | Low | — |
| 2 | `webdrivermanager` | **REMOVED** | Medium | Unused; Selenium Manager handles driver binaries natively |
| 3 | `cucumber-java` / `cucumber-testng` | **KEPT (optional)** | Medium | Marked `<optional>true</optional>`; dead step-def files deleted |
| 4 | `extentreports-cucumber7-adapter` | **REMOVED** | Medium | Unused; transitive CVEs eliminated |
| 5 | `testng` | **KEPT** | Low | — |
| 6 | `jackson-databind` (+ BOM) | **KEPT** | Low | — |
| 7 | `log4j-api` / `log4j-core` | **KEPT** | Low | — |
| 8 | `log4j-1.2-api` (bridge) | **REMOVED** | Medium | Migrated 2 files to native Log4j 2 API |
| 9 | `jsr305` | **KEPT** (sole annotation library) | Low | Consolidated — all annotations now JSR-305 |
| 10 | `jetbrains-annotations` | **REMOVED** | Low | Replaced with JSR-305 `@Nonnull` in WaitUtils |
| 11 | `javafaker` | **REPLACED** → `datafaker` 2.4.2 | Low | Same API, no transitive CVEs |

## Final Dependency Set

| Dependency | Version | Purpose |
|---|---|---|
| `selenium-java` | 4.38.0 | Browser automation |
| `cucumber-java` / `cucumber-testng` | 7.31.0 | BDD adapter (optional) |
| `testng` | 7.11.0 | Test runner |
| `jackson-databind` | 2.19.0 (BOM) | JSON parsing |
| `log4j-api` / `log4j-core` | 2.25.4 | Logging |
| `jsr305` | 3.0.2 | Nullability annotations |
| `datafaker` | 2.4.2 | Test data generation |

**Total: 7 dependencies (down from 11).** Zero hidden behavior. Zero unused dependencies.

---

## Detailed Analysis

---

### 1. `selenium-java` (4.38.0)

- **Purpose:** WebDriver API — powers the `SeleniumEngine` implementation of `UIEngine`.
- **Usage locations:** `SeleniumEngine`, `DriverFactory`, `DriverContext`, `Interactions` (legacy), `Via`, `WaitUtils`, `DOMUtils`, `TableHandler`, `After` hooks, and all test classes that drive browsers. ~20+ files with direct imports.
- **Hidden behavior risk:** **Low.** Selenium is explicit — locators, waits, and actions are all visible. The `RemoteWebDriver` protocol is well-documented.
- **Internal replacement feasibility:** The `UIEngine` interface (`core.engine.UIEngine`) abstracts over Selenium. A `PlaywrightEngine` implementation could replace `SeleniumEngine` without changing any test code — this is the multi-engine execution design (see `docs/experiments/active/2026-05-01-multi-engine-execution.md`).
- **Migration effort:** —
- **Recommendation:** **KEEP**
- **Reasoning:** Powers the default `SeleniumEngine`. All Selenium coupling is contained within the engine layer — `Action`, `Flow`, `Runner`, capability interfaces, and DSL are fully engine-agnostic.

---

### 2. `webdrivermanager` (6.3.3)

- **Purpose:** Automatic download and configuration of browser driver binaries (chromedriver, geckodriver, msedgedriver).
- **Usage locations:** **Zero active imports.** Not imported in any `.java` file. The only reference is a Javadoc comment in `DriverFactory.java` line 41: *"If using WebDriverManager, call its setup before building drivers."* — a suggestion, not a call.
- **Hidden behavior risk:** **Medium.** WebDriverManager uses reflection, HTTP calls to GitHub/Google APIs, and caches binaries in `~/.cache/selenium`. When it silently fails or picks the wrong version, debugging is non-trivial.
- **Internal replacement feasibility:** Not needed — **Selenium 4.6+** includes built-in [Selenium Manager](https://www.selenium.dev/documentation/selenium_manager/), which handles driver binary resolution natively. VOID already uses Selenium 4.38.0, so this is already handled without WebDriverManager.
- **Migration effort:** **Low.** Remove the dependency. No code changes needed since it's not imported anywhere. Update the Javadoc comment in `DriverFactory` and any docs that reference it.
- **Recommendation:** **REMOVE**
- **Reasoning:** Unused in code. Selenium 4's built-in Selenium Manager makes it redundant. It adds a transitive dependency tree with its own HTTP client and caching layer — unnecessary complexity and opacity for zero value.

---

### 3. `cucumber-java` (7.31.0) / `cucumber-testng` (7.31.0)

- **Purpose:** BDD step-definition annotations (`@Given`, `@When`, `@Then`) and Cucumber-TestNG runner integration.
- **Usage locations:** **Zero active imports.** The only file referencing Cucumber is `CommonStepDef.java` — and the **entire file is commented out** (all 546 lines). No `.feature` files exist in the project. The `StepDefInteractions` class and `AutomationVOID` do not import Cucumber — they provide a BDD-compatible interaction API but are framework-internal.
- **Hidden behavior risk:** **Medium.** Cucumber uses classpath scanning, reflection-based step binding, and its own lifecycle management that bypasses TestNG's native flow.
- **Internal replacement feasibility:** Already done — `StepDefInteractions` and `EnumResolver` provide string-to-enum resolution internally. When/if Cucumber is needed, users of VOID-as-a-dependency would declare it themselves.
- **Migration effort:** **Low.** Remove dependencies. Delete or archive the commented-out `CommonStepDef.java`. No functional code references Cucumber.
- **Recommendation:** **REMOVE**
- **Reasoning:** Dead dependency — no active imports, no feature files, no runner. VOID's internal step-definition support (`StepDefInteractions`, `ResolvableEnum`, `EnumResolver`) works independently of Cucumber. If a consumer project needs Cucumber, they should declare it in *their* POM, not inherit it from VOID.

---

### 4. `extentreports-cucumber7-adapter` (1.14.0)

- **Purpose:** Generates HTML Spark reports from Cucumber test runs.
- **Usage locations:** **Zero active imports.** No Java file imports `tech.grasshopper` or `com.aventstack`. The `extent.properties` config file exists but is inert without an active Cucumber runner. The HTML report in `target/ExtentReports/` appears to be a leftover artifact.
- **Hidden behavior risk:** **Medium.** The adapter hooks into Cucumber's plugin system via SPI/classpath discovery. It pulls in transitive dependencies including `poi-ooxml` (with known CVE-2025-31672) and `extentreports` core.
- **Internal replacement feasibility:** VOID already has comprehensive logging via `CustomLogger`. For HTML reporting, TestNG's built-in reports (`target/surefire-reports/`) are already generated. If richer reporting is needed, it should be a consumer-side concern.
- **Migration effort:** **Low.** Remove dependency and `extent.properties`. Clean up `src/main/resources/ExtentReports/` directory.
- **Recommendation:** **REMOVE**
- **Reasoning:** Unused, adds transitive CVE exposure (poi-ooxml), and is tightly coupled to Cucumber (which is also unused). VOID's logging system and TestNG's native reports cover reporting needs.

---

### 5. `testng` (7.11.0)

- **Purpose:** Test runner — `@Test`, `@BeforeClass`, `@DataProvider`, `@Factory`, assertions, suite XML.
- **Usage locations:** All test classes in `src/test/java/` (~20+ files). Suite configuration in `src/testNgXml/testng.xml`. Referenced by `maven-surefire-plugin`.
- **Hidden behavior risk:** **Low.** TestNG is explicit — annotations are declarative, the lifecycle is well-defined, and suite XML gives full control. No bytecode manipulation.
- **Internal replacement feasibility:** Not feasible or desirable. Writing a test runner is out-of-scope.
- **Migration effort:** —
- **Recommendation:** **KEEP**
- **Reasoning:** Essential test infrastructure. Fully aligned with VOID's philosophy — explicit annotations, predictable lifecycle, no magic.

---

### 6. `jackson-databind` (2.19.0, managed via BOM)

- **Purpose:** JSON parsing and serialization — `ObjectMapper`, `JsonNode`, `ObjectNode`, `TypeReference`.
- **Usage locations:** **Heavily used across the codebase:**
  - `JsonLocatorReader` — reads `.json` locator files
  - `JsonTreeBuilder` — builds JSON ASTs for migration
  - `JsonLocatorMigrator` — serializes enum→JSON
  - `JsonNodeLookup` — traverses JSON trees
  - `EnumLocatorScanner` — scans enums into `ObjectNode`
  - `JsonReader` — generic JSON reading utility
  - `JsonLogger` — JSON writing/serialization utility
  - `FileUtils` — fallback JSON map loading
  - 7+ test classes
- **Hidden behavior risk:** **Low.** Jackson is explicit — you call `mapper.readTree()`, `mapper.writeValueAsString()`, etc. No annotation magic in VOID's usage (no `@JsonProperty`, no auto-binding to POJOs).
- **Internal replacement feasibility:** Technically possible with `javax.json` or manual parsing, but would require rewriting 10+ classes and lose streaming performance, tree model, and pretty-printing. The effort would be enormous for no real gain.
- **Migration effort:** —
- **Recommendation:** **KEEP**
- **Reasoning:** Jackson is the industry-standard JSON library for Java. VOID uses it explicitly (tree model, not annotation-driven binding), so there's no hidden behavior. The locator resolution pipeline depends on it fundamentally. Note: the BOM-managed version has a transitive CVE on `jackson-core` (GHSA-72hv-8253-57qq) — monitor for a patch release.

---

### 7. `log4j-api` / `log4j-core` (2.25.4)

- **Purpose:** Logging backend — provides the actual log appenders, layouts, and configuration (`log4j2.xml`).
- **Usage locations:** Indirectly used by every class that calls `CustomLogger`. The `log4j2.xml` config files in `src/main/resources/` and `src/test/resources/` configure appenders and levels. Log4j 2 Core processes all log events.
- **Hidden behavior risk:** **Low.** Configuration is explicit via `log4j2.xml`. Appender behavior is well-documented. No bytecode manipulation.
- **Internal replacement feasibility:** Writing a full logging backend (file rotation, async appenders, pattern layouts) would be substantial effort for no benefit.
- **Migration effort:** —
- **Recommendation:** **KEEP**
- **Reasoning:** Logging infrastructure is legitimately complex. Log4j 2 is explicit, configurable, and VOID wraps it behind `CustomLogger` for full control of output format and behavior.

---

### 8. `log4j-1.2-api` (2.25.4) — Log4j 1.x Bridge

- **Purpose:** Provides the `org.apache.log4j.Logger` class from Log4j 1.x, routing calls to the Log4j 2 backend. This lets code use the legacy `import org.apache.log4j.Logger` API.
- **Usage locations:** **2 files:**
  - `CustomLogger.java` — `import org.apache.log4j.Logger`
  - `LoggerContext.java` — `import org.apache.log4j.Logger`
- **Hidden behavior risk:** **Medium.** The bridge silently redirects Log4j 1.x API calls to Log4j 2 internals. This is an invisible transformation — the code says "Log4j 1.x" but runs "Log4j 2." This is exactly the kind of hidden behavior VOID's philosophy opposes.
- **Internal replacement feasibility:** **Trivial.** Change 2 import statements from `org.apache.log4j.Logger` to `org.apache.logging.log4j.Logger` (the native Log4j 2 API). Update `Logger.getLogger(...)` calls to `LogManager.getLogger(...)`. ~10 minutes of work.
- **Migration effort:** **Low.** 2 files, ~5 lines total.
- **Recommendation:** **REPLACE** — migrate to native Log4j 2 API, then remove the bridge dependency.
- **Reasoning:** The bridge exists solely to avoid updating 2 import statements. It adds an invisible redirection layer that contradicts VOID's transparency principle. The fix is trivial.

---

### 9. `jsr305` (3.0.2) — `javax.annotation.Nullable` / `@Nonnull`

- **Purpose:** Nullability annotations for public API parameters and return types.
- **Usage locations:** **4 files:**
  - `DataGenerator.java` — `@Nullable`
  - `JsonLogger.java` — `@Nullable`
  - `JsonReader.java` — `@Nullable`
  - `WaitUtils.java` — `@Nullable`
- **Hidden behavior risk:** **Low.** Pure compile-time/IDE annotations. No runtime behavior, no bytecode changes, no reflection. They are documentation markers.
- **Internal replacement feasibility:** Could define a custom `@Nullable` annotation internally, but that would reduce IDE/tool integration (IntelliJ, SpotBugs, Error Prone all recognize `javax.annotation.Nullable`).
- **Migration effort:** —
- **Recommendation:** **KEEP** — but consider consolidating with JetBrains annotations (see #10).
- **Reasoning:** Zero-cost documentation annotations. Fully aligned with VOID's principle of explicit contracts. Having both JSR-305 and JetBrains annotations is slightly redundant — see note below.

---

### 10. `jetbrains-annotations` (26.0.2) — `@NotNull` / `@Nullable`

- **Purpose:** Nullability annotations — same role as JSR-305 but from JetBrains.
- **Usage locations:** **1 file:**
  - `WaitUtils.java` — `@NotNull` (5 usages)
- **Hidden behavior risk:** **Low.** Same as JSR-305 — pure annotations, no runtime behavior.
- **Internal replacement feasibility:** Same as JSR-305.
- **Migration effort:** —
- **Recommendation:** **KEEP** — but consider **consolidating** to one annotation library.
- **Reasoning:** Having both `javax.annotation.Nullable` (JSR-305) and `org.jetbrains.annotations.NotNull` (JetBrains) in the same codebase is a minor inconsistency. Both are zero-cost. The pragmatic choice is to pick one and standardize:
  - **Option A:** Standardize on JSR-305 (`javax.annotation.*`) — more portable, recognized by more tools.
  - **Option B:** Standardize on JetBrains — better IntelliJ integration, actively maintained.
  - Either way, the cost is changing 5 import lines in `WaitUtils.java`.

---

### 11. `javafaker` (1.0.2)

- **Purpose:** Test data generation — random names, emails, dates, company names, IDs, etc.
- **Usage locations:** **1 file:**
  - `DataGenerator.java` — `Faker` instance used to power all `FieldType` generators (names, emails, dates, amounts, IDs, etc.) across ~23 generator registrations.
- **Hidden behavior risk:** **Low.** Faker is explicit — you call `faker.name().fullName()` and get a string. No annotations, no reflection, no bytecode changes.
- **Internal replacement feasibility:** Possible but high-effort and low-value. You'd need to implement randomized locale-aware data pools for names, addresses, companies, phone numbers, emails, etc. This is hundreds of data points with regional variations.
- **Migration effort:** —
- **Recommendation:** **KEEP** — with one caveat.
- **Reasoning:** Faker provides genuine complexity reduction (locale-aware random data) without hiding any logic. It's used explicitly through a single internal wrapper (`DataGenerator`), so consumers never touch Faker directly. 
- **⚠️ Caveat:** JavaFaker 1.0.2 is **unmaintained** and pulls in `snakeyaml:1.23`, which has **8 known CVEs** (including CVE-2022-1471, severity 8.3). Consider migrating to [**Datafaker**](https://github.com/datafaker-net/datafaker) (`net.datafaker:datafaker`), which is the actively maintained fork with the same API and no vulnerable transitive dependencies. The migration is a near-drop-in replacement (change import from `com.github.javafaker.Faker` to `net.datafaker.Faker`).

---

## Cross-Cutting Observations

### Annotation Libraries (JSR-305 + JetBrains)

Both are used (`@Nullable` from JSR-305 in 4 files, `@NotNull` from JetBrains in 1 file). This is a minor inconsistency. **Recommendation:** Standardize on one. JSR-305 is the more portable choice. Migration: change 5 `@NotNull` usages in `WaitUtils.java` to `@javax.annotation.Nonnull`, then remove `jetbrains-annotations` dependency.

### Log4j 1.x Bridge

The `log4j-1.2-api` bridge is the most concrete violation of VOID's transparency philosophy currently in the codebase. It creates an invisible API translation layer for the sake of 2 import statements. **Recommendation:** Migrate to native Log4j 2 API (trivial change), then remove the bridge.

### Dead Dependencies (Cucumber + Extent + WebDriverManager)

Three dependencies have **zero active usage**: `cucumber-java`, `cucumber-testng`, `extentreports-cucumber7-adapter`, and `webdrivermanager`. Together they bring in a significant transitive dependency tree including packages with known CVEs. **Recommendation:** Remove all three immediately. If consumers need Cucumber or Extent, they declare them in their own projects.

---

## Recommended Action Plan (Priority Order)

| Priority | Action | Files Changed | Risk |
|----------|--------|---------------|------|
| 1 | **Remove `webdrivermanager`** — unused, redundant with Selenium 4 | `pom.xml`, `DriverFactory.java` (Javadoc), docs | None |
| 2 | **Remove `cucumber-java` + `cucumber-testng`** — unused, all code commented out | `pom.xml`, optionally delete `CommonStepDef.java` | None |
| 3 | **Remove `extentreports-cucumber7-adapter`** — unused, transitive CVEs | `pom.xml`, `extent.properties`, docs | None |
| 4 | **Replace `log4j-1.2-api`** — migrate 2 files to native Log4j 2 API | `CustomLogger.java`, `LoggerContext.java`, `pom.xml` | Very Low |
| 5 | **Consolidate annotations** — pick JSR-305 or JetBrains, not both | `WaitUtils.java`, `pom.xml` | Very Low |
| 6 | **Migrate `javafaker` → `datafaker`** — same API, no CVEs | `pom.xml`, `DataGenerator.java` (import change) | Low |

### After Cleanup: Final Dependency Set

| Dependency | Purpose | Status |
|---|---|---|
| `selenium-java` | Browser automation | Essential |
| `testng` | Test runner | Essential |
| `jackson-databind` | JSON parsing | Essential |
| `log4j-api` + `log4j-core` | Logging | Essential |
| `jsr305` (or `jetbrains-annotations`) | Nullability annotations | Aligned |
| `datafaker` (replaces `javafaker`) | Test data generation | Aligned |

**Total: 6 dependencies (down from 11).** Zero hidden behavior. Zero unused code. Zero known CVEs in direct dependencies.

---

*This audit reflects the codebase state as of 2026-05-01, updated June 2026 to reflect the UIEngine/Action/Flow/Runner architecture. Selenium is now isolated behind the `UIEngine` interface — making future engine additions (Playwright) possible without dependency changes to the core framework.*
