/**
 * Polymorphic backing formats for raw locator templates.
 *
 * <p>{@link core.resolvers.locator.source.LocatorSource} is the interface; concrete impls:</p>
 * <ul>
 *   <li>{@link core.resolvers.locator.source.HardcodedLocatorSource} — null fileName,
 *       key is the template.</li>
 *   <li>{@link core.resolvers.locator.source.PropertiesLocatorSource} — simple
 *       {@code .properties} lookup via {@code PropertiesReader}.</li>
 *   <li>{@link core.resolvers.locator.source.LayeredPropertiesLocatorSource} — cached
 *       {@code ConfigLoader.Layered} ({@code TEST → MAIN → external override → sysprops/env}).</li>
 *   <li>{@link core.resolvers.locator.source.JsonLocatorSource} — classpath {@code .json}
 *       with dot-path + deep-find traversal.</li>
 * </ul>
 *
 * <p>{@link core.resolvers.locator.source.LocatorSourceRegistry} is the
 * Chain-of-Responsibility selector — open for extension via {@code .with(extra)}.</p>
 */
package core.resolvers.locator.source;

