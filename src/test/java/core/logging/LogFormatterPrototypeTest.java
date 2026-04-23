package core.logging;

import core.logging.config.LogConfig;

import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * PROTOTYPE — Fixed-Width Columnar Log Formatter  (v2)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Layout:
 *   [Timestamp 23] │ [Level 7] │ [Action 18] │ message (unlimited) → trace
 *
 * Strategy:
 *   • First 3 columns → fixed width, always aligned.
 *   • Message          → free-flow, no trim, no wrap.
 *   • Trace            → optional suffix, dimmed.
 *   • Padding applied BEFORE color so ANSI bytes never shift alignment.
 *
 * Run:  mvn -pl . test -Dtest=LogFormatterPrototypeTest -q
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class LogFormatterPrototypeTest {

    // ── Column widths — read from LogConfig (override if needed) ─────────────
    private static int tsWidth()     { return LogConfig.current().getTsWidth();     }
    private static int levelWidth()  { return LogConfig.current().getLevelWidth();  }
    private static int actionWidth() { return LogConfig.current().getActionWidth(); }

    private static final String DIV   = " │ ";
    private static final String ARROW = " → ";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── ANSI shortcuts ───────────────────────────────────────────────────────
    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";

    // Level colors
    private static final String C_ERROR = "\u001B[38;2;255;105;85m";   // coral
    private static final String C_WARN  = "\u001B[38;2;255;200;50m";   // gold
    private static final String C_INFO  = "\u001B[38;2;80;185;255m";   // sky-blue
    private static final String C_DEBUG = "\u001B[38;2;140;150;165m";  // cool-grey

    // Action intent colors
    private static final String C_INTERACTION = "\u001B[38;2;60;220;175m";   // mint
    private static final String C_NAVIGATION  = "\u001B[38;2;185;155;255m";  // lavender
    private static final String C_OBSERVE     = "\u001B[38;2;255;185;110m";  // peach
    private static final String C_DATA        = "\u001B[38;2;90;220;220m";   // steel-cyan
    private static final String C_SUCCESS     = "\u001B[38;2;100;240;120m";  // lime-green
    private static final String C_ALERT       = "\u001B[38;2;255;105;85m";   // coral
    private static final String C_BASE        = "\u001B[38;2;210;215;220m";  // soft-white

    // Timestamp / trace dim color
    private static final String C_DIM   = "\u001B[38;5;240m";  // dark-grey

    // ── Utility: left-pad to fixed visible width (no ANSI, no truncation) ────

    /**
     * Left-justify {@code text} in exactly {@code width} visible characters.
     * If {@code width} is 0 the text is returned as-is (free-flow, no padding).
     * Call this BEFORE injecting any ANSI codes.
     */
    static String pad(String text, int width) {
        if (text == null) text = "";
        if (width <= 0) return text;           // 0 = no fixed width, free-flow
        return String.format("%-" + width + "s", text);
    }

    /** Strip ANSI — use when writing to plain-text file. */
    static String stripAnsi(String text) {
        return text == null ? "" : text.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    // ── Colorize helpers (always called AFTER padding) ────────────────────────

    static String colorizeLevel(String paddedLevel, String level) {
        String c = switch (level.trim().toUpperCase()) {
            case "ERROR" -> C_ERROR;
            case "WARN"  -> C_WARN;
            case "INFO"  -> C_INFO;
            default      -> C_DEBUG;
        };
        return BOLD + c + paddedLevel + RESET;
    }

    static String colorizeAction(String paddedAction, String intent) {
        String c = switch (intent.toUpperCase()) {
            case "INTERACTION" -> C_INTERACTION;
            case "NAVIGATION"  -> C_NAVIGATION;
            case "OBSERVE"     -> C_OBSERVE;
            case "DATA"        -> C_DATA;
            case "SUCCESS"     -> C_SUCCESS;
            case "ALERT"       -> C_ALERT;
            default            -> C_BASE;
        };
        return c + paddedAction + RESET;
    }

    // ── Core formatter ────────────────────────────────────────────────────────

    /**
     * Build one fully-formatted log line.
     *
     * <pre>
     *   [TS 23] │ [LEVEL 7] │ [ACTION 18] │ message text... → CallerTrace
     * </pre>
     *
     * @param level   "INFO" | "WARN" | "ERROR" | "DEBUG"
     * @param action  action label, e.g. "CLICK [>]"
     * @param intent  intent group, e.g. "INTERACTION"
     * @param message free-form message — no width limit
     * @param trace   optional caller trace; null or blank → omitted
     * @param ansi    true → colorized console; false → plain file output
     */
    static String format(String level, String action, String intent,
                         String message, String trace, boolean ansi) {

        String ts = LocalDateTime.now().format(FMT);

        // 1. Pad the three structural columns BEFORE touching any color
        String tsP     = pad(ts,     tsWidth());
        String levelP  = pad(level,  levelWidth());
        String actionP = pad(action, actionWidth());

        // 2. Optional trace suffix
        String trailRaw = (trace != null && !trace.isBlank()) ? ARROW + trace : "";

        if (!ansi) {
            // Plain / file-safe: padded structure + free message + trace
            return tsP + DIV + levelP + DIV + actionP + DIV + message + trailRaw;
        }

        // 3. Apply color AFTER padding
        String tsCol     = C_DIM + tsP     + RESET;
        String levelCol  = colorizeLevel(levelP,  level);
        String actionCol = colorizeAction(actionP, intent);
        String msgCol    = C_BASE + message + RESET;
        String trailCol  = trailRaw.isBlank() ? "" : C_DIM + trailRaw + RESET;

        return tsCol + DIV + levelCol + DIV + actionCol + DIV + msgCol + trailCol;
    }

    // ── Sample entries ────────────────────────────────────────────────────────

    record LogEntry(String level, String action, String intent,
                    String message, String trace) {}

    private static final LogEntry[] SAMPLES = {
        new LogEntry("INFO",  "CLICK [>]",    "INTERACTION", "Click executed on login button",                              "LoginPage.clickSubmit ← LoginTest.run"),
        new LogEntry("INFO",  "INPUT [>>]",   "INTERACTION", "Entered username: admin_super_long_value_here",               "LoginPage.setUsername ← LoginTest.run"),
        new LogEntry("INFO",  "TAB [->]",     "NAVIGATION",  "Switched to Settings tab",                                   "NavBar.selectTab ← SettingsTest.open"),
        new LogEntry("INFO",  "WAIT [~]",     "OBSERVE",     "Waiting for spinner to disappear",                           "BasePage.waitForLoad ← DashboardTest.verify"),
        new LogEntry("INFO",  "RESULT [:]",   "OBSERVE",     "Found 24 matching records",                                  "SearchPage.getResults ← SearchTest.run"),
        new LogEntry("INFO",  "TABLE [=]",    "DATA",        "Rendering user data table — 6 columns, 42 rows",             "TableHelper.render ← UserTest.verify"),
        new LogEntry("INFO",  "SUCCESS [+]",  "SUCCESS",     "Login flow completed successfully",                           "LoginPage.login ← SmokeTest.verifyLogin"),
        new LogEntry("WARN",  "FALLBACK [<-]","ALERT",       "CSS locator failed — falling back to XPath strategy",        "LocatorResolver.resolve ← BasePage.find"),
        new LogEntry("ERROR", "TIMEOUT [!!]", "ALERT",       "Element '.spinner' not visible after 10 000 ms",             "WaitUtils.forVisible ← OrderPage.submit"),
        new LogEntry("DEBUG", "FRAME [{}]",   "NAVIGATION",  "Switched to iframe #payment-gateway",                        "FrameHelper.switchTo ← PaymentPage.fill"),
        new LogEntry("INFO",  "DROPDOWN [v]", "INTERACTION", "Selected option 'New Zealand' from country dropdown",        "CountryDropdown.select ← AddressForm.fill"),
        // No trace example
        new LogEntry("INFO",  "LOG",          "BASE",        "Configuration loaded from test.properties",                   null),
    };

    // ── TestNG test ───────────────────────────────────────────────────────────

    @Test
    public void prototypeColumnarOutput() {
        System.out.println();
        System.out.println("── CONSOLE (ANSI colored) — default LogConfig " + "─".repeat(60));
        printHeader();
        printRuler();

        for (LogEntry e : SAMPLES) {
            System.out.println(format(e.level(), e.action(), e.intent(),
                                      e.message(), e.trace(), true));
        }

        printRuler();
        System.out.println();
        System.out.println("── FILE (plain, no ANSI) " + "─".repeat(81));
        printRuler();

        for (LogEntry e : SAMPLES) {
            System.out.println(format(e.level(), e.action(), e.intent(),
                                      e.message(), e.trace(), false));
        }

        printRuler();
        System.out.println();
    }

    /**
     * Demonstrates live reconfiguration via {@link LogConfig}.
     * Widens the action column and shrinks the TS column at runtime — no restart needed.
     */
    @Test
    public void prototypeLogConfigLiveReconfig() {
        System.out.println();
        System.out.println("── Before: default column widths ─────────────────────────────────────────");
        printHeader();
        printRuler();
        System.out.println(format("INFO", "CLICK [>]", "INTERACTION",
                "Default column widths from LogConfig", "Demo.before", true));
        printRuler();

        // ── Live patch via LogConfig ──────────────────────────────────────────
        LogConfig.patch(c -> c
                .setTsWidth(19)
                .setLevelWidth(5)
                .setActionWidth(22));

        System.out.println();
        System.out.println("── After:  tsWidth=19  levelWidth=5  actionWidth=22 (patched at runtime) ─");
        printHeader();
        printRuler();
        System.out.println(format("WARN",  "FALLBACK [<-]", "ALERT",
                "Column widths changed without rebuild — LogConfig.patch()", "Demo.after", true));
        System.out.println(format("ERROR", "TIMEOUT [!!]",  "ALERT",
                "Every subsequent log line reflects the new widths immediately", "Demo.after", true));
        printRuler();

        // ── Restore defaults ──────────────────────────────────────────────────
        LogConfig.patch(c -> c
                .setTsWidth(LogConfig.Builder.DEFAULT_TS_WIDTH)
                .setLevelWidth(LogConfig.Builder.DEFAULT_LEVEL_WIDTH)
                .setActionWidth(LogConfig.Builder.DEFAULT_ACTION_WIDTH));
        System.out.println();
    }

    // ── Header / ruler ────────────────────────────────────────────────────────

    private void printHeader() {
        String header = BOLD
                + pad("TIMESTAMP",  tsWidth())     + DIV
                + pad("LEVEL",      levelWidth())  + DIV
                + pad("ACTION",     actionWidth()) + DIV
                + "MESSAGE" + RESET;
        System.out.println(header);
    }

    private void printRuler() {
        String ruler = "─".repeat(tsWidth())     + "─┼─"
                     + "─".repeat(levelWidth())  + "─┼─"
                     + "─".repeat(actionWidth()) + "─┼─"
                     + "─".repeat(55);
        System.out.println(C_DIM + ruler + RESET);
    }
}
