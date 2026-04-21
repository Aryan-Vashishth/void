package elements;

import elements.api.*;
import elements.meta.ElementRole;

import java.util.Map;

/**
 * Element descriptors for the Test Automation Practice page.
 *
 * @see <a href="https://testautomationpractice.blogspot.com/">Practice Page</a>
 */
public interface PracticePageElements {

    String FILE = "practice-page-elements.properties";

    // -----------------------------------------------------------------------
    // Page Header
    // -----------------------------------------------------------------------

    /**
     * Static heading/banner of the practice page.
     */
    enum PageHeader implements ReadOnlyElement, ResolvableEnum {
        PAGE_HEADING("PAGE_HEADING");

        private final String key;
        PageHeader(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTextLocator()      { return key; }

        @Override public String getPrimaryLocator()              { return ReadOnlyElement.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return ReadOnlyElement.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return ReadOnlyElement.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Form – Text Input Fields
    // -----------------------------------------------------------------------

    /**
     * Single-line and multi-line text inputs on the main registration form.
     */
    enum FormInputs implements TextInputField, ResolvableEnum {
        NAME_INPUT("NAME_INPUT"),
        PHONE_INPUT("PHONE_INPUT"),
        EMAIL_INPUT("EMAIL_INPUT"),
        ADDRESS_INPUT("ADDRESS_INPUT"),
        DATE_PICKER_INPUT("DATE_PICKER_INPUT");

        private final String key;
        FormInputs(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getInputLocator()     { return key; }

        @Override public String getPrimaryLocator()              { return TextInputField.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return TextInputField.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return TextInputField.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Form – Gender Radio Buttons
    // -----------------------------------------------------------------------

    /**
     * Gender radio-button elements (Male / Female).
     */
    enum GenderRadio implements Clickable, ResolvableEnum {
        GENDER_MALE_RADIO("GENDER_MALE_RADIO"),
        GENDER_FEMALE_RADIO("GENDER_FEMALE_RADIO");

        private final String key;
        GenderRadio(String k) { this.key = k; }

        @Override public String getExternalFileName()  { return FILE; }
        @Override public String getTriggerLocator()    { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Form – Days Checkboxes
    // -----------------------------------------------------------------------

    /**
     * Day-of-week checkboxes.  Each constant maps to its own XPath locator.
     */
    enum DayCheckboxes implements Checkbox, ResolvableEnum {
        CHECKBOX_MONDAY("CHECKBOX_MONDAY"),
        CHECKBOX_TUESDAY("CHECKBOX_TUESDAY"),
        CHECKBOX_WEDNESDAY("CHECKBOX_WEDNESDAY"),
        CHECKBOX_THURSDAY("CHECKBOX_THURSDAY"),
        CHECKBOX_FRIDAY("CHECKBOX_FRIDAY"),
        CHECKBOX_SATURDAY("CHECKBOX_SATURDAY"),
        CHECKBOX_SUNDAY("CHECKBOX_SUNDAY");

        private final String key;
        DayCheckboxes(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Form – Country Native Select Dropdown
    // -----------------------------------------------------------------------

    /**
     * Native {@code <select>} element for Country.
     * <p>TRIGGER = the {@code <select>} tag; LIST = individual {@code <option>} (parameterised).</p>
     */
    enum CountryDropdown implements Dropdown, ResolvableEnum {
        COUNTRY("COUNTRY_TRIGGER", "COUNTRY_LIST");

        private final String trigger;
        private final String list;
        CountryDropdown(String t, String l) { this.trigger = t; this.list = l; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return trigger; }
        @Override public String getListLocator()      { return list; }

        @Override public String getPrimaryLocator()              { return Dropdown.super.getPrimaryLocator(); }
        @Override public String getSecondaryLocator()            { return Dropdown.super.getSecondaryLocator(); }
        @Override public String getDisplayText()                 { return Dropdown.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Dropdown.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
        @Override public int getIndex()                          { return 0; }
    }

    // -----------------------------------------------------------------------
    // Form – Colors Bootstrap Dropdown
    // -----------------------------------------------------------------------

    /**
     * Bootstrap-style dropdown for selecting a colour.
     */
    enum ColorsDropdown implements Dropdown, ResolvableEnum {
        COLORS("COLORS_TRIGGER", "COLORS_LIST");

        private final String trigger;
        private final String list;
        ColorsDropdown(String t, String l) { this.trigger = t; this.list = l; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return trigger; }
        @Override public String getListLocator()      { return list; }

        @Override public String getPrimaryLocator()              { return Dropdown.super.getPrimaryLocator(); }
        @Override public String getSecondaryLocator()            { return Dropdown.super.getSecondaryLocator(); }
        @Override public String getDisplayText()                 { return Dropdown.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Dropdown.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
        @Override public int getIndex()                          { return 0; }
    }

    // -----------------------------------------------------------------------
    // Form – Action Buttons
    // -----------------------------------------------------------------------

    /**
     * Form-level action buttons (Submit / Reset).
     */
    enum FormButtons implements Clickable, ResolvableEnum {
        SUBMIT_BUTTON("SUBMIT_BUTTON"),
        RESET_BUTTON("RESET_BUTTON");

        private final String key;
        FormButtons(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Alerts / Dialog Triggers
    // -----------------------------------------------------------------------

    /**
     * Buttons that trigger browser native alert / confirm / prompt dialogs.
     */
    enum AlertButtons implements Clickable, ResolvableEnum {
        ALERT_BUTTON("ALERT_BUTTON"),
        CONFIRM_BUTTON("CONFIRM_BUTTON"),
        PROMPT_BUTTON("PROMPT_BUTTON");

        private final String key;
        AlertButtons(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // jQuery Autocomplete / Search
    // -----------------------------------------------------------------------

    /**
     * jQuery UI autocomplete field + suggestion list.
     */
    enum AutocompleteSearch implements SearchField, ResolvableEnum {
        SEARCH("SEARCH_INPUT", "SEARCH_LIST");

        private final String input;
        private final String list;
        AutocompleteSearch(String i, String l) { this.input = i; this.list = l; }

        @Override public String getExternalFileName()     { return FILE; }
        @Override public String getSearchInputLocator()   { return input; }
        @Override public String getSearchButtonLocator()  { return list; }

        @Override public String getPrimaryLocator()              { return SearchField.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return SearchField.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return SearchField.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // HTML Book Table
    // -----------------------------------------------------------------------

    /**
     * Static HTML table ("BookTable") listing book names, authors, etc.
     */
    enum BookTable implements TableElement, ResolvableEnum {
        BOOK_TABLE("TABLE_CONTAINER", "TABLE_ROW", "TABLE_HEADER", "TABLE_CELL");

        private final String table;
        private final String row;
        private final String header;
        private final String cell;
        BookTable(String t, String r, String h, String c) {
            this.table = t; this.row = r; this.header = h; this.cell = c;
        }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTableLocator()     { return table; }
        @Override public String getRowLocator()       { return row; }
        @Override public String getHeaderLocator()    { return header; }
        @Override public String getCellLocator()      { return cell; }

        @Override public String getPrimaryLocator()              { return getTableLocator(); }
        @Override public String getDisplayText()                 { return TableElement.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return TableElement.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Dynamic / Product Web Table
    // -----------------------------------------------------------------------

    /**
     * Dynamic product table ("productTable").
     */
    enum WebTable implements TableElement, ResolvableEnum {
        PRODUCT_TABLE("WEB_TABLE_CONTAINER", "WEB_TABLE_ROW", "WEB_TABLE_HEADER", "WEB_TABLE_CELL");

        private final String table;
        private final String row;
        private final String header;
        private final String cell;
        WebTable(String t, String r, String h, String c) {
            this.table = t; this.row = r; this.header = h; this.cell = c;
        }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTableLocator()     { return table; }
        @Override public String getRowLocator()       { return row; }
        @Override public String getHeaderLocator()    { return header; }
        @Override public String getCellLocator()      { return cell; }

        @Override public String getPrimaryLocator()              { return getTableLocator(); }
        @Override public String getDisplayText()                 { return TableElement.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return TableElement.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Drag and Drop
    // -----------------------------------------------------------------------

    /**
     * Source (draggable) and target (droppable) elements for drag-and-drop interactions.
     */
    enum DragDrop implements Clickable, ResolvableEnum {
        DRAG_SOURCE("DRAG_SOURCE"),
        DROP_TARGET("DROP_TARGET");

        private final String key;
        DragDrop(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Double-Click
    // -----------------------------------------------------------------------

    /**
     * Double-click button and its result field.
     */
    enum DoubleClick implements Clickable, ResolvableEnum {
        DOUBLE_CLICK_BUTTON("DOUBLE_CLICK_BUTTON"),
        DOUBLE_CLICK_RESULT("DOUBLE_CLICK_RESULT");

        private final String key;
        DoubleClick(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Right-Click / Context Menu
    // -----------------------------------------------------------------------

    /**
     * Right-click area and context-menu list.
     */
    enum ContextMenu implements Clickable, ResolvableEnum {
        RIGHT_CLICK_BUTTON("RIGHT_CLICK_BUTTON"),
        CONTEXT_MENU("CONTEXT_MENU");

        private final String key;
        ContextMenu(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // iFrame
    // -----------------------------------------------------------------------

    /**
     * iFrame container and inner body element for switching frame context.
     */
    enum IFrame implements ReadOnlyElement, ResolvableEnum {
        IFRAME_CONTAINER("IFRAME_CONTAINER"),
        IFRAME_TEXT_BODY("IFRAME_TEXT_BODY");

        private final String key;
        IFrame(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTextLocator()      { return key; }

        @Override public String getPrimaryLocator()              { return ReadOnlyElement.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return ReadOnlyElement.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return ReadOnlyElement.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // New Browser Tab / Window
    // -----------------------------------------------------------------------

    /**
     * Button that opens a new browser tab/window.
     */
    enum WindowHandling implements Clickable, ResolvableEnum {
        NEW_TAB_BUTTON("NEW_TAB_BUTTON");

        private final String key;
        WindowHandling(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }

        @Override public String getPrimaryLocator()              { return Clickable.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return Clickable.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return Clickable.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Key Press Section
    // -----------------------------------------------------------------------

    /**
     * Key-press test input and result display.
     */
    enum KeyPressArea implements TextInputField, ResolvableEnum {
        KEY_PRESS_INPUT("KEY_PRESS_INPUT");

        private final String key;
        KeyPressArea(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getInputLocator()     { return key; }

        @Override public String getPrimaryLocator()              { return TextInputField.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return TextInputField.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return TextInputField.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }

    // -----------------------------------------------------------------------
    // Key Press Result (read-only display)
    // -----------------------------------------------------------------------

    /**
     * Result text shown after a key press event.
     */
    enum KeyPressResult implements ReadOnlyElement, ResolvableEnum {
        KEY_PRESS_RESULT("KEY_PRESS_RESULT");

        private final String key;
        KeyPressResult(String k) { this.key = k; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTextLocator()      { return key; }

        @Override public String getPrimaryLocator()              { return ReadOnlyElement.super.getPrimaryLocator(); }
        @Override public String getDisplayText()                 { return ReadOnlyElement.super.getDisplayText(); }
        @Override public Map<ElementRole, String> getAllLocatorRoles() { return ReadOnlyElement.super.getAllLocatorRoles(); }
        @Override public Map.Entry<String, String> toEntry()     { return ResolvableEnum.super.toEntry(); }
        @Override public Object[] getArgs()                      { return new Object[0]; }
    }
}

