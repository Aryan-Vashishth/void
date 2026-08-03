/**
 * {@code domain.automation.web.resolve.api} -- Locator resolution contract.
 *
 * <p>Defines how {@link domain.automation.web.vocabulary.element.UIElement} instances are
 * resolved to {@link domain.automation.web.locator.LocatorDescriptor} objects at execution
 * time. The central entry points are {@link domain.automation.web.resolve.api.LocatorResolvers}
 * (factory) and {@link domain.automation.web.resolve.api.LocatorResolver} (builder + instance).</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.resolve.api.LocatorResolver} -- resolves a UIElement
 *       to a {@code LocatorDescriptor} using a configurable source chain.</li>
 *   <li>{@link domain.automation.web.resolve.api.LocatorResolvers} -- factory for common
 *       resolver configurations (strict, lenient).</li>
 *   <li>{@link domain.automation.web.resolve.api.LocatorRequest} -- immutable value object
 *       carrying file name, key, and runtime args for a single resolution lookup.</li>
 *   <li>{@link domain.automation.web.resolve.api.LocatorContext} -- SPI for file-name
 *       resolution; {@link domain.automation.web.resolve.api.DefaultLocatorContext} is the
 *       conventional implementation (caches per enum class).</li>
 *   <li>{@link domain.automation.web.resolve.api.ConventionalLocatorPath} -- derives the
 *       classpath path for a page class's locator file from its package name.</li>
 *   <li>{@link domain.automation.web.resolve.api.LocatorPaths} -- utilities for constructing
 *       and normalising locator file paths.</li>
 * </ul>
 */
package domain.automation.web.resolve.api;
