package core.resolvers.locator.sync;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.testng.Assert.*;

public class LocatorSyncOrchestratorTest {

    private Path tempBase;
    private final LocatorSyncOrchestrator orchestrator = new LocatorSyncOrchestrator();

    @BeforeClass
    public void setup() throws IOException {
        tempBase = Files.createTempDirectory("void-sync-test");
    }

    @AfterClass
    public void cleanup() throws IOException {
        Files.walk(tempBase).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    @Test
    public void newPage_createsPropertiesFile() throws IOException {
        Path base = tempBase.resolve("new_page");
        int code = orchestrator.sync(SyncTestFixturePage.class, false, base);

        // All keys are blank → exit EMPTY_KEYS (1)
        assertEquals(code, LocatorSyncOrchestrator.EXIT_EMPTY_KEYS);

        // File must exist at conventional path
        Path propsFile = base.resolve(
            SyncTestFixturePage.class.getName().replace('.', '/') + "/locators.properties"
        );
        assertTrue(Files.exists(propsFile), "Properties file must be created at conventional path");
    }

    @Test
    public void newPage_fileContainsAllExpectedKeys() throws IOException {
        Path base = tempBase.resolve("key_check");
        orchestrator.sync(SyncTestFixturePage.class, false, base);

        Path propsFile = base.resolve(
            SyncTestFixturePage.class.getName().replace('.', '/') + "/locators.properties"
        );
        String content = Files.readString(propsFile, StandardCharsets.UTF_8);

        assertTrue(content.contains("SyncTestFixturePage.Inputs.USERNAME.INPUT="), content);
        assertTrue(content.contains("SyncTestFixturePage.Inputs.EMAIL.INPUT="), content);
        assertTrue(content.contains("SyncTestFixturePage.Actions.SUBMIT.TRIGGER="), content);
        assertTrue(content.contains("SyncTestFixturePage.Actions.CANCEL.TRIGGER="), content);
        assertTrue(content.contains("SyncTestFixturePage.Labels.ERROR_MSG.TEXT="), content);
        // Selectable → two roles
        assertTrue(content.contains("SyncTestFixturePage.Dropdowns.COUNTRY.TRIGGER="), content);
        assertTrue(content.contains("SyncTestFixturePage.Dropdowns.COUNTRY.LIST="), content);
    }

    @Test
    public void idempotent_secondRunAddsNoKeys() throws IOException {
        Path base = tempBase.resolve("idempotent");

        // First run — creates file with blank values
        orchestrator.sync(SyncTestFixturePage.class, false, base);

        Path propsFile = base.resolve(
            SyncTestFixturePage.class.getName().replace('.', '/') + "/locators.properties"
        );
        long sizeAfterFirstRun = Files.size(propsFile);

        // Second run — file already exists with same keys
        orchestrator.sync(SyncTestFixturePage.class, false, base);
        long sizeAfterSecondRun = Files.size(propsFile);

        assertEquals(sizeAfterSecondRun, sizeAfterFirstRun,
            "Second sync must not add duplicate keys or modify file size");
    }

    @Test
    public void existingFile_preservesFilledValues() throws IOException {
        Path base = tempBase.resolve("preserve");
        Path propsFile = base.resolve(
            SyncTestFixturePage.class.getName().replace('.', '/') + "/locators.properties"
        );

        // Pre-create file with one key filled
        Files.createDirectories(propsFile.getParent());
        Files.writeString(propsFile,
            "# --- Inputs ---\n" +
            "SyncTestFixturePage.Inputs.USERNAME.INPUT=//input[@id='user']\n"
        );

        orchestrator.sync(SyncTestFixturePage.class, false, base);

        String content = Files.readString(propsFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("SyncTestFixturePage.Inputs.USERNAME.INPUT=//input[@id='user']"),
            "Pre-filled value must be preserved");
    }

    @Test
    public void emptyKeys_exitCodeIsOne() throws IOException {
        Path base = tempBase.resolve("empty_keys_exit");
        int code = orchestrator.sync(SyncTestFixturePage.class, false, base);
        assertEquals(code, LocatorSyncOrchestrator.EXIT_EMPTY_KEYS,
            "Exit code must be 1 when any locator value is blank");
    }

    @Test
    public void missingKey_mergedOnSecondRunAfterEnumExpanded() throws IOException {
        Path base = tempBase.resolve("merge_key");
        Path propsFile = base.resolve(
            SyncTestFixturePage.class.getName().replace('.', '/') + "/locators.properties"
        );

        // Simulate pre-existing file that is missing a key
        Files.createDirectories(propsFile.getParent());
        Files.writeString(propsFile,
            "# --- Inputs ---\n" +
            "SyncTestFixturePage.Inputs.USERNAME.INPUT=//user\n"
            // EMAIL key deliberately absent
        );

        orchestrator.sync(SyncTestFixturePage.class, false, base);

        String content = Files.readString(propsFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("SyncTestFixturePage.Inputs.EMAIL.INPUT="),
            "Missing key must be merged into the file");
        assertTrue(content.contains("SyncTestFixturePage.Inputs.USERNAME.INPUT=//user"),
            "Existing filled key must be preserved after merge");
    }
}
