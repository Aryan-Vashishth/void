package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tests.demo.pages.AccountMappingElements;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Integration test for {@link JsonLocatorMigrator} using the complex
 * {@link AccountMappingElements} class that exercises every {@code Element}
 * sub-interface with deep nesting, properties resolution, and dynamic templates.
 *
 * <p><b>Coverage matrix (interface → enum → expected roles):</b></p>
 * <pre>
 *  ReadOnlyElement          → Header            → TEXT
 *  SearchField              → SearchBar         → SEARCH_INPUT, SEARCH_BUTTON
 *  SearchableDropdown       → StatusDropdown     → TRIGGER, SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT
 *  TextInputField           → DateRange         → INPUT
 *  Checkbox                 → ActiveOnly        → TRIGGER
 *  Clickable                → ApplyButton       → TRIGGER
 *  WritableTableElement     → AccountTable      → TABLE, ROW, COLUMN, CELL, HEADER, ADD_ROW_BUTTON, REMOVE_ROW_BUTTON, FOOTER_INPUT_ROW
 *    └ TextInputField       → InlineEditor      → INPUT
 *  Dropdown                 → BulkActions       → TRIGGER, LIST
 *  Clickable                → ExportButton      → TRIGGER
 *  ToolTipElement           → TooltipField      → TEXT, TOOLTIP_CONTENT
 *  MultipleIdenticalDropdowns → RoleSelector    → MULTI_TRIGGER, MULTI_LIST
 *  FileInputElement         → FileUpload        → INPUT
 *  Clickable                → Pagination        → TRIGGER
 *    └ Dropdown             → PageSizeDropdown  → TRIGGER, LIST
 * </pre>
 */
