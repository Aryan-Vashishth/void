package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link MultiDropdownTarget}.
 *
 * <p>Provides {@link #open()} and {@link #selectByText(String)} methods.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → MultiDropdownTarget → MultiDropdownAction
 * </pre>
 */
public interface MultiDropdownAction extends MultiDropdownTarget {

    default Action open() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.MULTI_TRIGGER);
            engine.click(descriptor);
        };
    }

    default Action selectByText(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.MULTI_LIST);
            engine.selectByVisibleText(descriptor, text);
        };
    }
}

