package Elements.interfacesv2;

/**
 * BaseElement
 * -----------------------------------------------------------------------------
 * Root contract for all UI element enums.
 * - Supports BOTH JSON and legacy .properties locator storage.
 * - New primary/secondary locator accessors replace getKey().
 * - Backward compatibility: getKey() is provided as a @Deprecated default
 *   that delegates to getPrimaryLocator().
 */
public interface BaseElement {

    /**
     * The primary locator identifier.
     * For JSON: field name or logical id (e.g., "locator", "inputLocator", "listLocator").
     * For .properties: the key name (e.g., "FILTER_BY_INPUT").
     */
    String getPrimaryLocator();

    /**
     * Optional secondary locator identifier (may be null).
     * Typical uses: triggerLocator (dropdown), searchInputLocator (search), etc.
     */
    default String getSecondaryLocator() {
        return null;
    }

    /**
     * Arguments for String.format-style substitution (%s, %d, etc.).
     * Return an empty array when no args are required.
     */
    Object[] getArgs();

    /**
     * Human-friendly label for logs and reports.
     * Default: if args exist, use the first; else fall back to primary locator id.
     */
    default String getDisplayText() {
        Object[] args = getArgs();
        return (args != null && args.length > 0 && args[0] != null)
                ? args[0].toString()
                : String.valueOf(getPrimaryLocator());
    }

    /**
     * Indicates the preferred locator source.
     * AUTO = try JSON first then fall back to .properties.
     */
    default LocatorFormat getPreferredLocatorFormat() {
        return LocatorFormat.AUTO;
    }

    /**
     * Legacy .properties file name (classpath resource). Return null if none.
     * e.g., "manage-users-elements.properties"
     */
    @Deprecated
    default String getPropertyFile() {
        return null;
    }

    /**
     * JSON file name (classpath resource). If null, the reader may derive it
     * from getPropertyFile() by swapping ".properties" → ".json".
     * You can also return a path under a base folder if you prefer.
     */
    default String getJsonFile() {
        String props = getPropertyFile();
        if (props == null) return null;
        if (props.endsWith(".properties")) {
            return props.substring(0, props.length() - ".properties".length()) + ".json";
        }
        return null;
    }

    /**
     * Optional JSON namespace/dot-path. Most readers infer from enum nesting.
     * Return null to let the reader infer.
     */
    default String getJsonNamespace() {
        return null;
    }

    /**
     * Backward-compat for older code. Delegates to getPrimaryLocator().
     */
    @Deprecated
    default String getKey() {
        return getPrimaryLocator();
    }

    /**
     * Whether a secondary locator is present.
     */
    default boolean hasSecondaryLocator() {
        String s = getSecondaryLocator();
        return s != null && !s.trim().isEmpty();
    }

    enum LocatorFormat {
        JSON,
        PROPERTIES,
        /** Try JSON first, then .properties. */
        AUTO
    }
}

