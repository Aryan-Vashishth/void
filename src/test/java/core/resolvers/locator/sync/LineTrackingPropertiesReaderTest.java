package core.resolvers.locator.sync;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.testng.Assert.*;

@Test(groups = {"integration"})
public class LineTrackingPropertiesReaderTest {

    private LineTrackingPropertiesReader readerFor(String... lines) throws IOException {
        Path tmp = Files.createTempFile("ltpr-test", ".properties");
        Files.writeString(tmp, String.join("\n", lines));
        LineTrackingPropertiesReader reader = new LineTrackingPropertiesReader();
        reader.load(tmp);
        Files.delete(tmp);
        return reader;
    }

    // ---------- isEmpty ----------

    @Test
    public void isEmpty_noEntries_returnsTrue() throws IOException {
        LineTrackingPropertiesReader reader = readerFor();
        assertTrue(reader.isEmpty());
    }

    @Test
    public void isEmpty_withEntry_returnsFalse() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("key=value");
        assertFalse(reader.isEmpty());
    }

    // ---------- basic load ----------

    @Test
    public void load_singleEntry_keyAndValueParsedCorrectly() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("SomePage.Fields.NAME.INPUT=//input");
        assertTrue(reader.contains("SomePage.Fields.NAME.INPUT"));
        assertEquals(reader.getValue("SomePage.Fields.NAME.INPUT"), "//input");
    }

    @Test
    public void load_multipleEntries_allPresent() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "PageA.Foo.BAR.INPUT=//x",
            "PageA.Foo.BAZ.TRIGGER=//y"
        );
        assertTrue(reader.contains("PageA.Foo.BAR.INPUT"));
        assertTrue(reader.contains("PageA.Foo.BAZ.TRIGGER"));
    }

    // ---------- skipped lines ----------

    @Test
    public void load_commentLines_notAddedToMap() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "# this is a comment",
            "real.key=real value"
        );
        assertFalse(reader.contains("# this is a comment"));
        assertTrue(reader.contains("real.key"));
    }

    @Test
    public void load_emptyLines_notAddedToMap() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "",
            "   ",
            "key=value"
        );
        assertEquals(reader.allLineNumbers().size(), 1);
    }

    @Test
    public void load_lineWithoutEquals_skipped() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "this-has-no-equals-sign",
            "valid=entry"
        );
        assertFalse(reader.contains("this-has-no-equals-sign"));
        assertTrue(reader.contains("valid"));
    }

    @Test
    public void load_lineWithEmptyKey_skipped() throws IOException {
        // "=value" has an empty key after trimming
        LineTrackingPropertiesReader reader = readerFor("=orphan-value");
        assertTrue(reader.isEmpty());
    }

    // ---------- whitespace trimming ----------

    @Test
    public void load_whitespaceAroundKey_trimmed() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("  spaced.key  =value");
        assertTrue(reader.contains("spaced.key"));
    }

    @Test
    public void load_whitespaceAroundValue_trimmed() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("key=  trimmed value  ");
        assertEquals(reader.getValue("key"), "trimmed value");
    }

    @Test
    public void load_valueWithEqualsSign_onlyFirstEqualsSplits() throws IOException {
        // "key=a=b" — value is "a=b"
        LineTrackingPropertiesReader reader = readerFor("key=a=b");
        assertEquals(reader.getValue("key"), "a=b");
    }

    // ---------- line numbers ----------

    @Test
    public void load_lineNumbers_oneBasedAndCorrect() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "# comment line 1",
            "",
            "first.key=val1",     // line 3
            "second.key=val2"     // line 4
        );
        assertEquals(reader.getLineNumber("first.key"), 3);
        assertEquals(reader.getLineNumber("second.key"), 4);
    }

    @Test
    public void getLineNumber_missingKey_returnsNegativeOne() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("key=value");
        assertEquals(reader.getLineNumber("missing.key"), -1);
    }

    // ---------- allLineNumbers ----------

    @Test
    public void allLineNumbers_returnsUnmodifiableView() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("k=v");
        Map<String, Integer> view = reader.allLineNumbers();
        assertThrows(UnsupportedOperationException.class, () -> view.put("extra", 99));
    }

    @Test
    public void allLineNumbers_preservesInsertionOrder() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "a=1",
            "b=2",
            "c=3"
        );
        String[] keys = reader.allLineNumbers().keySet().toArray(new String[0]);
        assertEquals(keys, new String[]{"a", "b", "c"});
    }
}
