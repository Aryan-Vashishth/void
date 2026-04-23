package core.logging.ansi;

/**
 * Stateless factory for ANSI SGR (Select Graphic Rendition) escape sequences.
 *
 * <p>This class is the single source of truth for <em>building</em> ANSI escape
 * strings. The companion {@link AnsiColors} class is a pure data catalog of
 * named, pre-built color constants and depends on this class.</p>
 *
 * <p>Three tiers are supported:</p>
 * <ol>
 *   <li><b>Standard 16-color</b> — {@code \u001B[<code>m}</li>
 *   <li><b>256-color</b> — {@code \u001B[38;5;<n>m} (FG) / {@code \u001B[48;5;<n>m} (BG)</li>
 *   <li><b>True RGB (24-bit)</b> — {@code \u001B[38;2;R;G;Bm} (FG) / {@code \u001B[48;2;R;G;Bm} (BG)</li>
 * </ol>
 */
public final class AnsiEscape {

    // ── SGR control sequences ─────────────────────────────────────────────────
    /** Resets all SGR attributes (color, weight, style). */
    public static final String RESET  = sgr(0);
    /** Bold / increased intensity. */
    public static final String BOLD   = sgr(1);
    /** Dim / decreased intensity (a.k.a. faint). */
    public static final String DIM    = sgr(2);
    /** Italic (not universally supported by all terminals). */
    public static final String ITALIC = sgr(3);

    private AnsiEscape() {}

    // ── Generic SGR builder ───────────────────────────────────────────────────

    /**
     * Build an arbitrary SGR escape sequence: {@code \u001B[<c1>;<c2>;…m}.
     *
     * <p>Use this for combinations not covered by the dedicated factories,
     * e.g. {@code sgr(38, 5, 208, 1)} → 256-color orange + bold.</p>
     *
     * @param codes one or more SGR parameters
     * @return ANSI escape sequence
     * @throws IllegalArgumentException if {@code codes} is empty
     */
    public static String sgr(int... codes) {
        if (codes == null || codes.length == 0) {
            throw new IllegalArgumentException("sgr() requires at least one code");
        }
        StringBuilder sb = new StringBuilder(8 + codes.length * 4).append("\u001B[");
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(codes[i]);
        }
        return sb.append('m').toString();
    }

    // ── Standard 16-color palette ─────────────────────────────────────────────

    public static String fg16(int code) {
        if (!((code >= 30 && code <= 37) || (code >= 90 && code <= 97))) {
            throw new IllegalArgumentException(
                    "16-color FG code must be in 30–37 or 90–97 (got " + code + ")");
        }
        return sgr(code);
    }

    public static String bg16(int code) {
        if (!((code >= 40 && code <= 47) || (code >= 100 && code <= 107))) {
            throw new IllegalArgumentException(
                    "16-color BG code must be in 40–47 or 100–107 (got " + code + ")");
        }
        return sgr(code);
    }

    // ── True RGB (24-bit) ─────────────────────────────────────────────────────

    public static String rgbFg(int r, int g, int b) {
        validateRgb(r, g, b);
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m";
    }

    public static String rgbBg(int r, int g, int b) {
        validateRgb(r, g, b);
        return "\u001B[48;2;" + r + ";" + g + ";" + b + "m";
    }

    public static String rgbFg(int rgb) {
        return rgbFg((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    public static String rgbBg(int rgb) {
        return rgbBg((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    // ── 256-color palette ─────────────────────────────────────────────────────

    public static String fg256(int code) {
        validate256(code);
        return "\u001B[38;5;" + code + "m";
    }

    public static String bg256(int code) {
        validate256(code);
        return "\u001B[48;5;" + code + "m";
    }

    // ── Composition helpers ───────────────────────────────────────────────────

    public static String colorize(String text, String ansi) {
        return ansi + text + RESET;
    }

    public static String colorize(String text, String fg, String bg) {
        return fg + bg + text + RESET;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private static void validateRgb(int r, int g, int b) {
        if ((r | g | b) < 0 || r > 255 || g > 255 || b > 255) {
            throw new IllegalArgumentException(
                    "RGB components must be in 0–255 (got r=" + r + ", g=" + g + ", b=" + b + ")");
        }
    }

    private static void validate256(int code) {
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException(
                    "256-color code must be in 0–255 (got " + code + ")");
        }
    }
}

