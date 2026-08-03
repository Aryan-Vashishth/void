package domain.automation.web.engine;

import core.engine.EngineBootstrap;
import core.engine.EngineConfig;
import core.engine.Executor;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static core.logging.CustomLogger.*;

/**
 * Factory for creating {@link UIEngine} instances based on configuration.
 *
 * <p>Reads the {@code engine} property from config and looks up the registered engine
 * by name. Engines register themselves via the {@link EngineRegistrar} SPI: add an
 * implementation to {@code META-INF/services/core.engine.EngineRegistrar} on the
 * classpath. The factory discovers all registrars at class-load time via
 * {@link ServiceLoader}.</p>
 *
 * <h3>Adding an engine</h3>
 * <p>No edits to this class are required. Implement {@link EngineRegistrar} in the
 * engine's own package and add its fully-qualified name to the services file.</p>
 *
 * <h3>Supported values (built-in)</h3>
 * <ul>
 *   <li>{@code selenium} (default) -- Selenium WebDriver-based engine</li>
 * </ul>
 */
public final class UIEngineFactory {

    /** Config property key for engine selection. */
    public static final String PROP_ENGINE = "engine";

    /** Default engine when not specified. */
    public static final String DEFAULT_ENGINE = "selenium";

    private static final Map<String, EngineRegistrar> REGISTRY = new ConcurrentHashMap<>();

    static {
        ServiceLoader.load(EngineRegistrar.class)
                .forEach(r -> REGISTRY.put(r.name(), r));
    }

    private UIEngineFactory() {}

    /**
     * Registers an engine implementation programmatically.
     *
     * <p>Prefer the {@link EngineRegistrar} SPI over this method. Use this only when
     * the classpath-based service descriptor is unavailable (e.g., dynamic module
     * loading in tests).</p>
     *
     * @param registrar the engine registrar to add
     */
    public static void register(EngineRegistrar registrar) {
        REGISTRY.put(registrar.name(), registrar);
    }

    /**
     * Creates and initializes an Executor based on the given config.
     *
     * @param config    combined configuration properties
     * @param bootstrap engine initialization token (see {@link EngineBootstrap})
     * @return initialized Executor
     * @throws IllegalStateException if the configured engine is not registered
     */
    public static Executor create(Properties config, EngineBootstrap bootstrap) {
        String engineName = resolveEngineName(config);
        info.log("[UIEngineFactory] Creating engine: " + engineName);

        EngineRegistrar registrar = REGISTRY.get(engineName);
        if (registrar == null) {
            throw new IllegalStateException(
                    "Unknown engine: '" + engineName + "'. Registered: " + REGISTRY.keySet());
        }

        Executor executor = registrar.create(bootstrap);

        EngineConfig engineConfig = new EngineConfig(config);
        executor.initialize(engineConfig);

        info.log("[UIEngineFactory] Engine '" + engineName + "' initialized. Timeout="
                + engineConfig.getDefaultTimeout().toSeconds() + "s");
        return executor;
    }

    /**
     * Resolves the engine name from properties, System properties, or ENV.
     * Priority: System property > ENV > config file > default.
     */
    public static String resolveEngineName(Properties config) {
        String value = System.getProperty(PROP_ENGINE);
        if (value != null && !value.isBlank()) {
            debug.log("[UIEngineFactory] Engine from System property: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        value = System.getenv("ENGINE");
        if (value != null && !value.isBlank()) {
            debug.log("[UIEngineFactory] Engine from ENV: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        if (config != null) {
            value = config.getProperty(PROP_ENGINE);
            if (value != null && !value.isBlank()) {
                debug.log("[UIEngineFactory] Engine from config: " + value);
                return value.trim().toLowerCase(Locale.ROOT);
            }
        }

        debug.log("[UIEngineFactory] No engine specified, defaulting to: " + DEFAULT_ENGINE);
        return DEFAULT_ENGINE;
    }
}
