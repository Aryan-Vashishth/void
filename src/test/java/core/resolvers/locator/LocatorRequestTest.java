package core.resolvers.locator;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for the {@link LocatorRequest} value object. */
public class LocatorRequestTest {

    @Test
    public void of_normalisesNullArgsToEmptyArray() {
        LocatorRequest r = LocatorRequest.of("file.properties", "KEY", (Object[]) null);
        assertNotNull(r.args());
        assertEquals(r.args().length, 0);
    }

    @Test
    public void isHardcoded_trueWhenFileNameNull() {
        assertTrue(LocatorRequest.of(null, "//x").isHardcoded());
        assertFalse(LocatorRequest.of("a.properties", "K").isHardcoded());
    }

    @Test
    public void withArgs_returnsCopyWithNewArgs() {
        LocatorRequest base = LocatorRequest.of("f", "K", "a");
        LocatorRequest next = base.withArgs("b", "c");
        assertEquals(base.args(), new Object[]{"a"});
        assertEquals(next.args(), new Object[]{"b", "c"});
        assertEquals(next.fileName(), "f");
        assertEquals(next.key(), "K");
    }

    @Test
    public void canonicalConstructor_normalisesNullArgs() {
        LocatorRequest r = new LocatorRequest("f", "K", null);
        assertEquals(r.args().length, 0);
    }
}

