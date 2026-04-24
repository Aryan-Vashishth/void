// file: core/resolvers/locator/json/JsonLocatorMigrator.java
package core.resolvers.locator.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.error;

/**
 * Façade that orchestrates building and persisting JSON locator files from enum-based
 * page descriptors.
 *
 * <p><b>Phase&nbsp;5 refactor:</b> the original 296-line monolith was decomposed along
 * its five distinct responsibilities:</p>
 * <ul>
 *   <li>{@link PropertiesIndex} — per-run cache of merged TEST/MAIN property bundles
 *       (replaces a static {@code ThreadLocal})</li>
 *   <li>{@link EnumLocatorScanner} — reflection over enum constants → key/value pairs</li>
 *   <li>{@link JsonTreeBuilder} — recursive class-tree walking</li>
 *   <li>{@link JsonMigratorCli} — command-line entry point</li>
 *   <li>This class — file I/O + the legacy public static API</li>
 * </ul>
 *
 * <p>The legacy static signatures are preserved exactly; all behaviour is unchanged.</p>
 */
public final class JsonLocatorMigrator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Base directory under resources for all locator artifacts. */
    public static final Path DEFAULT_LOCATORS_DIR = Paths.get("src/main/resources/locators");
    /** Default output directory for generated JSON locator files. */
    public static final Path DEFAULT_OUT_DIR      = DEFAULT_LOCATORS_DIR.resolve("json");

    private JsonLocatorMigrator() { /* Static façade — prevent instantiation. */ }

    /* =============================== Public API =============================== */

    /** Build a JSON string with resolved locators for {@code rootClass}. */
    public static String buildResolvedJson(Class<?> rootClass) {
        Objects.requireNonNull(rootClass, "rootClass must not be null");
        ObjectNode root = new JsonTreeBuilder().build(rootClass);
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            error.failed("[migrate:error] root=" + rootClass.getSimpleName() + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    /** Build JSON and write to the default directory under {@code resources/locators/json}. */
    public static Path writeResolvedJson(Class<?> rootClass) {
        String json     = buildResolvedJson(rootClass);
        String fileName = rootClass.getSimpleName().toLowerCase(Locale.ROOT) + "-locators.json";
        return writeJsonString(json, DEFAULT_OUT_DIR.resolve(fileName));
    }

    /** Build JSON and write to an explicit file. */
    public static Path writeResolvedJsonTo(Class<?> rootClass, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        return writeJsonString(buildResolvedJson(rootClass), outputFile);
    }

    /** Persist a pre-built JSON string to {@code outputFile}, creating parent dirs as needed. */
    public static Path writeJsonString(String json, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        long startNs = System.nanoTime();
        debug.log("[write:start] file=" + outputFile);
        try {
            Path parent = outputFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                debug.log("[write:mkdir] " + parent);
            }
            Files.writeString(outputFile, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            debug.log("[write:done] file=" + outputFile
                    + " bytes=" + json.length()
                    + " ms=" + (System.nanoTime() - startNs) / 1_000_000L);
            return outputFile;
        } catch (IOException e) {
            error.failed("[write:error] file=" + outputFile + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to write JSON to " + outputFile, e);
        }
    }

}
