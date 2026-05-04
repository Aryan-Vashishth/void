package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.SearchableDropdownTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link SearchableDropdownTarget}.
 *
 * <p>Combines dropdown open/select with search type/submit actions.</p>
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

