// file: core/resolvers/locator/json/JsonLocatorMigrator.java
package core.resolvers.locator.json;

import elements.api.Element;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.utils.ConfigLoader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static core.logging.CustomLogger.*;

/**
 * Migrates enum-based locator methods into a JSON structure while preserving page structure.
 *
 * Behavior:
 *  - Keeps the same nested page structure as classes/enums.
 *  - Only includes methods starting with "get" and ending with "Locator".
 *  - If getExternalFileName() returns null → outputs raw values.
 *  - If getExternalFileName() returns a path → loads from classpath resources using ConfigLoader.
 */
public final class JsonLocatorMigrator {

    private static final ObjectMapper M = new ObjectMapper();
    /** Base directory under resources for all locator artifacts. */
    public static final Path DEFAULT_LOCATORS_DIR = Paths.get("src/main/resources/locators");
    /** Default output directory for generated JSON locator files. */
    public static final Path DEFAULT_OUT_DIR = DEFAULT_LOCATORS_DIR.resolve("json");

    /**
     * Per-migration properties cache: fully-qualified classpath path → merged Properties.
     * <p>
     * Populated at the start of {@link #buildResolvedJson} and cleared in its {@code finally} block.
     * This prevents the same {@code .properties} file from being loaded multiple times when several
     * enums in the same class tree share the same external file.
     */
    private static final ThreadLocal<Map<String, Properties>> PROPS_CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private JsonLocatorMigrator() {
        // Static utility — prevent instantiation. No WebDriver needed at migration time.
    }

    /* =============================== Public API =============================== */

    /** Build JSON string with resolved locators for the given root class. */
    public static String buildResolvedJson(Class<?> rootClass) {
        Objects.requireNonNull(rootClass, "rootClass must not be null");
        long startNs = System.nanoTime();
        debug.log("[migrate:start] root=" + rootClass.getSimpleName());
        PROPS_CACHE.set(new HashMap<>());   // fresh cache for this migration run
        try {
            ObjectNode root = M.createObjectNode();
            buildClassInto(root, rootClass);
            String json = M.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            debug.log("[migrate:done] root=" + rootClass.getSimpleName()
                    + " ms=" + (System.nanoTime() - startNs) / 1_000_000L
                    + " topNodes=" + countFields(root));
            return json;
        } catch (IOException e) {
            error.failed("[migrate:error] root=" + rootClass.getSimpleName() + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to serialize JSON", e);
        } finally {
            PROPS_CACHE.remove();           // prevent ThreadLocal memory leak
        }
    }

    /**
     * Build JSON and write to the default directory under resources.
     * <p>
     * Calls {@link #buildResolvedJson} exactly <b>once</b>; the result is
     * passed directly to the writer — no second rebuild.
     */
    public static Path writeResolvedJson(Class<?> rootClass) {
        String json     = buildResolvedJson(rootClass);
        String fileName = rootClass.getSimpleName().toLowerCase(Locale.ROOT) + "-locators.json";
        return writeJsonString(json, DEFAULT_OUT_DIR.resolve(fileName));
    }

    /**
     * Build JSON and write to an explicit file.
     * <p>
     * Calls {@link #buildResolvedJson} exactly <b>once</b>; the result is
     * passed directly to the writer — no second rebuild.
     */
    public static Path writeResolvedJsonTo(Class<?> rootClass, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        String json = buildResolvedJson(rootClass);
        return writeJsonString(json, outputFile);
    }

    /**
     * Write a pre-built JSON string to a file.
     * Use this when you already have the JSON (e.g. from {@link #buildResolvedJson})
     * and want to persist it without triggering a second migration pass.
     */
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

    /* =============================== Conversion =============================== */

    /** Recursively converts the class and nested enums into JSON nodes. */
    private static void buildClassInto(ObjectNode current, Class<?> clazz) {
        long start = System.nanoTime();
        ObjectNode thisNode = current.putObject(clazz.getSimpleName());
        int locators = 0;
        if (clazz.isEnum()) locators = writeEnumMethodLocators(thisNode, clazz);
        int nested = 0;
        for (Class<?> nestedClass : nestedInSourceOrder(clazz)) {
            if (nestedClass.isSynthetic() || nestedClass.getName().contains("$$")) continue;
            buildClassInto(thisNode, nestedClass);
            nested++;
        }
        debug.log("[class] name=" + clazz.getSimpleName() + " enum=" + clazz.isEnum() + " locators=" + locators + " nested=" + nested + " ms=" + (System.nanoTime()-start)/1_000_000L);
    }

    /** Preserve approximate source order. */
    private static List<Class<?>> nestedInSourceOrder(Class<?> clazz) {
        Class<?>[] arr = clazz.getDeclaredClasses();
        List<Class<?>> list = new ArrayList<>(Arrays.asList(arr));
        Collections.reverse(list);
        return list;
    }

    /**
     * Emits locator mappings for enum constants.
     * Uses classpath-based ConfigLoader if getExternalFileName() is defined.
     * Properties are loaded via {@link #PROPS_CACHE} — each file is loaded at most once
     * per migration run regardless of how many enums reference it.
     */
    private static int writeEnumMethodLocators(ObjectNode enumNode, Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) { warn.log("[enum] empty " + enumClass.getSimpleName()); return 0; }
        Object target = Arrays.stream(constants).filter(c -> c instanceof Element).findFirst().orElse(constants[0]);
        String propsPath = findFirstExternalFileName(constants);
        Properties props = null;
        if (propsPath != null && !propsPath.isBlank()) {
            // Resolve the properties path the same way ElementLocatorResolverV1 does:
            // prepend "locators/" unless the path is already rooted there.
            String cpPath = propsPath.startsWith("locators/") ? propsPath : "locators/" + propsPath;
            // Use the per-migration cache — loads each file at most once per buildResolvedJson call.
            props = PROPS_CACHE.get().computeIfAbsent(cpPath, JsonLocatorMigrator::loadMergedProperties);
            debug.log("[enum] props name=" + enumClass.getSimpleName()
                    + " file=" + cpPath + " keys=" + props.size());
        }
        int added = 0; int resolved = 0; int raw = 0;
        for (Method m : enumClass.getDeclaredMethods()) {
            String name = m.getName();
            if (!name.startsWith("get") || !name.endsWith("Locator")) continue;
            if (m.getParameterCount() != 0 || !String.class.equals(m.getReturnType())) continue;
            String keyName = decapitalize(name.substring(3));
            try {
                m.setAccessible(true);
                Object rawVal = m.invoke(target);
                if (!(rawVal instanceof String val)) continue;
                if (val.isBlank()) continue;
                String resolvedVal = props == null ? null : props.getProperty(val.trim());
                String finalVal = (resolvedVal != null && !resolvedVal.isBlank()) ? resolvedVal.trim() : val;
                enumNode.put(keyName, finalVal);
                added++;
                if (resolvedVal != null && !resolvedVal.isBlank()) resolved++; else raw++;
            } catch (ReflectiveOperationException e) {
                warn.log("[enum] reflectFail enum=" + enumClass.getSimpleName() + " method=" + name + " msg=" + e.getMessage());
            }
        }
        debug.log("[enum] name=" + enumClass.getSimpleName() + " added=" + added + " resolved=" + resolved + " raw=" + raw + " fields=" + enumNode.size());
        return added;
    }

