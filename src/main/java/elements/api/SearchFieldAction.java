package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link SearchFieldTarget}.
 *
 * <p>Provides {@link #typeSearch(String)} and {@link #submitSearch()} methods that return
 * deferred {@link Action}s. Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget ─┐
 *   Element → ClickTarget     ─┤→ SearchFieldTarget → SearchFieldAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       SearchBar.SEARCH.typeSearch("account-123"),
 *       SearchBar.SEARCH.submitSearch()
 *   );
 * </pre>
 */
public interface SearchFieldAction extends SearchFieldTarget {

    /**
     * Produces an Action that types text into the search input.
     * Resolution is deferred against SEARCH_INPUT role.
     *
     * @param text search text
     * @return type Action
     */
    default Action typeSearch(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_INPUT);
            engine.type(descriptor, text);
        };
    }

    /**
     * Produces an Action that clicks the search button.
     * Resolution is deferred against SEARCH_BUTTON role.
     *
     * @return submit Action
     */
    default Action submitSearch() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_BUTTON);
            engine.click(descriptor);
        };
    }
}

