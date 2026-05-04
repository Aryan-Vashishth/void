package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for text input fields (e.g., &lt;input type="text"/&gt; or textarea).
 *
 * <p>Defines the structural contract for input elements — exposes locator key
 * and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Primary locator role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget → TextInputAction
 * </pre>
 */
public interface TextInputTarget extends Element {

    /** @return property key for the input field locator template. */
    String getInputLocator();

    @Override
    default String getPrimaryLocator() { return getInputLocator(); }

    /** @return readable label (first arg or raw key). */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getInputLocator();
    }

    /** Builds role map exposing INPUT only. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String input = getInputLocator();
        if (input != null && !input.isBlank()) roles.put(ElementRole.INPUT, input);
        return roles;
    }
}

