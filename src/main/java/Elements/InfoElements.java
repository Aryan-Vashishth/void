package Elements;

import Elements.interfacesv1.*;
import Elements.interfacesv1.ResolvableEnum;
import Elements.interfacesv1.Element;

public enum InfoElements implements ResolvableEnum, Element {
    CURRENT_USER_TYPE,
    ALL_TOOLTIPS;

    @Override public String getExternalFileName() { return "info-elements.properties"; }
    @Override public String getPrimaryLocator() { return name(); }
    @Override public Object[] getArgs() { return new Object[0]; }
    @Override public java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        roles.put(ElementRole.TEXT, getPrimaryLocator());
        return roles;
    }
}
