package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static core.logging.CustomLogger.debug;

/**
 * Recursive class-tree walker that produces a Jackson {@link ObjectNode} mirroring the
 * nested-enum structure of a root class. For each enum encountered, locator entries are
 * emitted via {@link EnumLocatorScanner}; properties files are loaded once each via the
 * shared {@link PropertiesIndex}.
 *
 * <p>Extracted from the monolithic {@code JsonLocatorMigrator} as part of the Phase&nbsp;5
 * SRP split — this class knows only about <em>tree shape</em>; it does not load properties,
 * scan enums, write files, or parse CLI arguments.</p>
 */
public final class JsonTreeBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PropertiesIndex     propertiesIndex;
    private final EnumLocatorScanner  scanner;

    /** Default constructor: fresh {@link PropertiesIndex} per builder. */
    public JsonTreeBuilder() {
        this(new PropertiesIndex());
    }

    /** Inject a custom {@link PropertiesIndex} (useful for shared caches across builds, or tests). */
    public JsonTreeBuilder(PropertiesIndex propertiesIndex) {
        this.propertiesIndex = propertiesIndex;
        this.scanner         = new EnumLocatorScanner(propertiesIndex);
    }

    public PropertiesIndex propertiesIndex() { return propertiesIndex; }

    /** Build the JSON tree for {@code rootClass}. */
    public ObjectNode build(Class<?> rootClass) {
        Objects.requireNonNull(rootClass, "rootClass must not be null");
        long startNs = System.nanoTime();
        debug.log("[migrate:start] root=" + rootClass.getSimpleName());

        ObjectNode root = MAPPER.createObjectNode();
        buildClassInto(root, rootClass);

        debug.log("[migrate:done] root=" + rootClass.getSimpleName()
                + " ms=" + (System.nanoTime() - startNs) / 1_000_000L
                + " topNodes=" + countFields(root)
                + " propsLoaded=" + propertiesIndex.size());
        return root;
    }

    // ---- internal -----------------------------------------------------------

    /** Recursively converts the class and nested enums into JSON nodes. */
    private void buildClassInto(ObjectNode current, Class<?> clazz) {
        long start = System.nanoTime();
        ObjectNode thisNode = current.putObject(clazz.getSimpleName());

        int locators = clazz.isEnum() ? scanner.writeInto(thisNode, clazz) : 0;

        int nested = 0;
        for (Class<?> nestedClass : nestedInSourceOrder(clazz)) {
            if (nestedClass.isSynthetic() || nestedClass.getName().contains("$$")) continue;
            buildClassInto(thisNode, nestedClass);
            nested++;
        }

        debug.log("[class] name=" + clazz.getSimpleName()
                + " enum=" + clazz.isEnum()
                + " locators=" + locators
                + " nested=" + nested
                + " ms=" + (System.nanoTime() - start) / 1_000_000L);
    }

    /** Preserve approximate source order (the JVM tends to return declared classes reversed). */
    private static List<Class<?>> nestedInSourceOrder(Class<?> clazz) {
        List<Class<?>> list = new ArrayList<>(Arrays.asList(clazz.getDeclaredClasses()));
        Collections.reverse(list);
        return list;
    }

    private static int countFields(ObjectNode node) {
        if (node == null) return 0;
        int c = 0;
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) { c++; it.next(); }
        return c;
    }
}

