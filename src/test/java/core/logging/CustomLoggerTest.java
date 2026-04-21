package core.logging;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.*;

import static core.logging.CustomLogger.*;

/**
 * Visual / smoke test for CustomLogger.
 *
 * A @Factory creates TWO instances of this class:
 *   • Instance 1 — ANSI enabled  → colored live console, still single-line in Test History
 *   • Instance 2 — ANSI disabled → plain text everywhere
 *
 * All 51 tests run twice (102 total). The test-tree in IntelliJ shows them under
 * "CustomLoggerTest [ANSI=ON]" and "CustomLoggerTest [ANSI=OFF]".
 */
public class CustomLoggerTest {

    // ── Factory wiring ────────────────────────────────────────────────────────

    private final boolean ansiEnabled;

    /** Used by @Factory. */
    public CustomLoggerTest(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
    }

    /** No-arg constructor required when TestNG runs the class directly (not via factory). */
    public CustomLoggerTest() {
        // Fall back to auto-detection when no factory is involved.
        this(false);
    }

    @Factory
    public static Object[] factory() {
        return new Object[]{
                new CustomLoggerTest(true),   // pass 1 — ANSI ON
                new CustomLoggerTest(false)  // pass 2 — ANSI OFF
        };
    }

    /** Shows "[ANSI=ON]" / "[ANSI=OFF]" in IntelliJ's test tree and report. */
    @Override
    public String toString() {
        return "[ANSI=" + (ansiEnabled ? "ON" : "OFF") + "]";
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeClass
    public void setup() {
        initialize(CustomLoggerTest.class);
        if (ansiEnabled) enableAnsi(); else disableAnsi();
        setTheme(LogTheme.MODERN_CLEAN);
    }

    @AfterMethod
    public void resetState() {
        // Restore this instance's ANSI state and theme after every test so that
        // any test that toggles ANSI (e.g. testAnsiToggle) doesn't bleed into the next.
        setTheme(LogTheme.MODERN_CLEAN);
        if (ansiEnabled) enableAnsi(); else disableAnsi();
    }

    @BeforeMethod
    public void printTestHeader(Method method) {
        // Re-apply this instance's ANSI state before every test.
        if (ansiEnabled) enableAnsi(); else disableAnsi();
        setTheme(LogTheme.MODERN_CLEAN);
        Test annotation = method.getAnnotation(Test.class);
        String desc = (annotation != null && !annotation.description().isEmpty())
                ? annotation.description() : method.getName();
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("[TEST] " + desc + "  (ANSI " + (ansiEnabled ? "ON" : "OFF") + ")");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── Info ──────────────────────────────────────────────────────────────────

    @Test(priority = 1, description = "info.log — plain message")
    public void testInfoLog() {
        info.log(">>> Section: INFO level logs");
        info.log("This is an INFO message");
    }

    @Test(priority = 2, description = "info.log — key/value pairs")
    public void testInfoLogFields() {
        info.log("User details", "name", "Alice", "role", "Admin", "active", true);
    }

    @Test(priority = 3, description = "info.log — map")
    public void testInfoLogMap() {
        info.log("Config map", fields("host", "localhost", "port", 8080, "secure", false));
    }

    // ── Warn ──────────────────────────────────────────────────────────────────

    @Test(priority = 4, description = "warn.log — plain message")
    public void testWarnLog() {
        info.log(">>> Section: WARN level logs");
        warn.log("This is a WARN message");
    }

    @Test(priority = 5, description = "warn.log — key/value pairs")
    public void testWarnLogFields() {
        warn.log("Deprecation warning", "method", "oldMethod()", "since", "v2.0", "replacement", "newMethod()");
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    @Test(priority = 6, description = "error.log — plain message")
    public void testErrorLog() {
        info.log(">>> Section: ERROR level logs");
        error.log("This is an ERROR message");
    }

    @Test(priority = 7, description = "error.log — with fields")
    public void testErrorLogFields() {
        error.log("Exception detail", "class", "NullPointerException", "line", 42, "file", "Foo.java");
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    @Test(priority = 8, description = "debug.log — plain message")
    public void testDebugLog() {
        info.log(">>> Section: DEBUG level logs");
        debug.log("This is a DEBUG message");
    }

    @Test(priority = 9, description = "debug.log — with fields")
    public void testDebugLogFields() {
        debug.log("Debug context", "thread", Thread.currentThread().getName(), "ts", System.currentTimeMillis());
    }

    // ── Action Labels ─────────────────────────────────────────────────────────

    @Test(priority = 10, description = "CLICK [>]")
    public void testClick() {
        info.log(">>> Section: LogActions symbols");
        info.click("Clicked the Submit button");
    }

    @Test(priority = 11, description = "CHECKBOX [x]")
    public void testCheckbox() { info.checkbox("Checked 'Accept Terms' checkbox"); }

    @Test(priority = 12, description = "TEXT [T]")
    public void testText() { info.text("Typed 'hello@example.com' into email field"); }

    @Test(priority = 13, description = "VALIDATION [?!]")
    public void testValidation() { info.validation("Email format is invalid"); }

    @Test(priority = 14, description = "FALLBACK [<-]")
    public void testFallback() { info.fallback("Primary locator failed, falling back to XPath"); }

    @Test(priority = 15, description = "WAIT [~]")
    public void testWait() { info.wait("Waiting for spinner to disappear"); }

    @Test(priority = 16, description = "INPUT [>>]")
    public void testInput() { info.input("Entered 'John Doe' in Name field"); }

    @Test(priority = 17, description = "TABLE [=] label")
    public void testTableLabel() { info.table("Rendering users table"); }

    @Test(priority = 18, description = "GRID [#]")
    public void testGrid() { info.grid("Rendering data grid"); }

    @Test(priority = 19, description = "COMPLETE [+]")
    public void testComplete() { info.complete("Form submission completed"); }

    @Test(priority = 20, description = "SUCCESS [+]")
    public void testSuccess() { info.success("Login successful"); }

    @Test(priority = 21, description = "UPLOAD [^]")
    public void testUpload() { info.upload("Uploaded file: report.pdf"); }

    @Test(priority = 22, description = "DROPDOWN [v]")
    public void testDropdown() { info.dropdown("Selected 'Australia' from Country dropdown"); }

    @Test(priority = 23, description = "FRAME [{}]")
    public void testFrame() { info.frame("Switched to iFrame: payment-frame"); }

    @Test(priority = 24, description = "TAB [->]")
    public void testTab() { info.tab("Switched to tab: Settings"); }

    @Test(priority = 25, description = "BREADCRUMB [/]")
    public void testBreadcrumb() { info.breadcrumb("Home / Users / Edit"); }

    @Test(priority = 26, description = "SEARCHED [*]")
    public void testSearch() { info.search("Searched for 'John'"); }

    @Test(priority = 27, description = "ERROR [x]")
    public void testErrorAction() { info.error("Element not found on page"); }

    @Test(priority = 28, description = "RESULT [:]")
    public void testResult() { info.result("Query returned 42 records"); }

    @Test(priority = 29, description = "TIMEOUT [!!]")
    public void testTimeout() { info.timeout("Element did not appear within 10s"); }

    @Test(priority = 30, description = "FAILED [x]")
    public void testFailed() { info.failed("Test step: clickSubmit — FAILED"); }

    @Test(priority = 31, description = "TOGGLE [o]")
    public void testToggle() { info.toggle("Toggled dark mode ON"); }

    @Test(priority = 32, description = "SKIP [>>]")
    public void testSkip() { info.skip("Skipping optional step: cookie banner"); }

    @Test(priority = 33, description = "RESOLVED [ok]")
    public void testResolved() { info.resolved("Locator resolved via fallback strategy"); }

    // ── Tables ────────────────────────────────────────────────────────────────

    @Test(priority = 34, description = "table — list of maps with title")
    public void testTableList() {
        info.log(">>> Section: Table rendering");
        List<Map<String, Object>> rows = List.of(
                fields("ID", 1, "Name", "Alice",   "Role", "Admin"),
                fields("ID", 2, "Name", "Bob",     "Role", "User"),
                fields("ID", 3, "Name", "Charlie", "Role", "Viewer")
        );
        info.table(rows, "Users Table");
    }

    @Test(priority = 35, description = "table — single row map with title")
    public void testTableSingleRow() {
        info.table(fields("Host", "localhost", "Port", 8080, "Secure", true), "Server Config");
    }

    @Test(priority = 36, description = "table — list of maps, no title")
    public void testTableNoTitle() {
        List<Map<String, Object>> rows = List.of(
                fields("Step", "Login",       "Status", "PASS"),
                fields("Step", "Navigate",    "Status", "PASS"),
                fields("Step", "Submit Form", "Status", "FAIL")
        );
        info.table(rows);
    }

    // ── Tree / Row ────────────────────────────────────────────────────────────

    @Test(priority = 37, description = "tree — structured key/value")
    public void testTree() {
        info.log(">>> Section: Tree / Row structured output");
        info.tree("Request payload",
                fields("method", "POST", "endpoint", "/api/login",
                       "body", "{\"user\":\"alice\"}", "timeout", 5000));
    }

    @Test(priority = 38, description = "resolved — tree structured")
    public void testResolvedTree() {
        info.resolved("Locator resolved",
                fields("strategy", "CSS", "value", ".submit-btn", "fallback", false));
    }

    @Test(priority = 39, description = "row — vertical key/value display")
    public void testRow() {
        info.row(fields("Username", "alice", "Password", "***", "Remember", true));
    }

    // ── Themes ────────────────────────────────────────────────────────────────

    @Test(priority = 40, description = "theme — INDUSTRIAL_STEEL")
    public void testThemeIndustrialSteel() {
        info.log(">>> Section: Themes");
        setTheme(LogTheme.INDUSTRIAL_STEEL);
        info.log("INDUSTRIAL_STEEL theme active");
        info.click("Clicked a button");
        info.success("All good");
        setTheme(LogTheme.MODERN_CLEAN);
    }

    @Test(priority = 41, description = "theme — NIGHT_CLUB")
    public void testThemeNightClub() {
        setTheme(LogTheme.NIGHT_CLUB);
        info.log("NIGHT_CLUB theme active");
        info.error("Something went wrong");
        info.wait("Waiting...");
        setTheme(LogTheme.MODERN_CLEAN);
    }

    @Test(priority = 42, description = "theme — CARBON_ORANGE")
    public void testThemeCarbonOrange() {
        setTheme(LogTheme.CARBON_ORANGE);
        info.log("CARBON_ORANGE theme active");
        info.dropdown("Selected an option");
        info.fallback("Falling back to secondary locator");
        setTheme(LogTheme.MODERN_CLEAN);
    }

    // ── ANSI toggle ───────────────────────────────────────────────────────────

    /**
     * Demonstrates ANSI toggle within a single test for reference.
     * With the factory, you can also see the full suite in both modes above.
     */
    @Test(priority = 43, description = "ANSI on/off — single-line output in all viewers")
    public void testAnsiToggle() {
        info.log(">>> Section: ANSI toggle (instance ANSI=" + (ansiEnabled ? "ON" : "OFF") + ")");
        info.log("This line uses the instance's ANSI setting");
        info.success("Success with instance ANSI setting");
        warn.log("Warn with instance ANSI setting");
        error.log("Error with instance ANSI setting");
        info.click("Click with instance ANSI setting");

        // Show the contrast: temporarily flip and flip back
        if (ansiEnabled) {
            disableAnsi();
            info.log("  ↳ ANSI temporarily OFF inside ANSI=ON instance");
            enableAnsi();
        } else {
            enableAnsi();
            info.log("  ↳ ANSI temporarily ON inside ANSI=OFF instance");
            disableAnsi();
        }
        info.log("Back to instance default ANSI=" + (ansiEnabled ? "ON" : "OFF"));
    }

    // ── Debug-specific overloads ───────────────────────────────────────────────

    @Test(priority = 44, description = "debug — all action labels")
    public void testDebugActions() {
        info.log(">>> Section: Debug-level action labels");
        debug.click("Debug click [>]");
        debug.wait("Debug wait [~]");
        debug.text("Debug text [T]");
        debug.success("Debug success [+]");
        debug.complete("Debug complete [+]");
        debug.error("Debug error [x]");
        debug.table("Debug table label [=]");
        debug.grid("Debug grid label [#]");
    }

    @Test(priority = 45, description = "debug — table with single row map")
    public void testDebugTable() {
        debug.table(fields("Key", "debug-value", "Status", "Active"));
    }

    @Test(priority = 46, description = "debug — table list with title")
    public void testDebugTableList() {
        debug.table(List.of(
                fields("Test", "TC-001", "Result", "PASS"),
                fields("Test", "TC-002", "Result", "FAIL")
        ), "Test Results");
    }

    // ── Object overloads ──────────────────────────────────────────────────────

    @Test(priority = 47, description = "log(Object) — string, list, map, null")
    public void testLogObject() {
        info.log("Just a plain string object");
        info.log((Object) null);
        info.log(List.of("alpha", "beta", "gamma"));
        info.log(fields("k1", "v1", "k2", "v2"));
    }

    @Test(priority = 48, description = "log(heading, Object)")
    public void testLogHeadingObject() {
        info.log("Env vars", fields("ENV", "staging", "BUILD", "42"));
        info.log("Empty list", List.of());
    }

    // ── Multiline ─────────────────────────────────────────────────────────────

    @Test(priority = 49, description = "multiline message")
    public void testMultiline() {
        info.log("Line 1\nLine 2\nLine 3");
        error.log("Error line A\nError line B");
    }

    // ── fields() validation ───────────────────────────────────────────────────

    @Test(priority = 50,
          description = "fields() — odd args must throw IllegalArgumentException",
          expectedExceptions = IllegalArgumentException.class)
    public void testFieldsOddArgsThrows() {
        fields("key1", "value1", "orphanKey");
    }

    // ── Summary banner ────────────────────────────────────────────────────────

    @Test(priority = 51, description = "full summary — every label in one shot")
    public void testFullSummaryBanner() {
        info.log("=== CustomLogger Label Summary ===");
        info.click("CLICK [>]");
        info.checkbox("CHECKBOX [x]");
        info.text("TEXT [T]");
        info.input("INPUT [>>]");
        info.dropdown("DROPDOWN [v]");
        info.upload("UPLOAD [^]");
        info.tab("TAB [->]");
        info.frame("FRAME [{}]");
        info.breadcrumb("BREADCRUMB [/]");
        info.search("SEARCHED [*]");
        info.toggle("TOGGLE [o]");
        info.wait("WAIT [~]");
        info.table("TABLE [=]");
        info.grid("GRID [#]");
        info.result("RESULT [:]");
        info.resolved("RESOLVED [ok]");
        info.validation("VALIDATION [?!]");
        info.fallback("FALLBACK [<-]");
        info.skip("SKIP [>>]");
        info.complete("COMPLETE [+]");
        info.success("SUCCESS [+]");
        warn.log("WARN");
        info.error("ERROR [x]");
        info.timeout("TIMEOUT [!!]");
        info.failed("FAILED [x]");
        Assert.assertTrue(true, "All labels rendered — inspect console.");
    }
}
