package core.context;

import core.engine.UIEngine;

import java.util.Objects;
import java.util.Properties;

/**
 * Engine-agnostic, per-session execution context.
 *
 * <p>Holds the resolved configuration and the active {@link UIEngine} for a
 * single VOID session. Passed explicitly through the call chain — no hidden
 * static state required.</p>
 *
 * <h3>Why this replaces ExecutionContext</h3>
 * <ul>
 *   <li>{@link ExecutionContext} holds a raw {@code WebDriver} — Selenium-coupled.</li>
 *   <li>This class holds a {@link UIEngine}, enabling Playwright or any future engine.</li>
 *   <li>Enables parallel execution — each thread/test gets its own context.</li>
 *   <li>Makes dependencies visible at construction time.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   SessionContext ctx = new SessionContext(config, engine);
 *   ctx.engine().navigateTo("https://example.com");
 *   String val = ctx.getConfig("some.key");
 * </pre>
 *
 * @see UIEngine
 * @see ExecutionContext
 */
public final class SessionContext {

    private final Properties config;
    private final UIEngine engine;

    /**
     * Creates a new session context.
     *
     * @param config resolved configuration properties (must not be null)
     * @param engine active UIEngine instance (must not be null)
     * @throws NullPointerException if either argument is null
     */
    public SessionContext(Properties config, UIEngine engine) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /** Returns the active {@link UIEngine} for this session. */
    public UIEngine engine() {
        return engine;
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

    /**
     * Returns the engine name (e.g., "selenium", "playwright").
     *
     * @return engine identifier
     */
    public String getEngineName() {
        return engine.getEngineName();
    }

    @Override
    public String toString() {
        return "SessionContext{configKeys=" + config.size()
                + ", engine=" + engine.getEngineName() + '}';
    }
}

