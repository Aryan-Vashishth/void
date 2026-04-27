package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.elements.DemoPageElements;
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
        // Login enum constants have hardcoded getInputLocator() returning an xpath template
        String val = node.path("USERNAME").asText();
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
}

