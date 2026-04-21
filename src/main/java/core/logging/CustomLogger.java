package core.logging;

import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
 *   <li>{@link LoggerContext} — shared runtime state (ANSI flag, logger, filters)</li>
 *   <li>{@link CustomLogger}  — (this class) global config + level instances</li>
 * </ul>
 */
public class CustomLogger {

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
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

    /** INFO-level logger. */
    public static class Info extends LogActions {
        public Info() { super("INFO"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "INFO", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "INFO"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

    /** WARN-level logger. */
    public static class Warn extends LogActions {
        public Warn() { super("WARN"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "WARN", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "WARN"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

    /** ERROR-level logger. */
    public static class Error extends LogActions {
        public Error() { super("ERROR"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "ERROR", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "ERROR"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
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

    // ── LogConfig entry points ────────────────────────────────────────────────

    /**
     * Apply a fully constructed {@link LogConfig} as the live configuration.
     * <pre>{@code
     * CustomLogger.configure(
     *     LogConfig.builder()
     *         .theme(LogTheme.COCKPIT)
     *         .tableCellLimit(60)
     *         .callerColor(true)
     *         .build()
     * );
     * }</pre>
     */
    public static void configure(LogConfig config)            { LogConfig.apply(config); }

    /**
     * Patch the live {@link LogConfig} in-place via a {@link Consumer}.
     * <pre>{@code
     * CustomLogger.configure(c -> c.setTheme(LogTheme.HIGH_CONTRAST).enableCallerColor());
     * }</pre>
     */
    public static void configure(Consumer<LogConfig> mutator) { LogConfig.patch(mutator); }

    /** Returns the live {@link LogConfig} instance for direct manipulation. */
    public static LogConfig config()                          { return LogConfig.current(); }

    // ── Log4j logger — delegates to LoggerContext ─────────────────────────────

    public static void initialize(Class<?> clazz) { LoggerContext.initLogger(clazz); }
    public static Logger getSafeLogger()           { return LoggerContext.getLogger(); }
    public static boolean isDebugEnabled()         { return LoggerContext.isDebugEnabled(); }

    // ── ANSI support — delegates to LoggerContext ─────────────────────────────

    public static void enableAnsi()       { LoggerContext.enableAnsi(); }
    public static void disableAnsi()      { LoggerContext.disableAnsi(); }
    public static boolean isAnsiEnabled() { return LoggerContext.isAnsiEnabled(); }

    // ── Theme selection — delegates to BuiltInThemes ──────────────────────────

    public static void setTheme(LogTheme theme)           { BuiltInThemes.setTheme(theme); }
    public static void setCustomTheme(ThemeColors colors) { BuiltInThemes.setCustomTheme(colors); }
    public static LogTheme getCurrentTheme()              { return BuiltInThemes.getCurrentTheme(); }

    // ── Caller-color feature flag — delegates to LoggerContext ────────────────

    @ConsoleOnly
    public static void enableCallerColor()       { LoggerContext.enableCallerColor(); }
    public static void disableCallerColor()      { LoggerContext.disableCallerColor(); }
    public static boolean isCallerColorEnabled() { return LoggerContext.isCallerColorEnabled(); }

    // ── Call-chain filtering — delegates to LoggerContext ─────────────────────

    public static final Set<String> SUPPRESS_CONTAINS         = LoggerContext.SUPPRESS_CONTAINS;
    public static final Set<String> SUPPRESS_METHOD_PREFIXES  = LoggerContext.SUPPRESS_METHOD_PREFIXES;
    public static final Set<String> INCLUDE_ONLY_PREFIXES     = LoggerContext.INCLUDE_ONLY_PREFIXES;

    public static void includeOnlyPackages(String... prefixes)   { LoggerContext.includeOnlyPackages(prefixes); }
    public static void suppressClassContains(String... substrings){ LoggerContext.suppressClassContains(substrings); }
    public static void clearIncludes()                           { LoggerContext.clearIncludes(); }

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
