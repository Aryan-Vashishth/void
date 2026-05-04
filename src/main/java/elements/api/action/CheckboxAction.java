package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.CheckboxTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link CheckboxTarget}.
 *
 * <p>Provides {@link #toggle()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → CheckboxTarget → CheckboxAction
 * </pre>
 */
public interface CheckboxAction extends CheckboxTarget {

    default Action toggle() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }
}

