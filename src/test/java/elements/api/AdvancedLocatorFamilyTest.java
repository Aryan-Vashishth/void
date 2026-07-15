package elements.api;

import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link AdvancedLocatorFamily} defaults.
 */
public class AdvancedLocatorFamilyTest {

    // ── Test page interfaces ───────────────────────────────────────────────

    interface VendorPage {

        /**
         * Mixed family: most constants auto-derive, a few carry explicit values.
         * Demonstrates the canonical AdvancedLocatorFamily pattern.
         */
        enum Filters implements Clickable, AdvancedLocatorFamily {

            COUNTRY,                                   // auto: "Country"
            PROGRAM_NAME,                              // auto: "Program Name"
            HQ_STATE_PROVINCE("HQ State/Province"),    // explicit: slash + mixed casing
            SAVE_AND_CONTINUE("Save & Continue"),      // explicit: ampersand
            CRM("CRM");                                // explicit: all-caps acronym

            private final String semanticValue;
            Filters()         { this.semanticValue = null; }
            Filters(String v) { this.semanticValue = v;    }

            @Override public String getPrimaryLocator() { return AdvancedLocatorFamily.super.getPrimaryLocator(); }
            @Override public String getTriggerLocator() { return getPrimaryLocator(); }
            @Override public String getSemanticValue()  { return semanticValue; }
        }

        /** All-auto family — no explicit values; verifies LocatorFamily fallback still works. */
        enum Sections implements Typeable, AdvancedLocatorFamily {

            GENERAL_INFO,
            CONTACT_DETAILS;

            private final String semanticValue;
            Sections()         { this.semanticValue = null; }
            Sections(String v) { this.semanticValue = v; }

            @Override public String getPrimaryLocator() { return AdvancedLocatorFamily.super.getPrimaryLocator(); }
            @Override public String getInputLocator()   { return getPrimaryLocator(); }
            @Override public String getSemanticValue()  { return semanticValue; }
        }
    }

    // ── getPrimaryLocator — inherited from LocatorFamily ──────────────────

    @Test
    public void getPrimaryLocator_allConstantsShareFamilyKey() {
        String expected = "VendorPage.Filters";
        for (VendorPage.Filters f : VendorPage.Filters.values()) {
            assertEquals(f.getPrimaryLocator(), expected,
                    "Constant " + f.name() + " should share the family key");
        }
    }

    @Test
    public void getPrimaryLocator_doesNotContainConstantName() {
        assertFalse(VendorPage.Filters.CRM.getPrimaryLocator().contains("CRM"));
        assertFalse(VendorPage.Filters.HQ_STATE_PROVINCE.getPrimaryLocator().contains("HQ_STATE_PROVINCE"));
    }

    // ── getSemanticValue ──────────────────────────────────────────────────

    @Test
    public void getSemanticValue_noConstructorArg_returnsNull() {
        assertNull(VendorPage.Filters.COUNTRY.getSemanticValue());
        assertNull(VendorPage.Filters.PROGRAM_NAME.getSemanticValue());
    }

    @Test
    public void getSemanticValue_withConstructorArg_returnsExplicitValue() {
        assertEquals(VendorPage.Filters.HQ_STATE_PROVINCE.getSemanticValue(), "HQ State/Province");
        assertEquals(VendorPage.Filters.SAVE_AND_CONTINUE.getSemanticValue(), "Save & Continue");
        assertEquals(VendorPage.Filters.CRM.getSemanticValue(), "CRM");
    }

    // ── getArgs — core routing logic ──────────────────────────────────────

    @Test
    public void getArgs_noExplicitValue_returnsWordTransform() {
        assertEquals(VendorPage.Filters.COUNTRY.getArgs(),      new Object[]{"Country"});
        assertEquals(VendorPage.Filters.PROGRAM_NAME.getArgs(), new Object[]{"Program Name"});
    }

    @Test
    public void getArgs_withExplicitValue_returnsExplicitValue() {
        assertEquals(VendorPage.Filters.HQ_STATE_PROVINCE.getArgs(), new Object[]{"HQ State/Province"});
        assertEquals(VendorPage.Filters.SAVE_AND_CONTINUE.getArgs(), new Object[]{"Save & Continue"});
        assertEquals(VendorPage.Filters.CRM.getArgs(),               new Object[]{"CRM"});
    }

    @Test
    public void getArgs_mixedEnum_eachConstantResolvesCorrectly() {
        assertEquals(VendorPage.Filters.COUNTRY.getArgs(),           new Object[]{"Country"});
        assertEquals(VendorPage.Filters.PROGRAM_NAME.getArgs(),      new Object[]{"Program Name"});
        assertEquals(VendorPage.Filters.HQ_STATE_PROVINCE.getArgs(), new Object[]{"HQ State/Province"});
        assertEquals(VendorPage.Filters.SAVE_AND_CONTINUE.getArgs(), new Object[]{"Save & Continue"});
        assertEquals(VendorPage.Filters.CRM.getArgs(),               new Object[]{"CRM"});
    }

    @Test
    public void getArgs_allAutoEnum_allUseWordTransform() {
        assertEquals(VendorPage.Sections.GENERAL_INFO.getArgs(),     new Object[]{"General Info"});
        assertEquals(VendorPage.Sections.CONTACT_DETAILS.getArgs(),  new Object[]{"Contact Details"});
    }

    // ── getExternalFileName ───────────────────────────────────────────────

    @Test
    public void getExternalFileName_returnsConventionalPath() {
        assertEquals(VendorPage.Filters.COUNTRY.getExternalFileName(),
                "elements/api/AdvancedLocatorFamilyTest$VendorPage/locators.properties");
        assertEquals(VendorPage.Filters.CRM.getExternalFileName(),
                "elements/api/AdvancedLocatorFamilyTest$VendorPage/locators.properties");
    }

    // ── capability locator methods delegate to family key ─────────────────

    @Test
    public void getTriggerLocator_returnsFamilyKey() {
        assertEquals(VendorPage.Filters.CRM.getTriggerLocator(), "VendorPage.Filters");
    }

    @Test
    public void getInputLocator_returnsFamilyKey() {
        assertEquals(VendorPage.Sections.GENERAL_INFO.getInputLocator(), "VendorPage.Sections");
    }

    // ── key is constant, arg varies by constant ───────────────────────────

    @Test
    public void keyConstant_argVaries_acrossMixedConstants() {
        // All share one key
        assertEquals(VendorPage.Filters.COUNTRY.getPrimaryLocator(),
                VendorPage.Filters.CRM.getPrimaryLocator());
        // But args differ (auto vs explicit)
        assertNotEquals(
                VendorPage.Filters.COUNTRY.getArgs()[0],
                VendorPage.Filters.CRM.getArgs()[0]);
    }

    // ── instanceof checks ─────────────────────────────────────────────────

    @Test
    public void advancedLocatorFamily_isAlsoLocatorFamily() {
        assertTrue(VendorPage.Filters.COUNTRY instanceof LocatorFamily);
        assertTrue(VendorPage.Filters.CRM    instanceof LocatorFamily);
    }

    @Test
    public void advancedLocatorFamily_isAlsoElement() {
        assertTrue(VendorPage.Filters.COUNTRY instanceof Element);
    }
}
