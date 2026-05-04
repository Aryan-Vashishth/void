package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.ListTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ListTarget}.
 *
 * <p>Provides {@link #scrollToList()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ListTarget → ListAction
 * </pre>
 */
public interface ListAction extends ListTarget {

    default Action scrollToList() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.scrollTo(descriptor);
        };
    }
}

