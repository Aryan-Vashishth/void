package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link SearchableDropdownTarget}.
 *
 * <p>Combines dropdown open/select with search type/submit actions. All resolution
 * is deferred inside the Action lambda.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   DropdownTarget ─┐
 *   SearchableTarget┤→ SearchableDropdownTarget → SearchableDropdownAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       FilterPanel.STATUS.open(),
 *       FilterPanel.STATUS.typeSearch("Active"),
 *       FilterPanel.STATUS.submitSearch()
 *   );
 * </pre>
 */
public interface SearchableDropdownAction extends SearchableDropdownTarget {

    default Action open() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }

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

    default Action selectByText(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.SEARCH_RESULT);
            engine.selectByVisibleText(descriptor, text);
        };
    }
}

