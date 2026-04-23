/**
 * Locator resolution layer.
 *
 * <p>Reads element locators from external sources ({@code .properties}, {@code .json}, or
 * hardcoded inline) and resolves them into Selenium {@code By} instances at runtime.</p>
 *
 * <h2>Architecture (post-Phase 1–6 OO refactor)</h2>
 * <ul>
 *   <li>{@link core.resolvers.locator.LocatorRequest} — value object {@code (fileName, key, args)}.</li>
 *   <li>{@link core.resolvers.locator.LocatorTemplate} — placeholder formatter with
 *       {@link core.resolvers.locator.LocatorTemplate.Policy#STRICT STRICT} and
 *       {@link core.resolvers.locator.LocatorTemplate.Policy#PAD_LAST PAD_LAST} policies.</li>
 *   <li>{@link core.resolvers.locator.ByParser} — single canonical raw-string → {@code By} parser
 *       (prefix table + heuristic fallback).</li>
 *   <li>{@link core.resolvers.locator.LocatorSource} — polymorphic backing format
 *       ({@link core.resolvers.locator.HardcodedLocatorSource hardcoded},
 *       {@link core.resolvers.locator.PropertiesLocatorSource properties},
 *       {@link core.resolvers.locator.LayeredPropertiesLocatorSource layered properties},
 *       {@link core.resolvers.locator.JsonLocatorSource json}).</li>
 *   <li>{@link core.resolvers.locator.LocatorSourceRegistry} — open-for-extension chain selecting
 *       the source for a given file name.</li>
 *   <li>{@link core.resolvers.locator.LocatorResolver} — instance orchestrator composing all of
 *       the above; built via {@link core.resolvers.locator.LocatorResolver#builder()}.</li>
 *   <li>{@link core.resolvers.locator.LocatorResolvers} — static façade exposing
 *       {@link core.resolvers.locator.LocatorResolvers#strict() strict()} (recommended) and
 *       {@link core.resolvers.locator.LocatorResolvers#legacyPadded() legacyPadded()}.</li>
 * </ul>
 *
 * <h2>For new code</h2>
 * Use {@link core.resolvers.locator.LocatorResolvers#strict()}:
 * <pre>{@code
 * By by = LocatorResolvers.strict().resolve(file, key, args);
 * By by = LocatorResolvers.strict().resolve(myElement);
 * }</pre>
 *
 * <h2>Legacy</h2>
 * The static façades {@link core.resolvers.locator.ElementLocatorResolverV1} and
 * {@link core.resolvers.locator.LocatorResolverV1} are now thin delegates and are marked
 * {@link Deprecated @Deprecated(forRemoval = true)}. Migrate callers to
 * {@link core.resolvers.locator.LocatorResolvers}.
 *
 * <p>The format-specific readers live in the {@code json} and {@code properties} sub-packages.</p>
 */
package core.resolvers.locator;

