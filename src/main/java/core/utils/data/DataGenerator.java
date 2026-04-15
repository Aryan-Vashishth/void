package core.utils.data;

import core.utils.io.FileUtils;
import core.utils.io.json.JsonLogger;
import elements.api.ResolvableEnum;
import com.github.javafaker.Faker;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import java.nio.file.Paths;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;

public class DataGenerator {

    private DataGenerator() { /* static utility */ }

    private static final Faker faker = new Faker();

    public enum FieldType implements ResolvableEnum {
        PROGRAM_NAME,
        OPPORTUNITY_NAME,
        OPPORTUNITY_DESCRIPTION,
        ACCOUNT_NAME,
        WEBSITE,
        COUNTRY,
        HQ_STATE_PROVINCE,
        INCENTIVE_TYPE,
        INCENTIVE_AMOUNT,
        EXPIRATION_DATE,
        DUNS_TAX_ID_NUMBER,
        NOTES,
        // Generic types (fallbacks)
        TEXT,
        NAME,
        COMPANY,
        EMAIL,
        NUMBER,
        CURRENCY,
        DATE,
        STATE,
        INCENTIVE_AMOUNT_IN_USD,
        ID
    }
    private static final Map<FieldType, DataSupplier> fieldTypeGenerators = new EnumMap<>(FieldType.class);

    static {
        fieldTypeGenerators.put(FieldType.PROGRAM_NAME, () -> faker.name().name());
        fieldTypeGenerators.put(FieldType.OPPORTUNITY_NAME, () -> faker.commerce().productName());
        fieldTypeGenerators.put(FieldType.OPPORTUNITY_DESCRIPTION, () -> faker.chuckNorris().fact());
        fieldTypeGenerators.put(FieldType.ACCOUNT_NAME, () -> faker.company().name().replace("'", ""));
        fieldTypeGenerators.put(FieldType.WEBSITE, () -> faker.internet().url());
        fieldTypeGenerators.put(FieldType.COUNTRY, () -> "United States");
        fieldTypeGenerators.put(FieldType.HQ_STATE_PROVINCE, () -> "Washington");
        fieldTypeGenerators.put(FieldType.INCENTIVE_TYPE, () -> faker.country().currency());
        fieldTypeGenerators.put(FieldType.INCENTIVE_AMOUNT_IN_USD, () -> String.valueOf(faker.number().numberBetween(100, 100000)));
        fieldTypeGenerators.put(FieldType.INCENTIVE_AMOUNT, () -> String.valueOf(faker.number().numberBetween(100, 100000)));
        fieldTypeGenerators.put(FieldType.EXPIRATION_DATE, () -> new SimpleDateFormat("MM/dd/yyyy").format(faker.date().future(120, TimeUnit.DAYS)));
        fieldTypeGenerators.put(FieldType.DUNS_TAX_ID_NUMBER, () -> faker.idNumber().valid());
        fieldTypeGenerators.put(FieldType.NOTES, () -> faker.chuckNorris().fact());

        fieldTypeGenerators.put(FieldType.TEXT, () -> faker.chuckNorris().fact());
        fieldTypeGenerators.put(FieldType.NAME, () -> faker.name().fullName());
        fieldTypeGenerators.put(FieldType.COMPANY, () -> faker.company().name().replace("'", ""));
        fieldTypeGenerators.put(FieldType.EMAIL, () -> faker.internet().emailAddress());
        fieldTypeGenerators.put(FieldType.NUMBER, () -> String.valueOf(faker.number().numberBetween(1, 10000)));
        fieldTypeGenerators.put(FieldType.CURRENCY, () -> String.format("%.2f", faker.number().randomDouble(2, 10, 100000)));
        fieldTypeGenerators.put(FieldType.DATE, () -> new SimpleDateFormat("MM/dd/yyyy").format(faker.date().future(90, TimeUnit.DAYS)));
        fieldTypeGenerators.put(FieldType.STATE, () -> faker.address().state());
        fieldTypeGenerators.put(FieldType.ID, () -> faker.idNumber().valid());
    }

    @FunctionalInterface
    private interface DataSupplier { String get(); }

    /** Writes one sample value per FieldType to JSON (uses JsonLogger defaults). */
    @Deprecated(since = "2.0")
    // ⚠ Crosses layer boundary — DataGenerator should not write I/O.
    // Replace with: JsonLogger.Write.MapWriter.writeFlatMap(null, path, DataGenerator.generateAllSamples());
    public static void saveFieldTypeSamples(String relativePath) {
        JsonLogger.Write.MapWriter.writeFlatMap(null, relativePath, generateAllSamples());
    }

    /** Persists fieldName -> FieldType map to JSON (stringified values). */
    @Deprecated(since = "2.0")
    // ⚠ Crosses layer boundary — DataGenerator should not write I/O.
    // Replace with: JsonLogger.Write.MapWriter.writeFlatMap(null, path, DataGenerator.toStringifiedMap(map));
    public static void saveFieldTypeMapAsJson(Map<String, FieldType> fieldTypeMap, String relativePath) {
        JsonLogger.Write.MapWriter.writeFlatMap(null, relativePath, toStringifiedMap(fieldTypeMap));
    }

