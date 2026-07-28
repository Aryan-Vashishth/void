package core.engine;

/**
 * SPI for engine implementations to register themselves with {@link UIEngineFactory}.
 *
 * <p>Discovered at runtime via {@link java.util.ServiceLoader}. Implementations must be
 * listed in {@code META-INF/services/core.engine.EngineRegistrar} on the classpath.</p>
 *
 * <p>To add a new engine without editing {@code core.engine}:</p>
 * <ol>
 *   <li>Implement this interface in the engine's own package.</li>
 *   <li>Add the fully-qualified class name to
 *       {@code META-INF/services/core.engine.EngineRegistrar}.</li>
 * </ol>
 */
public interface EngineRegistrar {

    /** Engine identifier -- must match the value returned by {@link UIEngine#getEngineName()}. */
    String name();

    /**
     * Creates an uninitialized engine from the given bootstrap token.
     *
     * <p>{@link UIEngineFactory} calls {@link UIEngine#initialize(EngineConfig)} after this
     * method returns. Implementations must not call {@code initialize} themselves.</p>
     *
     * @param bootstrap engine initialization token supplied by the session builder
     * @return a new, uninitialized engine instance
     * @throws IllegalArgumentException if the bootstrap token is incompatible with this engine
     */
    UIEngine create(EngineBootstrap bootstrap);
}
