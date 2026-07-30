/**
 * {@code core.engine.selenium} -- Selenium WebDriver implementation of the UIEngine contract.
 *
 * <p>Contains {@link core.engine.selenium.SeleniumEngine}, the default (and currently only
 * production) implementation of {@link core.engine.UIEngine}. This engine translates
 * engine-agnostic {@link elements.locator.LocatorDescriptor} objects into Selenium
 * {@code By} locators and executes all browser interactions through the WebDriver API.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.engine.selenium.SeleniumEngine} -- implements all {@code UIEngine}
 *       methods: click, type, select, hover, read text, resolve locators, etc.
 *       Internally handles scrolling, explicit waits, retries, and fallback
 *       strategies.</li>
 *   <li>{@link core.engine.selenium.SeleniumEngineRegistrar} -- self-registers
 *       {@code SeleniumEngine} via the {@code core.engine.EngineRegistrar} SPI.</li>
 *   <li>{@link core.engine.selenium.WebDomainRegistrar} -- registers the Web domain
 *       with the kernel's {@code core.engine.DomainRegistrar} SPI; temporary home
 *       alongside engine registrars pending I6.4 relocation.</li>
 * </ul>
 *
 * <h3>Responsibilities of SeleniumEngine</h3>
 * <ul>
 *   <li>Resolve {@link elements.locator.LocatorDescriptor} to Selenium {@code By}</li>
 *   <li>Scroll elements into view before interaction</li>
 *   <li>Apply explicit waits (visibility, clickability) as needed</li>
 *   <li>Retry transient failures (stale element, intercepted click)</li>
 *   <li>Execute JavaScript-based fallbacks when standard interactions fail</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Callers must <b>not</b> perform their own scrolling, waiting, or retry logic
 *       -- the engine owns all execution concerns.</li>
 *   <li>This package is the only place in VOID that directly depends on
 *       {@code org.openqa.selenium} for element interaction.</li>
 *   <li>Adding a second web engine (e.g., Playwright) means a new sub-package
 *       alongside this one plus a new {@code EngineRegistrar} services file entry
 *       -- no changes to kernel or {@code core.engine} contract classes required.</li>
 * </ul>
 *
 * <h3>Domain ownership (ADR-021 addendum, runtime-redesign I6.2)</h3>
 * <p>All types in this package are <strong>Web-domain implementation</strong>
 * (Selenium executor layer). Physical relocation to
 * {@code domain.automation.web.selenium} is gated on the I6.4 Class Migration
 * Matrix execution. {@code WebDomainRegistrar} specifically relocates to
 * {@code domain.automation.web.WebDomainRegistrar} (domain root, not
 * selenium-specific). ADR-021 addendum.</p>
 *
 * @see core.engine.UIEngine
 * @see elements.locator.LocatorDescriptor
 * @see elements.locator.LocatorStrategy
 */
package core.engine.selenium;

