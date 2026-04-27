package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import elements.api.Element;
import elements.meta.ElementRole;

import java.util.Map;
import java.util.Properties;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.warn;

/**
 * Scans an enum class and emits one JSON entry per enum constant. For each constant
 * that implements {@link Element}, the constant's {@code name()} becomes the JSON key
 * and its resolved locator(s) become the value.
 *
 * <ul>
 *   <li><b>Single-role</b> element (e.g. {@code ReadOnlyElement}, {@code Clickable}):
 *       emitted as {@code "CONSTANT_NAME" : "resolvedLocator"}.</li>
 *   <li><b>Multi-role</b> element (e.g. {@code Dropdown}):
 *       emitted as {@code "CONSTANT_NAME" : { "TRIGGER" : "…", "LIST" : "…" }}.</li>
 * </ul>
 *
 * <p>Locator values are resolved against the external {@code .properties} bundle
 * referenced by the element (if any). Class-tree recursion lives in
 * {@link JsonTreeBuilder}; properties caching lives in {@link PropertiesIndex}.</p>
 */
public final class EnumLocatorScanner {

    private final PropertiesIndex propertiesIndex;

    public EnumLocatorScanner(PropertiesIndex propertiesIndex) {
        this.propertiesIndex = propertiesIndex;
    }

    /**
     * Append discovered {@code constantName → resolvedLocator} entries to {@code into}.
     *
     * @return number of entries added
     */
    public int writeInto(ObjectNode into, Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            warn.log("[enum] empty " + enumClass.getSimpleName());
            return 0;
        }

        Properties props = loadPropsFor(constants);
        if (props != null) {
            debug.log("[enum] props name=" + enumClass.getSimpleName() + " keys=" + props.size());
        }

        int added = 0, resolved = 0, raw = 0;
        for (Object constant : constants) {
            if (!(constant instanceof Element element)) continue;
            String constantName = ((Enum<?>) constant).name();
            Map<ElementRole, String> roles = element.getAllLocatorRoles();

            if (roles.isEmpty()) continue;

            if (roles.size() == 1) {
                // Single-role: emit as simple string value
                String rawVal = roles.values().iterator().next();
                if (rawVal == null || rawVal.isBlank()) continue;
                String resolvedVal = resolve(props, rawVal);
                into.put(constantName, resolvedVal);
                added++;
                if (!resolvedVal.equals(rawVal)) resolved++; else raw++;
            } else {
                // Multi-role: emit as nested object { ROLE_NAME: resolvedValue }
                ObjectNode rolesNode = into.putObject(constantName);
                for (Map.Entry<ElementRole, String> entry : roles.entrySet()) {
                    String rawVal = entry.getValue();
                    if (rawVal == null || rawVal.isBlank()) continue;
                    String resolvedVal = resolve(props, rawVal);
                    rolesNode.put(entry.getKey().name(), resolvedVal);
                    added++;
                    if (!resolvedVal.equals(rawVal)) resolved++; else raw++;
                }
            }
        }
        debug.log("[enum] name=" + enumClass.getSimpleName()
                + " added=" + added + " resolved=" + resolved + " raw=" + raw
                + " fields=" + into.size());
        return added;
    }

    // ---- internal -----------------------------------------------------------

    /** Resolve a raw locator key against the properties bundle; return as-is if no match. */
    private static String resolve(Properties props, String rawVal) {
        if (props == null) return rawVal;
        String resolved = props.getProperty(rawVal.trim());
        return (resolved != null && !resolved.isBlank()) ? resolved.trim() : rawVal;
    }

    /** Find first {@link Element} constant with a non-blank external file name and load its props. */
    private Properties loadPropsFor(Object[] constants) {
        for (Object c : constants) {
            if (c instanceof Element e) {
                try {
                    String pf = e.getExternalFileName();
                    if (pf != null && !pf.isBlank()) return propertiesIndex.get(pf);
                } catch (Throwable ignored) { /* tolerate misbehaving constants */ }
            }
        }
        return null;
    }
}

