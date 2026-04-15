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
import java.util.IllegalFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 4) parse into By using the shared prefix parser
        return PropertiesFileLocatorReaderV1.toBy(resolved);
    }

    /**
     * Resolve the primary locator for a v1 Element.
     * Uses {@link Element#getPrimaryLocator()} directly — which each sub-interface overrides
     * correctly (e.g. {@code Dropdown} → {@code getTriggerLocator()}, {@code ReadOnlyElement} →
     * {@code getTextLocator()}). This is more robust than the old roles-map fallthrough that
     * silently fell to "first entry" when no PRIMARY key was present.
     */
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

    /**
     * Resolve the best available locator for a v1 Element.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@link Element#getPrimaryLocator()} — the interface-level override always returns the
     *       semantically correct key (e.g. trigger for {@code Dropdown}, text for
     *       {@code ReadOnlyElement}).  This avoids the previous silent fallthrough that occurred
     *       when the roles map had no {@code PRIMARY} entry.</li>
     *   <li>{@link Element#getSecondaryLocator()} — used only when primary is absent.</li>
     *   <li>First entry in {@link Element#getAllLocatorRoles()} — last-resort fallback.</li>
     * </ol>
     */
    public static By getBestAvailable(Element e, Object... overrideArgs) {
        String file = e.getExternalFileName();
        Object[] args = (overrideArgs != null && overrideArgs.length > 0) ? overrideArgs : e.getArgs();

        // 1. Prefer getPrimaryLocator() — correctly overridden by every sub-interface
        String key = e.getPrimaryLocator();
        if (!isBlank(key)) return getLocator(file, key, args);

        // 2. Secondary fallback
        key = e.getSecondaryLocator();
        if (!isBlank(key)) return getLocator(file, key, args);

        // 3. Last resort: first value from role map (keeps backward-compat with custom impls)
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
