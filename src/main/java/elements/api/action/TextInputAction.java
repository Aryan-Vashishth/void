package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.TextInputTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link TextInputTarget}.
 *
 * <p>Provides {@link #type(String)} and {@link #clear()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget → TextInputAction
 * </pre>
 */
public interface TextInputAction extends TextInputTarget {

    default Action type(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.type(descriptor, text);
        };
    }

    default Action clear() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.clear(descriptor);
        };
    }
}

