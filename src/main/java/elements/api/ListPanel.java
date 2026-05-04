package elements.api;

import elements.meta.ElementRole;

/**
 * Represents a repeating collection container (UL/OL, table rows group, card deck, etc.).
 * <p>Role: {@link ElementRole#LIST}</p>
 */
public interface ListPanel extends Element {

    /**
     * Key for the dynamic list XPath (e.g., with %s for argument substitution).
     */
    String getListLocator();

    /**
     * Index within the list, if needed.
     */
    int getIndex();

    /**
     * Returns a human-readable label for logs. Uses the first argument if present, else the list key.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListLocator();
    }

    /** Build role map exposing LIST only. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String list = getListLocator();
        if(list!=null && !list.isBlank()) roles.put(ElementRole.LIST, list);
        return roles;
    }

}

