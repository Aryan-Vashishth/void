package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for elements that can be clicked (button, link, icon, etc.).
 *
 * <p>Defines the structural contract for clickable elements — exposes locator key
 * and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Primary locator role: {@link ElementRole#TRIGGER}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → ClickAction
 * </pre>
 */
public interface ClickTarget extends Element {

    /** @return property key for the clickable element's locator template. */
    String getTriggerLocator();

    @Override
    default String getPrimaryLocator() { return getTriggerLocator(); }

    /** @return human readable label (first arg if present otherwise raw key). */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTriggerLocator();
    }

    /** Builds role map exposing TRIGGER only. */
    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        return roles;
    }
}

