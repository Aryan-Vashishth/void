package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link DropdownTarget}.
 *
 * <p>Provides {@link #open()}, {@link #selectByText(String)}, and
 * {@link #selectByValue(String)} methods that return deferred {@link Action}s.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget ─┐
 *   Element → ListTarget  ─┤→ DropdownTarget → DropdownAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       FilterPanel.STATUS.open(),
 *       FilterPanel.STATUS.selectByText("Active")
 *   );
 * </pre>
 */
public interface DropdownAction extends DropdownTarget {

    /**
     * Produces an Action that clicks the dropdown trigger to open it.
     * Resolution is deferred.
     *
     * @return open Action
     */
    default Action open() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }

    /**
     * Produces an Action that selects an option by visible text.
     * Resolution is deferred against the LIST role.
     *
     * @param text visible option text
     * @return select Action
     */
    default Action selectByText(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.selectByVisibleText(descriptor, text);
        };
    }

    /**
     * Produces an Action that selects an option by value attribute.
     * Resolution is deferred against the LIST role.
     *
     * @param value option value
     * @return select Action
     */
    default Action selectByValue(String value) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.LIST);
            engine.selectByValue(descriptor, value);
        };
    }
}

