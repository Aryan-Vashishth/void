package Elements.interfacesv2;

/**
 * TableElement is for representing tables and sub-elements (rows, columns, headers) in UI automation.
 * Extends BaseElement for locator property and dynamic arguments.
 */
public interface TableElement extends BaseElement {
    /**
     * Key for the main table XPath pattern.
     */
    String getTableKey();

    /**
     * Key for row locator XPath (optional).
     */
    default String getRowKey() { return null; }

    /**
     * Key for column locator XPath (optional).
     */
    default String getColumnKey() { return null; }

    /**
     * Key for specific cell XPath (optional).
     */
    default String getCellKey() { return null; }

    /**
     * Key for table header XPath (optional).
     */
    default String getHeaderKey() { return null; }

    /**
     * Returns a display text for this table (for logs).
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTableKey();
    }
}
