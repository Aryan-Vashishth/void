package elements.exapmlepages;

import elements.api.*;

/**
 * Complex page-element catalogue for the Account Mapping page.
 *
 * <p>Exercises <b>every</b> {@link Element} sub-interface with deep nesting,
 * properties-backed resolution, and dynamic {@code %s} locator templates.
 * Purpose-built to stress-test {@code JsonLocatorMigrator} output.</p>
 *
 * <pre>
 * AccountMappingElements
 * ├── Header            (ReadOnlyElement)  — static page labels
 * ├── SearchBar         (SearchField)      — 2 roles: SEARCH_INPUT, SEARCH_BUTTON
 * ├── FilterPanel
 * │   ├── StatusDropdown    (SearchableDropdown)  — 4 roles: TRIGGER, SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT
 * │   ├── DateRange         (TextInputField)      — 1 role:  INPUT
 * │   ├── ActiveOnly        (Checkbox)            — 1 role:  TRIGGER
 * │   └── ApplyButton       (Clickable)           — 1 role:  TRIGGER
 * ├── AccountTable      (WritableTableElement)    — up to 8 roles
 * │   └── InlineEditor  (TextInputField)          — nested inside table context
 * ├── ActionBar
 * │   ├── BulkActions   (Dropdown)                — 2 roles: TRIGGER, LIST
 * │   └── ExportButton  (Clickable)               — 1 role:  TRIGGER
 * ├── DetailPanel
 * │   ├── TooltipField     (ToolTipElement)       — 2 roles: TEXT, TOOLTIP_CONTENT
 * │   ├── RoleSelector     (MultipleIdenticalDropdowns) — 2 roles: MULTI_TRIGGER, MULTI_LIST
 * │   └── FileUpload       (FileInputElement)     — 1 role:  INPUT
 * └── Pagination        (Clickable)               — nested with its own PageSizeDropdown
 *     └── PageSizeDropdown (Dropdown)             — 2 roles: TRIGGER, LIST
 * </pre>
 */
public interface AccountMappingElements {

    /** Shared properties file for all enums in this interface. */
    String PROPS = "account-mapping-elements.properties";

    // ========================================================================
    // 1. Header — ReadOnlyElement (single role: TEXT)
    // ========================================================================

    enum Header implements ReadOnlyElement {
        PAGE_TITLE("PAGE_TITLE"),
        BREADCRUMB("BREADCRUMB"),
        LAST_SYNCED_LABEL("LAST_SYNCED_LABEL");

        private final String key;
        Header(String k) { this.key = k; }

        @Override public String getExternalFileName() { return PROPS; }
        @Override public String getTextLocator()      { return key; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    // ========================================================================
    // 2. SearchBar — SearchField (2 roles: SEARCH_INPUT, SEARCH_BUTTON)
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
    // 3. FilterPanel — container interface for filter-related enums
    // ========================================================================

    interface FilterPanel {

        // 3a. StatusDropdown — SearchableDropdown (4 roles)
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

        // 3b. DateRange — TextInputField with dynamic templates
        enum DateRange implements TextInputField {
            DATE_FROM("DATE_FROM_INPUT", "From"),
            DATE_TO("DATE_TO_INPUT", "To");

            private final String key;
            private final String label;
            DateRange(String k, String l) { this.key = k; this.label = l; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{label}; }
        }

        // 3c. ActiveOnly — Checkbox (extends Clickable, single role: TRIGGER)
        enum ActiveOnly implements Checkbox {
            ACTIVE_ONLY_TOGGLE("ACTIVE_ONLY_TRIGGER");

            private final String key;
            ActiveOnly(String k) { this.key = k; }

            @Override public String getTriggerLocator()   { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        }

        // 3d. ApplyButton — Clickable (single role: TRIGGER)
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
    // 4. AccountTable — WritableTableElement (up to 8 roles)
    //    with nested InlineEditor
    // ========================================================================

    enum AccountTable implements WritableTableElement {
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
        @Override public Object[] getArgs()                { return new Object[0]; }

        // 4a. InlineEditor — TextInputField nested inside AccountTable
        enum InlineEditor implements TextInputField {
            CELL_EDITOR("INLINE_EDIT_INPUT");

            private final String key;
            InlineEditor(String k) { this.key = k; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"1", "name"}; }
        }
    }

    // ========================================================================
    // 5. ActionBar — container for bulk actions and export
    // ========================================================================

    interface ActionBar {

        // 5a. BulkActions — Dropdown (2 roles: TRIGGER, LIST)
        enum BulkActions implements Dropdown {
            BULK_MENU("BULK_ACTION_TRIGGER", "BULK_ACTION_LIST");

            private final String trigger;
            private final String list;
            BulkActions(String t, String l) { this.trigger = t; this.list = l; }

            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"Delete Selected"}; }
        }

        // 5b. ExportButton — Clickable (single role: TRIGGER)
        enum ExportButton implements Clickable {
            EXPORT("EXPORT_TRIGGER");

            private final String key;
            ExportButton(String k) { this.key = k; }

            @Override public String getTriggerLocator()   { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        }
    }

    // ========================================================================
    // 6. DetailPanel — tooltip, multi-dropdown, file upload
    // ========================================================================

    interface DetailPanel {

        // 6a. TooltipField — ToolTipElement (2 roles: TEXT, TOOLTIP_CONTENT)
        enum TooltipField implements ToolTipElement {
            ACCOUNT_NAME("ACCOUNT_NAME_TEXT", "ACCOUNT_NAME_TOOLTIP", "...");

            private final String textKey;
            private final String tooltipKey;
            private final String endsWith;
            TooltipField(String t, String tt, String ew) { this.textKey = t; this.tooltipKey = tt; this.endsWith = ew; }

            @Override public String getTextLocator()           { return textKey; }
            @Override public String getToolTipContentLocator() { return tooltipKey; }
            @Override public String getEndsWith()              { return endsWith; }
            @Override public String getExternalFileName()      { return PROPS; }
            @Override public Object[] getArgs()                { return new Object[0]; }
        }

        // 6b. RoleSelector — MultipleIdenticalDropdowns (2 roles: MULTI_TRIGGER, MULTI_LIST)
        enum RoleSelector implements MultipleIdenticalDropdowns {
            ROLE_DROPDOWN("ROLE_MULTI_TRIGGER", "ROLE_MULTI_LIST");

            private final String trigger;
            private final String list;
            RoleSelector(String t, String l) { this.trigger = t; this.list = l; }

            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{1, "Admin"}; }
        }

        // 6c. FileUpload — FileInputElement (single role: INPUT)
        enum FileUpload implements FileInputElement {
            MAPPING_CSV("UPLOAD_INPUT");

            private final String key;
            FileUpload(String k) { this.key = k; }

            @Override public String getInputLocator()     { return key; }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        }
    }

    // ========================================================================
    // 7. Pagination — Clickable with nested PageSizeDropdown
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

        // 7a. PageSizeDropdown — Dropdown (2 roles: TRIGGER, LIST)
        enum PageSizeDropdown implements Dropdown {
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

