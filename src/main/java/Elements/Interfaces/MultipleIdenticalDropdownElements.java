package Elements.Interfaces;

/**
 * MultipleDropdownElement is a specialized interface for dropdowns where multiple
 * instances may appear (e.g., lists of dropdowns in a table or form).
 * Extends BaseElement for standardized locator/property access.
 */
public interface MultipleIdenticalDropdownElements extends DropdownElement {


    /**
     * Returns a human-readable display text for this dropdown, useful for logs.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListKey();
    }

    /**
     * Returns a new argument array with the index prepended.
     * Useful for accessing a specific instance of a repeated dropdown.
     */
    default Object[] getArgsWithIndex(int index) {
        Object[] original = getArgs();
        Object[] result = new Object[original.length + 1];
        result[0] = index;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }
}
