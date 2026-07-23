package core.target;

/**
 * Domain-neutral root abstraction for anything the framework can operate on.
 *
 * <p>A Target is any describable, potentially-parameterizable subject of an
 * engine action -- a web element, an API endpoint, a mobile component, etc.
 * This interface carries only the concepts that are independent of the
 * interaction medium:</p>
 * <ul>
 *   <li>A human-readable display label ({@link #getDisplayText()}) for logging
 *       and reporting.</li>
 *   <li>Optional template arguments ({@link #getArgs()}) for parameterized
 *       targets (e.g., a row index or a dynamic label).</li>
 * </ul>
 *
 * <p>UI-specific concerns (locator keys, locator roles, external locator files)
 * are defined on {@link elements.api.UIElement}, which extends this interface.</p>
 *
 * <p>Implementations are expected to be immutable value descriptors -- typically
 * enum constants. The framework does not enforce this at the type level.</p>
 *
 * @see elements.api.UIElement
 */
public interface Target {

    /** Shared empty-args sentinel -- signals this target requires no template arguments. */
    Object[] NO_ARGS = new Object[0];

    /**
     * Returns a human-readable label for this target.
     * Used in log output and assertion messages.
     *
     * @return non-null display text
     */
    String getDisplayText();

    /**
     * Returns dynamic arguments used to format parameterized locator or address templates.
     * Returns {@link #NO_ARGS} by default (no arguments required).
     *
     * @return argument array; never null
     */
    default Object[] getArgs() {
        return NO_ARGS;
    }

    /**
     * Returns {@code overrides} when non-null and non-empty; otherwise returns
     * {@link #getArgs()}.
     *
     * <p>Centralises the "override args take precedence over the target's own args" rule
     * that would otherwise be repeated as a ternary at every call site.</p>
     *
     * @param overrides caller-supplied argument overrides
     * @return effective argument array; never null
     */
    default Object[] effectiveArgs(Object... overrides) {
        return (overrides != null && overrides.length > 0) ? overrides : getArgs();
    }
}
