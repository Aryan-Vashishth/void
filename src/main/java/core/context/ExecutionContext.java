package core.context;

import org.openqa.selenium.WebDriver;

import java.util.Objects;
import java.util.Properties;

/**
 * Immutable, per-session execution context.
 *
 * <p>Holds the resolved configuration and the active {@link WebDriver} for a
 * single VOID session. Passed explicitly through the call chain — no hidden
 * static state required.</p>
 *
 * <h3>Why</h3>
 * <ul>
 *   <li>Eliminates global mutable singletons ({@code ConfigLoader.ACTIVE},
 *       {@code DriverContext.setPrimaryDriver(...)}).</li>
 *   <li>Enables parallel execution — each thread / test gets its own context.</li>
 *   <li>Makes dependencies visible at construction time.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   ExecutionContext ctx = new ExecutionContext(config, driver);
 *   ctx.getDriver().get("https://example.com");
 *   String val = ctx.getConfig("some.key");
 * </pre>
 */
public final class ExecutionContext {

    private final Properties config;
    private final WebDriver driver;

    /**
     * Creates a new execution context.
     *
     * @param config resolved configuration properties (must not be null)
     * @param driver active WebDriver instance (must not be null)
     * @throws NullPointerException if either argument is null
     */
    public ExecutionContext(Properties config, WebDriver driver) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.driver = Objects.requireNonNull(driver, "driver must not be null");
    }

    /** Returns the active {@link WebDriver} for this session. */
    public WebDriver getDriver() {
        return driver;
    }

    /** Returns the full configuration snapshot for this session. */
    public Properties getConfigProperties() {
        return config;
    }

    /**
     * Reads a single configuration value.
     *
     * @param key property key
     * @return value or {@code null} if absent
     */
    public String getConfig(String key) {
        return config.getProperty(key);
    }

    /**
     * Reads a single configuration value with a default fallback.
     *
     * @param key          property key
     * @param defaultValue fallback if key is absent
     * @return resolved value
     */
    public String getConfig(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    @Override
    public String toString() {
        return "ExecutionContext{configKeys=" + config.size()
                + ", driver=" + driver.getClass().getSimpleName() + '}';
    }
}

