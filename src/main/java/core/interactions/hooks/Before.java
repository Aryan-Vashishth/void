package core.interactions.hooks;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.engine.selenium.SeleniumEngine;
import core.utils.web.DOMUtils;
import core.utils.UIContext;
import core.utils.web.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static core.logging.CustomLogger.debug;

/**
 * Standard library of <b>before-action</b> {@link ActionHandler} constants.
 * <p>
 * Combine freely at call sites:
 * <pre>
 *   interactions.clickOn(List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.HIGHLIGHT_ELEMENT), element);
 * </pre>
 * This class is a pure constants holder — never instantiate it.
 */
public final class Before {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private Before() {}

    // ── No-ops / logging ──────────────────────────────────────────────────
    public static final ActionHandler DO_NOTHING  = engine -> {};
    public static final ActionHandler LOG_INTENT  = engine -> debug.log("[DEBUG] Performing UI action...");

    // ── Loader waits ──────────────────────────────────────────────────────
    public static final ActionHandler WAIT_FOR_ANGULAR_LOADER       = engine -> WaitUtils.resolveAngularLoader();
    public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER  = engine -> WaitUtils.resolveLoader(WaitUtils.SPIN_SPINNER_LOADER);

    // ── Element-state waits (require UIContext.getLastElement() to be set) ─
    /** Waits for the last resolved element to become clickable. */
    public static final ActionHandler WAIT_FOR_ELEMENT_CLICKABLE = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            WebDriver driver = (WebDriver) engine.getNativeDriver();
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.elementToBeClickable(element));
        }
    };

    /** Waits for the last resolved element to be visible. */
    public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            WebDriver driver = (WebDriver) engine.getNativeDriver();
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));
        }
    };

    // ── Element manipulation ───────────────────────────────────────────────
    /** Clears the last resolved input element. Throws if UIContext has no element. */
    public static final ActionHandler CLEAR_FIELD = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            element.clear();
        } else {
            throw new IllegalStateException(
                    "[Before.CLEAR_FIELD] UIContext.getLastElement() is null – resolve the element first.");
        }
    };

    /** Scrolls the last resolved element into view. */
    public static final ActionHandler SCROLL_TO_ELEMENT = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) DOMUtils.scrollToElement(element);
    };

    /** Highlights the last resolved element with a red border (debug aid). */
    public static final ActionHandler HIGHLIGHT_ELEMENT = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            WebDriver driver = (WebDriver) engine.getNativeDriver();
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].style.border='6px solid red';", element);
        }
    };
}

