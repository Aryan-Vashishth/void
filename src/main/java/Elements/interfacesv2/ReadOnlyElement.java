package Elements.interfacesv2;

/**
 * ReadOnlyElement
 * -----------------------------------------------------------------------------
 * Contract for labels, static fields, or read-only UI components.
 */
public interface ReadOnlyElement extends BaseElement {
    String getReadOnlyLocatorKey();

    @Override
    default String getPrimaryLocator() {
        return getReadOnlyLocatorKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getReadOnlyLocatorKey();
    }
}
