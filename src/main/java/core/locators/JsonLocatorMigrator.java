package core.locators;

import Elements.Interfaces.BaseElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * JsonLocatorMigrator (flattened output, deduplicated "locator")
 * --------------------------------------------------------------
 * - Single top-level node (e.g., "ManageUsersElements"), no duplicate self-nesting.
 * - If getKey() differs by enum constant, emits each CONSTANT as a direct field.
 * - If getKey() is identical for all constants, emits a single "locator" IFF it
 *   does not duplicate any other "*Locator" already emitted (e.g., inputLocator).
 * - Maps other *Key accessors to ...Locator (inputLocator, resultLocator, triggerLocator, listLocator, etc.).
 * - Smarter .properties discovery: classpath + filesystem + optional -Dlocators.base.
 * - NEW: Never prunes empty classes/enums (keeps placeholders for later filling).
 * - NEW: Preserves source declaration order of nested types.
 */
public final class JsonLocatorMigrator {

    private static final ObjectMapper M = new ObjectMapper();
    private static final Path DEFAULT_OUT_DIR = Paths.get("src/main/resources/locators");
    private static final String LOCATORS_BASE_SYS_PROP = "locators.base";

    private JsonLocatorMigrator() {}

    /* =============================== Public API =============================== */

    /** Build JSON string with resolved locators for the given root class. */
    public static String buildResolvedJson(Class<?> rootClass) {
        Objects.requireNonNull(rootClass, "rootClass must not be null");

        String propFileName = findPropertyFileFromAnyConstant(rootClass);
        if (propFileName == null || propFileName.isBlank()) {
            throw new IllegalStateException("Could not determine property file via BaseElement#getPropertyFile().");
        }
        Properties props = loadPropertiesSmart(propFileName, rootClass);

        ObjectNode root = M.createObjectNode();
        ObjectNode top = root.putObject(simple(rootClass));

        // Process the root class directly into 'top' (NO extra same-name child)
        buildClassInto(top, rootClass, props);

        // IMPORTANT: Do NOT prune empty nodes. We want placeholders to remain.
        // pruneEmpty(top); // removed on purpose

        try {
            return M.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    /** Write JSON to default dir with a name derived from the source .properties. */
    public static Path writeResolvedJson(Class<?> rootClass) {
        String propFile = findPropertyFileFromAnyConstant(rootClass);
        if (propFile == null) throw new IllegalStateException("No property file found via BaseElement#getPropertyFile().");
        String fileName = deriveOutputJsonName(propFile);
        Path out = DEFAULT_OUT_DIR.resolve(fileName);
        return writeResolvedJsonTo(rootClass, out);
    }

    /** Write JSON to an explicit file. */
    public static Path writeResolvedJsonTo(Class<?> rootClass, Path outputFile) {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        String json = buildResolvedJson(rootClass);
        try {
            Path parent = outputFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(outputFile, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to " + outputFile, e);
        }
    }

    /* =============================== Conversion =============================== */

    /** Writes the given class directly into the provided JSON object node. */
    private static void buildClassInto(ObjectNode current, Class<?> clazz, Properties props) {
        // If this class itself is an enum, emit it here
        if (clazz.isEnum()) {
            ObjectNode enumNode = current.putObject(simple(clazz));
            writeEnumLocators(enumNode, clazz, props);
        }

        // Iterate nested types in (heuristic) source declaration order
        for (Class<?> nested : nestedInSourceOrder(clazz)) {
            ObjectNode child = current.putObject(simple(nested));
            if (nested.isEnum()) {
                writeEnumLocators(child, nested, props);
            } else {
                buildClassInto(child, nested, props);
            }
        }
    }

    /**
     * Heuristic to preserve source declaration order.
     * Many JVMs/JDKs return getDeclaredClasses() in reverse of the source order.
     * Reversing here typically restores the original declaration sequence.
     */
    private static List<Class<?>> nestedInSourceOrder(Class<?> clazz) {
        Class<?>[] arr = clazz.getDeclaredClasses();
        List<Class<?>> list = new ArrayList<>(Arrays.asList(arr));
        Collections.reverse(list); // critical to match source order in practice
        return list;
    }

    /** Emit enum locators into enumNode with dedup for "locator". */
    private static void writeEnumLocators(ObjectNode enumNode, Class<?> enumClass, Properties props) {
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) return; // keep the enum node empty if no constants

        List<Method> keyMethods = discoverKeyMethods(enumClass);

        // 1) Emit OTHER *Key methods first and collect their values (to dedupe "locator" later).
        Map<String, String> otherLocatorValues = new LinkedHashMap<>();
        for (Method m : keyMethods) {
            if (m.getName().equals("getKey")) continue; // skip main getKey here
            String jsonKey = toLocatorJsonKey(m.getName());
            if (jsonKey == null) continue;
            String key = safeInvoke(constants[0], m);
            String resolved = resolve(props, key);
            if (resolved != null) {
                enumNode.put(jsonKey, resolved);
                otherLocatorValues.put(jsonKey, resolved);
            }
        }

        // 2) Handle getKey(): per-constant vs single unique value
        Optional<Method> getKeyMethodOpt = keyMethods.stream()
                .filter(m -> m.getName().equals("getKey")).findFirst();

        if (getKeyMethodOpt.isPresent()) {
            Method getKeyMethod = getKeyMethodOpt.get();

            Map<String, String> perConstResolved = new LinkedHashMap<>();
            Set<String> uniqueVals = new LinkedHashSet<>();

            for (Object c : constants) {
                String key = safeInvoke(c, getKeyMethod);
                String resolved = resolve(props, key);
                if (resolved != null) {
                    String constName = ((Enum<?>) c).name();
                    perConstResolved.put(constName, resolved);
                    uniqueVals.add(resolved);
                } else {
                    // Keep placeholder field for the constant (explicitly missing)
                    String constName = ((Enum<?>) c).name();
                    perConstResolved.putIfAbsent(constName, null);
                }
            }

            if (!perConstResolved.isEmpty()) {
                if (uniqueVals.size() == 1 && !uniqueVals.isEmpty()) {
                    String single = uniqueVals.iterator().next();
                    // Deduplicate: only emit "locator" if it doesn't equal any existing *Locator we already emitted
                    if (!otherLocatorValues.containsValue(single)) {
                        enumNode.put("locator", single);
                    }
                } else {
                    // DIFFERENT per-constant OR some missing → emit each CONSTANT explicitly
                    // Missing values will appear as JSON null to be filled later
                    perConstResolved.forEach((k, v) -> {
                        if (v == null) {
                            enumNode.putNull(k);
                        } else {
                            enumNode.put(k, v);
                        }
                    });
                }
            }
        }
    }

    /** Only public, no-arg, String-returning methods ending with "Key". */
    private static List<Method> discoverKeyMethods(Class<?> enumClass) {
        List<Method> out = new ArrayList<>();
        for (Method m : enumClass.getMethods()) {
            if (!m.getName().endsWith("Key")) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != String.class) continue;
            out.add(m);
        }
        out.sort(Comparator.comparing(Method::getName));
        return out;
    }

    /** Map method names to JSON keys (getKey→locator; avoid double “Locator”). */
    private static String toLocatorJsonKey(String methodName) {
        if (methodName == null || !methodName.endsWith("Key")) return null;
        if (methodName.equals("getKey")) return "locator";
        String base = methodName.startsWith("get") ? methodName.substring(3) : methodName;
        base = base.substring(0, base.length() - 3); // drop "Key"
        if (base.isEmpty()) return "locator";
        String lc = Character.toLowerCase(base.charAt(0)) + base.substring(1);
        return lc.endsWith("Locator") ? lc : lc + "Locator";
    }

    private static String safeInvoke(Object target, Method m) {
        try {
            Object v = m.invoke(target);
            return v == null ? null : v.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String resolve(Properties p, String key) {
        if (key == null || key.isBlank()) return null;
        String v = p.getProperty(key);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // Kept for reference; not used anymore (we no longer prune empties).
    @SuppressWarnings("unused")
    private static boolean pruneEmpty(ObjectNode node) {
        Iterator<String> it = node.fieldNames();
        List<String> toRemove = new ArrayList<>();
        while (it.hasNext()) {
            String f = it.next();
            if (node.get(f).isObject()) {
                ObjectNode child = (ObjectNode) node.get(f);
                boolean empty = pruneEmpty(child);
                if (empty) toRemove.add(f);
            }
        }
        for (String f : toRemove) node.remove(f);
        return node.size() == 0;
    }

    /* =============================== Properties discovery =============================== */

    private static Properties loadPropertiesSmart(String resourcePathFromEnum, Class<?> rootClass) {
        List<String> tried = new ArrayList<>();
        Properties props;

        String base = System.getProperty(LOCATORS_BASE_SYS_PROP);
        if (base != null && !base.isBlank()) {
            Path baseDir = Paths.get(base);
            Path candidate = baseDir.resolve(resourcePathFromEnum.replaceFirst("^/+", ""));
            props = tryLoadFromFile(candidate, tried);
            if (props != null) return props;
        }

        String rp = resourcePathFromEnum.startsWith("/") ? resourcePathFromEnum.substring(1) : resourcePathFromEnum;

        props = tryLoadFromClasspath(rp, tried);
        if (props != null) return props;

        String[] prefixes = { "", "locators/", "locators/properties/", "config/", "config/locators/" };
        for (String pre : prefixes) {
            String cand = pre + rp;
            props = tryLoadFromClasspath(cand, tried);
            if (props != null) return props;
        }

        Path[] fsRoots = {
                Paths.get(rp),
                Paths.get("src/main/resources", rp),
                Paths.get("src/test/resources", rp)
        };
        for (Path p : fsRoots) {
            props = tryLoadFromFile(p, tried);
            if (props != null) return props;
            for (String pre : prefixes) {
                Path cand = p.getParent() == null
                        ? Paths.get(pre, p.toString())
                        : p.getParent().resolve(pre).resolve(p.getFileName().toString());
                props = tryLoadFromFile(cand, tried);
                if (props != null) return props;
            }
        }

        StringBuilder sb = new StringBuilder("Properties not found on classpath or filesystem: ")
                .append(resourcePathFromEnum).append("\nTried:\n");
        for (String t : tried) sb.append(" - ").append(t).append('\n');
        throw new IllegalStateException(sb.toString());
    }

    private static Properties tryLoadFromClasspath(String name, List<String> tried) {
        tried.add("classpath:" + name);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = (cl == null) ? null : cl.getResourceAsStream(name)) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p;
            }
        } catch (IOException ignored) {}
        return null;
    }

    private static Properties tryLoadFromFile(Path path, List<String> tried) {
        tried.add("file:" + path.toAbsolutePath());
        if (path != null && Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                Properties p = new Properties();
                p.load(in);
                return p;
            } catch (IOException ignored) {}
        }
        return null;
    }

