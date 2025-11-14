// file: core/resolvers/locator/json/JsonLocatorReaderV1.java
package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.utils.ConfigLoader;

import java.io.InputStream;

/**
 * V1 JSON reader shim used by LocatorResolverV1.
 * Returns the RAW (unformatted) locator template for (fileName, key).
 *
 * Looks up JSON from the classpath under LOCATOR_BASE_PATH.
 * Ensure your resources are copied to the runtime classpath accordingly.
 */
public final class JsonLocatorReaderV1 {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Adjust this if you want a different base; keep consistent with how you package resources.
    private static final String LOCATOR_BASE_PATH = ConfigLoader.get(
            "locator.json.base.path",
            "locators/json/"
    );

    private JsonLocatorReaderV1() {}

    /** RAW accessor used by LocatorResolverV1 — no formatting here. */
    public static String getRaw(String fileName, String key) {
        if (fileName == null) return key; // hardcoded safeguard
        if (key == null || key.isBlank()) return null;

        JsonNode root = load(joinBase(fileName));
        if (root == null) return null;

        // First try dot-path lookup (e.g., "login.username")
        JsonNode node = findByDotPath(root, key);
        if (node == null) {
            // Fallback: find by field name anywhere (less strict)
            node = deepFindField(root, key);
        }
        return (node != null && node.isTextual()) ? node.asText() : null;
    }

    private static String joinBase(String f) {
        return f.startsWith(LOCATOR_BASE_PATH) ? f : LOCATOR_BASE_PATH + f;
    }

    private static JsonNode load(String cp) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(cp)) {
            if (in == null) return null;
            return MAPPER.readTree(in);
        } catch (Exception e) {
            return null;
        }
    }

    /** Strict dot-path traversal: "a.b.c" → node.get("a").get("b").get("c") */
    private static JsonNode findByDotPath(JsonNode root, String dotPath) {
        if (root == null || dotPath == null || dotPath.isBlank()) return null;
        JsonNode n = root;
        String[] parts = dotPath.split("\\.");
        for (String p : parts) {
            if (n == null) return null;
            n = n.get(p);
        }
        return n;
    }

    /** Fallback finder: search by field name anywhere in the tree. */
    private static JsonNode deepFindField(JsonNode node, String name) {
        if (node == null) return null;
        JsonNode direct = node.findPath(name);
        return direct.isMissingNode() ? null : direct;
        // Note: findPath returns MissingNode if not found.
    }
}
