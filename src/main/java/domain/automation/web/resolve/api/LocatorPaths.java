package domain.automation.web.resolve.api;

import core.utils.ConfigLoader;

/**
 * Centralised classpath base paths for locator resources.
 *
 * <p>Replaces the duplicated {@code "locators/"} string literal that used to live in
 * {@code ElementLocatorResolverV1}, {@code PropertiesFileLocatorReaderV1} and
 * {@code JsonLocatorMigrator}.</p>
 *
 * <p>Both bases are configurable at runtime via {@link ConfigLoader} keys
 * (see the {@code *_KEY} constants); defaults mirror the on-disk layout
 * {@code src/main/resources/locators/{properties,json}/}.</p>
 */
public final class LocatorPaths {

    /** ConfigLoader key for the {@code .properties} classpath base. */
    public static final String PROPERTIES_BASE_KEY     = "locator.properties.base.path";
    /** Default classpath base for {@code .properties} locator bundles. */
    public static final String PROPERTIES_BASE_DEFAULT = "locators/properties/";

    /** ConfigLoader key for the {@code .json} classpath base. */
    public static final String JSON_BASE_KEY     = "locator.json.base.path";
    /** Default classpath base for {@code .json} locator files. */
    public static final String JSON_BASE_DEFAULT = "locators/json/";

    /** Effective {@code .properties} classpath base (override via {@link #PROPERTIES_BASE_KEY}). */
    public static final String PROPERTIES_BASE = ConfigLoader.get(PROPERTIES_BASE_KEY, PROPERTIES_BASE_DEFAULT);

    /** Effective {@code .json} classpath base (override via {@link #JSON_BASE_KEY}). */
    public static final String JSON_BASE        = ConfigLoader.get(JSON_BASE_KEY, JSON_BASE_DEFAULT);

    private LocatorPaths() {}

    /** Prepend {@link #PROPERTIES_BASE} to {@code fileName} unless it is already prefixed. */
    public static String underProperties(String fileName) {
        return under(PROPERTIES_BASE, fileName);
    }

    /** Prepend {@link #JSON_BASE} to {@code fileName} unless it is already prefixed. */
    public static String underJson(String fileName) {
        return under(JSON_BASE, fileName);
    }

    /**
     * Prepend {@code base} to {@code fileName} unless the name already contains a
     * path separator, which signals that it is a self-rooted classpath path
     * (e.g. a Phase-5 conventional path such as
     * {@code "tests/demo/pages/DemoLoginPage/locators.json"}).
     */
    public static String under(String base, String fileName) {
        if (fileName == null) return null;
        return fileName.contains("/") ? fileName : base + fileName;
    }
}
