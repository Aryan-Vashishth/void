package core.logging.ansi;

import static core.logging.ansi.AnsiEscape.bg16;
import static core.logging.ansi.AnsiEscape.bg256;
import static core.logging.ansi.AnsiEscape.fg16;
import static core.logging.ansi.AnsiEscape.fg256;
import static core.logging.ansi.AnsiEscape.rgbBg;
import static core.logging.ansi.AnsiEscape.rgbFg;
import static core.logging.ansi.AnsiEscape.sgr;

/**
 * Catalog of named ANSI color and style constants.
 *
 * <p>This class is intentionally <strong>data only</strong>: it contains no
 * behavior. All escape-sequence construction lives in {@link AnsiEscape}.</p>
 */
public final class AnsiColors {

    private AnsiColors() {}

    // ── Control sequences (delegated to AnsiEscape — single source of truth) ─
    public static final String RESET  = AnsiEscape.RESET;
    public static final String BOLD   = AnsiEscape.BOLD;
    public static final String DIM    = AnsiEscape.DIM;
    public static final String ITALIC = AnsiEscape.ITALIC;

    // ── Standard 16-color foregrounds ─────────────────────────────────────────
    public static final String FG_BLACK          = fg16(30);
    public static final String FG_RED            = fg16(31);
    public static final String FG_GREEN          = fg16(32);
    public static final String FG_YELLOW         = fg16(33);
    public static final String FG_BLUE           = fg16(34);
    public static final String FG_MAGENTA        = fg16(35);
    public static final String FG_CYAN           = fg16(36);
    public static final String FG_WHITE          = fg16(37);
    public static final String FG_BRIGHT_BLACK   = fg16(90);
    public static final String FG_BRIGHT_RED     = fg16(91);
    public static final String FG_BRIGHT_GREEN   = fg16(92);
    public static final String FG_BRIGHT_YELLOW  = fg16(93);
    public static final String FG_BRIGHT_BLUE    = fg16(94);
    public static final String FG_BRIGHT_MAGENTA = fg16(95);
    public static final String FG_BRIGHT_CYAN    = fg16(96);
    public static final String FG_BRIGHT_WHITE   = fg16(97);
    /** @deprecated Use {@link #FG_BRIGHT_BLACK} */
    @Deprecated
    public static final String FG_DIM_WHITE      = sgr(37, 2);

    // ── Standard 16-color backgrounds ─────────────────────────────────────────
    public static final String BG_BLACK          = bg16(40);
    public static final String BG_RED            = bg16(41);
    public static final String BG_GREEN          = bg16(42);
    public static final String BG_YELLOW         = bg16(43);
    public static final String BG_BLUE           = bg16(44);
    public static final String BG_MAGENTA        = bg16(45);
    public static final String BG_CYAN           = bg16(46);
    public static final String BG_WHITE          = bg16(47);
    public static final String BG_BRIGHT_BLACK   = bg16(100);
    public static final String BG_BRIGHT_RED     = bg16(101);
    public static final String BG_BRIGHT_GREEN   = bg16(102);
    public static final String BG_BRIGHT_YELLOW  = bg16(103);
    public static final String BG_BRIGHT_BLUE    = bg16(104);
    public static final String BG_BRIGHT_MAGENTA = bg16(105);
    public static final String BG_BRIGHT_CYAN    = bg16(106);
    public static final String BG_BRIGHT_WHITE   = bg16(107);
    public static final String BG_GREY_100       = BG_BRIGHT_BLACK;
    public static final String BG_BRIGHT_GREY    = BG_WHITE;

