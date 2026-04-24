/**
 * Locator resolution layer.
 *
 * <p>Reads element locators from external sources ({@code .properties}, {@code .json}, or
 * hardcoded inline) and resolves them into Selenium {@code By} instances at runtime.</p>
 *
 * <h2>Sub-package layout (post-refactor)</h2>
 * <ul>
 *   <li>{@link core.resolvers.locator.api api} — public API: {@code LocatorResolver},
 *       {@code LocatorResolvers}, {@code LocatorRequest}, {@code LocatorPaths}.</li>
 *   <li>{@link core.resolvers.locator.parser parser} — raw-string → {@code By}
 *       ({@code ByParser}, {@code ByPrefixStrategy}).</li>
 *   <li>{@link core.resolvers.locator.template template} — {@code LocatorTemplate}
 *       with STRICT/PAD_LAST policies.</li>
 *   <li>{@link core.resolvers.locator.source source} — polymorphic backing formats:
 *       {@code LocatorSource} interface + {@code Hardcoded}, {@code Properties},
 *       {@code LayeredProperties}, {@code Json} impls + {@code LocatorSourceRegistry}.</li>
 *   <li>{@code json} / {@code properties} — format-specific readers and the JSON migrator.</li>
 * </ul>
 *
 * <h2>For new code</h2>
 * Use {@link core.resolvers.locator.api.LocatorResolvers#strict()}:
 * <pre>{@code
 * By by = LocatorResolvers.strict().resolve(file, key, args);
 * By by = LocatorResolvers.strict().resolve(myElement);
 * }</pre>
 */
package core.resolvers.locator;

