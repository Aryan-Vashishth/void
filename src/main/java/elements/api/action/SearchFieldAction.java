package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.SearchFieldTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link SearchFieldTarget}.
 *
 * <p>Provides {@link #typeSearch(String)} and {@link #submitSearch()}.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   TextInputTarget + ClickTarget → SearchFieldTarget → SearchFieldAction
 * </pre>
 */
public interface SearchFieldAction extends SearchFieldTarget {

    default Action typeSearch(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_INPUT);
            engine.type(descriptor, text);
        };
    }

    default Action submitSearch() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_BUTTON);
            engine.click(descriptor);
        };
    }
}

