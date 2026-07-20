package core.utils.web;

import core.resolvers.locator.api.LocatorResolvers;

import elements.api.Element;
import elements.api.capability.ReadOnly;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import core.driver.DriverContext;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.warn;

/**
 * Explicit-wait utilities for Selenium interactions.
 *
 * <p>Wraps {@link org.openqa.selenium.support.ui.FluentWait} with convenience methods
 * for common wait scenarios: element visibility/clickability, Angular CDK overlay
 * stabilisation, custom loader spinners, and DOM flicker detection.</p>
 *
 * <p>All methods obtain the active driver from {@link core.driver.DriverContext}.
 * Default timeout is {@value #DEFAULT_TIMEOUT_SEC} seconds with {@value #DEFAULT_POLLING_MS}ms
 * polling.</p>
 *
 * <p>Used internally by {@link core.interactions.hooks.Before} and {@link core.interactions.hooks.After}
 * hook constants, and can be called directly from page objects or step definitions.</p>
 *
 * @see core.utils.web.DOMUtils
 * @see core.interactions.hooks.Before
 * @see core.interactions.hooks.After
 */
public class WaitUtils {

    private WaitUtils() { /* static utility */ }

    /**
     * @deprecated Application-specific selector embedded in framework code. Move the Angular
     *             loader wait to a profile hook in the test project instead.
     *             See {@link core.interactions.hooks.Before#WAIT_FOR_ANGULAR_LOADER}.
     */
    @Deprecated(forRemoval = true)
    public static final By ANGULAR_LOADER = By.tagName("app-loader");

    /**
     * @deprecated Application-specific selector embedded in framework code. Define spinner
     *             selectors in the test project and pass via {@link #resolveLoader(By)}.
     */
    @Deprecated(forRemoval = true)
    public static final By SPIN_SPINNER_LOADER = By.xpath("//span[contains(@class, 'spin spinner')]");
    private static final int DEFAULT_TIMEOUT_SEC = 20;
    private static final int DEFAULT_POLLING_MS = 200;
    private static final int DEFAULT_STABILITY_MS = 3000;


    private static FluentWait<WebDriver> getFluentWait(WebDriver driver, int timeoutSeconds, int pollingMillis) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    /**
     * @deprecated Accepts Selenium types ({@link WebDriver}, {@link ExpectedCondition}, {@link By}) directly,
     *             violating ADR-007. Use {@link core.engine.UIEngine} wait methods or
     *             {@link #waitForCondition(String, java.time.Duration, java.time.Duration, java.util.function.Supplier)}
     *             for engine-agnostic condition waits.
     */
    @Deprecated(forRemoval = true)
    public static <T> T waitForCondition(
            WebDriver driver,
            ExpectedCondition<T> condition,
            By locator,
            Integer escapeTimeInSeconds,
            Integer pollingRateInMillis,
            Boolean enableLogging,
            String conditionLabel
    ) {
        final int timeoutSeconds = (escapeTimeInSeconds != null) ? escapeTimeInSeconds : 15;
        final int pollingMillis = (pollingRateInMillis != null) ? pollingRateInMillis : 300;
        final boolean loggingEnabled = (enableLogging != null) ? enableLogging : true;

        if (driver == null) {
            driver = DriverContext.getDriver();
        }

        if (condition == null) {
            if (locator == null) return null;
            condition = (ExpectedCondition<T>) ExpectedConditions.visibilityOfElementLocated(locator);
        }

        String label = (conditionLabel != null && !conditionLabel.isBlank())
                ? conditionLabel
                : (locator != null ? "condition on element: " + locator : "anonymous condition");

        FluentWait<WebDriver> wait = getFluentWait(driver, timeoutSeconds, pollingMillis);

        try {
            debug.wait("Waiting for: " + label + " (timeout " + timeoutSeconds + "s)");
            T result = wait.until(condition);
            debug.success("Condition met before timeout.");
            return result;
        } catch (TimeoutException e) {
            warn.timeout("Wait condition [" + label + "] was not met within " + timeoutSeconds + "s.");
            return null;
        }
    }

