package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.ReadOnlyTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ReadOnlyTarget}.
 *
 * <p>Provides {@link #scrollIntoView()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ReadOnlyAction
 * </pre>
 */
public interface ReadOnlyAction extends ReadOnlyTarget {

    default Action scrollIntoView() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TEXT);
            engine.scrollTo(descriptor);
        };
    }
}

