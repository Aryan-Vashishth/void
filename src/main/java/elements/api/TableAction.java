package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link TableTarget}.
 *
 * <p>Provides a {@link #scrollToTable()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → TableAction
 * </pre>
 */
public interface TableAction extends TableTarget {

    /**
     * Produces an Action that scrolls the table into view.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return scrollToTable Action
     */
    default Action scrollToTable() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TABLE);
            engine.scrollTo(descriptor);
        };
    }
}

