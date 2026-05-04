package elements.api;

import core.actions.Action;
import core.engine.LocatorDescriptor;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link SearchField}.
 *
 * <p>Provides {@link #typeSearch(String)} and {@link #submitSearch()} methods that eagerly
 * resolve the locator descriptor and return deferred {@link Action}s for Flow-based execution.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       SearchBar.SEARCH.typeSearch("account-123"),
 *       SearchBar.SEARCH.submitSearch()
 *   );
 * </pre>
 */
public interface SearchFieldElement extends SearchField {

    /**
     * Produces an Action that types text into the search input.
     * Descriptor is resolved against SEARCH_INPUT role.
     *
     * @param text search text
     * @return type Action
     */
    default Action typeSearch(String text) {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_INPUT);
        return (engine) -> engine.type(descriptor, text);
    }

    /**
     * Produces an Action that clicks the search button.
     * Descriptor is resolved against SEARCH_BUTTON role.
     *
     * @return submit Action
     */
    default Action submitSearch() {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_BUTTON);
        return (engine) -> engine.click(descriptor);
    }
}

