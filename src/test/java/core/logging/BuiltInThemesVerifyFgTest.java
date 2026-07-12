package core.logging;

import core.logging.intent.LogIntent;
import core.logging.theme.BuiltInThemes;
import core.logging.theme.LogTheme;
import core.logging.theme.ThemeColors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Asserts that every built-in theme exposes a non-null, non-empty {@code verifyFg}
 * and that {@link ThemeColors#resolve(String, LogIntent)} handles {@link LogIntent#VERIFY}
 * correctly for all themes and all log levels.
 *
 * These tests guard against the builder omitting {@code verifyFg} (which would silently
 * inherit the BOLD default from {@link ThemeColors.Builder}) and against any future theme
 * being added without setting the field.
 */
public class BuiltInThemesVerifyFgTest {

    // ── DataProviders ─────────────────────────────────────────────────────────

    @DataProvider(name = "allThemes")
    public Object[][] allThemes() {
        return new Object[][]{
            {LogTheme.PLAIN,            BuiltInThemes.PLAIN},
            {LogTheme.SOLARIZED_DARK,   BuiltInThemes.SOLARIZED_DARK},
            {LogTheme.HIGH_CONTRAST,    BuiltInThemes.HIGH_CONTRAST},
            {LogTheme.MODERN_CLEAN,     BuiltInThemes.MODERN_CLEAN},
            {LogTheme.COCKPIT,          BuiltInThemes.COCKPIT},
            {LogTheme.INDUSTRIAL_STEEL, BuiltInThemes.INDUSTRIAL_STEEL},
            {LogTheme.NIGHT_CLUB,       BuiltInThemes.NIGHT_CLUB},
            {LogTheme.CARBON_ORANGE,    BuiltInThemes.CARBON_ORANGE},
        };
    }

    @DataProvider(name = "themesAndLevels")
    public Object[][] themesAndLevels() {
        LogTheme[] themes = LogTheme.values();
        String[]   levels = {"INFO", "WARN", "ERROR", "DEBUG"};
        Object[][] data = new Object[themes.length * levels.length][2];
        int i = 0;
        for (LogTheme theme : themes) {
            for (String level : levels) {
                data[i++] = new Object[]{theme, level};
            }
        }
        return data;
    }

    /** Resolves a ThemeColors from a LogTheme using {@link BuiltInThemes#resolve(LogTheme)}. */
    private static ThemeColors colorsFor(LogTheme theme) {
        return BuiltInThemes.resolve(theme);
    }

    // ── verifyFg is non-null in every theme ───────────────────────────────────

    @Test(description = "Every built-in theme has a non-null verifyFg",
          dataProvider = "allThemes")
    public void verifyFg_isNotNull(LogTheme theme, ThemeColors colors) {
        assertNotNull(colors.verifyFg(),
                theme.name() + ".verifyFg() must not be null");
    }

    @Test(description = "Every built-in theme has a non-empty verifyFg",
          dataProvider = "allThemes")
    public void verifyFg_isNotEmpty(LogTheme theme, ThemeColors colors) {
        assertFalse(colors.verifyFg().isEmpty(),
                theme.name() + ".verifyFg() must not be empty");
    }

    // ── resolve() returns non-null for VERIFY at every level in every theme ────

    @Test(description = "resolve(level, VERIFY) returns non-null for every theme+level combination",
          dataProvider = "themesAndLevels")
    public void resolve_verify_isNotNull(LogTheme theme, String level) {
        ThemeColors colors = BuiltInThemes.resolve(theme);
        String result = colors.resolve(level, LogIntent.VERIFY);
        assertNotNull(result, theme.name() + " at level " + level + " returned null for VERIFY");
    }

    @Test(description = "resolve(level, VERIFY) returns non-empty for every theme+level combination",
          dataProvider = "themesAndLevels")
    public void resolve_verify_isNotEmpty(LogTheme theme, String level) {
        ThemeColors colors = BuiltInThemes.resolve(theme);
        String result = colors.resolve(level, LogIntent.VERIFY);
        assertFalse(result.isEmpty(),
                theme.name() + " at level " + level + " returned empty string for VERIFY");
    }

    // ── VERIFY differs from OBSERVE in every theme ────────────────────────────

    @Test(description = "VERIFY and OBSERVE resolve to different colors in every theme — they must be visually distinct",
          dataProvider = "allThemes")
    public void verifyFg_distinctFromObserveFg(LogTheme theme, ThemeColors colors) {
        String verifyColor  = colors.resolve("INFO", LogIntent.VERIFY);
        String observeColor = colors.resolve("INFO", LogIntent.OBSERVE);
        assertNotEquals(verifyColor, observeColor,
                theme.name() + ": VERIFY and OBSERVE should not map to the same color");
    }

    // ── resolve() handles all 8 intents without exception ─────────────────────

    @Test(description = "resolve() does not throw for any intent in any built-in theme",
          dataProvider = "allThemes")
    public void resolve_allIntents_doesNotThrow(LogTheme theme, ThemeColors colors) {
        for (LogIntent intent : LogIntent.values()) {
            // If resolve() throws, TestNG will record the failure with full context
            String result = colors.resolve("INFO", intent);
            assertNotNull(result, theme.name() + " returned null for intent " + intent);
        }
    }

    // ── getColors(LogTheme) returns the correct singleton ─────────────────────

    @Test(description = "BuiltInThemes.resolve(PLAIN) returns the PLAIN singleton")
    public void resolve_plain_returnsSingleton() {
        assertSame(BuiltInThemes.resolve(LogTheme.PLAIN), BuiltInThemes.PLAIN);
    }

    @Test(description = "BuiltInThemes.resolve(SOLARIZED_DARK) returns the SOLARIZED_DARK singleton")
    public void resolve_solarizedDark_returnsSingleton() {
        assertSame(BuiltInThemes.resolve(LogTheme.SOLARIZED_DARK), BuiltInThemes.SOLARIZED_DARK);
    }

    @Test(description = "BuiltInThemes.resolve(null) falls back to PLAIN without throwing")
    public void resolve_null_fallsBackToPlain() {
        ThemeColors result = BuiltInThemes.resolve(null);
        assertSame(result, BuiltInThemes.PLAIN);
    }
}
