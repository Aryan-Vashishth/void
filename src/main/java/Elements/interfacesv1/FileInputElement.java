package Elements.interfacesv1;

import Elements.ElementRole;

/**
 * Represents a file upload field (e.g., &lt;input type="file"/&gt;).
 * <p>Role: {@link ElementRole#INPUT}</p>
 */
public interface FileInputElement extends Element {

    /**
     * Locator key for use in the property file.
     */
    String getInputLocator();

    /**
     * Returns a display-friendly text for logging/debugging.
     * Uses the first argument if present, otherwise the key.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getInputLocator();
    }

    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String key = getInputLocator();
        if(key!=null && !key.isBlank()) roles.put(ElementRole.INPUT, key);
        return roles;
    }

    /** @deprecated Use {@link #getAllLocatorRoles()} */
    @Deprecated
    @Override
    default java.util.Map<String,String> getAllLocators(){
        java.util.Map<String,String> legacy = new java.util.LinkedHashMap<>();
        getAllLocatorRoles().forEach((r,v)-> legacy.put(r.name(), v));
        return legacy;
    }
}
