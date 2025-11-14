package Elements.interfacesv1;

import Elements.ElementRole;

/**
 * Represents a single text input field (e.g., &lt;input type="text"/&gt; or textarea).
 * <p>Primary locator role: {@link ElementRole#INPUT}</p>
 */
public interface TextInputField extends Element {
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
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String input = getInputLocator();
        if(input!=null && !input.isBlank()) roles.put(ElementRole.INPUT, input);
        return roles;
    }

    /** @deprecated Use {@link #getAllLocatorRoles()} */
    @Deprecated
    default java.util.Map<String,String> getAllLocators(){
        java.util.Map<String,String> legacy = new java.util.LinkedHashMap<>();
        getAllLocatorRoles().forEach((r,v)-> legacy.put(r.name(), v));
        return legacy;
    }
}
