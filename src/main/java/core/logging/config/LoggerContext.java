package core.logging.config;

import core.logging.ConsoleOnly;
import core.logging.CustomLogger;

import org.apache.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Backward-compatible facade over {@link LogConfig#current()}.
 *
 * <p>All mutable state lives in {@link LogConfig}. This class still owns the
 * Log4j {@link Logger} handle and delegates everything else.</p>
 *
 * <p>Prefer {@link LogConfig} directly in new code.</p>
 */
public final class LoggerContext {

    private LoggerContext() {}

    /** Returns {@link LogConfig#current()} — the single source of truth. */
    public static LogConfig config() { return LogConfig.current(); }

    // ── Timestamp format ──────────────────────────────────────────────────────

    /** @deprecated Read from {@link LogConfig#current()}.getTsFormat() directly. */
    @Deprecated
    public static DateTimeFormatter TS_FMT_get() { return LogConfig.current().getTsFormat(); }

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── Segment divider ───────────────────────────────────────────────────────

    public static void   setSegmentDivider(String d) { LogConfig.current().setSegmentDivider(d); }
    public static String getSegmentDivider()          { return LogConfig.current().getSegmentDivider(); }

    // ── Cell truncation ───────────────────────────────────────────────────────

    /** @deprecated Use {@link LogConfig#current()}.getTableCellLimit(). */
    @Deprecated
    public static int MAX_COL_WIDTH = LogConfig.Builder.DEFAULT_TABLE_CELL_LIMIT;

    public static String truncateCell(String s) { return LogConfig.current().truncateCell(s); }

    // ── Log4j logger ─────────────────────────────────────────────────────────

    private static volatile Logger log = Logger.getLogger(CustomLogger.class);

    public static void   initLogger(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }
    public static Logger getLogger() { return (log != null) ? log : Logger.getLogger(CustomLogger.class); }
    public static boolean isDebugEnabled() { return getLogger().isDebugEnabled(); }

    // ── ANSI / caller-color delegates ─────────────────────────────────────────

    public static void    enableAnsi()       { LogConfig.current().enableAnsi();  }
    public static void    disableAnsi()      { LogConfig.current().disableAnsi(); }
    public static boolean isAnsiEnabled()    { return LogConfig.current().isAnsiEnabled(); }

    @ConsoleOnly
    public static void    enableCallerColor()       { LogConfig.current().enableCallerColor();  }
    public static void    disableCallerColor()      { LogConfig.current().disableCallerColor(); }
    public static boolean isCallerColorEnabled()    { return LogConfig.current().isCallerColorEnabled(); }

    // ── Call-chain filter delegates ───────────────────────────────────────────

    public static Set<String> SUPPRESS_CONTAINS         = LogConfig.current().getSuppressContains();
    public static Set<String> SUPPRESS_METHOD_PREFIXES  = LogConfig.current().getSuppressMethodPrefixes();
    public static Set<String> INCLUDE_ONLY_PREFIXES     = LogConfig.current().getIncludeOnlyPrefixes();

    public static void includeOnlyPackages(String... prefixes) {
        LogConfig.current().includeOnlyPackages(prefixes);
    }
    public static void suppressClassContains(String... substrings) {
        LogConfig.current().suppressContaining(substrings);
    }
    public static void clearIncludes() { LogConfig.current().clearIncludeFilter(); }
}

