/**
 * {@code domain.automation.web.selenium} -- Selenium WebDriver implementation of the Web engine.
 *
 * <p>Contains the production implementation of {@link domain.automation.web.engine.UIEngine}
 * backed by Selenium WebDriver. This is the only package in VOID that may call WebDriver
 * element-interaction APIs directly (ADR-007, ADR-018).</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.selenium.SeleniumEngine} -- implements all
 *       {@code UIEngine} methods: click, type, select, hover, read text, resolve locators.
 *       Internally handles scrolling, explicit waits, retries, and JavaScript fallbacks.</li>
 *   <li>{@link domain.automation.web.selenium.SeleniumEngineRegistrar} -- self-registers
 *       {@code SeleniumEngine} via the {@code EngineRegistrar} SPI.</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>A second web engine (e.g., Playwright) is a sibling package
 *       ({@code domain.automation.web.playwright}) with its own {@code EngineRegistrar}
 *       entry -- no changes to kernel or contract types required.</li>
 *   <li>Driver lifecycle is in the {@code driver} sub-package.</li>
 * </ul>
 *
 * @see domain.automation.web.engine.UIEngine
 * @see domain.automation.web.selenium.driver
 */
package domain.automation.web.selenium;
