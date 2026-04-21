package core.logging;

/**
 * ANSI escape-code constants for terminal styling.
 *
 * <p>Three tiers are provided:</p>
 * <ol>
 *   <li><b>Standard 16-color</b> — {@code \u001B[<code>m} — supported everywhere.</li>
 *   <li><b>256-color</b> — {@code \u001B[38;5;<n>m} (FG) / {@code \u001B[48;5;<n>m} (BG)</li>
 *   <li><b>True RGB</b> — {@code \u001B[38;2;R;G;Bm} (FG) / {@code \u001B[48;2;R;G;Bm} (BG)</li>
 * </ol>
 */
public final class AnsiColors {

    private AnsiColors() {}

    // ── Control sequences ─────────────────────────────────────────────────────
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String DIM    = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";

    // ── Standard 16-color foregrounds ─────────────────────────────────────────
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
    @Deprecated
    public static final String FG_DIM_WHITE      = "\u001B[37;2m";

    // ── Standard 16-color backgrounds ─────────────────────────────────────────
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
    // aliases
    public static final String BG_GREY_100       = BG_BRIGHT_BLACK;
    public static final String BG_BRIGHT_GREY    = BG_WHITE;

    // ── 256-color foregrounds ─────────────────────────────────────────────────
    public static final String FG_256_ORANGE     = "\u001B[38;5;208m";
    public static final String FG_256_GOLD       = "\u001B[38;5;220m";
    public static final String FG_256_LIME       = "\u001B[38;5;118m";
    public static final String FG_256_SKY        = "\u001B[38;5;117m";
    public static final String FG_256_VIOLET     = "\u001B[38;5;135m";
    public static final String FG_256_PINK       = "\u001B[38;5;205m";
    public static final String FG_256_TEAL       = "\u001B[38;5;80m";
    public static final String FG_256_SALMON     = "\u001B[38;5;209m";
    public static final String FG_256_GREY_DARK  = "\u001B[38;5;240m";
    public static final String FG_256_GREY_MID   = "\u001B[38;5;246m";
    public static final String FG_256_GREY_LIGHT = "\u001B[38;5;252m";
    public static final String FG_256_NAVY       = "\u001B[38;5;17m";
    public static final String FG_256_MAROON     = "\u001B[38;5;88m";
    public static final String FG_256_OLIVE      = "\u001B[38;5;100m";
    public static final String FG_256_INDIGO     = "\u001B[38;5;54m";
    // backward-compat aliases
    public static final String FG_ORANGE_208      = FG_256_ORANGE;
    public static final String FG_BOLD_ORANGE_208 = "\u001B[38;5;208;1m";
    public static final String FG_NAVY_BLUE       = FG_256_NAVY;

    // ── 256-color backgrounds ─────────────────────────────────────────────────
    public static final String BG_256_ORANGE      = "\u001B[48;5;208m";
    public static final String BG_256_DARK_GREY   = "\u001B[48;5;235m";
    public static final String BG_256_MID_GREY    = "\u001B[48;5;238m";
    public static final String BG_256_NAVY        = "\u001B[48;5;17m";
    public static final String BG_256_DARK_GREEN  = "\u001B[48;5;22m";
    public static final String BG_256_DARK_TEAL   = "\u001B[48;5;23m";
    public static final String BG_256_DARK_PURPLE = "\u001B[48;5;53m";
    public static final String BG_256_DARK_OLIVE  = "\u001B[48;5;94m";
    public static final String BG_256_DARK_RED    = "\u001B[48;5;52m";
    public static final String BG_256_MAROON      = "\u001B[48;5;88m";
    public static final String BG_256_INDIGO      = "\u001B[48;5;54m";
    // backward-compat aliases
    public static final String BG_ORANGE_208     = BG_256_ORANGE;
    public static final String BG_DARKER_GREY    = BG_256_DARK_GREY;
    public static final String BG_DARKER_BLUE    = BG_256_NAVY;
    public static final String BG_DARKER_GREEN   = BG_256_DARK_GREEN;
    public static final String BG_DARKER_MAGENTA = BG_256_DARK_PURPLE;
    public static final String BG_DARKER_YELLOW  = BG_256_DARK_OLIVE;
    public static final String BG_DARKER_RED     = BG_256_DARK_RED;
    public static final String BG_MAROON_RED     = BG_256_MAROON;

