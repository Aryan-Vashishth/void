// file: core/resolvers/locator/properties/PropertiesFileLocatorReaderV1.java
package core.resolvers.locator.properties;

import core.utils.io.properties.PropertiesReader;
import org.openqa.selenium.By;

import java.util.Locale;

/**
 * V1: Converts locator strings into Selenium {@link By} via prefix-based parsing.
 * Use {@link #getRaw(String, String)} when you need to apply formatting in the resolver
 * before calling {@link #toBy(String)}.
 *
 * Supported (case-insensitive): id=, name=, class=, tag=, linkText=, partialLinkText=, css=, xpath=
 * Fallback: starts with '/' or '(' (or '//' / './/') -> XPath, else CSS.
 */
public final class PropertiesFileLocatorReaderV1 {

    /** Classpath base directory for all locator property files.
     *  Must match {@code ElementLocatorResolverV1.CLASSPATH_BASE}. */
    private static final String LOCATORS_BASE = "locators/";

    private PropertiesFileLocatorReaderV1() {}

    /** Convenience: read from .properties and convert directly to By (no arg formatting). */
    public static By getLocatorValueSafely(String fileName, String key) {
        String raw = getRaw(fileName, key);
        return toBy(raw);
    }

    /**
     * RAW accessor (no formatting). Prepends {@value LOCATORS_BASE} to {@code fileName}
     * when it is not already an absolute classpath path, keeping parity with
     * {@code ElementLocatorResolverV1.CLASSPATH_BASE} so that both resolvers
     * resolve the same {@code getExternalFileName()} values correctly.
     */
    public static String getRaw(String fileName, String key) {
        if (fileName == null) return null;
        String path = fileName.startsWith(LOCATORS_BASE) ? fileName : LOCATORS_BASE + fileName;
        return PropertiesReader.getValue(path, key); // may return null
    }

    /** Convert a raw locator string to {@link By}. */
    public static By toBy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Locator string is null or blank.");
        }

        String trimmed = raw.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Prefix parsing
        if (lower.startsWith("id="))               return By.id(valueAfter(trimmed, 3, "id"));
        if (lower.startsWith("name="))             return By.name(valueAfter(trimmed, 5, "name"));
        if (lower.startsWith("class="))            return By.className(valueAfter(trimmed, 6, "class"));
        if (lower.startsWith("tag="))              return By.tagName(valueAfter(trimmed, 4, "tag"));
        if (lower.startsWith("linktext="))         return By.linkText(valueAfter(trimmed, 9, "linkText"));
        if (lower.startsWith("partiallinktext="))  return By.partialLinkText(valueAfter(trimmed, 16, "partialLinkText"));
        if (lower.startsWith("css="))              return By.cssSelector(valueAfter(trimmed, 4, "css"));
        if (lower.startsWith("xpath="))            return By.xpath(valueAfter(trimmed, 6, "xpath"));

        // Fallback: infer automatically.
        // Note: startsWith("//") is already covered by startsWith("/"); kept for readability.
        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".//")) {
            return By.xpath(trimmed);
        }
        return By.cssSelector(trimmed);
    }

    /** Extract and trim substring; ensure it is non-empty to avoid silent errors. */
    private static String valueAfter(String s, int from, String prefixName) {
        String v = s.substring(from).trim();
        if (v.isEmpty()) {
            throw new IllegalStateException("Empty value after '" + prefixName + "=' in locator: " + s);
        }
        return v;
    }
}
