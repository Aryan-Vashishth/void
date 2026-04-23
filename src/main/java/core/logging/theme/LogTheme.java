package core.logging.theme;

/**
 * Enumeration of all built-in log themes.
 *
 * <p>Pass to {@code CustomLogger.setTheme(LogTheme)} to switch the active color scheme.</p>
 */
public enum LogTheme {

    /** ★ Default. Standard 16-color ANSI. */
    PLAIN,

    /** Solarized Dark — reduced eye-strain. */
    SOLARIZED_DARK,

    /** Maximum contrast — pure blacks + maximum-luminance foregrounds. */
    HIGH_CONTRAST,

    INDUSTRIAL_STEEL,
    NIGHT_CLUB,
    CARBON_ORANGE,
    MODERN_CLEAN,
    COCKPIT
}

