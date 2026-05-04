package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for file upload fields (e.g., &lt;input type="file"/&gt;).
 *
 * <p>Defines the structural contract for file input elements — exposes locator key
 * and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → FileInputTarget → FileInputAction
 * </pre>
 */
public interface FileInputTarget extends Element {

    /** Locator key for use in the property file. */
    String getInputLocator();

    @Override
    default String getPrimaryLocator() { return getInputLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getInputLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String key = getInputLocator();
        if (key != null && !key.isBlank()) roles.put(ElementRole.INPUT, key);
        return roles;
    }
}

