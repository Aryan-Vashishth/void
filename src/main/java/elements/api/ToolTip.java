package elements.api;

import elements.meta.ElementRole;

/**
 * Represents an element whose full text may appear in a tooltip (title/aria/overlay) when truncated visually.
 * <p>Roles: {@link ElementRole#TEXT} (base text) and {@link ElementRole#TOOLTIP_CONTENT} (resolved full text)</p>
 */
public interface ToolTip extends ReadOnly {

    String getToolTipContentLocator();

    @Override
    default String getSecondaryLocator() {
        return getToolTipContentLocator();
    }

    /**
     * Returns the tooltip text ending pattern (for fallback matching, e.g., "..." or "…").
     */
    String getEndsWith();

    /**
     * Returns a human-readable label for logs. Uses first argument if present, else the key.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>(ReadOnly.super.getAllLocatorRoles());
        String tip = getToolTipContentLocator();
        if(tip!=null && !tip.isBlank() && !roles.containsValue(tip)) roles.put(ElementRole.TOOLTIP_CONTENT, tip);
        return roles;
    }

}

