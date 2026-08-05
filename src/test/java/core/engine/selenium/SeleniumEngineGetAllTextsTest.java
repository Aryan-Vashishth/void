package core.engine.selenium;

import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.locator.LocatorStrategy;
import domain.automation.web.selenium.SeleniumEngine;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SeleniumEngine#getAllTexts(LocatorDescriptor)}.
 *
 * <p>{@code getAllTexts} delegates locator resolution to the package-private
 * {@code toBy()} method, which validates the descriptor before any WebDriver
 * call is made. The tests below verify those guards fire correctly without
 * opening a browser.
 *
 * <p>Browser-level behavior (element discovery, text extraction, ordering) is
 * covered by the SauceDemoTest integration suite.
 */
@SuppressWarnings("deprecation")
public class SeleniumEngineGetAllTextsTest {

    // Instantiate via the deprecated WebDriver bridge so no browser is opened.
    // The null driver is safe for these tests because toBy() throws before
    // driver.findElements() is ever reached.
    private static final SeleniumEngine ENGINE = new SeleniumEngine((org.openqa.selenium.WebDriver) null);

    // ── Null guards ───────────────────────────────────────────────────────────

    @Test(expectedExceptions = IllegalArgumentException.class,
          description = "null descriptor must be rejected before any WebDriver call")
    public void getAllTexts_nullDescriptor_throwsIllegalArgumentException() {
        ENGINE.getAllTexts(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          description = "descriptor with null value must be rejected before any WebDriver call")
    public void getAllTexts_nullValueDescriptor_throwsIllegalArgumentException() {
        LocatorDescriptor d = new LocatorDescriptor(null, LocatorStrategy.XPATH, null, null, null);
        ENGINE.getAllTexts(d);
    }

    // ── Strategy resolution ───────────────────────────────────────────────────

    @Test(description = "getAllTexts accepts an XPATH descriptor without throwing before driver access")
    public void getAllTexts_validXpathDescriptor_passesLocatorValidation() {
        // toBy() succeeds for a well-formed descriptor; the subsequent driver.findElements()
        // call would fail if attempted, but the test verifies the contract boundary:
        // locator validation is separate from element retrieval.
        LocatorDescriptor d = LocatorDescriptor.of("//div[@class='item']", LocatorStrategy.XPATH);
        try {
            ENGINE.getAllTexts(d);
        } catch (NullPointerException npe) {
            // Expected: driver is null so findElements() NPEs — but this NPE proves
            // toBy() succeeded (locator was valid) and the method reached driver access.
        } catch (IllegalArgumentException iae) {
            throw new AssertionError("Valid descriptor should not be rejected by toBy()", iae);
        }
    }

    @Test(description = "getAllTexts accepts a CSS descriptor without throwing before driver access")
    public void getAllTexts_validCssDescriptor_passesLocatorValidation() {
        LocatorDescriptor d = LocatorDescriptor.of(".inventory-item", LocatorStrategy.CSS);
        try {
            ENGINE.getAllTexts(d);
        } catch (NullPointerException npe) {
            // Expected: null driver — locator was valid and method reached driver access.
        } catch (IllegalArgumentException iae) {
            throw new AssertionError("Valid descriptor should not be rejected by toBy()", iae);
        }
    }

    @Test(description = "getAllTexts rejects an unknown strategy via toBy() before driver access")
    public void getAllTexts_unsupportedStrategy_throwsIllegalStateException() {
        LocatorStrategy accessibility = LocatorStrategy.of("ACCESSIBILITY_ID");
        LocatorDescriptor d = LocatorDescriptor.of("com.example.button", accessibility);
        try {
            ENGINE.getAllTexts(d);
            throw new AssertionError("Should have thrown IllegalStateException for unknown strategy");
        } catch (IllegalStateException ise) {
            // Expected: toBy() does not know ACCESSIBILITY_ID
        }
    }
}
