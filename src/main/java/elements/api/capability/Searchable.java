package elements.api.capability;

import elements.meta.ElementRole;

/**
 * Capability interface extending SearchField with a result list locator.
 *
 * <p>Additional role: {@link ElementRole#SEARCH_RESULT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Typeable → SearchField → Searchable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Searchable extends SearchField {

    String getSearchResultLocator();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(SearchField.super.getAllLocatorRoles());
        String result = getSearchResultLocator();
        if (result != null && !result.isBlank() && !roles.containsValue(result))
            roles.put(ElementRole.SEARCH_RESULT, result);
        return roles;
    }
}

