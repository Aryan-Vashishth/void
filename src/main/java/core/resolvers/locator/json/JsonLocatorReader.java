// file: core/resolvers/locator/json/JsonLocatorReaderV1.java
package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.resolvers.locator.api.LocatorPaths;

import java.io.InputStream;

/**
 * V1 JSON reader shim used by the locator resolvers.
 * Returns the RAW (unformatted) locator template for {@code (fileName, key)}.
 *
 * <p><b>Refactor note:</b> traversal logic moved to {@link JsonNodeLookup} so it can be
 * unit-tested in isolation. Class-path resolution uses {@link LocatorPaths#underJson(String)}.</p>
 */
public final class JsonLocatorReader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonLocatorReader() {}

    /** RAW accessor — no formatting here. */
    public static String getRaw(String fileName, String key) {
        if (fileName == null) return key; // hardcoded safeguard
        if (key == null || key.isBlank()) return null;

        JsonNode root = load(LocatorPaths.underJson(fileName));
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
