package core.utils;

/**
 * Centralized registry of all configuration file classpath locations.
 *
 * <p>Each subsystem owns its own config under {@code resources/},
 * aligned with its package structure. This class is the single source
 * of truth for those paths — no magic strings scattered in code.</p>
 *
 * <h3>Structure</h3>
 * <pre>
 *   src/main/resources/
 *   ├── core/
 *   │   ├── driver/config/
 *   │   │   ├── driver.properties        ← DRIVER_DEFAULT
 *   │   │   ├── driver-local.properties   ← DRIVER_LOCAL
 *   │   │   ├── driver-ci.properties      ← DRIVER_CI
 *   │   │   └── driver-grid.properties    ← DRIVER_GRID
 *   │   ├── utils/config/
 *   │   │   └── test.properties           ← UTILS_TEST
 *   │   └── logging/config/
 *   │       └── logging.properties        ← LOGGING  (future)
 *   └── locators/
 *       └── ...                           (locator bundles)
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>
 *   Properties p = ConfigLoader.loadFromClasspath(ConfigPaths.DRIVER_DEFAULT);
 * </pre>
 */
public final class ConfigPaths {

    private ConfigPaths() {}

    // ─── Driver ─────────────────────────────────────────────────────────────
    /** Default driver configuration (browser, timeouts, window, etc.) */
    public static final String DRIVER_DEFAULT = "core/driver/config/driver.properties";

    /** Local development overlay (headless=false, maximize, etc.) */
    public static final String DRIVER_LOCAL   = "core/driver/config/driver-local.properties";

    /** CI pipeline overlay (headless=true, etc.) */
    public static final String DRIVER_CI      = "core/driver/config/driver-ci.properties";

    /** Selenium Grid / remote overlay */
    public static final String DRIVER_GRID    = "core/driver/config/driver-grid.properties";

    // ─── Utils / Test Data ──────────────────────────────────────────────────
    /** Paths for JSON logging, test-data, uploads, fallback resources */
    public static final String UTILS_TEST     = "core/utils/config/test.properties";

    // ─── Logging (future) ───────────────────────────────────────────────────
    /** Custom logging configuration (reserved for future use) */
    public static final String LOGGING        = "core/logging/config/logging.properties";
}

