package core.resolvers.locator.source;

import core.resolvers.locator.api.LocatorRequest;

/**
 * Polymorphic source of raw locator templates — one implementation per backing format.
 *
 * <p>Replaces the {@code pickReader(...)} switch that used to live in
 * {@code LocatorResolverV1} and the inline JSON-extension check in
 * {@code ElementLocatorResolverV1.getRawLocator}. Adding a new backing format
 * (YAML, database, REST, …) is now an open-for-extension operation: implement
 * this interface and register the instance with {@link LocatorSourceRegistry}.</p>
 *
 * <p>Implementations are expected to be <em>stateless</em> with respect to a
 * single request, but may hold internal caches.</p>
 */
public interface LocatorSource {

    /** {@code true} if this source can serve the given {@code fileName}. */
    boolean supports(String fileName);

    /**
     * Return the raw, un-formatted locator template for the request,
     * or {@code null} if the key is not present in this source.
     *
     * @throws IllegalArgumentException if invoked when {@link #supports(String)}
     *                                  returns {@code false} for the request's file name
     */
    String readRaw(LocatorRequest request);

    /** Short identifier used in diagnostic messages and logs (e.g. {@code "properties"}). */
    String name();
}

