package elements.api;

import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SwitchLocatorFamily} defaults.
 */
public class SwitchLocatorFamilyTest {

    // ── Test page interfaces ───────────────────────────────────────────────

    interface ReportsPage {

        /**
         * Demonstrates the canonical SwitchLocatorFamily pattern.
         * All constants have explicit values via exhaustive switch.
         * KPI_SUMMARY and YTD_ANALYSIS would produce wrong acronyms via auto-derive.
         */
        enum Sections implements Clickable, SwitchLocatorFamily {
            OVERVIEW,
            KPI_SUMMARY,          // auto-derive would give "Kpi Summary" — switch gives "KPI Summary"
            VENDOR_PERFORMANCE,
            YTD_ANALYSIS;         // auto-derive would give "Ytd Analysis" — switch gives "YTD Analysis"

            @Override public String getPrimaryLocator() { return SwitchLocatorFamily.super.getPrimaryLocator(); }
            @Override public String getTriggerLocator() { return getPrimaryLocator(); }

            @Override
            public String getSemanticValue() {
                return switch (this) {
                    case OVERVIEW           -> "Overview";
                    case KPI_SUMMARY        -> "KPI Summary";
                    case VENDOR_PERFORMANCE -> "Vendor Performance";
                    case YTD_ANALYSIS       -> "YTD Analysis";
                };
            }
        }

        /** ReadOnly variant — verifies the pattern works with a different capability. */
        enum Labels implements ReadOnly, SwitchLocatorFamily {
            STATUS_BADGE,
            LAST_UPDATED;

            @Override public String getPrimaryLocator() { return SwitchLocatorFamily.super.getPrimaryLocator(); }
            @Override public String getTextLocator()    { return getPrimaryLocator(); }

            @Override
            public String getSemanticValue() {
                return switch (this) {
                    case STATUS_BADGE  -> "Status";
                    case LAST_UPDATED  -> "Last Updated";
                };
            }
        }
    }

    // ── getPrimaryLocator — inherited from LocatorFamily ──────────────────

    @Test
    public void getPrimaryLocator_allConstantsShareFamilyKey() {
        String expected = "ReportsPage.Sections";
        for (ReportsPage.Sections s : ReportsPage.Sections.values()) {
            assertEquals(s.getPrimaryLocator(), expected,
                    "Constant " + s.name() + " should share the family key");
        }
    }

    @Test
    public void getPrimaryLocator_doesNotContainConstantName() {
        assertFalse(ReportsPage.Sections.KPI_SUMMARY.getPrimaryLocator().contains("KPI_SUMMARY"));
        assertFalse(ReportsPage.Sections.YTD_ANALYSIS.getPrimaryLocator().contains("YTD_ANALYSIS"));
    }

    @Test
    public void getPrimaryLocator_readOnlyVariant_returnsCorrectFamilyKey() {
        assertEquals(ReportsPage.Labels.STATUS_BADGE.getPrimaryLocator(), "ReportsPage.Labels");
    }

    // ── getSemanticValue ──────────────────────────────────────────────────

    @Test
    public void getSemanticValue_returnsExplicitMappingForAllConstants() {
        assertEquals(ReportsPage.Sections.OVERVIEW.getSemanticValue(),           "Overview");
        assertEquals(ReportsPage.Sections.KPI_SUMMARY.getSemanticValue(),        "KPI Summary");
        assertEquals(ReportsPage.Sections.VENDOR_PERFORMANCE.getSemanticValue(), "Vendor Performance");
        assertEquals(ReportsPage.Sections.YTD_ANALYSIS.getSemanticValue(),       "YTD Analysis");
    }

    @Test
    public void getSemanticValue_acronym_differFromAutoDerive() {
        // Confirms the switch provides the corrected acronym form, not the word-transform default
        String autoKpi = "Kpi Summary";
        String autoYtd = "Ytd Analysis";
        assertNotEquals(ReportsPage.Sections.KPI_SUMMARY.getSemanticValue(), autoKpi);
        assertNotEquals(ReportsPage.Sections.YTD_ANALYSIS.getSemanticValue(), autoYtd);
    }

