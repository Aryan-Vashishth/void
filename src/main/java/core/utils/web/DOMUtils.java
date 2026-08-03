package core.utils.web;

import core.utils.UIContext;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import domain.automation.web.selenium.driver.SeleniumDriverContext;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;

/**
 * Static DOM manipulation utilities for Selenium {@link org.openqa.selenium.WebDriver}.
 *
 * @deprecated All operations in this class are available as engine-agnostic methods on
 *             {@link core.engine.UIEngine}: {@code scrollTo}, {@code hover},
 *             {@code switchToFrame}, {@code switchToDefaultContent}, {@code sendKeys}.
 *             This class will be removed once all callers migrate to the engine API.
 * @see core.engine.UIEngine
 */
@Deprecated(forRemoval = true)
public class DOMUtils {

    private DOMUtils() { /* static utility */ }

    /**
     * @deprecated Use {@link core.engine.UIEngine#scrollTo(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void scrollToElement(WebElement element) {
        try {
            WebDriver driver = SeleniumDriverContext.getDriver();
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
     * @deprecated Use {@link core.engine.UIEngine#hover(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void hoverOnElement(WebElement element) {
        debug.log("Attempting to hover on element: " + element);
        try {
            scrollToElement(element);
            WebDriver driver = SeleniumDriverContext.getDriver();
            Actions actions = new Actions(driver);
            actions.moveToElement(element).perform();
            info.success("Hovered on element successfully.");
        } catch (Exception e) {
            error.failed("Failed to hover on element.");
            throw new RuntimeException("Hover failed on element", e);
        }
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#switchToFrame(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void switchToFrame(By iframeLocator) {
        try {
            WebDriver driver = SeleniumDriverContext.getDriver();
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

                    // Direct JS click — no need to instantiate VOID in a utility class
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", lastElement);

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
     * @deprecated Use {@link core.engine.UIEngine#switchToDefaultContent()} instead.
     */
    @Deprecated(forRemoval = true)
    public static void switchToDefaultContent() {
        try {
            WebDriver driver = SeleniumDriverContext.getDriver();
            driver.switchTo().defaultContent();
            info.frame("Switched back to default content.");
        } catch (Exception e) {
            error.log("Failed to switch back to default content.");
            throw new RuntimeException("Failed to switch back to default content", e);
        }
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#sendKeys(CharSequence...)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void sendKey(Keys key) {
        try {
            WebDriver driver = SeleniumDriverContext.getDriver();
            debug.log("Sending key via Actions: " + key.name());
            new Actions(driver).sendKeys(key).perform();
            info.success("Key sent successfully: " + key.name());
        } catch (Exception e) {
            error.failed("Failed to send key: " + key.name());
            throw new RuntimeException("Could not send key: " + key.name(), e);
        }
    }

    /**
     * @deprecated Use {@code engine.sendKeys(Keys.ESCAPE)} via {@link core.engine.UIEngine#sendKeys(CharSequence...)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void sendEscapeKey() {
        sendKey(Keys.ESCAPE);
    }
}
