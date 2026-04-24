package elements.api;

import elements.meta.ElementRole;

/**
 * Composite element for a searchable input + action button pair.
 * <p>Roles: {@link ElementRole#SEARCH_INPUT} and {@link ElementRole#SEARCH_BUTTON}</p>
 */
public interface SearchField extends TextInputField, Clickable {

    String getSearchInputLocator();

    String getSearchButtonLocator();

    @Override
    default String getTriggerLocator() {
        return getSearchButtonLocator();
    }

    @Override
    default String getInputLocator() {
        return getSearchInputLocator();
    }

    @Override
    default String getPrimaryLocator() {
        return TextInputField.super.getPrimaryLocator();
    }

    @Override
    default String getDisplayText() {
        return TextInputField.super.getDisplayText();
    }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    /** Build role map including SEARCH_INPUT and SEARCH_BUTTON. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String input = getSearchInputLocator();
        if(input!=null && !input.isBlank()) roles.put(ElementRole.SEARCH_INPUT, input);
        String btn = getSearchButtonLocator();
        if(btn!=null && !btn.isBlank() && !btn.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, btn);
        return roles;
    }

}
