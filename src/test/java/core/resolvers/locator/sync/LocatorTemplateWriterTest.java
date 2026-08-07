package core.resolvers.locator.sync;

import core.resolvers.locator.sync.LocatorTemplateGenerator.LocatorKey;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

@Test(groups = {"integration"})
public class LocatorTemplateWriterTest {

    private Path tempDir;
    private final LocatorTemplateWriter writer = new LocatorTemplateWriter();

    @BeforeClass
    public void createTemp() throws IOException {
        tempDir = Files.createTempDirectory("void-sync-writer-test");
    }

    @AfterClass
    public void deleteTemp() throws IOException {
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    private Path tempFile(String name) { return tempDir.resolve(name); }

    // ── writeNew ────────────────────────────────────────────────────────────────

    @Test
    public void writeNew_createsFileWithHeader() throws IOException {
        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs", "MyPage.Inputs.USERNAME.INPUT")
        );
        Path file = tempFile("new_header.properties");
        writer.writeNew(file, "MyPage", keys);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("# MyPage — locators"), "Expected header comment");
    }

    @Test
    public void writeNew_emitsKeyWithBlankValue() throws IOException {
        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs", "MyPage.Inputs.USERNAME.INPUT")
        );
        Path file = tempFile("new_blank_value.properties");
        writer.writeNew(file, "MyPage", keys);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("MyPage.Inputs.USERNAME.INPUT="),
            "Expected key with empty value; got: " + content);
    }

    @Test
    public void writeNew_groupsConstantsBySection() throws IOException {
        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs",   "P.Inputs.A.INPUT"),
            new LocatorKey("Actions",  "P.Actions.B.TRIGGER")
        );
        Path file = tempFile("new_sections.properties");
        writer.writeNew(file, "P", keys);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("# --- Inputs ---"));
        assertTrue(content.contains("# --- Actions ---"));
        assertTrue(content.indexOf("# --- Inputs ---") < content.indexOf("# --- Actions ---"),
            "Inputs section must come before Actions section");
    }

    @Test
    public void writeNew_createsParentDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c/test.properties");
        writer.writeNew(nested, "P", List.of(new LocatorKey("X", "P.X.Y.INPUT")));
        assertTrue(Files.exists(nested));
    }

    // ── mergeInto ──────────────────────────────────────────────────────────────

    @Test
    public void mergeInto_returnsTrue_whenKeysMissing() throws IOException {
        Path file = tempFile("merge_missing.properties");
        Files.writeString(file, "# --- Inputs ---\nP.Inputs.A.INPUT=//a\n");

        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs", "P.Inputs.A.INPUT"),
            new LocatorKey("Inputs", "P.Inputs.B.INPUT")
        );
        boolean changed = writer.mergeInto(file, keys, Set.of("P.Inputs.A.INPUT"));
        assertTrue(changed, "Expected file to be modified when keys are missing");
    }

    @Test
    public void mergeInto_returnsFalse_whenNoKeysMissing() throws IOException {
        Path file = tempFile("merge_noop.properties");
        Files.writeString(file, "# --- Inputs ---\nP.Inputs.A.INPUT=//a\n");

        List<LocatorKey> keys = List.of(new LocatorKey("Inputs", "P.Inputs.A.INPUT"));
        boolean changed = writer.mergeInto(file, keys, Set.of("P.Inputs.A.INPUT"));
        assertFalse(changed);
    }

    @Test
    public void mergeInto_preservesExistingValues() throws IOException {
        Path file = tempFile("merge_preserve.properties");
        Files.writeString(file, "# --- Inputs ---\nP.Inputs.A.INPUT=//keep-me\n");

        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs", "P.Inputs.A.INPUT"),
            new LocatorKey("Inputs", "P.Inputs.B.INPUT")
        );
        writer.mergeInto(file, keys, Set.of("P.Inputs.A.INPUT"));

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("P.Inputs.A.INPUT=//keep-me"),
            "Existing value must be preserved unchanged");
    }

    @Test
    public void mergeInto_appendsNewKeyInExistingSection() throws IOException {
        Path file = tempFile("merge_append_section.properties");
        Files.writeString(file, "# --- Inputs ---\nP.Inputs.A.INPUT=//a\n\n# --- Actions ---\nP.Actions.B.TRIGGER=//b\n");

        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs",  "P.Inputs.A.INPUT"),
            new LocatorKey("Inputs",  "P.Inputs.C.INPUT"),  // new
            new LocatorKey("Actions", "P.Actions.B.TRIGGER")
        );
        writer.mergeInto(file, keys, Set.of("P.Inputs.A.INPUT", "P.Actions.B.TRIGGER"));

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("P.Inputs.C.INPUT="), "New key must appear in file");
        // New key must appear before the Actions section
        assertTrue(content.indexOf("P.Inputs.C.INPUT=") < content.indexOf("# --- Actions ---"),
            "New Inputs key must be inside the Inputs section, not after Actions");
    }

    @Test
    public void mergeInto_createsNewSectionForUnknownEnum() throws IOException {
        Path file = tempFile("merge_new_section.properties");
        Files.writeString(file, "# --- Inputs ---\nP.Inputs.A.INPUT=//a\n");

        List<LocatorKey> keys = List.of(
            new LocatorKey("Inputs",  "P.Inputs.A.INPUT"),
            new LocatorKey("Actions", "P.Actions.B.TRIGGER")  // new section
        );
        writer.mergeInto(file, keys, Set.of("P.Inputs.A.INPUT"));

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("# --- Actions ---"), "New section header must be added");
        assertTrue(content.contains("P.Actions.B.TRIGGER="), "New key in new section must appear");
    }
}
