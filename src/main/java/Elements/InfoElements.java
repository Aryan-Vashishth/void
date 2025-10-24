package Elements;

import Elements.Interfaces.ReadOnlyElement;
import Elements.Interfaces.ResolvableEnum;

public enum InfoElements implements ReadOnlyElement, ResolvableEnum {
    CURRENT_USER_TYPE,
    ALL_TOOLTIPS;

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public String getPropertyFile() {
        return "info-elements.properties";
    }

    @Override
    public Object[] getArgs() {
        return new Object[0];
    }

    @Override
    public String getDisplayText() {
        return ReadOnlyElement.super.getDisplayText();
    }

}
