package core.resolvers.locator;

import Elements.interfacesv1.*;
import com.beust.jcommander.internal.Nullable;
import core.utils.BaseUtils;
import core.utils.ConfigLoader;
import core.utils.UIContext;
import org.openqa.selenium.Beta;
import org.openqa.selenium.By;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/// LocatorResolverV1 &mdash; "behaves like old LocatorReader" but uses the new v1 interfaces.
/// &#x2705; Parity with old reader:
/// - Loads raw locator templates from classpath property bundles (with caching)
/// - Supports hardcoded templates by passing fileName=null
/// - Builds By with %s substitution (repeats last arg if fewer placeholders)
/// - Case-insensitive variant lowers args (for translate() XPaths)
/// - Universal Element-based resolvers + targeted helpers for dropdown/search patterns
/// &#x2705; Enhancements:
/// - Thread-safe hardcoded flag via ThreadLocal
/// - resolveLocatorTemplate() returns original template when no placeholders
/// - Auto-detects By type from prefixes:
///      id=, name=, class=, tag=, linkText=, partialLinkText=, css=, xpath=
/// - Falls back to heuristic: starts with "/", "(", "." &rarr; XPath; otherwise CSS
public class ElementLocatorResolverV1 extends BaseUtils {

    /** Default classpath base for locator bundles. */
    private static final String CLASSPATH_BASE = "locators/";

    /** Cache of merged bundles keyed by the passed fileName (e.g., "common-elements.properties"). */
    private static final Map<String, Properties> BUNDLE_CACHE = new ConcurrentHashMap<>();

    /** Thread-local marker for hardcoded template mode. */
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

        By by = toBy(resolved);

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
        if (dropdownIndex == null)
            return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(), dropdown.getArgs());
        return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(), dropdown.getArgsWithIndex(dropdownIndex));
    }

    public static By getDropdownListLocator(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        if (dropdownIndex == null)
            return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(), dropdown.getArgs());
        return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(), dropdown.getArgsWithIndex(dropdownIndex));
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

    public static int countPlaceholders(String template) {
        if (template == null || template.isEmpty()) return 0;
        int count = 0, idx = 0;
        String lower = template.toLowerCase(Locale.ROOT);
        while ((idx = lower.indexOf("%s", idx)) != -1) {
            count++;
            idx += 2;
        }
        return count;
    }

    public static String resolveLocatorTemplate(String template, Object... args) {
        if (template == null) return null;
        int n = countPlaceholders(template);
        if (n == 0) return template;

        if (args == null) args = new Object[0];
        if (n > args.length) {
            Object[] padded = new Object[n];
            for (int i = 0; i < n; i++) padded[i] = (i < args.length) ? args[i] : args[args.length - 1];
            args = padded;
        }

        String resolved = String.format(template, args);
        debug.log("[LOCATOR TEMPLATE RESOLVE]", "Template", template, "Args", Arrays.toString(args), "Resolved", resolved);
        return resolved;
    }

    /* ========================================================================
     * Internals
     * ====================================================================== */

    /**
     * Loads the raw locator string.
     * If fileName == null → hardcoded template (key itself).
     * Otherwise → from cached/merged bundle via ConfigLoader.
     */
    public static String getRawLocator(@Nullable String fileName, String key) {
        if (fileName == null) {
            IS_HARDCODED.set(Boolean.TRUE);
            return key;
        }

        UIContext.setLastElementMeta(fileName, key, null);
        Properties bundle = BUNDLE_CACHE.computeIfAbsent(fileName, ElementLocatorResolverV1::loadBundleWithConfigLoader);

        String value = bundle.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Locator not found for key: " + key + " in file: " + fileName);
        }
        return value;
    }

    /**
     * Loads/merges a single locator bundle using ConfigLoader:
     * - Classpath TEST then MAIN: "propertiesfiles/locators/<fileName>"
     * - Optional external override file from -Dlocators.override or ENV LOCATORS_OVERRIDE
     * - System properties and ENV last
     */
    private static Properties loadBundleWithConfigLoader(String fileName) {
        String cpPath = CLASSPATH_BASE + fileName;

        Properties merged = ConfigLoader.Layered.builder()
                // Prefer TEST scope first, then MAIN (keeps your test/resources overrides clean)
                .addClasspath(cpPath, true)     // TEST
                .addClasspath(cpPath, false)    // MAIN
                // Optional external override file (set path via -Dlocators.override=/path/locators.properties or ENV)
                .externalOverrideKeys("locators.override", "LOCATORS_OVERRIDE")
                .allowExternalOverride(true)
                // System + ENV last (handy if you export individual keys for ad-hoc tweaks)
                .includeSystemProperties(true)
                .includeEnvironment(true)
                .build();

        debug.log("[LOCATOR BUNDLE LOAD]",
                "FileName", fileName,
                "Classpath", cpPath,
                "Keys", String.valueOf(merged.size()));

        return merged;
    }

    /** Convert a final locator string to Selenium By, supporting rich prefixes. */
    private static By toBy(String locator) {
        if (locator == null) throw new IllegalArgumentException("Locator is null.");

        String trimmed = locator.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.startsWith("id="))               return By.id(trimmed.substring(3));
        if (lower.startsWith("name="))             return By.name(trimmed.substring(5));
        if (lower.startsWith("class="))            return By.className(trimmed.substring(6));
        if (lower.startsWith("tag="))              return By.tagName(trimmed.substring(4));
        if (lower.startsWith("linktext="))         return By.linkText(trimmed.substring(9));
        if (lower.startsWith("partiallinktext="))  return By.partialLinkText(trimmed.substring(16));
        if (lower.startsWith("css="))              return By.cssSelector(trimmed.substring(4));
        if (lower.startsWith("xpath="))            return By.xpath(trimmed.substring(6));

        // Heuristics: XPath if it "looks" like one; else CSS
        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".")) {
            return By.xpath(trimmed);
        }
        return By.cssSelector(trimmed);
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
