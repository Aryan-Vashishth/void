/**
 * {@code domain.automation.web.resolve} -- Locator resolution subsystem.
 *
 * <p>Parent package grouping all locator resolution concerns for the Web domain.
 * Resolution turns a {@link domain.automation.web.vocabulary.element.UIElement}
 * constant into a {@link domain.automation.web.locator.LocatorDescriptor} at
 * execution time by reading external locator files and parsing prefixed strings.</p>
 *
 * <h3>Sub-packages</h3>
 * <ul>
 *   <li>{@code resolve.api} -- resolution contract, resolver builder, path conventions</li>
 *   <li>{@code resolve.json} -- JSON-format locator file reader</li>
 *   <li>{@code resolve.parser} -- prefix-based locator string parser</li>
 *   <li>{@code resolve.properties} -- properties-format locator file reader</li>
 *   <li>{@code resolve.source} -- source abstraction and registry</li>
 * </ul>
 */
package domain.automation.web.resolve;
