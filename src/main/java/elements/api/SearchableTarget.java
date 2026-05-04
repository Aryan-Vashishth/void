package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for a searchable element with a result list locator.
 *
 * <p>Extends {@link SearchFieldTarget} with a SEARCH_RESULT locator.
 * Contains NO execution or Action logic.</p>
 *
 * <p>Additional role: {@link ElementRole#SEARCH_RESULT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget ─┐
 *   Element → ClickTarget     ─┤→ SearchFieldTarget → SearchableTarget
 * </pre>
 */
public interface SearchableTarget extends SearchFieldTarget {

    String getSearchResultLocator();

    /** Build role map including parent roles + SEARCH_RESULT. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(SearchFieldTarget.super.getAllLocatorRoles());
        String result = getSearchResultLocator();
        if (result != null && !result.isBlank() && !roles.containsValue(result))
            roles.put(ElementRole.SEARCH_RESULT, result);
        return roles;
    }
}

