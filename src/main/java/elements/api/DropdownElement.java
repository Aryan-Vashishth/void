package elements.api;

import core.actions.Action;
import core.engine.LocatorDescriptor;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link Dropdown}.
 *
 * <p>Provides {@link #open()}, {@link #selectByText(String)}, and
 * {@link #selectByValue(String)} methods that eagerly resolve the locator descriptor
 * and return deferred {@link Action}s for Flow-based execution.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       FilterPanel.STATUS.open(),
 *       FilterPanel.STATUS.selectByText("Active")
 *   );
 * </pre>
 */
public interface DropdownElement extends Dropdown {

    /**
     * Produces an Action that clicks the dropdown trigger to open it.
     * Descriptor is resolved eagerly; execution is deferred.
     *
     * @return open Action
     */
    default Action open() {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
        return (engine) -> engine.click(descriptor);
    }

    /**
     * Produces an Action that selects an option by visible text.
     * Descriptor is resolved against the LIST role.
     *
     * @param text visible option text
     * @return select Action
     */
    default Action selectByText(String text) {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
        return (engine) -> engine.selectByVisibleText(descriptor, text);
    }

    /**
     * Produces an Action that selects an option by value attribute.
     * Descriptor is resolved against the LIST role.
     *
     * @param value option value
     * @return select Action
     */
    default Action selectByValue(String value) {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
        return (engine) -> engine.selectByValue(descriptor, value);
    }
}

