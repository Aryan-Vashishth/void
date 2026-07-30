package core.bootstrap;

import core.logging.CustomLogger;
import core.utils.ConfigLoader;
import core.utils.ConfigPaths;

import java.util.Properties;

/**
 * One-time framework initialisation.
 *
 * <p>Performs bootstrap tasks that must happen exactly once per JVM:</p>
 * <ol>
 *   <li>Loads the utils/test config from the classpath</li>
 * </ol>
 *
 * <p>This class is domain-neutral: it does not validate web/driver configuration.
 * Web-domain config (e.g. {@code driver.properties}) is validated by
 * {@code SeleniumEngine.initialize()} at web session creation time, which preserves
 * fail-fast semantics for the web path without gating non-web domains at framework
 * startup.</p>
 *
 * <p>This class is intentionally free of driver logic, test logic, or any
 * mutable global state beyond the {@code initialized} guard. All resolved
 * configuration is returned to the caller via {@link #getUtilsConfig()}.</p>
 *
 * <p>Safe to call {@link #init()} multiple times -- only the first invocation
 * performs work.</p>
 */
public final class FrameworkBootstrap {

    private static volatile boolean initialized = false;

    /** Utils config loaded during bootstrap (empty until {@link #init()} completes). */
    private static Properties utilsConfig = new Properties();

    private FrameworkBootstrap() {}

    /**
     * Bootstrap the framework. Idempotent -- only the first call performs work.
     */
    public static synchronized void init() {
        if (initialized) return;

        CustomLogger.debug.log("FrameworkBootstrap: starting...");

        // Load utils/test config
        Properties loaded = ConfigLoader.loadFromClasspath(ConfigPaths.UTILS_TEST);
        if (!loaded.isEmpty()) {
            utilsConfig = loaded;
            // Backward-compat: seed ConfigLoader.ACTIVE so legacy callers
            // (e.g. ConfigLoader.get("key")) still resolve correctly.
            ConfigLoader.setActive(loaded);
            CustomLogger.debug.log("FrameworkBootstrap: utils config loaded (" + loaded.size() + " keys)");
        }

        initialized = true;
        CustomLogger.info.log("FrameworkBootstrap: initialised.");
    }

    /**
     * Returns the utils/test configuration loaded during bootstrap.
     * Returns an empty {@link Properties} if {@link #init()} has not yet been called.
     */
    public static Properties getUtilsConfig() {
        return utilsConfig;
    }

    /**
     * Whether the framework has been bootstrapped.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Resets bootstrap state. <b>Test-only</b> — never call in production code.
     */
    static synchronized void reset() {
        initialized = false;
        utilsConfig = new Properties();
    }
}


