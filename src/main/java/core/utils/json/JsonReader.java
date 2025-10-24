package core.utils.json;

import core.utils.BaseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.utils.ConfigLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static core.utils.FileUtils.resolveFile;

public class JsonReader extends BaseUtils {

    public JsonReader() {
        initializer();
    }

    private static class MapperUtils {
        private static final ObjectMapper mapper = new ObjectMapper();
        private static final String DEFAULT_BASE_PATH = ConfigLoader.get("json.reader.base.path", "target/logs/");
    }

    // ─────────────────────────────────────────────────────────────
    // READERS
    // ─────────────────────────────────────────────────────────────

    public static class Read {

        public static class MapReader {

            public static Map<String, Object> asObjectMapFromClasspath(@Nullable String basePath, String fileName) {
                try {
                    String root = (basePath == null || basePath.isBlank())
                            ? MapperUtils.DEFAULT_BASE_PATH
                            : basePath;
                    String resourcePath = (root.endsWith("/") ? root : root + "/") + fileName;

                    try (InputStream is = Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(resourcePath)) {

                        if (is == null) {
                            error.log("❌ JSON classpath resource not found: " + resourcePath);
                            throw new RuntimeException("JSON classpath resource not found: " + resourcePath);
                        }

                        debug.log("[MapReader] Reading Map<String,Object> from CLASSPATH: " + resourcePath);
                        return MapperUtils.mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    error.log("❌ Failed to read object map from classpath: " + e.getMessage());
                    throw new RuntimeException("Failed to read object map from classpath: " + fileName, e);
                }
            }

            public static Map<String, String> asFlatMap(@Nullable String sourceRoot, String fileName) {
                if (sourceRoot == null) {
                    sourceRoot = MapperUtils.DEFAULT_BASE_PATH;
                }
                File file = resolveFile(sourceRoot, fileName, "json");
                
                if (!file.exists()) {
                    error.log("❌ JSON flat map file not found: " + file.getAbsolutePath());
                    throw new RuntimeException("JSON flat map file not found: " + file.getAbsolutePath());
                }
                debug.log("[MapReader] Reading Map<String, String> from: " + file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return MapperUtils.mapper.readValue(is, new TypeReference<>() {});
                } catch (Exception e) {
                    error.log("❌ Failed to read flat map: " + e.getMessage());
                    throw new RuntimeException("Failed to read flat map from: " + file.getAbsolutePath(), e);
                }
            }

            public static Map<String, Object> asObjectMap(String sourceRoot, String fileName) {
                File file = resolveFile(sourceRoot, fileName, "json");
                
                if (file == null || !file.exists()) {
                    String attempted = (file == null ? "null" : file.getAbsolutePath());
                    error.log("❌ JSON object map file not found: " + attempted);
                    throw new RuntimeException("JSON object map file not found: " + attempted);
                }
                debug.log("[MapReader] Reading Map<String, Object> from: " + file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return MapperUtils.mapper.readValue(is, new TypeReference<>() {});
                } catch (Exception e) {
                    error.log("❌ Failed to read object map: " + e.getMessage());
                    throw new RuntimeException("Failed to read object map from: " + file.getAbsolutePath(), e);
                }
            }
        }

        public static class ListReader {
            public static List<Map<String, String>> asRowList(@Nullable String sourceRoot, String fileName) {
                if (sourceRoot == null) {
                    sourceRoot = MapperUtils.DEFAULT_BASE_PATH;
                }
                File file = resolveFile(sourceRoot, fileName, "json");
                
                if (!file.exists()) {
                    error.log("❌ JSON row list file not found: " + file.getAbsolutePath());
                    throw new RuntimeException("JSON row list file not found: " + file.getAbsolutePath());
                }
                debug.log("[ListReader] Reading List<Map<String, String>> from: " + file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return MapperUtils.mapper.readValue(is, new TypeReference<>() {
                    });
                } catch (Exception e) {
                    error.log("❌ Failed to read row list: " + e.getMessage());
                    throw new RuntimeException("Failed to read row list from: " + file.getAbsolutePath(), e);
                }
            }

            public static List<String> asColumnList(String sourceRoot, String fileName) {
                File file = resolveFile(sourceRoot, fileName, "json");
                
                if (file == null || !file.exists()) {
                    String attempted = (file == null ? "null" : file.getAbsolutePath());
                    error.log("❌ JSON column list file not found: " + attempted);
                    throw new RuntimeException("JSON column list file not found: " + attempted);
                }
                debug.log("[ListReader] Reading List<String> from: " + file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return MapperUtils.mapper.readValue(is, new TypeReference<>() {
                    });
                } catch (Exception e) {
                    error.log("❌ Failed to read column list: " + e.getMessage());
                    throw new RuntimeException("Failed to read column list from: " + file.getAbsolutePath(), e);
                }
            }
        }

        public static class ObjectReader {
            public static <T> T asObject(String sourceRoot, String fileName, Class<T> clazz) {
                File file = resolveFile(sourceRoot, fileName, "json");
                
                if (file == null || !file.exists()) {
                    String attempted = (file == null ? "null" : file.getAbsolutePath());
                    error.log("❌ JSON object file not found: " + attempted);
                    throw new RuntimeException("JSON object file not found: " + attempted);
                }
                debug.log("[ObjectReader] Reading " + clazz.getSimpleName() + " from: " + file.getAbsolutePath());
                try (InputStream is = new FileInputStream(file)) {
                    return MapperUtils.mapper.readValue(is, clazz);
                } catch (Exception e) {
                    error.log("❌ Failed to map JSON to object: " + e.getMessage());
                    throw new RuntimeException("Failed to map JSON to object: " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // VALUE EXTRACTORS
    // ─────────────────────────────────────────────────────────────

    public static class Extract {

        public static String valueAsJsonByKey(List<Map<String, String>> rows, String key) {
            try {
                for (Map<String, String> row : rows) {
                    if (row.containsKey(key)) {
                        ObjectNode node = MapperUtils.mapper.createObjectNode();
                        node.put(key, row.get(key));
                        return MapperUtils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(List.of(node));
                    }
                }
                throw new NoSuchElementException("Key not found: " + key);
            } catch (Exception e) {
                error.log("❌ Failed to extract key '" + key + "': " + e.getMessage());
                throw new RuntimeException("Key extraction failed: " + key, e);
            }
        }

        public static List<String> valuesByKey(List<Map<String, String>> rows, String key) {
            try {
                return rows.stream()
                        .filter(row -> row.containsKey(key))
                        .map(row -> row.get(key))
                        .toList();
            } catch (Exception e) {
                error.log("❌ Failed to extract values for key '" + key + "': " + e.getMessage());
                throw new RuntimeException("Value extraction failed for key: " + key, e);
            }
        }

        public static Map<String, String> rowByKeyValue(List<Map<String, String>> rows, String key, String value) {
            try {
                return rows.stream()
                        .filter(row -> value.equals(row.get(key)))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("No match for " + key + " = " + value));
            } catch (Exception e) {
                error.log("❌ Failed to find row for " + key + "=" + value + ": " + e.getMessage());
                throw new RuntimeException("Row lookup failed", e);
            }
        }

        public static boolean keyExists(List<Map<String, String>> rows, String key) {
            try {
                return rows.stream().anyMatch(row -> row.containsKey(key));
            } catch (Exception e) {
                error.log("❌ Failed to check key existence: " + e.getMessage());
                throw new RuntimeException("Key existence check failed: " + key, e);
            }
        }
    }
}