    /**
     * Returns one generated sample value per {@link FieldType}.
     * Callers decide where to send the output (write to JSON, assert in test, etc.).
     */
    public static Map<String, String> generateAllSamples() {
        Map<String, String> samples = new LinkedHashMap<>();
        for (Map.Entry<FieldType, DataSupplier> entry : fieldTypeGenerators.entrySet()) {
            try {
                String value = entry.getValue().get();
                samples.put(entry.getKey().name(), value);
                debug.log("Sample for " + entry.getKey().name() + ": " + value);
            } catch (Exception e) {
                error.log("Failed to generate sample for " + entry.getKey().name() + ": " + e.getMessage());
                samples.put(entry.getKey().name(), "ERROR");
            }
        }
        return samples;
    }

    /**
     * Converts a fieldName → FieldType map to a fieldName → String map (for serialisation).
     * Callers decide where to send the output.
     */
    public static Map<String, String> toStringifiedMap(Map<String, FieldType> fieldTypeMap) {
        if (fieldTypeMap == null || fieldTypeMap.isEmpty()) {
            warn.log("FieldType map is null or empty.");
            return new LinkedHashMap<>();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, FieldType> entry : fieldTypeMap.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? "null" : entry.getValue().name());
        }
        return out;
    }

    /**
     * Generate data for fieldName->FieldType.
     * If {@code fallbackJsonRef} is null/blank, the default is:
     *   {@literal <this package>}/field-type-fallbacks.json  (classpath-first, with IDE dev-path fallback)
     * If non-null:
     *   - If it looks like a path/resource (ends with .json or contains / or \) → read with CLASSPATH_FIRST
     *   - Else treat as a config key and use FileUtils' key-based loader
     */
    public static Map<String, String> generateTestData(Map<String, FieldType> fieldTypeMap,
                                                       @Nullable String fallbackJsonRef) {
        Map<String, String> fallbackMap = resolveFallbackMap(fallbackJsonRef);
        return generateUsing(fieldTypeMap, fallbackMap);
    }

    /** Same as above, but allows forcing a specific ReadMode when passing a path/resource. */
    public static Map<String, String> generateTestData(Map<String, FieldType> fieldTypeMap,
                                                       @Nullable String fallbackJsonRef,
                                                       FileUtils.ReadMode mode) {
        Map<String, String> fallbackMap = resolveFallbackMap(fallbackJsonRef, mode);
        return generateUsing(fieldTypeMap, fallbackMap);
    }

    public static Map<String, String> generateTestData(List<String> fieldNames,
                                                       List<FieldType> fieldTypes,
                                                       @Nullable String fallbackJsonRef) {
        if (fieldNames == null || fieldTypes == null || fieldNames.size() != fieldTypes.size()) {
            throw new IllegalArgumentException("Field names and types must be non-null and of the same size.");
        }
        Map<String, FieldType> fieldTypeMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            fieldTypeMap.put(fieldNames.get(i), fieldTypes.get(i));
        }
        return generateTestData(fieldTypeMap, fallbackJsonRef);
    }

    public static void registerCustomGenerator(FieldType type, DataSupplier generator) {
        fieldTypeGenerators.put(type, generator);
        debug.log("Registered custom generator for FieldType: " + type);
    }

    public static String generateValue(FieldType type) {
        try {
            return Optional.ofNullable(fieldTypeGenerators.get(type))
                    .map(DataSupplier::get)
                    .orElse("UNSUPPORTED");
        } catch (Exception e) {
            error.log("Failed to generate value for FieldType: " + type);
            return "ERROR";
        }
    }

    public static FieldType toFieldType(String fieldName) {
        try {
            String normalized = fieldName.trim()
                    .toUpperCase()
                    .replaceAll("[^A-Z0-9]+", "_")
                    .replaceAll("^_+|_+$", "");
            FieldType type = FieldType.valueOf(normalized);
            debug.log("Mapped field name '" + fieldName + "' to FieldType: " + type);
            return type;
        } catch (IllegalArgumentException e) {
            warn.log("Could not map field name to FieldType: " + fieldName);
            throw new IllegalArgumentException("Invalid field name for enum: " + fieldName, e);
        }
    }

    public static List<FieldType> toFieldTypeList(List<String> fieldNames) {
        List<FieldType> types = new ArrayList<>();
        for (String name : fieldNames) {
            try {
                types.add(toFieldType(name));
            } catch (IllegalArgumentException e) {
                error.log("Skipping invalid field name: " + name + " → " + e.getMessage());
            }
        }
        return types;
    }

    public static Map<String, FieldType> toFieldTypeMap(List<String> fieldNames) {
        Map<String, FieldType> fieldTypeMap = new LinkedHashMap<>();
        if (fieldNames == null || fieldNames.isEmpty()) {
            warn.log("Field names list is null or empty. Returning empty map.");
            return fieldTypeMap;
        }
        for (String name : fieldNames) {
            try {
                FieldType type = toFieldType(name);
                fieldTypeMap.put(name, type);
            } catch (IllegalArgumentException e) {
                error.log("No enum mapping for field name: " + name + ". Will use fallback if present.");
                fieldTypeMap.put(name, null);
            }
        }
        return fieldTypeMap;
    }

    // --------------------------------------------------------------------------------------------
    // Internals
    // --------------------------------------------------------------------------------------------

    private static Map<String, String> generateUsing(Map<String, FieldType> fieldTypeMap,
                                                     Map<String, String> fallbackMap) {
        Map<String, String> generatedData = new LinkedHashMap<>();
        for (Map.Entry<String, FieldType> entry : fieldTypeMap.entrySet()) {
            String fieldName = entry.getKey();
            FieldType type = entry.getValue();
            try {
                String value;
                if (type != null && fieldTypeGenerators.containsKey(type)) {
                    value = fieldTypeGenerators.get(type).get();
                    debug.success("Generated value for field '" + fieldName + "' with type [" + type + "]: " + value);
                } else if (fallbackMap.containsKey(fieldName)) {
                    value = fallbackMap.get(fieldName);
                    info.success("Using fallback value for field '" + fieldName + "': " + value);
                } else {
                    warn.log("No generator or fallback for field '" + fieldName + "'");
                    value = "UNSUPPORTED";
                }
                generatedData.put(fieldName, value);
            } catch (Exception e) {
                error.log("Failed to generate data for field '" + fieldName + "': " + e.getMessage());
                generatedData.put(fieldName, "ERROR");
            }
        }
        return generatedData;
    }

    /** Default classpath resource for fallbacks: <this-package>/field-type-fallbacks.json */
    private static String defaultPackageResourcePath() {
        String pkg = DataGenerator.class.getPackageName().replace('.', '/');
        return pkg + "/field-type-fallbacks.json";
    }

    // replace your existing resolveFallbackMap(...) methods with these:

    private static Map<String, String> resolveFallbackMap(@Nullable String ref) {
        if (ref == null || ref.isBlank()) {
            return loadDefaultPackageFallback(FileUtils.ReadMode.CLASSPATH_FIRST);
        }

        if (looksLikePathOrResource(ref)) {
            Map<String, String> m = FileUtils.loadFallbackJsonMap(ref, FileUtils.ReadMode.CLASSPATH_FIRST);
            if (!m.isEmpty()) return m;
            warn.log("Provided fallback path/resource not found: " + ref + " → falling back to package default");
            return loadDefaultPackageFallback(FileUtils.ReadMode.CLASSPATH_FIRST);
        }

        // Treat as config key
        Map<String, String> m = FileUtils.loadFallbackJsonMap(ref);
        if (!m.isEmpty()) return m;
        warn.log("Config key resolved to empty: " + ref + " → falling back to package default");
        return loadDefaultPackageFallback(FileUtils.ReadMode.CLASSPATH_FIRST);
    }

    private static Map<String, String> resolveFallbackMap(@Nullable String ref, FileUtils.ReadMode mode) {
        if (ref == null || ref.isBlank()) {
            return loadDefaultPackageFallback(mode);
        }

        if (looksLikePathOrResource(ref)) {
            Map<String, String> m = FileUtils.loadFallbackJsonMap(ref, mode);
            if (!m.isEmpty()) return m;
            warn.log("Provided fallback path/resource not found (mode=" + mode + "): " + ref + " → falling back to package default");
            return loadDefaultPackageFallback(mode);
        }

        // Config key: ignore mode and use high-level loader
        Map<String, String> m = FileUtils.loadFallbackJsonMap(ref);
        if (!m.isEmpty()) return m;
        warn.log("Config key resolved to empty: " + ref + " → falling back to package default");
        return loadDefaultPackageFallback(mode);
    }

    // helper: loads <this-package>/field-type-fallbacks.json, then dev candidates
    private static Map<String, String> loadDefaultPackageFallback(FileUtils.ReadMode mode) {
        String resource = defaultPackageResourcePath(); // core.utils/generator/field-type-fallbacks.json
        Map<String, String> m = FileUtils.loadFallbackJsonMap(resource, mode);
        if (!m.isEmpty()) return m;

        for (java.nio.file.Path p : devFallbackCandidates()) {
            m = FileUtils.loadFallbackJsonMap(p);
            if (!m.isEmpty()) return m;
        }

        warn.log("Default package fallback JSON not found (mode=" + mode + "): " + resource);
        return java.util.Collections.emptyMap();
    }


    private static List<Path> devFallbackCandidates() {
        String pkgPath = DataGenerator.class.getPackageName().replace('.', '/');
        return List.of(
                Paths.get("src", "test", "java", pkgPath, "field-type-fallbacks.json"),
                Paths.get("src", "main", "java", pkgPath, "field-type-fallbacks.json")
        );
    }

    private static boolean looksLikePathOrResource(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return lower.endsWith(".json") || s.contains("/") || s.contains("\\");
    }
}
