package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ToolTipTarget}.
 *
 * <p>Provides a {@link #hover()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnlyTarget → ToolTipTarget → ToolTipAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(DetailPanel.TOOLTIP_FIELD.hover());
 * </pre>
 */
public interface ToolTipAction extends ToolTipTarget {

    /**
     * Produces an Action that hovers over this element to trigger the tooltip.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return hover Action
     */
    default Action hover() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TEXT);
            engine.hover(descriptor);
        };
    }
}

