package core.logging;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.List;
import core.logging.intent.LogIntent;
import core.logging.theme.LogTheme;
import core.logging.theme.ThemeColors;

import static core.logging.ansi.AnsiColors.*;
import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;
import static core.logging.CustomLogger.fields;
import static core.logging.CustomLogger.initialize;
import static core.logging.CustomLogger.setTheme;
import static core.logging.CustomLogger.setCustomTheme;
import static core.logging.CustomLogger.enableAnsi;
import static core.logging.CustomLogger.disableAnsi;
import static core.logging.CustomLogger.enableCallerColor;
import static core.logging.CustomLogger.disableCallerColor;

/**
 * Visual theme verification for {@link CustomLogger}.
 *
 * <h2>Structure</h2>
 * <ul>
 *   <li>A {@code @Factory} creates one instance per ANSI state (ON / OFF).</li>
 *   <li>{@code @DataProvider} drives a parametric test over all {@link LogTheme}s.</li>
 *   <li>Each test group exercises one {@link LogIntent} category so the output is easy
 *       to scan in the console or IntelliJ Test History.</li>
 * </ul>
 *
 * <h2>How to read the output</h2>
 * <pre>
 *   ANSI=ON  → colored bands; inspect contrast visually in the live console.
 *   ANSI=OFF → plain text; verify the label / message content is correct.
 * </pre>
 *
 * <p>Tests are intentionally non-asserting on color — they exist so a developer
 * can see every theme/level/intent combination in one run and spot contrast issues.</p>
 */
public class CustomLoggerThemeTest {

    // ── Factory ───────────────────────────────────────────────────────────────

    private final boolean ansiEnabled;

    public CustomLoggerThemeTest(boolean ansiEnabled) { this.ansiEnabled = ansiEnabled; }
    public CustomLoggerThemeTest() { this(false); }

    @Factory
    public static Object[] factory() {
        return new Object[]{
                new CustomLoggerThemeTest(true),   // pass 1 — ANSI ON
                new CustomLoggerThemeTest(false)   // pass 2 — ANSI OFF
        };
    }

    @Override public String toString() {
        return "[ANSI=" + (ansiEnabled ? "ON" : "OFF") + "]";
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeClass
    public void globalSetup() {
        initialize(CustomLoggerThemeTest.class);
        applyAnsi();
    }

    @AfterMethod
    public void resetAfterTest() {
        setTheme(LogTheme.PLAIN);
        applyAnsi();
    }

    private void applyAnsi() {
        if (ansiEnabled) enableAnsi(); else disableAnsi();
    }

    // ── DataProvider — all themes ─────────────────────────────────────────────

    @DataProvider(name = "allThemes")
    public static Object[][] allThemes() {
        return new Object[][]{
                {LogTheme.PLAIN,            "PLAIN"},
                {LogTheme.SOLARIZED_DARK,   "SOLARIZED_DARK"},
                {LogTheme.HIGH_CONTRAST,    "HIGH_CONTRAST"},
                {LogTheme.MODERN_CLEAN,     "MODERN_CLEAN"},
                {LogTheme.INDUSTRIAL_STEEL, "INDUSTRIAL_STEEL"},
                {LogTheme.NIGHT_CLUB,       "NIGHT_CLUB"},
                {LogTheme.CARBON_ORANGE,    "CARBON_ORANGE"},
                {LogTheme.COCKPIT,          "COCKPIT"},
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void printThemeBanner(String themeName, String section) {
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  Theme: " + themeName + "  │  Section: " + section
                + "  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── BASE intent — log level labels ────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "BASE intent: info / warn / error / debug .log() — level BG + level FG",
          priority = 10)
    public void testBaseLevelLogs(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "BASE INTENT — Log Level Labels");

        info.log ("INFO  level message  — infoBg  + infoFg");
        warn.log ("WARN  level message  — warnBg  + warnFg");
        error.log("ERROR level message  — errorBg + errorFg");
        debug.log("DEBUG level message  — debugBg + debugFg");
    }

    // ── INTERACTION intent ────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "INTERACTION intent: click / checkbox / text / input / dropdown / toggle / upload",
          priority = 20)
    public void testInteractionIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "INTERACTION INTENT");

        info.log("── at INFO level (infoBg + interactionFg) ──");
        info.click   ("Clicked the Submit button");
        info.checkbox("Checked 'Accept Terms'");
        info.text    ("Typed 'hello@example.com' into email field");
        info.input   ("Entered 'John Doe' into Name field");
        info.dropdown("Selected 'Australia' from Country dropdown");
        info.toggle  ("Toggled dark mode ON");
        info.upload  ("Uploaded file: report.pdf");

