package elements.api;

import elements.meta.ElementRole;

/**
 * Represents a non-interactive text/display element (label, static cell, span).
 * <p>Primary locator role: {@link ElementRole#TEXT}</p>
 */
public interface ReadOnly extends Element {
    /** @return property key for locating the read-only text element. */
    String getTextLocator();

    @Override
    default String getPrimaryLocator() { return getTextLocator(); }

    /** @return display text (first arg or underlying text key). */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    /** Builds role map exposing TEXT only. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String text = getTextLocator();
        if(text!=null && !text.isBlank()) roles.put(ElementRole.TEXT, text);
        return roles;
    }

}

