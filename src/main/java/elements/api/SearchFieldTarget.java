package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for a composite search input + action button pair.
 *
 * <p>Defines the structural contract for search fields — exposes input and button
 * locator keys and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Roles: {@link ElementRole#SEARCH_INPUT} and {@link ElementRole#SEARCH_BUTTON}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget ─┐
 *   Element → ClickTarget     ─┤→ SearchFieldTarget → SearchFieldAction
 * </pre>
 */
public interface SearchFieldTarget extends TextInputTarget, ClickTarget {

    String getSearchInputLocator();

    String getSearchButtonLocator();

    @Override
    default String getTriggerLocator() { return getSearchButtonLocator(); }

    @Override
    default String getInputLocator() { return getSearchInputLocator(); }

    @Override
    default String getPrimaryLocator() { return TextInputTarget.super.getPrimaryLocator(); }

    @Override
    default String getDisplayText() { return TextInputTarget.super.getDisplayText(); }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    /** Build role map including SEARCH_INPUT and SEARCH_BUTTON. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String input = getSearchInputLocator();
        if (input != null && !input.isBlank()) roles.put(ElementRole.SEARCH_INPUT, input);
        String btn = getSearchButtonLocator();
        if (btn != null && !btn.isBlank() && !btn.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, btn);
        return roles;
    }
}

