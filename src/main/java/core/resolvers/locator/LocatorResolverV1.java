// file: core/resolvers/locator/LocatorResolverV1.java
package core.resolvers.locator;

import elements.meta.ElementRole;
import elements.api.Element;
import core.resolvers.locator.json.JsonLocatorReaderV1;
import core.resolvers.locator.properties.PropertiesFileLocatorReaderV1;
import core.utils.UIContext;
import org.openqa.selenium.By;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LocatorResolverV1 {

    private LocatorResolverV1() {
        // Static utility — prevent instantiation
    }

    // ===== public API =====
    public static By getLocator(String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];

        // 1) fetch raw (hardcoded/.properties/.json)
        String template = getRawLocator(fileName, key);

        // 2) format only if template contains (indexed or unindexed) %s
        String resolved = resolveLocatorTemplate(template, args);

        // 3) record context for diagnostics (mirrors ElementLocatorResolverV1 behaviour)
        UIContext.setLastElementMeta(fileName, key, args);

        // 4) parse into By using the shared canonical parser
        return ByParser.DEFAULT.parse(resolved);
    }

    /** Resolve the primary locator for a v1 Element. */
    public static By getLocator(Element e) {
        return getBestAvailable(e);
    }

    /** Resolve a specific role for a v1 Element. */
    public static By getLocator(Element e, ElementRole role, Object... overrideArgs) {
        String file = e.getExternalFileName(); // may be null → hardcoded template
        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        String key  = requireRoleKey(roles, role, e.getDisplayText());
        Object[] args = (overrideArgs != null && overrideArgs.length > 0) ? overrideArgs : e.getArgs();
        return getLocator(file, key, args);
    }

    /** Resolve the best available locator for a v1 Element (PRIMARY → SECONDARY → first available). */
    public static By getBestAvailable(Element e, Object... overrideArgs) {
        String file = e.getExternalFileName();
        Object[] args = (overrideArgs != null && overrideArgs.length > 0) ? overrideArgs : e.getArgs();

        // 1. Prefer getPrimaryLocator()
        String key = e.getPrimaryLocator();
        if (!isBlank(key)) return getLocator(file, key, args);

        // 2. Secondary fallback
        key = e.getSecondaryLocator();
        if (!isBlank(key)) return getLocator(file, key, args);

        // 3. Last resort: first non-blank value from role map
        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        key = roles.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No locators defined for element: " + e.getDisplayText()));
        return getLocator(file, key, args);
    }

    // ===== helpers =====

    public static String getRawLocator(String fileName, String key) {
        LocatorReader reader = pickReader(fileName);
        String raw = reader.getRaw(fileName, key);
        if (raw == null) {
            throw new IllegalStateException("Locator not found (no raw template): file=" + fileName + " key=" + key);
        }
        return raw;
    }

    private static LocatorReader pickReader(String fileName) {
        if (fileName == null) return (f, k) -> k; // hardcoded: key is the template
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".properties")) return PropertiesFileLocatorReaderV1::getRaw;
        if (lower.endsWith(".json"))       return JsonLocatorReaderV1::getRaw;
        throw new IllegalArgumentException(
                "Unsupported locator file extension: " + fileName + " (expected .properties or .json, or null for hardcoded)");
    }

    /**
     * Format a template under {@link LocatorTemplate.Policy#STRICT}: supports {@code %s}/{@code %n$s},
     * throws {@link IllegalStateException} on too few args.
     */
    public static String resolveLocatorTemplate(String template, Object... args) {
        return LocatorTemplate.strict(template).format(args);
    }

    private static String requireRoleKey(Map<ElementRole, String> roles, ElementRole role, String elementName) {
        String key = (roles != null) ? roles.get(role) : null;
        if (isBlank(key)) {
            throw new IllegalStateException("Missing locator for role: " + role +
                    (elementName == null ? "" : (" (element=\"" + elementName + "\")")));
        }
        return key;
    }

    private static Map<ElementRole, String> safeRoles(Map<ElementRole, String> in) {
        if (in == null || in.isEmpty()) return new LinkedHashMap<>();
        return (in instanceof LinkedHashMap) ? in : new LinkedHashMap<>(in);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
