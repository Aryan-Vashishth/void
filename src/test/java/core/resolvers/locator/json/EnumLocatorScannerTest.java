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
    public void writeInto_hardcodedEnum_emitsLocatorEntries() {
        ObjectNode node = M.createObjectNode();
        EnumLocatorScanner scanner = new EnumLocatorScanner(new PropertiesIndex());
        // DemoPageElements.Login is hardcoded (no external props file)
        int added = scanner.writeInto(node, DemoPageElements.Login.class);
        assertTrue(added > 0, "Expected at least one locator method discovered; got " + added);
        assertEquals(node.size(), added);
    }

    @Test
    public void writeInto_keyNamesAreDecapitalised() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        node.fieldNames().forEachRemaining(name -> assertTrue(
                Character.isLowerCase(name.charAt(0)),
                "Expected JSON key to start lowercase; got: " + name));
    }

    @Test
    public void writeInto_nonEnumClass_returnsZero() {
        ObjectNode node = M.createObjectNode();
        int added = new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, String.class);
        assertEquals(added, 0);
        assertEquals(node.size(), 0);
    }

    @Test
    public void writeInto_skipsNonLocatorMethods() {
        ObjectNode node = M.createObjectNode();
        new EnumLocatorScanner(new PropertiesIndex()).writeInto(node, DemoPageElements.Login.class);
        // Standard enum/object methods like getDeclaringClass, ordinal, name, getClass should NOT appear
        node.fieldNames().forEachRemaining(name -> {
            assertFalse(name.equals("class"));
            assertFalse(name.equals("declaringClass"));
            assertFalse(name.equals("name"));
        });
    }
}

