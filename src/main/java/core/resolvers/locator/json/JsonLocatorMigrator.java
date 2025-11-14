// file: core/resolvers/locator/json/JsonLocatorMigrator.java
package core.resolvers.locator.json;

import Elements.DemoPageElements;
import Elements.interfacesv1.Element;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.utils.BaseUtils;
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
public final class JsonLocatorMigrator extends BaseUtils {

    private static final ObjectMapper M = new ObjectMapper();
    /** Base directory under resources for all locator artifacts. */
    public static final Path DEFAULT_LOCATORS_DIR = Paths.get("src/main/resources/locators");
    /** Default output directory for generated JSON locator files. */
    public static final Path DEFAULT_OUT_DIR = DEFAULT_LOCATORS_DIR.resolve("json");

    private JsonLocatorMigrator() { initializer(); }

    /* =============================== Public API =============================== */

    /** Build JSON string with resolved locators for the given root class. */
    public static String buildResolvedJson(Class<?> rootClass) {
        Objects.requireNonNull(rootClass, "rootClass must not be null");
        long startNs = System.nanoTime();
        debug.log("[migrate:start] root=" + rootClass.getSimpleName());
        ObjectNode root = M.createObjectNode();
        buildClassInto(root, rootClass);
        try {
            String json = M.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            debug.log("[migrate:done] root=" + rootClass.getSimpleName() + " ms=" + (System.nanoTime()-startNs)/1_000_000L + " topNodes=" + countFields(root));
            return json;
        } catch (IOException e) {
            error.failed("[migrate:error] root=" + rootClass.getSimpleName() + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    /** Write JSON to default dir under resources. */
    public static Path writeResolvedJson(Class<?> rootClass) {
        String fileName = rootClass.getSimpleName().toLowerCase(Locale.ROOT) + "-locators.json";
        Path out = DEFAULT_OUT_DIR.resolve(fileName);
        return writeResolvedJsonTo(rootClass, out);
    }

    /** Write JSON to an explicit file. */
    public static Path writeResolvedJsonTo(Class<?> rootClass, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        long startNs = System.nanoTime();
        debug.log("[write:start] root=" + rootClass.getSimpleName() + " file=" + outputFile);
        String json = buildResolvedJson(rootClass);
        try {
            Path parent = outputFile.getParent();
            if (parent != null && !Files.exists(parent)) { Files.createDirectories(parent); debug.log("[write:mkdir] " + parent); }
            Files.write(outputFile, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            debug.log("[write:done] file=" + outputFile + " bytes=" + json.length() + " ms=" + (System.nanoTime()-startNs)/1_000_000L);
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
     */
    private static int writeEnumMethodLocators(ObjectNode enumNode, Class<?> enumClass) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) { warn.log("[enum] empty " + enumClass.getSimpleName()); return 0; }
        Object target = Arrays.stream(constants).filter(c -> c instanceof Element).findFirst().orElse(constants[0]);
        String propsPath = findFirstExternalFileName(constants);
        Properties props = null;
        if (propsPath != null && !propsPath.isBlank()) {
            props = ConfigLoader.loadFromClasspath(propsPath, ConfigLoader.ClasspathScope.MAIN);
            debug.log("[enum] propsLoaded name=" + enumClass.getSimpleName() + " file=" + propsPath + " keys=" + (props==null?0:props.size()));
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
                if (!(rawVal instanceof String)) continue;
                String val = (String) rawVal;
                if (val == null || val.isBlank()) continue;
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

    /* =============================== Demo =============================== */
    public static void main(String[] args) {
        Class<DemoPageElements> root = DemoPageElements.class;
        debug.log("[run] start root=" + root.getSimpleName());
        String json = buildResolvedJson(root);
        info.log(json);
        Path out = JsonLocatorMigrator.writeResolvedJson(root);
        debug.log("[run] output=" + out.toAbsolutePath() + " bytes=" + json.length());
    }

    // --- Logging helper utilities (non-intrusive) ---
    private static int countFields(ObjectNode node) {
        if (node == null) return 0;
        int c = 0; Iterator<String> it = node.fieldNames(); while (it.hasNext()) { c++; it.next(); }
        return c;
    }
    private static String sampleFieldNames(ObjectNode node, int max) {
        if (node == null) return "[]";
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext() && names.size() < max) names.add(it.next());
        return names.toString();
    }
    private static String samplePropertiesKeys(Properties p, int max) {
        if (p == null || p.isEmpty()) return "[]";
        List<String> keys = new ArrayList<>();
        for (String k : p.stringPropertyNames()) { keys.add(k); if (keys.size() >= max) break; }
        return keys.toString();
    }
}
