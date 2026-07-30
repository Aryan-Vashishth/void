/**
 * Engine abstraction layer for the VOID framework.
 *
 * <p>This package is intentionally split across two ownership categories
 * (ADR-021 addendum, I6.2 ownership audit). Types are kept in the same package
 * until I6.4 physically relocates the web-domain types.</p>
 *
 * <h3>Kernel-neutral types (remain in {@code core.engine} permanently)</h3>
 * <ul>
 *   <li>{@link core.engine.Executor} -- neutral execution-owner contract (ADR-021 AD2)</li>
 *   <li>{@link core.engine.EngineBootstrap} -- opaque session initialization token</li>
 *   <li>{@link core.engine.EngineConfig} -- neutral config carrier (timeout, pollingMs, baseUrl)</li>
 *   <li>{@link core.engine.DomainRegistrar} -- domain-registration SPI (I6.1)</li>
 *   <li>{@link core.engine.DomainRegistry} -- domain-registration factory (I6.1)</li>
 * </ul>
 *
 * <h3>Web-domain types (relocate to {@code domain.automation.web.engine} in I6.4)</h3>
 * <ul>
 *   <li>{@link core.engine.UIEngine} -- web execution contract (extends Executor;
 *       imports web-domain vocabulary; relocates to
 *       {@code domain.automation.web.engine.UIEngine})</li>
 *   <li>{@link core.engine.UIEngineFactory} -- web engine factory (relocates to
 *       {@code domain.automation.web.engine.UIEngineFactory})</li>
 *   <li>{@link core.engine.EngineRegistrar} -- web engine SPI (relocates to
 *       {@code domain.automation.web.engine.EngineRegistrar})</li>
 * </ul>
 *
 * <p>Locator types ({@code LocatorDescriptor}, {@code LocatorStrategy}) moved to
 * {@code elements.locator} in runtime-redesign I7.2; they relocate further to
 * {@code domain.automation.web.locator} in I6.4.</p>
 */
package core.engine;

