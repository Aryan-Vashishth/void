package core.actions;

import elements.api.capability.Checkable;
import elements.api.capability.Clickable;
import elements.api.capability.EditableTable;
import elements.api.capability.Hoverable;
import elements.api.capability.Listable;
import elements.api.capability.MultiSelectable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Searchable;
import elements.api.capability.SearchField;
import elements.api.capability.SearchableDropdown;
import elements.api.capability.Selectable;
import elements.api.capability.Table;
import elements.api.capability.Typeable;
import elements.api.capability.Uploadable;
import elements.meta.ElementRole;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * Verifies that each of the 14 capability interfaces self-describes through
 * {@link ActionCapabilityProvider#capability()} and returns the expected
 * {@link ActionCapability} constant.
 *
 * <p>Uses anonymous inner classes — no Mockito or engine required.</p>
 */
public class ActionCapabilityProviderTest {

    // ── Batch 1 ───────────────────────────────────────────────────────────

    @Test
    public void clickable_returnsClickableCapability() {
        Clickable element = stubClickable();
        assertEquals(element.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void typeable_returnsTypeableCapability() {
        Typeable element = stubTypeable();
        assertEquals(element.capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void selectable_returnsSelectableCapability() {
        Selectable element = stubSelectable();
        assertEquals(element.capability(), ActionCapability.SELECTABLE);
    }

    // ── Batch 2 ───────────────────────────────────────────────────────────

    @Test
    public void hoverable_returnsHoverableCapability() {
        Hoverable element = stubHoverable();
        assertEquals(element.capability(), ActionCapability.HOVERABLE);
    }

    @Test
    public void uploadable_returnsUploadableCapability() {
        Uploadable element = stubUploadable();
        assertEquals(element.capability(), ActionCapability.UPLOADABLE);
    }

    @Test
    public void checkable_returnsCheckableCapability() {
        Checkable element = stubCheckable();
        assertEquals(element.capability(), ActionCapability.CHECKABLE);
    }

    // ── Batch 3 ───────────────────────────────────────────────────────────

    @Test
    public void multiSelectable_returnsMultiSelectableCapability() {
        MultiSelectable element = stubMultiSelectable();
        assertEquals(element.capability(), ActionCapability.MULTI_SELECTABLE);
    }

    @Test
    public void searchField_returnsSearchFieldCapability() {
        SearchField element = stubSearchField();
        assertEquals(element.capability(), ActionCapability.SEARCH_FIELD);
    }

    @Test
    public void searchable_returnsSearchableCapability() {
        Searchable element = stubSearchable();
        assertEquals(element.capability(), ActionCapability.SEARCHABLE);
    }

    @Test
    public void searchableDropdown_returnsSearchableDropdownCapability() {
        SearchableDropdown element = stubSearchableDropdown();
        assertEquals(element.capability(), ActionCapability.SEARCHABLE_DROPDOWN);
    }

    @Test
    public void readOnly_returnsReadOnlyCapability() {
        ReadOnly element = stubReadOnly();
        assertEquals(element.capability(), ActionCapability.READ_ONLY);
    }

    @Test
    public void table_returnsTableCapability() {
        Table element = stubTable();
        assertEquals(element.capability(), ActionCapability.TABLE);
    }

    @Test
    public void editableTable_returnsEditableTableCapability() {
        EditableTable element = stubEditableTable();
        assertEquals(element.capability(), ActionCapability.EDITABLE_TABLE);
    }

    @Test
    public void listable_returnsListableCapability() {
        Listable element = stubListable();
        assertEquals(element.capability(), ActionCapability.LISTABLE);
    }

    // ── Provider pattern ──────────────────────────────────────────────────

    @Test
    public void actionCapabilityProvider_resolvesClickableWithoutRegistry() {
        Object element = stubClickable();
        // Simulates the intended metadata-only usage pattern.
        ActionCapability resolved = (element instanceof ActionCapabilityProvider p)
                ? p.capability()
                : ActionCapability.UNKNOWN;
        assertSame(resolved, ActionCapability.CLICKABLE);
    }

    @Test
    public void selectableOverridesClickableDefault() {
        // Selectable inherits ActionCapabilityProvider through Clickable.
        // Without an explicit override it would return CLICKABLE — must return SELECTABLE.
        Selectable sel = stubSelectable();
        assertEquals(sel.capability(), ActionCapability.SELECTABLE,
                "Selectable must override capability() to return SELECTABLE, not CLICKABLE");
    }

    @Test
    public void checkableOverridesClickableDefault() {
        Checkable chk = stubCheckable();
        assertEquals(chk.capability(), ActionCapability.CHECKABLE,
                "Checkable must override capability() to return CHECKABLE, not CLICKABLE");
    }

    @Test
    public void searchFieldResolvesConflictBetweenTypeableAndClickable() {
        SearchField sf = stubSearchField();
        assertEquals(sf.capability(), ActionCapability.SEARCH_FIELD,
                "SearchField must override capability() to resolve Typeable/Clickable conflict");
    }

    @Test
    public void searchableDropdownResolvesConflictBetweenSelectableAndSearchable() {
        SearchableDropdown sd = stubSearchableDropdown();
        assertEquals(sd.capability(), ActionCapability.SEARCHABLE_DROPDOWN,
                "SearchableDropdown must override capability() to resolve Selectable/Searchable conflict");
    }

    // ── Stubs ─────────────────────────────────────────────────────────────

    private static Clickable stubClickable() {
        return new Clickable() {
            @Override public String getTriggerLocator() { return "click"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Typeable stubTypeable() {
        return new Typeable() {
            @Override public String getInputLocator() { return "input"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Selectable stubSelectable() {
        return new Selectable() {
            @Override public String getTriggerLocator() { return "trigger"; }
            @Override public String getListLocator() { return "list"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Hoverable stubHoverable() {
        return new Hoverable() {
            @Override public String getTextLocator() { return "text"; }
            @Override public String getToolTipContentLocator() { return "tip"; }
            @Override public String getEndsWith() { return ""; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Uploadable stubUploadable() {
        return new Uploadable() {
            @Override public String getInputLocator() { return "file"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Checkable stubCheckable() {
        return new Checkable() {
            @Override public String getTriggerLocator() { return "chk"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static MultiSelectable stubMultiSelectable() {
        return new MultiSelectable() {
            @Override public String getTriggerLocator() { return "mt"; }
            @Override public String getListLocator() { return "ml"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static SearchField stubSearchField() {
        return new SearchField() {
            @Override public String getSearchInputLocator() { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Searchable stubSearchable() {
        return new Searchable() {
            @Override public String getSearchInputLocator() { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getSearchResultLocator() { return "sr"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static SearchableDropdown stubSearchableDropdown() {
        return new SearchableDropdown() {
            @Override public String getSearchInputLocator() { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getTriggerLocator() { return "tr"; }
            @Override public String getSearchResultLocator() { return "sr"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static ReadOnly stubReadOnly() {
        return new ReadOnly() {
            @Override public String getTextLocator() { return "txt"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Table stubTable() {
        return new Table() {
            @Override public String getTableLocator() { return "tbl"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static EditableTable stubEditableTable() {
        return new EditableTable() {
            @Override public String getTableLocator() { return "tbl"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Listable stubListable() {
        return new Listable() {
            @Override public String getListLocator() { return "lst"; }
            @Override public String getPrimaryLocator() { return "lst"; }
            @Override public int getIndex() { return 0; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }
}