    public static boolean waitForCondition(String conditionLabel,
                                           Duration timeout,
                                           Duration polling,
                                           java.util.function.Supplier<Boolean> condition) {
        WebDriver driver = DriverContext.getDriver();
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(polling)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        try {
            debug.wait("Waiting for: " + conditionLabel +
                    " (timeout " + timeout.toSeconds() + "s, polling " + polling.toMillis() + "ms)");
            boolean result = wait.until(drv -> condition.get());
            debug.success("Condition met before timeout.");
            return result;
        } catch (TimeoutException e) {
            warn.timeout("Wait condition [" + conditionLabel + "] was not met within " + timeout.toSeconds() + "s.");
            return false;
        }
    }


    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForVisible(core.engine.LocatorDescriptor, java.time.Duration)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void waitForElementToBeVisible(By locator) {
        waitForCondition(DriverContext.getDriver(), ExpectedConditions.visibilityOfElementLocated(locator), locator, 20, 200, true, "element to be visible: " + locator);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForAbsence(core.engine.LocatorDescriptor, java.time.Duration)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void waitForElementToDisappear(By locator) {
        waitForCondition(DriverContext.getDriver(), ExpectedConditions.invisibilityOfElementLocated(locator), locator, 20, 200, true, "element to disappear: " + locator);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForAbsence(core.engine.LocatorDescriptor, java.time.Duration)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void waitForElementToDisappear(ReadOnly element) {
        By locator = LocatorResolvers.strict().resolve(element);
        waitForElementToDisappear(locator);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForAbsence(core.engine.LocatorDescriptor, java.time.Duration)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean waitForElementToBeAbsent(By locator, int timeoutSeconds) {
        ExpectedCondition<Boolean> condition = ExpectedConditions.invisibilityOfElementLocated(locator);
        Boolean result = waitForCondition(
                DriverContext.getDriver(),
                condition,
                locator,
                timeoutSeconds,
                DEFAULT_POLLING_MS,
                false, // disable logging for negative checks
                "element to be absent: " + locator
        );
        return Boolean.TRUE.equals(result);
    }


    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForClickable(core.engine.LocatorDescriptor, java.time.Duration)} instead.
     */
    @Deprecated(forRemoval = true)
    public static void waitForElementToBeClickable(By locator) {
        waitForCondition(DriverContext.getDriver(),
                ExpectedConditions.elementToBeClickable(locator),
                locator,
                20,
                300,
                true,
                "element to be clickable: " + locator
        );
    }

    /**
     * @deprecated Angular CDK-specific concept. Move this wait to a profile hook in the test project.
     *             See {@link core.engine.UIEngine#waitForOverlay(java.time.Duration)} for overlay waits.
     */
    @Deprecated(forRemoval = true)
    public static void resolveAngularLoader() {
        resolveAngularLoader(3000, 20000, 200, true);
    }

    /**
     * @deprecated Angular CDK-specific concept. Move this wait to a profile hook in the test project.
     */
    @Deprecated(forRemoval = true)
    public static void resolveAngularLoader(int waitToAppearMs, int waitToDisappearMs, int pollingMs, boolean handleMultiple) {
        resolveLoader(ANGULAR_LOADER, waitToAppearMs, waitToDisappearMs, pollingMs, handleMultiple);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#waitForAbsence(core.engine.LocatorDescriptor, java.time.Duration)}
     *             for absence waits; pass a {@link core.engine.LocatorDescriptor} instead of {@link By}.
     */
    @Deprecated(forRemoval = true)
    public static void resolveLoader(@Nonnull By locator) {
        // Use the same default timings as Angular loader
        resolveLoader(locator, 3000, 20000, 200, false);
    }

    /**
     * @deprecated Use engine-agnostic wait methods on {@link core.engine.UIEngine} instead.
     */
    @Deprecated(forRemoval = true)
    public static void resolveLoader(@Nonnull By locator, @Nullable boolean handleMultiple) {
        // Use the same default timings as Angular loader
        resolveLoader(locator, 3000, 20000, 200, handleMultiple);
    }

    /**
     * @deprecated Use engine-agnostic wait methods on {@link core.engine.UIEngine} instead.
     */
    @Deprecated(forRemoval = true)
    public static void resolveLoader(@Nonnull By locator, int waitToAppearMs, int waitToDisappearMs) {
        resolveLoader(locator, waitToAppearMs, waitToDisappearMs, 200, true);
    }

    /**
     * @deprecated Use engine-agnostic wait methods on {@link core.engine.UIEngine} instead.
     *             For loader patterns, use {@link core.engine.UIEngine#waitForAbsence(core.engine.LocatorDescriptor, java.time.Duration)}.
     */
    @Deprecated(forRemoval = true)
    public static void resolveLoader(
            @Nonnull By locator,
            @Nullable Integer waitToAppearMs,
            @Nullable Integer waitToDisappearMs,
            @Nullable Integer pollingMs,
            @Nullable Boolean handleMultiple
    ) {
        WebDriver driver = DriverContext.getDriver();
        int appearMs = (waitToAppearMs != null) ? waitToAppearMs : 3000;
        int disappearMs = (waitToDisappearMs != null) ? waitToDisappearMs : 20000;
        int pollMs = (pollingMs != null) ? pollingMs : 200;
        boolean multi = (handleMultiple != null) ? handleMultiple : true;

        try {
            debug.wait("Waiting for loader (" + locator + ") to appear (timeout " + appearMs + " ms, polling " + pollMs + " ms)...");

            boolean appeared = Boolean.TRUE.equals(waitForCondition(
                    driver,
                    drv -> isLoaderPresent(drv, locator, multi),
                    locator,
                    appearMs / 1000,
                    pollMs,
                    true,
                    "loader (" + locator + ") to appear"
            ));

            if (!appeared) {
                debug.success("No loader (" + locator + ") appeared within " + appearMs + " ms Ã¢â‚¬â€ continuing without wait.");
                return;
            }

            debug.wait("Waiting for loader (" + locator + ") to disappear (timeout " + disappearMs + " ms, polling " + pollMs + " ms)...");

            boolean disappeared = Boolean.TRUE.equals(waitForCondition(
                    driver,
                    drv -> !isLoaderPresent(drv, locator, multi),
                    locator,
                    disappearMs / 1000,
                    pollMs,
                    true,
                    "loader (" + locator + ") to disappear"
            ));

            if (!disappeared) {
                warn.timeout("Loader (" + locator + ") did not disappear within " + disappearMs + " ms.");
                return;
            }

            debug.success("Loader (" + locator + ") disappeared.");

            // Optional: Flicker/stability check (if multi)
            if (multi) {
                debug.wait("Verifying loader (" + locator + ") does not reappear for stability window: " + DEFAULT_STABILITY_MS + " ms...");
                long deadline = System.currentTimeMillis() + DEFAULT_STABILITY_MS;

                while (System.currentTimeMillis() < deadline) {
                    if (isLoaderPresent(driver, locator, true)) {
                        warn.log("Loader (" + locator + ") reappeared during stability check. Waiting for disappearance again...");

                        boolean goneAgain = Boolean.TRUE.equals(waitForCondition(
                                driver,
                                drv -> !isLoaderPresent(drv, locator, true),
                                locator,
                                disappearMs / 1000,
                                pollMs,
                                true,
                                "loader (" + locator + ") to re-disappear"
                        ));

                        if (!goneAgain) {
                            warn.timeout("Loader (" + locator + ") did not re-disappear after flickering.");
                            return;
                        }

                        deadline = System.currentTimeMillis() + DEFAULT_STABILITY_MS;
                        debug.log("Stability window reset due to loader flicker.");
                    }
                    Thread.sleep(pollMs);
                }

                debug.success("Loader (" + locator + ") is stable Ã¢â‚¬â€ no flicker detected during stability window.");
            }
        } catch (Exception e) {
            warn.timeout("Loader (" + locator + ") wait failed with exception: " + e.getMessage());
        }
    }



    private static boolean isLoaderPresent(WebDriver driver, By locator, boolean handleMultiple) {
        try {
            if (handleMultiple) {
                List<WebElement> loaders = driver.findElements(locator);
                return loaders.stream().anyMatch(WebElement::isDisplayed);
            } else {
                WebElement loader = driver.findElement(locator);
                return loader.isDisplayed();
            }
        } catch (NoSuchElementException | StaleElementReferenceException ignored) {
            return false;
        }
    }

    /**
     * @deprecated Use engine-agnostic retrieval and assertion via {@link core.engine.UIEngine#getText(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean waitForElementTextToBePresent(By locator, String expectedText, int timeoutSeconds) {
        ExpectedCondition<Boolean> condition = driver -> {
            try {
                assert driver != null;
                WebElement element = driver.findElement(locator);
                String actualText = element.getText();
                if (actualText == null || actualText.trim().isEmpty()) return false;
                return expectedText == null || actualText.contains(expectedText);
            } catch (Exception e) {
                return false;
            }
        };

        String label = (expectedText == null)
                ? "any non-blank text in element: " + locator
                : "text '" + expectedText + "' in element: " + locator;

        Boolean result = waitForCondition(
                DriverContext.getDriver(),
                condition,
                locator,
                timeoutSeconds,
                DEFAULT_POLLING_MS,
                true,
                label
        );

        if (Boolean.TRUE.equals(result)) {
            debug.success("Element text matched condition: " + locator);
            return true;
        } else {
            warn.timeout("Element text condition not met: " + locator);
            return false;
        }
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#getText(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean waitForElementTextToBePresent(By locator, String expectedText) {
        return waitForElementTextToBePresent(locator, expectedText, DEFAULT_TIMEOUT_SEC);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#getText(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean waitForElementTextToBePresent(By locator) {
        return waitForElementTextToBePresent(locator, null, DEFAULT_TIMEOUT_SEC);
    }

    /**
     * @deprecated Use {@link core.engine.UIEngine#getText(core.engine.LocatorDescriptor)} instead.
     */
    @Deprecated(forRemoval = true)
    public static boolean waitForElementTextToBePresent(By locator, int timeoutSeconds) {
        return waitForElementTextToBePresent(locator, null, timeoutSeconds);
    }

    // Add near the top of the class for reuse
    private static final int QUICK_OBSERVE_MS = 500; // short, low-risk window

    /**
     * @deprecated Experimental variant -- use engine-agnostic wait methods on {@link core.engine.UIEngine} instead.
     */
    @Deprecated(forRemoval = true)
    public static void resolveLoaderTemp(
            @Nonnull By locator,
            @Nullable Integer waitToAppearMs,
            @Nullable Integer waitToDisappearMs,
            @Nullable Integer pollingMs,
            @Nullable Boolean handleMultiple
    ) {
        WebDriver driver = DriverContext.getDriver();
        final int appearMs   = (waitToAppearMs != null)    ? waitToAppearMs    : 3000;
        final int disappearMs= (waitToDisappearMs != null) ? waitToDisappearMs : 20000;
        final int pollMs     = (pollingMs != null)         ? pollingMs         : 200;
        final boolean multi  = (handleMultiple != null)    ? handleMultiple    : true;

        try {
            // ---- PHASE 0: immediate short-circuit if loader is ALREADY visible ----
            boolean initiallyVisible = isLoaderPresent(driver, locator, multi);
            if (!initiallyVisible) {
                // ---- PHASE 1: APPEAR (debounced) ----
                debug.wait("Waiting for loader (" + locator + ") to appear up to " + appearMs + " ms...");
                long appearDeadline = System.currentTimeMillis() + appearMs;
                boolean appearedStable = false;

                // Require two consecutive positive samples to avoid transient DOM blips
                boolean prevSeen = false;
                while (System.currentTimeMillis() < appearDeadline) {
                    boolean seen = isLoaderPresent(driver, locator, multi);
                    if (seen && prevSeen) { appearedStable = true; break; }
                    prevSeen = seen;
                    Thread.sleep(pollMs);
                }

                if (!appearedStable) {
                    debug.success("No loader (" + locator + ") appeared Ã¢â‚¬â€ continuing immediately.");
                    return; // nothing to wait for
                }
            } else {
                debug.log("Loader (" + locator + ") already visible Ã¢â‚¬â€ skipping 'appear' phase.");
            }

            // ---- PHASE 2: DISAPPEAR ----
            debug.wait("Waiting for loader (" + locator + ") to disappear (timeout " + disappearMs + " ms)...");
            long disappearDeadline = System.currentTimeMillis() + disappearMs;
            boolean gone = false;
            while (System.currentTimeMillis() < disappearDeadline) {
                if (!isLoaderPresent(driver, locator, multi)) { gone = true; break; }
                Thread.sleep(pollMs);
            }
            if (!gone) {
                warn.timeout("Loader (" + locator + ") did not disappear within " + disappearMs + " ms.");
                return;
            }
            debug.success("Loader (" + locator + ") disappeared.");

            // ---- PHASE 3: ADAPTIVE STABILITY (only-if-flicker) ----
            if (multi) {
                // Quick probe: if it reappears soon, escalate to full stabilityMs (3s default)
                debug.wait("Quick flicker check for loader (" + locator + ") ~" + QUICK_OBSERVE_MS + " ms...");
                long quickDeadline = System.currentTimeMillis() + QUICK_OBSERVE_MS;
                boolean flickered = false;
                while (System.currentTimeMillis() < quickDeadline) {
                    if (isLoaderPresent(driver, locator, true)) { flickered = true; break; }
                    Thread.sleep(pollMs);
                }

                if (!flickered) {
                    debug.success("No flicker detected in quick window Ã¢â‚¬â€ finishing immediately.");
                    return; // early out: no 3s tax
                }

                // Escalate to full stability window only if flicker detected
                debug.wait("Flicker detected Ã¢â‚¬â€ enforcing full stability window (" + DEFAULT_STABILITY_MS + " ms)...");
                long stabilityDeadline = System.currentTimeMillis() + DEFAULT_STABILITY_MS;
                while (System.currentTimeMillis() < stabilityDeadline) {
                    if (isLoaderPresent(driver, locator, true)) {
                        // wait again for disappearance and reset stability window
                        long reDisDeadline = System.currentTimeMillis() + disappearMs;
                        while (System.currentTimeMillis() < reDisDeadline && isLoaderPresent(driver, locator, true)) {
                            Thread.sleep(pollMs);
                        }
                        stabilityDeadline = System.currentTimeMillis() + DEFAULT_STABILITY_MS;
                    }
                    Thread.sleep(pollMs);
                }
                debug.success("Stable Ã¢â‚¬â€ no further loader flicker detected.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            warn.timeout("Loader wait interrupted: " + ie.getMessage());
        } catch (Exception e) {
            warn.timeout("Loader wait failed: " + e.getMessage());
        }
    }


}
