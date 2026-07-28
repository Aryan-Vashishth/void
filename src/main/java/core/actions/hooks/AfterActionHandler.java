package core.actions.hooks;

/**
 * Marker type for hooks that are valid in Action.after(...).
 *
 * <p>Kernel-owned (ADR-021, runtime-redesign I2.1); moved from
 * {@code core.interactions.hooks}, which retains a deprecated bridge until I9.3.</p>
 */
@FunctionalInterface
public interface AfterActionHandler extends ActionHandler {
}
