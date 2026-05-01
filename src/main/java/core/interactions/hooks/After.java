package core.interactions.hooks;

import core.engine.UIEngine;
import core.utils.web.DOMUtils;
import core.utils.UIContext;
import core.utils.web.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Standard library of <b>after-action</b> {@link ActionHandler} constants.
 * <p>
 * Use alongside {@link Before} to compose full hook chains:
 * <pre>
 *   interactions.clickOn(
 *       List.of(Before.WAIT_FOR_ANGULAR_LOADER),
 *       element,
 *       List.of(After.WAIT_FOR_ELEMENT_VISIBLE));
 * </pre>
 * This class is a pure constants holder — never instantiate it.
 */
public final class After {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private After() {}

    // ── No-ops ────────────────────────────────────────────────────────────
    public static final ActionHandler DO_NOTHING = engine -> {};

    // ── Loader waits ──────────────────────────────────────────────────────
    public static final ActionHandler WAIT_FOR_ANGULAR_LOADER      = engine -> WaitUtils.resolveAngularLoader();
    public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = engine -> WaitUtils.resolveLoader(WaitUtils.SPIN_SPINNER_LOADER);

    // ── Element-state waits ────────────────────────────────────────────────
    /** Waits for the last resolved element to be visible after the action. */
    public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            WebDriver driver = (WebDriver) engine.getNativeDriver();
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ExpectedConditions.visibilityOf(element));
        }
    };

    // ── Element manipulation ───────────────────────────────────────────────
    /** Highlights the last resolved element with a green border (success indicator). */
    public static final ActionHandler HIGHLIGHT_ELEMENT = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) {
            WebDriver driver = (WebDriver) engine.getNativeDriver();
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].style.border='6px solid green';", element);
        }
    };

    /** Scrolls the last resolved element into view after the action. */
    public static final ActionHandler SCROLL_TO_ELEMENT = engine -> {
        WebElement element = UIContext.getLastElement();
        if (element != null) DOMUtils.scrollToElement(element);
    };
}

