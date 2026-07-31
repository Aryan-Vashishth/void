/**
 * {@code domain.automation.web.engine} -- Web execution contract.
 *
 * <p>Defines the execution-owner contract for the Web domain:
 * {@link domain.automation.web.engine.UIEngine} is the single interaction authority
 * for web elements, and the SPI and factory that govern its lifecycle.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.engine.UIEngine} -- web execution contract;
 *       extends {@code core.engine.Executor}; the sole caller of WebDriver methods
 *       (ADR-007).</li>
 *   <li>{@link domain.automation.web.engine.UIEngineFactory} -- creates {@code UIEngine}
 *       instances from {@code EngineConfig}; dispatches to registered
 *       {@code EngineRegistrar} implementations.</li>
 *   <li>{@link domain.automation.web.engine.EngineRegistrar} -- SPI for registering
 *       {@code UIEngine} implementations; loaded via {@code ServiceLoader}.</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Callers must not perform their own scrolling, waiting, or retry logic --
 *       the engine owns all execution concerns (ADR-007).</li>
 *   <li>A second web engine (e.g., Playwright) is a new sibling package with a new
 *       {@code EngineRegistrar} entry -- no changes to this package or any kernel
 *       type are required.</li>
 * </ul>
 *
 * @see domain.automation.web.selenium.SeleniumEngine
 * @see core.engine.Executor
 */
package domain.automation.web.engine;
