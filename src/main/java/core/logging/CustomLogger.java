package core.logging;

import core.logging.ansi.AnsiColors;
import core.logging.config.LogConfig;
import core.logging.config.LoggerContext;
import core.logging.intent.LogIntent;
import core.logging.render.LogActions;
import core.logging.theme.BuiltInThemes;
import core.logging.theme.LogTheme;
import core.logging.theme.ThemeColors;

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
 *   <li>{@link core.logging.ansi.AnsiEscape}      — ANSI escape factory</li>
 *   <li>{@link core.logging.ansi.AnsiColors}      — named color constants</li>
 *   <li>{@link LogIntent}                         — semantic intent enum</li>
 *   <li>{@link LogTheme}                          — theme selection enum</li>
 *   <li>{@link ThemeColors}                       — immutable theme model + builder</li>
 *   <li>{@link BuiltInThemes}                     — built-in themes + active-theme registry</li>
 *   <li>{@link LogActions}                        — action methods (click, table, success, …)</li>
 *   <li>{@link LoggerContext}                     — Log4j logger holder + filter delegates</li>
 *   <li>{@link LogConfig}                         — single-source-of-truth runtime config</li>
 *   <li>{@link CustomLogger}                      — (this class) global config + level instances</li>
 * </ul>
 */
public class CustomLogger {

    // ── Level instances ───────────────────────────────────────────────────────

    public static final Debug debug = new Debug();
    public static final Info  info  = new Info();
    public static final Warn  warn  = new Warn();
    public static final Error error = new Error();

    // ── Level classes ─────────────────────────────────────────────────────────

