package core.logging;

import org.apache.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central logger facade for the void-framework.
 *
 * <h2>Quick-start</h2>
 * <pre>{@code
 * CustomLogger.info.log("Hello, world!");
 * CustomLogger.warn.click("Submit button");
 * CustomLogger.error.failed("Connection timed out");
 * CustomLogger.debug.table(myMap);
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li>{@link AnsiColors}    — all ANSI escape-code constants</li>
 *   <li>{@link LogIntent}     — semantic intent enum (INTERACTION, NAVIGATION, …)</li>
 *   <li>{@link LogTheme}      — theme selection enum</li>
 *   <li>{@link ThemeColors}   — immutable theme model + fluent builder</li>
 *   <li>{@link BuiltInThemes} — pre-built themes + active-theme registry</li>
 *   <li>{@link LogActions}    — all action methods (click, table, success, …)</li>
 *   <li>{@link CustomLogger}  — (this class) global config + level instances</li>
 * </ul>
 */
public class CustomLogger {

    // ── Timestamp format (shared with LogActions) ─────────────────────────────
    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── Cell truncation (shared with LogActions) ──────────────────────────────
    private static final int MAX_COL_WIDTH = 40;

    public static String truncateCell(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\R", " ");
        return s.length() > MAX_COL_WIDTH ? s.substring(0, MAX_COL_WIDTH - 3) + "..." : s;
    }

    // ── Level instances ───────────────────────────────────────────────────────

    public static final Debug debug = new Debug();
    public static final Info  info  = new Info();
    public static final Warn  warn  = new Warn();
    public static final Error error = new Error();

    // ── Level classes ─────────────────────────────────────────────────────────

    /** DEBUG-level logger. Composites every intent foreground with {@link ThemeColors#debugBg()}. */
    public static class Debug extends LogActions {
        public Debug() { super("DEBUG"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "DEBUG", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "DEBUG"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, CustomLogger.fields(pairs)); }
    }

    /** INFO-level logger. */
    public static class Info extends LogActions {
        public Info() { super("INFO"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "INFO", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "INFO"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, CustomLogger.fields(pairs)); }
    }

    /** WARN-level logger. */
    public static class Warn extends LogActions {
        public Warn() { super("WARN"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "WARN", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "WARN"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, CustomLogger.fields(pairs)); }
    }

    /** ERROR-level logger. */
    public static class Error extends LogActions {
        public Error() { super("ERROR"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "ERROR", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "ERROR"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, CustomLogger.fields(pairs)); }
    }

    // ── Helper: key/value field builder ──────────────────────────────────────

    public static LinkedHashMap<String, Object> fields(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("fields() requires an even number of key/value arguments");
        for (int i = 0; i < pairs.length; i += 2)
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }

    // ── Log4j logger ─────────────────────────────────────────────────────────

    protected static Logger log = Logger.getLogger(CustomLogger.class);

    public static void initialize(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }

    public static Logger getSafeLogger() {
        return (log != null) ? log : Logger.getLogger(CustomLogger.class);
    }

    public static boolean isDebugEnabled() { return getSafeLogger().isDebugEnabled(); }

    // ── ANSI support ──────────────────────────────────────────────────────────

    private static final AtomicBoolean ansiEnabled = new AtomicBoolean(detectAnsiSupport());

    private static boolean detectAnsiSupport() {
        String prop = System.getProperty("logger.ansi.enabled");
        if (prop != null) return Boolean.parseBoolean(prop);
        if (System.getProperty("idea.test.cyclic.buffer.size") != null) return false;
        if (System.getProperty("idea.launcher.bin.path")       != null) return false;
        if (System.console() != null) return true;
        if (System.getenv("TERM")      != null) return true;
        if ("true".equals(System.getenv("ANSICON"))) return true;
        return System.getenv("COLORTERM") != null;
    }

    public static void enableAnsi()       { ansiEnabled.set(true);  }
    public static void disableAnsi()      { ansiEnabled.set(false); }
    public static boolean isAnsiEnabled() { return ansiEnabled.get(); }

    // ── Theme selection — delegates to BuiltInThemes ──────────────────────────

    public static void setTheme(LogTheme theme)           { BuiltInThemes.setTheme(theme); }
    public static void setCustomTheme(ThemeColors colors) { BuiltInThemes.setCustomTheme(colors); }
    public static LogTheme getCurrentTheme()              { return BuiltInThemes.getCurrentTheme(); }

    // ── Caller-color feature flag ─────────────────────────────────────────────

    /**
     * When {@code true}, the caller/callee suffix is rendered with its own ANSI color.
     *
     * <p><b>⚠️ {@literal @ConsoleOnly} — DO NOT enable in CI or file-appender runs.</b></p>
     */
    @ConsoleOnly
    private static volatile boolean callerColorEnabled = false;

    @ConsoleOnly
    public static void enableCallerColor()       { callerColorEnabled = true;  }
    public static void disableCallerColor()      { callerColorEnabled = false; }
    public static boolean isCallerColorEnabled() { return callerColorEnabled;  }

    // ── Configurable call-chain filtering ─────────────────────────────────────

    public static final Set<String> SUPPRESS_CONTAINS = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "core.logging.CustomLogger", "core.logging.LogActions",
            "org.apache.log4j",
            "java.", "sun.", "jdk.",
            "com.sun.proxy", "jdk.proxy",
            "net.bytebuddy", "reflect."
    )));

    public static final Set<String> SUPPRESS_METHOD_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "log", "debug", "info", "warn", "error", "lambda$", "invoke"
    )));

    public static final Set<String> INCLUDE_ONLY_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

    /** Only consider stack frames whose class starts with one of these prefixes. */
    public static void includeOnlyPackages(String... prefixes) {
        INCLUDE_ONLY_PREFIXES.clear();
        if (prefixes != null)
            for (String p : prefixes) if (p != null && !p.isBlank()) INCLUDE_ONLY_PREFIXES.add(p);
    }

    /** Add class-name substrings to suppress from the call chain. */
    public static void suppressClassContains(String... substrings) {
        if (substrings != null)
            for (String s : substrings) if (s != null && !s.isBlank()) SUPPRESS_CONTAINS.add(s);
    }

    /** Remove include-only filter (revert to default suppression rules). */
    public static void clearIncludes() { INCLUDE_ONLY_PREFIXES.clear(); }

    // ── Experimental utilities ────────────────────────────────────────────────

    /**
     * Helpers that are <b>not</b> part of the stable logging API.
     * May be removed or changed without notice.
     */
    public static final class Experimental {
        private Experimental() {}

        /** Strips all ANSI escape sequences from {@code str}. */
        public static String stripAnsi(String str) {
            return str == null ? "" : str.replaceAll("\\u001B\\[[;\\d]*m", "");
        }

        /**
         * Extracts a foreground ANSI code from a combined FG+BG style string.
         * Not used by the core rendering path; kept for experimental theme tools.
         */
        public static String fgFromStyle(String style) {
            if (style == null) return AnsiColors.FG_BRIGHT_WHITE;
            java.util.regex.Matcher fg = java.util.regex.Pattern
                    .compile("(\u001B\\[3\\d{1,2}(;\\d{1,2})?m)").matcher(style);
            if (fg.find()) return fg.group();
            java.util.regex.Matcher m8 = java.util.regex.Pattern
                    .compile("(\u001B\\[4)(\\d)(m)").matcher(style);
            if (m8.find()) {
                String c = "\u001B[3" + m8.group(2) + "m";
                return c.equals(AnsiColors.FG_BLACK) ? AnsiColors.FG_BRIGHT_WHITE : c;
            }
            java.util.regex.Matcher m256 = java.util.regex.Pattern
                    .compile("(\u001B\\[48;5;)(\\d+)(m)").matcher(style);
            if (m256.find()) return "\u001B[38;5;" + m256.group(2) + "m";
            return AnsiColors.FG_BRIGHT_WHITE;
        }

        /** @deprecated Use {@link #fgFromStyle(String)} */
        @Deprecated
        public static String fgFromBg(String style) { return fgFromStyle(style); }
    }

    // ── Backward-compat color constant aliases ────────────────────────────────
    // These allow existing call-sites that reference CustomLogger.FG_BRIGHT_WHITE etc.
    // to continue compiling without changes.

    public static final String ANSI_RESET          = AnsiColors.RESET;
    public static final String ANSI_BOLD           = AnsiColors.BOLD;
    public static final String ANSI_DIM            = AnsiColors.DIM;
    public static final String ANSI_ITALIC         = AnsiColors.ITALIC;
    public static final String FG_BLACK            = AnsiColors.FG_BLACK;
    public static final String FG_RED              = AnsiColors.FG_RED;
    public static final String FG_GREEN            = AnsiColors.FG_GREEN;
    public static final String FG_YELLOW           = AnsiColors.FG_YELLOW;
    public static final String FG_BLUE             = AnsiColors.FG_BLUE;
    public static final String FG_MAGENTA          = AnsiColors.FG_MAGENTA;
    public static final String FG_CYAN             = AnsiColors.FG_CYAN;
    public static final String FG_WHITE            = AnsiColors.FG_WHITE;
    public static final String FG_BRIGHT_BLACK     = AnsiColors.FG_BRIGHT_BLACK;
    public static final String FG_BRIGHT_RED       = AnsiColors.FG_BRIGHT_RED;
    public static final String FG_BRIGHT_GREEN     = AnsiColors.FG_BRIGHT_GREEN;
    public static final String FG_BRIGHT_YELLOW    = AnsiColors.FG_BRIGHT_YELLOW;
    public static final String FG_BRIGHT_BLUE      = AnsiColors.FG_BRIGHT_BLUE;
    public static final String FG_BRIGHT_MAGENTA   = AnsiColors.FG_BRIGHT_MAGENTA;
    public static final String FG_BRIGHT_CYAN      = AnsiColors.FG_BRIGHT_CYAN;
    public static final String FG_BRIGHT_WHITE     = AnsiColors.FG_BRIGHT_WHITE;
    public static final String BG_BLACK            = AnsiColors.BG_BLACK;
    public static final String BG_YELLOW           = AnsiColors.BG_YELLOW;
    public static final String BG_GREY_100         = AnsiColors.BG_GREY_100;
    public static final String FG_PURPLE           = AnsiColors.FG_PURPLE;
    public static final String FG_DARKER_PURPLE    = AnsiColors.FG_DARKER_PURPLE;
    public static final String FG_DEEP_PURPLE      = AnsiColors.FG_DEEP_PURPLE;
}

