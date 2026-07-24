package core.resolvers.locator.api;

import core.resolvers.locator.parser.ByParser;
import core.resolvers.locator.source.LocatorSourceRegistry;
import core.resolvers.locator.template.LocatorTemplate;
import elements.api.capability.Clickable;
import elements.api.UIElement;
import elements.meta.ElementRole;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Unit tests for the unified {@link LocatorResolver} instance. */
public class LocatorResolverTest {

    private static final String PROP_FILE = "test-locators.properties";
    private static final String JSON_FILE = "test-locators.json";

    private static UIElement hardcoded(String tpl, Object... args) {
        return new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return tpl; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    // ---------- builder + defaults ----------

    @Test
    public void builder_defaultsAreStrictAndDefaultRegistry() {
        LocatorResolver r = LocatorResolver.builder().build();
        assertSame(r.registry(), LocatorSourceRegistry.DEFAULT);
        assertEquals(r.templatePolicy(), LocatorTemplate.Policy.STRICT);
        assertSame(r.byParser(), ByParser.DEFAULT);
        assertSame(r.locatorContext(), DefaultLocatorContext.INSTANCE);
    }

    @Test
    public void builder_overridesAreApplied() {
        LocatorSourceRegistry custom = LocatorSourceRegistry.DEFAULT;
        LocatorResolver r = LocatorResolver.builder()
                .policy(LocatorTemplate.Policy.PAD_LAST)
                .registry(custom)
                .build();
        assertEquals(r.templatePolicy(), LocatorTemplate.Policy.PAD_LAST);
        assertSame(r.registry(), custom);
    }

    // ---------- resolve(LocatorRequest) ----------

    @Test
    public void strict_resolvesHardcodedXpath() {
        By by = LocatorResolvers.strict().resolve(LocatorRequest.of(null, "//div[@id='x']"));
        assertEquals(by.toString(), By.xpath("//div[@id='x']").toString());
    }

    @Test
    public void strict_resolvesPropertiesFileKey() {
        By by = LocatorResolvers.strict().resolve(LocatorRequest.of(PROP_FILE, "BUTTON_TRIGGER"));
        assertEquals(by.toString(), By.xpath("//button[@id='submit']").toString());
    }

    @Test
    public void strict_resolvesJsonDotPath() {
        By by = LocatorResolvers.strict().resolve(LocatorRequest.of(JSON_FILE, "elements.SEARCH_BUTTON"));
        assertEquals(by.toString(), By.cssSelector("button.search-btn").toString());
    }

    @Test
    public void strict_substitutesIndexedPlaceholders() {
        By by = LocatorResolvers.strict().resolve(
                LocatorRequest.of(null, "//div[@a='%1$s' and @b='%2$s']", "x", "y"));
        assertEquals(by.toString(), By.xpath("//div[@a='x' and @b='y']").toString());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void strict_throwsOnMissingKey() {
        LocatorResolvers.strict().resolve(LocatorRequest.of(PROP_FILE, "NOPE_NOT_HERE"));
    }

    // ---------- padded policy ----------

    @Test
    public void padded_padsLastArg() {
        By by = LocatorResolvers.legacyPadded().resolve(
                LocatorRequest.of(null, "//tr[%s]//td[%s]", "9"));
        assertEquals(by.toString(), By.xpath("//tr[9]//td[9]").toString());
    }

    // ---------- element resolution ----------

    @Test
    public void resolve_element_usesPrimaryLocator() {
        UIElement e = hardcoded("//button[@type='submit']");
        By by = LocatorResolvers.strict().resolve(e);
        assertEquals(by.toString(), By.xpath("//button[@type='submit']").toString());
    }

    @Test
    public void resolve_elementRole_usesRoleMap() {
        Clickable c = new Clickable() {
            @Override public String getTriggerLocator()   { return "//a[@id='go']"; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
        By by = LocatorResolvers.strict().resolve(c, ElementRole.TRIGGER);
        assertEquals(by.toString(), By.xpath("//a[@id='go']").toString());
    }

    @Test
    public void resolveBest_overrideArgsTakePrecedence() {
        UIElement e = hardcoded("//x[@v='%s']", "default");
        By by = LocatorResolvers.strict().resolveBest(e, "override");
        assertEquals(by.toString(), By.xpath("//x[@v='override']").toString());
    }

    // ---------- raw lookup ----------

    @Test
    public void rawTemplate_returnsRawValueWithoutFormatting() {
        String raw = LocatorResolvers.strict().rawTemplate(LocatorRequest.of(PROP_FILE, "TEMPLATE_WITH_ARG"));
        assertTrue(raw.contains("%s"), "Raw value should retain %s placeholder; got: " + raw);
    }
}

