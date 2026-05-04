package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link TextInputTarget}.
 *
 * <p>Provides {@link #type(String)} and {@link #clear()} methods that return deferred
 * {@link Action}s. Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TextInputTarget → TextInputAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(
 *       LoginPage.USERNAME.type("admin"),
 *       LoginPage.PASSWORD.type("secret")
 *   );
 * </pre>
 */
public interface TextInputAction extends TextInputTarget {

    /**
     * Produces an Action that clears and types text into this input.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @param text the text to type
     * @return type Action
     */
    default Action type(String text) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.type(descriptor, text);
        };
    }

    /**
     * Produces an Action that clears this input field.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @return clear Action
     */
    default Action clear() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.clear(descriptor);
        };
    }
}

