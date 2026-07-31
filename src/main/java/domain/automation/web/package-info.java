/**
 * {@code domain.automation.web} -- Web automation domain root.
 *
 * <p>Entry point for the Web automation domain. Contains the domain registrar that
 * wires the Web domain's vocabulary and execution owners into the VOID runtime at
 * bootstrap (ADR-021, I6.1).</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.WebDomainRegistrar} -- registers the Web domain
 *       (UIElement vocabulary, UIEngine execution owner) via the kernel's
 *       {@code core.engine.DomainRegistrar} SPI.</li>
 * </ul>
 *
 * <h3>Domain ownership (ADR-021)</h3>
 * <p>Everything under {@code domain.automation.web.*} is Web-domain-owned. The two
 * sub-trees reflect the two ownership layers from ADR-021:</p>
 * <ul>
 *   <li><b>vocabulary</b> -- UIElement model, capabilities, deferred actions, roles</li>
 *   <li><b>implementation</b> -- UIEngine contract, Selenium executor, locator resolution</li>
 * </ul>
 *
 * @see domain.automation.web.engine.UIEngine
 * @see domain.automation.web.vocabulary.element.UIElement
 */
package domain.automation.web;
