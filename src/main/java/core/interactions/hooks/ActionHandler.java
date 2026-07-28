package core.interactions.hooks;

/**
 * @deprecated Moved to {@link core.actions.hooks.ActionHandler} (kernel-owned,
 * runtime-redesign I2.1, ADR-021). This bridge keeps existing imports and
 * implementations compiling; it carries no behavior of its own. Scheduled for
 * removal in I9.3 -- migrate imports to {@code core.actions.hooks.ActionHandler}.
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface ActionHandler extends core.actions.hooks.ActionHandler {
}
