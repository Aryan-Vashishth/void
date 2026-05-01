package core.utils;

import core.engine.LocatorDescriptor;
import org.openqa.selenium.WebElement;

public class UIContext {

    private static final ThreadLocal<Boolean> clickIsNavigating = ThreadLocal.withInitial(() -> false);

    public static void setClickIsNavigating(boolean isNavigating) {
        clickIsNavigating.set(isNavigating);
    }

    public static boolean isClickNavigating() {
        return clickIsNavigating.get();
    }


    public static class LastElementMeta {
        private final String propertyFile;
        private final String key;
        private final Object[] args;

        public LastElementMeta(String propertyFile, String key, Object[] args) {
            this.propertyFile = propertyFile;
            this.key = key;
            this.args = args;
        }

        public String getPropertyFile() {
            return propertyFile;
        }

        public String getKey() {
            return key;
        }

        public Object[] getArgs() {
            return args;
        }
    }

    private static final ThreadLocal<WebElement> lastElement = new ThreadLocal<>();
    private static final ThreadLocal<LastElementMeta> lastElementMeta = new ThreadLocal<>();
    private static final ThreadLocal<LocatorDescriptor> lastLocatorDescriptor = new ThreadLocal<>();

    public static void setLastElement(WebElement element) {
        lastElement.set(element);
    }

    public static WebElement getLastElement() {
        return lastElement.get();
    }

    /**
     * Stores the last resolved {@link LocatorDescriptor} for use by engine-agnostic hooks.
     *
     * @param descriptor the resolved locator descriptor
     */
    public static void setLastLocatorDescriptor(LocatorDescriptor descriptor) {
        lastLocatorDescriptor.set(descriptor);
    }

    /**
     * Returns the last resolved {@link LocatorDescriptor}, or null if not set.
     */
    public static LocatorDescriptor getLastLocatorDescriptor() {
        return lastLocatorDescriptor.get();
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
    }
}
