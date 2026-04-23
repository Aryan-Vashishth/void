package core.resolvers.locator;

import elements.api.*;
import com.beust.jcommander.internal.Nullable;
import core.resolvers.locator.json.JsonLocatorReaderV1;
import core.utils.ConfigLoader;
import core.utils.UIContext;
import org.openqa.selenium.Beta;
import org.openqa.selenium.By;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static core.logging.CustomLogger.*;

/// LocatorResolverV1 &mdash; legacy "behaves like old LocatorReader" façade.
/// Static API preserved for backward compatibility; parsing/formatting is now delegated to
/// {@link ByParser} and {@link LocatorTemplate} (Phase&nbsp;1 OO refactor).
public class ElementLocatorResolverV1 {

    /** Cache of merged bundles keyed by the passed fileName (e.g., "common-elements.properties"). */
    private static final Map<String, Properties> BUNDLE_CACHE = new ConcurrentHashMap<>();

    /** Thread-local marker for hardcoded template mode (used only to tweak debug log line). */
    private static final ThreadLocal<Boolean> IS_HARDCODED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /* ========================================================================
     * Public API
     * ====================================================================== */

    public static By getLocator(@Nullable String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];

        String template = getRawLocator(fileName, key);
        debug.log("[LOCATOR] Raw locator template:", "Template", template);

        debug.log("[LOCATOR] Resolving locator:",
                "Property File", fileName,
                "Key", key,
                "Args", args.length > 0 ? Arrays.toString(args) : "[]");

        UIContext.setLastElementMeta(fileName, key, args);

        String resolved = resolveLocatorTemplate(template, args);
        if (resolved == null) {
            error.log("[LOCATOR] Could not resolve locator template (missing %s or mismatch)",
                    "Key", key, "Template", template, "Args", Arrays.toString(args));
            throw new RuntimeException(buildFailureMessage("Could not resolve locator template", fileName, key, template, args));
        }

        By by = ByParser.DEFAULT.parse(resolved);

        if (!Boolean.TRUE.equals(IS_HARDCODED.get())) {
            debug.log("[LOCATOR] Final resolved locator:", "Key", key, "Resolved", resolved, "By", by.toString());
        } else {
            debug.log("[LOCATOR] Final resolved locator (HARDCODED):", "Key", key, "Resolved", resolved, "By", by.toString());
            IS_HARDCODED.set(Boolean.FALSE);
        }

