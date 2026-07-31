package domain.automation.web.selenium.driver;

import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static core.logging.CustomLogger.debug;

/**
 * SeleniumDriverContext
 * ---------------------------------------------------------------------------
 * Thread-safe container for managing multiple WebDriver instances per thread.
 * Each thread has its own registry of drivers, ensuring safe parallel runs.
 *
 * Features:
 * <ul>
 *   <li>Register multiple named drivers per thread.</li>
 *   <li>Convenience methods for PRIMARY and SECONDARY drivers.</li>
 *   <li>Explicit "active" driver tracking with fallback to PRIMARY/SECONDARY.</li>
 *   <li>Lazy creation via suppliers.</li>
 *   <li>Robust cleanup (all drivers quit, errors aggregated).</li>
 *   <li>Introspection: size, keys, hasDriver, etc.</li>
 * </ul>
 *
 * Typical usage:
 * <pre>
 *   SeleniumDriverContext.setDriver(new ChromeDriver()); // PRIMARY
 *   SeleniumDriverContext.setSecondaryDriver(new FirefoxDriver());
 *
 *   WebDriver driver = SeleniumDriverContext.getDriver();              // PRIMARY
 *   WebDriver secondary = SeleniumDriverContext.getSecondaryDriver();  // SECONDARY
 *
 *   SeleniumDriverContext.setActive("secondary");
 *   WebDriver active = SeleniumDriverContext.getActiveDriver();        // returns secondary
 *
 *   SeleniumDriverContext.quitAllDrivers(); // cleanup
 * </pre>
 */
public final class SeleniumDriverContext {

    private static final String PRIMARY = "primary";
    private static final String SECONDARY = "secondary";

    /** Thread-local map of driver key -> WebDriver. */
    private static final ThreadLocal<Map<String, WebDriver>> driverThreadLocal =
            ThreadLocal.withInitial(HashMap::new);

    /** Thread-local active key. */
    private static final ThreadLocal<String> activeKey = new ThreadLocal<>();

    private SeleniumDriverContext() {}

    // ============================================================
    // Core map-based methods
    // ============================================================

    /**
     * Register a driver under a key. Fails if key already exists.
     *
     * @param key    unique identifier (e.g., "chromeA")
     * @param driver driver instance
     */
    public static void register(String key, WebDriver driver) {
        debug.log("Registering driver with key: " + key);
        Map<String, WebDriver> map = driverThreadLocal.get();
        if (map.containsKey(key)) {
            throw new IllegalStateException("Driver already registered for key: " + key);
        }
        map.put(key, driver);
    }

    /**
     * Set (or replace) a driver under a key.
     *
     * @param key    driver key
     * @param driver driver instance
     */
    public static void setPrimaryDriver(String key, WebDriver driver) {
        debug.log("Setting driver for key: " + key);
        driverThreadLocal.get().put(key, driver);
    }

    /**
     * Get driver by key.
     *
     * @param key driver key
     * @return WebDriver instance
     * @throws IllegalStateException if no driver registered
     */
    public static WebDriver getDriver(String key) {
        debug.log("Getting driver for key: " + key);
        WebDriver driver = driverThreadLocal.get().get(key);
        if (driver == null) {
            throw new IllegalStateException("No driver for key: " + key);
        }
        return driver;
    }

    /**
     * True if a driver exists for the key.
     */
    public static boolean hasDriver(String key) {
        return driverThreadLocal.get().containsKey(key);
    }

    /**
     * Detach a driver from registry without quitting.
     *
     * @param key driver key
     * @return removed WebDriver or null
     */
    public static WebDriver detach(String key) {
        debug.log("Detaching driver for key: " + key);
        return driverThreadLocal.get().remove(key);
    }

    /**
     * Quit and remove driver by key.
     *
     * @param key driver key
     */
    public static void quitDriver(String key) {
        debug.log("Quitting driver for key: " + key);
        WebDriver driver = driverThreadLocal.get().remove(key);
        if (driver != null) {
            try {
                driver.quit();
            } catch (RuntimeException ex) {
                debug.log("Error quitting driver for key: " + key + " - " + ex.getMessage());
            }
        }
        if (key.equals(activeKey.get())) {
            activeKey.remove();
        }
    }

