package core.context;

import core.engine.Executor;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/**
 * Engine-agnostic, per-session execution context.
 *
 * <p>Holds the resolved configuration, the active {@link Executor}, and a stable
 * session identity for a single VOID session. Passed explicitly through the call
 * chain -- no hidden static state required.</p>
 *
 * <h3>Why this replaces ExecutionContext</h3>
 * <ul>
 *   <li>{@link ExecutionContext} holds a raw {@code WebDriver} -- Selenium-coupled.</li>
 *   <li>This class holds an {@link Executor}, enabling Playwright or any future engine.</li>
 *   <li>Enables parallel execution -- each thread/test gets its own context.</li>
 *   <li>Makes dependencies visible at construction time.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   SessionContext ctx = new SessionContext(config, executor);
 *   String val = ctx.getConfig("some.key");
 * </pre>
 *
 * @see Executor
 * @see ExecutionContext
 */
public final class SessionContext {

    private final String sessionId;
    private final Properties config;
    private final Executor executor;

    /**
     * Creates a new session context with a generated session ID.
     *
     * @param config   resolved configuration properties (must not be null)
     * @param executor active Executor instance (must not be null)
     * @throws NullPointerException if either argument is null
     */
    public SessionContext(Properties config, Executor executor) {
        this.sessionId = UUID.randomUUID().toString();
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    /** Returns the unique ID for this session. */
    public String sessionId() {
        return sessionId;
    }

    /** Returns the active {@link Executor} for this session. */
    public Executor engine() {
        return executor;
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
        return executor.getEngineName();
    }

    @Override
    public String toString() {
        return "SessionContext{sessionId=" + sessionId
                + ", engine=" + executor.getEngineName()
                + ", configKeys=" + config.size() + '}';
    }
}

