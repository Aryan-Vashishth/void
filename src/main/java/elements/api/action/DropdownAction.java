package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.DropdownTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link DropdownTarget}.
 *
 * <p>Provides {@link #open()}, {@link #selectByText(String)}, {@link #selectByValue(String)}.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   ClickTarget + ListTarget → DropdownTarget → DropdownAction
 * </pre>
 */
public interface DropdownAction extends DropdownTarget {

    default Action open() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }

    default Action selectByText(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.selectByVisibleText(descriptor, text);
        };
    }

    default Action selectByValue(String value) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.selectByValue(descriptor, value);
        };
    }
}

