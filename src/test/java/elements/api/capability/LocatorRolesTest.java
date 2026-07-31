package domain.automation.web.vocabulary.capability;

import domain.automation.web.vocabulary.role.ElementRole;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for the package-private {@link LocatorRoles} helper and its integration
 * with {@link SearchField} and {@link SearchableDropdown}.
 *
 * <p>All tests live in the same package to access the package-private API directly.</p>
 */
public class LocatorRolesTest {

    // ── roleMap: basic inclusion ─────────────────────────────────────────

    @Test
    public void roleMap_noEntries_returnsEmptyMap() {
        Map<ElementRole, String> result = LocatorRoles.roleMap();
        assertTrue(result.isEmpty());
    }

    @Test
    public void roleMap_singleEntry_returnsOneMapping() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER, "//button")
        );
        assertEquals(result.size(), 1);
        assertEquals(result.get(ElementRole.TRIGGER), "//button");
    }

    @Test
    public void roleMap_uniqueKeys_includesAllEntries() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.SEARCH_INPUT,  "//input"),
            LocatorRoles.role(ElementRole.SEARCH_BUTTON, "//button")
        );
        assertEquals(result.size(), 2);
        assertEquals(result.get(ElementRole.SEARCH_INPUT),  "//input");
        assertEquals(result.get(ElementRole.SEARCH_BUTTON), "//button");
    }

    // ── roleMap: dedup on key string ─────────────────────────────────────

    @Test
    public void roleMap_duplicateKey_secondEntryExcluded() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER,      "//shared"),
            LocatorRoles.role(ElementRole.SEARCH_INPUT, "//shared")
        );
        // First occurrence wins; second (same key string) must be dropped
        assertEquals(result.size(), 1);
        assertTrue(result.containsKey(ElementRole.TRIGGER));
        assertFalse(result.containsKey(ElementRole.SEARCH_INPUT));
    }

    @Test
    public void roleMap_duplicateKey_firstRoleKept_notSecond() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.LIST,    "//ul"),
            LocatorRoles.role(ElementRole.TRIGGER, "//ul")
        );
        assertEquals(result.get(ElementRole.LIST), "//ul");
        assertNull(result.get(ElementRole.TRIGGER));
    }

    // ── roleMap: null / blank key exclusion ──────────────────────────────

    @Test
    public void roleMap_nullKey_entryExcluded() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER, null),
            LocatorRoles.role(ElementRole.LIST,    "//ul")
        );
        assertFalse(result.containsKey(ElementRole.TRIGGER));
        assertTrue(result.containsKey(ElementRole.LIST));
    }

    @Test
    public void roleMap_blankKey_entryExcluded() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER, "   "),
            LocatorRoles.role(ElementRole.LIST,    "//ul")
        );
        assertFalse(result.containsKey(ElementRole.TRIGGER));
        assertTrue(result.containsKey(ElementRole.LIST));
    }

    @Test
    public void roleMap_emptyKey_entryExcluded() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.SEARCH_BUTTON, ""),
            LocatorRoles.role(ElementRole.SEARCH_INPUT,  "//input")
        );
        assertFalse(result.containsKey(ElementRole.SEARCH_BUTTON));
        assertEquals(result.get(ElementRole.SEARCH_INPUT), "//input");
    }

    // ── roleMap: insertion order preserved ───────────────────────────────

    @Test
    public void roleMap_preservesInsertionOrder() {
        Map<ElementRole, String> result = LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER,       "//btn"),
            LocatorRoles.role(ElementRole.SEARCH_INPUT,  "//inp"),
            LocatorRoles.role(ElementRole.SEARCH_BUTTON, "//sb"),
            LocatorRoles.role(ElementRole.SEARCH_RESULT, "//li")
        );
        Iterator<ElementRole> it = result.keySet().iterator();
        assertEquals(it.next(), ElementRole.TRIGGER);
        assertEquals(it.next(), ElementRole.SEARCH_INPUT);
        assertEquals(it.next(), ElementRole.SEARCH_BUTTON);
        assertEquals(it.next(), ElementRole.SEARCH_RESULT);
    }

    // ── SearchField.getAllLocatorRoles() integration ──────────────────────

    @Test
    public void searchField_getAllLocatorRoles_containsBothRoles() {
        SearchField sf = new SearchField() {
            @Override public String getSearchInputLocator()  { return "//input[@type='search']"; }
            @Override public String getSearchButtonLocator() { return "//button[@type='submit']"; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
        Map<ElementRole, String> roles = sf.getAllLocatorRoles();
        assertEquals(roles.size(), 2);
        assertEquals(roles.get(ElementRole.SEARCH_INPUT),  "//input[@type='search']");
        assertEquals(roles.get(ElementRole.SEARCH_BUTTON), "//button[@type='submit']");
    }

    @Test
    public void searchField_getAllLocatorRoles_deduplicatesWhenBothLocatorsAreSame() {
        SearchField sf = new SearchField() {
            @Override public String getSearchInputLocator()  { return "//shared"; }
            @Override public String getSearchButtonLocator() { return "//shared"; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
        Map<ElementRole, String> roles = sf.getAllLocatorRoles();
        assertEquals(roles.size(), 1, "duplicate locator string must be deduplicated");
    }

    // ── SearchableDropdown.getAllLocatorRoles() integration ───────────────

    @Test
    public void searchableDropdown_getAllLocatorRoles_containsFourRoles() {
        SearchableDropdown sd = new SearchableDropdown() {
            @Override public String getTriggerLocator()      { return "//trigger"; }
            @Override public String getSearchInputLocator()  { return "//input"; }
            @Override public String getSearchButtonLocator() { return "//button"; }
            @Override public String getSearchResultLocator() { return "//result"; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
        Map<ElementRole, String> roles = sd.getAllLocatorRoles();
        assertEquals(roles.size(), 4);
        assertEquals(roles.get(ElementRole.TRIGGER),       "//trigger");
        assertEquals(roles.get(ElementRole.SEARCH_INPUT),  "//input");
        assertEquals(roles.get(ElementRole.SEARCH_BUTTON), "//button");
        assertEquals(roles.get(ElementRole.SEARCH_RESULT), "//result");
    }

    @Test
    public void searchableDropdown_getAllLocatorRoles_deduplicatesSharedLocators() {
        SearchableDropdown sd = new SearchableDropdown() {
            @Override public String getTriggerLocator()      { return "//shared"; }
            @Override public String getSearchInputLocator()  { return "//shared"; }
            @Override public String getSearchButtonLocator() { return "//button"; }
            @Override public String getSearchResultLocator() { return "//result"; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
        Map<ElementRole, String> roles = sd.getAllLocatorRoles();
        // TRIGGER wins; SEARCH_INPUT dropped (same key string)
        assertTrue(roles.containsKey(ElementRole.TRIGGER));
        assertFalse(roles.containsKey(ElementRole.SEARCH_INPUT));
        assertEquals(roles.size(), 3);
    }
}