    // ── 256-color foregrounds ─────────────────────────────────────────────────
    public static final String FG_256_ORANGE     = fg256(208);
    public static final String FG_256_GOLD       = fg256(220);
    public static final String FG_256_LIME       = fg256(118);
    public static final String FG_256_SKY        = fg256(117);
    public static final String FG_256_VIOLET     = fg256(135);
    public static final String FG_256_PINK       = fg256(205);
    public static final String FG_256_TEAL       = fg256(80);
    public static final String FG_256_SALMON     = fg256(209);
    public static final String FG_256_GREY_DARK  = fg256(240);
    public static final String FG_256_GREY_MID   = fg256(246);
    public static final String FG_256_GREY_LIGHT = fg256(252);
    public static final String FG_256_NAVY       = fg256(17);
    public static final String FG_256_MAROON     = fg256(88);
    public static final String FG_256_OLIVE      = fg256(100);
    public static final String FG_256_INDIGO     = fg256(54);
    public static final String FG_ORANGE_208      = FG_256_ORANGE;
    public static final String FG_BOLD_ORANGE_208 = sgr(38, 5, 208, 1);
    public static final String FG_NAVY_BLUE       = FG_256_NAVY;

    // ── 256-color backgrounds ─────────────────────────────────────────────────
    public static final String BG_256_ORANGE      = bg256(208);
    public static final String BG_256_DARK_GREY   = bg256(235);
    public static final String BG_256_MID_GREY    = bg256(238);
    public static final String BG_256_NAVY        = bg256(17);
    public static final String BG_256_DARK_GREEN  = bg256(22);
    public static final String BG_256_DARK_TEAL   = bg256(23);
    public static final String BG_256_DARK_PURPLE = bg256(53);
    public static final String BG_256_DARK_OLIVE  = bg256(94);
    public static final String BG_256_DARK_RED    = bg256(52);
    public static final String BG_256_MAROON      = bg256(88);
    public static final String BG_256_INDIGO      = bg256(54);
    public static final String BG_ORANGE_208     = BG_256_ORANGE;
    public static final String BG_DARKER_GREY    = BG_256_DARK_GREY;
    public static final String BG_DARKER_BLUE    = BG_256_NAVY;
    public static final String BG_DARKER_GREEN   = BG_256_DARK_GREEN;
    public static final String BG_DARKER_MAGENTA = BG_256_DARK_PURPLE;
    public static final String BG_DARKER_YELLOW  = BG_256_DARK_OLIVE;
    public static final String BG_DARKER_RED     = BG_256_DARK_RED;
    public static final String BG_MAROON_RED     = BG_256_MAROON;

    // ── True-RGB foregrounds ──────────────────────────────────────────────────
    public static final String RGB_FG_SNOW_WHITE    = rgbFg(240, 242, 245);
    public static final String RGB_FG_SOFT_WHITE    = rgbFg(210, 215, 220);
    public static final String RGB_FG_COOL_GREY     = rgbFg(140, 150, 165);
    public static final String RGB_FG_WARM_GREY     = rgbFg(160, 158, 150);
    public static final String RGB_FG_DEEP_CHARCOAL = rgbFg(28, 30, 38);
    public static final String RGB_FG_GOLD          = rgbFg(255, 200, 50);
    public static final String RGB_FG_AMBER         = rgbFg(255, 170, 0);
    public static final String RGB_FG_PEACH         = rgbFg(255, 185, 110);
    public static final String RGB_FG_CORAL         = rgbFg(255, 105, 85);
    public static final String RGB_FG_SALMON        = rgbFg(255, 140, 105);
    public static final String RGB_FG_HOT_PINK      = rgbFg(255, 75, 170);
    public static final String RGB_FG_SKY_BLUE      = rgbFg(80, 185, 255);
    public static final String RGB_FG_ELECTRIC_BLUE = rgbFg(50, 140, 255);
    public static final String RGB_FG_STEEL_CYAN    = rgbFg(90, 220, 220);
    public static final String RGB_FG_MINT          = rgbFg(60, 220, 175);
    public static final String RGB_FG_LIME_GREEN    = rgbFg(100, 240, 120);
    public static final String RGB_FG_NEON_GREEN    = rgbFg(80, 255, 120);
    public static final String RGB_FG_LAVENDER      = rgbFg(185, 155, 255);
    public static final String RGB_FG_VIOLET        = rgbFg(160, 100, 255);
    public static final String RGB_FG_PURPLE        = rgbFg(155, 111, 224);
    public static final String RGB_FG_DARKER_PURPLE = rgbFg(122, 79, 196);
    public static final String RGB_FG_DEEP_PURPLE   = rgbFg(90, 54, 163);
    public static final String FG_PURPLE        = RGB_FG_PURPLE;
    public static final String FG_DARKER_PURPLE = RGB_FG_DARKER_PURPLE;
    public static final String FG_DEEP_PURPLE   = RGB_FG_DEEP_PURPLE;