        return by;
    }

    @Beta
    public static By getLocatorCaseInsensitive(@Nullable String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];
        Object[] lowerArgs = Arrays.stream(args)
                .map(a -> a == null ? null : a.toString().toLowerCase(Locale.ROOT))
                .toArray();
        return getLocator(fileName, key, lowerArgs);
    }

    // Universal, type-safe
    public static By getLocator(Element element) {
        return getLocator(element.getExternalFileName(), element.getPrimaryLocator(), element.getArgs());
    }

    @Beta
    public static By getLocatorCaseInsensitive(Element element) {
        return getLocatorCaseInsensitive(element.getExternalFileName(), element.getPrimaryLocator(), element.getArgs());
    }

    // Dropdown/Search helpers (unchanged signatures)
    public static By getDropdownTriggerLocator(Dropdown dropdown) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(), dropdown.getArgs());
    }

    public static By getDropdownListLocator(Dropdown dropdown) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(), dropdown.getArgs());
    }

    public static By getDropdownTriggerLocator(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        Object[] args = (dropdownIndex == null) ? dropdown.getArgs() : dropdown.getArgsWithIndex(dropdownIndex);
        return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(), args);
    }

    public static By getDropdownListLocator(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        Object[] args = (dropdownIndex == null) ? dropdown.getArgs() : dropdown.getArgsWithIndex(dropdownIndex);
        return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(), args);
    }

    public static By getSearchFieldLocator(SearchField field) {
        return getLocator(field.getExternalFileName(), field.getSearchInputLocator(), field.getArgs());
    }

    public static By getSearchResultLocator(Searchable searchable, Object... resultArgs) {
        String resultKey = searchable.getSearchResultLocator();
        if (resultKey == null || resultKey.isBlank())
            throw new IllegalArgumentException("Result locator key is not set for " + searchable.getDisplayText());

        Object[] args = (resultArgs != null && resultArgs.length > 0) ? resultArgs : searchable.getArgs();
        return getLocator(searchable.getExternalFileName(), resultKey, args);
    }

    public static String getLocatorTemplate(@Nullable String fileName, String key) {
        String template = getRawLocator(fileName, key);
        int n = countPlaceholders(template);
        debug.log("[LOCATOR TEMPLATE INFO]", "Key", key, "Template", template, "NumPlaceholders", n);
        return (n > 0) ? template : null;
    }

    /** Case-insensitive {@code %s}/{@code %S} count — delegates to {@link LocatorTemplate}. */
    public static int countPlaceholders(String template) {
        return LocatorTemplate.padded(template).placeholderCount();
    }

    /** Pad-last formatter — delegates to {@link LocatorTemplate} with {@link LocatorTemplate.Policy#PAD_LAST}. */
    public static String resolveLocatorTemplate(String template, Object... args) {
        if (template == null) return null;
        LocatorTemplate t = LocatorTemplate.padded(template);
        if (!t.hasPlaceholders()) return template;
        String resolved = t.format(args);
        debug.log("[LOCATOR TEMPLATE RESOLVE]", "Template", template, "Args",
                Arrays.toString(args == null ? new Object[0] : args), "Resolved", resolved);
        return resolved;
    }

    /* ========================================================================
     * Internals
     * ====================================================================== */

    /**
     * Loads the raw locator string.
     * <ul>
     *   <li>{@code fileName == null} → hardcoded template (key itself).</li>
     *   <li>{@code .json} → delegates to {@link JsonLocatorReaderV1}.</li>
     *   <li>otherwise → cached/merged bundle via {@link ConfigLoader} under {@link LocatorPaths#PROPERTIES_BASE}.</li>
     * </ul>
     */
    public static String getRawLocator(@Nullable String fileName, String key) {
        if (fileName == null) {
            IS_HARDCODED.set(Boolean.TRUE);
            return key;
        }

        UIContext.setLastElementMeta(fileName, key, null);

        // Route JSON files to the dedicated JSON reader
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            String raw = JsonLocatorReaderV1.getRaw(fileName, key);
            if (raw == null) {
                throw new RuntimeException("Locator not found for key: " + key + " in file: " + fileName);
            }
            return raw;
        }

        Properties bundle = BUNDLE_CACHE.computeIfAbsent(fileName, ElementLocatorResolverV1::loadBundleWithConfigLoader);

        String value = bundle.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Locator not found for key: " + key + " in file: " + fileName);
        }
        return value;
    }

    /**
     * Loads/merges a single locator bundle using ConfigLoader.
     */
    private static Properties loadBundleWithConfigLoader(String fileName) {
        String cpPath = LocatorPaths.underProperties(fileName);

        Properties merged = ConfigLoader.Layered.builder()
                .addClasspath(cpPath, true)     // TEST
                .addClasspath(cpPath, false)    // MAIN
                .externalOverrideKeys("locators.override", "LOCATORS_OVERRIDE")
                .allowExternalOverride(true)
                .includeSystemProperties(true)
                .includeEnvironment(true)
                .build();

        debug.log("[LOCATOR BUNDLE LOAD]",
                "FileName", fileName,
                "Classpath", cpPath,
                "Keys", String.valueOf(merged.size()));

        return merged;
    }

    private static String buildFailureMessage(String prefix,
                                              @Nullable String fileName,
                                              String key,
                                              String template,
                                              Object[] args) {
        return prefix + " [" +
                "file=" + fileName + ", " +
                "key=" + key + ", " +
                "template=" + template + ", " +
                "args=" + Arrays.toString(args) + ", " +
                "uiContext=" + UIContext.getLastElementMeta() +
                "]";
    }
}
