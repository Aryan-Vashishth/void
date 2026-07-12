package elements.api;

import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link LocatorFamily} defaults.
 */
public class LocatorFamilyTest {

    // ── Test page interfaces that mirror real page structure ───────────────

    /** Simulates a page interface; enum nested inside provides PageName.EnumName key. */
    interface AdminHomePage {

        /** Pure LocatorFamily (no capability) — tests key and args in isolation. */
        enum Tiles implements LocatorFamily {
            AUDIT_INFO,
            MANAGE_USERS,
            MANAGE_DOCS,
            MANAGE_VENDORS
        }

        /**
         * LocatorFamily combined with Clickable.
         * <p>Diamond: both Clickable and LocatorFamily override getPrimaryLocator() —
         * the enum must resolve explicitly. The capability locator method delegates
         * to the resolved getPrimaryLocator().</p>
         */
        enum ActionButtons implements Clickable, LocatorFamily {
            SAVE_CHANGES,
            DISCARD_CHANGES,
            EXPORT_REPORT;

            @Override
            public String getPrimaryLocator() { return LocatorFamily.super.getPrimaryLocator(); }

            @Override
            public String getTriggerLocator() { return getPrimaryLocator(); }
        }

        /**
         * LocatorFamily combined with Typeable.
         */
        enum SearchFields implements Typeable, LocatorFamily {
            VENDOR_SEARCH,
            DOCUMENT_SEARCH;

            @Override
            public String getPrimaryLocator() { return LocatorFamily.super.getPrimaryLocator(); }

            @Override
            public String getInputLocator() { return getPrimaryLocator(); }
        }
    }

    /** Top-level enum (no enclosing page) — key has no page prefix. */
    enum TopLevelFamily implements LocatorFamily {
        FIRST_ITEM,
        SECOND_ITEM
    }

    // ── getPrimaryLocator ──────────────────────────────────────────────────

    @Test
    public void getPrimaryLocator_nestedInPage_returnsPageDotEnum() {
        assertEquals(AdminHomePage.Tiles.AUDIT_INFO.getPrimaryLocator(), "AdminHomePage.Tiles");
    }

    @Test
    public void getPrimaryLocator_allConstantsShareSameKey() {
        String expected = "AdminHomePage.Tiles";
        assertEquals(AdminHomePage.Tiles.AUDIT_INFO.getPrimaryLocator(),     expected);
        assertEquals(AdminHomePage.Tiles.MANAGE_USERS.getPrimaryLocator(),   expected);
        assertEquals(AdminHomePage.Tiles.MANAGE_DOCS.getPrimaryLocator(),    expected);
        assertEquals(AdminHomePage.Tiles.MANAGE_VENDORS.getPrimaryLocator(), expected);
    }

    @Test
    public void getPrimaryLocator_doesNotIncludeConstantName() {
        String key = AdminHomePage.Tiles.MANAGE_USERS.getPrimaryLocator();
        assertFalse(key.contains("MANAGE_USERS"), "Family key must not contain constant name");
    }

    @Test
    public void getPrimaryLocator_enumInTestClass_includesTestClassName() {
        // TopLevelFamily is nested inside LocatorFamilyTest, so getEnclosingClass()
        // returns LocatorFamilyTest — key is "EnclosingClass.EnumName".
        // In production, all family enums are nested inside a page interface (two levels).
        assertEquals(TopLevelFamily.FIRST_ITEM.getPrimaryLocator(), "LocatorFamilyTest.TopLevelFamily");
    }

    @Test
    public void getPrimaryLocator_withClickable_diamondResolvesToFamilyKey() {
        assertEquals(AdminHomePage.ActionButtons.SAVE_CHANGES.getPrimaryLocator(),
                "AdminHomePage.ActionButtons");
    }

    @Test
    public void getPrimaryLocator_withTypeable_diamondResolvesToFamilyKey() {
        assertEquals(AdminHomePage.SearchFields.VENDOR_SEARCH.getPrimaryLocator(),
                "AdminHomePage.SearchFields");
    }

    // ── getArgs ───────────────────────────────────────────────────────────

    @Test
    public void getArgs_multiToken_splitOnUnderscore() {
        assertEquals(AdminHomePage.Tiles.MANAGE_USERS.getArgs(), new Object[]{"Manage Users"});
    }

    @Test
    public void getArgs_threeToken_allCapitalised() {
        assertEquals(AdminHomePage.Tiles.AUDIT_INFO.getArgs(), new Object[]{"Audit Info"});
    }

    @Test
    public void getArgs_withClickable_deriveFromConstantName() {
        assertEquals(AdminHomePage.ActionButtons.SAVE_CHANGES.getArgs(), new Object[]{"Save Changes"});
    }

    @Test
    public void getArgs_returnsArrayOfOneElement() {
        assertEquals(AdminHomePage.Tiles.MANAGE_DOCS.getArgs().length, 1);
    }

    @Test
    public void getArgs_differentConstantsDifferentArgs() {
        assertNotEquals(
                AdminHomePage.Tiles.MANAGE_USERS.getArgs()[0],
                AdminHomePage.Tiles.MANAGE_VENDORS.getArgs()[0]);
    }

    @Test
    public void getArgs_topLevelEnum_deriveFromConstantName() {
        assertEquals(TopLevelFamily.FIRST_ITEM.getArgs(),  new Object[]{"First Item"});
        assertEquals(TopLevelFamily.SECOND_ITEM.getArgs(), new Object[]{"Second Item"});
    }

    // ── getExternalFileName ───────────────────────────────────────────────

    @Test
    public void getExternalFileName_pureFamily_returnsNull() {
        assertNull(AdminHomePage.Tiles.AUDIT_INFO.getExternalFileName());
    }

    @Test
    public void getExternalFileName_withClickable_returnsNull() {
        assertNull(AdminHomePage.ActionButtons.SAVE_CHANGES.getExternalFileName());
    }

    // ── capability locator methods delegate to family key ─────────────────

    @Test
    public void getTriggerLocator_withClickable_returnsFamilyKey() {
        assertEquals(AdminHomePage.ActionButtons.SAVE_CHANGES.getTriggerLocator(),
                "AdminHomePage.ActionButtons");
    }

    @Test
    public void getInputLocator_withTypeable_returnsFamilyKey() {
        assertEquals(AdminHomePage.SearchFields.VENDOR_SEARCH.getInputLocator(),
                "AdminHomePage.SearchFields");
    }

    // ── key is constant, arg varies by constant ───────────────────────────

    @Test
    public void keyIsConstant_argVariesByConstant() {
        String key1 = AdminHomePage.Tiles.MANAGE_USERS.getPrimaryLocator();
        String key2 = AdminHomePage.Tiles.MANAGE_VENDORS.getPrimaryLocator();
        Object arg1 = AdminHomePage.Tiles.MANAGE_USERS.getArgs()[0];
        Object arg2 = AdminHomePage.Tiles.MANAGE_VENDORS.getArgs()[0];

        assertEquals(key1, key2);         // same template key for all constants
        assertNotEquals(arg1, arg2);      // different runtime arg per constant
    }
}
