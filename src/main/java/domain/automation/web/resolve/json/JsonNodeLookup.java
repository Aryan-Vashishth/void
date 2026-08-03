package domain.automation.web.resolve.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Pure, dependency-free traversal helpers for {@link JsonNode} trees, extracted from
 * {@code JsonLocatorReaderV1}. Kept package-private to the {@code json} sub-package because
 * the locator-specific lookup semantics (dot-path → deep-find fallback → textual coercion)
 * are an implementation detail of the JSON locator source.
 */
final class JsonNodeLookup {

    private JsonNodeLookup() {}

    /**
     * Strict dot-path traversal: {@code "a.b.c"} → {@code root.get("a").get("b").get("c")}.
     * Returns {@code null} if any intermediate node is missing.
     */
    public static JsonNode findByDotPath(JsonNode root, String dotPath) {
        if (root == null || dotPath == null || dotPath.isBlank()) return null;
        JsonNode n = root;
        for (String p : dotPath.split("\\.")) {
            if (n == null) return null;
            n = n.get(p);
        }
        return n;
    }

    /**
     * Fallback finder: search by field name anywhere in the tree (depth-first).
     * Returns {@code null} when not found (i.e. converts {@code MissingNode} → {@code null}).
     */
    public static JsonNode deepFindField(JsonNode node, String name) {
        if (node == null) return null;
        JsonNode direct = node.findPath(name);
        return direct.isMissingNode() ? null : direct;
    }

    /**
     * Locator-style lookup: try dot-path first, fall back to deep-find, return the textual
     * value of the resulting node — or {@code null} if nothing usable was found.
     */
    public static String findText(JsonNode root, String key) {
        JsonNode node = findByDotPath(root, key);
        if (node == null) node = deepFindField(root, key);
        return (node != null && node.isTextual()) ? node.asText() : null;
    }
}

