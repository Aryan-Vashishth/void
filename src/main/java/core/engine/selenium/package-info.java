/**
 * {@code core.engine.selenium} — Selenium WebDriver implementation of the UIEngine contract.
 *
 * <p>Contains {@link core.engine.selenium.SeleniumEngine}, the default (and currently only
 * production) implementation of {@link core.engine.UIEngine}. This engine translates
 * engine-agnostic {@link core.engine.LocatorDescriptor} objects into Selenium {@code By}
 * locators and executes all browser interactions through the WebDriver API.</p>
 *
 * <h3>Key type</h3>
 * <ul>
 *   <li>{@link core.engine.selenium.SeleniumEngine} — implements all {@code UIEngine}
 *       methods: click, type, select, hover, read text, resolve locators, etc.
 *       Internally handles scrolling, explicit waits, retries, and fallback
 *       strategies.</li>
 * </ul>
 *
 * <h3>Responsibilities of SeleniumEngine</h3>
 * <ul>
 *   <li>Resolve {@link core.engine.LocatorDescriptor} → Selenium {@code By}</li>
 *   <li>Scroll elements into view before interaction</li>
 *   <li>Apply explicit waits (visibility, clickability) as needed</li>
 *   <li>Retry transient failures (stale element, intercepted click)</li>
 *   <li>Execute JavaScript-based fallbacks when standard interactions fail</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Callers (Actions, Interactions) must <b>not</b> perform their own scrolling,
 *       waiting, or retry logic — the engine owns all execution concerns.</li>
 *   <li>This package is the only place in VOID that directly depends on
 *       {@code org.openqa.selenium} for element interaction.</li>
 *   <li>Adding a new engine (e.g., Playwright) means creating a new sub-package
 *       under {@code core.engine} — no changes to test-level code required.</li>
 * </ul>
 *
 * @see core.engine.UIEngine
 * @see core.engine.LocatorDescriptor
 * @see core.engine.LocatorStrategy
 */
package core.engine.selenium;