    /* =============================== BaseElement property-file scan =============================== */

    /** Walks the class + nested types; returns the first non-blank BaseElement#getPropertyFile(). */
    private static String findPropertyFileFromAnyConstant(Class<?> container) {
        if (container.isEnum()) {
            Object[] constants = container.getEnumConstants();
            if (constants != null) {
                for (Object c : constants) {
                    if (c instanceof BaseElement) {
                        try {
                            String pf = ((BaseElement) c).getPropertyFile();
                            if (pf != null && !pf.isBlank()) return pf;
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        for (Class<?> nested : container.getDeclaredClasses()) {
            String pf = findPropertyFileFromAnyConstant(nested);
            if (pf != null) return pf;
        }
        return null;
    }

    /* =============================== Name helpers =============================== */

    private static String deriveOutputJsonName(String propFile) {
        String base = propFile.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        if (base.endsWith(".properties")) base = base.substring(0, base.length() - ".properties".length());
        if (!base.endsWith(".json")) base = base + ".json";
        return base;
    }

    private static String simple(Class<?> c) { return c.getSimpleName(); }

    /* =============================== Main (quick demo) =============================== */

    public static void main(String[] args) {
        // Optionally: -Dlocators.base=src/main/resources/locators
        Path out = JsonLocatorMigrator.writeResolvedJson(Elements.ManageUsersElements.class);
        System.out.println("Wrote: " + out.toAbsolutePath());
    }
}
