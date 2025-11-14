package Elements.interfacesv1;
import Elements.ElementRole;

/**
 * Represents a dropdown with a trigger element and a list container (options panel).
 * <p>Roles: {@link ElementRole#TRIGGER} (button/icon) and {@link ElementRole#LIST} (options)</p>
 */
public interface Dropdown extends Clickable, ListElement {

    @Override
    String getTriggerLocator();

    @Override
    String getListLocator();


    @Override
    default String getPrimaryLocator() {
        return getTriggerLocator();
    }

    @Override
    default String getSecondaryLocator() {
        return getListLocator();
    }

    @Override
    default String getDisplayText() {
        return Clickable.super.getDisplayText();
    }


    @Override
    default int getIndex() {
        return 0;
    }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if(trigger!=null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String list = getListLocator();
        if(list!=null && !list.isBlank() && !list.equals(trigger)) roles.put(ElementRole.LIST, list);
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
