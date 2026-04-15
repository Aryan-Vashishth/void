package elements.meta;

import core.logging.CustomLogger;

import java.util.Map;

/**
 * EnumClassRegistry acts as a central registry for mapping human-readable
 * context labels (strings) to their corresponding enum classes that represent
 * UI elements in the test automation framework.
 *
 * <p>
 * This design enables dynamic resolution of enums (for clicking, searching, etc.)
 * using context-aware keys rather than hardcoded references. The context label
 * can represent different parts of the UI (like navigation bars, popups, tiles, etc.)
 * and is mapped to a strongly-typed enum class, allowing generic and reusable
 * UI action utilities.
 * </p>
 */
public class EnumClassRegistry {

    /**
     * Resolves a string key using the given prefix and suffix, which typically
     * represent the UI element and its context (e.g., prefix="tiles", suffix="admin_home").
     * This utility allows dynamic, context-aware enum resolution in the framework.
     *
     * <p>
     * If the full "{prefix}-{suffix}" key is present in the CONTEXT_MAP, it returns that.
     * Otherwise, it checks for just the suffix. Throws an exception if neither exists.
     * </p>
     *
     * @param keyPrefix UI element or feature (e.g., "tiles", "actions_dropdown")
     * @param keySuffix Context or module (e.g., "admin_home", "account_mapping")
     * @return The fully resolved context key present in the CONTEXT_MAP.
     * @throws IllegalArgumentException If no mapping exists for the provided prefix/suffix.
     */
    public static String resolveKeyUsingPrefixAndSuffix(String keyPrefix, String keySuffix) {
        // Construct the composite key using lower case for normalization
        String key = (keyPrefix + "-" + keySuffix).toLowerCase();
        if (CONTEXT_MAP.containsKey(key)) {
            // If the composite key exists, log and return
            CustomLogger.debug.resolved("resolveContextLabel(): " + key,
                    "Key Prefix: ", keyPrefix,
                    "Key Suffix: ", keySuffix
            );
            return key;
        } else if (CONTEXT_MAP.containsKey(keySuffix.toLowerCase())) {
            // If only the suffix exists as a key, log and return
            CustomLogger.debug.resolved("resolveContextLabel(): " + key,
                    "Key Prefix: ", keyPrefix,
                    "Key Suffix: ", keySuffix
            );
            return keySuffix.toLowerCase();
        } else {
            // Neither the composite nor suffix-only key exists
            throw new IllegalArgumentException("No enum registered for keyPrefix: '"
                    + keyPrefix + "' with keySuffix: '" + keySuffix + "'");
        }
    }


