package Elements.Interfaces;

/**
 * ListElement
 * -----------------------------------------------------------------------------
 * Represents a repeating list or grouped elements (e.g., rows, cards).
 */
public interface ListElement extends BaseElement {
    /** Key for the dynamic list XPath (e.g., with %s for argument substitution). */
    String getListLocatorKey();

    /** Index within the list, if needed. */
    default int getListIndex(){
        return 0;
    };

    // --- Bridge to BaseElement ---
    @Override
    default String getPrimaryLocator() {
        return getListLocatorKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getListLocatorKey();
    }
}
