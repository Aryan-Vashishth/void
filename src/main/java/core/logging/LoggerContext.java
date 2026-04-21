package core.logging;

import org.apache.log4j.Logger;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton holder for all mutable runtime state shared between
 * {@link CustomLogger} (configuration) and {@link LogActions} (rendering).
 *
 * <p>Extracting state here breaks the circular class dependency that would
 * otherwise exist between {@code CustomLogger} and {@code LogActions}.</p>
 */
public final class LoggerContext {

    private LoggerContext() {}

    // ── Timestamp format ──────────────────────────────────────────────────────
    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── Cell truncation ───────────────────────────────────────────────────────
    public static final int MAX_COL_WIDTH = 40;

    public static String truncateCell(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\R", " ");
        return s.length() > MAX_COL_WIDTH ? s.substring(0, MAX_COL_WIDTH - 3) + "..." : s;
    }

    // ── Log4j logger ─────────────────────────────────────────────────────────
    private static volatile Logger log = Logger.getLogger(CustomLogger.class);

    public static void initLogger(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }

    public static Logger getLogger() {
        return (log != null) ? log : Logger.getLogger(CustomLogger.class);
    }

    public static boolean isDebugEnabled() { return getLogger().isDebugEnabled(); }

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

    // ── Caller-color feature flag ─────────────────────────────────────────────
    @ConsoleOnly
    private static volatile boolean callerColorEnabled = false;

    @ConsoleOnly
    public static void enableCallerColor()       { callerColorEnabled = true;  }
    public static void disableCallerColor()      { callerColorEnabled = false; }
    public static boolean isCallerColorEnabled() { return callerColorEnabled;  }

    // ── Call-chain filter sets ────────────────────────────────────────────────
    public static final Set<String> SUPPRESS_CONTAINS =
            java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
                    "core.logging.CustomLogger", "core.logging.LogActions", "core.logging.LoggerContext",
                    "org.apache.log4j",
                    "java.", "sun.", "jdk.",
                    "com.sun.proxy", "jdk.proxy",
                    "net.bytebuddy", "reflect."
            )));

    public static final Set<String> SUPPRESS_METHOD_PREFIXES =
            java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
                    "log", "debug", "info", "warn", "error", "lambda$", "invoke"
            )));

    public static final Set<String> INCLUDE_ONLY_PREFIXES =
            java.util.Collections.synchronizedSet(new LinkedHashSet<>());

    public static void includeOnlyPackages(String... prefixes) {
        INCLUDE_ONLY_PREFIXES.clear();
        if (prefixes != null)
            for (String p : prefixes) if (p != null && !p.isBlank()) INCLUDE_ONLY_PREFIXES.add(p);
    }

    public static void suppressClassContains(String... substrings) {
        if (substrings != null)
            for (String s : substrings) if (s != null && !s.isBlank()) SUPPRESS_CONTAINS.add(s);
    }

    public static void clearIncludes() { INCLUDE_ONLY_PREFIXES.clear(); }
}

