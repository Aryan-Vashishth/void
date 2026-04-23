package core.logging;

import core.logging.config.LogConfig;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * Unit tests for the pad() utility and LogConfig column-width integration.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Covers:
 *   1. pad() core behaviour — exact width, under-width, zero-width, null
 *   2. LogConfig defaults  — all widths start at 0 (free-flow)
 *   3. Runtime setters     — setTsWidth / setLevelWidth / setActionWidth
 *   4. Builder overrides   — explicit widths via LogConfig.builder()
 *   5. LogConfig.patch()   — live mutation without rebuild
 *   6. apply() swap        — full config swap mid-test
 *   7. Column alignment    — formatted lines share the same visible prefix length
 */
public class LogPaddingTest {

    // ── Snapshot / restore live config around every test ──────────────────────

    private int savedTs, savedLevel, savedAction;

    @BeforeMethod
    public void snapshot() {
        savedTs     = LogConfig.current().getTsWidth();
        savedLevel  = LogConfig.current().getLevelWidth();
        savedAction = LogConfig.current().getActionWidth();
    }

    @AfterMethod
    public void restore() {
        LogConfig.current()
                 .setTsWidth(savedTs)
                 .setLevelWidth(savedLevel)
                 .setActionWidth(savedAction);
    }

    // ── Convenience wrapper (mirrors prototype) ───────────────────────────────

    private static String pad(String text, int width) {
        return LogFormatterPrototypeTest.pad(text, width);
    }

    // =========================================================================
    // 1. pad() core behaviour
    // =========================================================================

    @Test(description = "Exact-fit text fills the column with no trailing spaces")
    public void pad_exactFit() {
        String result = pad("INFO", 4);
        assertEquals(result, "INFO");
        assertEquals(result.length(), 4);
    }

    @Test(description = "Short text is right-padded with spaces to the requested width")
    public void pad_shortText_paddedToWidth() {
        String result = pad("OK", 7);
        assertEquals(result, "OK     ");
        assertEquals(result.length(), 7);
    }

    @Test(description = "Text longer than width overflows — no truncation (data is never lost)")
    public void pad_overflow_noTruncation() {
        String result = pad("VERY_LONG_ACTION_LABEL", 10);
        assertEquals(result, "VERY_LONG_ACTION_LABEL");
        assertTrue(result.length() > 10, "overflow text must be longer than requested width");
    }

    @Test(description = "width=0 returns the text as-is (free-flow mode)")
    public void pad_zeroWidth_freeFlow() {
        String text   = "CLICK [>]";
        String result = pad(text, 0);
        assertEquals(result, text);
    }

    @Test(description = "Negative width is treated the same as zero (free-flow)")
    public void pad_negativeWidth_freeFlow() {
        String text   = "INPUT [>>]";
        String result = pad(text, -5);
        assertEquals(result, text);
    }

    @Test(description = "null input is treated as empty string, then padded")
    public void pad_nullInput_treatedAsEmpty() {
        String result = pad(null, 5);
        assertEquals(result, "     ");
        assertEquals(result.length(), 5);
    }

    @Test(description = "null input with width=0 returns empty string")
    public void pad_nullInput_zeroWidth() {
        assertEquals(pad(null, 0), "");
    }

    @Test(description = "Empty string with positive width produces spaces only")
    public void pad_emptyString_spacesOnly() {
        String result = pad("", 6);
        assertEquals(result, "      ");
        assertEquals(result.length(), 6);
    }

    // =========================================================================
    // 2. pad() with various widths — data-driven
    // =========================================================================

    @DataProvider(name = "padCases")
    public Object[][] padCases() {
        return new Object[][] {
            // text,          width, expectedLength, expectedStartsWith
            { "INFO",         7,     7,              "INFO"       },
            { "WARN",         7,     7,              "WARN"       },
            { "ERROR",        7,     7,              "ERROR"      },
            { "DEBUG",        7,     7,              "DEBUG"      },
            { "CLICK [>]",   18,    18,              "CLICK [>]"  },
            { "INPUT [>>]",  18,    18,              "INPUT [>>]" },
            { "FALLBACK [<-]",18,   18,              "FALLBACK [<-]" }, // overflow — length > 18
            { "2026-04-21 13:00:00.000", 23, 23,    "2026-04-21" },
        };
    }

    @Test(dataProvider = "padCases",
          description  = "Parametric: pad() produces correct length and prefix for common log columns")
    public void pad_parametric(String text, int width, int expectedLen, String expectedPrefix) {
        String result = pad(text, width);
        assertTrue(result.startsWith(expectedPrefix),
                "Expected prefix '" + expectedPrefix + "' in '" + result + "'");
        // For overflow cases the result will be longer than width — that's intentional
        if (text.length() <= width) {
            assertEquals(result.length(), expectedLen,
                    "Padded length mismatch for '" + text + "' at width " + width);
        }
    }

