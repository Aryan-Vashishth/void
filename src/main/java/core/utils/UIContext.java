package core.utils;

import core.engine.LocatorDescriptor;
import org.openqa.selenium.WebElement;

/**
 * Thread-local context for tracking the most recent interaction state.
 *
 * <p>Used by hooks ({@link core.interactions.hooks.Before}, {@link core.interactions.hooks.After})
 * and the Interactions orchestrator to pass context between layers without method parameters.</p>
 *
 * <h3>Migration note</h3>
 * <p>{@link #setLastElement(WebElement)} and {@link #getLastElement()} are deprecated.
 * Prefer {@link #setLastActionTarget(LocatorDescriptor)} and {@link #getLastActionTarget()}
 * for engine-agnostic context tracking.</p>
 */
public class UIContext {

    private static final ThreadLocal<Boolean> clickIsNavigating = ThreadLocal.withInitial(() -> false);

    public static void setClickIsNavigating(boolean isNavigating) {
        clickIsNavigating.set(isNavigating);
    }

    public static boolean isClickNavigating() {
        return clickIsNavigating.get();
    }


    public record LastElementMeta(String propertyFile, String key, Object[] args) {
    }

    private static final ThreadLocal<WebElement> lastElement = new ThreadLocal<>();
    private static final ThreadLocal<LastElementMeta> lastElementMeta = new ThreadLocal<>();
    private static final ThreadLocal<LocatorDescriptor> lastLocatorDescriptor = new ThreadLocal<>();
    private static final ThreadLocal<LocatorDescriptor> lastActionTarget = new ThreadLocal<>();

    /**
     * @deprecated Use {@link #setLastActionTarget(LocatorDescriptor)} instead.
     *             WebElement is engine-specific and breaks portability.
     */
    @Deprecated(forRemoval = true)
    public static void setLastElement(WebElement element) {
        lastElement.set(element);
    }

    /**
     * @deprecated Use {@link #getLastActionTarget()} instead.
     *             WebElement is engine-specific and breaks portability.
     */
    @Deprecated(forRemoval = true)
    public static WebElement getLastElement() {
        return lastElement.get();
    }

    /**
     * Stores the last resolved {@link LocatorDescriptor} for use by engine-agnostic hooks.
     *
     * @param descriptor the resolved locator descriptor
     * @deprecated Hooks now receive the descriptor via {@link core.interactions.hooks.ActionHandler#execute}.
     *             Use {@link core.actions.HookedAction} to pass descriptors explicitly.
     *             This method will be removed once all legacy call paths are migrated.
     */
    @Deprecated(forRemoval = true)
    public static void setLastLocatorDescriptor(LocatorDescriptor descriptor) {
        lastLocatorDescriptor.set(descriptor);
    }

    /**
     * Returns the last resolved {@link LocatorDescriptor}, or null if not set.
     *
     * @deprecated Hooks now receive the descriptor directly via
     *             {@link core.interactions.hooks.ActionHandler#execute(core.engine.UIEngine, LocatorDescriptor)}.
     *             Do not add new usages. This method will be removed once all legacy call paths are migrated.
     */
    @Deprecated(forRemoval = true)
    public static LocatorDescriptor getLastLocatorDescriptor() {
        return lastLocatorDescriptor.get();
    }

    /**
     * Stores the descriptor of the element that is the target of the current action.
     * This is the primary engine-agnostic replacement for {@link #setLastElement(WebElement)}.
     *
     * @param descriptor the action target descriptor
     */
    public static void setLastActionTarget(LocatorDescriptor descriptor) {
        lastActionTarget.set(descriptor);
    }

    /**
     * Returns the descriptor of the element targeted by the most recent action, or null.
     * This is the primary engine-agnostic replacement for {@link #getLastElement()}.
     */
    public static LocatorDescriptor getLastActionTarget() {
        return lastActionTarget.get();
    }

    public static void setLastElementMeta(String propertyFile, String key, Object[] args) {
        lastElementMeta.set(new LastElementMeta(propertyFile, key, args));
    }

    public static LastElementMeta getLastElementMeta() {
        return lastElementMeta.get();
    }

    public static void clear() {
        clickIsNavigating.remove();
        lastElement.remove();
        lastElementMeta.remove();
        lastLocatorDescriptor.remove();
        lastActionTarget.remove();
    }
}
