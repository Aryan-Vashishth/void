package elements.api;

import elements.meta.ElementRole;

/**
 * Table variant supporting row insertion/removal and footer input editing.
 * <p>Extends base table roles with: {@link ElementRole#ADD_ROW_BUTTON}, {@link ElementRole#REMOVE_ROW_BUTTON}, {@link ElementRole#FOOTER_INPUT_ROW}</p>
 */
public interface WritableTableElement extends TableElement {
    /**
     * Key for the locator of the "Add Row" button (required for writable tables).
     */
    default String getAddRowButtonLocator(){ return null; }

    /**
     * Key for the locator of the "Remove Row" button (optional).
     */
    default String getRemoveRowButtonLocator(){ return null; }

    /**
     * Key for the footer input row (where new values may be typed) (optional).
     */
    default String getFooterInputRowLocator(){ return null; }

    /**
     * Primary locator for writable tables defaults to the underlying table key.
     */
    @Override
    default String getPrimaryLocator(){ return getTableLocator(); }

    /** Build role map including base table roles + writable action roles. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>(TableElement.super.getAllLocatorRoles());
        String add = getAddRowButtonLocator();
        if(add!=null && !add.isBlank() && !roles.containsValue(add)) roles.put(ElementRole.ADD_ROW_BUTTON, add);
        String remove = getRemoveRowButtonLocator();
        if(remove!=null && !remove.isBlank() && !roles.containsValue(remove)) roles.put(ElementRole.REMOVE_ROW_BUTTON, remove);
        String footer = getFooterInputRowLocator();
        if(footer!=null && !footer.isBlank() && !roles.containsValue(footer)) roles.put(ElementRole.FOOTER_INPUT_ROW, footer);
        return roles;
    }

}
