package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for non-interactive text/display elements (label, span, static cell).
 *
 * <p>Defines the structural contract — exposes locator key and role mapping.
 * Contains NO execution or Action logic.</p>
 *
 * <p>Primary locator role: {@link ElementRole#TEXT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ReadOnlyAction
 * </pre>
 */
public interface ReadOnlyTarget extends Element {

    /** @return property key for locating the read-only text element. */
    String getTextLocator();

    @Override
    default String getPrimaryLocator() { return getTextLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    /** Builds role map exposing TEXT only. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String text = getTextLocator();
        if (text != null && !text.isBlank()) roles.put(ElementRole.TEXT, text);
        return roles;
    }
}

