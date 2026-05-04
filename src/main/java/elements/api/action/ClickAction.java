package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.ClickTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ClickTarget}.
 *
 * <p>Provides {@link #click()} — deferred resolution inside the Action lambda.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → ClickAction
 * </pre>
 */
public interface ClickAction extends ClickTarget {

    default Action click() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }
}