    // ── Deep-shade foregrounds ────────────────────────────────────────────────
    public static final String RGB_FG_DEEP_RED    = rgbFg(210, 48, 48);
    public static final String RGB_FG_DEEP_GREEN  = rgbFg(38, 185, 72);
    public static final String RGB_FG_DEEP_CYAN   = rgbFg(28, 188, 196);
    public static final String RGB_FG_DEEP_AMBER  = rgbFg(200, 155, 20);
    public static final String RGB_FG_DEEP_VIOLET = rgbFg(152, 88, 210);
    public static final String RGB_FG_DEEP_ORANGE = rgbFg(205, 88, 18);
    public static final String RGB_FG_DEEP_PINK   = rgbFg(210, 40, 125);

    // ── True-RGB backgrounds ──────────────────────────────────────────────────
    public static final String RGB_BG_CHARCOAL      = rgbBg(35, 38, 46);
    public static final String RGB_BG_DARK_SLATE    = rgbBg(28, 32, 42);
    public static final String RGB_BG_MIDNIGHT      = rgbBg(16, 18, 26);
    public static final String RGB_BG_NEAR_BLACK    = rgbBg(20, 22, 30);
    public static final String RGB_BG_CARBON        = rgbBg(28, 28, 32);
    public static final String RGB_BG_STEEL_DARK    = rgbBg(48, 52, 62);
    public static final String RGB_BG_DARK_AMBER    = rgbBg(95, 62, 0);
    public static final String RGB_BG_VIVID_AMBER   = rgbBg(170, 105, 0);
    public static final String RGB_BG_RUST          = rgbBg(110, 48, 20);
    public static final String RGB_BG_CRIMSON       = rgbBg(100, 20, 25);
    public static final String RGB_BG_DARK_WINE     = rgbBg(90, 16, 36);
    public static final String RGB_BG_MAROON        = rgbBg(110, 22, 22);
    public static final String RGB_BG_ORANGE_VIVID  = rgbBg(200, 100, 0);
    public static final String RGB_BG_DARK_FOREST   = rgbBg(18, 52, 28);
    public static final String RGB_BG_DARK_OCEAN    = rgbBg(14, 38, 72);
    public static final String RGB_BG_DARK_TEAL     = rgbBg(14, 66, 66);
    public static final String RGB_BG_DARK_INDIGO   = rgbBg(28, 24, 68);
    public static final String RGB_BG_DARK_PURPLE   = rgbBg(45, 22, 72);
    public static final String RGB_BG_DARK_CYAN     = rgbBg(0, 80, 100);
    public static final String RGB_BG_SOFT_WHITE    = rgbBg(238, 240, 245);
    public static final String RGB_BG_LIGHT_GREY    = rgbBg(205, 210, 218);
    public static final String RGB_BG_WARM_AMBER_LT = rgbBg(190, 140, 15);
    public static final String RGB_BG_TOMATO_RED    = rgbBg(190, 45, 45);
    public static final String RGB_BG_HOT_PINK      = rgbBg(195, 38, 115);
    public static final String RGB_BG_NEON_PURPLE   = rgbBg(115, 18, 175);
    public static final String RGB_BG_ELECTRIC_TEAL = rgbBg(0, 150, 170);
}

