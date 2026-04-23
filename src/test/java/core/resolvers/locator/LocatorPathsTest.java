package core.resolvers.locator;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link LocatorPaths}. */
public class LocatorPathsTest {

    @Test
    public void underProperties_prependsBaseWhenMissing() {
        assertEquals(LocatorPaths.underProperties("foo.properties"), "locators/foo.properties");
    }

    @Test
    public void underProperties_leavesAlreadyPrefixedPathUntouched() {
        assertEquals(LocatorPaths.underProperties("locators/foo.properties"), "locators/foo.properties");
    }

    @Test
    public void underProperties_nullReturnsNull() {
        assertNull(LocatorPaths.underProperties(null));
    }

    @Test
    public void under_supportsCustomBase() {
        assertEquals(LocatorPaths.under("custom/", "x.json"), "custom/x.json");
        assertEquals(LocatorPaths.under("custom/", "custom/x.json"), "custom/x.json");
    }
}

