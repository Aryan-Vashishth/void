/**
 * Public-facing locator resolution API.
 *
 * <p>Contains the value objects and the orchestrator that callers interact with:</p>
 * <ul>
 *   <li>{@link core.resolvers.locator.api.LocatorPaths} — classpath-base constants.</li>
 *   <li>{@link core.resolvers.locator.api.LocatorRequest} — value object {@code (fileName, key, args)}.</li>
 *   <li>{@link core.resolvers.locator.api.LocatorResolver} — instance orchestrator (configurable via builder).</li>
 *   <li>{@link core.resolvers.locator.api.LocatorResolvers} — static façade exposing the
 *       preconfigured {@code strict()} and {@code legacyPadded()} singletons.</li>
 * </ul>
 *
 * <p>New code should depend on this package only -- internal SPI lives in
 * {@code parser}, {@code template}, and {@code source}.</p>
 *
 * <h3>Domain ownership (ADR-021 addendum, runtime-redesign I6.2)</h3>
 * <p>All types in this package are <strong>Web-domain implementation</strong>
 * (locator resolution public API). Physical relocation to
 * {@code domain.automation.web.resolve.api} is gated on the I6.4 Class Migration
 * Matrix execution. ADR-021 addendum.</p>
 */
package core.resolvers.locator.api;

