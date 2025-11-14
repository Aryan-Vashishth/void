package Elements.interfacesv2;


public interface DropdownElement extends ClickableElement, ListElement {

    @Override
    default String getPrimaryLocator() {
        return getTriggerKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getListLocator();
    }
}


