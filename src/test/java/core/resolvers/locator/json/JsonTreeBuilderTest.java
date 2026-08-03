package core.resolvers.locator.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.elements.DemoPageElements;
import domain.automation.web.resolve.json.JsonTreeBuilder;
import domain.automation.web.resolve.json.PropertiesIndex;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link JsonTreeBuilder}. */
public class JsonTreeBuilderTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void build_nullRoot_throws() {
        new JsonTreeBuilder().build(null);
    }

    @Test
    public void build_producesTopLevelNodeNamedAfterRootClass() {
        ObjectNode root = new JsonTreeBuilder().build(DemoPageElements.class);
        assertNotNull(root);
        assertTrue(root.has(DemoPageElements.class.getSimpleName()),
                "Expected top-level field for the root class; got: " + root.fieldNames().next());
        assertEquals(root.size(), 1);
    }

    @Test
    public void build_recursesIntoNestedEnums() {
        ObjectNode root = new JsonTreeBuilder().build(DemoPageElements.class);
        ObjectNode demo = (ObjectNode) root.get(DemoPageElements.class.getSimpleName());
        assertNotNull(demo);
        // DemoPageElements has nested Login enum that itself has a nested LoginButton enum
        assertTrue(demo.has("Login"), "Expected nested Login node");
    }

    @Test
    public void build_sharesPropertiesIndexAcrossInvocations() {
        PropertiesIndex shared = new PropertiesIndex();
        JsonTreeBuilder b = new JsonTreeBuilder(shared);

        b.build(DemoPageElements.class);
        int afterFirst = shared.size();
        b.build(DemoPageElements.class);
        int afterSecond = shared.size();

        // Second build must not reload the same .properties file.
        assertEquals(afterFirst, afterSecond,
                "PropertiesIndex should be reused across builds without growing");
    }

    @Test
    public void propertiesIndex_isAccessibleForInspection() {
        JsonTreeBuilder b = new JsonTreeBuilder();
        assertNotNull(b.propertiesIndex());
        assertEquals(b.propertiesIndex().size(), 0, "Fresh builder should have an empty index");
    }
}

