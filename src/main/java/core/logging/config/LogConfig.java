package core.logging.config;

import core.logging.ConsoleOnly;
import core.logging.theme.BuiltInThemes;
import core.logging.theme.LogTheme;
import core.logging.theme.ThemeColors;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Central configuration object for the void-framework logging system.
 *
 * <p>All previously scattered constants and flags — column widths, cell limits,
 * ANSI toggle, caller-color, theme, segment divider, call-chain filters — are
 * owned here as a single, coherent configuration unit.</p>
 */
public final class LogConfig {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile LogConfig CURRENT = new Builder().build();

    public static LogConfig current() { return CURRENT; }

    public static void apply(LogConfig config) {
        if (config != null) CURRENT = config;
    }

    public static void patch(Consumer<LogConfig> mutator) {
        if (mutator != null) mutator.accept(CURRENT);
    }

    // ── Column widths ─────────────────────────────────────────────────────────

    private volatile int tsWidth;
    private volatile int levelWidth;
    private volatile int actionWidth;

    public int getTsWidth()     { return tsWidth; }
    public int getLevelWidth()  { return levelWidth; }
    public int getActionWidth() { return actionWidth; }

    public LogConfig setTsWidth(int w)     { this.tsWidth     = Math.max(0, w); return this; }
    public LogConfig setLevelWidth(int w)  { this.levelWidth  = Math.max(0, w); return this; }
    public LogConfig setActionWidth(int w) { this.actionWidth = Math.max(0, w); return this; }

    // ── Timestamp format ──────────────────────────────────────────────────────

    private volatile DateTimeFormatter tsFormat;

    public DateTimeFormatter getTsFormat() { return tsFormat; }
    public LogConfig setTsFormat(String pattern) {
        this.tsFormat = DateTimeFormatter.ofPattern(pattern);
        return this;
    }
    public LogConfig setTsFormat(DateTimeFormatter fmt) {
        if (fmt != null) this.tsFormat = fmt;
        return this;
    }

    // ── Segment divider ───────────────────────────────────────────────────────

    private volatile String segmentDivider;

    public String getSegmentDivider() { return segmentDivider; }
    public LogConfig setSegmentDivider(String div) {
        this.segmentDivider = (div != null) ? div : " \u2502 ";
        return this;
    }

    // ── Trace arrow ───────────────────────────────────────────────────────────

    private volatile String traceArrow;

    public String getTraceArrow() { return traceArrow; }
    public LogConfig setTraceArrow(String arrow) {
        this.traceArrow = (arrow != null) ? arrow : " \u2192 ";
        return this;
    }

    // ── Table cell limit ──────────────────────────────────────────────────────

    private volatile int  tableCellLimit;
    private volatile boolean tableCellLimitEnabled;

    public int  getTableCellLimit()        { return tableCellLimit; }
    public boolean isTableCellLimitEnabled() { return tableCellLimitEnabled; }

    public LogConfig setTableCellLimit(int limit) {
        this.tableCellLimit        = Math.max(4, limit);
        this.tableCellLimitEnabled = true;
        return this;
    }
    public LogConfig disableTableCellLimit() { this.tableCellLimitEnabled = false; return this; }
    public LogConfig enableTableCellLimit()  { this.tableCellLimitEnabled = true;  return this; }

    public String truncateCell(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\R", " ");
        if (!tableCellLimitEnabled || s.length() <= tableCellLimit) return s;
        return s.substring(0, tableCellLimit - 3) + "...";
    }

    // ── ANSI output ───────────────────────────────────────────────────────────

    private volatile boolean ansiEnabled;

    public boolean isAnsiEnabled() { return ansiEnabled; }
    public LogConfig setAnsi(boolean enabled) { this.ansiEnabled = enabled; return this; }
    public LogConfig enableAnsi()  { return setAnsi(true);  }
    public LogConfig disableAnsi() { return setAnsi(false); }

    // ── Caller-color feature flag ─────────────────────────────────────────────

    @ConsoleOnly
    private volatile boolean callerColorEnabled;

    public boolean isCallerColorEnabled()       { return callerColorEnabled; }
    public LogConfig setCallerColor(boolean on) { this.callerColorEnabled = on; return this; }
    @ConsoleOnly
    public LogConfig enableCallerColor()        { return setCallerColor(true);  }
    public LogConfig disableCallerColor()       { return setCallerColor(false); }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private volatile LogTheme    theme;
    private volatile ThemeColors customTheme;

    public LogTheme    getTheme()       { return theme; }
    public ThemeColors getCustomTheme() { return customTheme; }

    public LogConfig setTheme(LogTheme theme) {
        this.theme       = theme;
        this.customTheme = null;
        return this;
    }
    public LogConfig setCustomTheme(ThemeColors colors) {
        this.customTheme = colors;
        return this;
    }

    public ThemeColors resolvedTheme() {
        if (customTheme != null) return customTheme;
        return BuiltInThemes.resolve(theme);
    }

    // ── Call-chain filter sets ────────────────────────────────────────────────

    private final Set<String> suppressContains;
    private final Set<String> suppressMethodPrefixes;
    private final Set<String> includeOnlyPrefixes;

    public Set<String> getSuppressContains()        { return suppressContains; }
    public Set<String> getSuppressMethodPrefixes()  { return suppressMethodPrefixes; }
    public Set<String> getIncludeOnlyPrefixes()     { return includeOnlyPrefixes; }

