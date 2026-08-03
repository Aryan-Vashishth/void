/**
 * {@code core.engine} -- Kernel-neutral execution contract.
 *
 * <p>Contains only the kernel-neutral types that the execution pipeline depends on.
 * Web-domain types ({@code UIEngine}, {@code UIEngineFactory}, {@code EngineRegistrar})
 * were relocated to {@code domain.automation.web.engine} in I6.4 (ADR-021 addendum).
 * Locator types were relocated to {@code domain.automation.web.locator} in I7.2 + I6.4.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.engine.Executor} -- neutral execution-owner contract (ADR-021 AD2);
 *       extended by {@code domain.automation.web.engine.UIEngine}.</li>
 *   <li>{@link core.engine.EngineBootstrap} -- opaque session initialisation token.</li>
 *   <li>{@link core.engine.EngineConfig} -- neutral config carrier (timeout, pollingMs,
 *       baseUrl).</li>
 *   <li>{@link core.engine.DomainRegistrar} -- domain-registration SPI (I6.1).</li>
 *   <li>{@link core.engine.DomainRegistry} -- domain-registration factory (I6.1).</li>
 * </ul>
 *
 * @see domain.automation.web.engine.UIEngine
 * @see domain.automation.web.engine.UIEngineFactory
 * @see domain.automation.web.engine.EngineRegistrar
 */
package core.engine;

