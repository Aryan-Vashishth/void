/**
 * {@code domain.automation.web.resolve.parser} -- Locator prefix parser.
 *
 * <p>Translates prefix-annotated locator strings ({@code xpath=//div}, {@code css=.btn},
 * {@code id=submit}) into typed {@link domain.automation.web.locator.LocatorDescriptor}
 * objects. Prefix handling extends naturally with the open
 * {@link domain.automation.web.locator.LocatorStrategy} set.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.resolve.parser.ByParser} -- parses a raw locator
 *       string into a {@code LocatorDescriptor}; delegates prefix matching to
 *       registered strategies.</li>
 *   <li>{@link domain.automation.web.resolve.parser.ByPrefixStrategy} -- maps a string
 *       prefix to a {@code LocatorStrategy}; extensible without editing the parser.</li>
 * </ul>
 */
package domain.automation.web.resolve.parser;