    public static class Debug extends LogActions {
        public Debug() { super("DEBUG"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "DEBUG", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "DEBUG"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

    public static class Info extends LogActions {
        public Info() { super("INFO"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "INFO", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "INFO"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

    public static class Warn extends LogActions {
        public Warn() { super("WARN"); }

        @Override public void log(String message)                        { logMessage(LogIntent.BASE, "WARN", message); }
        @Override public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "WARN"); }
        @Override public void log(String heading, Object... pairs)       { log(heading, fields(pairs)); }
    }

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

    public static void configure(LogConfig config)            { LogConfig.apply(config); }
    public static void configure(Consumer<LogConfig> mutator) { LogConfig.patch(mutator); }
    public static LogConfig config()                          { return LogConfig.current(); }

    // ── Log4j logger ──────────────────────────────────────────────────────────

    public static void initialize(Class<?> clazz) { LoggerContext.initLogger(clazz); }
    public static Logger getSafeLogger()           { return LoggerContext.getLogger(); }
    public static boolean isDebugEnabled()         { return LoggerContext.isDebugEnabled(); }

    // ── ANSI ──────────────────────────────────────────────────────────────────

    public static void enableAnsi()       { LoggerContext.enableAnsi(); }
    public static void disableAnsi()      { LoggerContext.disableAnsi(); }
    public static boolean isAnsiEnabled() { return LoggerContext.isAnsiEnabled(); }

    // ── Theme selection ───────────────────────────────────────────────────────

    public static void setTheme(LogTheme theme)           { BuiltInThemes.setTheme(theme); }
    public static void setCustomTheme(ThemeColors colors) { BuiltInThemes.setCustomTheme(colors); }
    public static LogTheme getCurrentTheme()              { return BuiltInThemes.getCurrentTheme(); }

    // ── Caller-color feature flag ─────────────────────────────────────────────

    @ConsoleOnly
    public static void enableCallerColor()       { LoggerContext.enableCallerColor(); }
    public static void disableCallerColor()      { LoggerContext.disableCallerColor(); }
    public static boolean isCallerColorEnabled() { return LoggerContext.isCallerColorEnabled(); }

    // ── Call-chain filtering ──────────────────────────────────────────────────

    public static final Set<String> SUPPRESS_CONTAINS         = LoggerContext.SUPPRESS_CONTAINS;
    public static final Set<String> SUPPRESS_METHOD_PREFIXES  = LoggerContext.SUPPRESS_METHOD_PREFIXES;
    public static final Set<String> INCLUDE_ONLY_PREFIXES     = LoggerContext.INCLUDE_ONLY_PREFIXES;

    public static void includeOnlyPackages(String... prefixes)   { LoggerContext.includeOnlyPackages(prefixes); }
    public static void suppressClassContains(String... substrings){ LoggerContext.suppressClassContains(substrings); }
    public static void clearIncludes()                           { LoggerContext.clearIncludes(); }

    // ── Experimental utilities ────────────────────────────────────────────────

    public static final class Experimental {
        private Experimental() {}

        public static String stripAnsi(String str) {
            return str == null ? "" : str.replaceAll("\\u001B\\[[;\\d]*m", "");
        }

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
    // ⚠ Deprecated: import core.logging.ansi.AnsiColors.* directly in new code.

    /** @deprecated Use {@link AnsiColors#RESET}. */          @Deprecated public static final String ANSI_RESET          = AnsiColors.RESET;
    /** @deprecated Use {@link AnsiColors#BOLD}. */           @Deprecated public static final String ANSI_BOLD           = AnsiColors.BOLD;
    /** @deprecated Use {@link AnsiColors#DIM}. */            @Deprecated public static final String ANSI_DIM            = AnsiColors.DIM;
    /** @deprecated Use {@link AnsiColors#ITALIC}. */         @Deprecated public static final String ANSI_ITALIC         = AnsiColors.ITALIC;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BLACK            = AnsiColors.FG_BLACK;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_RED              = AnsiColors.FG_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_GREEN            = AnsiColors.FG_GREEN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_YELLOW           = AnsiColors.FG_YELLOW;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BLUE             = AnsiColors.FG_BLUE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_MAGENTA          = AnsiColors.FG_MAGENTA;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_CYAN             = AnsiColors.FG_CYAN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_WHITE            = AnsiColors.FG_WHITE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_BLACK     = AnsiColors.FG_BRIGHT_BLACK;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_RED       = AnsiColors.FG_BRIGHT_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_GREEN     = AnsiColors.FG_BRIGHT_GREEN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_YELLOW    = AnsiColors.FG_BRIGHT_YELLOW;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_BLUE      = AnsiColors.FG_BRIGHT_BLUE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_MAGENTA   = AnsiColors.FG_BRIGHT_MAGENTA;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_CYAN      = AnsiColors.FG_BRIGHT_CYAN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_BRIGHT_WHITE     = AnsiColors.FG_BRIGHT_WHITE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BLACK            = AnsiColors.BG_BLACK;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_RED              = AnsiColors.BG_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_GREEN            = AnsiColors.BG_GREEN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_YELLOW           = AnsiColors.BG_YELLOW;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BLUE             = AnsiColors.BG_BLUE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_MAGENTA          = AnsiColors.BG_MAGENTA;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_CYAN             = AnsiColors.BG_CYAN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_WHITE            = AnsiColors.BG_WHITE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_BLACK     = AnsiColors.BG_BRIGHT_BLACK;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_RED       = AnsiColors.BG_BRIGHT_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_GREEN     = AnsiColors.BG_BRIGHT_GREEN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_YELLOW    = AnsiColors.BG_BRIGHT_YELLOW;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_BLUE      = AnsiColors.BG_BRIGHT_BLUE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_MAGENTA   = AnsiColors.BG_BRIGHT_MAGENTA;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_CYAN      = AnsiColors.BG_BRIGHT_CYAN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_WHITE     = AnsiColors.BG_BRIGHT_WHITE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_BRIGHT_GREY      = AnsiColors.BG_BRIGHT_GREY;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_GREY_100         = AnsiColors.BG_GREY_100;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_ORANGE_208       = AnsiColors.BG_ORANGE_208;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_GREY      = AnsiColors.BG_DARKER_GREY;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_BLUE      = AnsiColors.BG_DARKER_BLUE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_GREEN     = AnsiColors.BG_DARKER_GREEN;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_MAGENTA   = AnsiColors.BG_DARKER_MAGENTA;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_YELLOW    = AnsiColors.BG_DARKER_YELLOW;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_DARKER_RED       = AnsiColors.BG_DARKER_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String BG_MAROON_RED       = AnsiColors.BG_MAROON_RED;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_PURPLE           = AnsiColors.FG_PURPLE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_DARKER_PURPLE    = AnsiColors.FG_DARKER_PURPLE;
    /** @deprecated Use {@link AnsiColors}. */                @Deprecated public static final String FG_DEEP_PURPLE      = AnsiColors.FG_DEEP_PURPLE;
}

