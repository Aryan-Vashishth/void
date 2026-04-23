// file: core/resolvers/locator/LocatorReader.java
package core.resolvers.locator;

/**
 * Legacy two-argument reader interface.
 *
 * @deprecated since the Phase&nbsp;2 OO refactor; implement {@link LocatorSource} and register it
 *             with {@link LocatorSourceRegistry} instead. This functional interface is retained
 *             only for binary compatibility with external callers and will be removed.
 */
@Deprecated(forRemoval = true, since = "Phase 2 OO refactor")
@FunctionalInterface
public interface LocatorReader {
    /** Return the raw locator template for (fileName, key) or null if missing. */
    String getRaw(String fileName, String key);
}
