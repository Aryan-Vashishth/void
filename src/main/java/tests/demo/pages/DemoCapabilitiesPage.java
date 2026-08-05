package tests.demo.pages;

import domain.automation.web.vocabulary.capability.*;
import domain.automation.web.vocabulary.element.LocatorFamily;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Demo page cataloguing every UIElement capability with nested groupings.
 *
 * <pre>
 * DemoCapabilitiesPage
 * +-- PageHeader         (ReadOnly)
 * +-- GlobalSearch       (SearchField   -- SEARCH_INPUT, SEARCH_BUTTON)
 * +-- NotificationBell   (Clickable)
 * +-- FilterBar [interface]
 * |   +-- CategoryFilter (SearchableDropdown -- TRIGGER, SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT)
 * |   +-- DatePicker     (Typeable)
 * |   +-- ShowActive     (Checkable)
 * |   +-- RunButton      (Clickable)
 * +-- Sidebar [interface]
 * |   +-- InfoTooltip    (Hoverable     -- TEXT, TOOLTIP_CONTENT)
 * |   +-- TagSelector    (MultiSelectable -- MULTI_TRIGGER, MULTI_LIST)
 * |   +-- AttachFile     (Uploadable)
 * +-- QuickActions       (Selectable    -- TRIGGER, LIST)
 * |   +-- ActionItems    (Listable)
 * +-- AdvancedSearch     (Searchable    -- SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT)
 * +-- PagerControls      (Clickable + LocatorFamily -- shared %s template)
 *     +-- PageSize       (Selectable)
 * </pre>
 */
public interface DemoCapabilitiesPage {

    String PROPS = "tests/demo/pages/DemoCapabilitiesPage/locators.properties";

    // ── 1. PageHeader ── ReadOnly (TEXT) ──────────────────────────────────────

    enum PageHeader implements ReadOnly {
        PAGE_TITLE,
        BREADCRUMB;

        @Override public String getExternalFileName() { return PROPS; }
    }

    // ── 2. GlobalSearch ── SearchField (SEARCH_INPUT, SEARCH_BUTTON) ──────────

    enum GlobalSearch implements SearchField {
        SITE_SEARCH;

        @Override public String getSearchInputLocator()  { return locatorKeyForRole(ElementRole.SEARCH_INPUT); }
        @Override public String getSearchButtonLocator() { return locatorKeyForRole(ElementRole.SEARCH_BUTTON); }
        @Override public String getExternalFileName()    { return PROPS; }
        @Override public Object[] getArgs()              { return new Object[0]; }
    }

    // ── 3. NotificationBell ── Clickable (TRIGGER) ───────────────────────────

    enum NotificationBell implements Clickable {
        BELL_ICON;

        @Override public String getExternalFileName() { return PROPS; }
    }

    // ── 4. FilterBar ── container interface ──────────────────────────────────

    interface FilterBar {

        // 4a. CategoryFilter -- SearchableDropdown (TRIGGER, SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT)
        enum CategoryFilter implements SearchableDropdown {
            CATEGORY_DROPDOWN;

            @Override public String getTriggerLocator()      { return locatorKeyForRole(ElementRole.TRIGGER); }
            @Override public String getSearchInputLocator()  { return locatorKeyForRole(ElementRole.SEARCH_INPUT); }
            @Override public String getSearchButtonLocator() { return locatorKeyForRole(ElementRole.SEARCH_BUTTON); }
            @Override public String getSearchResultLocator() { return locatorKeyForRole(ElementRole.SEARCH_RESULT); }
            @Override public String getExternalFileName()    { return PROPS; }
            @Override public Object[] getArgs()              { return new Object[]{"Electronics"}; }
        }

        // 4b. DatePicker -- Typeable (INPUT)
        enum DatePicker implements Typeable {
            DATE_FROM,
            DATE_TO;

            @Override public String getExternalFileName() { return PROPS; }
        }

