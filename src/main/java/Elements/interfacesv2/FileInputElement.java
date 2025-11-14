package Elements.interfacesv2;

/**
 * FileInputElement
 * -----------------------------------------------------------------------------
 * Contract for file upload inputs.
 */
public interface FileInputElement extends BaseElement, ClickableElement {
    String getFileInputLocatorKey();

    @Override
    default String getPrimaryLocator() {
        return getFileInputLocatorKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getFileInputLocatorKey();
    }
}
