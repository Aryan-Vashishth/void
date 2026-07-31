package core.resolvers.locator.source;

import domain.automation.web.resolve.api.LocatorRequest;
import domain.automation.web.resolve.source.HardcodedLocatorSource;
import domain.automation.web.resolve.source.JsonLocatorSource;
import domain.automation.web.resolve.source.LocatorSource;
import domain.automation.web.resolve.source.LocatorSourceRegistry;
import domain.automation.web.resolve.source.PropertiesLocatorSource;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link LocatorSourceRegistry}. */
public class LocatorSourceRegistryTest {

    @Test
    public void default_selectsHardcodedForNullFileName() {
        assertSame(LocatorSourceRegistry.DEFAULT.select(null), HardcodedLocatorSource.INSTANCE);
    }

    @Test
    public void default_selectsPropertiesForDotProperties() {
        assertSame(LocatorSourceRegistry.DEFAULT.select("foo.properties"), PropertiesLocatorSource.INSTANCE);
    }

    @Test
    public void default_selectsJsonForDotJson() {
        assertSame(LocatorSourceRegistry.DEFAULT.select("foo.json"), JsonLocatorSource.INSTANCE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*Unsupported locator file.*")
    public void default_throwsOnUnsupportedExtension() {
        LocatorSourceRegistry.DEFAULT.select("foo.xml");
    }

    @Test
    public void with_appendsCustomSourceAtEnd() {
        LocatorSource custom = new LocatorSource() {
            @Override public boolean supports(String fileName) { return fileName != null && fileName.endsWith(".yaml"); }
            @Override public String  readRaw(LocatorRequest r) { return "yaml-value"; }
            @Override public String  name()                    { return "yaml"; }
        };
        LocatorSourceRegistry r = LocatorSourceRegistry.DEFAULT.with(custom);
        assertEquals(r.sources().size(), 4);
        assertSame(r.select("a.yaml"), custom);
        // Original sources still resolve correctly
        assertSame(r.select(null), HardcodedLocatorSource.INSTANCE);
    }

    @Test
    public void sources_isImmutable() {
        try {
            LocatorSourceRegistry.DEFAULT.sources().add(HardcodedLocatorSource.INSTANCE);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}