    /**
     * Load and merge TEST + MAIN classpath properties for the given path.
     * TEST entries win over MAIN on key conflict (mirrors ElementLocatorResolverV1).
     * Called at most once per path per migration run via {@link #PROPS_CACHE}.
     */
    private static Properties loadMergedProperties(String cpPath) {
        debug.log("[props:load] loading " + cpPath + " (TEST+MAIN)");
        Properties test   = ConfigLoader.loadFromClasspath(cpPath, ConfigLoader.ClasspathScope.TEST);
        Properties main   = ConfigLoader.loadFromClasspath(cpPath, ConfigLoader.ClasspathScope.MAIN);
        Properties merged = ConfigLoader.merge(main, test); // test wins
        debug.log("[props:loaded] file=" + cpPath + " keys=" + merged.size());
        return merged;
    }

    private static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() == 1) return s.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String findFirstExternalFileName(Object[] constants) {
        for (Object c : constants) {
            if (c instanceof Element) {
                try {
                    String pf = ((Element) c).getExternalFileName();
                    if (pf != null && !pf.isBlank()) return pf;
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    // --- Logging helper utilities (non-intrusive) ---
    private static int countFields(ObjectNode node) {
        if (node == null) return 0;
        int c = 0;
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) { c++; it.next(); }
        return c;
    }

    /* =============================== Demo =============================== */

    /**
     * CLI entry point.
     *
     * <p>Usage:
     * <pre>
     *   # Print JSON to stdout for a class:
     *   java JsonLocatorMigrator --print  com.example.MyPageElements
     *
     *   # Write JSON to the default output directory (src/main/resources/locators/json/):
     *   java JsonLocatorMigrator --write  com.example.MyPageElements
     *
     *   # Write to a specific file:
     *   java JsonLocatorMigrator --write  com.example.MyPageElements  path/to/output.json
     * </pre>
     *
     * <p>When no arguments are supplied a short usage hint is printed.
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  JsonLocatorMigrator --print  <fully.qualified.ClassName>");
            System.out.println("  JsonLocatorMigrator --write  <fully.qualified.ClassName>");
            System.out.println("  JsonLocatorMigrator --write  <fully.qualified.ClassName>  <outputFile>");
            return;
        }

        String mode      = args[0].toLowerCase(Locale.ROOT);
        String className = args[1];

        Class<?> rootClass;
        try {
            rootClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println("[error] Class not found: " + className);
            System.exit(1);
            return;
        }

        switch (mode) {
            case "--print": {
                String json = buildResolvedJson(rootClass);
                System.out.println(json);
                break;
            }
            case "--write": {
                Path out = (args.length >= 3)
                        ? Paths.get(args[2])
                        : null; // use default dir
                Path written = (out != null)
                        ? writeResolvedJsonTo(rootClass, out)
                        : writeResolvedJson(rootClass);
                System.out.println("[done] Written to: " + written.toAbsolutePath());
                break;
            }
            default:
                System.err.println("[error] Unknown mode: " + mode + ". Use --print or --write.");
                System.exit(1);
        }
    }
}
