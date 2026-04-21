package core.logging;

import static core.logging.AnsiColors.*;

/**
 * Registry of all built-in {@link ThemeColors} instances and the active-theme selector.
 *
 * <p>Call {@link #setTheme(LogTheme)} to change the active theme at runtime.
 * Call {@link #getColors()} to retrieve the currently active {@link ThemeColors}.</p>
 *
 * <p>You may also register your own custom theme via {@link #setCustomTheme(ThemeColors)}.</p>
 */
public final class BuiltInThemes {

    private BuiltInThemes() {}

    // ── PLAIN (★ default) ─────────────────────────────────────────────────────
    public static final ThemeColors PLAIN = ThemeColors.builder()
            .infoBg  ("\u001B[48;2;42;46;58m")
            .warnBg  ("\u001B[48;2;148;108;0m")
            .errorBg ("\u001B[48;2;155;16;16m")
            .debugBg ("\u001B[48;2;10;10;14m")
            .infoFg  (FG_BRIGHT_WHITE  + BOLD)
            .warnFg  (FG_BLACK         + BOLD)
            .errorFg (FG_BRIGHT_WHITE  + BOLD)
            .debugFg (FG_WHITE         + BOLD)
            .interactionFg (FG_BRIGHT_WHITE  + BOLD)
            .navigationFg  (FG_BRIGHT_CYAN   + BOLD)
            .observeFg     (FG_BRIGHT_YELLOW + BOLD)
            .dataFg        (FG_BRIGHT_MAGENTA+ BOLD)
            .successFg     (FG_BRIGHT_GREEN  + BOLD)
            .alertFg       (FG_BRIGHT_RED    + BOLD)
            .callerFg      (FG_BRIGHT_BLACK)
            .build();

    // ── SOLARIZED_DARK ────────────────────────────────────────────────────────
    public static final ThemeColors SOLARIZED_DARK = ThemeColors.builder()
            .infoBg  ("\u001B[48;2;7;54;66m")
            .warnBg  ("\u001B[48;2;101;74;0m")
            .errorBg ("\u001B[48;2;88;20;18m")
            .debugBg ("\u001B[48;2;0;43;54m")
            .infoFg  ("\u001B[38;2;147;161;161m" + BOLD)
            .warnFg  ("\u001B[38;2;181;137;0m"   + BOLD)
            .errorFg ("\u001B[38;2;220;50;47m"   + BOLD)
            .debugFg ("\u001B[38;2;101;123;131m")
            .interactionFg ("\u001B[38;2;131;148;150m" + BOLD)
            .navigationFg  ("\u001B[38;2;38;139;210m"  + BOLD)
            .observeFg     ("\u001B[38;2;181;137;0m"   + BOLD)
            .dataFg        ("\u001B[38;2;108;113;196m" + BOLD)
            .successFg     ("\u001B[38;2;133;153;0m"   + BOLD)
            .alertFg       ("\u001B[38;2;203;75;22m"   + BOLD)
            .callerFg      ("\u001B[38;2;88;110;117m")
            .build();

    // ── HIGH_CONTRAST ─────────────────────────────────────────────────────────
    public static final ThemeColors HIGH_CONTRAST = ThemeColors.builder()
            .infoBg  (BG_BLACK)
            .warnBg  (BG_BLACK)
            .errorBg (BG_BLACK)
            .debugBg (BG_BLACK)
            .infoFg  (FG_BRIGHT_WHITE  + BOLD)
            .warnFg  (FG_BRIGHT_YELLOW + BOLD)
            .errorFg (FG_BRIGHT_RED    + BOLD)
            .debugFg (FG_BRIGHT_BLACK  + BOLD)
            .interactionFg (FG_BRIGHT_WHITE   + BOLD)
            .navigationFg  (FG_BRIGHT_CYAN    + BOLD)
            .observeFg     (FG_BRIGHT_YELLOW  + BOLD)
            .dataFg        (FG_BRIGHT_MAGENTA + BOLD)
            .successFg     (FG_BRIGHT_GREEN   + BOLD)
            .alertFg       (FG_BRIGHT_RED     + BOLD)
            .callerFg      (FG_BRIGHT_BLACK)
            .build();

