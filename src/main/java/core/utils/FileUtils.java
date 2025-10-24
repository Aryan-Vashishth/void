package core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.beust.jcommander.internal.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * FileUtils
 * -----------------------------------------------------------------------------
 * Path-safe utilities and JSON readers with classpath/file fallback.
 * - High-level: config-key driven readers with defaults.
 * - Mid/Low-level: direct classpath or file reads; InputStream variant for full control.
 */
public class FileUtils extends BaseUtils {

    public FileUtils() { initializer(); }

    // ---------------------------------------------------------------------
    // JSON mapper + common type refs
    // ---------------------------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, String>> MAP_STR_STR = new TypeReference<>() {};

    // ---------------------------------------------------------------------
    // Read strategies for locating resources
    // ---------------------------------------------------------------------
    public enum ReadMode {
        /** Try classpath first, if missing fall back to file system. */
        CLASSPATH_FIRST,
        /** Try file system first, then fall back to classpath. */
        FILE_FIRST,
        /** Only try file system; don't check classpath. */
        FILE_ONLY,
        /** Only try classpath; don't check file system. */
        CLASSPATH_ONLY
    }

    // ---------------------------------------------------------------------
    // Strict path resolvers (basePath MUST be a real directory path)
    // ---------------------------------------------------------------------
    public static Path resolvePath(@Nullable String basePath, String relativePath, @Nullable String suffix) {
        if (basePath == null) {
            throw new IllegalArgumentException("basePath cannot be null for FileUtils.resolvePath().");
        }
        Objects.requireNonNull(relativePath, "relativePath");
        String fileName = ensureExtension(relativePath, suffix);
        Path base = Paths.get(basePath);
        Path resolved = base.resolve(fileName).normalize();
        debug.log("Resolved path: base=" + base + " relative=" + relativePath + " -> " + resolved);
        return resolved;
    }

    public static File resolveFile(@Nullable String basePath, String relativePath, @Nullable String suffix) {
        return resolvePath(basePath, relativePath, suffix).toFile();
    }

    public static File resolveJson(@Nullable String basePath, String relativePath) {
        return resolveFile(basePath, relativePath, "json");
    }

    // ---------------------------------------------------------------------
    // Config-key wrappers (explicitly resolve base dir from ConfigLoader)
    // ---------------------------------------------------------------------
    public static File resolveFileFromConfigKey(String baseDirKey, String relativePath, @Nullable String suffix) {
        String baseDir = ConfigLoader.get(baseDirKey, null);
        if (baseDir == null || baseDir.isBlank()) {
            throw new IllegalArgumentException("Missing/blank config value for baseDirKey: " + baseDirKey);
        }
        return resolveFile(baseDir, relativePath, suffix);
    }

    public static File resolveJsonFromConfigKey(String baseDirKey, String relativePath) {
        return resolveFileFromConfigKey(baseDirKey, relativePath, "json");
    }

    // ---------------------------------------------------------------------
    // Classpath helpers
    // ---------------------------------------------------------------------
    public static Optional<URL> findResourceURL(String baseResourcePath, String filePath, @Nullable String suffix) {
        Objects.requireNonNull(baseResourcePath, "baseResourcePath");
        Objects.requireNonNull(filePath, "filePath");
        String name = ensureExtension(filePath, suffix);
        String resource = baseResourcePath.endsWith("/") ? baseResourcePath + name : baseResourcePath + "/" + name;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(resource);
        if (url == null) {
            debug.log("Classpath resource not found: " + resource);
            return Optional.empty();
        }
        debug.log("Classpath resource found: " + resource + " -> " + url);
        return Optional.of(url);
    }

    public static String resolveResourceAbsolutePath(String basePath, String filePath, @Nullable String suffix) {
        return findResourceURL(basePath, filePath, suffix)
                .map(url -> new File(url.getFile()).getAbsolutePath())
                .orElseThrow(() -> new RuntimeException("Resource file not found: " + basePath + "/" + filePath));
    }

    // ---------------------------------------------------------------------
    // HIGH-LEVEL: Config-key driven fallback map (existing signature)
    // ---------------------------------------------------------------------
    /**
     * Reads fallback map by looking up a config key. Uses classpath-first and
     * defaults to "fallback/account-mapping/field-type-fallbacks.json".
     */
    public static Map<String, String> loadFallbackJsonMap(String fallbackKey) {
        String configured = ConfigLoader.get(fallbackKey, "fallback/fallback.json");
        return loadFallbackJsonMap(configured, ReadMode.CLASSPATH_FIRST);
    }

    /**
     * High-level overload: supply a custom default classpath resource to use when the key is missing.
     */
    public static Map<String, String> loadFallbackJsonMap(String fallbackKey, String defaultClasspathResource) {
        String configured = ConfigLoader.get(fallbackKey, defaultClasspathResource);
        return loadFallbackJsonMap(configured, ReadMode.CLASSPATH_FIRST);
    }