    /**
     * Quit and remove all drivers in this thread.
     */
    public static void quitAllDrivers() {
        debug.log("Quitting all drivers for thread: " + Thread.currentThread().getName());
        Map<String, WebDriver> drivers = driverThreadLocal.get();
        RuntimeException first = null;

        for (Map.Entry<String, WebDriver> e : new HashMap<>(drivers).entrySet()) {
            try {
                if (e.getValue() != null) e.getValue().quit();
            } catch (RuntimeException ex) {
                debug.log("Error quitting driver for key: " + e.getKey() + " - " + ex.getMessage());
                if (first == null) first = ex;
            }
        }

        drivers.clear();
        driverThreadLocal.remove();
        activeKey.remove();

        if (first != null) throw first;
    }

    // ============================================================
    // Primary / Secondary
    // ============================================================

    public static void setPrimaryDriver(WebDriver driver) { setPrimaryDriver(PRIMARY, driver); }

    public static WebDriver getDriver() { return getDriver(PRIMARY); }

    public static void setSecondaryDriver(WebDriver driver) { setPrimaryDriver(SECONDARY, driver); }

    public static WebDriver getSecondaryDriver() { return getDriver(SECONDARY); }

    public static boolean hasPrimary() { return hasDriver(PRIMARY); }

    public static boolean hasSecondary() { return hasDriver(SECONDARY); }

    public static WebDriver removePrimary() { return detach(PRIMARY); }

    public static WebDriver removeSecondary() { return detach(SECONDARY); }

    public static void quitPrimaryDriver() { quitDriver(PRIMARY); }

    public static void quitSecondaryDriver() { quitDriver(SECONDARY); }

    // ============================================================
    // Active driver
    // ============================================================

    /**
     * Mark a registered key as active.
     *
     * @param key driver key
     */
    public static void setActive(String key) {
        debug.log("Setting active driver key: " + key);
        if (!hasDriver(key)) {
            throw new IllegalStateException("No driver registered for key: " + key);
        }
        activeKey.set(key);
    }

    /**
     * Resolve the current active key.
     *
     * @return active key
     * @throws IllegalStateException if no drivers present
     */
    public static String getActiveKey() {
        String k = activeKey.get();
        if (k != null && hasDriver(k)) return k;
        if (hasPrimary()) return PRIMARY;
        if (hasSecondary()) return SECONDARY;
        throw new IllegalStateException("No active driver found for thread: " + Thread.currentThread().getName());
    }

    /**
     * Get the active WebDriver.
     *
     * @return active driver
     */
    public static WebDriver getActiveDriver() {
        String key = getActiveKey();
        debug.log("Resolving active driver with key: " + key);
        return getDriver(key);
    }

    // ============================================================
    // Lazy creation & helpers
    // ============================================================

    /**
     * Get or create a driver with supplier if missing.
     *
     * @param key      driver key
     * @param supplier supplier to create WebDriver
     */
    public static WebDriver getOrCreate(String key, Supplier<WebDriver> supplier) {
        debug.log("GetOrCreate driver for key: " + key);
        Map<String, WebDriver> map = driverThreadLocal.get();
        return map.computeIfAbsent(key, k -> supplier.get());
    }

    /**
     * Run an action with driver by key.
     *
     * @param key    driver key
     * @param action action to perform
     */
    public static void withDriver(String key, Consumer<WebDriver> action) {
        debug.log("Running action with driver for key: " + key);
        action.accept(getDriver(key));
    }

    /**
     * Run an action with the active driver.
     */
    public static void withActiveDriver(Consumer<WebDriver> action) {
        debug.log("Running action with active driver.");
        action.accept(getActiveDriver());
    }

    // ============================================================
    // Diagnostics
    // ============================================================

    /** Number of registered drivers in this thread. */
    public static int size() { return driverThreadLocal.get().size(); }

    /** Keys of registered drivers. */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(driverThreadLocal.get().keySet());
    }
}