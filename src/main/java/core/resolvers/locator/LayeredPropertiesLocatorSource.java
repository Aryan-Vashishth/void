package core.resolvers.locator;

import core.utils.ConfigLoader;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static core.logging.CustomLogger.debug;

/**
 * {@link LocatorSource} that loads {@code .properties} bundles via
 * {@link ConfigLoader.Layered} with the following layering (highest priority first):
 *
 * <ol>
 *   <li>System properties + environment variables</li>
 *   <li>External override file pointed to by {@code -Dlocators.override} or
 *       {@code LOCATORS_OVERRIDE} env var</li>
 *   <li>Test-classpath {@code locators/&lt;file&gt;}</li>
 *   <li>Main-classpath {@code locators/&lt;file&gt;}</li>
 * </ol>
 *
 * <p>Bundles are cached per {@code fileName}. This source is the layered counterpart
 * to {@link PropertiesLocatorSource} (which uses a simple, uncached
 * {@code PropertiesReader} lookup) and was extracted from the previous
 * inline {@code loadBundleWithConfigLoader} method on {@code ElementLocatorResolverV1}.</p>
 */
public final class LayeredPropertiesLocatorSource implements LocatorSource {

    /** Singleton — backing cache is internal. */
    public static final LayeredPropertiesLocatorSource INSTANCE = new LayeredPropertiesLocatorSource();

    private final Map<String, Properties> bundleCache = new ConcurrentHashMap<>();

    private LayeredPropertiesLocatorSource() {}

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".properties");
    }

    @Override
    public String readRaw(LocatorRequest request) {
        if (!supports(request.fileName())) {
            throw new IllegalArgumentException(
                    "LayeredPropertiesLocatorSource does not support fileName: " + request.fileName());
        }
        Properties bundle = bundleCache.computeIfAbsent(request.fileName(), this::loadBundle);
        return bundle.getProperty(request.key()); // null when missing
    }

    @Override
    public String name() { return "properties (layered)"; }

    /** Visible for tests: clear the bundle cache. */
    public void clearCache() { bundleCache.clear(); }

    // ---- internal -----------------------------------------------------------

    private Properties loadBundle(String fileName) {
        String cpPath = LocatorPaths.underProperties(fileName);

        Properties merged = ConfigLoader.Layered.builder()
                .addClasspath(cpPath, true)     // TEST scope first
                .addClasspath(cpPath, false)    // MAIN scope
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
}