    // ── True-RGB foregrounds ──────────────────────────────────────────────────
    // Neutrals
    public static final String RGB_FG_SNOW_WHITE    = "\u001B[38;2;240;242;245m";
    public static final String RGB_FG_SOFT_WHITE    = "\u001B[38;2;210;215;220m";
    public static final String RGB_FG_COOL_GREY     = "\u001B[38;2;140;150;165m";
    public static final String RGB_FG_WARM_GREY     = "\u001B[38;2;160;158;150m";
    public static final String RGB_FG_DEEP_CHARCOAL = "\u001B[38;2;28;30;38m";
    // Warm
    public static final String RGB_FG_GOLD          = "\u001B[38;2;255;200;50m";
    public static final String RGB_FG_AMBER         = "\u001B[38;2;255;170;0m";
    public static final String RGB_FG_PEACH         = "\u001B[38;2;255;185;110m";
    public static final String RGB_FG_CORAL         = "\u001B[38;2;255;105;85m";
    public static final String RGB_FG_SALMON        = "\u001B[38;2;255;140;105m";
    public static final String RGB_FG_HOT_PINK      = "\u001B[38;2;255;75;170m";
    // Cool
    public static final String RGB_FG_SKY_BLUE      = "\u001B[38;2;80;185;255m";
    public static final String RGB_FG_ELECTRIC_BLUE = "\u001B[38;2;50;140;255m";
    public static final String RGB_FG_STEEL_CYAN    = "\u001B[38;2;90;220;220m";
    public static final String RGB_FG_MINT          = "\u001B[38;2;60;220;175m";
    public static final String RGB_FG_LIME_GREEN    = "\u001B[38;2;100;240;120m";
    public static final String RGB_FG_NEON_GREEN    = "\u001B[38;2;80;255;120m";
    // Purple family
    public static final String RGB_FG_LAVENDER      = "\u001B[38;2;185;155;255m";
    public static final String RGB_FG_VIOLET        = "\u001B[38;2;160;100;255m";
    public static final String RGB_FG_PURPLE        = "\u001B[38;2;155;111;224m";
    public static final String RGB_FG_DARKER_PURPLE = "\u001B[38;2;122;79;196m";
    public static final String RGB_FG_DEEP_PURPLE   = "\u001B[38;2;90;54;163m";
    // backward-compat aliases
    public static final String FG_PURPLE        = RGB_FG_PURPLE;
    public static final String FG_DARKER_PURPLE = RGB_FG_DARKER_PURPLE;
    public static final String FG_DEEP_PURPLE   = RGB_FG_DEEP_PURPLE;

    // ── True-RGB backgrounds ──────────────────────────────────────────────────
    // Dark neutrals
    public static final String RGB_BG_CHARCOAL      = "\u001B[48;2;35;38;46m";
    public static final String RGB_BG_DARK_SLATE    = "\u001B[48;2;28;32;42m";
    public static final String RGB_BG_MIDNIGHT      = "\u001B[48;2;16;18;26m";
    public static final String RGB_BG_NEAR_BLACK    = "\u001B[48;2;20;22;30m";
    public static final String RGB_BG_CARBON        = "\u001B[48;2;28;28;32m";
    public static final String RGB_BG_STEEL_DARK    = "\u001B[48;2;48;52;62m";
    // Warm dark
    public static final String RGB_BG_DARK_AMBER    = "\u001B[48;2;95;62;0m";
    public static final String RGB_BG_VIVID_AMBER   = "\u001B[48;2;170;105;0m";
    public static final String RGB_BG_RUST          = "\u001B[48;2;110;48;20m";
    public static final String RGB_BG_CRIMSON       = "\u001B[48;2;100;20;25m";
    public static final String RGB_BG_DARK_WINE     = "\u001B[48;2;90;16;36m";
    public static final String RGB_BG_MAROON        = "\u001B[48;2;110;22;22m";
    public static final String RGB_BG_ORANGE_VIVID  = "\u001B[48;2;200;100;0m";
    // Cool dark
    public static final String RGB_BG_DARK_FOREST   = "\u001B[48;2;18;52;28m";
    public static final String RGB_BG_DARK_OCEAN    = "\u001B[48;2;14;38;72m";
    public static final String RGB_BG_DARK_TEAL     = "\u001B[48;2;14;66;66m";
    public static final String RGB_BG_DARK_INDIGO   = "\u001B[48;2;28;24;68m";
    public static final String RGB_BG_DARK_PURPLE   = "\u001B[48;2;45;22;72m";
    public static final String RGB_BG_DARK_CYAN     = "\u001B[48;2;0;80;100m";
    // Bright / light
    public static final String RGB_BG_SOFT_WHITE    = "\u001B[48;2;238;240;245m";
    public static final String RGB_BG_LIGHT_GREY    = "\u001B[48;2;205;210;218m";
    public static final String RGB_BG_WARM_AMBER_LT = "\u001B[48;2;190;140;15m";
    public static final String RGB_BG_TOMATO_RED    = "\u001B[48;2;190;45;45m";
    // Vivid
    public static final String RGB_BG_HOT_PINK      = "\u001B[48;2;195;38;115m";
    public static final String RGB_BG_NEON_PURPLE   = "\u001B[48;2;115;18;175m";
    public static final String RGB_BG_ELECTRIC_TEAL = "\u001B[48;2;0;150;170m";
}

