package elements.api;

import core.actions.Action;
import core.engine.LocatorDescriptor;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link Checkbox}.
 *
 * <p>Provides a {@link #toggle()} method that eagerly resolves the locator descriptor
 * (using {@link ElementRole#TRIGGER}) and returns a deferred {@link Action} for
 * Flow-based execution.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(FilterPanel.ACTIVE_ONLY.toggle());
 * </pre>
 */
public interface CheckboxElement extends Checkbox {

    /**
     * Produces an Action that toggles (clicks) this checkbox.
     * Descriptor is resolved eagerly; execution is deferred.
     *
     * @return toggle Action
     */
    default Action toggle() {
        LocatorDescriptor descriptor =
                LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
        return (engine) -> engine.click(descriptor);
    }
}

