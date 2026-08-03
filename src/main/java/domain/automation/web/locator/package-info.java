/**
 * {@code domain.automation.web.locator} -- Web-domain locator vocabulary.
 *
 * <p>Engine-agnostic locator representation: describes where a web element lives
 * without binding to any WebDriver API. Produced by the resolution layer and consumed
 * by {@link domain.automation.web.engine.UIEngine} implementations.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.locator.LocatorDescriptor} -- immutable value
 *       carrying a strategy and locator expression (e.g., XPATH + "//button[@id='ok']"),
 *       plus optional metadata (source file, raw key, template).</li>
 *   <li>{@link domain.automation.web.locator.LocatorStrategy} -- open strategy set;
 *       extensible interface with XPATH, CSS, ID, NAME, TAG, LINK_TEXT, and
 *       PARTIAL_LINK_TEXT defined as constants (I7.1).</li>
 *   <li>{@link domain.automation.web.locator.NamedStrategy} -- record implementing
 *       {@code LocatorStrategy} for strategy constants.</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Contains no Selenium types. {@code By} is confined to
 *       {@code domain.automation.web.selenium} (ADR-018, ADR-019).</li>
 *   <li>{@code LocatorStrategy} was opened from a closed enum to an interface in I7.1
 *       so domain-defined strategies require no framework edits.</li>
 * </ul>
 */
package domain.automation.web.locator;
