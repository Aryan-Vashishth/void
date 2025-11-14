package Elements.interfacesv2;

/**
 * Represents a UI element with a tooltip, supporting locator key and tooltip matching.
 */
public interface ToolTipElement extends BaseElement {

    /**
     * Key to look up the locator for the tooltip element.
     */
    String getTooltipLocatorKey();

    /**
     * Returns the tooltip text ending pattern (for fallback matching, e.g., "..." or "…").
     */
    String getEndsWith();

    /**
     * Returns a human-readable label for logs. Uses first argument if present, else the key.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTooltipLocatorKey();
    }
}
