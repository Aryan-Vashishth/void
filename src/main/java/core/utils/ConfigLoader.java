package core.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ConfigLoader (generic)
 * -----------------------------------------------------------------------------
 * A small, static utility for reading, writing, and merging {@code .properties}
 * anywhere in your project—without coupling to any specific domain (e.g., WebDriver).
 *
 * <p><strong>Highlights</strong></p>
 * <ul>
 *   <li>{@link #loadFromClasspath(String)} / {@link #loadFromFile(Path)}</li>
 *   <li>{@link #merge(Properties...)} with "last wins"</li>
 *   <li>{@link Layered.Builder} to compose classpath &amp; file resources, an optional
 *       external override file, System properties, and mapped ENV variables</li>
 *   <li>Temp config helpers: push/pull/clear single or multiple keys for any {@link Path}</li>
 *   <li>Template writer for arbitrary keys via {@link #createTemplate(Path, Collection, Properties, boolean, boolean)}</li>
 * </ul>
 *
 * <p><strong>Usage (layered)</strong></p>
 * <pre>
 * Properties p = ConfigLoader.Layered.builder()
 *     .addClasspath("config/driver.properties", true)   // test scope
 *     .addClasspath("config/driver.properties", false)  // main scope fallback
 *     .externalOverrideKeys("config.file", "CONFIG_FILE")
 *     .includeSystemProperties(true)
 *     .includeEnvironment(true)
 *     .build();
 * </pre>
 */
public final class ConfigLoader {

    private static final Logger LOG = Logger.getLogger(ConfigLoader.class.getName());

    private ConfigLoader() { }

    // -------------------------------------------------------------------------
    // Simple load / write
    // -------------------------------------------------------------------------

    /**
     * Loads a properties file from the classpath (ANY scope: first match wins).
     *
     * @param resourcePath classpath resource path (e.g., {@code "config/app.properties"})
     * @return a non-null {@link Properties} (empty if not found or on error)
     */
    public static Properties loadFromClasspath(String resourcePath) {
        return loadFromClasspath(resourcePath, ClasspathScope.ANY);
    }

    /**
     * Loads a properties file from the classpath with an explicit scope preference.
     * <ul>
     *   <li>TEST → prefers a URL containing {@code "/test-classes/"}.</li>
     *   <li>MAIN → prefers a URL <em>not</em> containing {@code "/test-classes/"}.</li>
     *   <li>ANY  → first match returned by the classloader.</li>
     * </ul>
     * If the preferred match is not found, gracefully falls back to ANY (first available).
     *
     * @param resourcePath classpath resource path
     * @param scope        desired scope (TEST, MAIN, ANY)
     * @return a non-null {@link Properties} (empty if not found or on error)
     */
    public static Properties loadFromClasspath(String resourcePath, ClasspathScope scope) {
        Properties p = new Properties();
        if (resourcePath == null || resourcePath.isBlank()) return p;

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            if (scope == ClasspathScope.ANY) {
                try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                    if (in != null) {
                        p.load(in);
                        logFine("Loaded classpath properties (ANY): " + resourcePath + " (" + p.size() + " keys)");
                    } else {
                        logFine("Classpath properties not found (ANY): " + resourcePath);
                    }
                }
                return p;
            }

            // Enumerate all matches to pick a scoped one.
            Enumeration<URL> urls = cl.getResources(resourcePath);
            URL chosen = null;
            List<URL> all = new ArrayList<>();
            while (urls.hasMoreElements()) {
                URL u = urls.nextElement();
                all.add(u);
            }

            if (!all.isEmpty()) {
                String want = "/test-classes/";
                if (scope == ClasspathScope.TEST) {
                    for (URL u : all) {
                        if (urlLooksLikeTestClasses(u, want)) {
                            chosen = u; break;
                        }
                    }
                } else if (scope == ClasspathScope.MAIN) {
                    for (URL u : all) {
                        if (!urlLooksLikeTestClasses(u, want)) {
                            chosen = u; break;
                        }
                    }
                }

                // Fallback if exact scoped match wasn't found
                if (chosen == null) chosen = all.get(0);
            }

            if (chosen != null) {
                try (InputStream in = chosen.openStream()) {
                    p.load(in);
                }
                logFine("Loaded classpath properties (" + scope + "): " + resourcePath +
                        " from " + chosen + " (" + p.size() + " keys)");
            } else {
                logFine("Classpath properties not found (" + scope + "): " + resourcePath);
            }
        } catch (Exception e) {
            logWarn("Failed to read classpath (" + scope + ") " + resourcePath + " :: " + e.getMessage());
        }
        return p;
    }

    private static boolean urlLooksLikeTestClasses(URL u, String marker) {
        if (u == null) return false;
        String s = String.valueOf(u);
        return s.contains(marker) || s.contains("\\test-classes\\");
    }

    /**
     * Loads a properties file from the filesystem.
     *
     * @param path file path
     * @return a non-null {@link Properties} (empty if not found or on error)
     */
    public static Properties loadFromFile(Path path) {
        Properties p = new Properties();
        try {
            if (path != null && Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    p.load(in);
                }
                logFine("Loaded properties file: " + path + " (" + p.size() + " keys)");
            } else {
                logFine("Properties file not found: " + path);
            }
        } catch (Exception e) {
            logWarn("Failed to read properties file " + path + " :: " + e.getMessage());
        }
        return p;
    }

    /**
     * Writes {@link Properties} to a file. Parent directories are created if missing.
     *
     * @param p       properties to write (null safe)
     * @param path    output path (required)
     * @param comment optional file comment
     * @throws IllegalStateException if writing fails
     */
    public static void writeToFile(Properties p, Path path, String comment) {
        Objects.requireNonNull(path, "path");
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                (p == null ? new Properties() : p).store(out, (comment == null ? "" : comment));
            }
            logInfo("Wrote properties to: " + path.toAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write properties to: " + path, e);
        }
    }

    // -------------------------------------------------------------------------
    // Merge
    // -------------------------------------------------------------------------

    /**
     * Merges a list of {@link Properties} from left to right.
     * Later sources override earlier ones for the same key (i.e., "last wins").
     *
     * @param sources list of sources (nulls are skipped)
     * @return merged {@link Properties}
     */
    public static Properties merge(List<Properties> sources) {
        Properties out = new Properties();
        for (Properties src : sources) {
            if (src == null) continue;
            for (String k : src.stringPropertyNames()) {
                String v = src.getProperty(k);
                if (v != null && !v.isBlank()) out.setProperty(k, v);
            }
        }
        return out;
    }

    /** Convenience varargs wrapper for {@link #merge(List)} with "last wins". */
    public static Properties merge(Properties... sources) {
        return merge(Arrays.asList(sources));
    }

    // -------------------------------------------------------------------------
    // Active (optional) global config + convenience getters
    // -------------------------------------------------------------------------

    /** Optional global "active" properties storage. */
    private static final Properties ACTIVE = new Properties();

    /** Replaces the active config atomically. */
    public static synchronized void setActive(Properties p) {
        ACTIVE.clear();
        if (p != null) {
            for (String k : p.stringPropertyNames()) {
                String v = p.getProperty(k);
                if (v != null) ACTIVE.setProperty(k, v);
            }
        }
    }

    /** Reads a key from ACTIVE → System properties → ENV (UPPER_SNAKE of key). */
    public static String get(String key) {
        String v = get(key, null);
        if (v == null) {
            throw new IllegalArgumentException("Config key not found: " + key);
        }
        return v;
    }

    /** Reads a key from ACTIVE → System properties → ENV (UPPER_SNAKE of key). */
    public static String get(String key, String defaultValue) {
        if (key == null || key.isBlank()) return defaultValue;

        // 1) ACTIVE
        String v = ACTIVE.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();

        // 2) System properties
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();

        // 3) ENV (map "some.key.name" -> "SOME_KEY_NAME")
        String envKey = key.toUpperCase(java.util.Locale.ROOT).replace('.', '_');
        v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v.trim();

        // 4) default
        return defaultValue;
    }

    /** Shallow clone (String keys/values only). */
    public static Properties cloneOf(Properties p) {
        Properties c = new Properties();
        if (p == null) return c;
        for (String k : p.stringPropertyNames()) {
            String v = p.getProperty(k);
            if (v != null) c.setProperty(k, v);
        }
        return c;
    }

    // -------------------------------------------------------------------------
    // Layered builder
    // -------------------------------------------------------------------------

    /**
     * Classpath scope preference when loading duplicate resource paths.
     */
    public enum ClasspathScope {
        TEST, MAIN, ANY
    }

    /**
     * Builds layered properties: classpath → files → optional external override → System → ENV (mapped).
     * Missing items are skipped gracefully; "last wins".
     */
    public static final class Layered {

        private Layered() { }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private static final class CpEntry {
                final String path;
                final ClasspathScope scope;
                CpEntry(String path, ClasspathScope scope) { this.path = path; this.scope = scope; }
            }

            private final List<CpEntry> classpathResources = new ArrayList<>();
            private final List<Path> files = new ArrayList<>();
            private final List<Properties> extraProps = new ArrayList<>();

            private String externalSystemPropertyKey = "config.file";
            private String externalEnvVarKey = "CONFIG_FILE";
            private boolean allowExternalOverride = true;

            private boolean includeSystemProperties = true;
            private boolean includeEnvironment = true;

            /** ENV→property mapping (explicit). */
            private final Map<String, String> envMap = new LinkedHashMap<>();

            /** Optional dynamic ENV mapper. */
            private BiFunction<String, String, String> environmentMapper;

            /** Adds a classpath resource (ANY scope). */
            public Builder addClasspath(String resourcePath) {
                if (resourcePath != null && !resourcePath.isBlank())
                    classpathResources.add(new CpEntry(resourcePath, ClasspathScope.ANY));
                return this;
            }

            /**
             * Adds a classpath resource with explicit scope.
             * @param resourcePath classpath path (e.g., "config/driver.properties")
             * @param testScope    true → TEST scope; false → MAIN scope
             */
            public Builder addClasspath(String resourcePath, boolean testScope) {
                if (resourcePath != null && !resourcePath.isBlank()) {
                    classpathResources.add(new CpEntry(resourcePath, testScope ? ClasspathScope.TEST : ClasspathScope.MAIN));
                }
                return this;
            }

            /** Adds a classpath resource with explicit {@link ClasspathScope}. */
            public Builder addClasspath(String resourcePath, ClasspathScope scope) {
                if (resourcePath != null && !resourcePath.isBlank()) {
                    classpathResources.add(new CpEntry(resourcePath, scope == null ? ClasspathScope.ANY : scope));
                }
                return this;
            }

            /** Adds a file path (e.g., {@code Paths.get("config","driver-local.properties")}). */
            public Builder addFile(Path path) {
                if (path != null) files.add(path);
                return this;
            }

            /** Adds an already-loaded Properties layer (applied after classpath/files but before System/ENV). */
            public Builder addProperties(Properties p) {
                if (p != null && !p.isEmpty()) extraProps.add(p);
                return this;
            }

            /** Configures keys for an optional external override file (e.g., {@code -Dconfig.file} or ENV). */
            public Builder externalOverrideKeys(String systemPropertyKey, String envVarKey) {
                this.externalSystemPropertyKey = systemPropertyKey;
                this.externalEnvVarKey = envVarKey;
                return this;
            }

            /** Enables/disables reading the external override file (default: {@code true}). */
            public Builder allowExternalOverride(boolean allow) {
                this.allowExternalOverride = allow;
                return this;
            }

            public Builder includeSystemProperties(boolean include) {
                this.includeSystemProperties = include;
                return this;
            }

            public Builder includeEnvironment(boolean include) {
                this.includeEnvironment = include;
                return this;
            }

            /** Explicitly maps an ENV variable to a property key (e.g., {@code mapEnv("BROWSER","browser")}). */
            public Builder mapEnv(String envKey, String propertyKey) {
                if (envKey != null && propertyKey != null && !propertyKey.isBlank()) {
                    envMap.put(envKey, propertyKey);
                }
                return this;
            }

            /** Bulk map ENV variables to property keys. */
            public Builder mapEnv(Map<String, String> mappings) {
                if (mappings != null) envMap.putAll(mappings);
                return this;
            }

            /**
             * Provides a function to map ENV keys dynamically.
             * Return the desired property key for (envKey, envValue) or {@code null} to skip that env.
             */
            public Builder environmentMapper(BiFunction<String, String, String> mapper) {
                this.environmentMapper = mapper;
                return this;
            }

            /**
             * Builds the merged {@link Properties}. Order:
             * <ol>
             *   <li>Classpath resources (in the exact order added; each loaded with its specified scope)</li>
             *   <li>Filesystem files</li>
             *   <li>Extra {@link Properties}</li>
             *   <li>External override (optional)</li>
             *   <li>System properties (optional)</li>
             *   <li>Environment variables mapped to properties (optional)</li>
             * </ol>
             * "Last wins" on key collisions.
             */
            public Properties build() {
                List<Properties> layers = new ArrayList<>();

                // 1) classpath resources (respect scope per entry)
                for (CpEntry cp : classpathResources) {
                    layers.add(loadFromClasspath(cp.path, cp.scope));
                }

                // 2) filesystem files
                for (Path f : files) layers.add(loadFromFile(f));

                // 3) extra Properties explicitly added
                layers.addAll(extraProps);

                // 4) optional external override file
                if (allowExternalOverride) {
                    Path ext = resolveExternalPath(externalSystemPropertyKey, externalEnvVarKey);
                    if (ext != null) layers.add(loadFromFile(ext));
                }

                // 5) System properties
                if (includeSystemProperties) layers.add(System.getProperties());

                // 6) Environment variables → mapped Properties
                if (includeEnvironment) layers.add(environmentAsProperties(envMap, environmentMapper));

                return merge(layers);
            }

            private Path resolveExternalPath(String sysPropKey, String envKey) {
                String viaSys = (sysPropKey == null) ? null : System.getProperty(sysPropKey);
                String viaEnv = (envKey == null) ? null : System.getenv(envKey);
                String first = firstNonBlank(viaSys, viaEnv);
                if (first == null || first.isBlank()) return null;
                Path p = Paths.get(first);
                if (!Files.exists(p)) {
                    logWarn("External override path not found: " + p);
                }
                return p;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Environment mapping (generic)
    // -------------------------------------------------------------------------

    /**
     * Converts ENV variables to {@link Properties} using an explicit mapping and/or a dynamic mapper.
     * The explicit mapping copies exact ENV keys to given property keys. The mapper can transform
     * any subset of remaining (or all) ENV keys to property keys.
     *
     * @param explicitMapping ENV key → target property key
     * @param mapper          optional (envKey, envValue) → target property key (return null to skip)
     * @return mapped {@link Properties}
     */
    public static Properties environmentAsProperties(Map<String, String> explicitMapping,
                                                     BiFunction<String, String, String> mapper) {
        Properties out = new Properties();
        Map<String, String> env = System.getenv();

        // 1) explicit mappings (exact keys only)
        if (explicitMapping != null && !explicitMapping.isEmpty()) {
            for (Map.Entry<String, String> e : explicitMapping.entrySet()) {
                String envKey = e.getKey();
                String propKey = e.getValue();
                if (envKey == null || propKey == null || propKey.isBlank()) continue;
                String val = env.get(envKey);
                if (val != null && !val.isBlank()) out.setProperty(propKey, val.trim());
            }
        }

        // 2) dynamic mapper (you can filter inside your mapper)
        if (mapper != null) {
            for (Map.Entry<String, String> e : env.entrySet()) {
                String envKey = e.getKey();
                String envVal = e.getValue();
                String targetProp = mapper.apply(envKey, envVal);
                if (targetProp != null && !targetProp.isBlank() && envVal != null && !envVal.isBlank()) {
                    out.setProperty(targetProp, envVal.trim());
                }
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Temp config helpers (generic; you pass the path)
    // -------------------------------------------------------------------------

    /** Loads only the temp config at a path (no layering). */
    public static Properties tempLoadOnly(Path tempPath) { return loadFromFile(tempPath); }

    /** Returns {@code base} overlaid with the temp file (temp wins on conflicts). */
    public static Properties tempOverlay(Properties base, Path tempPath) {
        return merge(base == null ? new Properties() : base, tempLoadOnly(tempPath));
    }

    /** Reads a single key from the temp config. */
    public static String tempPull(Path tempPath, String key) {
        return tempLoadOnly(tempPath).getProperty(key);
    }

    /** Reads all keys from the temp config. */
    public static Properties tempPullAll(Path tempPath) {
        return tempLoadOnly(tempPath);
    }

    /**
     * Writes/updates a single key in the temp config. Passing {@code null} removes the key.
     * Parent directories are created on first write.
     */
    public static synchronized void tempPush(Path tempPath, String key, String value) {
        Objects.requireNonNull(tempPath, "tempPath");
        Objects.requireNonNull(key, "key");
        Properties t = tempLoadOnly(tempPath);
        if (value == null) t.remove(key);
        else t.setProperty(key, value);
        writeToFile(t, tempPath, "temp (single update)");
    }

    /**
     * Merges multiple entries into the temp config, or overwrites entirely.
     *
     * @param tempPath     file path for temp config
     * @param updates      properties to merge/write
     * @param overwriteAll if true, replaces file contents with {@code updates}; otherwise merges
     */
    public static synchronized void tempPushAll(Path tempPath, Properties updates, boolean overwriteAll) {
        Objects.requireNonNull(tempPath, "tempPath");
        if (updates == null) return;
        Properties toWrite = overwriteAll ? cloneOf(updates) : merge(tempLoadOnly(tempPath), updates);
        writeToFile(toWrite, tempPath, overwriteAll ? "temp (overwrite)" : "temp (merge)");
    }

    /** Removes a key from the temp config (no-op if absent). */
    public static synchronized void tempDeleteKey(Path tempPath, String key) {
        Objects.requireNonNull(tempPath, "tempPath");
        Properties t = tempLoadOnly(tempPath);
        if (t.containsKey(key)) {
            t.remove(key);
            writeToFile(t, tempPath, "temp (delete key)");
        }
    }

    /** Clears the temp config (file becomes an empty properties file). */
    public static synchronized void tempClear(Path tempPath) {
        Objects.requireNonNull(tempPath, "tempPath");
        writeToFile(new Properties(), tempPath, "temp (clear)");
    }

    // -------------------------------------------------------------------------
    // Template writer (generic)
    // -------------------------------------------------------------------------

    /**
     * Creates a {@code .properties} template at the given path with the provided keys
     * and optional example values. Keys are written in order; if a key has an example,
     * an {@code # e.g. key=value} line is placed beneath it.
     */
    public static Path createTemplate(Path path,
                                      Collection<String> keys,
                                      Properties exampleValues,
                                      boolean includeHeader,
                                      boolean overwrite) {
        Objects.requireNonNull(path, "path");
        try {
            if (!overwrite && Files.exists(path)) {
                throw new IllegalStateException("Template not created; file already exists: " + path);
            }
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            String nl = System.lineSeparator();
            StringBuilder sb = new StringBuilder(1024);
            if (includeHeader) {
                sb.append("# Properties Template").append(nl)
                        .append("# Generated: ").append(new Date()).append(nl)
                        .append("# Fill or override keys as needed. Empty values fall back to defaults if your code supports it.")
                        .append(nl).append(nl);
            }
            if (keys != null) {
                for (String k : keys) {
                    if (k == null || k.isBlank()) continue;
                    sb.append(k).append("=").append(nl);
                    if (exampleValues != null) {
                        String ex = exampleValues.getProperty(k);
                        if (ex != null && !ex.isBlank()) {
                            sb.append("# e.g. ").append(k).append("=").append(ex).append(nl);
                        }
                    }
                    sb.append(nl);
                }
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logInfo("Template written: " + path.toAbsolutePath());
            return path;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write template: " + path, e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void logFine(String msg) { LOG.log(Level.FINE, msg); }
    private static void logInfo(String msg) { LOG.log(Level.INFO, msg); }
    private static void logWarn(String msg) { LOG.log(Level.WARNING, msg); }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
