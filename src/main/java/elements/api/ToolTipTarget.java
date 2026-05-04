package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for tooltip elements whose full text appears on hover.
 *
 * <p>Extends {@link ReadOnlyTarget} with tooltip content locator.
 * Contains NO execution or Action logic.</p>
 *
 * <p>Roles: {@link ElementRole#TEXT} (base text) and {@link ElementRole#TOOLTIP_CONTENT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ToolTipTarget → ToolTipAction
 * </pre>
 */
public interface ToolTipTarget extends ReadOnlyTarget {

    String getToolTipContentLocator();

    @Override
    default String getSecondaryLocator() { return getToolTipContentLocator(); }

    /** Returns the tooltip text ending pattern (for fallback matching). */
    String getEndsWith();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(ReadOnlyTarget.super.getAllLocatorRoles());
        String tip = getToolTipContentLocator();
        if (tip != null && !tip.isBlank() && !roles.containsValue(tip)) roles.put(ElementRole.TOOLTIP_CONTENT, tip);
        return roles;
    }
}