    // ── getArgs — always uses getSemanticValue() ──────────────────────────

    @Test
    public void getArgs_returnsSemanticValueForEachConstant() {
        assertEquals(ReportsPage.Sections.OVERVIEW.getArgs(),           new Object[]{"Overview"});
        assertEquals(ReportsPage.Sections.KPI_SUMMARY.getArgs(),        new Object[]{"KPI Summary"});
        assertEquals(ReportsPage.Sections.VENDOR_PERFORMANCE.getArgs(), new Object[]{"Vendor Performance"});
        assertEquals(ReportsPage.Sections.YTD_ANALYSIS.getArgs(),       new Object[]{"YTD Analysis"});
    }

    @Test
    public void getArgs_returnsArrayOfOneElement() {
        assertEquals(ReportsPage.Sections.KPI_SUMMARY.getArgs().length, 1);
    }

    @Test
    public void getArgs_doesNotFallBackToWordTransform() {
        // KPI_SUMMARY word-transform would be "Kpi Summary"; switch gives "KPI Summary"
        Object arg = ReportsPage.Sections.KPI_SUMMARY.getArgs()[0];
        assertEquals(arg, "KPI Summary");
        assertNotEquals(arg, "Kpi Summary");
    }

    @Test
    public void getArgs_readOnlyVariant_usesSemanticValue() {
        assertEquals(ReportsPage.Labels.STATUS_BADGE.getArgs(),  new Object[]{"Status"});
        assertEquals(ReportsPage.Labels.LAST_UPDATED.getArgs(),  new Object[]{"Last Updated"});
    }

    // ── getExternalFileName ───────────────────────────────────────────────

    @Test
    public void getExternalFileName_returnsConventionalPath() {
        assertEquals(ReportsPage.Sections.OVERVIEW.getExternalFileName(),
                "elements/api/SwitchLocatorFamilyTest$ReportsPage/locators.properties");
        assertEquals(ReportsPage.Labels.STATUS_BADGE.getExternalFileName(),
                "elements/api/SwitchLocatorFamilyTest$ReportsPage/locators.properties");
    }

    // ── capability locator methods delegate to family key ─────────────────

    @Test
    public void getTriggerLocator_returnsFamilyKey() {
        assertEquals(ReportsPage.Sections.KPI_SUMMARY.getTriggerLocator(), "ReportsPage.Sections");
    }

    @Test
    public void getTextLocator_returnsFamilyKey() {
        assertEquals(ReportsPage.Labels.STATUS_BADGE.getTextLocator(), "ReportsPage.Labels");
    }

    // ── instanceof checks ─────────────────────────────────────────────────

    @Test
    public void switchLocatorFamily_isAlsoLocatorFamily() {
        assertTrue(ReportsPage.Sections.OVERVIEW instanceof LocatorFamily);
        assertTrue(ReportsPage.Labels.STATUS_BADGE   instanceof LocatorFamily);
    }

    @Test
    public void switchLocatorFamily_isAlsoElement() {
        assertTrue(ReportsPage.Sections.OVERVIEW instanceof UIElement);
    }

    @Test
    public void switchLocatorFamily_isNotAdvancedLocatorFamily() {
        // SwitchLocatorFamily and AdvancedLocatorFamily are siblings, not a hierarchy.
        // Cast through Object to avoid compile-time incompatible-types error.
        Object o = ReportsPage.Sections.OVERVIEW;
        assertFalse(o instanceof AdvancedLocatorFamily);
    }

    // ── key is constant, arg varies by constant ───────────────────────────

    @Test
    public void keyConstant_argVariesByConstant() {
        String key1 = ReportsPage.Sections.KPI_SUMMARY.getPrimaryLocator();
        String key2 = ReportsPage.Sections.YTD_ANALYSIS.getPrimaryLocator();
        Object arg1 = ReportsPage.Sections.KPI_SUMMARY.getArgs()[0];
        Object arg2 = ReportsPage.Sections.YTD_ANALYSIS.getArgs()[0];

        assertEquals(key1, key2);
        assertNotEquals(arg1, arg2);
    }
}
