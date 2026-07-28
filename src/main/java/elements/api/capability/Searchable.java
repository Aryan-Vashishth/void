package elements.api.capability;

import core.actions.ActionCapability;
import elements.meta.ElementRole;

/**
 * Capability interface extending SearchField with a result list locator.
 *
 * <p>Additional role: {@link ElementRole#SEARCH_RESULT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Typeable → SearchField → Searchable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
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

    @Override
    default ActionCapability capability() { return ActionCapability.SEARCHABLE; }
}

