package Elements.interfacesv2;

/**
 * Represents a table that supports writable operations (inputs in footer, add/remove rows).
 * Extends TableElement for standard table behavior.
 */
public interface WritableTableElement extends TableElement {

    /**
     * Key for locating footer input fields.
     */
    String getFooterInputKey();

    /**
     * Key for the add-row button.
     */
    String getAddButtonKey();

    /**
     * Key for the remove-row button.
     */
    String getRemoveButtonKey();
}
