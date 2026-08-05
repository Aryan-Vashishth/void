package core.resolvers.locator.sync;

import core.resolvers.locator.sync.OrphanKeyDetector.OrphanWarning;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class OrphanKeyDetectorTest {

    private final OrphanKeyDetector detector = new OrphanKeyDetector();

    private LineTrackingPropertiesReader readerFor(String... lines) throws IOException {
        Path tmp = Files.createTempFile("orphan-test", ".properties");
        Files.writeString(tmp, String.join("\n", lines));
        LineTrackingPropertiesReader reader = new LineTrackingPropertiesReader();
        reader.load(tmp);
        Files.delete(tmp);
        return reader;
    }

    @Test
    public void noOrphans_whenAllKeysMatchCurrentEnums() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.Inputs.USERNAME.INPUT=//input[@id='user']",
            "SyncTestFixturePage.Actions.SUBMIT.TRIGGER=//button[@type='submit']"
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertTrue(warnings.isEmpty(), "Expected no orphan warnings: " + warnings);
    }

    @Test
    public void orphan_whenConstantRemovedFromEnum() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.Inputs.OLD_FIELD.INPUT=//input[@id='old']"
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertTrue(warnings.get(0).reason().contains("OLD_FIELD"),
            "Reason must name the missing constant");
    }

    @Test
    public void orphan_whenEnumClassRemovedFromPage() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.OldGroup.SOME_FIELD.INPUT=//x"
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertTrue(warnings.get(0).reason().contains("OldGroup"),
            "Reason must name the missing enum class");
    }

    @Test
    public void malformed_keyWithFewerThanThreeSegments() throws IOException {
        LineTrackingPropertiesReader reader = readerFor("JUST_KEY=value");
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertTrue(warnings.get(0).reason().contains("malformed"), "Expected malformed warning");
    }

    @Test
    public void oldFormat_threeSegments_resolvedByConstantLookup() throws IOException {
        // Old format without role suffix: PageClass.EnumClass.CONSTANT
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.Inputs.USERNAME=//input"  // no role suffix
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        // USERNAME is a valid constant in Inputs, so no orphan warning
        assertTrue(warnings.isEmpty(), "Valid constant without role suffix must not be flagged");
    }

    @Test
    public void locatorFamily_twoSegmentKey_knownEnum_noWarning() throws IOException {
        // LocatorFamily emits "PageName.EnumName" with no constant suffix.
        // Inputs is a valid nested enum in SyncTestFixturePage.
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.Inputs=//input[contains(@class,'%s')]"
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertTrue(warnings.isEmpty(), "Valid LocatorFamily key must not be flagged: " + warnings);
    }

    @Test
    public void locatorFamily_twoSegmentKey_unknownEnum_reportsOrphan() throws IOException {
        // "OldFamily" does not exist as a nested enum in SyncTestFixturePage.
        LineTrackingPropertiesReader reader = readerFor(
            "SyncTestFixturePage.OldFamily=//div[%s]"
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertTrue(warnings.get(0).reason().contains("OldFamily"),
            "Reason must name the missing enum class");
    }

    @Test
    public void lineNumber_reportedCorrectly() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "# comment",
            "",
            "SyncTestFixturePage.Inputs.GHOST.INPUT=//ghost"  // line 3
        );
        List<OrphanWarning> warnings = detector.detect(SyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertEquals(warnings.get(0).lineNumber(), 3);
    }

    // ── Nested interface tests ────────────────────────────────────────────────

    @Test
    public void nestedInterfaceEnum_validKey_noWarning() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "LoginForm.Fields.USERNAME_FIELD.INPUT=//input[@id='user']",
            "LoginForm.Buttons.LOGIN_BUTTON.TRIGGER=//button[@type='submit']"
        );
        List<OrphanWarning> warnings = detector.detect(NestedSyncTestFixturePage.class, reader);
        assertTrue(warnings.isEmpty(),
            "Valid nested interface enum keys must not be flagged as orphans: " + warnings);
    }

    @Test
    public void nestedInterfaceEnum_unknownConstant_reportedAsOrphan() throws IOException {
        LineTrackingPropertiesReader reader = readerFor(
            "LoginForm.Fields.OLD_FIELD.INPUT=//input[@id='old']"
        );
        List<OrphanWarning> warnings = detector.detect(NestedSyncTestFixturePage.class, reader);
        assertEquals(warnings.size(), 1);
        assertTrue(warnings.get(0).reason().contains("OLD_FIELD"),
            "Orphan reason must name the stale constant");
    }
}
