package domain.automation.web.resolve.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link JsonNodeLookup}. */
public class JsonNodeLookupTest {

    private static final ObjectMapper M = new ObjectMapper();

    private static JsonNode tree() throws Exception {
        return M.readTree(
                "{ \"a\": { \"b\": { \"c\": \"deep\" } }, " +
                "  \"FLAT\": \"flat-value\", " +
                "  \"sibling\": { \"NESTED\": \"nested-value\" }, " +
                "  \"numericNode\": 123 }");
    }

    @Test
    public void findByDotPath_walksTree() throws Exception {
        JsonNode n = JsonNodeLookup.findByDotPath(tree(), "a.b.c");
        assertNotNull(n);
        assertEquals(n.asText(), "deep");
    }

    @Test
    public void findByDotPath_missingNodeReturnsNull() throws Exception {
        assertNull(JsonNodeLookup.findByDotPath(tree(), "a.b.MISSING"));
        assertNull(JsonNodeLookup.findByDotPath(tree(), "x.y.z"));
    }

    @Test
    public void findByDotPath_blankInputsReturnNull() throws Exception {
        assertNull(JsonNodeLookup.findByDotPath(tree(), ""));
        assertNull(JsonNodeLookup.findByDotPath(tree(), null));
        assertNull(JsonNodeLookup.findByDotPath(null, "a.b"));
    }

    @Test
    public void deepFindField_findsNestedField() throws Exception {
        JsonNode n = JsonNodeLookup.deepFindField(tree(), "NESTED");
        assertNotNull(n);
        assertEquals(n.asText(), "nested-value");
    }

    @Test
    public void deepFindField_missingReturnsNull() throws Exception {
        assertNull(JsonNodeLookup.deepFindField(tree(), "TOTALLY_ABSENT"));
    }

    @Test
    public void findText_dotPathFirst() throws Exception {
        assertEquals(JsonNodeLookup.findText(tree(), "a.b.c"), "deep");
    }

    @Test
    public void findText_fallsBackToDeepFind() throws Exception {
        // "NESTED" is not at root, so dot-path fails → deep-find succeeds
        assertEquals(JsonNodeLookup.findText(tree(), "NESTED"), "nested-value");
    }

    @Test
    public void findText_returnsNullForNonTextualNode() throws Exception {
        assertNull(JsonNodeLookup.findText(tree(), "numericNode"));
    }

    @Test
    public void findText_returnsNullForMissing() throws Exception {
        assertNull(JsonNodeLookup.findText(tree(), "no.such.path"));
    }
}

