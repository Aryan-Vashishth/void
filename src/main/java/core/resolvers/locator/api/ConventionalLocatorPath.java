package core.resolvers.locator.api;

/**
 * Derives the conventional classpath resource path for a page's locator repository.
 *
 * <p>Convention: the fully-qualified class name maps to a directory on the classpath,
 * and the file name is fixed ({@code locators.json} or {@code locators.properties}).
 * Example: {@code tests.demo.pages.DemoLoginPage}
 * → {@code tests/demo/pages/DemoLoginPage/locators.json}.</p>
 *
 * <p>Package inclusion prevents collisions between identically-named page classes in
 * different packages — two {@code LoginPage} classes in different packages produce
 * distinct paths.</p>
 *
 * <p>Place the file at {@code src/main/resources/<derived-path>} and the runtime will
 * discover it without any annotation or override on the element enum.</p>
 */
public final class ConventionalLocatorPath {

    /** Standard file name for JSON locator repositories at the conventional path. */
    public static final String JSON_FILE  = "locators.json";
    /** Standard file name for properties locator repositories at the conventional path. */
    public static final String PROPS_FILE = "locators.properties";

    private ConventionalLocatorPath() {}

    /**
     * Returns {@code "pkg/sub/ClassName/locators.json"} for the given class.
     * The returned path is classpath-relative and can be passed directly to
     * {@link ClassLoader#getResource(String)}.
     */
    public static String forClass(Class<?> pageClass) {
        return dirFor(pageClass) + JSON_FILE;
    }

    /**
     * Returns {@code "pkg/sub/ClassName/locators.properties"} for the given class.
     */
    public static String forClassProperties(Class<?> pageClass) {
        return dirFor(pageClass) + PROPS_FILE;
    }

    /**
     * Returns {@code "pkg/sub/ClassName/"} — the conventional directory for the given class.
     * Append a file name to build the full classpath path.
     */
    public static String dirFor(Class<?> pageClass) {
        return pageClass.getName().replace('.', '/') + "/";
    }
}
