package core.locators;

import Elements.Interfaces.BaseElement;
import org.openqa.selenium.By;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * JsonLocatorReader (V2)
 * ---------------------------------------------------------
 * Centralized JSON locator resolution based on BaseElement V2.
 * - Uses element.getPrimaryLocator() / getSecondaryLocator() to choose keys.
 * - Finds the JSON file by converting the properties file name
 *   (e.g., "manage-users-elements.properties" -> "locators/json/manage-users-elements.json").
 * - Deep-searches the JSON tree for a field with the exact key name.
 * - Safely applies String.format-style (%s) arguments from BaseElement.getArgs().
 * - If JSON is missing or key not found, callers can fall back to PropertiesFileLocatorReader.
 */
public final class JsonLocatorReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonLocatorReader() {}

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

    /* -------------------------------------------------------
     * Internal helpers
     * ----------------------------------------------------- */

    private static By toBy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Resolved locator is null/blank.");
        }
        String trimmed = raw.trim();

        // Simple heuristics: prefer XPath; fall back to CSS if it looks like one
        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".//") || trimmed.startsWith("//")) {
            return By.xpath(trimmed);
        }
        // If it *looks* like CSS (starts with #, ., [attr], tag, etc.) assume CSS
        return By.cssSelector(trimmed);
    }

    private static String resolveRaw(BaseElement element, String key, Object[] args) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Locator key is null/blank for " + safeName(element));
        }

        JsonNode root = loadJsonNode(inferJsonPathFrom(element.getPropertyFile()));
        if (root == null) {
            return null; // Let caller decide whether to fall back
        }

        JsonNode match = deepFindField(root, key);
        if (match == null || !match.isTextual()) {
            return null; // Let caller decide whether to fall back
        }

        String template = match.asText();
        return applyArgs(template, args);
    }

    private static String inferJsonPathFrom(String propertiesFileName) {
        // Accept already-json names, else convert *.properties -> *.json
        String baseName = propertiesFileName == null ? "" : propertiesFileName.trim();
        if (baseName.isEmpty()) return null;

        if (baseName.endsWith(".json")) {
            return baseName.startsWith("locators/json/")
                    ? baseName
                    : "locators/json/" + baseName;
        }

        int dot = baseName.lastIndexOf('.');
        String stem = (dot > 0) ? baseName.substring(0, dot) : baseName;
        return "locators/json/" + stem + ".json";
    }

    private static JsonNode loadJsonNode(String classpath) {
        if (classpath == null || classpath.isBlank()) return null;

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath)) {
            if (in == null) return null;
            return MAPPER.readTree(in);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Depth-first search for a field with the given name anywhere in the tree.
     * This is resilient to nested structures like:
     * {
     *   "ManageUsersElements": { "UserCards": { "locator": "//..." } }
     * }
     */
    private static JsonNode deepFindField(JsonNode root, String fieldName) {
        if (root == null || fieldName == null || fieldName.isBlank()) return null;

        Deque<JsonNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            JsonNode node = stack.pop();

            if (node.has(fieldName)) {
                return node.get(fieldName);
            }

            // Explore children
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                JsonNode child = entry.getValue();
                if (child != null && (child.isObject() || child.isArray())) {
                    stack.push(child);
                }
            }

            if (node.isArray()) {
                for (JsonNode child : node) {
                    if (child != null && (child.isObject() || child.isArray())) {
                        stack.push(child);
                    }
                }
            }
        }
        return null;
    }

    private static String applyArgs(String template, Object[] args) {
        if (template == null) return null;
        // Normalize null args
        Object[] safeArgs = (args == null) ? new Object[0] : args;

        // If template has %s placeholders, use String.format
        if (template.contains("%s")) {
            return String.format(template, safeArgs);
        }
        // Otherwise, allow {0}, {1} via MessageFormat
        if (template.contains("{0")) {
            return MessageFormat.format(template, safeArgs);
        }
        return template;
    }

    private static String safeName(BaseElement e) {
        try { return ((Enum<?>) e).name(); } catch (Exception ignore) { return Objects.toString(e); }
    }
}
