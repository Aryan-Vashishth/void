package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import elements.api.Element;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.warn;

/**
 * Pure reflection helper that scans an enum class for {@code getXxxLocator()} methods,
 * resolves their values against an optional external {@code .properties} bundle, and writes
 * the resulting {@code key → value} pairs into a Jackson {@link ObjectNode}.
 *
 * <p>Extracted from the monolithic {@code JsonLocatorMigrator.writeEnumMethodLocators}
 * to follow the Single Responsibility Principle: this class knows only about enum
 * reflection and key/value emission; class-tree recursion lives in {@link JsonTreeBuilder},
 * properties caching lives in {@link PropertiesIndex}.</p>
 *
 * <p>Discovery rule for "locator method": no parameters, returns {@link String}, name starts
 * with {@code get} and ends with {@code Locator}. The JSON key is the middle portion
 * decapitalised (e.g. {@code getInputLocator → "inputLocator"}).</p>
 */
public final class EnumLocatorScanner {

    private final PropertiesIndex propertiesIndex;

    public EnumLocatorScanner(PropertiesIndex propertiesIndex) {
        this.propertiesIndex = propertiesIndex;
    }

    /**
     * Append discovered {@code locatorKey → resolvedValue} entries to {@code into}.
     *
     * @return number of entries added
     */
    public int writeInto(ObjectNode into, Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            warn.log("[enum] empty " + enumClass.getSimpleName());
            return 0;
        }
        Object target = Arrays.stream(constants)
                .filter(c -> c instanceof Element)
                .findFirst()
                .orElse(constants[0]);

        Properties props = loadPropsFor(constants);
        if (props != null) {
            debug.log("[enum] props name=" + enumClass.getSimpleName() + " keys=" + props.size());
        }

        int added = 0, resolved = 0, raw = 0;
        for (Method m : enumClass.getDeclaredMethods()) {
            if (!isLocatorMethod(m)) continue;
            String keyName = decapitalize(m.getName().substring(3));
            try {
                m.setAccessible(true);
                Object rawVal = m.invoke(target);
                if (!(rawVal instanceof String val) || val.isBlank()) continue;

                String resolvedVal = (props == null) ? null : props.getProperty(val.trim());
                String finalVal = (resolvedVal != null && !resolvedVal.isBlank()) ? resolvedVal.trim() : val;
                into.put(keyName, finalVal);
                added++;
                if (resolvedVal != null && !resolvedVal.isBlank()) resolved++;
                else                                              raw++;
            } catch (ReflectiveOperationException e) {
                warn.log("[enum] reflectFail enum=" + enumClass.getSimpleName()
                        + " method=" + m.getName() + " msg=" + e.getMessage());
            }
        }
        debug.log("[enum] name=" + enumClass.getSimpleName()
                + " added=" + added + " resolved=" + resolved + " raw=" + raw
                + " fields=" + into.size());
        return added;
    }

    // ---- internal -----------------------------------------------------------

    /** Locator method: {@code public String getXxxLocator()} with no parameters. */
    private static boolean isLocatorMethod(Method m) {
        String name = m.getName();
        return name.startsWith("get")
                && name.endsWith("Locator")
                && m.getParameterCount() == 0
                && String.class.equals(m.getReturnType());
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

    private static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}

