package Elements;

import Elements.interfacesv1.*;

import java.util.Map;

public interface ManageUsersElements {

    enum UserCards implements ReadOnlyElement, ResolvableEnum {
        CONTAINER("CONTAINER"),
        FULL_NAME("FULL_NAME"),
        USERNAME("USERNAME"),
        EMAIL("EMAIL"),
        COMPANY("COMPANY"),
        USER_TYPE("USER_TYPE"),
        LOGIN_AS_BUTTON("LOGIN_AS_BUTTON");
        private final String key; UserCards(String k){this.key=k;}
        @Override public String getExternalFileName(){ return "manage-users-elements.properties"; }
        @Override public String getTextLocator(){ return key; }

        @Override
        public String getPrimaryLocator() {
            return ReadOnlyElement.super.getPrimaryLocator();
        }

        @Override
        public String getDisplayText() {
            return ReadOnlyElement.super.getDisplayText();
        }

        @Override
        public Map<ElementRole, String> getAllLocatorRoles() {
            return ReadOnlyElement.super.getAllLocatorRoles();
        }

        @Override
        public Map.Entry<String, String> toEntry() {
            return ResolvableEnum.super.toEntry();
        }

        @Override public Object[] getArgs(){ return new Object[0]; }
    }
}

