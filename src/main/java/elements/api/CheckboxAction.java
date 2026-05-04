package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link CheckboxTarget}.
 *
 * <p>Provides a {@link #toggle()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → CheckboxTarget → CheckboxAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(FilterPanel.ACTIVE_ONLY.toggle());
 * </pre>
 */
public interface CheckboxAction extends CheckboxTarget {

    /**
     * Produces an Action that toggles (clicks) this checkbox.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return toggle Action
     */
    default Action toggle() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }
}

