package core.engine;

import java.util.Properties;

/**
 * SPI for domains to register themselves with the runtime at bootstrap.
 *
 * <p>Discovered at runtime via {@link java.util.ServiceLoader}. Implementations
 * must be listed in {@code META-INF/services/core.engine.DomainRegistrar} on
 * the classpath.</p>
 *
 * <p>A domain announces itself by implementing this interface and registering
 * its executor factory. The runtime consults only this surface to learn what
 * domains exist -- no runtime file edits are needed to add a new domain.</p>
 *
 * <p>To add a new domain without editing runtime code:</p>
 * <ol>
 *   <li>Implement this interface in the domain's own package.</li>
 *   <li>Add the fully-qualified class name to
 *       {@code META-INF/services/core.engine.DomainRegistrar}.</li>
 * </ol>
 *
 * <p>{@code createExecutor} must return a <em>fully initialized</em> executor.
 * Initialization is the domain's responsibility; the runtime does not call
 * {@link Executor#initialize} after this method returns.</p>
 */
public interface DomainRegistrar {

    /** Domain identifier -- unique across all registered domains. */
    String name();

    /**
     * Creates and initializes an executor for this domain.
     *
     * @param config    combined configuration properties (engine selection, timeouts, etc.)
     * @param bootstrap engine initialization token supplied by the session builder
     * @return a fully initialized executor ready for use in a session
     * @throws IllegalArgumentException if the config or bootstrap is incompatible with this domain
     * @throws IllegalStateException if executor creation or initialization fails
     */
    Executor createExecutor(Properties config, EngineBootstrap bootstrap);
}
