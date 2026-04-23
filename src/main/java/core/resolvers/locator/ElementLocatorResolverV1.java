package core.resolvers.locator;

import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolver;
import core.resolvers.locator.api.LocatorResolvers;
import core.resolvers.locator.template.LocatorTemplate;
import elements.api.*;
import com.beust.jcommander.internal.Nullable;
import core.utils.UIContext;
import org.openqa.selenium.Beta;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.Locale;

import static core.logging.CustomLogger.debug;

/**
 * Legacy static façade — preserved for backward compatibility.
 *
 * <p>All orchestration now delegates to {@link LocatorResolvers#legacyPadded()},
 * which composes {@link LocatorTemplate.Policy#PAD_LAST} with a
 * {@link LayeredPropertiesLocatorSource}. The {@code IS_HARDCODED} {@code ThreadLocal}
 * has been removed — hardcoded-mode logging now flows through
 * {@link LocatorRequest#isHardcoded()}.</p>
 *
 * @deprecated since the Phase&nbsp;3 OO refactor. New code should use {@link LocatorResolvers#legacyPadded()}
 *             (or, preferably, {@link LocatorResolvers#strict()}) directly. This class will be removed
 *             after callers have migrated.
 */
@Deprecated(forRemoval = true, since = "Phase 3 OO refactor")
public class ElementLocatorResolverV1 {

    private static LocatorResolver R() { return LocatorResolvers.legacyPadded(); }

    /* ========================================================================
     * Public API
     * ====================================================================== */

    public static By getLocator(@Nullable String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];
        UIContext.setLastElementMeta(fileName, key, args);
        try {
            return R().resolve(LocatorRequest.of(fileName, key, args));
        } catch (IllegalStateException ex) {
            // Preserve the legacy contract: missing key / unresolved template → RuntimeException
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Beta
    public static By getLocatorCaseInsensitive(@Nullable String fileName, String key, Object... args) {
        if (args == null) args = new Object[0];
        Object[] lowerArgs = Arrays.stream(args)
                .map(a -> a == null ? null : a.toString().toLowerCase(Locale.ROOT))
                .toArray();
        return getLocator(fileName, key, lowerArgs);
    }

    public static By getLocator(Element element) {
        return getLocator(element.getExternalFileName(), element.getPrimaryLocator(), element.getArgs());
    }

    @Beta
    public static By getLocatorCaseInsensitive(Element element) {
        return getLocatorCaseInsensitive(element.getExternalFileName(), element.getPrimaryLocator(), element.getArgs());
    }

    // Dropdown / Search helpers — preserved signatures
    public static By getDropdownTriggerLocator(Dropdown dropdown) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(), dropdown.getArgs());
    }

    public static By getDropdownListLocator(Dropdown dropdown) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(), dropdown.getArgs());
    }

    public static By getDropdownTriggerLocator(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getTriggerLocator(),
                dropdown.argsForIndex(dropdownIndex));
    }

    public static By getDropdownListLocator(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        return getLocator(dropdown.getExternalFileName(), dropdown.getListLocator(),
                dropdown.argsForIndex(dropdownIndex));
    }

    public static By getSearchFieldLocator(SearchField field) {
        return getLocator(field.getExternalFileName(), field.getSearchInputLocator(), field.getArgs());
    }

    public static By getSearchResultLocator(Searchable searchable, Object... resultArgs) {
        String resultKey = searchable.getSearchResultLocator();
        if (resultKey == null || resultKey.isBlank())
            throw new IllegalArgumentException("Result locator key is not set for " + searchable.getDisplayText());

        return getLocator(searchable.getExternalFileName(), resultKey, searchable.effectiveArgs(resultArgs));
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

    /** Pad-last formatter — delegates to {@link LocatorTemplate.Policy#PAD_LAST}. */
    public static String resolveLocatorTemplate(String template, Object... args) {
        if (template == null) return null;
        LocatorTemplate t = LocatorTemplate.padded(template);
        if (!t.hasPlaceholders()) return template;
        String resolved = t.format(args);
        debug.log("[LOCATOR TEMPLATE RESOLVE]", "Template", template, "Args",
                Arrays.toString(args == null ? new Object[0] : args), "Resolved", resolved);
        return resolved;
    }

    /**
     * Loads the raw locator string. Preserves legacy {@link RuntimeException} on missing key.
     */
    public static String getRawLocator(@Nullable String fileName, String key) {
        UIContext.setLastElementMeta(fileName, key, null);
        try {
            return R().rawTemplate(LocatorRequest.of(fileName, key));
        } catch (IllegalStateException ex) {
            throw new RuntimeException("Locator not found for key: " + key + " in file: " + fileName, ex);
        }
    }
}
