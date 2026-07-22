package core.bridge.selenium;

import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link SeleniumLocatorBridge#fromBy(By)}.
 *
 * Verifies the four recognised prefix mappings, the XPATH fallback for unknown prefixes,
 * and value extraction. No browser opened -- {@link By} instances are constructed and
 * inspected in-process only.
 */
public class SeleniumLocatorBridgeTest {

    // ── Strategy mapping ──────────────────────────────────────────────────────

    @Test
    public void fromBy_xpathBy_returnsXpathDescriptor() {
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.xpath("//button[@id='submit']"));

        assertEquals(d.strategy(), LocatorStrategy.XPATH);
        assertEquals(d.value(), "//button[@id='submit']");
    }

    @Test
    public void fromBy_cssBy_returnsCssDescriptor() {
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.cssSelector(".submit-btn"));

        assertEquals(d.strategy(), LocatorStrategy.CSS);
        assertEquals(d.value(), ".submit-btn");
    }

    @Test
    public void fromBy_idBy_returnsIdDescriptor() {
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.id("username"));

        assertEquals(d.strategy(), LocatorStrategy.ID);
        assertEquals(d.value(), "username");
    }

    @Test
    public void fromBy_nameBy_returnsNameDescriptor() {
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.name("email"));

        assertEquals(d.strategy(), LocatorStrategy.NAME);
        assertEquals(d.value(), "email");
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    @Test
    public void fromBy_unknownPrefixBy_fallsBackToXpath() {
        // By.linkText produces "By.linkText: foo" which matches none of the four prefixes.
        // Contract: fall back to XPATH (safer for raw expression strings) and emit a warning.
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.linkText("foo"));

        assertEquals(d.strategy(), LocatorStrategy.XPATH,
                "Unknown prefix must fall back to XPATH strategy");
    }

    @Test
    public void fromBy_unknownPrefixBy_preservesRawByStringAsValue() {
        // The full By.toString() is used as the locator value so nothing is silently dropped.
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.linkText("foo"));

        assertTrue(d.value().contains("foo"),
                "Fallback value must contain the original locator text");
    }

    // ── Value round-trip ──────────────────────────────────────────────────────

    @Test
    public void fromBy_xpathWithComplexExpression_preservesValue() {
        String expr = "//tr[td[contains(.,'KEY')]]/following-sibling::tr[1]";
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.xpath(expr));

        assertEquals(d.value(), expr,
                "fromBy must not modify the XPath expression");
    }

    @Test
    public void fromBy_cssWithSpecialChars_preservesValue() {
        String selector = "input[name='q']:not(:disabled)";
        LocatorDescriptor d = SeleniumLocatorBridge.fromBy(By.cssSelector(selector));

        assertEquals(d.value(), selector,
                "fromBy must not modify the CSS selector");
    }
}
