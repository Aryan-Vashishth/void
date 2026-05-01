package core.logging.config;

import core.logging.ConsoleOnly;
import core.logging.CustomLogger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

    /** Returns {@link LogConfig#current()} â€” the single source of truth. */
    public static LogConfig config() { return LogConfig.current(); }

    // â”€â”€ Timestamp format â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // â”€â”€ Segment divider â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void   setSegmentDivider(String d) { LogConfig.current().setSegmentDivider(d); }
    public static String getSegmentDivider()          { return LogConfig.current().getSegmentDivider(); }

    // â”€â”€ Cell truncation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static String truncateCell(String s) { return LogConfig.current().truncateCell(s); }

    // â”€â”€ Log4j logger â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static volatile Logger log = LogManager.getLogger(CustomLogger.class);

    public static void   initLogger(Class<?> clazz) {
        log = (clazz != null) ? LogManager.getLogger(clazz) : LogManager.getLogger(CustomLogger.class);
    }
    public static Logger getLogger() { return (log != null) ? log : LogManager.getLogger(CustomLogger.class); }
    public static boolean isDebugEnabled() { return getLogger().isDebugEnabled(); }

    // â”€â”€ ANSI / caller-color delegates â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void    enableAnsi()       { LogConfig.current().enableAnsi();  }
    public static void    disableAnsi()      { LogConfig.current().disableAnsi(); }
    public static boolean isAnsiEnabled()    { return LogConfig.current().isAnsiEnabled(); }

    @ConsoleOnly
    public static void    enableCallerColor()       { LogConfig.current().enableCallerColor();  }
    public static void    disableCallerColor()      { LogConfig.current().disableCallerColor(); }
    public static boolean isCallerColorEnabled()    { return LogConfig.current().isCallerColorEnabled(); }

    // â”€â”€ Call-chain filter delegates â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

