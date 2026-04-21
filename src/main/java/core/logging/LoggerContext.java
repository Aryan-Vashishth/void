package core.logging;

import org.apache.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Backward-compatible facade over {@link LogConfig#current()}.
 *
 * <p>All mutable state has been moved to {@link LogConfig}. This class now
 * delegates every read and write to the live {@link LogConfig} singleton so
 * that existing call-sites continue to compile without changes.</p>
 *
 * <p>Prefer using {@link LogConfig} directly in new code.</p>
 */
public final class LoggerContext {

    private LoggerContext() {}

    // ── Convenience accessor ──────────────────────────────────────────────────

    /** Returns {@link LogConfig#current()} — the single source of truth. */
    public static LogConfig config() { return LogConfig.current(); }

    // ── Timestamp format ──────────────────────────────────────────────────────

    /** @deprecated Read from {@link LogConfig#current()#getTsFormat()} directly. */
    public static DateTimeFormatter TS_FMT_get() { return LogConfig.current().getTsFormat(); }

    /**
     * Kept as a public constant for call-sites that reference {@code LoggerContext.TS_FMT}.
     * Reflects the default pattern; for runtime-configurable access use
     * {@link LogConfig#current()#getTsFormat()}.
     */
    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── Segment divider ───────────────────────────────────────────────────────

    public static void   setSegmentDivider(String d) { LogConfig.current().setSegmentDivider(d); }
    public static String getSegmentDivider()          { return LogConfig.current().getSegmentDivider(); }

    // ── Cell truncation ───────────────────────────────────────────────────────

    /** @deprecated Use {@link LogConfig#current()#getTableCellLimit()}. */
    public static int MAX_COL_WIDTH = LogConfig.Builder.DEFAULT_TABLE_CELL_LIMIT;

    public static String truncateCell(String s) { return LogConfig.current().truncateCell(s); }

    // ── Log4j logger ─────────────────────────────────────────────────────────

    private static volatile Logger log = Logger.getLogger(CustomLogger.class);

    public static void   initLogger(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }
    public static Logger getLogger() { return (log != null) ? log : Logger.getLogger(CustomLogger.class); }
    public static boolean isDebugEnabled() { return getLogger().isDebugEnabled(); }

    // ── ANSI support — delegates to LogConfig ────────────────────────────────

    public static void    enableAnsi()       { LogConfig.current().enableAnsi();  }
    public static void    disableAnsi()      { LogConfig.current().disableAnsi(); }
    public static boolean isAnsiEnabled()    { return LogConfig.current().isAnsiEnabled(); }

    // ── Caller-color — delegates to LogConfig ─────────────────────────────────

    @ConsoleOnly
    public static void    enableCallerColor()       { LogConfig.current().enableCallerColor();  }
    public static void    disableCallerColor()      { LogConfig.current().disableCallerColor(); }
    public static boolean isCallerColorEnabled()    { return LogConfig.current().isCallerColorEnabled(); }

    // ── Call-chain filter sets — live views from LogConfig ────────────────────

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
