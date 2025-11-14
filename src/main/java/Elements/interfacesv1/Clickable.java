package Elements.interfacesv1;

import Elements.ElementRole;

/**
 * Element representing a UI component that can be clicked (button, link, icon, etc.).
 * <p>Primary locator role: {@link ElementRole#TRIGGER}</p>
 */
public interface Clickable extends Element {
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
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if(trigger!=null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        return roles;
    }

    /** @deprecated Prefer {@link #getAllLocatorRoles()} */
    @Deprecated
    default java.util.Map<String,String> getAllLocators(){
        java.util.Map<String,String> legacy = new java.util.LinkedHashMap<>();
        getAllLocatorRoles().forEach((r,v)-> legacy.put(r.name(), v));
        return legacy;
    }
}