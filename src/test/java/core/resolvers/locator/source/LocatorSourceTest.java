package core.resolvers.locator.source;

import domain.automation.web.resolve.api.LocatorRequest;
import domain.automation.web.resolve.source.HardcodedLocatorSource;
import domain.automation.web.resolve.source.JsonLocatorSource;
import domain.automation.web.resolve.source.PropertiesLocatorSource;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link HardcodedLocatorSource}, {@link PropertiesLocatorSource}, {@link JsonLocatorSource}. */
@Test(groups = {"integration"})
public class LocatorSourceTest {

    private static final String PROP_FILE = "test-locators.properties"; // base prepended internally
    private static final String JSON_FILE = "test-locators.json";       // base prepended internally

    // ---------- HardcodedLocatorSource ----------

    @Test
    public void hardcoded_supportsOnlyNullFileName() {
        assertTrue(HardcodedLocatorSource.INSTANCE.supports(null));
        assertFalse(HardcodedLocatorSource.INSTANCE.supports("x.properties"));
        assertFalse(HardcodedLocatorSource.INSTANCE.supports("x.json"));
    }

    @Test
    public void hardcoded_returnsKeyVerbatim() {
        String raw = HardcodedLocatorSource.INSTANCE.readRaw(LocatorRequest.of(null, "//div[@id='x']"));
        assertEquals(raw, "//div[@id='x']");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void hardcoded_rejectsUnsupportedFileName() {
        HardcodedLocatorSource.INSTANCE.readRaw(LocatorRequest.of("a.properties", "K"));
    }

    @Test
    public void hardcoded_name() {
        assertEquals(HardcodedLocatorSource.INSTANCE.name(), "hardcoded");
    }

    // ---------- PropertiesLocatorSource ----------

    @Test
    public void properties_supportsDotProperties_caseInsensitive() {
        assertTrue(PropertiesLocatorSource.INSTANCE.supports("a.properties"));
        assertTrue(PropertiesLocatorSource.INSTANCE.supports("a.PROPERTIES"));
        assertFalse(PropertiesLocatorSource.INSTANCE.supports("a.json"));
        assertFalse(PropertiesLocatorSource.INSTANCE.supports(null));
    }

    @Test
    public void properties_returnsValueForKnownKey() {
        String raw = PropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "BUTTON_TRIGGER"));
        assertNotNull(raw);
        assertTrue(raw.contains("submit"));
    }

    @Test
    public void properties_returnsNullForMissingKey() {
        assertNull(PropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "DEFINITELY_MISSING_XYZ")));
    }

    // ---------- JsonLocatorSource ----------

    @Test
    public void json_supportsDotJson_caseInsensitive() {
        assertTrue(JsonLocatorSource.INSTANCE.supports("a.json"));
        assertTrue(JsonLocatorSource.INSTANCE.supports("a.JSON"));
        assertFalse(JsonLocatorSource.INSTANCE.supports("a.properties"));
        assertFalse(JsonLocatorSource.INSTANCE.supports(null));
    }

    @Test
    public void json_returnsValueForFlatKey() {
        String raw = JsonLocatorSource.INSTANCE.readRaw(LocatorRequest.of(JSON_FILE, "FLAT_KEY"));
        assertEquals(raw, "//span[@id='flat']");
    }

    @Test
    public void json_returnsValueForDotPathKey() {
        String raw = JsonLocatorSource.INSTANCE.readRaw(LocatorRequest.of(JSON_FILE, "elements.SEARCH_INPUT"));
        assertNotNull(raw);
    }
}

