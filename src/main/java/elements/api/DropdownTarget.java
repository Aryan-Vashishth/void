package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for dropdown elements with a trigger and a list/options panel.
 *
 * <p>Defines the structural contract — exposes trigger and list locator keys
 * and role mapping. Contains NO execution or Action logic.</p>
 *
 * <p>Roles: {@link ElementRole#TRIGGER} (button/icon) and {@link ElementRole#LIST} (options)</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget ─┐
 *   Element → ListTarget  ─┤→ DropdownTarget → DropdownAction
 * </pre>
 */
public interface DropdownTarget extends ClickTarget, ListTarget {

    @Override
    String getTriggerLocator();

    @Override
    String getListLocator();

    @Override
    default String getPrimaryLocator() { return getTriggerLocator(); }

    @Override
    default String getSecondaryLocator() { return getListLocator(); }

    @Override
    default String getDisplayText() { return ClickTarget.super.getDisplayText(); }

    @Override
    default int getIndex() { return 0; }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String list = getListLocator();
        if (list != null && !list.isBlank() && !list.equals(trigger)) roles.put(ElementRole.LIST, list);
        return roles;
    }
}

