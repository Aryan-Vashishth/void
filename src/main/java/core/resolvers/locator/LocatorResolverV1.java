// file: core/resolvers/locator/LocatorResolverV1.java
package core.resolvers.locator;

import Elements.ElementRole;
import Elements.interfacesv1.Element;
import core.resolvers.locator.json.JsonLocatorReaderV1;
import core.resolvers.locator.properties.PropertiesFileLocatorReaderV1;
import core.utils.BaseUtils;
import org.openqa.selenium.By;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocatorResolverV1 extends BaseUtils {



    private LocatorResolverV1() {
        // prevent instantiation
        initializer();
    }

    // ===== public API =====
    public static By getLocator(String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];

        // 1) fetch raw (hardcoded/.properties/.json)
        String template = getRawLocator(fileName, key);

        // 2) format only if template contains (indexed or unindexed) %s
        String resolved = resolveLocatorTemplate(template, args);

        // 3) parse into By using the shared prefix parser
        return PropertiesFileLocatorReaderV1.toBy(resolved);
    }

    /** Resolve PRIMARY (or best available) for a v1 Element. */
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

    /** Resolve best available: PRIMARY → SECONDARY → first role present. */
    public static By getBestAvailable(Element e, Object... overrideArgs) {
        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        if (roles.isEmpty()) {
            throw new IllegalStateException("No locators defined for element: " + e.getDisplayText());
        }

        String file = e.getExternalFileName();
        Object[] args = (overrideArgs != null && overrideArgs.length > 0) ? overrideArgs : e.getArgs();

        String key = roles.get(ElementRole.PRIMARY);
        if (isBlank(key)) key = roles.get(ElementRole.SECONDARY);
        if (isBlank(key)) {
            key = roles.values().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No locators defined for element: " + e.getDisplayText()));
        }
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

    // Match %s or %1$s (we intentionally scope to 's' conversions for locator strings)
    private static final Pattern S_PLACEHOLDER = Pattern.compile("%(\\d+\\$)?s");

    /** Format when template contains (indexed or unindexed) %s; wrap format errors with context. */
    public static String resolveLocatorTemplate(String template, Object... args) {
        if (template == null) return null;
        Matcher m = S_PLACEHOLDER.matcher(template);
        if (!m.find()) return template; // no %s → return as-is
        try {
            return String.format(Locale.ROOT, template, args == null ? new Object[0] : args);
        } catch (IllegalFormatException ex) {
            String argSummary = (args == null) ? "null" : ("len=" + args.length);
            throw new IllegalStateException(
                    "Locator template format error. template=\"" + template + "\", args(" + argSummary + ")", ex);
        }
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
