package core.resolvers.locator.template;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for {@link LocatorTemplate} — covers both formatting policies. */
public class LocatorTemplateTest {

    // ---------- placeholderCount() ----------

    @Test
    public void padded_countsCaseInsensitive() {
        assertEquals(LocatorTemplate.padded("//a[%s]/b[%S]").placeholderCount(), 2);
    }

    @Test
    public void strict_countsLowercaseSOnly() {
        assertEquals(LocatorTemplate.strict("//a[%s]/b[%S]").placeholderCount(), 1);
    }

    @Test
    public void strict_countsIndexedPlaceholders() {
        assertEquals(LocatorTemplate.strict("%1$s and %2$s and %s").placeholderCount(), 3);
    }

    @Test
    public void anyPolicy_nullOrEmpty_returnsZero() {
        assertEquals(LocatorTemplate.padded(null).placeholderCount(), 0);
        assertEquals(LocatorTemplate.strict("").placeholderCount(), 0);
    }

    // ---------- format() — PAD_LAST ----------

    @Test
    public void padded_padsLastArgWhenTooFew() {
        assertEquals(LocatorTemplate.padded("//tr[%s]//td[%s]").format("7"), "//tr[7]//td[7]");
    }

    @Test
    public void padded_returnsTemplateWhenNoPlaceholders() {
        String tpl = "//div[@id='main']";
        assertEquals(LocatorTemplate.padded(tpl).format("ignored"), tpl);
    }

    @Test
    public void padded_nullTemplate_returnsNull() {
        assertNull(LocatorTemplate.padded(null).format("x"));
    }

    // ---------- format() — STRICT ----------

    @Test
    public void strict_substitutesIndexedPlaceholders() {
        assertEquals(
                LocatorTemplate.strict("//div[@a='%1$s' and @b='%2$s']").format("foo", "bar"),
                "//div[@a='foo' and @b='bar']");
    }

    @Test
    public void strict_noPlaceholder_returnsTemplate() {
        String tpl = "css=.btn";
        assertEquals(LocatorTemplate.strict(tpl).format(new Object[0]), tpl);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void strict_tooFewArgs_throws() {
        LocatorTemplate.strict("//td[%s]//span[%s]").format();
    }

    @Test
    public void strict_nullTemplate_returnsNull() {
        assertNull(LocatorTemplate.strict(null).format("x"));
    }
}