    /**
     * CONTEXT_MAP is the central mapping between string-based context keys
     * (e.g., "tiles-admin_home", "navigation_bar-ui") and their corresponding
     * enum classes. These enums encapsulate locators and behaviors for specific
     * UI elements or element groups within the automation framework.
     *
     * <p>
     * Key format is typically "{element}-{context}", where:
     * <ul>
     *     <li><b>element</b>: UI component or feature (e.g., "tiles", "navigation_bar")</li>
     *     <li><b>context</b>: UI page or module (e.g., "admin_home", "account_mapping")</li>
     * </ul>
     * </p>
     */
    public static final Map<String, Class<? extends Enum<?>>> CONTEXT_MAP = Map.ofEntries(
//            // ====== COMMON ELEMENTS ======
//            Map.entry("navigation_bar-ui", CommonElements.NavigationBar.class),
//            Map.entry("switcher-ui", CommonElements.VartopiaSwitcher.class),
//            Map.entry("quick_search-ui", CommonElements.quickSearch.class),
//
//            // ====== NEW REGISTRATION =======
//            Map.entry("partner_information-new_registration", NewRegistrationElements.PartnerInformation.class),
//            Map.entry("program_selection-new_registration", NewRegistrationElements.ProgramSelection.class),
//            Map.entry("opportunity_information-new_registration", NewRegistrationElements.OpportunityInformation.class),
//            Map.entry("sales_rep_and_end_customer_information-new_registration", NewRegistrationElements.SalesRepAndEndCustomerInformation.class),
//            //------ Navigation-------
//            Map.entry("partner_information_navigation-new_registration", NewRegistrationElements.PartnerInformation.Navigation.class),
//            Map.entry("program_selection_navigation-new_registration", NewRegistrationElements.ProgramSelection.Navigation.class),
//            Map.entry("opportunity_information_navigation-new_registration", NewRegistrationElements.OpportunityInformation.Navigation.class),
//            Map.entry("sales_rep_and_end_customer_information_navigation-new_registration", NewRegistrationElements.SalesRepAndEndCustomerInformation.Navigation.class),
//
//
//            // ====== ADMIN HOME ELEMENTS ======
//            Map.entry("tiles-admin_home", AdminHomeElements.Tiles.class),
//
//            // ====== ACCOUNT MAPPING ELEMENTS (Account Mapping Home Page) ======
//            Map.entry("default_reports_tiles-account_mapping_home_page", AccountMappingElements.defaultReportsTiles.class),
//            Map.entry("import_records_dropdown-account_mapping_home_page", AccountMappingElements.importRecordsDropdown.class),
//
//            // ====== IMPORT RECORDS POPUP ELEMENTS ======
//            Map.entry("navigation_buttons-import_records_popup", AccountMappingElements.ImportRecordsPopup.navigationButtons.class),
//            Map.entry("current_view_headers-import_records_popup", AccountMappingElements.ImportRecordsPopup.popupViewsHeaders.class),
//            Map.entry("file_upload_field-import_records_popup", AccountMappingElements.ImportRecordsPopup.uploadField.class),
//            Map.entry("table-import_records_popup", AccountMappingElements.ImportRecordsPopup.importRecordsTable.class),
//            Map.entry("table_row_buttons-import_records_popup", AccountMappingElements.ImportRecordsPopup.importRecordsTable.addRemoveRowButtons.class),
//
//            // ====== RECORDS PAGE ELEMENTS (Account Mapping) ======
//            Map.entry("records_grid-account_mapping", RecordsPageElements.RecordsGridTable.class),
//            Map.entry("records_search_bar-account_mapping", RecordsPageElements.SearchBars.class),
//            Map.entry("default_reports-account_mapping", RecordsPageElements.AccountMapping.DefaultReports.class),
//            Map.entry("actions_dropdown-account_mapping", RecordsPageElements.AccountMapping.ActionsDropdown.class),
//            Map.entry("actions_import_records_sub_dropdown-account_mapping", RecordsPageElements.AccountMapping.ActionsDropdown.ImportRecords.class),
//            Map.entry("group_by_dropdown-account_mapping", RecordsPageElements.AccountMapping.GroupByDropdown.class),
//            Map.entry("bulk_update_dropdown-account_mapping", RecordsPageElements.AccountMapping.BulkUpdateDropdown.class),
//            Map.entry("three_dots_menu-account_mapping", RecordsPageElements.AccountMapping.ThreeDotsMenu.class),
//            Map.entry("three_dots_rows-account_mapping", RecordsPageElements.ThreeDotsRows.class),
//
//            // ====== RECORDS PAGE ELEMENTS (Registrations) ======
//            Map.entry("actions_dropdown-registrations", RecordsPageElements.Registrations.ActionsDropdown.class),
//            Map.entry("group_by_dropdown-registrations", RecordsPageElements.Registrations.GroupByVendorDropdown.class),
//
//            // ====== MANAGE USERS ELEMENTS ======
//            Map.entry("filter_text-manage_users", ManageUsersElements.FilterBy.TextInputField.class),
//            Map.entry("filter_toggle-manage_users", ManageUsersElements.FilterBy.Toggle.class),
//            Map.entry("filter_dropdown-manage_users", ManageUsersElements.FilterBy.UserTypeDropdown.class),
//            Map.entry("search_button-manage_users", ManageUsersElements.FilterBy.SearchButton.class),
//            Map.entry("user_tile_readonly-manage_users", ManageUsersElements.UserCards.class),
//            Map.entry("user_tile_button-manage_users", ManageUsersElements.UserCardClickableElement.class),
//            Map.entry("user_tile_tooltip-manage_users", ManageUsersElements.UserTileToolTipElement.class)
    );
}
