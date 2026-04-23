package core.resolvers.locator.json;

import core.resolvers.locator.api.LocatorPaths;
import core.utils.ConfigLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static core.logging.CustomLogger.debug;

/**
 * Per-migration-run cache of {@code .properties} files keyed by their fully-qualified
 * classpath path (e.g. {@code "locators/login.properties"}).
 *
 * <p>Replaces the static {@code ThreadLocal<Map<String, Properties>>} that used to live in
 * {@code JsonLocatorMigrator}. Each migration run creates a fresh {@code PropertiesIndex}
 * instance, which is naturally garbage-collected when the run completes — no thread-local
 * memory leak risk and no cross-run state.</p>
 *
 * <p>Loading layers TEST classpath over MAIN classpath, mirroring the
 * {@code ElementLocatorResolverV1} contract.</p>
 *
 * <p>Not thread-safe — intended to be used by a single migration thread at a time.</p>
 */
public final class PropertiesIndex {

    private final Map<String, Properties> cache = new HashMap<>();

    /**
     * Return the merged {@link Properties} for {@code fileName} (auto-prefixed with
     * {@link LocatorPaths#PROPERTIES_BASE} if not already), loading once and caching.
     */
    public Properties get(String fileName) {
        if (fileName == null || fileName.isBlank()) return new Properties();
        String cpPath = LocatorPaths.underProperties(fileName);
        return cache.computeIfAbsent(cpPath, PropertiesIndex::loadMerged);
    }

    /** Number of distinct property files loaded so far. */
    public int size() { return cache.size(); }

    // ---- internal -----------------------------------------------------------

    private static Properties loadMerged(String cpPath) {
        debug.log("[props:load] loading " + cpPath + " (TEST+MAIN)");
        Properties test   = ConfigLoader.loadFromClasspath(cpPath, ConfigLoader.ClasspathScope.TEST);
        Properties main   = ConfigLoader.loadFromClasspath(cpPath, ConfigLoader.ClasspathScope.MAIN);
        Properties merged = ConfigLoader.merge(main, test); // TEST wins on conflict
        debug.log("[props:loaded] file=" + cpPath + " keys=" + merged.size());
        return merged;
    }
}

