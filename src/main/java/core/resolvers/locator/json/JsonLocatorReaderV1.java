// file: core/resolvers/locator/json/JsonLocatorReaderV1.java
package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.resolvers.locator.api.LocatorPaths;
import core.utils.ConfigLoader;

import java.io.InputStream;

/**
 * V1 JSON reader shim used by the locator resolvers.
 * Returns the RAW (unformatted) locator template for {@code (fileName, key)}.
 *
 * <p><b>Refactor note:</b> traversal logic moved to {@link JsonNodeLookup} so it can be
 * unit-tested in isolation. Class-path resolution uses {@link LocatorPaths}.</p>
 */
public final class JsonLocatorReaderV1 {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Configurable classpath base (default {@link LocatorPaths#JSON_BASE_DEFAULT}). */
    private static final String LOCATOR_BASE_PATH = ConfigLoader.get(
            "locator.json.base.path",
            LocatorPaths.JSON_BASE_DEFAULT
    );

    private JsonLocatorReaderV1() {}

    /** RAW accessor — no formatting here. */
    public static String getRaw(String fileName, String key) {
        if (fileName == null) return key; // hardcoded safeguard
        if (key == null || key.isBlank()) return null;

        JsonNode root = load(LocatorPaths.under(LOCATOR_BASE_PATH, fileName));
        if (root == null) return null;

        return JsonNodeLookup.findText(root, key);
    }

    private static JsonNode load(String cp) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(cp)) {
            if (in == null) return null;
            return MAPPER.readTree(in);
        } catch (Exception e) {
            return null;
        }
    }
}
