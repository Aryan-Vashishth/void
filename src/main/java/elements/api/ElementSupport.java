package elements.api;

/**
 * Package-private utility: structural enum facts for {@link UIElement} implementations.
 *
 * <p>Scope is intentionally narrow and frozen: exactly three structural helpers.
 * No presentation logic, no resolver-specific formatting. See ADR-017.</p>
 */
final class ElementSupport {

    private ElementSupport() {}

    static String nameOf(UIElement e) {
        return e instanceof Enum<?> en ? en.name() : e.getClass().getSimpleName();
    }

    static Class<?> declaringClassOf(UIElement e) {
        if (e instanceof Enum<?> en) {
            Class<?> dc = en.getDeclaringClass();
            return dc != null ? dc : en.getClass();
        }
        return e.getClass();
    }

    /**
     * Returns the ordinal of an enum-backed element, or throws for non-enum implementors.
     *
     * <p>Throws rather than returning {@code 0} because ordinal is a semantic index value:
     * a silent {@code 0} is indistinguishable from a valid first-position index and would
     * produce wrong list offsets without a visible failure.</p>
     */
    static int ordinalOf(UIElement e) {
        if (e instanceof Enum<?> en) return en.ordinal();
        throw new UnsupportedOperationException(
            e.getClass().getSimpleName() +
            " implements Listable but has no ordinal semantics. Override Listable.getIndex().");
    }
}