    public LogConfig suppressContaining(String... substrings) {
        if (substrings != null)
            for (String s : substrings) if (s != null && !s.isBlank()) suppressContains.add(s);
        return this;
    }
    public LogConfig suppressMethodPrefix(String... prefixes) {
        if (prefixes != null)
            for (String p : prefixes) if (p != null && !p.isBlank()) suppressMethodPrefixes.add(p);
        return this;
    }
    public LogConfig includeOnlyPackages(String... prefixes) {
        includeOnlyPrefixes.clear();
        if (prefixes != null)
            for (String p : prefixes) if (p != null && !p.isBlank()) includeOnlyPrefixes.add(p);
        return this;
    }
    public LogConfig clearIncludeFilter() { includeOnlyPrefixes.clear(); return this; }

    // ── Private constructor (builder only) ────────────────────────────────────

    private LogConfig(Builder b) {
        this.tsWidth               = b.tsWidth;
        this.levelWidth            = b.levelWidth;
        this.actionWidth           = b.actionWidth;
        this.tsFormat              = b.tsFormat;
        this.segmentDivider        = b.segmentDivider;
        this.traceArrow            = b.traceArrow;
        this.tableCellLimit        = b.tableCellLimit;
        this.tableCellLimitEnabled = b.tableCellLimitEnabled;
        this.ansiEnabled           = b.ansiEnabled;
        this.callerColorEnabled    = b.callerColorEnabled;
        this.theme                 = b.theme;
        this.customTheme           = b.customTheme;
        this.suppressContains      = Collections.synchronizedSet(new LinkedHashSet<>(b.suppressContains));
        this.suppressMethodPrefixes= Collections.synchronizedSet(new LinkedHashSet<>(b.suppressMethodPrefixes));
        this.includeOnlyPrefixes   = Collections.synchronizedSet(new LinkedHashSet<>(b.includeOnlyPrefixes));
    }

    public LogConfig apply() { LogConfig.apply(this); return this; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        public static final int    DEFAULT_TS_WIDTH         = 0;
        public static final int    DEFAULT_LEVEL_WIDTH      = 0;
        public static final int    DEFAULT_ACTION_WIDTH     = 0;
        public static final int    DEFAULT_TABLE_CELL_LIMIT = 40;

        private int    tsWidth               = DEFAULT_TS_WIDTH;
        private int    levelWidth            = DEFAULT_LEVEL_WIDTH;
        private int    actionWidth           = DEFAULT_ACTION_WIDTH;
        private DateTimeFormatter tsFormat   =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        private String segmentDivider        = " \u2502 ";   // " │ "
        private String traceArrow            = " \u2192 ";   // " → "
        private int    tableCellLimit        = DEFAULT_TABLE_CELL_LIMIT;
        private boolean tableCellLimitEnabled = false;
        private boolean ansiEnabled          = detectAnsiSupport();
        private boolean callerColorEnabled   = false;
        private LogTheme    theme            = LogTheme.PLAIN;
        private ThemeColors customTheme      = null;
        private final Set<String> suppressContains = new LinkedHashSet<>(List.of(
                "core.logging.CustomLogger",
                "core.logging.render.LogActions",
                "core.logging.config.LoggerContext",
                "core.logging.config.LogConfig",
                "org.apache.log4j",
                "java.", "sun.", "jdk.",
                "com.sun.proxy", "jdk.proxy",
                "net.bytebuddy", "reflect."
        ));
        private final Set<String> suppressMethodPrefixes = new LinkedHashSet<>(List.of(
                "log", "debug", "info", "warn", "error", "lambda$", "invoke"
        ));
        private final Set<String> includeOnlyPrefixes = new LinkedHashSet<>();

        public Builder tsWidth(int w)     { this.tsWidth     = Math.max(0, w); return this; }
        public Builder levelWidth(int w)  { this.levelWidth  = Math.max(0, w); return this; }
        public Builder actionWidth(int w) { this.actionWidth = Math.max(0, w); return this; }

        public Builder tsFormat(String pattern) {
            this.tsFormat = DateTimeFormatter.ofPattern(pattern); return this;
        }
        public Builder tsFormat(DateTimeFormatter fmt) {
            if (fmt != null) this.tsFormat = fmt; return this;
        }

        public Builder segmentDivider(String div)  { this.segmentDivider = div; return this; }
        public Builder traceArrow(String arrow)    { this.traceArrow     = arrow; return this; }

        public Builder tableCellLimit(int limit) {
            this.tableCellLimit = Math.max(4, limit); return this;
        }
        public Builder tableCellLimitEnabled(boolean on) {
            this.tableCellLimitEnabled = on; return this;
        }
        public Builder noTableCellLimit() { this.tableCellLimitEnabled = false; return this; }

        public Builder ansi(boolean enabled)        { this.ansiEnabled       = enabled; return this; }
        public Builder callerColor(boolean enabled) { this.callerColorEnabled = enabled; return this; }

        public Builder theme(LogTheme t)            { this.theme = t; this.customTheme = null; return this; }
        public Builder customTheme(ThemeColors c)   { this.customTheme = c; return this; }

        public Builder suppressContaining(String... substrings) {
            if (substrings != null)
                for (String s : substrings) if (s != null && !s.isBlank()) suppressContains.add(s);
            return this;
        }
        public Builder suppressMethodPrefix(String... prefixes) {
            if (prefixes != null)
                for (String p : prefixes) if (p != null && !p.isBlank()) suppressMethodPrefixes.add(p);
            return this;
        }
        public Builder includeOnlyPackages(String... prefixes) {
            includeOnlyPrefixes.clear();
            if (prefixes != null)
                for (String p : prefixes) if (p != null && !p.isBlank()) includeOnlyPrefixes.add(p);
            return this;
        }

        public LogConfig build() { return new LogConfig(this); }

        public LogConfig buildAndApply() { return build().apply(); }

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
    }
}

