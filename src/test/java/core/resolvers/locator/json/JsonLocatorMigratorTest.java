package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.elements.DemoPageElements;
import domain.automation.web.resolve.api.ConventionalLocatorPath;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
@Test(groups = {"integration"})
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
    // buildResolvedJson() – enum constant names as JSON keys
    // =====================================================================

    @Test(description = "Login enum constant 'USERNAME' is present as JSON key")
    public void buildResolvedJson_loginEnum_usernameKeyPresent() throws IOException {
        JsonNode loginNode = getLoginNode();
        assertTrue(loginNode.has("USERNAME"),
                "Expected 'USERNAME' key in Login node; found: " + loginNode.fieldNames());
    }

    @Test(description = "Login enum constant 'PASSWORD' is present as JSON key")
    public void buildResolvedJson_loginEnum_passwordKeyPresent() throws IOException {
        JsonNode loginNode = getLoginNode();
        assertTrue(loginNode.has("PASSWORD"),
                "Expected 'PASSWORD' key in Login node; found: " + loginNode.fieldNames());
    }

    @Test(description = "Login.USERNAME nested INPUT role contains the hardcoded XPath template")
    public void buildResolvedJson_loginEnum_usernameValue_isHardcodedXpath() throws IOException {
        JsonNode loginNode = getLoginNode();
        // Phase 19 Part B: scanner always emits { "CONSTANT": { "ROLE": "value" } }
        String val = loginNode.path("USERNAME").path("INPUT").asText();
        assertFalse(val.isBlank(), "Expected non-blank USERNAME.INPUT value");
        assertTrue(val.contains("input"),
                "Expected the XPath to reference an input element; got: " + val);
    }

    @Test(description = "NavBar enum constant 'PARTNER' is present as JSON key")
    public void buildResolvedJson_navBarEnum_partnerKeyPresent() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        assertTrue(navBarNode.has("PARTNER"),
                "Expected 'PARTNER' key in NavBar node; found: " + navBarNode.fieldNames());
    }

    @Test(description = "NavBar enum constant 'VENDOR' is present as JSON key")
    public void buildResolvedJson_navBarEnum_vendorKeyPresent() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        assertTrue(navBarNode.has("VENDOR"),
                "Expected 'VENDOR' key in NavBar node; found: " + navBarNode.fieldNames());
    }

    @Test(description = "NavBar.PARTNER has TRIGGER role with non-blank value")
    public void buildResolvedJson_navBarEnum_partnerTriggerValue_nonBlank() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        JsonNode partnerNode = navBarNode.path("PARTNER");
        // Multi-role (Dropdown): emitted as { "TRIGGER": "…", "LIST": "…" }
        assertTrue(partnerNode.has("TRIGGER"),
                "Expected 'TRIGGER' role under PARTNER; found: " + partnerNode);
        assertFalse(partnerNode.path("TRIGGER").asText().isBlank(),
                "Expected non-blank TRIGGER value for PARTNER");
    }

    @Test(description = "NavBar.PARTNER has LIST role with non-blank value")
    public void buildResolvedJson_navBarEnum_partnerListValue_nonBlank() throws IOException {
        JsonNode navBarNode = getNavBarNode();
        JsonNode partnerNode = navBarNode.path("PARTNER");
        assertTrue(partnerNode.has("LIST"),
                "Expected 'LIST' role under PARTNER; found: " + partnerNode);
        assertFalse(partnerNode.path("LIST").asText().isBlank(),
                "Expected non-blank LIST value for PARTNER");
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

    @Test(description = "LoginButton enum constant 'SUBMIT' → JSON key 'SUBMIT'")
    public void buildResolvedJson_loginButton_submitKeyPresent() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        JsonNode loginButtonNode = findNodeAnywhere(root, "LoginButton");
        assertNotNull(loginButtonNode, "LoginButton node not found in JSON");
        assertTrue(loginButtonNode.has("SUBMIT"),
                "Expected 'SUBMIT' in LoginButton node; found: " + loginButtonNode.fieldNames());
    }

    @Test(description = "LoginButton.SUBMIT nested TRIGGER role contains XPath for a button element")
    public void buildResolvedJson_loginButton_submitValue_containsButton() throws IOException {
        JsonNode root = parseJson(DemoPageElements.class);
        JsonNode loginButtonNode = findNodeAnywhere(root, "LoginButton");
        assertNotNull(loginButtonNode, "LoginButton node not found");
        // Phase 19 Part B: scanner always emits { "CONSTANT": { "ROLE": "value" } }
        String val = loginButtonNode.path("SUBMIT").path("TRIGGER").asText();
        assertTrue(val.contains("button"),
                "Expected SUBMIT.TRIGGER to reference a button; got: " + val);
    }

    // =====================================================================
    // buildResolvedJson() – non-Element keys are NOT included
    // =====================================================================

    @Test(description = "getExternalFileName is NOT emitted (not an enum constant name)")
    public void buildResolvedJson_externalFileNameKey_notPresent() {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        // The key "externalFileName" must not appear anywhere (not an enum constant name)
        assertFalse(json.contains("\"externalFileName\""),
                "getExternalFileName should not be emitted as a key");
    }

    @Test(description = "getArgs is NOT emitted (not an enum constant name)")
    public void buildResolvedJson_argsKey_notPresent() {
        String json = JsonLocatorMigrator.buildResolvedJson(DemoPageElements.class);
        assertFalse(json.contains("\"args\""),
                "getArgs should not be emitted as a key");
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
    // Phase 7 — writeResolvedJsonConventional()
    // =====================================================================

    @Test(description = "writeResolvedJsonConventional target path follows pkg/ClassName/locators.json")
    public void writeResolvedJsonConventional_pathFollowsConvention() {
        String cpRelPath = ConventionalLocatorPath.forClass(DemoPageElements.class);
        Path expectedOutput = Paths.get("src/main/resources").resolve(cpRelPath);
        String normalised = expectedOutput.toString().replace('\\', '/');
        assertTrue(normalised.endsWith("DemoPageElements/locators.json"),
                "Expected path to end with 'DemoPageElements/locators.json'; got: " + normalised);
    }

    @Test(description = "writeResolvedJsonTo with conventional path in tempDir produces valid JSON")
    public void writeResolvedJsonTo_conventionalPath_producesValidJson() throws IOException {
        String cpRelPath = ConventionalLocatorPath.forClass(DemoPageElements.class);
        Path outFile = tempDir.resolve(cpRelPath);
        Path returned = JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, outFile);
        assertTrue(Files.exists(returned), "Expected output file to be created");
        JsonNode parsed = MAPPER.readTree(Files.readString(returned));
        assertNotNull(parsed);
        assertTrue(parsed.has("DemoPageElements"),
                "Written JSON should contain 'DemoPageElements' top-level key");
    }

    @Test(description = "writeResolvedJsonTo with conventional path places file inside pkg/ClassName/ dir")
    public void writeResolvedJsonTo_conventionalPath_fileIsInsideClassNameDir() throws IOException {
        String cpRelPath = ConventionalLocatorPath.forClass(DemoPageElements.class);
        Path outFile = tempDir.resolve(cpRelPath);
        Path returned = JsonLocatorMigrator.writeResolvedJsonTo(DemoPageElements.class, outFile);
        String normalised = returned.toString().replace('\\', '/');
        assertTrue(normalised.contains("DemoPageElements/locators.json"),
                "Conventional path must contain 'ClassName/locators.json'; got: " + normalised);
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

