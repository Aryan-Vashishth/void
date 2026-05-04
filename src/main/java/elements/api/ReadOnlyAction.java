package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ReadOnlyTarget}.
 *
 * <p>Provides a {@link #scrollIntoView()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ReadOnlyAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(Header.PAGE_TITLE.scrollIntoView());
 * </pre>
 */
public interface ReadOnlyAction extends ReadOnlyTarget {

    /**
     * Produces an Action that scrolls this element into view.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return scrollIntoView Action
     */
    default Action scrollIntoView() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TEXT);
            engine.scrollTo(descriptor);
        };
    }
}

