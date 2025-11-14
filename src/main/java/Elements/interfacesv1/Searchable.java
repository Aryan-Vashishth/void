package Elements.interfacesv1;

import Elements.ElementRole;

/**
 * Extension of a {@link SearchField} that exposes a result list/panel locator.
 * <p>Adds role: {@link ElementRole#SEARCH_RESULT}</p>
 */
public interface Searchable extends SearchField {

    String getSearchResultLocator();

    /** Build role map including parent roles + SEARCH_RESULT. */
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(SearchField.super.getAllLocatorRoles());
        String result = getSearchResultLocator();
        if (result != null && !result.isBlank() && !roles.containsValue(result))
            roles.put(ElementRole.SEARCH_RESULT, result);
        return roles;
    }

    /** @deprecated Use {@link #getAllLocatorRoles()} */
    @Deprecated
    @Override
    default java.util.Map<String, String> getAllLocators() {
        java.util.Map<String, String> legacy = new java.util.LinkedHashMap<>();
        getAllLocatorRoles().forEach((r, v) -> legacy.put(r.name(), v));
        return legacy;
    }
}