        info.log("── at DEBUG level (debugBg + interactionFg) ──");
        debug.click   ("Debug click");
        debug.checkbox("Debug checkbox");
        debug.text    ("Debug text");
        debug.input   ("Debug input");
        debug.dropdown("Debug dropdown");
        debug.toggle  ("Debug toggle");
        debug.upload  ("Debug upload");
    }

    // ── NAVIGATION intent ─────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "NAVIGATION intent: tab / frame / breadcrumb",
          priority = 30)
    public void testNavigationIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "NAVIGATION INTENT");

        info.log("── at INFO level (infoBg + navigationFg) ──");
        info.tab       ("Switched to tab: Settings");
        info.frame     ("Switched to iFrame: payment-frame");
        info.breadcrumb("Home / Users / Edit");

        info.log("── at DEBUG level (debugBg + navigationFg) ──");
        debug.tab       ("Debug tab");
        debug.frame     ("Debug frame");
        debug.breadcrumb("Debug breadcrumb");
    }

    // ── OBSERVE intent ────────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "OBSERVE intent: wait / search / result",
          priority = 40)
    public void testObserveIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "OBSERVE INTENT");

        info.log("── at INFO level (infoBg + observeFg) ──");
        info.wait  ("Waiting for spinner to disappear");
        info.search("Searched for 'John'");
        info.result("Query returned 42 records");

        info.log("── at DEBUG level (debugBg + observeFg) ──");
        debug.wait  ("Debug wait");
        debug.search("Debug search");
        debug.result("Debug result");
    }

    // ── DATA intent ───────────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "DATA intent: table / grid / row — with real structured data",
          priority = 50)
    public void testDataIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "DATA INTENT");

        info.log("── table label at INFO level (infoBg + dataFg) ──");
        info.table("Rendering users table");
        info.grid ("Rendering data grid");

        info.log("── structured table at INFO level ──");
        info.table(List.of(
                fields("ID", 1, "Name", "Alice",   "Role", "Admin"),
                fields("ID", 2, "Name", "Bob",     "Role", "User"),
                fields("ID", 3, "Name", "Charlie", "Role", "Viewer")
        ), "Users — " + themeName);

        info.log("── single-row table ──");
        info.table(fields("Host", "localhost", "Port", 8080, "Secure", true), "Server Config");

        info.log("── row display ──");
        info.row(fields("Username", "alice", "Password", "***", "Remember", true));

        info.log("── table at DEBUG level (debugBg + dataFg) ──");
        debug.table(List.of(
                fields("Test", "TC-001", "Result", "PASS"),
                fields("Test", "TC-002", "Result", "FAIL")
        ), "Test Results");
    }

    // ── SUCCESS intent ────────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "SUCCESS intent: success / complete / resolved",
          priority = 60)
    public void testSuccessIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "SUCCESS INTENT");

        info.log("── at INFO level (infoBg + successFg) ──");
        info.success ("Login successful");
        info.complete("Form submission completed");
        info.resolved("Locator resolved via fallback strategy");

        info.log("── resolved tree at INFO level ──");
        info.resolved("Locator resolved", fields(
                "strategy", "CSS", "value", ".submit-btn", "fallback", false));

        info.log("── at DEBUG level (debugBg + successFg) ──");
        debug.success ("Debug success");
        debug.complete("Debug complete");
        debug.resolved("Debug resolved");
    }

    // ── ALERT intent ──────────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "ALERT intent: error[x] / failed / timeout / validation / fallback / skip",
          priority = 70)
    public void testAlertIntent(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "ALERT INTENT");

        info.log("── at INFO level (infoBg + alertFg) ──");
        info.error     ("Element not found on page");
        info.failed    ("Test step: clickSubmit — FAILED");
        info.timeout   ("Element did not appear within 10s");
        info.validation("Email format is invalid");
        info.fallback  ("Primary locator failed, falling back to XPath");
        info.skip      ("Skipping optional step: cookie banner");

        info.log("── at WARN level (warnBg + alertFg) ──");
        warn.error     ("Warn-level error signal");
        warn.validation("Warn-level validation failure");
        warn.fallback  ("Warn-level fallback");

        info.log("── at DEBUG level (debugBg + alertFg) ──");
        debug.error  ("Debug error[x]");
        debug.timeout("Debug timeout");
    }

    // ── Cross-level comparison ────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "Same action on all four log levels — shows how level BG changes while intent FG stays the same",
          priority = 80)
    public void testSameActionAcrossLevels(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "SAME ACTION — ALL LEVELS (click)");

        info.log ("INFO  click  — infoBg  + interactionFg");  info.click ("Clicked at INFO level");
        warn.log ("WARN  click  — warnBg  + interactionFg");  warn.click ("Clicked at WARN level");
        error.log("ERROR click  — errorBg + interactionFg");  error.click("Clicked at ERROR level");
        debug.log("DEBUG click  — debugBg + interactionFg");  debug.click("Clicked at DEBUG level");

        info.log ("INFO  wait   — infoBg  + observeFg");  info.wait ("Waited at INFO level");
        warn.log ("WARN  wait   — warnBg  + observeFg");  warn.wait ("Waited at WARN level");
        error.log("ERROR wait   — errorBg + observeFg");  error.wait("Waited at ERROR level");
        debug.log("DEBUG wait   — debugBg + observeFg");  debug.wait("Waited at DEBUG level");

        info.log ("INFO  success— infoBg  + successFg");  info.success ("Success at INFO level");
        warn.log ("WARN  success— warnBg  + successFg");  warn.success ("Success at WARN level");
        error.log("ERROR success— errorBg + successFg");  error.success("Success at ERROR level");
        debug.log("DEBUG success— debugBg + successFg");  debug.success("Success at DEBUG level");
    }

    // ── Tree structured logging ───────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "Tree and resolved structured key/value output per theme",
          priority = 90)
    public void testTreeOutput(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "TREE / RESOLVED OUTPUT");

        info.tree("Request payload", fields(
                "method",   "POST",
                "endpoint", "/api/login",
                "body",     "{\"user\":\"alice\"}",
                "timeout",  5000));

        info.resolved("Locator resolved", fields(
                "strategy", "CSS",
                "value",    ".submit-btn",
                "fallback", false,
                "retries",  3));

        warn.log("Request payload (at WARN level)", fields(
                "method",   "POST",
                "endpoint", "/api/login"));

        debug.log("Debug context", fields(
                "thread", Thread.currentThread().getName(),
                "ts",     System.currentTimeMillis()));
    }

    // ── Full label sweep ──────────────────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "All action labels in one pass — quick theme smoke test",
          priority = 100)
    public void testAllLabels(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "ALL LABELS — FULL SWEEP");

        // BASE
        info.log ("INFO");
        warn.log ("WARN");
        error.log("ERROR");
        debug.log("DEBUG");

        // INTERACTION
        info.click("CLICK");  info.checkbox("CHECKBOX");  info.text("TEXT");
        info.input("INPUT");  info.dropdown("DROPDOWN");  info.toggle("TOGGLE");
        info.upload("UPLOAD");

        // NAVIGATION
        info.tab("TAB");  info.frame("FRAME");  info.breadcrumb("BREADCRUMB");

        // OBSERVE
        info.wait("WAIT");  info.search("SEARCH");  info.result("RESULT");

        // DATA
        info.table("TABLE");  info.grid("GRID");

        // SUCCESS
        info.success("SUCCESS");  info.complete("COMPLETE");  info.resolved("RESOLVED");

        // ALERT
        info.error("ERROR[x]");  info.failed("FAILED");  info.timeout("TIMEOUT");
        info.validation("VALIDATION");  info.fallback("FALLBACK");  info.skip("SKIP");
    }

    // ── Caller color (@ConsoleOnly) ───────────────────────────────────────────

    @Test(dataProvider = "allThemes",
          description = "Caller color (@ConsoleOnly) — two ANSI segments, only verify in live console",
          priority = 110)
    public void testCallerColor(LogTheme theme, String themeName) {
        setTheme(theme);
        printThemeBanner(themeName, "CALLER COLOR — @ConsoleOnly");

        info.log("--- callerColor OFF (default, single-block, safe for Test History) ---");
        disableCallerColor();
        info.click("Click with callerColor OFF");
        info.success("Success with callerColor OFF");

        if (ansiEnabled) {
            info.log("--- callerColor ON (@ConsoleOnly — inspect live console only) ---");
            enableCallerColor();
            info.click("Click with callerColor ON — caller suffix should be dim");
            info.success("Success with callerColor ON — caller suffix should be dim");
            disableCallerColor();   // always restore
            info.log("--- callerColor restored OFF ---");
        } else {
            info.log("--- callerColor ON skipped (ANSI=OFF — no visual difference) ---");
        }
    }

    // ── PLAIN theme — dedicated smoke test ───────────────────────────────────

    /**
     * Dedicated PLAIN smoke test (★ default theme).
     *
     * <p>Uses only the classic 16-color ANSI palette so output renders correctly
     * in every terminal emulator, CI runner, Windows CMD, and log-file viewer.</p>
     */
    @Test(description = "PLAIN theme — full intent sweep + level BG verification  ★ default theme",
          priority = 108)
    public void testPlainTheme() {
        setTheme(LogTheme.PLAIN);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  PLAIN ★ DEFAULT — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("  Same hue family as 16-color ANSI, darker RGB shades — bright FGs pop more");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — deep steel grey BG  rgb(42,46,58)   + bright white FG");
        warn.log ("WARN  — deep amber BG       rgb(148,108,0)  + black FG");
        error.log("ERROR — deep crimson BG     rgb(155,16,16)  + bright white FG");
        debug.log("DEBUG — near-black BG       rgb(10,10,14)   + white FG");

        info.log("── INTERACTION — bright white bold FG ──");
        info.click("Clicked the Submit button");  info.checkbox("Checked 'Accept Terms'");
        info.text("Typed 'alice@example.com'");   info.input("Entered display name 'Alice'");
        info.dropdown("Selected country: Australia");
        info.toggle("Toggled dark mode ON");       info.upload("Uploaded file: report.pdf");
        debug.click("Debug — click event dispatched");

        info.log("── NAVIGATION — bright cyan bold FG ──");
        info.tab("Switched to tab: Settings");
        info.frame("Entered iFrame: payment-frame");
        info.breadcrumb("Home / Users / Edit");
        debug.frame("Debug — frame context entered");

        info.log("── OBSERVE — bright yellow bold FG ──");
        info.wait("Waiting for spinner to disappear");
        info.search("Searched for 'Alice'");  info.result("Query returned 42 records");
        debug.wait("Debug — polling every 500 ms");

        info.log("── DATA — bright magenta bold FG ──");
        info.table("Rendering users table");  info.grid("Rendering data grid");
        info.table(List.of(
                fields("ID", 1, "Name", "Alice",   "Role", "Admin"),
                fields("ID", 2, "Name", "Bob",     "Role", "User"),
                fields("ID", 3, "Name", "Charlie", "Role", "Viewer")
        ), "Users — PLAIN");
        info.row(fields("Host", "localhost", "Port", 8080, "Secure", true));
        debug.table(List.of(
                fields("Check", "DB connection", "Result", "PASS"),
                fields("Check", "Cache warmup",  "Result", "PASS")
        ), "Health Checks");

        info.log("── SUCCESS — bright green bold FG ──");
        info.success("Login successful");  info.complete("Form submission completed");
        info.resolved("Locator resolved via CSS selector",
                fields("strategy", "CSS", "value", "#save-btn", "retries", 1));
        debug.success("Debug — assertion passed");

        info.log("── ALERT — bright red bold FG ──");
        info.error("Element not found");   info.failed("Step 'clickSubmit' — FAILED");
        info.timeout("Element did not appear within 10 s");
        info.validation("Email format is invalid");
        info.fallback("Primary locator failed, falling back to XPath");
        info.skip("Skipping optional step: cookie banner");
        warn.error("Warn-level error (yellow BG + red FG)");
        error.failed("Error-level failure (red BG + red FG)");

        info.log("── click across all levels ──");
        info.click("INFO  click — dark-grey BG");  warn.click("WARN  click — yellow BG");
        error.click("ERROR click — red BG");        debug.click("DEBUG click — black BG");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("PLAIN smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── SOLARIZED_DARK theme — dedicated smoke test ───────────────────────────

    /**
     * Dedicated SOLARIZED_DARK smoke test.
     * Exact Solarized palette RGB values — reduced eye-strain during long sessions.
     */
    @Test(description = "SOLARIZED_DARK theme — full intent sweep + level BG verification",
          priority = 109)
    public void testSolarizedDarkTheme() {
        setTheme(LogTheme.SOLARIZED_DARK);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  SOLARIZED_DARK — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("  Exact Solarized palette RGB values — reduced eye-strain");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — base02 BG  rgb(7,54,66)   — blue-green teal");
        warn.log ("WARN  — yellow BG  darkened        — muted amber");
        error.log("ERROR — red BG     darkened        — muted crimson");
        debug.log("DEBUG — base03 BG  rgb(0,43,54)   — deepest slate");

        info.log("── INTERACTION — Solarized base0 #839496 FG ──");
        info.click("Clicked the Submit button");  info.checkbox("Checked 'Remember me'");
        info.text("Typed 'alice@company.com'");   info.input("Entered display name 'Alice Nguyen'");
        info.dropdown("Selected plan tier: Professional");
        info.toggle("Toggled two-factor authentication ON");
        info.upload("Uploaded avatar: profile-photo.png");
        debug.click("Debug — click event dispatched");

        info.log("── NAVIGATION — Solarized blue #268bd2 FG ──");
        info.tab("Switched to tab: Account Settings");
        info.frame("Entered iFrame: billing-widget");
        info.breadcrumb("Dashboard / Settings / Profile");
        debug.frame("Debug — frame context entered");

        info.log("── OBSERVE — Solarized yellow #b58900 FG ──");
        info.wait("Waiting for dashboard tiles to load");
        info.search("Searched for user 'alice'");  info.result("3 matching accounts returned");
        debug.wait("Debug — polling every 500 ms");

        info.log("── DATA — Solarized violet #6c71c4 FG ──");
        info.table("Rendering accounts table");   info.grid("Rendering activity grid");
        info.table(List.of(
                fields("ID", "U-001", "Name", "Alice Nguyen", "Plan", "Pro",     "Active", true),
                fields("ID", "U-002", "Name", "Bob Chen",    "Plan", "Starter",  "Active", true),
                fields("ID", "U-003", "Name", "Carol Davis", "Plan", "Free",     "Active", false)
        ), "Users — SOLARIZED_DARK");
        info.row(fields("Environment", "Production", "Region", "us-east-1", "Build", "v4.2.1"));
        debug.table(List.of(
                fields("Check", "DB connection", "Result", "PASS"),
                fields("Check", "Cache warmup",  "Result", "PASS")
        ), "Health Checks");

        info.log("── SUCCESS — Solarized green #859900 FG ──");
        info.success("User profile saved successfully");   info.complete("Onboarding wizard completed");
        info.resolved("Element resolved via CSS selector",
                fields("strategy", "CSS", "value", "#save-btn", "retries", 1));
        debug.success("Debug — assertion passed");

        info.log("── ALERT — Solarized orange #cb4b16 FG ──");
        info.error("Network request failed: 503 Service Unavailable");
        info.failed("Step 'verifyDashboard' — FAILED");
        info.timeout("Page load exceeded 15 s timeout");
        info.validation("Password must be at least 12 characters");
        info.fallback("CSS locator failed, retrying with XPath");
        info.skip("Skipping optional cookie consent banner");
        warn.error("Warn-level error (muted-amber BG + orange FG)");
        error.failed("Error-level failure (muted-crimson BG + orange FG)");

        info.log("── click across all levels ──");
        info.click("INFO  click — base02 BG");  warn.click("WARN  click — darkened yellow BG");
        error.click("ERROR click — darkened red BG");  debug.click("DEBUG click — base03 BG");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("SOLARIZED_DARK smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── HIGH_CONTRAST theme — dedicated smoke test ────────────────────────────

    /**
     * Dedicated HIGH_CONTRAST smoke test.
     * Pure-black backgrounds on all log levels; bold maximum-saturation foregrounds.
     * WCAG AA+, projectors, low-quality displays.
     */
    @Test(description = "HIGH_CONTRAST theme — full intent sweep + level BG verification",
          priority = 110)
    public void testHighContrastTheme() {
        setTheme(LogTheme.HIGH_CONTRAST);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  HIGH_CONTRAST — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("  Pure-black BG on all levels — maximum-saturation bold FGs");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — bright WHITE  FG on black BG");
        warn.log ("WARN  — bright YELLOW FG on black BG");
        error.log("ERROR — bright RED    FG on black BG");
        debug.log("DEBUG — bright GREY   FG on black BG");

        info.log("── INTERACTION — bright white bold FG ──");
        info.click("Clicked the Submit button");   info.checkbox("Checked 'Accept Terms'");
        info.text("Typed 'alice@example.com'");    info.input("Entered display name 'Alice'");
        info.dropdown("Selected country: Australia");
        info.toggle("Toggled dark mode ON");        info.upload("Uploaded file: report.pdf");
        warn.click("Warn-level click — yellow FG, black BG");
        error.click("Error-level click — red FG, black BG");
        debug.click("Debug-level click — grey FG, black BG");

        info.log("── NAVIGATION — bright cyan bold FG ──");
        info.tab("Switched to tab: Settings");
        info.frame("Entered iFrame: payment-frame");
        info.breadcrumb("Home / Users / Edit");
        debug.frame("Debug — frame context entered");

        info.log("── OBSERVE — bright yellow bold FG ──");
        info.wait("Waiting for spinner to disappear");
        info.search("Searched for 'Alice'");   info.result("Query returned 42 records");
        debug.wait("Debug — polling every 500 ms");

        info.log("── DATA — bright magenta bold FG ──");
        info.table("Rendering users table");   info.grid("Rendering data grid");
        info.table(List.of(
                fields("ID", 1, "Name", "Alice",   "Role", "Admin"),
                fields("ID", 2, "Name", "Bob",     "Role", "User"),
                fields("ID", 3, "Name", "Charlie", "Role", "Viewer")
        ), "Users — HIGH_CONTRAST");
        info.row(fields("Host", "localhost", "Port", 8080, "Secure", true));
        debug.table(List.of(
                fields("Check", "DB connection", "Result", "PASS"),
                fields("Check", "Cache warmup",  "Result", "PASS")
        ), "Health Checks");

        info.log("── SUCCESS — bright green bold FG ──");
        info.success("Login successful");  info.complete("Form submission completed");
        info.resolved("Locator resolved via CSS selector",
                fields("strategy", "CSS", "value", "#save-btn", "retries", 1));
        debug.success("Debug — assertion passed");

        info.log("── ALERT — bright red bold FG ──");
        info.error("Element not found");    info.failed("Step 'clickSubmit' — FAILED");
        info.timeout("Element did not appear within 10 s");
        info.validation("Email format is invalid");
        info.fallback("Primary locator failed, falling back to XPath");
        info.skip("Skipping optional step: cookie banner");
        warn.error("Warn-level error signal (yellow FG + black BG)");
        error.failed("Error-level failure (red FG throughout)");

        info.log("── full intent rainbow at INFO level (pure-black BG) ──");
        info.click("INTERACTION — bright white");   info.tab("NAVIGATION  — bright cyan");
        info.wait("OBSERVE     — bright yellow");   info.table("DATA        — bright magenta");
        info.success("SUCCESS     — bright green"); info.error("ALERT       — bright red");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("HIGH_CONTRAST smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── MODERN_CLEAN theme — dedicated smoke test ─────────────────────────────

    /** Dedicated MODERN_CLEAN smoke test — polished dark IDE style. */
    @Test(description = "MODERN_CLEAN theme — full intent sweep + level BG verification",
          priority = 111)
    public void testModernCleanTheme() {
        setTheme(LogTheme.MODERN_CLEAN);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  MODERN_CLEAN — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — charcoal BG  rgb(35,38,46)");
        warn.log ("WARN  — dark amber BG rgb(95,62,0)");
        error.log("ERROR — dark crimson BG rgb(100,20,25)");
        debug.log("DEBUG — midnight BG  rgb(16,18,26)");

        info.log("── INTERACTION — snow-white bold FG ──");
        info.click("Clicked the primary CTA button");   info.checkbox("Checked 'Remember me'");
        info.text("Typed 'alice@company.com'");          info.input("Entered display name 'Alice Nguyen'");
        info.dropdown("Selected plan tier: Professional");
        info.toggle("Toggled two-factor authentication ON");
        info.upload("Uploaded avatar: profile-photo.png");
        debug.click("Debug — click event dispatched");

        info.log("── NAVIGATION — steel-cyan bold FG ──");
        info.tab("Switched to tab: Account Settings");
        info.frame("Entered iFrame: billing-widget");
        info.breadcrumb("Dashboard / Settings / Profile");
        debug.frame("Debug — frame context entered");

        info.log("── OBSERVE — gold bold FG ──");
        info.wait("Waiting for dashboard tiles to load");
        info.search("Searched for user 'alice'");  info.result("3 matching accounts returned");
        debug.wait("Debug — polling every 500 ms");

        info.log("── DATA — lavender bold FG ──");
        info.table("Rendering accounts table");   info.grid("Rendering activity grid");
        info.table(List.of(
                fields("ID", "U-001", "Name", "Alice Nguyen", "Plan", "Pro",   "Active", true),
                fields("ID", "U-002", "Name", "Bob Chen",    "Plan", "Starter","Active", true),
                fields("ID", "U-003", "Name", "Carol Davis", "Plan", "Free",   "Active", false)
        ), "Users — MODERN_CLEAN");
        info.row(fields("Environment", "Production", "Region", "us-east-1", "Build", "v4.2.1"));
        debug.table(List.of(
                fields("Check", "DB connection",  "Result", "PASS"),
                fields("Check", "Cache warmup",   "Result", "PASS"),
                fields("Check", "Feature flags",  "Result", "SKIP")
        ), "Health Checks");

        info.log("── SUCCESS — lime-green bold FG ──");
        info.success("User profile saved successfully");   info.complete("Onboarding wizard completed");
        info.resolved("Element resolved via CSS selector",
                fields("strategy", "CSS", "value", "#save-btn", "retries", 1));
        debug.success("Debug — assertion passed");

        info.log("── ALERT — coral bold FG ──");
        info.error("Network request failed: 503");  info.failed("Step 'verifyDashboard' — FAILED");
        info.timeout("Page load exceeded 15 s");    info.validation("Password must be ≥ 12 chars");
        info.fallback("CSS locator failed, retrying with XPath");
        info.skip("Skipping optional cookie consent banner");
        warn.error("Warn-level error (amber BG + coral FG)");
        error.failed("Error-level failure (crimson BG + coral FG)");

        info.log("── click across all levels ──");
        info.click("INFO  click — charcoal BG"); warn.click("WARN  click — dark amber BG");
        error.click("ERROR click — crimson BG"); debug.click("DEBUG click — midnight BG");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("MODERN_CLEAN smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── INDUSTRIAL_STEEL theme — dedicated smoke test ─────────────────────────

    /** Dedicated INDUSTRIAL_STEEL smoke test — factory-floor HMI personality. */
    @Test(description = "INDUSTRIAL_STEEL theme — full intent sweep + level BG verification",
          priority = 112)
    public void testIndustrialSteelTheme() {
        setTheme(LogTheme.INDUSTRIAL_STEEL);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  INDUSTRIAL_STEEL — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — brushed steel BG  rgb(55,60,70)");
        warn.log ("WARN  — safety yellow BG  rgb(175,135,0)");
        error.log("ERROR — danger red BG     rgb(155,22,22)");
        debug.log("DEBUG — coal/soot BG      rgb(20,22,28)");

        info.log("── INTERACTION — snow-white FG ──");
        info.click("Actuated valve control V-101");    info.checkbox("Confirmed safety interlock");
        info.text("Typed operator ID 'OPR-0042'");     info.input("Set setpoint: 750 RPM");
        info.dropdown("Selected operating mode: AUTO");
        info.toggle("Toggled emergency-stop bypass OFF");
        info.upload("Uploaded PLC program: line3-v2.bin");
        debug.click("Debug — HMI button pressed");

        info.log("── NAVIGATION — instrument-blue FG ──");
        info.tab("Switched to tab: Process Overview");
        info.frame("Entered panel: Boiler Control");
        info.breadcrumb("Plant / Line-3 / Boiler / Controls");
        debug.frame("Debug — frame focus changed");

        info.log("── OBSERVE — amber caution-lamp FG ──");
        info.wait("Waiting for motor to reach steady state");
        info.search("Scanning sensor tag: TI-204");
        info.result("Temperature reading: 182.4 °C (within range)");
        debug.wait("Debug — polling sensor every 250 ms");

        info.log("── DATA — phosphor-green CRT FG ──");
        info.table("Rendering process values table"); info.grid("Rendering alarm grid");
        info.table(List.of(
                fields("Tag", "TI-204", "Value", "182.4°C",  "Status", "NORMAL"),
                fields("Tag", "PI-112", "Value", "4.2 bar",  "Status", "NORMAL"),
                fields("Tag", "FI-310", "Value", "0.0 L/min","Status", "LOW FLOW")
        ), "Process Values — INDUSTRIAL_STEEL");
        info.row(fields("Shift", "Day", "Operator", "OPR-0042", "Line", "3", "UPH", 847));

        info.log("── SUCCESS — go-signal green FG ──");
        info.success("Batch #B-2041 completed within tolerance");
        info.complete("Startup sequence finished — all systems nominal");
        info.resolved("Alarm resolved: FI-310 flow restored",
                fields("tag", "FI-310", "ack_by", "OPR-0042", "duration_s", 47));
        debug.success("Debug — unit test assertion passed");

        info.log("── ALERT — danger-lamp red FG ──");
        info.error("Sensor TI-204 exceeded high-high limit (220 °C)");
        info.failed("Interlock sequence ILK-007 — FAILED to latch");
        info.timeout("Actuator V-101 did not confirm open within 5 s");
        info.validation("Setpoint 1200 RPM exceeds safe operating limit");
        info.fallback("Primary PLC comm failed, switching to backup path");
        info.skip("Skipping optional pre-heat phase — already warm");
        warn.error("Warn-level alarm (yellow BG + red FG)");
        error.failed("Error-level trip (red BG + red FG)");

        info.log("── click across all levels ──");
        info.click("INFO  click — steel BG");  warn.click("WARN  click — yellow BG");
        error.click("ERROR click — red BG");   debug.click("DEBUG click — coal BG");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("INDUSTRIAL_STEEL smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── NIGHT_CLUB theme — dedicated smoke test ───────────────────────────────

    /** Dedicated NIGHT_CLUB smoke test — vivid neon / disco club personality. */
    @Test(description = "NIGHT_CLUB theme — full intent sweep + level BG verification",
          priority = 113)
    public void testNightClubTheme() {
        setTheme(LogTheme.NIGHT_CLUB);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  NIGHT_CLUB — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — deep teal BG  rgb(14,66,66)");
        warn.log ("WARN  — hot pink BG   rgb(195,38,115)");
        error.log("ERROR — neon purple BG rgb(115,18,175)");
        debug.log("DEBUG — dark indigo BG rgb(28,24,68)");

        info.log("── INTERACTION — hot-pink FG ──");
        info.click("Tapped track: 'Midnight Drive' ▶");   info.checkbox("Checked 'VIP Access'");
        info.text("Entered DJ alias 'NightFox'");           info.input("Set BPM: 128");
        info.dropdown("Selected genre: Synthwave");
        info.toggle("Toggled strobe lights ON");
        info.upload("Uploaded set list: saturday-night.m3u");
        debug.click("Debug — beat trigger fired");

        info.log("── NAVIGATION — sky-blue FG ──");
        info.tab("Switched to tab: Dance Floor");
        info.frame("Entered screen: VJ Visualizer");
        info.breadcrumb("Venue / Stage / DJ Booth / Mixer");
        debug.frame("Debug — viewport changed");

        info.log("── OBSERVE — neon-green FG ──");
        info.wait("Waiting for crowd energy to peak");
        info.search("Searching track library for 'Daft Punk'");
        info.result("47 tracks found matching query");
        debug.wait("Debug — audio buffer filling...");

        info.log("── DATA — gold FG ──");
        info.table("Rendering setlist table"); info.grid("Rendering effects grid");
        info.table(List.of(
                fields("Track", "Midnight Drive",  "BPM", 128, "Key", "Am"),
                fields("Track", "Neon Horizon",    "BPM", 132, "Key", "Cm"),
                fields("Track", "Electric Sunset", "BPM", 124, "Key", "Dm")
        ), "Tonight's Setlist — NIGHT_CLUB");
        info.row(fields("Venue", "Warehouse 23", "Capacity", 800, "Stage", "Main", "Hour", "23:00"));

        info.log("── SUCCESS — bright green FG ──");
        info.success("Drop landed perfectly — crowd response: PEAK");
        info.complete("Full set played without incident");
        info.resolved("Next track cued and crossfade resolved",
                fields("from", "Midnight Drive", "to", "Neon Horizon", "xfade_s", 8));
        debug.success("Debug — audio engine assertion passed");

        info.log("── ALERT — coral FG ──");
        info.error("Audio clipping detected on channel 3 (>0 dBFS)");
        info.failed("Crossfade automation — FAILED to trigger");
        info.timeout("DMX controller did not respond within 2 s");
        info.validation("BPM 220 is outside the supported sync range");
        info.fallback("Primary audio path failed, switching to backup deck");
        info.skip("Skipping optional intro jingle");
        warn.error("Warn-level audio fault (hot-pink BG + coral FG)");
        error.failed("Error-level system fault (neon-purple BG + coral FG)");

        info.log("── click across all levels ──");
        info.click("INFO  click — teal BG");      warn.click("WARN  click — hot-pink BG");
        error.click("ERROR click — purple BG");   debug.click("DEBUG click — indigo BG");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("NIGHT_CLUB smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── CARBON_ORANGE theme — dedicated smoke test ────────────────────────────

    /** Dedicated CARBON_ORANGE smoke test — dark-carbon base with amber/orange accent. */
    @Test(description = "CARBON_ORANGE theme — full intent sweep + level BG verification",
          priority = 114)
    public void testCarbonOrangeTheme() {
        setTheme(LogTheme.CARBON_ORANGE);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  CARBON_ORANGE — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  — carbon BG  rgb(28,28,32)");
        warn.log ("WARN  — vivid orange BG  rgb(200,100,0)");
        error.log("ERROR — crimson BG  rgb(100,20,25)");
        debug.log("DEBUG — midnight BG rgb(16,18,26)");

        info.log("── INTERACTION — amber bold FG (signature) ──");
        info.click("Fired API call: POST /api/orders");  info.checkbox("Confirmed 'Enable retries'");
        info.text("Entered search query: 'order-8842'"); info.input("Set retry limit: 3");
        info.dropdown("Selected environment: Staging");
        info.toggle("Toggled verbose logging ON");
        info.upload("Uploaded test fixtures: orders.json");
        debug.click("Debug — HTTP interceptor triggered");

        info.log("── NAVIGATION — steel-cyan FG ──");
        info.tab("Switched to tab: API Explorer");
        info.frame("Entered modal: Request Builder");
        info.breadcrumb("Console / APIs / Orders / POST");
        debug.frame("Debug — panel focus changed");

        info.log("── OBSERVE — peach FG ──");
        info.wait("Waiting for response: POST /api/orders (timeout 30 s)");
        info.search("Querying order-service for order-8842");
        info.result("Response 201 Created — order-id: 8842-alpha");
        debug.wait("Debug — retry attempt 1/3");

        info.log("── DATA — lavender FG ──");
        info.table("Rendering orders table");  info.grid("Rendering metrics grid");
        info.table(List.of(
                fields("Order",  "8842-alpha", "Status", "CREATED",   "Total", "$149.00"),
                fields("Order",  "8841-zeta",  "Status", "SHIPPED",   "Total", "$89.99"),
                fields("Order",  "8840-beta",  "Status", "DELIVERED", "Total", "$220.50")
        ), "Recent Orders — CARBON_ORANGE");
        info.row(fields("Service", "order-service", "Version", "2.4.1",
                "Latency_ms", 84, "Env", "Staging"));
        debug.table(List.of(
                fields("Metric", "p50_ms", "Value", 42),
                fields("Metric", "p95_ms", "Value", 118),
                fields("Metric", "p99_ms", "Value", 290)
        ), "Latency Percentiles");

        info.log("── SUCCESS — lime-green FG ──");
        info.success("Order 8842-alpha created and confirmed");
        info.complete("End-to-end checkout flow completed");
        info.resolved("Payment gateway resolved via fallback processor",
                fields("primary", "Stripe", "fallback", "PayPal", "latency_ms", 204));
        debug.success("Debug — contract assertion passed");

        info.log("── ALERT — coral FG ──");
        info.error("Inventory service returned 500 Internal Server Error");
        info.failed("Checkout step 'reserveStock' — FAILED");
        info.timeout("Payment gateway timeout after 30 s");
        info.validation("Promo code 'SAVE20' is expired");
        info.fallback("Primary payment processor failed, using backup");
        info.skip("Skipping optional loyalty-points redemption step");
        warn.error("Warn-level error (vivid-orange BG + coral FG)");
        error.failed("Error-level failure (crimson BG + coral FG)");

        info.log("── click across all levels — amber FG constant ──");
        info.click("INFO  click — carbon BG");    warn.click("WARN  click — orange BG");
        error.click("ERROR click — crimson BG");  debug.click("DEBUG click — midnight BG");

        info.log("── table across all levels — lavender FG stays, BG shifts ──");
        info.table("table at INFO  (carbon BG + lavender FG)");
        warn.table("table at WARN  (orange BG + lavender FG)");
        error.table("table at ERROR (crimson BG + lavender FG)");
        debug.table("table at DEBUG (midnight BG + lavender FG)");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("CARBON_ORANGE smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── COCKPIT theme — dedicated smoke test ──────────────────────────────────

    /**
     * Dedicated COCKPIT smoke test — mission-control dashboard personality.
     * Semantic status-light level BGs: dark-green / dark-amber / maroon-red / dark-grey.
     */
    @Test(description = "COCKPIT theme — full intent sweep + level BG verification",
          priority = 115)
    public void testCockpitTheme() {
        setTheme(LogTheme.COCKPIT);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  COCKPIT THEME — dedicated smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        info.log ("INFO  level  — dark green  BG  (nominal / all systems go)");
        warn.log ("WARN  level  — dark amber  BG  (caution)");
        error.log("ERROR level  — maroon red  BG  (critical alert)");
        debug.log("DEBUG level  — dark grey   BG  (diagnostic / quiet)");

        info.log("── INTERACTION — snow-white FG on green BG ──");
        info.click("Clicked mission-control panel");  info.checkbox("Checked 'Arm system'");
        info.text("Typed call-sign 'VIPER-1'");
        info.input("Entered coordinates 34.0522° N, 118.2437° W");
        info.dropdown("Selected frequency band: UHF");
        info.toggle("Toggled autopilot ON");
        info.upload("Uploaded flight plan: mission-alpha.json");

        info.log("── NAVIGATION — mint FG on green BG ──");
        info.tab("Switched to tab: Radar");
        info.frame("Switched to iFrame: telemetry-feed");
        info.breadcrumb("Mission / Cockpit / Systems / Comms");

        info.log("── OBSERVE — gold FG on green BG ──");
        info.wait("Awaiting transponder acknowledgement...");
        info.search("Scanning frequency: 121.5 MHz");
        info.result("Lock acquired — target identified");

        info.log("── DATA — lavender FG on green BG  (COCKPIT signature) ──");
        info.table("Rendering telemetry table");  info.grid("Rendering sensor grid");
        info.table(List.of(
                fields("System", "Engines",   "Status", "NOMINAL",  "Temp°C", 820),
                fields("System", "Hydraulics","Status", "NOMINAL",  "PSI",    3000),
                fields("System", "Comms",     "Status", "DEGRADED", "SNR dB", 12)
        ), "Cockpit Systems — COCKPIT theme");
        info.row(fields("Altitude", "35000 ft", "Speed", "Mach 0.85", "Heading", "270°"));

        info.log("── SUCCESS — lime-green FG on green BG ──");
        info.success("All pre-flight checks passed");
        info.complete("Autopilot engaged successfully");
        info.resolved("Nav-beacon resolved via secondary antenna",
                fields("strategy", "GPS", "satellite", "WAAS-35", "accuracy_m", 2));

        info.log("── ALERT — coral FG ──");
        info.error("Engine #2 flame-out detected");
        info.failed("Auto-restart sequence — FAILED");
        info.timeout("ATC response timeout after 30 s");
        info.validation("Fuel load exceeds maximum gross weight");
        info.fallback("Primary nav failed, falling back to INS");
        info.skip("Skipping optional step: cabin pressure check");

        info.log("── ALERT at WARN/ERROR levels ──");
        warn.error("Warn-level alert (amber BG + coral FG)");
        warn.validation("Warn-level validation (amber BG + coral FG)");
        error.failed("Error-level failure (maroon BG + coral FG)");
        error.timeout("Error-level timeout (maroon BG + coral FG)");

        info.log("── click across all levels ──");
        info.click("Click at INFO  — dark-green BG"); warn.click("Click at WARN  — dark-amber BG");
        error.click("Click at ERROR — maroon-red BG"); debug.click("Click at DEBUG — dark-grey  BG");

        info.log("── table across all levels — lavender FG stays, BG shifts ──");
        info.table("table at INFO  (dark-green BG + lavender FG)");
        warn.table("table at WARN  (dark-amber BG + lavender FG)");
        error.table("table at ERROR (maroon-red BG + lavender FG)");
        debug.table("table at DEBUG (dark-grey  BG + lavender FG)");

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("COCKPIT theme smoke test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ── Custom theme ──────────────────────────────────────────────────────────

    /**
     * Builds a bespoke theme via the fluent builder, registers it with
     * {@link CustomLogger#setCustomTheme(ThemeColors)}, runs a visual sweep,
     * then verifies {@link ThemeColors#resolve} returns non-empty strings for
     * every level × intent combination.
     */
    @Test(description = "Custom theme via ThemeColors.builder() — validates builder API and setCustomTheme()",
          priority = 120)
    public void testCustomTheme() {
        // Build a bespoke theme using AnsiColors constants + fluent builder.
        ThemeColors myTheme = ThemeColors.builder()
                .infoBg    (BG_256_DARK_GREEN)
                .warnBg    (BG_256_DARK_OLIVE)
                .errorBg   (BG_256_DARK_RED)
                .debugBg   (BG_256_DARK_GREY)
                .infoFg    (FG_BRIGHT_GREEN  + BOLD)
                .warnFg    (FG_BRIGHT_YELLOW + BOLD)
                .errorFg   (FG_BRIGHT_RED    + BOLD)
                .debugFg   (FG_BRIGHT_WHITE  + BOLD)
                .interactionFg (FG_BRIGHT_WHITE  + BOLD)
                .navigationFg  (FG_BRIGHT_CYAN   + BOLD)
                .observeFg     (FG_BRIGHT_YELLOW + BOLD)
                .dataFg        (FG_BRIGHT_CYAN   + BOLD)
                .successFg     (FG_BRIGHT_GREEN  + BOLD)
                .alertFg       (FG_BRIGHT_RED    + BOLD)
                .callerFg      (FG_DIM_WHITE)
                .build();

        // Apply as active theme through CustomLogger.
        setCustomTheme(myTheme);

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.log("  CUSTOM THEME — builder API smoke test  │  ANSI=" + (ansiEnabled ? "ON" : "OFF"));
        info.log("  Built via ThemeColors.builder()  +  CustomLogger.setCustomTheme()");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Visual sweep — should now render with the custom palette.
        info.log ("INFO  level — dark-green BG + bright-green FG");
        warn.log ("WARN  level — olive BG     + bright-yellow FG");
        error.log("ERROR level — dark-red BG  + bright-red FG");
        debug.log("DEBUG level — dark-grey BG + bright-white FG");

        info.click("INTERACTION — bright white FG");
        info.tab("NAVIGATION  — bright cyan FG");
        info.wait("OBSERVE     — bright yellow FG");
        info.table("DATA        — bright cyan FG");
        info.success("SUCCESS     — bright green FG");
        info.error("ALERT       — bright red FG");

        info.table(List.of(
                fields("Level", "INFO",  "BG", "dark-green",  "FG", "bright-green"),
                fields("Level", "WARN",  "BG", "dark-olive",  "FG", "bright-yellow"),
                fields("Level", "ERROR", "BG", "dark-red",    "FG", "bright-red"),
                fields("Level", "DEBUG", "BG", "dark-grey",   "FG", "bright-white")
        ), "Custom Theme — Level Map");

        // Programmatic verification: resolve() must return non-empty for every cell.
        String[] levels     = {"INFO", "WARN", "ERROR", "DEBUG"};
        LogIntent[] intents = LogIntent.values();

        info.log("resolve() matrix verification:");
        for (String level : levels) {
            for (LogIntent intent : intents) {
                String resolved = myTheme.resolve(level, intent);
                assert resolved != null && !resolved.isEmpty()
                        : "resolve(" + level + ", " + intent + ") returned empty";
            }
            info.success("Level " + level + " — all " + intents.length + " intents resolved OK");
        }

        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        info.success("Custom theme test complete");
        info.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Restore to PLAIN so @AfterMethod cleanup is unambiguous.
        setTheme(LogTheme.PLAIN);
    }
}

