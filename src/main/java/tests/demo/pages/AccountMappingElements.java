package tests.demo.pages;

import elements.api.Element;
import elements.api.capability.*;

/**
 * Complex page-element catalogue for the Account Mapping page.
 *
 * <p>Exercises <b>every</b> {@link Element} sub-interface with deep nesting,
 * properties-backed resolution, and dynamic {@code %s} locator templates.
 * Purpose-built to stress-test {@code JsonLocatorMigrator} output.</p>
 *
 * <pre>
 * AccountMappingElements
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ Header            (ReadOnly)  Ã¢â‚¬â€ static page labels
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ SearchBar         (SearchField)      Ã¢â‚¬â€ 2 roles: SEARCH_INPUT, SEARCH_BUTTON
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ FilterPanel
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ StatusDropdown    (SearchableDropdown)  Ã¢â‚¬â€ 4 roles: TRIGGER, SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ DateRange         (Typeable)      Ã¢â‚¬â€ 1 role:  INPUT
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ ActiveOnly        (Checkable)            Ã¢â‚¬â€ 1 role:  TRIGGER
 * Ã¢â€â€š   Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ ApplyButton       (Clickable)           Ã¢â‚¬â€ 1 role:  TRIGGER
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ AccountTable      (EditableTable)    Ã¢â‚¬â€ up to 8 roles
 * Ã¢â€â€š   Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ InlineEditor  (Typeable)          Ã¢â‚¬â€ nested inside table context
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ ActionBar
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ BulkActions   (Selectable)                Ã¢â‚¬â€ 2 roles: TRIGGER, LIST
 * Ã¢â€â€š   Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ ExportButton  (Clickable)               Ã¢â‚¬â€ 1 role:  TRIGGER
 * Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ DetailPanel
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ TooltipField     (Hoverable)       Ã¢â‚¬â€ 2 roles: TEXT, TOOLTIP_CONTENT
 * Ã¢â€â€š   Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬ RoleSelector     (MultiSelectable) Ã¢â‚¬â€ 2 roles: MULTI_TRIGGER, MULTI_LIST
 * Ã¢â€â€š   Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ FileUpload       (Uploadable)     Ã¢â‚¬â€ 1 role:  INPUT
 * Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ Pagination        (Clickable)               Ã¢â‚¬â€ nested with its own PageSizeDropdown
 *     Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬ PageSizeDropdown (Selectable)             Ã¢â‚¬â€ 2 roles: TRIGGER, LIST
 * </pre>
 */
public interface AccountMappingElements {

    /** Shared properties file for all enums in this interface. */
    String PROPS = "account-mapping-elements.properties";

    // ========================================================================
    // 1. Header Ã¢â‚¬â€ ReadOnly (single role: TEXT)
    // ========================================================================

    enum Header implements ReadOnly {
        PAGE_TITLE("PAGE_TITLE"),
        BREADCRUMB("BREADCRUMB"),
        LAST_SYNCED_LABEL("LAST_SYNCED_LABEL");

        private final String key;
        Header(String k) { this.key = k; }

        @Override public String getExternalFileName() { return PROPS; }
        @Override public String getTextLocator()      { return key; }
    }

    // ========================================================================
    // 2. SearchBar Ã¢â‚¬â€ SearchField (2 roles: SEARCH_INPUT, SEARCH_BUTTON)
    // ========================================================================

    enum SearchBar implements SearchField {
        ACCOUNT_SEARCH("SEARCH_INPUT", "SEARCH_BUTTON");

        private final String inputKey;
        private final String buttonKey;
        SearchBar(String input, String button) { this.inputKey = input; this.buttonKey = button; }

        @Override public String getSearchInputLocator()  { return inputKey; }
        @Override public String getSearchButtonLocator() { return buttonKey; }
        @Override public String getExternalFileName()    { return PROPS; }
        @Override public Object[] getArgs()              { return new Object[0]; }
    }

