package core.resolvers.locator.json;

import domain.automation.web.resolve.json.PropertiesIndex;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

/** Unit tests for {@link PropertiesIndex}. */
@Test(groups = {"integration"})
public class PropertiesIndexTest {

    private static final String PROP_FILE = "test-locators.properties";

    @Test
    public void get_loadsExistingFile() {
        PropertiesIndex idx = new PropertiesIndex();
        Properties p = idx.get(PROP_FILE);
        assertNotNull(p);
        assertTrue(p.size() > 0, "Expected non-empty properties; got size=" + p.size());
        assertEquals(idx.size(), 1);
    }

    @Test
    public void get_isCachedSecondCallSameInstance() {
        PropertiesIndex idx = new PropertiesIndex();
        Properties first  = idx.get(PROP_FILE);
        Properties second = idx.get(PROP_FILE);
        assertSame(first, second, "Second call should return cached instance");
        assertEquals(idx.size(), 1);
    }

    @Test
    public void get_blankOrNullFileNameReturnsEmpty() {
        PropertiesIndex idx = new PropertiesIndex();
        assertTrue(idx.get(null).isEmpty());
        assertTrue(idx.get("").isEmpty());
        assertTrue(idx.get("   ").isEmpty());
        assertEquals(idx.size(), 0);
    }

    @Test
    public void get_acceptsAlreadyPrefixedPath() {
        PropertiesIndex idx = new PropertiesIndex();
        Properties p = idx.get("locators/properties/" + PROP_FILE);
        assertTrue(p.size() > 0);
    }

    @Test
    public void get_independentInstancesDoNotShareCache() {
        PropertiesIndex a = new PropertiesIndex();
        PropertiesIndex b = new PropertiesIndex();
        a.get(PROP_FILE);
        assertEquals(a.size(), 1);
        assertEquals(b.size(), 0);
    }
}

