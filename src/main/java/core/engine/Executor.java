package core.engine;

/**
 * Kernel-neutral execution-owner contract (ADR-021 AD2).
 *
 * <p>An {@code Executor} is the concept that carries a capability set and knows how to
 * execute an Interaction against a set of Targets. It is the kernel's abstraction over
 * all domain executors -- web, REST, CLI, database, etc.</p>
 *
 * <h3>Minimality rule</h3>
 * <p>Only methods that every possible executor can implement meaningfully belong here.
 * A method that a non-web executor could only implement by throwing
 * {@code UnsupportedOperationException} is domain-specific and belongs on the
 * domain's refinement (e.g. {@link UIEngine}), not here.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Executor                      (kernel, neutral -- this interface)
 *       ^
 *       |
 *   UIEngine                      (web domain's refinement)
 *       ^
 *       |
 *   SeleniumEngine, PlaywrightEngine  (web domain implementations)
 * </pre>
 *
 * <h3>Kernel membership</h3>
 * <p>This type is in the kernel membership list (ADR-021). It may not import
 * {@code elements.*}, Selenium, or {@code core.driver} -- enforced by
 * {@code KernelBoundaryRulesTest.executorContractIsNeutral}.</p>
 *
 * @see UIEngine
 */
public interface Executor {

    /**
     * Initializes this executor with the given configuration.
     * Called once by {@link UIEngineFactory} before any interaction methods.
     *
     * @param config engine configuration
     */
    void initialize(EngineConfig config);

    /**
     * Shuts down this executor, releasing all resources (connections, drivers, sessions).
     */
    void shutdown();

    /**
     * Returns a short identifier for this executor (e.g., {@code "selenium"},
     * {@code "playwright"}).
     *
     * @return executor name; never null
     */
    String getEngineName();
}