    // ========================================================================
    // 3. FilterPanel Ã¢â‚¬â€ container interface for filter-related enums
    // ========================================================================

    interface FilterPanel {

        // 3a. StatusDropdown Ã¢â‚¬â€ SearchableDropdown (4 roles)
        enum StatusDropdown implements SearchableDropdown {
            USER_STATUS("STATUS_TRIGGER", "STATUS_SEARCH_INPUT", "STATUS_SEARCH_BTN", "STATUS_RESULTS");

            private final String trigger, searchInput, searchBtn, results;
            StatusDropdown(String t, String si, String sb, String r) {
                this.trigger = t; this.searchInput = si; this.searchBtn = sb; this.results = r;
            }

            @Override public String getTriggerLocator()      { return trigger; }
            @Override public String getSearchInputLocator()   { return searchInput; }
            @Override public String getSearchButtonLocator()  { return searchBtn; }
            @Override public String getSearchResultLocator()  { return results; }
            @Override public String getExternalFileName()     { return PROPS; }
            @Override public Object[] getArgs()               { return new Object[]{"Active"}; }
        }

        // 3b. DateRange Ã¢â‚¬â€ Typeable with dynamic templates
        enum DateRange implements Typeable {
            DATE_FROM("DATE_FROM_INPUT", "From"),
            DATE_TO("DATE_TO_INPUT", "To");

            private final String key;
            private final String label;
            DateRange(String k, String l) { this.key = k; this.label = l; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{label}; }
        }

        // 3c. ActiveOnly Ã¢â‚¬â€ Checkable (extends Clickable, single role: TRIGGER)
        enum ActiveOnly implements Checkable {
            ACTIVE_ONLY_TOGGLE("ACTIVE_ONLY_TRIGGER");

            private final String key;
            ActiveOnly(String k) { this.key = k; }

            @Override public String getTriggerLocator()   { return key; }
            @Override public String getExternalFileName() { return PROPS; }
        }

        // 3d. ApplyButton Ã¢â‚¬â€ Clickable (single role: TRIGGER)
        enum ApplyButton implements Clickable {
            APPLY("APPLY_FILTER_TRIGGER", "Apply"),
            RESET("RESET_FILTER_TRIGGER", "Reset");

            private final String key;
            private final String label;
            ApplyButton(String k, String l) { this.key = k; this.label = l; }

            @Override public String getTriggerLocator()   { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{label}; }
        }
    }

    // ========================================================================
    // 4. AccountTable Ã¢â‚¬â€ EditableTable (up to 8 roles)
    //    with nested InlineEditor
    // ========================================================================

    enum AccountTable implements EditableTable {
        MAPPING_GRID(
                "ACCT_TABLE", "ACCT_ROW", "ACCT_COLUMN", "ACCT_CELL", "ACCT_HEADER",
                "ACCT_ADD_ROW", "ACCT_REMOVE_ROW", "ACCT_FOOTER_INPUT"
        );

        private final String table, row, col, cell, header, addRow, removeRow, footerInput;
        AccountTable(String t, String r, String c, String ce, String h, String ar, String rr, String fi) {
            this.table = t; this.row = r; this.col = c; this.cell = ce;
            this.header = h; this.addRow = ar; this.removeRow = rr; this.footerInput = fi;
        }

        @Override public String getTableLocator()          { return table; }
        @Override public String getRowLocator()            { return row; }
        @Override public String getColumnLocator()         { return col; }
        @Override public String getCellLocator()           { return cell; }
        @Override public String getHeaderLocator()         { return header; }
        @Override public String getAddRowButtonLocator()   { return addRow; }
        @Override public String getRemoveRowButtonLocator(){ return removeRow; }
        @Override public String getFooterInputRowLocator() { return footerInput; }
        @Override public String getExternalFileName()      { return PROPS; }

        // 4a. InlineEditor Ã¢â‚¬â€ Typeable nested inside AccountTable
        enum InlineEditor implements Typeable {
            CELL_EDITOR("INLINE_EDIT_INPUT");

