package core.interactions.hooks;

/**
 * @deprecated Moved to {@link core.actions.hooks.AfterActionHandler} (kernel-owned,
 * runtime-redesign I2.1, ADR-021). This bridge keeps existing imports and
 * implementations compiling; it carries no behavior of its own. Scheduled for
 * removal in I9.3 -- migrate imports to {@code core.actions.hooks.AfterActionHandler}.
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface AfterActionHandler extends core.actions.hooks.AfterActionHandler {
}
