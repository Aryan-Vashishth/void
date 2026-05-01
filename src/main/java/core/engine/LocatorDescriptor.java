package core.engine;

import java.util.Arrays;

/**
 * Engine-agnostic locator descriptor.
 *
 * <p>Bridges the gap between VOID's locator resolution system (which produces
 * string-based locator values) and the execution engine (which needs a native
 * locator format). Each {@link UIEngine} implementation translates this descriptor
 * into its own locator type internally.</p>
 *
 * <p>Example:
 * <pre>
 *   new LocatorDescriptor("//button[@id='apply']", LocatorStrategy.XPATH)
 * </pre>
 *
 * @param value    the resolved locator string (e.g., {@code //button[@id='apply']})
 * @param strategy the locator strategy (XPATH, CSS, ID, NAME)
 * @param args     dynamic substitution args (already applied to value; kept for metadata/logging)
 */
public record LocatorDescriptor(
        String value,
        LocatorStrategy strategy,
        Object[] args
) {

    /** Convenience constructor without args. */
    public LocatorDescriptor(String value, LocatorStrategy strategy) {
        this(value, strategy, new Object[0]);
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

    @Override
    public String toString() {
        return strategy + "=" + value +
                (args != null && args.length > 0 ? " args=" + Arrays.toString(args) : "");
    }
}

