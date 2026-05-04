package elements.api;

import elements.meta.ElementRole;

/**
 * Represents a file upload field (e.g., &lt;input type="file"/&gt;).
 * <p>Role: {@link ElementRole#INPUT}</p>
 */
public interface FileInput extends Element {

    /**
     * Locator key for use in the property file.
     */
    String getInputLocator();

    @Override
    default String getPrimaryLocator() { return getInputLocator(); }

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

}

