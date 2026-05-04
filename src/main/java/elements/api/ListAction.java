package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ListTarget}.
 *
 * <p>Provides a {@link #scrollToList()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ListTarget → ListAction
 * </pre>
 */
public interface ListAction extends ListTarget {

    /**
     * Produces an Action that scrolls the list container into view.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return scrollToList Action
     */
    default Action scrollToList() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.scrollTo(descriptor);
        };
    }
}

