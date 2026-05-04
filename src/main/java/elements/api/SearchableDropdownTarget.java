package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for searchable dropdowns (trigger + search input + button + result list).
 *
 * <p>Combines {@link DropdownTarget} and {@link SearchableTarget}. Contains NO Action logic.</p>
 *
 * <p>Roles: {@link ElementRole#TRIGGER}, {@link ElementRole#SEARCH_INPUT},
 * {@link ElementRole#SEARCH_BUTTON}, {@link ElementRole#SEARCH_RESULT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   DropdownTarget ─┐
 *   SearchableTarget┤→ SearchableDropdownTarget → SearchableDropdownAction
 * </pre>
 */
public interface SearchableDropdownTarget extends DropdownTarget, SearchableTarget {

    @Override
    String getSearchInputLocator();

    @Override
    String getSearchButtonLocator();

    @Override
    String getTriggerLocator();

    @Override
    String getSearchResultLocator();

    @Override
    default String getInputLocator() { return getSearchInputLocator(); }

    @Override
    default String getListLocator() { return getSearchResultLocator(); }

    @Override
    default String getPrimaryLocator() { return DropdownTarget.super.getPrimaryLocator(); }

    @Override
    default String getSecondaryLocator() { return DropdownTarget.super.getSecondaryLocator(); }

    @Override
    default String getDisplayText() { return DropdownTarget.super.getDisplayText(); }

    @Override
    default int getIndex() { return DropdownTarget.super.getIndex(); }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String input = getSearchInputLocator();
        if (input != null && !input.isBlank() && !input.equals(trigger)) roles.put(ElementRole.SEARCH_INPUT, input);
        String button = getSearchButtonLocator();
        if (button != null && !button.isBlank() && !button.equals(trigger) && !button.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, button);
        String list = getListLocator();
        if (list != null && !list.isBlank() && !list.equals(trigger) && !list.equals(input) && !list.equals(button)) roles.put(ElementRole.SEARCH_RESULT, list);
        return roles;
    }
}

