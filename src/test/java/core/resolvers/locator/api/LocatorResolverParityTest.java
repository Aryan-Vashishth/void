package core.resolvers.locator.api;

import core.resolvers.locator.ElementLocatorResolverV1;
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Parity tests asserting that the new instance-based {@link LocatorResolvers} produces the
 * exact same {@link By} as the legacy static façades for a representative input set.
 *
 * <ul>
 *   <li>{@link LocatorResolvers#strict()}       ≡ {@link LocatorResolverV1}</li>
 *   <li>{@link LocatorResolvers#legacyPadded()} ≡ {@link ElementLocatorResolverV1}</li>
 * </ul>
 *
 * <p>These tests are the safety net that lets us delete the legacy façades once all
 * callers have migrated.</p>
 */
@SuppressWarnings({"deprecation", "removal"})
public class LocatorResolverParityTest {

    private static final String PROP_FILE = "test-locators.properties";

    // ---------------------------------------------------------------------
    // Strict policy parity (LocatorResolvers.strict() ≡ LocatorResolverV1)
    // ---------------------------------------------------------------------

    @DataProvider(name = "strictCases")
    public Object[][] strictCases() {
        return new Object[][] {
            // hardcoded
            { null,      "//div[@id='x']",                      new Object[] { } },
            { null,      "id=foo",                              new Object[] { } },
            { null,      "css=button.primary",                  new Object[] { } },
            { null,      "//input[@placeholder='%s']",          new Object[] { "Search" } },
            { null,      "//div[@a='%1$s' and @b='%2$s']",      new Object[] { "x", "y" } },
            // properties-backed
            { PROP_FILE, "BUTTON_TRIGGER",                      new Object[] { } },
            { PROP_FILE, "LOGIN_INPUT",                         new Object[] { } },
            { PROP_FILE, "TEMPLATE_WITH_ARG",                   new Object[] { "Email" } },
            { PROP_FILE, "TEMPLATE_TWO_ARGS",                   new Object[] { "1", "3" } },
        };
    }

    @Test(dataProvider = "strictCases",
          description = "LocatorResolvers.strict() must produce the same By as legacy LocatorResolverV1")
    public void strict_parity(String file, String key, Object[] args) {
        By legacy = LocatorResolverV1.getLocator(file, key, args);
        By modern = LocatorResolvers.strict().resolve(file, key, args);
        assertEquals(modern.toString(), legacy.toString(),
                "Parity broken for file=" + file + " key=" + key);
    }

    // ---------------------------------------------------------------------
    // Padded policy parity (LocatorResolvers.legacyPadded() ≡ ElementLocatorResolverV1)
    // ---------------------------------------------------------------------

    @DataProvider(name = "paddedCases")
    public Object[][] paddedCases() {
        return new Object[][] {
            // hardcoded
            { null,      "//div",                       new Object[] { } },
            { null,      "id=mainHeader",               new Object[] { } },
            { null,      "//input[@placeholder='%s']",  new Object[] { "Search" } },
            // PAD_LAST signature behaviour: fewer args than placeholders → repeat last
            { null,      "//tr[%s]//td[%s]",            new Object[] { "5" } },
            { null,      "[%s][%s][%s]",                new Object[] { "X" } },
            // properties-backed
            { PROP_FILE, "BUTTON_TRIGGER",              new Object[] { } },
            { PROP_FILE, "TEMPLATE_TWO_ARGS",           new Object[] { "7" } }, // pad-last
            { PROP_FILE, "TEMPLATE_TWO_ARGS",           new Object[] { "1", "3" } },
        };
    }

    @Test(dataProvider = "paddedCases",
          description = "LocatorResolvers.legacyPadded() must produce the same By as legacy ElementLocatorResolverV1")
    public void legacyPadded_parity(String file, String key, Object[] args) {
        By legacy = ElementLocatorResolverV1.getLocator(file, key, args);
        By modern = LocatorResolvers.legacyPadded().resolve(file, key, args);
        assertEquals(modern.toString(), legacy.toString(),
                "Parity broken for file=" + file + " key=" + key);
    }
}

