package core.engine;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Engine-agnostic locator descriptor.
 *
 * <p>Bridges the gap between VOID's locator resolution system (which produces
 * string-based locator values) and the execution engine (which needs a native
 * locator format). Each {@link UIEngine} implementation translates this descriptor
 * into its own locator type internally.</p>
 *
 * <p>For scoped (parent→child) element lookups, use {@link #withParent(LocatorDescriptor)}
 * to compose a locator tree. The engine resolves the parent first, then searches
 * within that scope for the child element.</p>
 *
 * <p>Example:
 * <pre>
 *   new LocatorDescriptor("//button[@id='apply']", LocatorStrategy.XPATH)
 *   // Scoped: find button within a specific row
 *   row.withParent(tableLocator)
 * </pre>
 *
 * @param value    the resolved locator string (e.g., {@code //button[@id='apply']})
 * @param strategy the locator strategy (XPATH, CSS, ID, NAME)
 * @param args     dynamic substitution args (already applied to value; kept for metadata/logging)
 * @param parent   optional parent descriptor for scoped lookups (null = global scope)
 */
public record LocatorDescriptor(
        String value,
        LocatorStrategy strategy,
        Object[] args,
        LocatorDescriptor parent
) {

    /** Canonical constructor without parent (global scope). */
    public LocatorDescriptor(String value, LocatorStrategy strategy, Object[] args) {
        this(value, strategy, args, null);
    }

    /** Convenience constructor without args or parent. */
    public LocatorDescriptor(String value, LocatorStrategy strategy) {
        this(value, strategy, new Object[0], null);
    }

    /**
     * Returns a new descriptor that is scoped within the given parent.
     * The engine will find the parent element first, then search within it.
     *
     * @param parent the parent scope descriptor
     * @return new descriptor with parent context
     */
    public LocatorDescriptor withParent(LocatorDescriptor parent) {
        return new LocatorDescriptor(this.value, this.strategy, this.args, parent);
    }

    /** @return true if this descriptor has a parent scope */
    public boolean isScoped() {
        return parent != null;
    }

    /**
     * Creates a descriptor by inferring the strategy from the locator value.
     *
     * @param value resolved locator string
     * @return descriptor with inferred strategy
     */
    public static LocatorDescriptor of(String value) {
        return new LocatorDescriptor(value, LocatorStrategy.infer(value));
    }

    /**
     * Creates a descriptor with an explicit strategy.
     *
     * @param value    resolved locator string
     * @param strategy explicit strategy
     * @return descriptor
     */
    public static LocatorDescriptor of(String value, LocatorStrategy strategy) {
        return new LocatorDescriptor(value, strategy);
    }

    /**
     * Creates a descriptor with explicit strategy and args metadata.
     *
     * @param value    resolved locator string
     * @param strategy explicit strategy
     * @param args     substitution args (for logging/diagnostics)
     * @return descriptor
     */
    public static LocatorDescriptor of(String value, LocatorStrategy strategy, Object... args) {
        return new LocatorDescriptor(value, strategy, args);
    }

    @Override @Nonnull
    public String toString() {
        String base = strategy + "=" + value +
                (args != null && args.length > 0 ? " args=" + Arrays.toString(args) : "");
        return parent != null ? base + " [within: " + parent + "]" : base;
    }
}
