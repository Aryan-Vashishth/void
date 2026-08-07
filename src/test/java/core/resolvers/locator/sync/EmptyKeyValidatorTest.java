package core.resolvers.locator.sync;

import core.resolvers.locator.sync.EmptyKeyValidator.EmptyKeyError;
import core.resolvers.locator.sync.LocatorTemplateGenerator.LocatorKey;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

@Test(groups = {"integration"})
public class EmptyKeyValidatorTest {

    private final EmptyKeyValidator validator = new EmptyKeyValidator();

    private LineTrackingPropertiesReader readerFor(String... lines) throws IOException {
        Path tmp = Files.createTempFile("empty-key-test", ".properties");
        Files.writeString(tmp, String.join("\n", lines));
        LineTrackingPropertiesReader reader = new LineTrackingPropertiesReader();
        reader.load(tmp);
        Files.delete(tmp);
        return reader;
    }

    @Test
    public void noErrors_whenAllKeysHaveValues() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("P.X.A.INPUT=//input");
        List<LocatorKey> expected = List.of(new LocatorKey("X", "P.X.A.INPUT"));
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void error_whenKeyHasEmptyValue() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("P.X.A.INPUT=");
        List<LocatorKey> expected = List.of(new LocatorKey("X", "P.X.A.INPUT"));
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertEquals(errors.size(), 1);
        assertEquals(errors.get(0).key(), "P.X.A.INPUT");
    }

    @Test
    public void error_whenKeyIsAbsent_lineNumberIsMinusOne() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("P.X.OTHER.INPUT=//x");
        List<LocatorKey> expected = List.of(new LocatorKey("X", "P.X.MISSING.INPUT"));
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertEquals(errors.size(), 1);
        assertEquals(errors.get(0).lineNumber(), -1, "Absent key has no line number");
    }

    @Test
    public void error_lineNumberMatchesActualLine() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "# comment",
            "P.X.A.INPUT=//a",
            "P.X.B.INPUT="    // blank — line 3
        );
        List<LocatorKey> expected = List.of(
            new LocatorKey("X", "P.X.A.INPUT"),
            new LocatorKey("X", "P.X.B.INPUT")
        );
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertEquals(errors.size(), 1);
        assertEquals(errors.get(0).key(), "P.X.B.INPUT");
        assertEquals(errors.get(0).lineNumber(), 3);
    }

    @Test
    public void error_blankValueTreatedAsEmpty() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("P.X.A.INPUT=   ");
        List<LocatorKey> expected = List.of(new LocatorKey("X", "P.X.A.INPUT"));
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertEquals(errors.size(), 1, "Whitespace-only value must be treated as empty");
    }

    @Test
    public void multipleErrors_reportedInOrder() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "P.X.A.INPUT=",
            "P.X.B.INPUT=",
            "P.X.C.INPUT=//filled"
        );
        List<LocatorKey> expected = List.of(
            new LocatorKey("X", "P.X.A.INPUT"),
            new LocatorKey("X", "P.X.B.INPUT"),
            new LocatorKey("X", "P.X.C.INPUT")
        );
        List<EmptyKeyError> errors = validator.validate(expected, reader);
        assertEquals(errors.size(), 2);
        assertEquals(errors.get(0).key(), "P.X.A.INPUT");
        assertEquals(errors.get(1).key(), "P.X.B.INPUT");
    }
}
