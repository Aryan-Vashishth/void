package Elements.Interfaces;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * For search fields that can dynamically resolve input and result locators/arguments.
 */
public interface SearchableElementInput extends BaseElement, TextInputFieldElement, ClickableElement, ListElement {

    @Override
    default String getPrimaryLocator() {
        return getTriggerKey();
    }

    @Override
    default String getSecondaryLocator() {
        return getTriggerKey();
    }

    /**
     * @return Supplier for input arguments (for the input field locator).
     */
    Supplier<Object[]> getArgsSupplier();


    /**
     * @return Supplier for result arguments (for the result item locator).
     */
    Supplier<Object[]> getResultArgsSupplier();

    /**
     * @return A Supplier that always returns an empty Object[].
     */
    static Supplier<Object[]> emptyArgs() {
        return () -> new Object[]{};
    }

    /**
     * @param count The number of argument slots to generate, each as "%s".
     * @return A Supplier that returns an Object[] of {@code count} "%s" placeholders.
     */
    static Supplier<Object[]> argsForCount(int count) {
        return () -> {
            Object[] arr = new Object[count];
            Arrays.fill(arr, "%s");
            return arr;
        };
    }

    /**
     * Delegates to the supplied input arguments.
     */
    @Override
    default Object[] getArgs() {
        return getArgsSupplier().get();
    }

    @Override
    default String getTextInputLocatorKey() {
        return "";
    }

    /**
     * Display text, usually the first input arg (for logging).
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgsSupplier().get();
        return args.length > 0 && args[0] != null ? args[0].toString() : getTextInputLocatorKey();
    }

    /**
     * @return Optional of the result locator key, if defined.
     */
    default Optional<String> getOptionalResultLocatorKey() {
        String key = getListLocatorKey();
        return (key == null || key.isEmpty()) ? Optional.empty() : Optional.of(key);
    }

    /**
     * Fills all argument slots for the result locator with provided values.
     * If only one value is given but multiple are needed, all are filled with that value.
     * If more values are provided than needed, extra values are ignored.
     * If no value is provided, fills with "%s".
     *
     * @param values Value(s) to fill
     * @return Filled argument array
     */
    default Object[] getFilledResultArgs(Object... values) {
        Object[] argTemplate = getResultArgsSupplier().get();
        Object[] filled = new Object[argTemplate.length];
        for (int i = 0; i < filled.length; i++) {
            filled[i] = (values.length == 0) ? null : values[Math.min(i, values.length - 1)];
        }
        return filled;
    }


    /**
     * @return True if the result locator key is present and non-empty.
     */
    default boolean hasResultLocator() {
        return getOptionalResultLocatorKey().isPresent();
    }


    @Override
    default int getListIndex() {
        return 0;
    }
}
