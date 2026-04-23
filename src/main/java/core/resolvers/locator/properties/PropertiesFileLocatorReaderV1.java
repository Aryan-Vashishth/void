// file: core/resolvers/locator/properties/PropertiesFileLocatorReaderV1.java
package core.resolvers.locator.properties;

import core.resolvers.locator.api.LocatorPaths;
import core.resolvers.locator.parser.ByParser;
import core.utils.io.properties.PropertiesReader;
import org.openqa.selenium.By;

/**
 * V1: Converts locator strings into Selenium {@link By} via prefix-based parsing.
 *
 * <p><b>Refactor note:</b> all parsing logic now delegates to {@link ByParser}; this class is
 * retained as a thin façade for backward compatibility. New code should use {@link ByParser}
 * directly.</p>
 */
public final class PropertiesFileLocatorReaderV1 {

    private PropertiesFileLocatorReaderV1() {}

    /** Convenience: read from .properties and convert directly to By (no arg formatting). */
    public static By getLocatorValueSafely(String fileName, String key) {
        return toBy(getRaw(fileName, key));
    }

    /**
     * RAW accessor (no formatting). Prepends {@link LocatorPaths#PROPERTIES_BASE} to {@code fileName}
     * when it is not already prefixed.
     */
    public static String getRaw(String fileName, String key) {
        if (fileName == null) return null;
        return PropertiesReader.getValue(LocatorPaths.underProperties(fileName), key); // may return null
    }

    /** Convert a raw locator string to {@link By}. Throws {@link IllegalStateException} on null/blank. */
    public static By toBy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Locator string is null or blank.");
        }
        return ByParser.DEFAULT.parse(raw);
    }
}
