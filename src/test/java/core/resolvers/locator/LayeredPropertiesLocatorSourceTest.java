package core.resolvers.locator;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link LayeredPropertiesLocatorSource}. */
public class LayeredPropertiesLocatorSourceTest {

    private static final String PROP_FILE = "test-locators.properties";

    @Test
    public void supports_dotPropertiesOnly() {
        assertTrue(LayeredPropertiesLocatorSource.INSTANCE.supports("a.properties"));
        assertTrue(LayeredPropertiesLocatorSource.INSTANCE.supports("a.PROPERTIES"));
        assertFalse(LayeredPropertiesLocatorSource.INSTANCE.supports("a.json"));
        assertFalse(LayeredPropertiesLocatorSource.INSTANCE.supports(null));
    }

    @Test
    public void readRaw_returnsValueForKnownKey() {
        String raw = LayeredPropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "BUTTON_TRIGGER"));
        assertNotNull(raw);
        assertTrue(raw.contains("submit"));
    }

    @Test
    public void readRaw_returnsNullForMissingKey() {
        assertNull(LayeredPropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "MISSING_XXX")));
    }

    @Test
    public void readRaw_isCachedAcrossInvocations() {
        // Two reads should return the same value (smoke test for cache correctness).
        String a = LayeredPropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "BUTTON_TRIGGER"));
        String b = LayeredPropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of(PROP_FILE, "BUTTON_TRIGGER"));
        assertEquals(a, b);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void readRaw_rejectsUnsupportedFileName() {
        LayeredPropertiesLocatorSource.INSTANCE.readRaw(LocatorRequest.of("a.json", "K"));
    }

    @Test
    public void name_isLayeredProperties() {
        assertTrue(LayeredPropertiesLocatorSource.INSTANCE.name().contains("layered"));
    }
}

