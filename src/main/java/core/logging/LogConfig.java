package core.logging;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * Central configuration object for the void-framework logging system.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>All previously scattered constants and flags — column widths, cell limits,
 * ANSI toggle, caller-color, theme, segment divider, call-chain filters — are
 * owned here as a single, coherent configuration unit.</p>
 *
 * <h2>Usage — builder (initial setup)</h2>
 * <pre>{@code
 * LogConfig.builder()
 *     .tsWidth(23).levelWidth(7).actionWidth(18)
 *     .tableCellLimit(40)
 *     .segmentDivider(" │ ")
 *     .ansi(true)
 *     .callerColor(false)
 *     .theme(LogTheme.MODERN_CLEAN)
 *     .suppressContaining("com.example.internal")
 *     .build()
 *     .apply();          // ← makes this the live config
 * }</pre>
 *
 * <h2>Usage — runtime toggle (no rebuild needed)</h2>
 * <pre>{@code
 * LogConfig.current().setAnsi(true);
 * LogConfig.current().setTheme(LogTheme.COCKPIT);
 * LogConfig.current().setTableCellLimit(60);
 * LogConfig.current().disableTableCellLimit();   // unlimited
 * }</pre>
 *
 * <h2>Usage — patch a single field via Consumer</h2>
 * <pre>{@code
 * LogConfig.patch(c -> c.setTheme(LogTheme.HIGH_CONTRAST));
 * }</pre>
 *
 * <p>{@link LoggerContext} and {@link LogActions} read exclusively from
 * {@link #current()} — they no longer hold their own state.</p>
 */
public final class LogConfig {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static volatile LogConfig CURRENT = new Builder().build();

    /** Returns the currently active configuration. Never {@code null}. */
    public static LogConfig current() { return CURRENT; }

    /**
     * Replaces the active configuration with {@code config}.
     * Thread-safe (volatile write).
     */
    public static void apply(LogConfig config) {
        if (config != null) CURRENT = config;
    }

    /**
     * Convenience: mutate the live config in-place via a {@link Consumer}.
     * <pre>{@code LogConfig.patch(c -> c.setTheme(LogTheme.COCKPIT)); }</pre>
     */
    public static void patch(Consumer<LogConfig> mutator) {
        if (mutator != null) mutator.accept(CURRENT);
    }

    // ── Column widths (for fixed-width columnar output) ───────────────────────

    private volatile int tsWidth;
    private volatile int levelWidth;
    private volatile int actionWidth;

    /** Timestamp column visible-character width. Default: {@value Builder#DEFAULT_TS_WIDTH}. */
    public int getTsWidth()     { return tsWidth; }
    /** Level column visible-character width. Default: {@value Builder#DEFAULT_LEVEL_WIDTH}. */
    public int getLevelWidth()  { return levelWidth; }
    /** Action column visible-character width. Default: {@value Builder#DEFAULT_ACTION_WIDTH}. */
    public int getActionWidth() { return actionWidth; }

    public LogConfig setTsWidth(int w)     { this.tsWidth     = Math.max(0, w); return this; }
    public LogConfig setLevelWidth(int w)  { this.levelWidth  = Math.max(0, w); return this; }
    public LogConfig setActionWidth(int w) { this.actionWidth = Math.max(0, w); return this; }

    // ── Timestamp format ──────────────────────────────────────────────────────

    private volatile DateTimeFormatter tsFormat;

    /** Default: {@code yyyy-MM-dd HH:mm:ss.SSS}. */
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

    /**
     * Visual separator printed between log segments
     * (timestamp, level, action, message, trace).
     * Default: {@code " │ "}.
     */
    public String getSegmentDivider() { return segmentDivider; }
    public LogConfig setSegmentDivider(String div) {
        this.segmentDivider = (div != null) ? div : " \u2502 ";
        return this;
    }

    // ── Trace arrow ───────────────────────────────────────────────────────────

    private volatile String traceArrow;

    /**
     * Separator between the message and the caller-trace suffix.
     * Default: {@code " → "}.
     */
    public String getTraceArrow() { return traceArrow; }
    public LogConfig setTraceArrow(String arrow) {
        this.traceArrow = (arrow != null) ? arrow : " \u2192 ";
        return this;
    }

    // ── Table cell limit ──────────────────────────────────────────────────────

    private volatile int  tableCellLimit;
    private volatile boolean tableCellLimitEnabled;

    /**
     * Maximum visible characters per table cell before truncation with {@code "..."}.
     * Only active when {@link #isTableCellLimitEnabled()} is {@code true}.
     * Default: {@value Builder#DEFAULT_TABLE_CELL_LIMIT}.
     */
    public int  getTableCellLimit()        { return tableCellLimit; }
    public boolean isTableCellLimitEnabled() { return tableCellLimitEnabled; }

    public LogConfig setTableCellLimit(int limit) {
        this.tableCellLimit        = Math.max(4, limit);
        this.tableCellLimitEnabled = true;
        return this;
    }
    /** Turn off cell truncation — table cells will display their full value. */
    public LogConfig disableTableCellLimit() { this.tableCellLimitEnabled = false; return this; }
    /** Re-enable cell truncation at the current limit. */
    public LogConfig enableTableCellLimit()  { this.tableCellLimitEnabled = true;  return this; }

    /**
     * Applies the cell limit to {@code s} if enabled; returns the original string otherwise.
     * Newlines are always collapsed to spaces.
     */
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

    /** Returns the resolved {@link ThemeColors} for the active theme or custom override. */
    public ThemeColors resolvedTheme() {
        if (customTheme != null) return customTheme;
        return BuiltInThemes.resolve(theme);
    }

    // ── Call-chain filter sets ────────────────────────────────────────────────

    private final Set<String> suppressContains;
    private final Set<String> suppressMethodPrefixes;
    private final Set<String> includeOnlyPrefixes;

    /** Class-name substrings to suppress from caller resolution. */
    public Set<String> getSuppressContains()        { return suppressContains; }
    /** Method-name prefixes to suppress from caller resolution. */
    public Set<String> getSuppressMethodPrefixes()  { return suppressMethodPrefixes; }
    /** If non-empty, only class names matching one of these prefixes are shown. */
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

    /** Makes this instance the globally active configuration and returns {@code this}. */
    public LogConfig apply() { LogConfig.apply(this); return this; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        // ── Defaults ──────────────────────────────────────────────────────────
        static final int    DEFAULT_TS_WIDTH         = 0;   // 0 = no fixed width (free-flow)
        static final int    DEFAULT_LEVEL_WIDTH       = 0;   // 0 = no fixed width
        static final int    DEFAULT_ACTION_WIDTH      = 0;   // 0 = no fixed width
        static final int    DEFAULT_TABLE_CELL_LIMIT  = 40;  // used only when limit is enabled

        private int    tsWidth               = DEFAULT_TS_WIDTH;
        private int    levelWidth            = DEFAULT_LEVEL_WIDTH;
        private int    actionWidth           = DEFAULT_ACTION_WIDTH;
        private DateTimeFormatter tsFormat   =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        private String segmentDivider        = " \u2502 ";   // " │ "
        private String traceArrow            = " \u2192 ";   // " → "
        private int    tableCellLimit        = DEFAULT_TABLE_CELL_LIMIT;
        private boolean tableCellLimitEnabled = false;  // off by default — no truncation
        private boolean ansiEnabled          = detectAnsiSupport();
        private boolean callerColorEnabled   = false;
        private LogTheme    theme            = LogTheme.PLAIN;
        private ThemeColors customTheme      = null;
        private final Set<String> suppressContains = new LinkedHashSet<>(List.of(
                "core.logging.CustomLogger", "core.logging.LogActions", "core.logging.LoggerContext",
                "core.logging.LogConfig",
                "org.apache.log4j",
                "java.", "sun.", "jdk.",
                "com.sun.proxy", "jdk.proxy",
                "net.bytebuddy", "reflect."
        ));
        private final Set<String> suppressMethodPrefixes = new LinkedHashSet<>(List.of(
                "log", "debug", "info", "warn", "error", "lambda$", "invoke"
        ));
        private final Set<String> includeOnlyPrefixes = new LinkedHashSet<>();

        // ── Column widths ─────────────────────────────────────────────────────
        public Builder tsWidth(int w)     { this.tsWidth     = Math.max(0, w); return this; }
        public Builder levelWidth(int w)  { this.levelWidth  = Math.max(0, w); return this; }
        public Builder actionWidth(int w) { this.actionWidth = Math.max(0, w); return this; }

        // ── Timestamp ─────────────────────────────────────────────────────────
        public Builder tsFormat(String pattern) {
            this.tsFormat = DateTimeFormatter.ofPattern(pattern); return this;
        }
        public Builder tsFormat(DateTimeFormatter fmt) {
            if (fmt != null) this.tsFormat = fmt; return this;
        }

        // ── Dividers ──────────────────────────────────────────────────────────
        public Builder segmentDivider(String div)  { this.segmentDivider = div; return this; }
        public Builder traceArrow(String arrow)    { this.traceArrow     = arrow; return this; }

        // ── Cell limit ────────────────────────────────────────────────────────
        public Builder tableCellLimit(int limit) {
            this.tableCellLimit = Math.max(4, limit); return this;
        }
        public Builder tableCellLimitEnabled(boolean on) {
            this.tableCellLimitEnabled = on; return this;
        }
        /** Disable cell truncation — full values always shown. */
        public Builder noTableCellLimit() { this.tableCellLimitEnabled = false; return this; }

        // ── ANSI / color ──────────────────────────────────────────────────────
        public Builder ansi(boolean enabled)        { this.ansiEnabled       = enabled; return this; }
        public Builder callerColor(boolean enabled) { this.callerColorEnabled = enabled; return this; }

        // ── Theme ─────────────────────────────────────────────────────────────
        public Builder theme(LogTheme t)            { this.theme = t; this.customTheme = null; return this; }
        public Builder customTheme(ThemeColors c)   { this.customTheme = c; return this; }

        // ── Call-chain filters ────────────────────────────────────────────────
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

        /** Build the {@link LogConfig} instance (does NOT automatically apply it). */
        public LogConfig build() { return new LogConfig(this); }

        /** Build and immediately {@link LogConfig#apply() apply} as the live config. */
        public LogConfig buildAndApply() { return build().apply(); }

        // ── ANSI auto-detection (mirrors previous LoggerContext logic) ─────────
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
