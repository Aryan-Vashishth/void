package core.engine;

import core.engine.selenium.SeleniumEngine;
import org.openqa.selenium.WebDriver;

import java.util.Locale;
import java.util.Properties;

import static core.logging.CustomLogger.*;

/**
 * Factory for creating {@link UIEngine} instances based on configuration.
 *
 * <p>Reads the {@code engine} property from config and instantiates the
 * corresponding engine implementation. Defaults to Selenium if not specified.</p>
 *
 * <h3>Supported values</h3>
 * <ul>
 *   <li>{@code selenium} (default) — Selenium WebDriver-based engine</li>
 *   <li>{@code playwright} — Playwright-based engine (future, Phase 3)</li>
 * </ul>
 */
public final class UIEngineFactory {

    /** Config property key for engine selection. */
    public static final String PROP_ENGINE = "engine";

    /** Default engine when not specified. */
    public static final String DEFAULT_ENGINE = "selenium";

    private UIEngineFactory() {}

    /**
     * Creates and initializes a UIEngine based on the given config.
     *
     * @param config  combined configuration properties
     * @param driver  WebDriver instance (used by SeleniumEngine; ignored by other engines)
     * @return initialized UIEngine
     * @throws IllegalStateException if the configured engine is not supported
     */
    public static UIEngine create(Properties config, WebDriver driver) {
        String engineName = resolveEngineName(config);
        info.log("[UIEngineFactory] Creating engine: " + engineName);

        UIEngine engine = switch (engineName) {
            case "selenium" -> new SeleniumEngine(driver);
            // case "playwright" -> new PlaywrightEngine(); // Phase 3
            default -> throw new IllegalStateException(
                    "Unsupported engine: '" + engineName + "'. Supported values: selenium");
        };

        EngineConfig engineConfig = new EngineConfig(config);
        engine.initialize(engineConfig);

        info.log("[UIEngineFactory] Engine '" + engineName + "' initialized. Timeout="
                + engineConfig.getDefaultTimeout().toSeconds() + "s");
        return engine;
    }

    /**
     * Resolves the engine name from properties, System properties, or ENV.
     * Priority: System property > ENV > config file > default.
     */
    public static String resolveEngineName(Properties config) {
        // 1. System property
        String value = System.getProperty(PROP_ENGINE);
        if (value != null && !value.isBlank()) {
            debug.log("[UIEngineFactory] Engine from System property: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        // 2. Environment variable
        value = System.getenv("ENGINE");
        if (value != null && !value.isBlank()) {
            debug.log("[UIEngineFactory] Engine from ENV: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        // 3. Config properties
        if (config != null) {
            value = config.getProperty(PROP_ENGINE);
            if (value != null && !value.isBlank()) {
                debug.log("[UIEngineFactory] Engine from config: " + value);
                return value.trim().toLowerCase(Locale.ROOT);
            }
        }

        // 4. Default
        debug.log("[UIEngineFactory] No engine specified, defaulting to: " + DEFAULT_ENGINE);
        return DEFAULT_ENGINE;
    }
}

