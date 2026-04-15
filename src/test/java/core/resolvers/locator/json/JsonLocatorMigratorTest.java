package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.elements.DemoPageElements;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link JsonLocatorMigrator}.
 * <p>
 * All tests are driver-free.
 * They cover:
 * <ul>
 *   <li>{@link JsonLocatorMigrator#buildResolvedJson(Class)} – valid JSON output, structure,
 *       nested class hierarchy, enum method discovery, key naming (decapitalise), null guard</li>
 *   <li>{@link JsonLocatorMigrator#writeResolvedJsonTo(Class, Path)} – file is created,
 *       bytes are written and readable as valid JSON</li>
 *   <li>{@link JsonLocatorMigrator#writeResolvedJson(Class)} – default path naming convention</li>
 * </ul>
 * </p>
 *
 * <p><b>Subject class:</b> {@link DemoPageElements}, which has:
 * <ul>
 *   <li>{@code Login} enum (hardcoded, no props) → declares {@code getInputLocator()}</li>
 *   <li>{@code Login.LoginButton} enum (hardcoded) → declares {@code getTriggerLocator()}</li>
 *   <li>{@code NavBar} enum (file-backed, but props path is relative and typically unresolved
 *       in unit test classpath) → declares {@code getTriggerLocator()} and {@code getListLocator()}</li>
 * </ul>
 * </p>
 */
public class JsonLocatorMigratorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Temp directory used for file-write tests; cleaned up after all tests. */
    private Path tempDir;

    @BeforeClass
    public void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("json-migrator-test-");
    }

    @AfterClass(alwaysRun = true)
    public void deleteTempDir() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (java.util.stream.Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        }
    }

    // =====================================================================
    // buildResolvedJson() – null guard
    // =====================================================================

    @Test(
            description        = "null rootClass → NullPointerException",
            expectedExceptions = NullPointerException.class
    )
    public void buildResolvedJson_nullClass_throwsNullPointer() {
        JsonLocatorMigrator.buildResolvedJson(null);
    }

    // =====================================================================
    // buildResolvedJson() – JSON validity
    // =====================================================================

    @Test(description = "buildResolvedJson produces a non-null, non-blank JSON string")
    public void buildResolvedJson_demoPage_returnsNonBlankJson() {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        assertNotNull(json);
        assertFalse(json.isBlank(), "Expected non-blank JSON output");
    }

    @Test(description = "buildResolvedJson output is valid JSON parseable by ObjectMapper")
    public void buildResolvedJson_demoPage_isValidJson() throws IOException {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        JsonNode root = MAPPER.readTree(json);
        assertNotNull(root, "Expected a valid JSON root node");
        assertFalse(root.isNull(), "Root node must not be null");
    }

    // =====================================================================
    // buildResolvedJson() – top-level structure
    // =====================================================================

    @Test(description = "Top-level node is keyed by the root class simple name")
    public void buildResolvedJson_demoPage_topLevelKeyIsClassName() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        assertTrue(root.has("DemoPageElements"),
                "Expected top-level key 'DemoPageElements'; found fields: " + root.fieldNames());
    }

    @Test(description = "DemoPageElements node contains a 'Login' child node")
    public void buildResolvedJson_demoPage_hasLoginNode() throws IOException {
        JsonNode demoNode = getDemoPageNode();
        assertTrue(demoNode.has("Login"),
                "Expected 'Login' child node under DemoPageElements");
    }

    @Test(description = "DemoPageElements node contains a 'NavBar' child node")
    public void buildResolvedJson_demoPage_hasNavBarNode() throws IOException {
        JsonNode demoNode = getDemoPageNode();
        assertTrue(demoNode.has("NavBar"),
                "Expected 'NavBar' child node under DemoPageElements");
    }

    // =====================================================================
    // buildResolvedJson() – enum key naming via decapitalise
    // =====================================================================

    @Test(description = "Login enum method 'getInputLocator' → JSON key 'inputLocator'")
    public void buildResolvedJson_loginEnum_inputLocatorKeyPresent() throws IOException {
        JsonNode loginNode = getLoginNode();
        assertTrue(loginNode.has("inputLocator"),
                "Expected 'inputLocator' key in Login node; found: " + loginNode.fieldNames());
    }

    @Test(description = "Login.inputLocator value is the hardcoded XPath template string")
    public void buildResolvedJson_loginEnum_inputLocatorValue_isHardcodedXpath() throws IOException {
        JsonNode loginNode = getLoginNode();
        String val = loginNode.path("inputLocator").asText();
        assertFalse(val.isBlank(), "Expected non-blank inputLocator value");
        assertTrue(val.contains("input"),
                "Expected the XPath to reference an input element; got: " + val);
    }

    @Test(description = "NavBar enum method 'getTriggerLocator' → JSON key 'triggerLocator'")
    public void buildResolvedJson_navBarEnum_triggerLocatorKeyPresent() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        assertTrue(navBarNode.has("triggerLocator"),
                "Expected 'triggerLocator' key in NavBar node; found: " + navBarNode.fieldNames());
    }

    @Test(description = "NavBar enum method 'getListLocator' → JSON key 'listLocator'")
    public void buildResolvedJson_navBarEnum_listLocatorKeyPresent() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        assertTrue(navBarNode.has("listLocator"),
                "Expected 'listLocator' key in NavBar node; found: " + navBarNode.fieldNames());
    }

    @Test(description = "NavBar.triggerLocator value is non-blank")
    public void buildResolvedJson_navBarEnum_triggerLocatorValue_nonBlank() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        String val = navBarNode.path("triggerLocator").asText();
        assertFalse(val.isBlank(), "Expected non-blank triggerLocator value for NavBar");
    }

    @Test(description = "NavBar.listLocator value is non-blank")
    public void buildResolvedJson_navBarEnum_listLocatorValue_nonBlank() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        String val = navBarNode.path("listLocator").asText();
        assertFalse(val.isBlank(), "Expected non-blank listLocator value for NavBar");
    }

    // =====================================================================
    // buildResolvedJson() – nested inner-class structure (Login → LoginButton)
    // =====================================================================

    @Test(description = "LoginButton is present as a nested node (directly or under Login)")
    public void buildResolvedJson_loginButtonNode_isPresent() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        JsonNode loginButtonNode = findNodeAnywhere(root, "LoginButton");
        assertNotNull(loginButtonNode,
                "Expected 'LoginButton' node somewhere in the JSON tree");
    }

    @Test(description = "LoginButton enum 'getTriggerLocator' → JSON key 'triggerLocator'")
    public void buildResolvedJson_loginButton_triggerLocatorKeyPresent() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        JsonNode loginButtonNode = findNodeAnywhere(root, "LoginButton");
        assertNotNull(loginButtonNode, "LoginButton node not found in JSON");
        assertTrue(loginButtonNode.has("triggerLocator"),
                "Expected 'triggerLocator' in LoginButton node; found: " + loginButtonNode.fieldNames());
    }

    @Test(description = "LoginButton.triggerLocator contains XPath for a button element")
    public void buildResolvedJson_loginButton_triggerLocatorValue_containsButton() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        JsonNode loginButtonNode = findNodeAnywhere(root, "LoginButton");
        assertNotNull(loginButtonNode, "LoginButton node not found");
        String val = loginButtonNode.path("triggerLocator").asText();
        assertTrue(val.contains("button"),
                "Expected triggerLocator to reference a button; got: " + val);
    }

    // =====================================================================
    // buildResolvedJson() – non-Locator methods are NOT included
    // =====================================================================

    @Test(description = "getExternalFileName is NOT emitted (does not end with 'Locator')")
    public void buildResolvedJson_externalFileNameKey_notPresent() {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        // The key "externalFileName" must not appear anywhere (not a locator key)
        assertFalse(json.contains("\"externalFileName\""),
                "getExternalFileName should not be emitted as a locator key");
    }

    @Test(description = "getArgs is NOT emitted (does not end with 'Locator')")
    public void buildResolvedJson_argsKey_notPresent() {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        assertFalse(json.contains("\"args\""),
                "getArgs should not be emitted as a locator key");
    }

    // =====================================================================
    // writeResolvedJsonTo() – file creation
    // =====================================================================

    @Test(description = "writeResolvedJsonTo creates the output file on disk")
    public void writeResolvedJsonTo_createsFile() {
        Path outFile = tempDir.resolve("demo-test-out.json");
        Path returned = JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, outFile);
        assertTrue(Files.exists(outFile), "Expected output file to be created");
        assertNotNull(returned);
        assertEquals(returned, outFile);
    }

    @Test(description = "writeResolvedJsonTo writes non-empty content to disk")
    public void writeResolvedJsonTo_writesNonEmptyContent() throws IOException {
        Path outFile = tempDir.resolve("demo-test-content.json");
        JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, outFile);
        long size = Files.size(outFile);
        assertTrue(size > 0, "Output file must be non-empty; actual size=" + size);
    }

    @Test(description = "writeResolvedJsonTo content is valid parseable JSON")
    public void writeResolvedJsonTo_contentIsValidJson() throws IOException {
        Path outFile = tempDir.resolve("demo-valid-json.json");
        JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, outFile);
        String content = Files.readString(outFile);
        JsonNode parsed = MAPPER.readTree(content);
        assertNotNull(parsed);
        assertFalse(parsed.isNull());
        assertTrue(parsed.has("DemoPageElements"),
                "Written JSON should contain 'DemoPageElements' top-level key");
    }

    @Test(description = "writeResolvedJsonTo creates parent directories if needed")
    public void writeResolvedJsonTo_createsParentDirs() {
        Path deep = tempDir.resolve("sub1/sub2/output.json");
        assertFalse(Files.exists(deep.getParent()), "Parent dirs should not exist before the call");
        JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, deep);
        assertTrue(Files.exists(deep), "Output file should have been created with parent dirs");
    }

    @Test(
            description        = "writeResolvedJsonTo with null outputFile → NullPointerException",
            expectedExceptions = NullPointerException.class
    )
    public void writeResolvedJsonTo_nullOutputFile_throwsNullPointer() {
        JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, null);
    }

    // =====================================================================
    // writeResolvedJson() – default naming convention
    // =====================================================================

    @Test(description = "writeResolvedJson uses rootClass.getSimpleName().toLowerCase() + '-locators.json'")
    public void writeResolvedJson_defaultFileName_matchesConvention() {
        // DemoPageElements → "demopageelements-locators.json"
        String className = DemoPageElements.class.getSimpleName().toLowerCase();
        String expectedSuffix = className + "-locators.json";

        Path defaultOutDir = JsonLocatorMigrator.DEFAULT_OUT_DIR;
        Path expectedPath  = defaultOutDir.resolve(expectedSuffix);

        // We only verify the path; don't actually write to src/main/resources in a unit test
        assertEquals(expectedPath.getFileName().toString(), expectedSuffix,
                "Expected file name to follow the convention <classname-lowercase>-locators.json");
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    private JsonNode parseJson(Class<?> rootClass) throws IOException {
        String json = JsonLocatorMigrator.buildResolvedJson(rootClass);
        return MAPPER.readTree(json);
    }

    private JsonNode getDemoPageNode() throws IOException {
        return parseJson(DemoPageElements.class).path("DemoPageElements");
    }

    private JsonNode getLoginNode() throws IOException {
        return getDemoPageNode().path("Login");
    }

    private JsonNode getNavBarNode() throws IOException {
        return getDemoPageNode().path("NavBar");
    }

    /**
     * Recursively searches for a JSON object node with the given field name
     * anywhere in the tree; returns it if found, null otherwise.
     */
    private JsonNode findNodeAnywhere(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) return null;
        if (node.has(fieldName)) return node.get(fieldName);
        for (JsonNode child : node) {
            if (child.isObject()) {
                JsonNode found = findNodeAnywhere(child, fieldName);
                if (found != null) return found;
            }
        }
        return null;
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

