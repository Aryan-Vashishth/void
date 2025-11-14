package Elements.interfacesv2;

/**
 * ClickableElement
 * -----------------------------------------------------------------------------
 * Root contract for anything that can be clicked (button, link, tile, trigger).
 * - primaryLocator = click target
 * - secondaryLocator = same as click target (so it can double as trigger)
 */
public interface ClickableElement extends BaseElement {

    /** Logical id for the click target locator. */
    String getTriggerKey();

    // --- Bridge to BaseElement ---

    @Override
    default String getPrimaryLocator() {
        return getTriggerKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getTriggerKey();
    }

}
