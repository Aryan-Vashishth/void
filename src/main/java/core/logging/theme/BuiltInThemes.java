package core.logging.theme;

import static core.logging.ansi.AnsiColors.*;

/**
 * Registry of all built-in {@link ThemeColors} instances and the active-theme selector.
 */
public final class BuiltInThemes {

    private BuiltInThemes() {}

    // ── PLAIN (★ default) ─────────────────────────────────────────────────────
    public static final ThemeColors PLAIN = ThemeColors.builder()
            .infoBg  ("\u001B[48;2;42;46;58m")
            .warnBg  ("\u001B[48;2;148;108;0m")
            .errorBg ("\u001B[48;2;155;16;16m")
            .debugBg ("\u001B[48;2;10;10;14m")
            .infoFg  (FG_BRIGHT_WHITE      + BOLD)
            .warnFg  (FG_BLACK             + BOLD)
            .errorFg (FG_BRIGHT_WHITE      + BOLD)
            .debugFg (FG_WHITE             + BOLD)
            .interactionFg (FG_BRIGHT_WHITE      + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN     + BOLD)
            .observeFg     (RGB_FG_DEEP_AMBER    + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET   + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN    + BOLD)
            .alertFg       (RGB_FG_DEEP_RED      + BOLD)
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
            .errorFg ("\u001B[38;2;195;38;38m"   + BOLD)
            .debugFg ("\u001B[38;2;101;123;131m")
            .interactionFg ("\u001B[38;2;131;148;150m" + BOLD)
            .navigationFg  ("\u001B[38;2;28;138;195m"  + BOLD)
            .observeFg     ("\u001B[38;2;175;130;0m"   + BOLD)
            .dataFg        ("\u001B[38;2;100;100;188m" + BOLD)
            .successFg     ("\u001B[38;2;110;145;0m"   + BOLD)
            .alertFg       ("\u001B[38;2;195;55;20m"   + BOLD)
            .callerFg      ("\u001B[38;2;88;110;117m")
            .build();

    // ── HIGH_CONTRAST ─────────────────────────────────────────────────────────
    public static final ThemeColors HIGH_CONTRAST = ThemeColors.builder()
            .infoBg  (BG_BLACK)
            .warnBg  (BG_BLACK)
            .errorBg (BG_BLACK)
            .debugBg (BG_BLACK)
            .infoFg  (FG_BRIGHT_WHITE   + BOLD)
            .warnFg  (RGB_FG_DEEP_AMBER + BOLD)
            .errorFg (RGB_FG_DEEP_RED   + BOLD)
            .debugFg (FG_BRIGHT_BLACK   + BOLD)
            .interactionFg (FG_BRIGHT_WHITE    + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_AMBER  + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
            .callerFg      (FG_BRIGHT_BLACK)
            .build();

    // ── MODERN_CLEAN ──────────────────────────────────────────────────────────
    public static final ThemeColors MODERN_CLEAN = ThemeColors.builder()
            .infoBg  (RGB_BG_CHARCOAL)
            .warnBg  (RGB_BG_DARK_AMBER)
            .errorBg (RGB_BG_CRIMSON)
            .debugBg (RGB_BG_MIDNIGHT)
            .infoFg  (RGB_FG_SNOW_WHITE    + BOLD)
            .warnFg  (RGB_FG_GOLD          + BOLD)
            .errorFg (RGB_FG_DEEP_RED      + BOLD)
            .debugFg (RGB_FG_COOL_GREY     + BOLD)
            .interactionFg (RGB_FG_SNOW_WHITE  + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_AMBER  + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
            .callerFg      (RGB_FG_COOL_GREY)
            .build();

    // ── COCKPIT ───────────────────────────────────────────────────────────────
    public static final ThemeColors COCKPIT = ThemeColors.builder()
            .infoBg  (RGB_BG_DARK_FOREST)
            .warnBg  (RGB_BG_DARK_AMBER)
            .errorBg (RGB_BG_DARK_WINE)
            .debugBg (RGB_BG_NEAR_BLACK)
            .infoFg  (RGB_FG_DEEP_GREEN  + BOLD)
            .warnFg  (RGB_FG_GOLD        + BOLD)
            .errorFg (RGB_FG_SNOW_WHITE  + BOLD)
            .debugFg (RGB_FG_COOL_GREY)
            .interactionFg (RGB_FG_SNOW_WHITE  + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_AMBER  + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
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
            .interactionFg (RGB_FG_SNOW_WHITE  + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_AMBER  + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
            .callerFg      ("\u001B[38;2;110;118;130m")
            .build();

    // ── NIGHT_CLUB ────────────────────────────────────────────────────────────
    public static final ThemeColors NIGHT_CLUB = ThemeColors.builder()
            .infoBg  (RGB_BG_DARK_TEAL)
            .warnBg  (RGB_BG_HOT_PINK)
            .errorBg (RGB_BG_NEON_PURPLE)
            .debugBg (RGB_BG_DARK_INDIGO)
            .infoFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .warnFg  (RGB_FG_SNOW_WHITE  + BOLD)
            .errorFg (RGB_FG_SNOW_WHITE  + BOLD)
            .debugFg (RGB_FG_LAVENDER)
            .interactionFg (RGB_FG_DEEP_PINK   + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .dataFg        (RGB_FG_DEEP_AMBER  + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
            .callerFg      (RGB_FG_COOL_GREY)
            .build();

    // ── CARBON_ORANGE ─────────────────────────────────────────────────────────
    public static final ThemeColors CARBON_ORANGE = ThemeColors.builder()
            .infoBg  (RGB_BG_CARBON)
            .warnBg  (RGB_BG_ORANGE_VIVID)
            .errorBg (RGB_BG_CRIMSON)
            .debugBg (RGB_BG_MIDNIGHT)
            .infoFg  (RGB_FG_AMBER         + BOLD)
            .warnFg  (RGB_FG_DEEP_CHARCOAL + BOLD)
            .errorFg (RGB_FG_DEEP_RED      + BOLD)
            .debugFg (RGB_FG_WARM_GREY)
            .interactionFg (RGB_FG_DEEP_ORANGE + BOLD)
            .navigationFg  (RGB_FG_DEEP_CYAN   + BOLD)
            .observeFg     (RGB_FG_DEEP_ORANGE + BOLD)
            .dataFg        (RGB_FG_DEEP_VIOLET + BOLD)
            .successFg     (RGB_FG_DEEP_GREEN  + BOLD)
            .alertFg       (RGB_FG_DEEP_RED    + BOLD)
            .callerFg      (RGB_FG_WARM_GREY)
            .build();

    // ── Active theme state ────────────────────────────────────────────────────

    private static volatile LogTheme currentTheme = LogTheme.PLAIN;
    private static volatile ThemeColors customTheme = null;

    public static void setTheme(LogTheme theme) {
        currentTheme  = theme;
        customTheme   = null;
    }

    public static void setCustomTheme(ThemeColors colors) {
        customTheme = colors;
    }

    public static LogTheme getCurrentTheme() { return currentTheme; }

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

    public static ThemeColors getColors() {
        if (customTheme != null) return customTheme;
        return resolve(currentTheme);
    }
}