        // 4c. ShowActive -- Checkable (TRIGGER)
        enum ShowActive implements Checkable {
            ACTIVE_TOGGLE;

            @Override public String getExternalFileName() { return PROPS; }
        }

        // 4d. RunButton -- Clickable (TRIGGER)
        enum RunButton implements Clickable {
            APPLY_FILTERS,
            CLEAR_FILTERS;

            @Override public String getExternalFileName() { return PROPS; }
        }
    }

    // ── 5. Sidebar ── container interface ────────────────────────────────────

    interface Sidebar {

        // 5a. InfoTooltip -- Hoverable (TEXT, TOOLTIP_CONTENT)
        enum InfoTooltip implements Hoverable {
            ITEM_DESCRIPTION;

            @Override public String getToolTipContentLocator() { return locatorKeyForRole(ElementRole.TOOLTIP_CONTENT); }
            @Override public String getEndsWith()              { return "..."; }
            @Override public String getExternalFileName()      { return PROPS; }
        }

        // 5b. TagSelector -- MultiSelectable (MULTI_TRIGGER, MULTI_LIST)
        enum TagSelector implements MultiSelectable {
            TAG_DROPDOWN;

            @Override public String getTriggerLocator()   { return locatorKeyForRole(ElementRole.MULTI_TRIGGER); }
            @Override public String getListLocator()      { return locatorKeyForRole(ElementRole.MULTI_LIST); }
            @Override public Object[] getArgs()           { return new Object[]{1, "Featured"}; }
            @Override public String getExternalFileName() { return PROPS; }
        }

        // 5c. AttachFile -- Uploadable (INPUT)
        enum AttachFile implements Uploadable {
            DOCUMENT_UPLOAD;

            @Override public String getExternalFileName() { return PROPS; }
        }
    }

    // ── 6. QuickActions ── Selectable (TRIGGER, LIST) with nested ActionItems ─

    enum QuickActions implements Selectable {
        ACTIONS_MENU;

        @Override public String getTriggerLocator()   { return locatorKeyForRole(ElementRole.TRIGGER); }
        @Override public String getListLocator()      { return locatorKeyForRole(ElementRole.LIST); }
        @Override public String getExternalFileName() { return PROPS; }
        @Override public Object[] getArgs()           { return new Object[]{"Export"}; }

        // 6a. ActionItems -- Listable (LIST)
        enum ActionItems implements Listable {
            EXPORT_CSV,
            EXPORT_PDF,
            PRINT_VIEW;

            @Override public String getExternalFileName() { return PROPS; }
        }
    }

    // ── 7. AdvancedSearch ── Searchable (SEARCH_INPUT, SEARCH_BUTTON, SEARCH_RESULT)

    enum AdvancedSearch implements Searchable {
        KEYWORD_SEARCH;

        @Override public String getSearchInputLocator()  { return locatorKeyForRole(ElementRole.SEARCH_INPUT); }
        @Override public String getSearchButtonLocator() { return locatorKeyForRole(ElementRole.SEARCH_BUTTON); }
        @Override public String getSearchResultLocator() { return locatorKeyForRole(ElementRole.SEARCH_RESULT); }
        @Override public String getExternalFileName()    { return PROPS; }
        @Override public Object[] getArgs()              { return new Object[0]; }
    }

    // ── 8. PagerControls ── Clickable + LocatorFamily (shared %s template) ───

    enum PagerControls implements Clickable, LocatorFamily {
        PREVIOUS, NEXT, FIRST, LAST;

        // 8a. PageSize -- Selectable (TRIGGER, LIST)
        enum PageSize implements Selectable {
            PAGE_SIZE_SELECTOR;

            @Override public String getTriggerLocator()   { return locatorKeyForRole(ElementRole.TRIGGER); }
            @Override public String getListLocator()      { return locatorKeyForRole(ElementRole.LIST); }
            @Override public String getExternalFileName() { return PROPS; }
            @Override public Object[] getArgs()           { return new Object[]{"25"}; }
        }
    }
}
