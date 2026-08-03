/**
 * {@code domain.automation.web.resolve.source} -- Locator source abstraction and registry.
 *
 * <p>Defines the {@link domain.automation.web.resolve.source.LocatorSource} SPI and its
 * concrete implementations. A {@code LocatorSource} supplies raw locator strings for a
 * given {@link domain.automation.web.resolve.api.LocatorRequest};
 * {@link domain.automation.web.resolve.source.LocatorSourceRegistry} chains sources in
 * priority order (first non-empty result wins).</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.resolve.source.LocatorSource} -- SPI interface for
 *       locator suppliers.</li>
 *   <li>{@link domain.automation.web.resolve.source.LocatorSourceRegistry} -- chains
 *       multiple sources in priority order.</li>
 *   <li>{@link domain.automation.web.resolve.source.JsonLocatorSource} -- reads from
 *       JSON locator files.</li>
 *   <li>{@link domain.automation.web.resolve.source.PropertiesLocatorSource} -- reads
 *       from properties locator files.</li>
 *   <li>{@link domain.automation.web.resolve.source.HardcodedLocatorSource} -- uses the
 *       locator key directly when no external file is involved.</li>
 *   <li>{@link domain.automation.web.resolve.source.LayeredPropertiesLocatorSource} --
 *       merges multiple properties files in precedence order.</li>
 * </ul>
 */
package domain.automation.web.resolve.source;
