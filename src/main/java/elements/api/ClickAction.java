package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link ClickTarget}.
 *
 * <p>Provides a {@link #click()} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → ClickAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(LoginPage.LOGIN_BUTTON.click());
 * </pre>
 */
public interface ClickAction extends ClickTarget {

    /**
     * Produces an Action that clicks this element.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return click Action
     */
    default Action click() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TRIGGER);
            engine.click(descriptor);
        };
    }
}

