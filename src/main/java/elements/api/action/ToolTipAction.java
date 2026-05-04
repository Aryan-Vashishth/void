package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.ToolTipTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ToolTipTarget}.
 *
 * <p>Provides {@link #hover()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ToolTipTarget → ToolTipAction
 * </pre>
 */
public interface ToolTipAction extends ToolTipTarget {

    default Action hover() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TEXT);
            engine.hover(descriptor);
        };
    }
}

