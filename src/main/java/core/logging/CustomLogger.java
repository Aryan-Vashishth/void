package core.logging;

import org.apache.log4j.Logger;

import java.lang.annotation.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class CustomLogger {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int MAX_COL_WIDTH = 40;

    private static String truncateCell(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\R", " ");
        return s.length() > MAX_COL_WIDTH ? s.substring(0, MAX_COL_WIDTH - 3) + "..." : s;
    }

    public static boolean isDebugEnabled() { return getSafeLogger().isDebugEnabled(); }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANSI COLOR CONSTANTS
    //   Standard:   \u001B[<code>m
    //   256-color:  \u001B[38;5;<n>m  (FG)   \u001B[48;5;<n>m  (BG)
    //   True RGB:   \u001B[38;2;R;G;Bm (FG)   \u001B[48;2;R;G;Bm (BG)
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Control sequences ─────────────────────────────────────────────────────
    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_BOLD   = "\u001B[1m";
    public static final String ANSI_DIM    = "\u001B[2m";
    public static final String ANSI_ITALIC = "\u001B[3m";

    // ── Legacy 16-color foregrounds (kept for backward compatibility) ──────────
    public static final String FG_BLACK          = "\u001B[30m";
    public static final String FG_RED            = "\u001B[31m";
    public static final String FG_GREEN          = "\u001B[32m";
    public static final String FG_YELLOW         = "\u001B[33m";
    public static final String FG_BLUE           = "\u001B[34m";
    public static final String FG_MAGENTA        = "\u001B[35m";
    public static final String FG_CYAN           = "\u001B[36m";
    public static final String FG_WHITE          = "\u001B[37m";
    public static final String FG_BRIGHT_BLACK   = "\u001B[90m";
    public static final String FG_BRIGHT_RED     = "\u001B[91m";
    public static final String FG_BRIGHT_GREEN   = "\u001B[92m";
    public static final String FG_BRIGHT_YELLOW  = "\u001B[93m";
    public static final String FG_BRIGHT_BLUE    = "\u001B[94m";
    public static final String FG_BRIGHT_MAGENTA = "\u001B[95m";
    public static final String FG_BRIGHT_CYAN    = "\u001B[96m";
    public static final String FG_BRIGHT_WHITE   = "\u001B[97m";
    /** @deprecated Use {@link #FG_BRIGHT_BLACK} */
    public static final String FG_DIM_WHITE      = "\u001B[37;2m";

    // ── Legacy 16-color backgrounds (kept for backward compatibility) ──────────
    public static final String BG_BLACK          = "\u001B[40m";
    public static final String BG_RED            = "\u001B[41m";
    public static final String BG_GREEN          = "\u001B[42m";
    public static final String BG_YELLOW         = "\u001B[43m";
    public static final String BG_BLUE           = "\u001B[44m";
    public static final String BG_MAGENTA        = "\u001B[45m";
    public static final String BG_CYAN           = "\u001B[46m";
    public static final String BG_WHITE          = "\u001B[47m";
    public static final String BG_BRIGHT_BLACK   = "\u001B[100m";
    public static final String BG_BRIGHT_RED     = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN   = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW  = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE    = "\u001B[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN    = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE   = "\u001B[107m";
    // aliases kept for source compatibility
    public static final String BG_GREY_100       = BG_BRIGHT_BLACK;
    public static final String BG_BRIGHT_GREY    = BG_WHITE;

    // ── 256-color foregrounds (\u001B[38;5;Nm) ────────────────────────────────
    public static final String FG_256_ORANGE     = "\u001B[38;5;208m";  // xterm orange
    public static final String FG_256_GOLD       = "\u001B[38;5;220m";  // xterm gold
    public static final String FG_256_LIME       = "\u001B[38;5;118m";  // bright lime
    public static final String FG_256_SKY        = "\u001B[38;5;117m";  // sky blue
    public static final String FG_256_VIOLET     = "\u001B[38;5;135m";  // violet
    public static final String FG_256_PINK       = "\u001B[38;5;205m";  // hot pink
    public static final String FG_256_TEAL       = "\u001B[38;5;80m";   // teal
    public static final String FG_256_SALMON     = "\u001B[38;5;209m";  // salmon
    public static final String FG_256_GREY_DARK  = "\u001B[38;5;240m";  // dark grey
    public static final String FG_256_GREY_MID   = "\u001B[38;5;246m";  // mid grey
    public static final String FG_256_GREY_LIGHT = "\u001B[38;5;252m";  // light grey
    public static final String FG_256_NAVY       = "\u001B[38;5;17m";   // navy blue
    public static final String FG_256_MAROON     = "\u001B[38;5;88m";   // maroon
    public static final String FG_256_OLIVE      = "\u001B[38;5;100m";  // olive
    public static final String FG_256_INDIGO     = "\u001B[38;5;54m";   // indigo

    // 256-color aliases (backward compat)
    public static final String FG_ORANGE_208      = FG_256_ORANGE;
    public static final String FG_BOLD_ORANGE_208 = "\u001B[38;5;208;1m";
    public static final String FG_NAVY_BLUE       = FG_256_NAVY;

    // ── 256-color backgrounds (\u001B[48;5;Nm) ───────────────────────────────
    public static final String BG_256_ORANGE      = "\u001B[48;5;208m";
    public static final String BG_256_DARK_GREY   = "\u001B[48;5;235m"; // very dark grey
    public static final String BG_256_MID_GREY    = "\u001B[48;5;238m"; // mid dark grey
    public static final String BG_256_NAVY        = "\u001B[48;5;17m";
    public static final String BG_256_DARK_GREEN  = "\u001B[48;5;22m";
    public static final String BG_256_DARK_TEAL   = "\u001B[48;5;23m";
    public static final String BG_256_DARK_PURPLE = "\u001B[48;5;53m";
    public static final String BG_256_DARK_OLIVE  = "\u001B[48;5;94m";
    public static final String BG_256_DARK_RED    = "\u001B[48;5;52m";
    public static final String BG_256_MAROON      = "\u001B[48;5;88m";
    public static final String BG_256_INDIGO      = "\u001B[48;5;54m";

    // 256-color backward-compat aliases
    public static final String BG_ORANGE_208     = BG_256_ORANGE;
    public static final String BG_DARKER_GREY    = BG_256_DARK_GREY;
    public static final String BG_DARKER_BLUE    = BG_256_NAVY;
    public static final String BG_DARKER_GREEN   = BG_256_DARK_GREEN;
    public static final String BG_DARKER_MAGENTA = BG_256_DARK_PURPLE;
    public static final String BG_DARKER_YELLOW  = BG_256_DARK_OLIVE;
    public static final String BG_DARKER_RED     = BG_256_DARK_RED;
    public static final String BG_MAROON_RED     = BG_256_MAROON;

    // ── True-RGB foregrounds (\u001B[38;2;R;G;Bm) ────────────────────────────
    // Neutrals
    public static final String RGB_FG_SNOW_WHITE    = "\u001B[38;2;240;242;245m"; // near-white
    public static final String RGB_FG_SOFT_WHITE    = "\u001B[38;2;210;215;220m"; // muted white
    public static final String RGB_FG_COOL_GREY     = "\u001B[38;2;140;150;165m"; // calm grey
    public static final String RGB_FG_WARM_GREY     = "\u001B[38;2;160;158;150m"; // warm grey
    public static final String RGB_FG_DEEP_CHARCOAL = "\u001B[38;2;28;30;38m";    // near-black fg
    // Warm
    public static final String RGB_FG_GOLD          = "\u001B[38;2;255;200;50m";  // rich gold
    public static final String RGB_FG_AMBER         = "\u001B[38;2;255;170;0m";   // deep amber
    public static final String RGB_FG_PEACH         = "\u001B[38;2;255;185;110m"; // soft peach
    public static final String RGB_FG_CORAL         = "\u001B[38;2;255;105;85m";  // coral red
    public static final String RGB_FG_SALMON        = "\u001B[38;2;255;140;105m"; // salmon
    public static final String RGB_FG_HOT_PINK      = "\u001B[38;2;255;75;170m";  // hot pink
    // Cool
    public static final String RGB_FG_SKY_BLUE      = "\u001B[38;2;80;185;255m";  // sky blue
    public static final String RGB_FG_ELECTRIC_BLUE = "\u001B[38;2;50;140;255m";  // electric blue
    public static final String RGB_FG_STEEL_CYAN    = "\u001B[38;2;90;220;220m";  // steel cyan
    public static final String RGB_FG_MINT          = "\u001B[38;2;60;220;175m";  // mint green
    public static final String RGB_FG_LIME_GREEN    = "\u001B[38;2;100;240;120m"; // lime green
    public static final String RGB_FG_NEON_GREEN    = "\u001B[38;2;80;255;120m";  // neon green
    // Purple family
    public static final String RGB_FG_LAVENDER      = "\u001B[38;2;185;155;255m"; // soft lavender
    public static final String RGB_FG_VIOLET        = "\u001B[38;2;160;100;255m"; // vivid violet
    public static final String RGB_FG_PURPLE        = "\u001B[38;2;155;111;224m"; // standard purple
    public static final String RGB_FG_DARKER_PURPLE = "\u001B[38;2;122;79;196m";  // darker purple
    public static final String RGB_FG_DEEP_PURPLE   = "\u001B[38;2;90;54;163m";   // deep purple
    // Aliases for backward compat
    public static final String FG_PURPLE            = RGB_FG_PURPLE;
    public static final String FG_DARKER_PURPLE     = RGB_FG_DARKER_PURPLE;
    public static final String FG_DEEP_PURPLE       = RGB_FG_DEEP_PURPLE;

    // ── True-RGB backgrounds (\u001B[48;2;R;G;Bm) ────────────────────────────
    // Dark neutrals
    public static final String RGB_BG_CHARCOAL      = "\u001B[48;2;35;38;46m";   // dark slate
    public static final String RGB_BG_DARK_SLATE    = "\u001B[48;2;28;32;42m";   // deeper slate
    public static final String RGB_BG_MIDNIGHT      = "\u001B[48;2;16;18;26m";   // near-black
    public static final String RGB_BG_NEAR_BLACK    = "\u001B[48;2;20;22;30m";   // used for debug
    public static final String RGB_BG_CARBON        = "\u001B[48;2;28;28;32m";   // carbon fibre
    public static final String RGB_BG_STEEL_DARK    = "\u001B[48;2;48;52;62m";   // steel grey
    // Warm dark
    public static final String RGB_BG_DARK_AMBER    = "\u001B[48;2;95;62;0m";    // dark amber
    public static final String RGB_BG_VIVID_AMBER   = "\u001B[48;2;170;105;0m";  // vivid amber (warn, light)
    public static final String RGB_BG_RUST          = "\u001B[48;2;110;48;20m";  // burnt rust
    public static final String RGB_BG_CRIMSON       = "\u001B[48;2;100;20;25m";  // dark crimson
    public static final String RGB_BG_DARK_WINE     = "\u001B[48;2;90;16;36m";   // deep wine
    public static final String RGB_BG_MAROON        = "\u001B[48;2;110;22;22m";  // maroon
    public static final String RGB_BG_ORANGE_VIVID  = "\u001B[48;2;200;100;0m";  // vivid orange
    // Cool dark
    public static final String RGB_BG_DARK_FOREST   = "\u001B[48;2;18;52;28m";   // dark forest green
    public static final String RGB_BG_DARK_OCEAN    = "\u001B[48;2;14;38;72m";   // dark ocean blue
    public static final String RGB_BG_DARK_TEAL     = "\u001B[48;2;14;66;66m";   // deep teal
    public static final String RGB_BG_DARK_INDIGO   = "\u001B[48;2;28;24;68m";   // dark indigo
    public static final String RGB_BG_DARK_PURPLE   = "\u001B[48;2;45;22;72m";   // dark purple
    public static final String RGB_BG_DARK_CYAN     = "\u001B[48;2;0;80;100m";   // dark cyan
    // Bright / light (INDUSTRIAL)
    public static final String RGB_BG_SOFT_WHITE    = "\u001B[48;2;238;240;245m"; // off-white
    public static final String RGB_BG_LIGHT_GREY    = "\u001B[48;2;205;210;218m"; // light grey
    public static final String RGB_BG_WARM_AMBER_LT = "\u001B[48;2;190;140;15m";  // warm amber light
    public static final String RGB_BG_TOMATO_RED    = "\u001B[48;2;190;45;45m";   // tomato red
    // Vivid (NIGHT_CLUB)
    public static final String RGB_BG_HOT_PINK      = "\u001B[48;2;195;38;115m";  // hot pink
    public static final String RGB_BG_NEON_PURPLE   = "\u001B[48;2;115;18;175m";  // neon purple
    public static final String RGB_BG_ELECTRIC_TEAL = "\u001B[48;2;0;150;170m";   // electric teal

    // ═══════════════════════════════════════════════════════════════════════════
    // END OF COLOR CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════

    // ── @ConsoleOnly annotation ───────────────────────────────────────────────
    /**
     * Marks a flag or method that is <strong>only safe when output goes to a live
     * ANSI-capable terminal</strong>.
     *
     * <p>Enabling a {@code @ConsoleOnly} feature breaks the <em>one ANSI block per
     * line</em> contract that keeps IntelliJ Test History, CI logs, and file appenders
     * clean. You may see duplicate/split entries, raw escape codes, or garbled output.</p>
     *
     * <p><b>Always call the corresponding {@code disable*()} method before CI / file runs.</b></p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Documented
    public @interface ConsoleOnly {}

    // ── LogTheme ──────────────────────────────────────────────────────────────
    public enum LogTheme {
        // ── Practical / everyday ──────────────────────────────────────────────
        /** ★ Default. Standard 16-color ANSI. Renders correctly in every terminal,
         *  CI runner, and file viewer. Boring on purpose — zero surprises. */
        PLAIN,
        /** Ethan Schoonover's Solarized Dark palette. Reduced eye-strain for long
         *  automation sessions; excellent readability on dark terminals. */
        SOLARIZED_DARK,
        /** Maximum contrast — pure blacks + maximum-luminance foregrounds.
         *  Ideal for accessibility or low-quality display/projector use. */
        HIGH_CONTRAST,
        // ── Stylised ─────────────────────────────────────────────────────────
        INDUSTRIAL_STEEL, NIGHT_CLUB, CARBON_ORANGE, MODERN_CLEAN, COCKPIT
    }
    // ── LogIntent ─────────────────────────────────────────────────────────────
    /**
     * Classifies <em>what</em> a log line is communicating — independently of the
     * log level. The theme maps each intent to a <b>foreground color</b>; the
     * log level supplies the <b>background color</b>. The two are composed at
     * render time: {@code intentFg + levelBg}.
     *
     * <table border="1">
     * <tr><th>Intent</th><th>Actions</th></tr>
     * <tr><td>BASE</td><td>log(), info/warn/error/debug label lines</td></tr>
     * <tr><td>INTERACTION</td><td>click, checkbox, text, input, dropdown, toggle, upload</td></tr>
     * <tr><td>NAVIGATION</td><td>tab, frame, breadcrumb</td></tr>
     * <tr><td>OBSERVE</td><td>wait, search, result</td></tr>
     * <tr><td>DATA</td><td>table, grid, row</td></tr>
     * <tr><td>SUCCESS</td><td>success, complete, resolved</td></tr>
     * <tr><td>ALERT</td><td>error[x], failed, timeout, validation, fallback, skip</td></tr>
     * </table>
     */
    public enum LogIntent {
        /** Plain log/label lines — text color matches the log level. */
        BASE,
        /** User interaction: click, checkbox, text, input, dropdown, toggle, upload. */
        INTERACTION,
        /** Page navigation: tab, frame, breadcrumb. */
        NAVIGATION,
        /** Observing / reading: wait, search, result. */
        OBSERVE,
        /** Structured data: table, grid, row. */
        DATA,
        /** Positive outcome: success, complete, resolved. */
        SUCCESS,
        /** Negative / warning signals: error[x], failed, timeout, validation, fallback, skip. */
        ALERT
    }

    // ── ThemeColors ───────────────────────────────────────────────────────────
    /**
     * Holds the two color axes that define a theme:
     *
     * <ol>
     *   <li><b>Level backgrounds</b> ({@code infoBg}, {@code warnBg}, {@code errorBg},
     *       {@code debugBg}) — the ANSI background applied to the <em>entire</em> line.</li>
     *   <li><b>Level foregrounds</b> ({@code infoFg} … {@code debugFg}) — text color used
     *       for {@link LogIntent#BASE} lines (plain {@code .log()} calls).</li>
     *   <li><b>Intent foregrounds</b> ({@code interactionFg}, {@code navigationFg}, …) —
     *       text color for each {@link LogIntent} group, combined with the current level
     *       background at render time via {@link #resolve(String, LogIntent)}.</li>
     * </ol>
     *
     * <p><b>Creating a custom theme:</b></p>
     * <pre>{@code
     * ThemeColors MY_THEME = ThemeColors.theme()
     *     // ── Level backgrounds ─────────────────────────────
     *     .infoBg    (BG_GREY_100)
     *     .warnBg    (BG_YELLOW)
     *     .errorBg   (BG_DARKER_RED)
     *     .debugBg   (BG_DARKER_GREY)
     *     // ── Level text (BASE intent) ──────────────────────
     *     .infoFg    (FG_BRIGHT_WHITE + ANSI_BOLD)
     *     .warnFg    (FG_BLACK        + ANSI_BOLD)
     *     .errorFg   (FG_BRIGHT_WHITE + ANSI_BOLD)
     *     .debugFg   (FG_BRIGHT_WHITE + ANSI_BOLD)
     *     // ── Intent foregrounds ────────────────────────────
     *     .interactionFg (FG_BRIGHT_WHITE  + ANSI_BOLD) // click, input, …
     *     .navigationFg  (FG_BRIGHT_CYAN   + ANSI_BOLD) // tab, frame, …
     *     .observeFg     (FG_BRIGHT_YELLOW + ANSI_BOLD) // wait, search, …
     *     .dataFg        (FG_BRIGHT_WHITE  + ANSI_BOLD) // table, grid, …
     *     .successFg     (FG_BRIGHT_GREEN  + ANSI_BOLD) // success, complete, …
     *     .alertFg       (FG_BRIGHT_RED    + ANSI_BOLD) // error, failed, …
     *     // ── @ConsoleOnly (caller suffix color) ───────────
     *     .callerFg  (FG_DIM_WHITE)
     *     .build();
     * }</pre>
     *
     * <p><b>Contrast tip:</b> intent foregrounds are composited on top of all level
     * backgrounds. Choose FG values that remain readable on every level BG you define,
     * or accept that rare combinations (e.g. {@code warn.click()}) may be sub-optimal.</p>
     */
    public static final class ThemeColors {

        // Level backgrounds — one per log level
        private final String infoBg;
        private final String warnBg;
        private final String errorBg;
        private final String debugBg;

        // Level foregrounds — used for BASE intent (plain .log() calls)
        private final String infoFg;
        private final String warnFg;
        private final String errorFg;
        private final String debugFg;

        // Intent foregrounds — combined with level BG at render time
        private final String interactionFg; // INTERACTION group
        private final String navigationFg;  // NAVIGATION group
        private final String observeFg;     // OBSERVE group
        private final String dataFg;        // DATA group
        private final String successFg;     // SUCCESS group
        private final String alertFg;       // ALERT group

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

        // ── Accessors ────────────────────────────────────────────────────────
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
        public static Builder theme() { return new Builder(); }

        // ── Fluent Builder ───────────────────────────────────────────────────
        public static final class Builder {

            // Level backgrounds
            private String infoBg  = BG_GREY_100;
            private String warnBg  = BG_YELLOW;
            private String errorBg = BG_DARKER_RED;
            private String debugBg = BG_DARKER_GREY;

            // Level foregrounds (BASE intent)
            private String infoFg  = FG_BRIGHT_WHITE + ANSI_BOLD;
            private String warnFg  = FG_BLACK        + ANSI_BOLD;
            private String errorFg = FG_BRIGHT_WHITE + ANSI_BOLD;
            private String debugFg = FG_BRIGHT_WHITE + ANSI_BOLD;

            // Intent foregrounds
            private String interactionFg = FG_BRIGHT_WHITE  + ANSI_BOLD;
            private String navigationFg  = FG_BRIGHT_CYAN   + ANSI_BOLD;
            private String observeFg     = FG_BRIGHT_YELLOW + ANSI_BOLD;
            private String dataFg        = FG_BRIGHT_WHITE  + ANSI_BOLD;
            private String successFg     = FG_BRIGHT_GREEN  + ANSI_BOLD;
            private String alertFg       = FG_BRIGHT_RED    + ANSI_BOLD;

            private String callerFg = FG_DIM_WHITE;
            private String reset    = ANSI_RESET;

            private Builder() {}

            // ── Level backgrounds ─────────────────────────────────────────────
            /** Background color for the entire INFO log line. */
            public Builder infoBg(String bg)  { this.infoBg  = bg; return this; }
            /** Background color for the entire WARN log line. */
            public Builder warnBg(String bg)  { this.warnBg  = bg; return this; }
            /** Background color for the entire ERROR log line. */
            public Builder errorBg(String bg) { this.errorBg = bg; return this; }
            /** Background color for the entire DEBUG log line. */
            public Builder debugBg(String bg) { this.debugBg = bg; return this; }

            // ── Level foregrounds (BASE intent — plain .log() calls) ──────────
            /** Text color for INFO label lines ({@link LogIntent#BASE}). */
            public Builder infoFg(String fg)  { this.infoFg  = fg; return this; }
            /** Text color for WARN label lines ({@link LogIntent#BASE}). */
            public Builder warnFg(String fg)  { this.warnFg  = fg; return this; }
            /** Text color for ERROR label lines ({@link LogIntent#BASE}). */
            public Builder errorFg(String fg) { this.errorFg = fg; return this; }
            /** Text color for DEBUG label lines ({@link LogIntent#BASE}). */
            public Builder debugFg(String fg) { this.debugFg = fg; return this; }

            // ── Intent foregrounds (combined with levelBg at render time) ─────
            /**
             * Text color for {@link LogIntent#INTERACTION} lines —
             * click, checkbox, text, input, dropdown, toggle, upload.
             */
            public Builder interactionFg(String fg) { this.interactionFg = fg; return this; }

            /**
             * Text color for {@link LogIntent#NAVIGATION} lines —
             * tab, frame, breadcrumb.
             */
            public Builder navigationFg(String fg)  { this.navigationFg  = fg; return this; }

            /**
             * Text color for {@link LogIntent#OBSERVE} lines —
             * wait, search, result.
             */
            public Builder observeFg(String fg)     { this.observeFg     = fg; return this; }

            /**
             * Text color for {@link LogIntent#DATA} lines —
             * table, grid, row.
             */
            public Builder dataFg(String fg)        { this.dataFg        = fg; return this; }

            /**
             * Text color for {@link LogIntent#SUCCESS} lines —
             * success, complete, resolved.
             */
            public Builder successFg(String fg)     { this.successFg     = fg; return this; }

            /**
             * Text color for {@link LogIntent#ALERT} lines —
             * error[x], failed, timeout, validation, fallback, skip.
             */
            public Builder alertFg(String fg)       { this.alertFg       = fg; return this; }

            /**
             * ANSI foreground applied to the caller/callee suffix.
             * <p>Only used when {@link CustomLogger#enableCallerColor()} is active
             * ({@link ConsoleOnly}).</p>
             */
            public Builder callerFg(String fg)      { this.callerFg      = fg; return this; }

            /** ANSI reset sequence (default: {@link CustomLogger#ANSI_RESET}). */
            public Builder reset(String seq)        { this.reset         = seq; return this; }

            /** Builds and returns the immutable {@link ThemeColors} instance. */
            public ThemeColors build() { return new ThemeColors(this); }
        }
    }

    // ── Built-in themes ──────────────────────────────────────────────────────
    // All themes use True-RGB (24-bit) for backgrounds and key foregrounds so
    // every hue is exactly what was intended — no terminal palette surprises.
    //
    // Level BG  = background of the entire log line (one per log level).
    // Intent FG = text color composited on top of the active level BG.
    // Contrast rule: minimum WCAG 3:1 between every FG+BG combination used.

    // ── MODERN_CLEAN ─────────────────────────────────────────────────────────
    // Personality : polished dark IDE (VS Code Dark+); neutral slate backgrounds;
    //               a full 6-hue intent rainbow that pops on every level BG.
    private static final ThemeColors MODERN_CLEAN = ThemeColors.theme()
            // Level backgrounds — dark slate family; each level is a distinct shade
            .infoBg  (RGB_BG_CHARCOAL)    // rgb(35,38,46)  — cool dark slate        ██
            .warnBg  (RGB_BG_DARK_AMBER)  // rgb(95,62,0)   — deep amber             ██
            .errorBg (RGB_BG_CRIMSON)     // rgb(100,20,25) — dark crimson           ██
            .debugBg (RGB_BG_MIDNIGHT)    // rgb(16,18,26)  — near-black             ██
            // Level FGs (BASE intent) — high-contrast text per background
            .infoFg  (RGB_FG_SNOW_WHITE  + ANSI_BOLD)  // 240,242,245 on slate       ✅
            .warnFg  (RGB_FG_GOLD        + ANSI_BOLD)  // 255,200,50  on dark amber  ✅
            .errorFg (RGB_FG_CORAL       + ANSI_BOLD)  // 255,105,85  on crimson     ✅
            .debugFg (RGB_FG_COOL_GREY   + ANSI_BOLD)  // 140,150,165 on midnight    ✅
            // Intent FGs — six vivid hues readable on all four dark backgrounds
            .interactionFg (RGB_FG_SNOW_WHITE  + ANSI_BOLD)  // ● WHITE   click/input   ✅
            .navigationFg  (RGB_FG_STEEL_CYAN  + ANSI_BOLD)  // ● CYAN    tab/frame     ✅
            .observeFg     (RGB_FG_GOLD        + ANSI_BOLD)  // ● GOLD    wait/search   ✅
            .dataFg        (RGB_FG_LAVENDER    + ANSI_BOLD)  // ● LAVNDR  table/grid    ✅
            .successFg     (RGB_FG_LIME_GREEN  + ANSI_BOLD)  // ● GREEN   success       ✅
            .alertFg       (RGB_FG_CORAL       + ANSI_BOLD)  // ● CORAL   error/fail    ✅
            .callerFg      (RGB_FG_COOL_GREY)               // dim suffix  @ConsoleOnly ⚠️
            .build();

    // ── COCKPIT ──────────────────────────────────────────────────────────────
    // Personality: mission-control dashboard; semantic status-light BGs per level
    //              (green=nominal, amber=caution, maroon=alert, black=diagnostic);
    //              magenta DATA is the visual "signature" of this theme.
    private static final ThemeColors COCKPIT = ThemeColors.theme()
            // Level BGs — four vivid status colors; readable from across the room
            .infoBg  (RGB_BG_DARK_FOREST)  // rgb(18,52,28)  — forest green  "go"      ██
            .warnBg  (RGB_BG_DARK_AMBER)   // rgb(95,62,0)   — dark amber    "caution" ██
            .errorBg (RGB_BG_DARK_WINE)    // rgb(90,16,36)  — deep wine     "alert"   ██
            .debugBg (RGB_BG_NEAR_BLACK)   // rgb(20,22,30)  — near-black    "diag"    ██
            // Level FGs (BASE) — one notch dimmer than intent FGs so actions pop
            .infoFg  (RGB_FG_LIME_GREEN  + ANSI_BOLD)  // bright lime  on forest green ✅
            .warnFg  (RGB_FG_GOLD        + ANSI_BOLD)  // rich gold    on dark amber   ✅
            .errorFg (RGB_FG_SNOW_WHITE  + ANSI_BOLD)  // stark white  on wine         ✅
            .debugFg (RGB_FG_COOL_GREY)                // muted grey   on near-black   ✅
            // Intent FGs — mission-control rainbow; every slot unique
            .interactionFg (RGB_FG_SNOW_WHITE  + ANSI_BOLD)  // ● WHITE   click/input       ✅
            .navigationFg  (RGB_FG_MINT        + ANSI_BOLD)  // ● MINT    tab/frame          ✅
            .observeFg     (RGB_FG_GOLD        + ANSI_BOLD)  // ● GOLD    wait/search/result ✅
            .dataFg        (RGB_FG_LAVENDER    + ANSI_BOLD)  // ● LAVNDR  table/grid (sign.) ✅
            .successFg     (RGB_FG_LIME_GREEN  + ANSI_BOLD)  // ● GREEN   success/complete   ✅
            .alertFg       (RGB_FG_CORAL       + ANSI_BOLD)  // ● CORAL   error/timeout      ✅
            .callerFg      (RGB_FG_COOL_GREY)               // dim suffix  @ConsoleOnly      ⚠️
            .build();

    // ── INDUSTRIAL_STEEL ─────────────────────────────────────────────────────
    // Personality: heavy-industry HMI / factory panel.
    //   INFO  = brushed steel plate — medium dark grey, white stencil lettering
    //   WARN  = safety caution stripe — deep safety yellow, black stencil text
    //   ERROR = danger zone — blood red panel, stark white alarm text
    //   DEBUG = coal / soot channel — near-black, dim readout text
    //
    // Intent palette borrows from industrial indicator lights:
    //   INTERACTION = white panel label      NAVIGATION = instrument blue
    //   OBSERVE     = amber caution lamp     DATA       = CRT phosphor green
    //   SUCCESS     = go-signal green        ALERT      = danger red lamp
    private static final ThemeColors INDUSTRIAL_STEEL = ThemeColors.theme()
            // Level BGs
            .infoBg  ("\u001B[48;2;55;60;70m")   // brushed steel     rgb(55,60,70)  ██
            .warnBg  ("\u001B[48;2;175;135;0m")  // safety yellow     rgb(175,135,0) ██
            .errorBg ("\u001B[48;2;155;22;22m")  // danger red        rgb(155,22,22) ██
            .debugBg ("\u001B[48;2;20;22;28m")   // coal / soot       rgb(20,22,28)  ██
            // Level FGs (BASE) — stencil text per panel
            .infoFg  (RGB_FG_SNOW_WHITE    + ANSI_BOLD)             // white stencil on steel   ✅
            .warnFg  (RGB_FG_DEEP_CHARCOAL + ANSI_BOLD)             // black stencil on yellow  ✅
            .errorFg (RGB_FG_SNOW_WHITE    + ANSI_BOLD)             // white alarm on red       ✅
            .debugFg ("\u001B[38;2;110;118;130m")                   // dim readout on coal      ✅
            // Intent FGs — industrial indicator-light palette, bright enough on all 4 BGs
            .interactionFg (RGB_FG_SNOW_WHITE              + ANSI_BOLD) // ● WHITE  panel label   ✅
            .navigationFg  ("\u001B[38;2;80;195;255m"      + ANSI_BOLD) // ● BLUE   instrument    ✅
            .observeFg     ("\u001B[38;2;255;200;40m"      + ANSI_BOLD) // ● AMBER  caution lamp  ✅
            .dataFg        ("\u001B[38;2;60;235;160m"      + ANSI_BOLD) // ● PHOSPHR CRT readout  ✅
            .successFg     ("\u001B[38;2;50;220;90m"       + ANSI_BOLD) // ● GREEN  go-signal     ✅
            .alertFg       ("\u001B[38;2;255;75;55m"       + ANSI_BOLD) // ● RED    danger lamp   ✅
            .callerFg      ("\u001B[38;2;110;118;130m")                 // dim suffix @ConsoleOnly ⚠️
            .build();

    // ── NIGHT_CLUB (DISCO) ────────────────────────────────────────────────────
    // Personality: vivid neon club lighting; all backgrounds are saturated jewel
    //              tones; foregrounds are neon-bright to cut through.
    private static final ThemeColors DISCO = ThemeColors.theme()
            // Level BGs — jewel-tone saturated backgrounds
            .infoBg  (RGB_BG_DARK_TEAL)    // rgb(14,66,66)    — deep teal    ██
            .warnBg  (RGB_BG_HOT_PINK)     // rgb(195,38,115)  — hot pink     ██
            .errorBg (RGB_BG_NEON_PURPLE)  // rgb(115,18,175)  — neon purple  ██
            .debugBg (RGB_BG_DARK_INDIGO)  // rgb(28,24,68)    — dark indigo  ██
            // Level FGs (BASE) — max-luminance per BG
            .infoFg  (RGB_FG_MINT      + ANSI_BOLD)  // mint       on teal    ✅
            .warnFg  (RGB_FG_SNOW_WHITE + ANSI_BOLD) // snow       on pink    ✅
            .errorFg (RGB_FG_SNOW_WHITE + ANSI_BOLD) // snow       on purple  ✅
            .debugFg (RGB_FG_LAVENDER)               // lavender   on indigo  ✅
            // Intent FGs — neon rainbow; every hue pops on every BG above
            .interactionFg (RGB_FG_HOT_PINK     + ANSI_BOLD)  // ● PINK   click/input   ✅
            .navigationFg  (RGB_FG_SKY_BLUE     + ANSI_BOLD)  // ● SKY    tab/frame     ✅
            .observeFg     (RGB_FG_NEON_GREEN   + ANSI_BOLD)  // ● NEON   wait/search   ✅
            .dataFg        (RGB_FG_GOLD         + ANSI_BOLD)  // ● GOLD   table/grid    ✅
            .successFg     (RGB_FG_NEON_GREEN   + ANSI_BOLD)  // ● NEON   success       ✅
            .alertFg       (RGB_FG_CORAL        + ANSI_BOLD)  // ● CORAL  error/fail    ✅
            .callerFg      (RGB_FG_COOL_GREY)               // @ConsoleOnly            ⚠️
            .build();

    // ── CARBON_ORANGE ─────────────────────────────────────────────────────────
    // Personality: carbon-fibre dark base with a hot amber/orange accent system;
    //              all backgrounds are very dark so the orange glows cleanly.
    private static final ThemeColors CARBON_ORANGE = ThemeColors.theme()
            // Level BGs — carbon-black family + vivid orange for WARN
            .infoBg  (RGB_BG_CARBON)       // rgb(28,28,32)   — carbon fibre  ██
            .warnBg  (RGB_BG_ORANGE_VIVID) // rgb(200,100,0)  — vivid orange  ██
            .errorBg (RGB_BG_CRIMSON)      // rgb(100,20,25)  — dark crimson  ██
            .debugBg (RGB_BG_MIDNIGHT)     // rgb(16,18,26)   — midnight      ██
            // Level FGs (BASE) — amber glow on dark; charcoal on bright orange
            .infoFg  (RGB_FG_AMBER      + ANSI_BOLD)  // amber     on carbon  ✅
            .warnFg  (RGB_FG_DEEP_CHARCOAL + ANSI_BOLD)// charcoal  on orange  ✅
            .errorFg (RGB_FG_PEACH      + ANSI_BOLD)  // peach     on crimson ✅
            .debugFg (RGB_FG_WARM_GREY)               // warm grey on midnight ✅
            // Intent FGs — amber-anchored palette; each slot clearly differentiated
            .interactionFg (RGB_FG_AMBER      + ANSI_BOLD)  // ● AMBER  click/input  ✅
            .navigationFg  (RGB_FG_STEEL_CYAN + ANSI_BOLD)  // ● CYAN   tab/frame    ✅
            .observeFg     (RGB_FG_PEACH      + ANSI_BOLD)  // ● PEACH  wait/search  ✅
            .dataFg        (RGB_FG_LAVENDER   + ANSI_BOLD)  // ● LAVNDR table/grid   ✅
            .successFg     (RGB_FG_LIME_GREEN + ANSI_BOLD)  // ● GREEN  success      ✅
            .alertFg       (RGB_FG_CORAL      + ANSI_BOLD)  // ● CORAL  error/fail   ✅
            .callerFg      (RGB_FG_WARM_GREY)               // @ConsoleOnly          ⚠️
            .build();

    // ── PLAIN ─────────────────────────────────────────────────────────────────
    // Personality: boring on purpose — the most practical theme.
    //   Same hue family as classic 16-color ANSI but with darker, richer RGB
    //   backgrounds so the bright-bold foreground colors pop out clearly.
    //
    //   INFO  = deep steel grey  rgb(42,46,58)   + bright white text
    //   WARN  = deep amber       rgb(148,108,0)  + black text          (safety yellow, richer)
    //   ERROR = deep crimson     rgb(155,16,16)  + bright white text   (alarm red, richer)
    //   DEBUG = near-black       rgb(10,10,14)   + white text          (quietest channel)
    //
    // ★ This is the default theme.
    private static final ThemeColors PLAIN = ThemeColors.theme()
            // Level BGs — darker RGB shades of the classic 16-color hue family
            .infoBg  ("\u001B[48;2;42;46;58m")    // deep steel grey  rgb(42,46,58)   ██
            .warnBg  ("\u001B[48;2;148;108;0m")   // deep amber       rgb(148,108,0)  ██
            .errorBg ("\u001B[48;2;155;16;16m")   // deep crimson     rgb(155,16,16)  ██
            .debugBg ("\u001B[48;2;10;10;14m")    // near-black       rgb(10,10,14)   ██
            // Level FGs (BASE) — maximum contrast per background
            .infoFg  (FG_BRIGHT_WHITE  + ANSI_BOLD)   // white  on steel grey  ✅
            .warnFg  (FG_BLACK         + ANSI_BOLD)   // black  on deep amber  ✅
            .errorFg (FG_BRIGHT_WHITE  + ANSI_BOLD)   // white  on crimson     ✅
            .debugFg (FG_WHITE         + ANSI_BOLD)   // white  on near-black  ✅
            // Intent FGs — classic 16-color palette; pop strongly on the darker BGs above
            .interactionFg (FG_BRIGHT_WHITE  + ANSI_BOLD)  // ● WHITE   click/input   ✅
            .navigationFg  (FG_BRIGHT_CYAN   + ANSI_BOLD)  // ● CYAN    tab/frame     ✅
            .observeFg     (FG_BRIGHT_YELLOW + ANSI_BOLD)  // ● YELLOW  wait/search   ✅
            .dataFg        (FG_BRIGHT_MAGENTA+ ANSI_BOLD)  // ● MAGENTA table/grid    ✅
            .successFg     (FG_BRIGHT_GREEN  + ANSI_BOLD)  // ● GREEN   success       ✅
            .alertFg       (FG_BRIGHT_RED    + ANSI_BOLD)  // ● RED     error/fail    ✅
            .callerFg      (FG_BRIGHT_BLACK)                // dim suffix @ConsoleOnly ⚠️
            .build();

    // ── SOLARIZED_DARK ────────────────────────────────────────────────────────
    // Personality: Ethan Schoonover's Solarized Dark — reduced eye-strain;
    //   carefully balanced hues with a warm dark blue-green base.
    //   Exact Solarized palette RGB values used for backgrounds and key accents.
    //
    //   INFO  = base03 bg (main)   WARN  = yellow-tinted dark
    //   ERROR = red-tinted dark    DEBUG = deepest base03
    private static final ThemeColors SOLARIZED_DARK = ThemeColors.theme()
            // Level BGs — Solarized Dark base tones (True RGB)
            .infoBg  ("\u001B[48;2;7;54;66m")    // base02  rgb(7,54,66)    ██
            .warnBg  ("\u001B[48;2;101;74;0m")   // yellow  darkened        ██
            .errorBg ("\u001B[48;2;88;20;18m")   // red     darkened        ██
            .debugBg ("\u001B[48;2;0;43;54m")    // base03  rgb(0,43,54)    ██
            // Level FGs (BASE) — Solarized accent colors
            .infoFg  ("\u001B[38;2;147;161;161m" + ANSI_BOLD)  // base1   #93a1a1 ✅
            .warnFg  ("\u001B[38;2;181;137;0m"   + ANSI_BOLD)  // yellow  #b58900 ✅
            .errorFg ("\u001B[38;2;220;50;47m"   + ANSI_BOLD)  // red     #dc322f ✅
            .debugFg ("\u001B[38;2;101;123;131m")               // base00  #657b83 ✅
            // Intent FGs — Solarized accent rainbow
            .interactionFg ("\u001B[38;2;131;148;150m" + ANSI_BOLD)  // ● base0  #839496   ✅
            .navigationFg  ("\u001B[38;2;38;139;210m"  + ANSI_BOLD)  // ● blue   #268bd2   ✅
            .observeFg     ("\u001B[38;2;181;137;0m"   + ANSI_BOLD)  // ● yellow #b58900   ✅
            .dataFg        ("\u001B[38;2;108;113;196m" + ANSI_BOLD)  // ● violet #6c71c4   ✅
            .successFg     ("\u001B[38;2;133;153;0m"   + ANSI_BOLD)  // ● green  #859900   ✅
            .alertFg       ("\u001B[38;2;203;75;22m"   + ANSI_BOLD)  // ● orange #cb4b16   ✅
            .callerFg      ("\u001B[38;2;88;110;117m")               // base01  @ConsoleOnly ⚠️
            .build();

    // ── HIGH_CONTRAST ─────────────────────────────────────────────────────────
    // Personality: pure-black backgrounds + maximum-luminance foregrounds.
    //   Designed for accessibility (WCAG AA+), projector use, and low-quality
    //   displays.  No subtle shading — every element is immediately obvious.
    //
    //   All backgrounds are pure black; all text is bold maximum-saturation color.
    private static final ThemeColors HIGH_CONTRAST_THEME = ThemeColors.theme()
            // Level BGs — pure black for all levels; level is communicated by FG only
            .infoBg  (BG_BLACK)  // ██
            .warnBg  (BG_BLACK)  // ██
            .errorBg (BG_BLACK)  // ██
            .debugBg (BG_BLACK)  // ██
            // Level FGs (BASE) — saturated bold labels
            .infoFg  (FG_BRIGHT_WHITE  + ANSI_BOLD)   // pure white    on black ✅
            .warnFg  (FG_BRIGHT_YELLOW + ANSI_BOLD)   // bright yellow on black ✅
            .errorFg (FG_BRIGHT_RED    + ANSI_BOLD)   // bright red    on black ✅
            .debugFg (FG_BRIGHT_BLACK  + ANSI_BOLD)   // bright grey   on black ✅
            // Intent FGs — max-contrast intent labels
            .interactionFg (FG_BRIGHT_WHITE   + ANSI_BOLD)  // ● WHITE    click/input  ✅
            .navigationFg  (FG_BRIGHT_CYAN    + ANSI_BOLD)  // ● CYAN     tab/frame    ✅
            .observeFg     (FG_BRIGHT_YELLOW  + ANSI_BOLD)  // ● YELLOW   wait/search  ✅
            .dataFg        (FG_BRIGHT_MAGENTA + ANSI_BOLD)  // ● MAGENTA  table/grid   ✅
            .successFg     (FG_BRIGHT_GREEN   + ANSI_BOLD)  // ● GREEN    success      ✅
            .alertFg       (FG_BRIGHT_RED     + ANSI_BOLD)  // ● RED      error/fail   ✅
            .callerFg      (FG_BRIGHT_BLACK)                 // grey suffix @ConsoleOnly ⚠️
            .build();

    // ── Active theme ─────────────────────────────────────────────────────────

    private static LogTheme currentTheme = LogTheme.PLAIN;

    public static void setTheme(LogTheme theme) { currentTheme = theme; }

    private static ThemeColors getColors() {
        return switch (currentTheme) {
            case PLAIN            -> PLAIN;
            case SOLARIZED_DARK   -> SOLARIZED_DARK;
            case HIGH_CONTRAST    -> HIGH_CONTRAST_THEME;
            case MODERN_CLEAN     -> MODERN_CLEAN;
            case INDUSTRIAL_STEEL -> INDUSTRIAL_STEEL;
            case NIGHT_CLUB       -> DISCO;
            case CARBON_ORANGE    -> CARBON_ORANGE;
            case COCKPIT          -> COCKPIT;
        };
    }

    // ── Caller-color feature flag ─────────────────────────────────────────────
    /**
     * When {@code true}, the caller/callee suffix is rendered with its own ANSI
     * color ({@link ThemeColors#callerFg()}), producing a <em>second</em> ANSI
     * segment on the line.
     *
     * <p><b>⚠️ {@literal @ConsoleOnly} — DO NOT enable in CI or file-appender runs.</b><br>
     * Breaks the one-block-per-line contract: Test History may show split entries,
     * file logs will contain raw escape codes.</p>
     *
     * <pre>{@code
     * CustomLogger.enableCallerColor();   // live terminal only
     * // ...
     * CustomLogger.disableCallerColor();  // restore before CI / file logging
     * }</pre>
     */
    @ConsoleOnly
    private static volatile boolean callerColorEnabled = false;

    /** @see #callerColorEnabled */
    @ConsoleOnly
    public static void enableCallerColor()       { callerColorEnabled = true;  }
    public static void disableCallerColor()      { callerColorEnabled = false; }
    public static boolean isCallerColorEnabled() { return callerColorEnabled;  }

    // ── Logger instances ──────────────────────────────────────────────────────

    public static final Debug debug = new Debug();
    public static final Info  info  = new Info();
    public static final Warn  warn  = new Warn();
    public static final Error error = new Error();

    public static LinkedHashMap<String, Object> fields(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("fields() requires an even number of key/value arguments");
        for (int i = 0; i < pairs.length; i += 2)
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }

    protected static Logger log = Logger.getLogger(CustomLogger.class);
    private static final AtomicBoolean isAnsiEnabled = new AtomicBoolean(detectAnsiSupport());

    private static boolean detectAnsiSupport() {
        String prop = System.getProperty("logger.ansi.enabled");
        if (prop != null) return Boolean.parseBoolean(prop);
        if (System.getProperty("idea.test.cyclic.buffer.size") != null) return false;
        if (System.getProperty("idea.launcher.bin.path") != null) return false;
        if (System.console() != null) return true;
        if (System.getenv("TERM") != null) return true;
        if ("true".equals(System.getenv("ANSICON"))) return true;
        return System.getenv("COLORTERM") != null;
    }

    public static void enableAnsi()  { isAnsiEnabled.set(true);  }
    public static void disableAnsi() { isAnsiEnabled.set(false); }

    public static void initialize(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }

    // ── Configurable call-chain filtering ─────────────────────────────────────

    private static final Set<String> SUPPRESS_CONTAINS = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "core.logging.CustomLogger",
            "org.apache.log4j",
            "java.", "sun.", "jdk.",
            "com.sun.proxy", "jdk.proxy",
            "net.bytebuddy", "reflect."
    )));
    private static final Set<String> SUPPRESS_METHOD_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "log", "debug", "info", "warn", "error", "lambda$", "invoke"
    )));
    private static final Set<String> INCLUDE_ONLY_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

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

    // ── Log-level classes ─────────────────────────────────────────────────────

    /**
     * DEBUG-level logger.
     *
     * <p>Because {@code logLevel = "DEBUG"}, {@link ThemeColors#resolve} automatically
     * composites every intent foreground with {@link ThemeColors#debugBg()} — no overrides
     * needed for visual distinction from INFO-level output.</p>
     */
    public static class Debug extends LogActions {
        public Debug() { super("DEBUG"); }

        // Override only the label: "DEBUG" instead of the base "LOG"
        @Override public void log(String message) {
            logMessage(LogIntent.BASE, "DEBUG", message);
        }
        @Override public void log(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.BASE, "DEBUG");
        }
        @Override public void log(String heading, Object... pairs) {
            log(heading, CustomLogger.fields(pairs));
        }
    }

    public static class Info extends LogActions {
        public Info() { super("INFO"); }

        public void log(String message) { logMessage(LogIntent.BASE, "INFO", message); }
        public void log(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.BASE, "INFO");
        }
        public void log(String heading, Object... pairs) { log(heading, CustomLogger.fields(pairs)); }
    }

    public static class Warn extends LogActions {
        public Warn() { super("WARN"); }

        public void log(String message) { logMessage(LogIntent.BASE, "WARN", message); }
        public void log(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.BASE, "WARN");
        }
        public void log(String heading, Object... pairs) { log(heading, CustomLogger.fields(pairs)); }
    }

    public static class Error extends LogActions {
        public Error() { super("ERROR"); }

        public void log(String message) { logMessage(LogIntent.BASE, "ERROR", message); }
        public void log(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.BASE, "ERROR");
        }
        public void log(String heading, Object... pairs) { log(heading, CustomLogger.fields(pairs)); }
    }

    // ── LogActions — shared action methods ────────────────────────────────────

    /**
     * Base class for all log-level instances. Every action method resolves its ANSI
     * style via {@link ThemeColors#resolve(String, LogIntent)}, compositing the
     * action's {@link LogIntent} foreground with the current log level's background.
     *
     * <p>This means the same action (e.g. {@code click}) automatically renders with a
     * different background when called through {@code debug.*} vs {@code info.*} —
     * no per-level overrides required.</p>
     */
    public static class LogActions {
        protected final String logLevel;

        public LogActions(String logLevel) { this.logLevel = logLevel; }

        // ── INTERACTION group ─────────────────────────────────────────────────
        public void click(String message)      { logMessage(LogIntent.INTERACTION, "CLICK [>]",      message); }
        public void checkbox(String message)   { logMessage(LogIntent.INTERACTION, "CHECKBOX [x]",   message); }
        public void text(String message)       { logMessage(LogIntent.INTERACTION, "TEXT [T]",       message); }
        public void input(String message)      { logMessage(LogIntent.INTERACTION, "INPUT [>>]",     message); }
        public void dropdown(String message)   { logMessage(LogIntent.INTERACTION, "DROPDOWN [v]",   message); }
        public void toggle(String message)     { logMessage(LogIntent.INTERACTION, "TOGGLE [o]",     message); }
        public void upload(String message)     { logMessage(LogIntent.INTERACTION, "UPLOAD [^]",     message); }

        // ── NAVIGATION group ──────────────────────────────────────────────────
        public void tab(String message)        { logMessage(LogIntent.NAVIGATION,  "TAB [->]",       message); }
        public void frame(String message)      { logMessage(LogIntent.NAVIGATION,  "FRAME [{}]",     message); }
        public void breadcrumb(String message) { logMessage(LogIntent.NAVIGATION,  "BREADCRUMB [/]", message); }

        // ── OBSERVE group ─────────────────────────────────────────────────────
        public void wait(String message)       { logMessage(LogIntent.OBSERVE,     "WAIT [~]",       message); }
        public void search(String message)     { logMessage(LogIntent.OBSERVE,     "SEARCHED [*]",   message); }
        public void result(String message)     { logMessage(LogIntent.OBSERVE,     "RESULT [:]",     message); }

        // ── DATA group ────────────────────────────────────────────────────────
        public void table(String message)      { logMessage(LogIntent.DATA,        "TABLE [=]",      message); }
        public void grid(String message)       { logMessage(LogIntent.DATA,        "GRID [#]",       message); }

        // ── SUCCESS group ─────────────────────────────────────────────────────
        public void success(String message)    { logMessage(LogIntent.SUCCESS,     "SUCCESS [+]",    message); }
        public void complete(String message)   { logMessage(LogIntent.SUCCESS,     "COMPLETE [+]",   message); }
        public void resolved(String message)   { logMessage(LogIntent.SUCCESS,     "RESOLVED [ok]",  message); }

        // ── ALERT group ───────────────────────────────────────────────────────
        public void error(String message)      { logMessage(LogIntent.ALERT,       "ERROR [x]",      message); }
        public void failed(String message)     { logMessage(LogIntent.ALERT,       "FAILED [x]",     message); }
        public void timeout(String message)    { logMessage(LogIntent.ALERT,       "TIMEOUT [!!]",   message); }
        public void validation(String message) { logMessage(LogIntent.ALERT,       "VALIDATION [?!]",message); }
        public void fallback(String message)   { logMessage(LogIntent.ALERT,       "FALLBACK [<-]",  message); }
        public void skip(String message)       { logMessage(LogIntent.ALERT,       "SKIP [>>]",      message); }

        // ── Object overloads ─────────────────────────────────────────────────

        public void log(Object obj) {
            if (obj == null)                    { logMessage(LogIntent.BASE, "LOG", "null"); }
            else if (obj instanceof Map<?, ?> m){ table(m); }
            else if (obj instanceof List<?> l)  { logList(l); }
            else if (obj.getClass().isArray())  { logList(java.util.Arrays.asList((Object[]) obj)); }
            else                                { logMessage(LogIntent.BASE, "LOG", obj.toString()); }
        }

        public void log(String heading, Object obj) {
            if (obj == null)                    { logMessage(LogIntent.BASE, "LOG", heading + ": null"); }
            else if (obj instanceof Map<?, ?> m){ logMessage(LogIntent.BASE, "LOG", heading + ":"); table(m); }
            else if (obj instanceof List<?> l)  { logMessage(LogIntent.BASE, "LOG", heading + ":"); logList(l); }
            else if (obj.getClass().isArray())  { logMessage(LogIntent.BASE, "LOG", heading + ":"); logList(java.util.Arrays.asList((Object[]) obj)); }
            else                                { logMessage(LogIntent.BASE, "LOG", heading + ": " + obj); }
        }

        public void log(List<?> list) { logList(list); }

        public void log(String heading, Object... pairs) {
            treeInternal(heading, CustomLogger.fields(pairs), LogIntent.BASE, "LOG");
        }

        // Overridable stubs — subclasses (Info/Warn/Error/Debug) override with level-specific labels
        public void log(String message)                        { logMessage(LogIntent.BASE, "LOG", message); }
        public void log(String heading, Map<String, ?> fields) { treeInternal(heading, fields, LogIntent.BASE, "LOG"); }

        private void logList(List<?> list) {
            if (list == null || list.isEmpty()) { logMessage(LogIntent.BASE, "LOG", "(empty list)"); return; }
            int i = 0;
            for (Object item : list) {
                String prefix = "  [" + (i++) + "] ";
                if (item instanceof Map<?, ?> m) { logMessage(LogIntent.BASE, "LOG", prefix); table(m); }
                else                             { logMessage(LogIntent.BASE, "LOG", prefix + item); }
            }
        }

        // ── Table rendering ───────────────────────────────────────────────────

        private static String center(String s, int width) {
            String plain = s.replaceAll("\\u001B\\[[;\\d]*m", "");
            int len = plain.length();
            if (len >= width) return s.substring(0, width);
            int left = (width - len) / 2;
            return " ".repeat(left) + s + " ".repeat(width - len - left);
        }

        public void table(List<? extends Map<?, ?>> rows, String title) {
            if (rows == null || rows.isEmpty()) {
                logMessage(LogIntent.DATA, "TABLE", "No rows to display.");
                return;
            }
            LinkedHashMap<String, Integer> colWidths = new LinkedHashMap<>();
            for (Map<?, ?> row : rows) {
                for (Object key : row.keySet()) {
                    String col = String.valueOf(key);
                    String val = truncateCell(String.valueOf(row.get(key)));
                    colWidths.put(col, Math.max(
                            colWidths.getOrDefault(col, col.length()),
                            Math.max(col.length(), val.length())));
                }
            }
            List<String> headers = new java.util.ArrayList<>(colWidths.keySet());
            boolean ansi = isAnsiEnabled.get();
            String color = ansi ? getColors().resolve(logLevel, LogIntent.DATA) : "";
            String rst   = ansi ? getColors().reset() : "";

            String hBorder  = "+" + headers.stream()
                    .map(h -> "-".repeat(colWidths.get(h) + 2))
                    .reduce((a, b) -> a + "+" + b).orElse("") + "+";
            String headerRow = "|" + headers.stream()
                    .map(h -> " " + truncateCell(h) + " ".repeat(colWidths.get(h) - h.length()) + " ")
                    .reduce((a, b) -> a + "|" + b).orElse("") + "|";

            StringBuilder sb = new StringBuilder();
            sb.append(hBorder).append("\n");

            if (title != null && !title.isEmpty()) {
                String bold     = ansi ? ANSI_BOLD  : "";
                String resetBold = ansi ? ANSI_RESET : "";
                sb.append("|").append(bold)
                  .append(center(" " + title + " ", headerRow.length() - 2))
                  .append(resetBold).append("|\n");
                sb.append(hBorder).append("\n");
            }

            sb.append(headerRow).append("\n").append(hBorder).append("\n");
            for (Map<?, ?> row : rows) {
                String rowStr = "|" + headers.stream().map(h -> {
                    Object v = null;
                    for (Object key : row.keySet())
                        if (String.valueOf(key).equals(h)) { v = row.get(key); break; }
                    if (v == null) v = "";
                    String s = truncateCell(String.valueOf(v));
                    return " " + s + " ".repeat(colWidths.get(h) - s.length()) + " ";
                }).reduce((a, b) -> a + "|" + b).orElse("") + "|";
                sb.append(rowStr).append("\n");
            }
            sb.append(hBorder);
            // Use the explicit-color overload so the pre-built table string is wrapped correctly.
            logMessage(color, "TABLE", "\n" + sb + rst);
        }

        public void table(List<? extends Map<?, ?>> rows) { table(rows, null); }

        public void table(Map<?, ?> row, String title) {
            if (row == null || row.isEmpty()) { logMessage(LogIntent.DATA, "TABLE", "No data to display."); return; }
            table(java.util.Collections.singletonList(row), title);
        }

        public void table(Map<?, ?> row) { table(row, (String) null); }

        public void row(Map<?, ?> data) {
            if (data == null || data.isEmpty()) { logMessage(LogIntent.DATA, "ROW", "(empty)"); return; }
            int maxKeyLen = data.keySet().stream()
                    .map(k -> String.valueOf(k).length()).max(Integer::compareTo).orElse(0);
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                logMessage(LogIntent.DATA, "ROW",
                        String.format("%-" + maxKeyLen + "s : %s",
                                String.valueOf(entry.getKey()),
                                truncateCell(String.valueOf(entry.getValue()))));
            }
        }

        // ── Tree / resolved ───────────────────────────────────────────────────

        public void tree(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.BASE, "LOG");
        }

        public void tree(String heading, Object... pairs) {
            treeInternal(heading, CustomLogger.fields(pairs), LogIntent.BASE, "LOG");
        }

        public void resolved(String heading, Map<String, ?> fields) {
            treeInternal(heading, fields, LogIntent.SUCCESS, "RESOLVED");
        }

        public void resolved(String heading, Object... pairs) {
            treeInternal(heading, CustomLogger.fields(pairs), LogIntent.SUCCESS, "RESOLVED");
        }

        /** Internal tree renderer — composes color from intent + level, then logs each branch. */
        protected void treeInternal(String heading, Map<String, ?> fields,
                                    LogIntent intent, String label) {
            if (heading == null || fields == null) return;
            logMessage(intent, label, heading);
            int size = fields.size(), i = 0;
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                i++;
                String prefix = (i < size) ? "          ├─ " : "          └─ ";
                Object v = entry.getValue();
                String value;
                if (v != null && v.getClass().isArray()) {
                    if      (v instanceof Object[]  a) value = java.util.Arrays.deepToString(a);
                    else if (v instanceof int[]     a) value = java.util.Arrays.toString(a);
                    else if (v instanceof long[]    a) value = java.util.Arrays.toString(a);
                    else if (v instanceof double[]  a) value = java.util.Arrays.toString(a);
                    else if (v instanceof boolean[] a) value = java.util.Arrays.toString(a);
                    else if (v instanceof char[]    a) value = java.util.Arrays.toString(a);
                    else if (v instanceof float[]   a) value = java.util.Arrays.toString(a);
                    else if (v instanceof short[]   a) value = java.util.Arrays.toString(a);
                    else if (v instanceof byte[]    a) value = java.util.Arrays.toString(a);
                    else value = String.valueOf(v);
                } else {
                    value = String.valueOf(v);
                }
                logMessage(intent, label,
                        prefix + String.format("%-12s: %s", entry.getKey(), value));
            }
        }

        // ── Core rendering ────────────────────────────────────────────────────

        /**
         * Primary render path — resolves ANSI style from {@code intent} + {@code logLevel},
         * then delegates to {@link #logMultiline}.
         */
        protected void logMessage(LogIntent intent, String actionLabel, String message) {
            logMultiline(getColors().resolve(logLevel, intent), actionLabel, message,
                         CustomLogger.isDebugEnabled());
        }

        /**
         * Low-level render path with an explicit pre-composed ANSI style string.
         * Used by the table renderer which builds its own multi-line block.
         */
        protected void logMessage(String actionColor, String actionLabel, String message) {
            logMultiline(actionColor, actionLabel, message, CustomLogger.isDebugEnabled());
        }

        /**
         * Splits {@code message} on newlines and emits one log entry per line.
         *
         * <p><b>Single-color-per-line (default):</b> each line is wrapped in exactly one
         * {@code <ANSI-open>…<ANSI-reset>} block — safe for IntelliJ Test History, CI, and
         * file appenders.</p>
         *
         * <p><b>{@literal @ConsoleOnly} mode</b> (when {@link #callerColorEnabled} is
         * {@code true}): the caller suffix gets a second ANSI segment using
         * {@link ThemeColors#callerFg()}, intentionally breaking the one-block contract for
         * a richer live-console look. Do <b>not</b> enable in CI or file-appender runs.</p>
         */
        protected void logMultiline(String actionColor, String actionLabel,
                                    String message, boolean showCaller) {
            if (message == null) message = "null";
            String ts         = "[" + LocalDateTime.now().format(TS_FMT) + "]";
            String callerText = showCaller ? getCallerString() : "";
            String[] lines    = message.split("\\R", -1);
            boolean ansi      = isAnsiEnabled.get();

            for (int i = 0; i < lines.length; i++) {
                String body = ts + " [" + actionLabel + "] " + lines[i];
                String out;
                if (ansi) {
                    if (i == 0 && !callerText.isEmpty() && callerColorEnabled) {
                        // ⚠️ @ConsoleOnly: two ANSI segments — caller gets its own color.
                        out = actionColor + body + ANSI_RESET
                            + " " + getColors().callerFg() + callerText + ANSI_RESET;
                    } else {
                        // ✅ Single-block: one ANSI open + one reset per line.
                        String cp = (i == 0 && !callerText.isEmpty()) ? " " + callerText : "";
                        out = actionColor + body + cp + ANSI_RESET;
                    }
                } else {
                    String cp = (i == 0 && !callerText.isEmpty()) ? " " + callerText : "";
                    out = body + cp;
                }
                switch (logLevel) {
                    case "ERROR" -> getSafeLogger().error(out);
                    case "WARN"  -> getSafeLogger().warn(out);
                    case "INFO"  -> getSafeLogger().info(out);
                    default      -> getSafeLogger().debug(out);
                }
            }
        }

        // ── Caller resolution ─────────────────────────────────────────────────

        private static String simpleClass(String fqcn) {
            int i = (fqcn == null) ? -1 : fqcn.lastIndexOf('.');
            return (i >= 0) ? fqcn.substring(i + 1) : (fqcn == null ? "" : fqcn);
        }

        private static boolean filteredOut(String className, String methodName) {
            if (methodName != null)
                for (String p : SUPPRESS_METHOD_PREFIXES)
                    if (methodName.startsWith(p)) return true;
            if (className != null) {
                if (!INCLUDE_ONLY_PREFIXES.isEmpty()) {
                    if (INCLUDE_ONLY_PREFIXES.stream().noneMatch(className::startsWith)) return true;
                }
                for (String s : SUPPRESS_CONTAINS)
                    if (className.contains(s) || className.startsWith(s)) return true;
            }
            return false;
        }

        /** Returns {@code "Callee.method ← Caller.method"} as plain text. */
        protected String getCallerString() {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            int calleeIdx = -1, callerIdx = -1;
            for (int i = 3; i < st.length; i++) {
                if (filteredOut(st[i].getClassName(), st[i].getMethodName())) continue;
                if (calleeIdx == -1) { calleeIdx = i; continue; }
                if (st[i].getClassName().equals(st[calleeIdx].getClassName())
                        && st[i].getMethodName().equals(st[calleeIdx].getMethodName())) continue;
                callerIdx = i; break;
            }
            if (calleeIdx == -1) return "";
            String cm = st[calleeIdx].getMethodName();
            String left = simpleClass(st[calleeIdx].getClassName()) + "."
                    + ("<init>".equals(cm) ? "(constructor)" : "<clinit>".equals(cm) ? "(static init)" : cm);
            if (callerIdx == -1) return left;
            String pm = st[callerIdx].getMethodName();
            return left + " \u2190 " + simpleClass(st[callerIdx].getClassName()) + "."
                    + ("<init>".equals(pm) ? "(constructor)" : "<clinit>".equals(pm) ? "(static init)" : pm);
        }

        /** Convenience for callers that need a pre-composed style (e.g. table renderer). */
        protected String getLevelColor() {
            return getColors().resolve(logLevel, LogIntent.BASE);
        }
    }

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
        public static String fgFromBg(String style) {
            if (style == null) return FG_BRIGHT_WHITE;
            java.util.regex.Matcher fg = java.util.regex.Pattern
                    .compile("(\u001B\\[3\\d{1,2}(;\\d{1,2})?m)").matcher(style);
            if (fg.find()) return fg.group();
            java.util.regex.Matcher m8 = java.util.regex.Pattern
                    .compile("(\u001B\\[4)(\\d)(m)").matcher(style);
            if (m8.find()) {
                String c = "\u001B[3" + m8.group(2) + "m";
                return c.equals(FG_BLACK) ? FG_BRIGHT_WHITE : c;
            }
            java.util.regex.Matcher m256 = java.util.regex.Pattern
                    .compile("(\u001B\\[48;5;)(\\d+)(m)").matcher(style);
            if (m256.find()) return "\u001B[38;5;" + m256.group(2) + "m";

            return FG_BRIGHT_WHITE;
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    protected static Logger getSafeLogger() {
        return (log != null) ? log : Logger.getLogger(CustomLogger.class);
    }
}