    // ── MODERN_CLEAN ──────────────────────────────────────────────────────────
    public static final ThemeColors MODERN_CLEAN = ThemeColors.builder()
            .infoBg  (RGB_BG_CHARCOAL)
            .warnBg  (RGB_BG_DARK_AMBER)
            .errorBg (RGB_BG_CRIMSON)
            .debugBg (RGB_BG_MIDNIGHT)
            .infoFg  (RGB_FG_SNOW_WHITE + BOLD)
            .warnFg  (RGB_FG_GOLD       + BOLD)
            .errorFg (RGB_FG_CORAL      + BOLD)
            .debugFg (RGB_FG_COOL_GREY  + BOLD)
            .interactionFg (RGB_FG_SNOW_WHITE + BOLD)
            .navigationFg  (RGB_FG_STEEL_CYAN + BOLD)
            .observeFg     (RGB_FG_GOLD       + BOLD)
            .dataFg        (RGB_FG_LAVENDER   + BOLD)
            .successFg     (RGB_FG_LIME_GREEN + BOLD)
            .alertFg       (RGB_FG_CORAL      + BOLD)
            .callerFg      (RGB_FG_COOL_GREY)
            .build();

    // ── COCKPIT ───────────────────────────────────────────────────────────────
    public static final ThemeColors COCKPIT = ThemeColors.builder()
            .infoBg  (RGB_BG_DARK_FOREST)
            .warnBg  (RGB_BG_DARK_AMBER)
            .errorBg (RGB_BG_DARK_WINE)
            .debugBg (RGB_BG_NEAR_BLACK)
            .infoFg  (RGB_FG_LIME_GREEN + BOLD)
            .warnFg  (RGB_FG_GOLD       + BOLD)
            .errorFg (RGB_FG_SNOW_WHITE + BOLD)
            .debugFg (RGB_FG_COOL_GREY)
            .interactionFg (RGB_FG_SNOW_WHITE + BOLD)
            .navigationFg  (RGB_FG_MINT       + BOLD)
            .observeFg     (RGB_FG_GOLD       + BOLD)
            .dataFg        (RGB_FG_LAVENDER   + BOLD)
            .successFg     (RGB_FG_LIME_GREEN + BOLD)
            .alertFg       (RGB_FG_CORAL      + BOLD)
            .callerFg      (RGB_FG_COOL_GREY)
            .build();

    // ── INDUSTRIAL_STEEL ──────────────────────────────────────────────────────
    public static final ThemeColors INDUSTRIAL_STEEL = ThemeColors.builder()
            .infoBg  ("\u001B[48;2;55;60;70m")
            .warnBg  ("\u001B[48;2;175;135;0m")
            .errorBg ("\u001B[48;2;155;22;22m")
            .debugBg ("\u001B[48;2;20;22;28m")
            .infoFg  (RGB_FG_SNOW_WHITE    + BOLD)
            .warnFg  (RGB_FG_DEEP_CHARCOAL + BOLD)
            .errorFg (RGB_FG_SNOW_WHITE    + BOLD)
            .debugFg ("\u001B[38;2;110;118;130m")
            .interactionFg (RGB_FG_SNOW_WHITE              + BOLD)
            .navigationFg  ("\u001B[38;2;80;195;255m"      + BOLD)
            .observeFg     ("\u001B[38;2;255;200;40m"      + BOLD)
            .dataFg        ("\u001B[38;2;60;235;160m"      + BOLD)
            .successFg     ("\u001B[38;2;50;220;90m"       + BOLD)
            .alertFg       ("\u001B[38;2;255;75;55m"       + BOLD)
            .callerFg      ("\u001B[38;2;110;118;130m")
            .build();

