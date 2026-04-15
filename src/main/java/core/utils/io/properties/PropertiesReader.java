// file: core/util/PropertiesFileReader.java
package core.utils.io.properties;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Common utility for reading .properties files from classpath.
 * Caches loaded bundles to avoid repeated I/O.
 */
public final class PropertiesReader {

    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    private PropertiesReader() {}

    /**
     * Loads (and caches) a .properties file from the classpath.
     *
     * @param filePathInResources classpath-relative name, e.g. "locators/properties/login.properties"
     * @return loaded Properties instance
     * @throws IllegalStateException if file not found or cannot be read
     */
    public static Properties load(String filePathInResources) {
        return CACHE.computeIfAbsent(filePathInResources, f -> {
            try (InputStream in = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(f)) {
                if (in == null) {
                    throw new IllegalStateException("Properties file not found: " + f);
                }
                Properties props = new Properties();
                props.load(in);
                return props;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load properties: " + f, e);
            }
        });
    }

    /**
     * Returns a single property value from the given file, or null if missing.
     */
    public static String getValue(String filePathInResources, String key) {
        Properties props = load(filePathInResources);
        return props.getProperty(key);
    }

    /**
     * Clears the in-memory cache (useful in tests).
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
