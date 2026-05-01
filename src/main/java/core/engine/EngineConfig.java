package core.engine;

import java.time.Duration;
import java.util.Properties;

/**
 * Configuration holder for {@link UIEngine} initialization.
 *
 * <p>Provides engine-agnostic configuration values that any engine needs
 * at startup (timeouts, base URL, headless mode, etc.). Engine-specific
 * settings are passed through the underlying {@link Properties}.</p>
 */
public final class EngineConfig {

    private final Properties properties;

    /** Default wait timeout for element interactions. */
    private final Duration defaultTimeout;

    /** Default polling interval for explicit waits. */
    private final Duration pollingInterval;

    /** Base URL for navigation. */
    private final String baseUrl;

    public EngineConfig(Properties properties) {
        this.properties = properties != null ? properties : new Properties();
        this.defaultTimeout = Duration.ofSeconds(
                parseLong(this.properties.getProperty("engine.timeout", "10")));
        this.pollingInterval = Duration.ofMillis(
                parseLong(this.properties.getProperty("engine.pollingMs", "200")));
        this.baseUrl = this.properties.getProperty("engine.baseUrl", "");
    }

    public Duration getDefaultTimeout() { return defaultTimeout; }
    public Duration getPollingInterval() { return pollingInterval; }
    public String getBaseUrl() { return baseUrl; }
    public Properties getProperties() { return properties; }

    public String getProperty(String key) { return properties.getProperty(key); }
    public String getProperty(String key, String defaultValue) { return properties.getProperty(key, defaultValue); }

    private static long parseLong(String value) {
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException e) { return 10L; }
    }
}

