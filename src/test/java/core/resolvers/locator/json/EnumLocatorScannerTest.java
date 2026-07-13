package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.elements.DemoPageElements;
import elements.fixture.ConventionalPropsPage;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link EnumLocatorScanner}. */
public class EnumLocatorScannerTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    public void writeInto_hardcodedEnum_emitsOneEntryPerConstant() {
        ObjectNode node = M.createObjectNode();
        EnumLocatorScanner scanner = new EnumLocatorScanner(new PropertiesIndex());
        // DemoPageElements.Login has 2 constants: USERNAME, PASSWORD
        int added = scanner.writeInto(node, DemoPageElements.Login.class);
        assertTrue(added > 0, "Expected at least one entry per constant; got " + added);
        assertEquals(node.size(), 2, "Expected one entry per Login constant (USERNAME, PASSWORD)");
    }

    @Test
    public void writeInto_keysAreEnumConstantNames() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        assertTrue(node.has("USERNAME"), "Expected 'USERNAME' key from Login enum constant");
        assertTrue(node.has("PASSWORD"), "Expected 'PASSWORD' key from Login enum constant");
    }

    @Test
    public void writeInto_nonEnumClass_returnsZero() {
        ObjectNode node = M.createObjectNode();
        int added = new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, String.class);
        assertEquals(added, 0);
        assertEquals(node.size(), 0);
    }

    @Test
    public void writeInto_valuesAreLocatorStrings() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        // Login enum constants have hardcoded getInputLocator() — emitted as nested role object
        // Phase 19 Part B: scanner always emits { "CONSTANT": { "ROLE": "value" } }
        String val = node.path("USERNAME").path("INPUT").asText();
        assertFalse(val.isBlank(), "Expected non-blank locator value for USERNAME");
        assertTrue(val.contains("input"), "Expected XPath referencing an input element; got: " + val);
    }

    @Test
    public void writeInto_skipsNonElementConstants() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        // Only Element constants should appear; standard enum/object fields should NOT
        node.fieldNames().forEachRemaining(name -> {
            assertFalse(name.equals("class"));
            assertFalse(name.equals("declaringClass"));
            assertFalse(name.equals("name"));
        });
    }

    // =====================================================================
    // Phase 7 — conventional properties path resolution
    // =====================================================================

    @Test(description = "Scanner probes conventional .properties path and resolves locators from it")
    public void writeInto_conventionalPropertiesPath_resolvesXpathFromProperties() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, ConventionalPropsPage.Fields.class);
        // Phase 19 Part B: keys in locators.properties use the fully-qualified format
        // ConventionalPropsPage.Fields.EMAIL_INPUT.INPUT=//input[@type='email']
        // Scanner resolves them and emits nested role objects.
        assertEquals(node.path("EMAIL_INPUT").path("INPUT").asText(), "//input[@type='email']",
                "Expected EMAIL_INPUT to resolve from conventional properties path");
        assertEquals(node.path("PHONE_INPUT").path("INPUT").asText(), "//input[@type='tel']",
                "Expected PHONE_INPUT to resolve from conventional properties path");
    }

    @Test(description = "Scanner emits nested role object when no properties file exists for the page")
    public void writeInto_noProperties_emitsRawConstantName() {
        // DemoPageElements.Login has hardcoded locators; its enclosing class has no .properties file
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        // Phase 19 Part B: emitted as { "USERNAME": { "INPUT": "rawLocatorKey" } }
        String usernameVal = node.path("USERNAME").path("INPUT").asText();
        assertFalse(usernameVal.isBlank(),
                "Expected a non-blank locator value even without a properties file");
    }
}

