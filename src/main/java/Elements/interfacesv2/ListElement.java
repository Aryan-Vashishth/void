package Elements.interfacesv2;

/**
 * ListElement
 * -----------------------------------------------------------------------------
 * Represents a repeating list or grouped elements (e.g., rows, cards).
 */
public interface ListElement extends BaseElement {
    /** Key for the dynamic list XPath (e.g., with %s for argument substitution). */
    String getListLocator();

    /** Index within the list, if needed. */
    default int getListIndex(){
        return 0;
    };


    @Override
    default String getPrimaryLocator() {
        return getListLocator();
    }

}
