/**
 * {@code domain.automation.web.resolve.json} -- JSON locator file reader.
 *
 * <p>Reads locator values from JSON-format locator files ({@code locators.json}) and
 * builds the flat in-memory index used by the resolution pipeline.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.resolve.json.JsonLocatorReader} -- entry point;
 *       reads a classpath-relative JSON file and resolves a keyed locator string.</li>
 *   <li>{@link domain.automation.web.resolve.json.JsonTreeBuilder} -- builds a
 *       {@code PropertiesIndex} from a Jackson {@code JsonNode} tree, flattening
 *       nested keys into dot-separated paths.</li>
 *   <li>{@link domain.automation.web.resolve.json.PropertiesIndex} -- flat key-to-value
 *       index produced from a parsed locator file.</li>
 *   <li>{@link domain.automation.web.resolve.json.JsonNodeLookup} -- package-private
 *       utility for navigating Jackson {@code JsonNode} trees.</li>
 * </ul>
 */
package domain.automation.web.resolve.json;