            private final String key;
            InlineEditor(String k) { this.key = k; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"1", "name"}; }
        }
    }

    // ========================================================================
    // 5. ActionBar Ã¢â‚¬â€ container for bulk actions and export
    // ========================================================================

    interface ActionBar {

        // 5a. BulkActions Ã¢â‚¬â€ Selectable (2 roles: TRIGGER, LIST)
        enum BulkActions implements Selectable {
            BULK_MENU("BULK_ACTION_TRIGGER", "BULK_ACTION_LIST");

            private final String trigger;
            private final String list;
            BulkActions(String t, String l) { this.trigger = t; this.list = l; }

            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"Delete Selected"}; }
        }

        // 5b. ExportButton Ã¢â‚¬â€ Clickable (single role: TRIGGER)
        enum ExportButton implements Clickable {
            EXPORT("EXPORT_TRIGGER");

            private final String key;
            ExportButton(String k) { this.key = k; }

            @Override public String getTriggerLocator()   { return key; }
            @Override public String getExternalFileName() { return PROPS; }
        }
    }

    // ========================================================================
    // 6. DetailPanel Ã¢â‚¬â€ tooltip, multi-Selectable, file upload
    // ========================================================================

    interface DetailPanel {

        // 6a. TooltipField Ã¢â‚¬â€ Hoverable (2 roles: TEXT, TOOLTIP_CONTENT)
        enum TooltipField implements Hoverable {
            ACCOUNT_NAME("ACCOUNT_NAME_TEXT", "ACCOUNT_NAME_TOOLTIP", "...");

            private final String textKey;
            private final String tooltipKey;
            private final String endsWith;
            TooltipField(String t, String tt, String ew) { this.textKey = t; this.tooltipKey = tt; this.endsWith = ew; }

            @Override public String getTextLocator()           { return textKey; }
            @Override public String getToolTipContentLocator() { return tooltipKey; }
            @Override public String getEndsWith()              { return endsWith; }
            @Override public String getExternalFileName()      { return PROPS; }
        }

        // 6b. RoleSelector Ã¢â‚¬â€ MultiSelectable (2 roles: MULTI_TRIGGER, MULTI_LIST)
        enum RoleSelector implements MultiSelectable {
            ROLE_DROPDOWN("ROLE_MULTI_TRIGGER", "ROLE_MULTI_LIST");

            private final String trigger;
            private final String list;
            RoleSelector(String t, String l) { this.trigger = t; this.list = l; }

            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{1, "Admin"}; }
        }

        // 6c. FileUpload Ã¢â‚¬â€ Uploadable (single role: INPUT)
        enum FileUpload implements Uploadable {
            MAPPING_CSV("UPLOAD_INPUT");

            private final String key;
            FileUpload(String k) { this.key = k; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
        }
    }

    // ========================================================================
    // 7. Pagination Ã¢â‚¬â€ Clickable with nested PageSizeDropdown
    // ========================================================================

    enum Pagination implements Clickable {
        NEXT_PAGE("NEXT_PAGE_TRIGGER", "Next"),
        PREV_PAGE("PREV_PAGE_TRIGGER", "Previous");

        private final String key;
        private final String label;
        Pagination(String k, String l) { this.key = k; this.label = l; }

        @Override public String getTriggerLocator()   { return key; }
        @Override public String getExternalFileName() { return PROPS; }
        @Override public Object[] getArgs()           { return new Object[]{label}; }

        // 7a. PageSizeDropdown Ã¢â‚¬â€ Selectable (2 roles: TRIGGER, LIST)
        enum PageSizeDropdown implements Selectable {
            PAGE_SIZE("PAGE_SIZE_TRIGGER", "PAGE_SIZE_LIST");

            private final String trigger;
            private final String list;
            PageSizeDropdown(String t, String l) { this.trigger = t; this.list = l; }

            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"25"}; }
        }
    }
}