    // ---------------------------------------------------------------------
    // MID-LEVEL: Choose read strategy explicitly for a given path/resource
    // ---------------------------------------------------------------------
    /**
     * Reads a fallback map from either classpath or file system based on the provided {@link ReadMode}.
     * The {@code configured} argument can be a classpath resource (e.g., "fallback/x.json") or a file path.
     */
    public static Map<String, String> loadFallbackJsonMap(String configured, ReadMode mode) {
        if (configured == null || configured.isBlank()) {
            error.log("❌ loadFallbackJsonMap called with blank configured path/resource");
            return new HashMap<>();
        }

        try {
            return switch (mode) {
                case CLASSPATH_FIRST -> {
                    Map<String, String> m = tryClasspath(configured);
                    if (!m.isEmpty()) yield m;
                    yield tryFile(configured);
                }
                case FILE_FIRST -> {
                    Map<String, String> m = tryFile(configured);
                    if (!m.isEmpty()) yield m;
                    yield tryClasspath(configured);
                }
                case FILE_ONLY -> tryFile(configured);
                case CLASSPATH_ONLY -> tryClasspath(configured);
            };
        } catch (Exception e) {
            error.log("❌ Failed to load fallback JSON (" + configured + ", mode=" + mode + "): " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ---------------------------------------------------------------------
    // LOW-LEVEL: Direct reads (file, classpath, InputStream)
    // ---------------------------------------------------------------------
    /** Low-level: read from a concrete file system path. */
    public static Map<String, String> loadFallbackJsonMap(Path file) {
        if (file == null) return new HashMap<>();
        if (!Files.exists(file)) {
            error.log("❌ Fallback file not found: " + file.toAbsolutePath());
            return new HashMap<>();
        }
        try (InputStream fis = new FileInputStream(file.toFile())) {
            Map<String, String> map = MAPPER.readValue(fis, MAP_STR_STR);
            info.log("✅ Loaded fallback field values from FILE: " + file.toAbsolutePath() + " (" + map.size() + " keys)");
            return map;
        } catch (Exception e) {
            error.log("❌ Failed reading file " + file.toAbsolutePath() + ": " + e.getMessage());
            return new HashMap<>();
        }
    }

    /** Low-level: read from an exact classpath resource. */
    public static Map<String, String> loadFallbackJsonMapFromClasspath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) return new HashMap<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is == null) {
                error.log("❌ Classpath resource not found: " + resourcePath);
                return new HashMap<>();
            }
            Map<String, String> map = MAPPER.readValue(is, MAP_STR_STR);
            info.log("✅ Loaded fallback field values from CLASSPATH: " + resourcePath + " (" + map.size() + " keys)");
            return map;
        } catch (Exception e) {
            error.log("❌ Failed reading classpath resource " + resourcePath + ": " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Lowest-level: caller supplies the InputStream and manages its lifecycle.
     * Throws to give complete control to the caller (no logging).
     */
    public static Map<String, String> loadFallbackJsonMap(InputStream in) throws Exception {
        Objects.requireNonNull(in, "in");
        return MAPPER.readValue(in, MAP_STR_STR);
    }

    // ---------------------------------------------------------------------
    // Internal helpers used by mid/high level methods
    // ---------------------------------------------------------------------
    private static Map<String, String> tryClasspath(String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(resource)) {
            if (is == null) {
                debug.log("Classpath miss: " + resource);
                return new HashMap<>();
            }
            Map<String, String> map = MAPPER.readValue(is, MAP_STR_STR);
            info.log("✅ Loaded from CLASSPATH: " + resource + " (" + map.size() + " keys)");
            return map;
        } catch (Exception e) {
            warn.log("Classpath read failed (" + resource + "): " + e.getMessage());
            return new HashMap<>();
        }
    }

    private static Map<String, String> tryFile(String configured) {
        Path p = Paths.get(configured);
        if (!Files.exists(p)) {
            debug.log("File miss: " + p.toAbsolutePath());
            return new HashMap<>();
        }
        try (InputStream fis = new FileInputStream(p.toFile())) {
            Map<String, String> map = MAPPER.readValue(fis, MAP_STR_STR);
            info.log("✅ Loaded from FILE: " + p.toAbsolutePath() + " (" + map.size() + " keys)");
            return map;
        } catch (Exception e) {
            warn.log("File read failed (" + p.toAbsolutePath() + "): " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ---------------------------------------------------------------------
    // FS utilities
    // ---------------------------------------------------------------------
    public static void ensureDirectoryExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new RuntimeException("Failed to create directories for: " + parent.getAbsolutePath());
        }
    }

    // ---------------------------------------------------------------------
    // Small internal util
    // ---------------------------------------------------------------------
    private static String ensureExtension(String name, @Nullable String suffix) {
        if (suffix == null || suffix.isBlank()) return name;
        String dotExt = "." + suffix.replaceFirst("^\\.", "");
        return name.endsWith(dotExt) ? name : (name + dotExt);
    }
}
