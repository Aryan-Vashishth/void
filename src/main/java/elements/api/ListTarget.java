package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for list/collection container elements (UL/OL, options panel, card deck).
 *
 * <p>Defines the structural contract for list containers — exposes locator key
 * and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Role: {@link ElementRole#LIST}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ListTarget → ListAction
 * </pre>
 */
public interface ListTarget extends Element {

    /** Key for the dynamic list XPath (e.g., with %s for argument substitution). */
    String getListLocator();

    /** Index within the list, if needed. */
    int getIndex();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListLocator();
    }

    /** Build role map exposing LIST only. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String list = getListLocator();
        if (list != null && !list.isBlank()) roles.put(ElementRole.LIST, list);
        return roles;
    }
}

