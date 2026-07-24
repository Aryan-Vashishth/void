package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.resolvers.locator.api.ConventionalLocatorPath;
import elements.api.UIElement;
import elements.meta.ElementRole;

import java.util.Map;
import java.util.Properties;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.warn;

/**
 * Scans an enum class and emits one JSON entry per enum constant. For each constant
 * that implements {@link UIElement}, the constant's {@code name()} becomes the JSON key
 * and its resolved locator(s) become the value, always in nested role-object form:
 * {@code "CONSTANT_NAME" : { "ROLE" : "resolvedLocator" }}.
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

        Properties props = loadPropsFor(constants, enumClass);
        if (props != null) {
            debug.log("[enum] props name=" + enumClass.getSimpleName() + " keys=" + props.size());
        }

        int added = 0, resolved = 0, raw = 0;
        for (Object constant : constants) {
            if (!(constant instanceof UIElement element)) continue;
            String constantName = ((Enum<?>) constant).name();
            Map<ElementRole, String> roles = element.getAllLocatorRoles();

            if (roles.isEmpty()) continue;

            // Always emit as nested role object { ROLE_NAME: resolvedValue } — uniform for
            // single- and multi-role elements. Dot-path lookup in JsonNodeLookup handles both.
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

    /**
     * Load the properties bundle for the enum's enclosing page class.
     *
     * <p>Priority:</p>
     * <ol>
     *   <li>Phase 5 conventional path: {@code pkg/ClassName/locators.properties}</li>
     *   <li>Explicit {@code getExternalFileName()} — only honoured when it ends with {@code .properties}</li>
     * </ol>
     */
    private Properties loadPropsFor(Object[] constants, Class<?> enumClass) {
        // Phase 5: probe conventional properties path for the enclosing page class
        Class<?> pageClass = enumClass.getEnclosingClass();
        if (pageClass == null) pageClass = enumClass;
        String conventionalPath = ConventionalLocatorPath.forClassProperties(pageClass);
        Properties fromConventional = propertiesIndex.get(conventionalPath);
        if (fromConventional != null && !fromConventional.isEmpty()) return fromConventional;

        // Fallback: honour explicit getExternalFileName() only when it names a .properties file
        for (Object c : constants) {
            if (c instanceof UIElement e) {
                try {
                    String pf = e.getExternalFileName();
                    if (pf != null && !pf.isBlank() && pf.endsWith(".properties")) {
                        return propertiesIndex.get(pf);
                    }
                } catch (Throwable ignored) { /* tolerate misbehaving constants */ }
            }
        }
        return null;
    }
}

