package core.utils.web;

import WebApplication.VOID;
import core.utils.UIContext;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import core.driver.DriverContext;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;

/**
 * Static DOM manipulation utilities for Selenium {@link org.openqa.selenium.WebDriver}.
 *
 * <p>Provides JavaScript-powered helpers for scrolling, hovering, highlighting,
 * and low-level DOM inspection. All methods obtain the active driver from
 * {@link core.driver.DriverContext} — no instance needed.</p>
 *
 * <p>Typical usage from hooks or interaction helpers:
 * <pre>
 *   DOMUtils.scrollToElement(element);
 *   DOMUtils.hoverOnElement(element);
 * </pre>
 *
 * @see core.utils.web.WaitUtils
 * @see core.driver.DriverContext
 */
public class DOMUtils {

    private DOMUtils() { /* static utility */ }

    /**
     * Scrolls to a WebElement using JavaScript scrollIntoView.
     */
    public static void scrollToElement(WebElement element) {
        try {
            WebDriver driver = DriverContext.getDriver();
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior: 'instant', block: 'center', inline: 'nearest'});",
                    element
            );
        } catch (Exception e) {
            error.log("Failed to scroll to element");
            debug.error(e.getMessage());
        }
    }

    /**
     * Hovers over a WebElement using Selenium Actions class.
     */
    public static void hoverOnElement(WebElement element) {
        debug.log("Attempting to hover on element: " + element);
        try {
            scrollToElement(element);
            WebDriver driver = DriverContext.getDriver();
            Actions actions = new Actions(driver);
            actions.moveToElement(element).perform();
            info.success("Hovered on element successfully.");
        } catch (Exception e) {
            error.failed("Failed to hover on element.");
            throw new RuntimeException("Hover failed on element", e);
        }
    }

    /**
     * Switches into an iframe using its locator after waiting for visibility.
     */
    public static void switchToFrame(By iframeLocator) {
        try {
            WebDriver driver = DriverContext.getDriver();
            WebElement iframe;
            if (iframeLocator != null) {
                try {
                    iframe = WaitUtils.waitForCondition(
                            driver,
                            ExpectedConditions.presenceOfElementLocated(iframeLocator),
                            iframeLocator,
                            10,
                            300,
                            true,
                            "iframe to become visible: " + iframeLocator
                    );
                } catch (TimeoutException e) {
                    WebElement lastElement = UIContext.getLastElement();
                    debug.error("Last element in action was: " + lastElement.getText() + " Locator: " + lastElement);
                    warn.fallback("Trying Clicking Using JSExecutor as WebDriver click failed");

                    new VOID().interaction().clickOn(
                            null,
                            lastElement,
                            true,
                            null
                    );

                    iframe = WaitUtils.waitForCondition(
                            driver,
                            ExpectedConditions.presenceOfElementLocated(iframeLocator),
                            iframeLocator,
                            15,
                            300,
                            true,
                            "iframe to become visible: " + iframeLocator
                    );
                }

                if (iframe != null) {
                    driver.switchTo().frame(iframe);
                    debug.log("[FRAME] Switched into iframe located by: " + iframeLocator);
                } else {
                    throw new TimeoutException("Iframe not found within timeout.");
                }
            } else {
                info.log("[FRAME] No iframe locator provided, staying in current context.");
            }
        } catch (Exception e) {
            error.log("Failed to switch to iframe located by: " + iframeLocator);
            throw new RuntimeException("Failed to switch to iframe: " + iframeLocator, e);
        }
    }

    /**
     * Switches back to the main/default frame.
     */
    public static void switchToDefaultContent() {
        try {
            WebDriver driver = DriverContext.getDriver();
            driver.switchTo().defaultContent();
            info.frame("Switched back to default content.");
        } catch (Exception e) {
            error.log("Failed to switch back to default content.");
            throw new RuntimeException("Failed to switch back to default content", e);
        }
    }

    public static void sendKey(Keys key) {
        try {
            WebDriver driver = DriverContext.getDriver();
            debug.log("Sending key via Actions: " + key.name());
            new Actions(driver).sendKeys(key).perform();
            info.success("Key sent successfully: " + key.name());
        } catch (Exception e) {
            error.failed("Failed to send key: " + key.name());
            throw new RuntimeException("Could not send key: " + key.name(), e);
        }
    }

    /**
     * Sends the ESCAPE key using Selenium Actions to close overlays, popups, or modals.
     */
    public static void sendEscapeKey() {
        sendKey(Keys.ESCAPE);
    }
}