    // =========================================================================
    // 3. LogConfig defaults — widths start at 0
    // =========================================================================

    @Test(description = "Default tsWidth is 0 (free-flow, no padding)")
    public void logConfig_defaultTsWidth_isZero() {
        LogConfig fresh = LogConfig.builder().build();
        assertEquals(fresh.getTsWidth(), 0);
    }

    @Test(description = "Default levelWidth is 0 (free-flow)")
    public void logConfig_defaultLevelWidth_isZero() {
        assertEquals(LogConfig.builder().build().getLevelWidth(), 0);
    }

    @Test(description = "Default actionWidth is 0 (free-flow)")
    public void logConfig_defaultActionWidth_isZero() {
        assertEquals(LogConfig.builder().build().getActionWidth(), 0);
    }

    @Test(description = "Default tableCellLimit is OFF (unlimited)")
    public void logConfig_defaultTableCellLimit_isDisabled() {
        assertFalse(LogConfig.builder().build().isTableCellLimitEnabled());
    }

    // =========================================================================
    // 4. Runtime setters
    // =========================================================================

    @Test(description = "setTsWidth() changes the width reported by getTsWidth()")
    public void setter_tsWidth() {
        LogConfig.current().setTsWidth(23);
        assertEquals(LogConfig.current().getTsWidth(), 23);
    }

    @Test(description = "setLevelWidth() changes the width reported by getLevelWidth()")
    public void setter_levelWidth() {
        LogConfig.current().setLevelWidth(7);
        assertEquals(LogConfig.current().getLevelWidth(), 7);
    }

    @Test(description = "setActionWidth() changes the width reported by getActionWidth()")
    public void setter_actionWidth() {
        LogConfig.current().setActionWidth(18);
        assertEquals(LogConfig.current().getActionWidth(), 18);
    }

    @Test(description = "Setting width to 0 restores free-flow mode")
    public void setter_resetToZero_freeFlow() {
        LogConfig.current().setActionWidth(18);
        LogConfig.current().setActionWidth(0);
        assertEquals(LogConfig.current().getActionWidth(), 0);

        // pad() with that width should return text unchanged
        String text = "CLICK [>]";
        assertEquals(pad(text, LogConfig.current().getActionWidth()), text);
    }

    @Test(description = "Setters return 'this' for chaining")
    public void setter_chainingWorks() {
        LogConfig cfg = LogConfig.current()
                .setTsWidth(23)
                .setLevelWidth(7)
                .setActionWidth(18);
        assertSame(cfg, LogConfig.current(), "Chained setters must return the same instance");
        assertEquals(cfg.getTsWidth(),    23);
        assertEquals(cfg.getLevelWidth(), 7);
        assertEquals(cfg.getActionWidth(),18);
    }

    // =========================================================================
    // 5. Builder overrides
    // =========================================================================

    @Test(description = "Builder.tsWidth() sets the width correctly")
    public void builder_tsWidth() {
        LogConfig cfg = LogConfig.builder().tsWidth(23).build();
        assertEquals(cfg.getTsWidth(), 23);
    }

    @Test(description = "Builder.levelWidth() sets the width correctly")
    public void builder_levelWidth() {
        LogConfig cfg = LogConfig.builder().levelWidth(7).build();
        assertEquals(cfg.getLevelWidth(), 7);
    }

    @Test(description = "Builder.actionWidth() sets the width correctly")
    public void builder_actionWidth() {
        LogConfig cfg = LogConfig.builder().actionWidth(18).build();
        assertEquals(cfg.getActionWidth(), 18);
    }

    @Test(description = "Builder.tableCellLimit() enables truncation with given limit")
    public void builder_tableCellLimit_enablesTruncation() {
        LogConfig cfg = LogConfig.builder().tableCellLimit(40).tableCellLimitEnabled(true).build();
        assertTrue(cfg.isTableCellLimitEnabled());
        assertEquals(cfg.getTableCellLimit(), 40);
    }

    @Test(description = "Builder.noTableCellLimit() disables truncation")
    public void builder_noTableCellLimit() {
        LogConfig cfg = LogConfig.builder().noTableCellLimit().build();
        assertFalse(cfg.isTableCellLimitEnabled());
    }

    // =========================================================================
    // 6. LogConfig.patch() — live mutation
    // =========================================================================

