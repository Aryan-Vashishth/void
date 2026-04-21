package core.logging;

import core.utils.CustomLogger;

import static core.logging.AnsiColors.*;

/**
 * Immutable value object that holds the two color axes defining a theme:
 *
 * <ol>
 *   <li><b>Level backgrounds</b> — ANSI background applied to the entire line per log level.</li>
 *   <li><b>Level foregrounds</b> — text color used for {@link LogIntent#BASE} lines.</li>
 *   <li><b>Intent foregrounds</b> — text color for each {@link LogIntent} group,
 *       combined with the current level background at render time via
 *       {@link #resolve(String, LogIntent)}.</li>
 * </ol>
 *
 * <p><b>Creating a custom theme:</b></p>
 * <pre>{@code
 * ThemeColors MY_THEME = ThemeColors.builder()
 *     .infoBg(AnsiColors.BG_GREY_100)
 *     .warnBg(AnsiColors.BG_YELLOW)
 *     ...
 *     .build();
 * }</pre>
 */
public final class ThemeColors {

    // Level backgrounds
    private final String infoBg;
    private final String warnBg;
    private final String errorBg;
    private final String debugBg;

    // Level foregrounds (BASE intent)
    private final String infoFg;
    private final String warnFg;
    private final String errorFg;
    private final String debugFg;

    // Intent foregrounds
    private final String interactionFg;
    private final String navigationFg;
    private final String observeFg;
    private final String dataFg;
    private final String successFg;
    private final String alertFg;

    /** @see ConsoleOnly */
    private final String callerFg;
    private final String reset;

    private ThemeColors(Builder b) {
        this.infoBg        = b.infoBg;
        this.warnBg        = b.warnBg;
        this.errorBg       = b.errorBg;
        this.debugBg       = b.debugBg;
        this.infoFg        = b.infoFg;
        this.warnFg        = b.warnFg;
        this.errorFg       = b.errorFg;
        this.debugFg       = b.debugFg;
        this.interactionFg = b.interactionFg;
        this.navigationFg  = b.navigationFg;
        this.observeFg     = b.observeFg;
        this.dataFg        = b.dataFg;
        this.successFg     = b.successFg;
        this.alertFg       = b.alertFg;
        this.callerFg      = b.callerFg;
        this.reset         = b.reset;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String infoBg()        { return infoBg; }
    public String warnBg()        { return warnBg; }
    public String errorBg()       { return errorBg; }
    public String debugBg()       { return debugBg; }
    public String infoFg()        { return infoFg; }
    public String warnFg()        { return warnFg; }
    public String errorFg()       { return errorFg; }
    public String debugFg()       { return debugFg; }
    public String interactionFg() { return interactionFg; }
    public String navigationFg()  { return navigationFg; }
    public String observeFg()     { return observeFg; }
    public String dataFg()        { return dataFg; }
    public String successFg()     { return successFg; }
    public String alertFg()       { return alertFg; }
    /** @see ConsoleOnly */
    public String callerFg()      { return callerFg; }
    public String reset()         { return reset; }

    /**
     * Composes a ready-to-use ANSI style string for the given log level and intent.
     *
     * <p>Formula: {@code intentFg + levelBg}. For {@link LogIntent#BASE} the intent
     * foreground is the level-specific foreground ({@code infoFg}, {@code warnFg}, …).</p>
     *
     * @param logLevel the current log level string ("INFO", "WARN", "ERROR", "DEBUG")
     * @param intent   the semantic intent of the log line
     * @return a combined ANSI style string ready for prepending to log output
     */
    public String resolve(String logLevel, LogIntent intent) {
        String bg = switch (logLevel) {
            case "ERROR" -> errorBg;
            case "WARN"  -> warnBg;
            case "DEBUG" -> debugBg;
            default      -> infoBg;
        };
        String fg = switch (intent) {
            case BASE        -> switch (logLevel) {
                case "ERROR" -> errorFg;
                case "WARN"  -> warnFg;
                case "DEBUG" -> debugFg;
                default      -> infoFg;
            };
            case INTERACTION -> interactionFg;
            case NAVIGATION  -> navigationFg;
            case OBSERVE     -> observeFg;
            case DATA        -> dataFg;
            case SUCCESS     -> successFg;
            case ALERT       -> alertFg;
        };
        return fg + bg;
    }

    /** Entry-point for the fluent builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * @deprecated Use {@link #builder()} instead.
     */
    @Deprecated
    public static Builder theme() { return builder(); }

    // ── Fluent Builder ────────────────────────────────────────────────────────
    public static final class Builder {

        // Level backgrounds
        private String infoBg  = BG_GREY_100;
        private String warnBg  = BG_YELLOW;
        private String errorBg = BG_DARKER_RED;
        private String debugBg = BG_DARKER_GREY;

        // Level foregrounds (BASE intent)
        private String infoFg  = FG_BRIGHT_WHITE + BOLD;
        private String warnFg  = FG_BLACK        + BOLD;
        private String errorFg = FG_BRIGHT_WHITE + BOLD;
        private String debugFg = FG_BRIGHT_WHITE + BOLD;

        // Intent foregrounds
        private String interactionFg = FG_BRIGHT_WHITE  + BOLD;
        private String navigationFg  = FG_BRIGHT_CYAN   + BOLD;
        private String observeFg     = FG_BRIGHT_YELLOW + BOLD;
        private String dataFg        = FG_BRIGHT_WHITE  + BOLD;
        private String successFg     = FG_BRIGHT_GREEN  + BOLD;
        private String alertFg       = FG_BRIGHT_RED    + BOLD;

        private String callerFg = FG_DIM_WHITE;
        private String reset    = RESET;

        private Builder() {}

        // Level backgrounds
        public Builder infoBg(String bg)  { this.infoBg  = bg; return this; }
        public Builder warnBg(String bg)  { this.warnBg  = bg; return this; }
        public Builder errorBg(String bg) { this.errorBg = bg; return this; }
        public Builder debugBg(String bg) { this.debugBg = bg; return this; }

        // Level foregrounds (BASE intent)
        public Builder infoFg(String fg)  { this.infoFg  = fg; return this; }
        public Builder warnFg(String fg)  { this.warnFg  = fg; return this; }
        public Builder errorFg(String fg) { this.errorFg = fg; return this; }
        public Builder debugFg(String fg) { this.debugFg = fg; return this; }

        // Intent foregrounds
        public Builder interactionFg(String fg) { this.interactionFg = fg; return this; }
        public Builder navigationFg(String fg)  { this.navigationFg  = fg; return this; }
        public Builder observeFg(String fg)     { this.observeFg     = fg; return this; }
        public Builder dataFg(String fg)        { this.dataFg        = fg; return this; }
        public Builder successFg(String fg)     { this.successFg     = fg; return this; }
        public Builder alertFg(String fg)       { this.alertFg       = fg; return this; }

        /**
         * ANSI foreground applied to the caller/callee suffix.
         * <p>Only used when {@link CustomLogger#enableCallerColor()} is active
         * ({@link ConsoleOnly}).</p>
         */
        public Builder callerFg(String fg)  { this.callerFg = fg; return this; }
        public Builder reset(String seq)    { this.reset    = seq; return this; }

        /** Builds and returns the immutable {@link ThemeColors} instance. */
        public ThemeColors build() { return new ThemeColors(this); }
    }
}