public class AccountMappingMigratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode root;
    private JsonNode top; // AccountMappingElements node

    @BeforeClass
    public void buildJson() throws IOException {
        String json = JsonLocatorMigrator.buildResolvedJson(AccountMappingElements.class);
        assertNotNull(json);
        assertFalse(json.isBlank());
        root = MAPPER.readTree(json);
        top = root.path("AccountMappingElements");
        assertFalse(top.isMissingNode(), "Top-level 'AccountMappingElements' node must exist");
    }

    // =====================================================================
    // Structure — top-level nesting
    // =====================================================================

    @Test(description = "Top-level key is the interface name")
    public void topLevelKey_isClassName() {
        assertTrue(root.has("AccountMappingElements"));
        assertEquals(root.size(), 1, "Only one top-level key expected");
    }

    @Test(description = "All 7 direct children are present (Header, SearchBar, FilterPanel, AccountTable, ActionBar, DetailPanel, Pagination)")
    public void directChildren_allPresent() {
        assertTrue(top.has("Header"),       "Missing Header");
        assertTrue(top.has("SearchBar"),    "Missing SearchBar");
        assertTrue(top.has("FilterPanel"),  "Missing FilterPanel");
        assertTrue(top.has("AccountTable"), "Missing AccountTable");
        assertTrue(top.has("ActionBar"),    "Missing ActionBar");
        assertTrue(top.has("DetailPanel"),  "Missing DetailPanel");
        assertTrue(top.has("Pagination"),   "Missing Pagination");
    }

    // =====================================================================
    // 1. Header — ReadOnlyElement (single-role TEXT → string value)
    // =====================================================================

    @Test(description = "Header has all 3 constants as keys")
    public void header_hasAllConstants() {
        JsonNode header = top.path("Header");
        assertTrue(header.has("PAGE_TITLE"));
        assertTrue(header.has("BREADCRUMB"));
        assertTrue(header.has("LAST_SYNCED_LABEL"));
    }

    @Test(description = "Header TEXT role values are resolved XPaths (not raw property keys)")
    public void header_valuesAreResolved() {
        JsonNode header = top.path("Header");
        // Phase 19 Part B: single-role emitted as nested { "PAGE_TITLE": { "TEXT": "xpath" } }
        String val = header.path("PAGE_TITLE").path("TEXT").asText();
        assertTrue(val.contains("page-title"), "Expected resolved XPath; got: " + val);
    }

    @Test(description = "Header constants are nested role objects (TEXT role)")
    public void header_valuesAreStrings() {
        JsonNode header = top.path("Header");
        // Phase 19 Part B: single-role always emitted as nested { "ROLE": "value" }
        assertTrue(header.path("PAGE_TITLE").path("TEXT").isTextual());
        assertTrue(header.path("BREADCRUMB").path("TEXT").isTextual());
    }

    // =====================================================================
    // 2. SearchBar — SearchField (2 roles → multi-role object)
    // =====================================================================

    @Test(description = "SearchBar.ACCOUNT_SEARCH has SEARCH_INPUT and SEARCH_BUTTON roles")
    public void searchBar_hasRoles() {
        JsonNode sb = top.path("SearchBar").path("ACCOUNT_SEARCH");
        assertFalse(sb.isMissingNode(), "ACCOUNT_SEARCH constant must exist");
        assertTrue(sb.has("SEARCH_INPUT"),  "Missing SEARCH_INPUT role");
        assertTrue(sb.has("SEARCH_BUTTON"), "Missing SEARCH_BUTTON role");
    }

    @Test(description = "SearchBar role values are resolved XPaths")
    public void searchBar_valuesAreResolved() {
        JsonNode sb = top.path("SearchBar").path("ACCOUNT_SEARCH");
        assertTrue(sb.path("SEARCH_INPUT").asText().contains("Search accounts"),
                "Expected resolved search input XPath");
    }

    // =====================================================================
    // 3a. FilterPanel > StatusDropdown — SearchableDropdown (4 roles)
    // =====================================================================

    @Test(description = "StatusDropdown.USER_STATUS has 4 roles")
    public void statusDropdown_hasFourRoles() {
        JsonNode sd = top.path("FilterPanel").path("StatusDropdown").path("USER_STATUS");
        assertFalse(sd.isMissingNode(), "USER_STATUS constant must exist");
        assertTrue(sd.has("TRIGGER"),       "Missing TRIGGER role");
        assertTrue(sd.has("SEARCH_INPUT"),  "Missing SEARCH_INPUT role");
        assertTrue(sd.has("SEARCH_BUTTON"), "Missing SEARCH_BUTTON role");
        assertTrue(sd.has("SEARCH_RESULT"), "Missing SEARCH_RESULT role");
    }

    @Test(description = "StatusDropdown SEARCH_RESULT contains dynamic %s template")
    public void statusDropdown_searchResultHasTemplate() {
        String val = top.path("FilterPanel").path("StatusDropdown")
                .path("USER_STATUS").path("SEARCH_RESULT").asText();
        assertTrue(val.contains("%s"), "Expected dynamic template with %s; got: " + val);
    }

    // =====================================================================
    // 3b. FilterPanel > DateRange — TextInputField (single role INPUT)
    // =====================================================================

    @Test(description = "DateRange has DATE_FROM and DATE_TO constants")
    public void dateRange_hasConstants() {
        JsonNode dr = top.path("FilterPanel").path("DateRange");
        assertTrue(dr.has("DATE_FROM"));
        assertTrue(dr.has("DATE_TO"));
    }

    @Test(description = "DateRange INPUT role values are resolved (single-role nested object)")
    public void dateRange_valuesAreStrings() {
        JsonNode dr = top.path("FilterPanel").path("DateRange");
        // Phase 19 Part B: single-role emitted as { "DATE_FROM": { "INPUT": "xpath" } }
        assertTrue(dr.path("DATE_FROM").path("INPUT").isTextual());
        assertTrue(dr.path("DATE_FROM").path("INPUT").asText().contains("dateFrom"));
    }

    // =====================================================================
    // 3c. FilterPanel > ActiveOnly — Checkbox (single role TRIGGER)
    // =====================================================================

    @Test(description = "ActiveOnly.ACTIVE_ONLY_TOGGLE TRIGGER role value is resolved")
    public void activeOnly_isResolvedString() {
        JsonNode ao = top.path("FilterPanel").path("ActiveOnly");
        assertTrue(ao.has("ACTIVE_ONLY_TOGGLE"));
        // Phase 19 Part B: single-role emitted as { "ACTIVE_ONLY_TOGGLE": { "TRIGGER": "xpath" } }
        assertTrue(ao.path("ACTIVE_ONLY_TOGGLE").path("TRIGGER").isTextual());
        assertTrue(ao.path("ACTIVE_ONLY_TOGGLE").path("TRIGGER").asText().contains("activeOnly"));
    }

    // =====================================================================
    // 3d. FilterPanel > ApplyButton — Clickable
    // =====================================================================

    @Test(description = "ApplyButton has APPLY and RESET constants")
    public void applyButton_hasConstants() {
        JsonNode ab = top.path("FilterPanel").path("ApplyButton");
        assertTrue(ab.has("APPLY"));
        assertTrue(ab.has("RESET"));
    }

    @Test(description = "ApplyButton.APPLY TRIGGER role resolves to XPath containing 'Apply'")
    public void applyButton_applyResolved() {
        // Phase 19 Part B: single-role emitted as { "APPLY": { "TRIGGER": "xpath" } }
        String val = top.path("FilterPanel").path("ApplyButton").path("APPLY").path("TRIGGER").asText();
        assertTrue(val.contains("Apply"), "Expected Apply in XPath; got: " + val);
    }

    // =====================================================================
    // 4. AccountTable — WritableTableElement (up to 8 roles)
    // =====================================================================

    @Test(description = "AccountTable.MAPPING_GRID has all 8 WritableTable roles")
    public void accountTable_hasAllRoles() {
        JsonNode grid = top.path("AccountTable").path("MAPPING_GRID");
        assertFalse(grid.isMissingNode(), "MAPPING_GRID constant must exist");
        assertTrue(grid.has("TABLE"),            "Missing TABLE role");
        assertTrue(grid.has("ROW"),              "Missing ROW role");
        assertTrue(grid.has("COLUMN"),           "Missing COLUMN role");
        assertTrue(grid.has("CELL"),             "Missing CELL role");
        assertTrue(grid.has("HEADER"),           "Missing HEADER role");
        assertTrue(grid.has("ADD_ROW_BUTTON"),   "Missing ADD_ROW_BUTTON role");
        assertTrue(grid.has("REMOVE_ROW_BUTTON"),"Missing REMOVE_ROW_BUTTON role");
        assertTrue(grid.has("FOOTER_INPUT_ROW"), "Missing FOOTER_INPUT_ROW role");
    }

    @Test(description = "AccountTable ROW value contains dynamic %s placeholder")
    public void accountTable_rowHasTemplate() {
        String val = top.path("AccountTable").path("MAPPING_GRID").path("ROW").asText();
        assertTrue(val.contains("%s"), "Expected %s in ROW template; got: " + val);
    }

    // =====================================================================
    // 4a. AccountTable > InlineEditor — nested TextInputField
    // =====================================================================

    @Test(description = "InlineEditor is nested under AccountTable")
    public void inlineEditor_isNestedUnderTable() {
        JsonNode ie = top.path("AccountTable").path("InlineEditor");
        assertFalse(ie.isMissingNode(), "InlineEditor should be nested under AccountTable");
        assertTrue(ie.has("CELL_EDITOR"), "Missing CELL_EDITOR constant");
    }

    @Test(description = "InlineEditor.CELL_EDITOR INPUT role has dynamic template with %s")
    public void inlineEditor_hasTemplate() {
        // Phase 19 Part B: single-role emitted as { "CELL_EDITOR": { "INPUT": "xpath" } }
        String val = top.path("AccountTable").path("InlineEditor").path("CELL_EDITOR").path("INPUT").asText();
        assertTrue(val.contains("%s"), "Expected dynamic template; got: " + val);
    }

    // =====================================================================
    // 5a. ActionBar > BulkActions — Dropdown (2 roles)
    // =====================================================================

    @Test(description = "BulkActions.BULK_MENU has TRIGGER and LIST roles")
    public void bulkActions_hasTwoRoles() {
        JsonNode ba = top.path("ActionBar").path("BulkActions").path("BULK_MENU");
        assertFalse(ba.isMissingNode());
        assertTrue(ba.has("TRIGGER"), "Missing TRIGGER role");
        assertTrue(ba.has("LIST"),    "Missing LIST role");
    }

    // =====================================================================
    // 5b. ActionBar > ExportButton — Clickable
    // =====================================================================

    @Test(description = "ExportButton.EXPORT TRIGGER role value is resolved")
    public void exportButton_isResolvedString() {
        JsonNode eb = top.path("ActionBar").path("ExportButton");
        assertTrue(eb.has("EXPORT"));
        // Phase 19 Part B: single-role emitted as { "EXPORT": { "TRIGGER": "xpath" } }
        assertTrue(eb.path("EXPORT").path("TRIGGER").isTextual());
        assertTrue(eb.path("EXPORT").path("TRIGGER").asText().contains("export"));
    }

    // =====================================================================
    // 6a. DetailPanel > TooltipField — ToolTipElement (2 roles)
    // =====================================================================

    @Test(description = "TooltipField.ACCOUNT_NAME has TEXT and TOOLTIP_CONTENT roles")
    public void tooltipField_hasTwoRoles() {
        JsonNode tf = top.path("DetailPanel").path("TooltipField").path("ACCOUNT_NAME");
        assertFalse(tf.isMissingNode());
        assertTrue(tf.has("TEXT"),            "Missing TEXT role");
        assertTrue(tf.has("TOOLTIP_CONTENT"), "Missing TOOLTIP_CONTENT role");
    }

    @Test(description = "TooltipField TOOLTIP_CONTENT is resolved to overlay XPath")
    public void tooltipField_tooltipResolved() {
        String val = top.path("DetailPanel").path("TooltipField")
                .path("ACCOUNT_NAME").path("TOOLTIP_CONTENT").asText();
        assertTrue(val.contains("tooltip"), "Expected tooltip in XPath; got: " + val);
    }

    // =====================================================================
    // 6b. DetailPanel > RoleSelector — MultipleIdenticalDropdowns (2 roles)
    // =====================================================================

    @Test(description = "RoleSelector.ROLE_DROPDOWN has MULTI_TRIGGER and MULTI_LIST roles")
    public void roleSelector_hasTwoRoles() {
        JsonNode rs = top.path("DetailPanel").path("RoleSelector").path("ROLE_DROPDOWN");
        assertFalse(rs.isMissingNode());
        assertTrue(rs.has("MULTI_TRIGGER"), "Missing MULTI_TRIGGER role");
        assertTrue(rs.has("MULTI_LIST"),    "Missing MULTI_LIST role");
    }

    @Test(description = "RoleSelector MULTI_TRIGGER has dynamic index placeholder")
    public void roleSelector_triggerHasTemplate() {
        String val = top.path("DetailPanel").path("RoleSelector")
                .path("ROLE_DROPDOWN").path("MULTI_TRIGGER").asText();
        assertTrue(val.contains("%s"), "Expected %s in multi-trigger template; got: " + val);
    }

    // =====================================================================
    // 6c. DetailPanel > FileUpload — FileInputElement (single role)
    // =====================================================================

    @Test(description = "FileUpload.MAPPING_CSV INPUT role value is resolved")
    public void fileUpload_isResolvedString() {
        JsonNode fu = top.path("DetailPanel").path("FileUpload");
        assertTrue(fu.has("MAPPING_CSV"));
        // Phase 19 Part B: single-role emitted as { "MAPPING_CSV": { "INPUT": "xpath" } }
        assertTrue(fu.path("MAPPING_CSV").path("INPUT").isTextual());
        assertTrue(fu.path("MAPPING_CSV").path("INPUT").asText().contains("file"));
    }

    // =====================================================================
    // 7. Pagination — Clickable with nested PageSizeDropdown
    // =====================================================================

    @Test(description = "Pagination has NEXT_PAGE and PREV_PAGE constants")
    public void pagination_hasConstants() {
        JsonNode pg = top.path("Pagination");
        assertTrue(pg.has("NEXT_PAGE"));
        assertTrue(pg.has("PREV_PAGE"));
    }

    @Test(description = "PageSizeDropdown is nested under Pagination")
    public void pageSizeDropdown_isNestedUnderPagination() {
        JsonNode psd = top.path("Pagination").path("PageSizeDropdown");
        assertFalse(psd.isMissingNode(), "PageSizeDropdown should be nested under Pagination");
        assertTrue(psd.has("PAGE_SIZE"), "Missing PAGE_SIZE constant");
    }

    @Test(description = "PageSizeDropdown.PAGE_SIZE has TRIGGER and LIST roles")
    public void pageSizeDropdown_hasTwoRoles() {
        JsonNode ps = top.path("Pagination").path("PageSizeDropdown").path("PAGE_SIZE");
        assertFalse(ps.isMissingNode());
        assertTrue(ps.has("TRIGGER"), "Missing TRIGGER role");
        assertTrue(ps.has("LIST"),    "Missing LIST role");
    }

    // =====================================================================
    // Cross-cutting: no raw property keys leaked as values
    // =====================================================================

    @Test(description = "Header PAGE_TITLE TEXT role is resolved XPath, not raw key")
    public void noRawKeys_headerPageTitle() {
        // Phase 19 Part B: single-role nested — check the leaf TEXT value
        String val = top.path("Header").path("PAGE_TITLE").path("TEXT").asText();
        assertNotEquals(val, "PAGE_TITLE", "Value should be resolved, not the raw key");
    }

    @Test(description = "ApplyButton APPLY TRIGGER role is resolved XPath, not raw key")
    public void noRawKeys_applyButton() {
        // Phase 19 Part B: single-role nested — check the leaf TRIGGER value
        String val = top.path("FilterPanel").path("ApplyButton").path("APPLY").path("TRIGGER").asText();
        assertNotEquals(val, "APPLY_FILTER_TRIGGER", "Value should be resolved, not the raw key");
    }

    // =====================================================================
    // Write test — ensure file can be written
    // =====================================================================

    @Test(description = "writeResolvedJson produces a path using convention")
    public void writeResolvedJson_followsConvention() {
        String className = AccountMappingElements.class.getSimpleName().toLowerCase();
        String expectedSuffix = className + "-locators.json";
        java.nio.file.Path expectedPath = JsonLocatorMigrator.DEFAULT_OUT_DIR.resolve(expectedSuffix);
        assertEquals(expectedPath.getFileName().toString(), expectedSuffix);
    }
}