    // ── NIGHT_CLUB (DISCO) ────────────────────────────────────────────────────
    public static final ThemeColors NIGHT_CLUB = ThemeColors.builder()
            .infoBg  (RGB_BG_DARK_TEAL)
            .warnBg  (RGB_BG_HOT_PINK)
            .errorBg (RGB_BG_NEON_PURPLE)
            .debugBg (RGB_BG_DARK_INDIGO)
            .infoFg  (RGB_FG_MINT       + BOLD)
            .warnFg  (RGB_FG_SNOW_WHITE + BOLD)
            .errorFg (RGB_FG_SNOW_WHITE + BOLD)
            .debugFg (RGB_FG_LAVENDER)
            .interactionFg (RGB_FG_HOT_PINK   + BOLD)
            .navigationFg  (RGB_FG_SKY_BLUE   + BOLD)
            .observeFg     (RGB_FG_NEON_GREEN + BOLD)
            .dataFg        (RGB_FG_GOLD       + BOLD)
            .successFg     (RGB_FG_NEON_GREEN + BOLD)
            .alertFg       (RGB_FG_CORAL      + BOLD)
            .callerFg      (RGB_FG_COOL_GREY)
            .build();

    // ── CARBON_ORANGE ─────────────────────────────────────────────────────────
    public static final ThemeColors CARBON_ORANGE = ThemeColors.builder()
            .infoBg  (RGB_BG_CARBON)
            .warnBg  (RGB_BG_ORANGE_VIVID)
            .errorBg (RGB_BG_CRIMSON)
            .debugBg (RGB_BG_MIDNIGHT)
            .infoFg  (RGB_FG_AMBER        + BOLD)
            .warnFg  (RGB_FG_DEEP_CHARCOAL + BOLD)
            .errorFg (RGB_FG_PEACH        + BOLD)
            .debugFg (RGB_FG_WARM_GREY)
            .interactionFg (RGB_FG_AMBER      + BOLD)
            .navigationFg  (RGB_FG_STEEL_CYAN + BOLD)
            .observeFg     (RGB_FG_PEACH      + BOLD)
            .dataFg        (RGB_FG_LAVENDER   + BOLD)
            .successFg     (RGB_FG_LIME_GREEN + BOLD)
            .alertFg       (RGB_FG_CORAL      + BOLD)
            .callerFg      (RGB_FG_WARM_GREY)
            .build();

    // ── Active theme state ────────────────────────────────────────────────────

    private static volatile LogTheme currentTheme = LogTheme.PLAIN;
    private static volatile ThemeColors customTheme = null;

    /** Sets the active theme from the built-in {@link LogTheme} catalogue. */
    public static void setTheme(LogTheme theme) {
        currentTheme  = theme;
        customTheme   = null;
    }

    /**
     * Overrides the active theme with a fully custom {@link ThemeColors} instance.
     * Call {@link #setTheme(LogTheme)} to revert to a built-in theme.
     */
    public static void setCustomTheme(ThemeColors colors) {
        customTheme = colors;
    }

    /** Returns the currently active {@link LogTheme} key (may be {@code null} if a custom theme is set). */
    public static LogTheme getCurrentTheme() { return currentTheme; }

    /**
     * Resolves a {@link LogTheme} key to its {@link ThemeColors} without
     * consulting the active-theme state. Used by {@link LogConfig#resolvedTheme()}.
     */
    public static ThemeColors resolve(LogTheme theme) {
        if (theme == null) return PLAIN;
        return switch (theme) {
            case PLAIN            -> PLAIN;
            case SOLARIZED_DARK   -> SOLARIZED_DARK;
            case HIGH_CONTRAST    -> HIGH_CONTRAST;
            case MODERN_CLEAN     -> MODERN_CLEAN;
            case INDUSTRIAL_STEEL -> INDUSTRIAL_STEEL;
            case NIGHT_CLUB       -> NIGHT_CLUB;
            case CARBON_ORANGE    -> CARBON_ORANGE;
            case COCKPIT          -> COCKPIT;
        };
    }

    /** Returns the {@link ThemeColors} for the currently active theme. */
    public static ThemeColors getColors() {
        if (customTheme != null) return customTheme;
        return switch (currentTheme) {
            case PLAIN            -> PLAIN;
            case SOLARIZED_DARK   -> SOLARIZED_DARK;
            case HIGH_CONTRAST    -> HIGH_CONTRAST;
            case MODERN_CLEAN     -> MODERN_CLEAN;
            case INDUSTRIAL_STEEL -> INDUSTRIAL_STEEL;
            case NIGHT_CLUB       -> NIGHT_CLUB;
            case CARBON_ORANGE    -> CARBON_ORANGE;
            case COCKPIT          -> COCKPIT;
        };
    }
}

