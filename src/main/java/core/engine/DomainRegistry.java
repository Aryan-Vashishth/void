package core.engine;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static core.logging.CustomLogger.*;

/**
 * Registry for domain implementations.
 *
 * <p>Resolves the active domain by name and delegates executor creation to the
 * corresponding {@link DomainRegistrar}. Domains register themselves via the
 * {@link DomainRegistrar} SPI: add an implementation to
 * {@code META-INF/services/core.engine.DomainRegistrar} on the classpath.
 * The registry discovers all registrars at class-load time via
 * {@link ServiceLoader}.</p>
 *
 * <h3>Adding a domain</h3>
 * <p>No edits to this class are required. Implement {@link DomainRegistrar} in
 * the domain's own package and add its fully-qualified name to the services
 * file.</p>
 *
 * <h3>Domain name resolution</h3>
 * <p>Priority: System property {@code domain} &gt; ENV {@code DOMAIN} &gt;
 * config property {@code domain} &gt; {@link #DEFAULT_DOMAIN}.</p>
 *
 * <h3>Built-in domains</h3>
 * <ul>
 *   <li>{@code web} (default) -- Selenium-backed UI automation</li>
 * </ul>
 */
public final class DomainRegistry {

    /** Config and System property key for domain selection. */
    public static final String PROP_DOMAIN = "domain";

    /** Default domain when not specified. */
    public static final String DEFAULT_DOMAIN = "web";

    private static final Map<String, DomainRegistrar> REGISTRY = new ConcurrentHashMap<>();

    static {
        ServiceLoader.load(DomainRegistrar.class)
                .forEach(r -> REGISTRY.put(r.name(), r));
    }

    private DomainRegistry() {}

    /**
     * Registers a domain implementation programmatically.
     *
     * <p>Prefer the {@link DomainRegistrar} SPI over this method. Use this only
     * when the classpath-based service descriptor is unavailable (e.g., dynamic
     * module loading in tests).</p>
     *
     * @param registrar the domain registrar to add
     */
    public static void register(DomainRegistrar registrar) {
        REGISTRY.put(registrar.name(), registrar);
    }

    /**
     * Creates a fully initialized executor for the given domain.
     *
     * @param domainName resolved domain identifier
     * @param config     combined configuration properties
     * @param bootstrap  engine initialization token
     * @return fully initialized executor
     * @throws IllegalStateException if the domain is not registered
     */
    public static Executor create(String domainName, Properties config, EngineBootstrap bootstrap) {
        info.log("[DomainRegistry] Creating executor for domain: " + domainName);

        DomainRegistrar registrar = REGISTRY.get(domainName);
        if (registrar == null) {
            throw new IllegalStateException(
                    "Unknown domain: '" + domainName + "'. Registered: " + REGISTRY.keySet());
        }

        Executor executor = registrar.createExecutor(config, bootstrap);
        info.log("[DomainRegistry] Domain '" + domainName + "' executor ready.");
        return executor;
    }

    /**
     * Resolves the domain name from System properties, ENV, config, or the default.
     *
     * @param config combined configuration properties (may be {@code null})
     * @return resolved domain name, never {@code null}
     */
    public static String resolveDomainName(Properties config) {
        String value = System.getProperty(PROP_DOMAIN);
        if (value != null && !value.isBlank()) {
            debug.log("[DomainRegistry] Domain from System property: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        value = System.getenv("DOMAIN");
        if (value != null && !value.isBlank()) {
            debug.log("[DomainRegistry] Domain from ENV: " + value);
            return value.trim().toLowerCase(Locale.ROOT);
        }

        if (config != null) {
            value = config.getProperty(PROP_DOMAIN);
            if (value != null && !value.isBlank()) {
                debug.log("[DomainRegistry] Domain from config: " + value);
                return value.trim().toLowerCase(Locale.ROOT);
            }
        }

        debug.log("[DomainRegistry] No domain specified, defaulting to: " + DEFAULT_DOMAIN);
        return DEFAULT_DOMAIN;
    }
}
