package Elements.interfacesv2;

/**
 * TextFieldElement represents a text field/input in the UI.
 * Extends BaseElement for locator property and dynamic arguments.
 */
public interface TextInputFieldElement extends BaseElement {
    /**
     * Key to look up the locator for the text field.
     */
    String getTextInputLocatorKey();

    /**
     * Returns a display-friendly text for logging and reporting.
     * Uses the first argument if present, otherwise uses the key.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getKey();
    }
}
