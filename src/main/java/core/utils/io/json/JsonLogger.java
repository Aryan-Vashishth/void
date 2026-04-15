package core.utils.io.json;

import core.utils.ConfigLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static core.utils.io.FileUtils.*;
import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;


public class JsonLogger {

    private JsonLogger() { /* static utility */ }

    private static final String DEFAULT_BASE_PATH = ConfigLoader.get("json.logger.base.path", "target/logs/");


    private static class MapperUtils {
        private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    // Configurable base paths for writer and logger
    private static final String WRITER_BASE_PATH = ConfigLoader.get("json.writer.base.path", "target/logs/");
    private static final String LOGGER_BASE_PATH = ConfigLoader.get("json.logger.base.path", "target/logs/");

    public static class Write {

        public static class GenericWriter {
            public static <T> void toJson(@Nullable String sourceRoot, String fileName, T data) {
                if (data == null) {
                    warn.log("Attempted to write null data to: " + fileName);
                    return;
                }
                if (sourceRoot == null) {
                    sourceRoot = DEFAULT_BASE_PATH;
                }
                File file = resolveFile(sourceRoot, fileName, "json");
                ensureDirectoryExists(file);

                try (FileOutputStream fos = new FileOutputStream(file, false);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                    MapperUtils.mapper.writeValue(bos, data);
                    bos.flush();
                    fos.flush();
                    debug.log("✅ JSON written to: " + file.getAbsolutePath());

                } catch (IOException e) {
                    error.log("❌ Failed to write JSON to: " + file.getAbsolutePath() + " → " + e.getMessage());
                    throw new RuntimeException("Failed to write JSON to: " + file.getAbsolutePath(), e);
                }
            }
        }

        public static class MapWriter {
            // Write using default writer path
            public static void writeRowList(@Nullable String sourceRoot, String fileName, List<Map<String, String>> data) {
                GenericWriter.toJson(sourceRoot, fileName, data);
            }
            public static void writeFlatMap(@Nullable String sourceRoot, String fileName, Map<String, String> data) {
                GenericWriter.toJson(sourceRoot, fileName, data);
            }
            // Write to logger path if specifically logging
            public static void logRowList(String fileName, List<Map<String, String>> data) {
                GenericWriter.toJson(LOGGER_BASE_PATH, fileName, data);
            }
            public static void logFlatMap(String fileName, Map<String, String> data) {
                GenericWriter.toJson(LOGGER_BASE_PATH, fileName, data);
            }
        }

        public static class ListWriter {
            public static void writeStringList(String fileName, List<String> data) {
                GenericWriter.toJson(LOGGER_BASE_PATH, fileName, data);
            }
            public static void logStringList(String fileName, List<String> data) {
                GenericWriter.toJson(LOGGER_BASE_PATH, fileName, data);
            }
        }
    }
}
