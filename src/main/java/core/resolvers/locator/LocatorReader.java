// file: core/resolvers/locator/LocatorReader.java
package core.resolvers.locator;

@FunctionalInterface
public interface LocatorReader {
    /** Return the raw locator template for (fileName, key) or null if missing. */
    String getRaw(String fileName, String key);
}
