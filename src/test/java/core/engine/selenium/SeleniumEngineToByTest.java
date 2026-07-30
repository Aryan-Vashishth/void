package core.engine.selenium;

import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumEngine#toBy(LocatorDescriptor)}.
 *
 * {@code toBy} is a public static bridge used by deprecated utilities (TableHandler)
 * during the Selenium-decoupling transition. All four strategies must map correctly,
 * and null inputs must be rejected before any Selenium call is made.
 *
 * No browser is opened -- {@link By} instances are inspected via {@code toString()},
 * which Selenium guarantees to include the locator value.
 */
public class SeleniumEngineToByTest {

    // ── Strategy mapping ──────────────────────────────────────────────────────

    @Test
    public void toBy_xpathDescriptor_returnsXpathBy() {
        LocatorDescriptor d = LocatorDescriptor.of("//button[@id='submit']", LocatorStrategy.XPATH);

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().startsWith("By.xpath:"),
                "Expected By.xpath but got: " + by);
        assertTrue(by.toString().contains("//button[@id='submit']"));
    }

    @Test
    public void toBy_cssDescriptor_returnsCssBy() {
        LocatorDescriptor d = LocatorDescriptor.of(".submit-btn", LocatorStrategy.CSS);

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().startsWith("By.cssSelector:"),
                "Expected By.cssSelector but got: " + by);
        assertTrue(by.toString().contains(".submit-btn"));
    }

    @Test
    public void toBy_idDescriptor_returnsIdBy() {
        LocatorDescriptor d = LocatorDescriptor.of("username", LocatorStrategy.ID);

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().startsWith("By.id:"),
                "Expected By.id but got: " + by);
        assertTrue(by.toString().contains("username"));
    }

    @Test
    public void toBy_nameDescriptor_returnsNameBy() {
        LocatorDescriptor d = LocatorDescriptor.of("email", LocatorStrategy.NAME);

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().startsWith("By.name:"),
                "Expected By.name but got: " + by);
        assertTrue(by.toString().contains("email"));
    }

    // ── Value round-trip ──────────────────────────────────────────────────────

    @Test
    public void toBy_xpathWithFormatPlaceholder_preservesRawValue() {
        // TableHandler passes raw (pre-formatted) locator strings; toBy must not modify them
        LocatorDescriptor d = LocatorDescriptor.of("//tr[%s]//td", LocatorStrategy.XPATH);

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().contains("//tr[%s]//td"),
                "toBy must not modify the locator value string");
    }

    @Test
    public void toBy_labeledDescriptor_usesValueNotLabel() {
        LocatorDescriptor d = LocatorDescriptor.of("//div", LocatorStrategy.XPATH)
                .withLabel("SomePage > Group > ELEMENT");

        By by = SeleniumEngine.toBy(d);

        assertTrue(by.toString().contains("//div"),
                "toBy must resolve from value, not label");
        assertFalse(by.toString().contains("SomePage"),
                "label must not appear in the By output");
    }

    // ── Open strategy set ────────────────────────────────────────────────────

    @Test
    public void toBy_customStrategy_throwsWithHelpfulMessage() {
        LocatorStrategy accessibility = LocatorStrategy.of("ACCESSIBILITY_ID");
        LocatorDescriptor d = LocatorDescriptor.of("com.example.button", accessibility);

        IllegalStateException ex = expectThrows(IllegalStateException.class,
                () -> SeleniumEngine.toBy(d));

        assertTrue(ex.getMessage().contains("ACCESSIBILITY_ID"),
                "Error message must name the unsupported strategy: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("BY_FACTORIES"),
                "Error message must point to the registration site: " + ex.getMessage());
    }

    @Test
    public void toBy_customStrategyEqualsByName_treatedSameAsConstant() {
        LocatorDescriptor withConstant = LocatorDescriptor.of("//a", LocatorStrategy.XPATH);
        LocatorDescriptor withOf       = LocatorDescriptor.of("//a", LocatorStrategy.of("XPATH"));

        assertEquals(SeleniumEngine.toBy(withConstant).toString(),
                     SeleniumEngine.toBy(withOf).toString(),
                "Two XPATH strategies with the same name must map to the same By");
    }

    // ── Null guards ───────────────────────────────────────────────────────────

    @Test(expectedExceptions = IllegalArgumentException.class,
          description = "null descriptor must be rejected before any Selenium call")
    public void toBy_nullDescriptor_throwsIllegalArgumentException() {
        SeleniumEngine.toBy(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          description = "descriptor with null value must be rejected before any Selenium call")
    public void toBy_nullValue_throwsIllegalArgumentException() {
        LocatorDescriptor d = new LocatorDescriptor(null, LocatorStrategy.XPATH, null, null, null);
        SeleniumEngine.toBy(d);
    }
}