    @Test(description = "patch() mutates the live config in-place")
    public void patch_mutatesLiveConfig() {
        LogConfig before = LogConfig.current();
        LogConfig.patch(c -> c.setTsWidth(19).setLevelWidth(5).setActionWidth(22));

        assertSame(LogConfig.current(), before, "patch() must not swap the instance");
        assertEquals(LogConfig.current().getTsWidth(),    19);
        assertEquals(LogConfig.current().getLevelWidth(), 5);
        assertEquals(LogConfig.current().getActionWidth(),22);
    }

    @Test(description = "apply() replaces the live config with a new instance")
    public void apply_replacesInstance() {
        LogConfig original = LogConfig.current();
        LogConfig fresh    = LogConfig.builder().tsWidth(23).levelWidth(7).actionWidth(18).build();
        fresh.apply();

        assertNotSame(LogConfig.current(), original);
        assertSame(LogConfig.current(), fresh);
        assertEquals(LogConfig.current().getTsWidth(), 23);
    }

    // =========================================================================
    // 7. Column alignment — formatted lines have identical visible column widths
    // =========================================================================

    @Test(description = "When widths are set, all lines align at the same column boundaries")
    public void alignment_fixedWidths_linesAlignAtSameBoundary() {
        LogConfig.patch(c -> c.setTsWidth(23).setLevelWidth(7).setActionWidth(18));

        String ts     = "2026-04-21 13:00:00.000";
        String[] rows = {
            pad(ts, 23) + " | " + pad("INFO",  7) + " | " + pad("CLICK [>]",    18) + " | Short message",
            pad(ts, 23) + " | " + pad("WARN",  7) + " | " + pad("FALLBACK [<-]",18) + " | A much longer message text",
            pad(ts, 23) + " | " + pad("ERROR", 7) + " | " + pad("TIMEOUT [!!]", 18) + " | Element not found",
        };

        // All rows must share the same visible prefix up to (but not including) the message
        int prefixLen = (23 + 3 + 7 + 3 + 18 + 3); // ts + " | " + level + " | " + action + " | "
        for (String row : rows) {
            assertTrue(row.length() >= prefixLen,
                    "Row too short: '" + row + "'");
            // The first prefixLen characters must be the same across all rows for TS column
            assertEquals(row.substring(0, 23), pad(ts, 23),
                    "Timestamp column not aligned in: " + row);
        }
        System.out.println("\n── Alignment verification ──────────────────────────────────────────────");
        for (String row : rows) System.out.println(row);
    }

    @Test(description = "When widths are 0, lines have no padding and columns are free-flow")
    public void alignment_zeroWidths_noExtraSpaces() {
        LogConfig.patch(c -> c.setTsWidth(0).setLevelWidth(0).setActionWidth(0));

        String level  = "INFO";
        String action = "CLICK [>]";
        String result = pad(level, 0) + " | " + pad(action, 0);

        // No trailing spaces added
        assertTrue(result.startsWith("INFO | CLICK [>]"),
                "Free-flow should not add spaces: '" + result + "'");
        assertFalse(result.startsWith("INFO    "),
                "Free-flow must not pad level with spaces");
    }

    // =========================================================================
    // 8. truncateCell() — via LogConfig
    // =========================================================================

    @Test(description = "truncateCell() is a pass-through when limit is disabled (default)")
    public void truncateCell_disabled_returnsFullString() {
        LogConfig cfg = LogConfig.builder().noTableCellLimit().build();
        String longText = "A".repeat(200);
        assertEquals(cfg.truncateCell(longText), longText);
    }

    @Test(description = "truncateCell() truncates when limit is enabled")
    public void truncateCell_enabled_truncatesWithEllipsis() {
        LogConfig cfg = LogConfig.builder().tableCellLimit(10).tableCellLimitEnabled(true).build();
        String result = cfg.truncateCell("Hello World Extended");
        assertEquals(result.length(), 10);
        assertTrue(result.endsWith("..."));
    }

    @Test(description = "truncateCell() leaves short strings untouched")
    public void truncateCell_enabled_shortStringUnchanged() {
        LogConfig cfg = LogConfig.builder().tableCellLimit(40).tableCellLimitEnabled(true).build();
        assertEquals(cfg.truncateCell("Short"), "Short");
    }

    @Test(description = "truncateCell() collapses newlines to spaces regardless of limit setting")
    public void truncateCell_collapsesNewlines() {
        LogConfig cfg = LogConfig.builder().noTableCellLimit().build();
        // \n → space, \r\n → single space (\\R matches the whole CRLF sequence)
        assertEquals(cfg.truncateCell("line1\nline2\r\nline3"), "line1 line2 line3");
    }

    @Test(description = "truncateCell() on null returns empty string")
    public void truncateCell_null_returnsEmpty() {
        assertEquals(LogConfig.current().truncateCell(null), "");
    }
}

