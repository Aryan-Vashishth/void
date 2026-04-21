package core.logging;


/**
 * Enumeration of all built-in log themes.
 *
 * <p>Pass to {@code CustomLogger.setTheme(LogTheme)} to switch the active color scheme.</p>
 */
public enum LogTheme {

    // ── Practical / everyday ──────────────────────────────────────────────────
    /**
     * ★ Default. Standard 16-color ANSI. Renders correctly in every terminal,
     * CI runner, and file viewer. Boring on purpose — zero surprises.
     */
    PLAIN,

    /**
     * Ethan Schoonover's Solarized Dark palette. Reduced eye-strain for long
     * automation sessions; excellent readability on dark terminals.
     */
    SOLARIZED_DARK,

    /**
     * Maximum contrast — pure blacks + maximum-luminance foregrounds.
     * Ideal for accessibility or low-quality display / projector use.
     */
    HIGH_CONTRAST,

    // ── Stylised ─────────────────────────────────────────────────────────────
    INDUSTRIAL_STEEL,
    NIGHT_CLUB,
    CARBON_ORANGE,
    MODERN_CLEAN,
    COCKPIT
}

