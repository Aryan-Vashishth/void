package elements.api;

import elements.meta.ElementRole;

/**
 * Dropdown variant with integrated search capability.
 * <p>Roles: {@link ElementRole#TRIGGER}, {@link ElementRole#SEARCH_INPUT}, {@link ElementRole#SEARCH_BUTTON}, {@link ElementRole#SEARCH_RESULT}</p>
 */
public interface SearchableDropdown extends Dropdown, Searchable {
    @Override
    String getSearchInputLocator();

    @Override
    String getSearchButtonLocator();

    @Override
    String getTriggerLocator();

    @Override
    String getSearchResultLocator();

    @Override
    default String getInputLocator() {
        return Searchable.super.getInputLocator();
    }

    @Override
    default String getListLocator(){
     return getSearchResultLocator();
    }

    @Override
    default String getPrimaryLocator() {
        return Dropdown.super.getPrimaryLocator();
    }

    @Override
    default String getSecondaryLocator() {
        return Dropdown.super.getSecondaryLocator();
    }

    @Override
    default String getDisplayText() {
        return Dropdown.super.getDisplayText();
    }

    @Override
    default int getIndex() {
        return Dropdown.super.getIndex();
    }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();


    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if(trigger!=null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String input = getSearchInputLocator();
        if(input!=null && !input.isBlank() && !input.equals(trigger)) roles.put(ElementRole.SEARCH_INPUT, input);
        String button = getSearchButtonLocator();
        if(button!=null && !button.isBlank() && !button.equals(trigger) && !button.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, button);
        String list = getListLocator();
        if(list!=null && !list.isBlank() && !list.equals(trigger) && !list.equals(input) && !list.equals(button)) roles.put(ElementRole.SEARCH_RESULT, list);
        return roles;
    }

}
