package core.resolvers.locator.properties;

import Elements.interfacesv2.BaseElement;
import org.openqa.selenium.By;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PropertiesFileLocatorReader (V2)
 * ---------------------------------------------------------
 * Centralized .properties locator resolution based on BaseElement V2.
 * - Uses element.getPrimaryLocator() / getSecondaryLocator() to choose keys.
 * - Base path is "locators/properties/" (classpath).
 * - Applies String.format (%s) or MessageFormat {0} style arguments safely.
 */
public final class PropertiesFileLocatorReader {

    private static final String LOCATOR_BASE_PATH = "locators/properties/";
    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    private PropertiesFileLocatorReader() {}

    /* -------------------------------------------------------
     * Public API
     * ----------------------------------------------------- */

    /** Resolve the PRIMARY locator as a Selenium By. */
    public static By resolvePrimary(BaseElement element) {
        return toBy(resolveRaw(element, element.getPrimaryLocator(), element.getArgs()));
    }

    /** Resolve the SECONDARY locator as a Selenium By. */
    public static By resolveSecondary(BaseElement element) {
        return toBy(resolveRaw(element, element.getSecondaryLocator(), element.getArgs()));
    }

    /** Resolve any arbitrary key of this element to a By. */
    public static By resolveKey(BaseElement element, String key, Object... args) {
        return toBy(resolveRaw(element, key, (args != null && args.length > 0) ? args : element.getArgs()));
    }

    /** Direct low-level accessor if you only need the raw string. */
    public static String getRaw(String propertiesFile, String key, Object... args) {
        Properties props = load(propertiesFile);
        String value = props.getProperty(requireKey(key));
        if (value == null) {
            throw new IllegalStateException("Missing locator key '" + key + "' in " + propertiesFile);
        }
        return applyArgs(value, args);
    }

    /* -------------------------------------------------------
     * Internal helpers
     * ----------------------------------------------------- */

    private static String resolveRaw(BaseElement element, String key, Object[] args) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Locator key is null/blank for " + safeName(element));
        }
        String file = element.getPropertyFile();
        if (file == null || file.isBlank()) {
            throw new IllegalStateException("Property file is not set for " + safeName(element));
        }
        Properties props = load(file);
        String raw = props.getProperty(requireKey(key));
        if (raw == null) {
            throw new IllegalStateException("Missing locator key '" + key + "' in " + file);
        }
        return applyArgs(raw, args);
    }

    private static Properties load(String fileName) {
        return CACHE.computeIfAbsent(fileName, f -> {
            String cp = f.startsWith(LOCATOR_BASE_PATH) ? f : LOCATOR_BASE_PATH + f;
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(cp)) {
                if (in == null) {
                    throw new IllegalStateException("Properties file not found on classpath: " + cp);
                }
                Properties p = new Properties();
                p.load(in);
                return p;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load properties: " + cp, e);
            }
        });
    }

    private static By toBy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Resolved locator is null/blank.");
        }
        String trimmed = raw.trim();

        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".//") || trimmed.startsWith("//")) {
            return By.xpath(trimmed);
        }
        return By.cssSelector(trimmed);
    }

    private static String applyArgs(String template, Object[] args) {
        if (template == null) return null;
        Object[] safeArgs = (args == null) ? new Object[0] : args;

        if (template.contains("%s")) {
            return String.format(Locale.ROOT, template, safeArgs);
        }
        if (template.contains("{0")) {
            return MessageFormat.format(template, safeArgs);
        }
        return template;
    }

    private static String requireKey(String key) {
        String k = (key == null) ? "" : key.trim();
        if (k.isEmpty()) throw new IllegalArgumentException("Requested locator key is blank.");
        return k;
    }

    private static String safeName(BaseElement e) {
        try { return ((Enum<?>) e).name(); } catch (Exception ignore) { return Objects.toString(e); }
    }
}
