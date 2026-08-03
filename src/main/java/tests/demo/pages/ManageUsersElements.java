package tests.demo.pages;

import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.capability.*;
import core.utils.ResolvableEnum;

public interface ManageUsersElements {

    enum UserCards implements ReadOnly, ResolvableEnum {
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
    }
}

