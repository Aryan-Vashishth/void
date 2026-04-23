package core.resolvers.locator;

/**
 * Centralised classpath base paths for locator resources.
 *
 * <p>Replaces the duplicated {@code "locators/"} string literal that used to live in
 * {@code ElementLocatorResolverV1}, {@code PropertiesFileLocatorReaderV1} and
 * {@code JsonLocatorMigrator}.</p>
 */
public final class LocatorPaths {

    /** Classpath base for {@code .properties} locator bundles. */
    public static final String PROPERTIES_BASE = "locators/";

    /** Default classpath base for {@code .json} locator files. */
    public static final String JSON_BASE_DEFAULT = "locators/json/";

    private LocatorPaths() {}

    /** Prepend {@link #PROPERTIES_BASE} to {@code fileName} unless it is already prefixed. */
    public static String underProperties(String fileName) {
        if (fileName == null) return null;
        return fileName.startsWith(PROPERTIES_BASE) ? fileName : PROPERTIES_BASE + fileName;
    }

    /** Prepend {@code base} to {@code fileName} unless it is already prefixed. */
    public static String under(String base, String fileName) {
        if (fileName == null) return null;
        return fileName.startsWith(base) ? fileName : base + fileName;
    }
}

