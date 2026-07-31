// file: core/resolvers/locator/json/JsonLocatorReaderV1.java
package domain.automation.web.resolve.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.automation.web.resolve.api.LocatorPaths;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1 JSON reader shim used by the locator resolvers.
 * Returns the RAW (unformatted) locator template for {@code (fileName, key)}.
 *
 * <p><b>Refactor note:</b> traversal logic moved to {@link JsonNodeLookup} so it can be
 * unit-tested in isolation. Class-path resolution uses {@link LocatorPaths#underJson(String)}.</p>
 */
public final class JsonLocatorReader {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ConcurrentHashMap<String, Optional<JsonNode>> NODE_CACHE =
            new ConcurrentHashMap<>();

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
        return NODE_CACHE.computeIfAbsent(cp, key -> {
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(key)) {
                if (in == null) return Optional.empty();
                return Optional.of(MAPPER.readTree(in));
            } catch (Exception e) {
                return Optional.empty();
            }
        }).orElse(null);
    }
}
