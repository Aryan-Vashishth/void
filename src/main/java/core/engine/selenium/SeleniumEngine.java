package core.engine.selenium;

import core.driver.DriverContext;
import core.driver.DriverFactory;
import core.engine.EngineConfig;
import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.engine.UIEngine;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.Element;
import elements.meta.ElementRole;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static core.logging.CustomLogger.*;

/**
 * Selenium-based {@link UIEngine} implementation.
 *
 * <p>Wraps a {@link WebDriver} instance and translates {@link LocatorDescriptor}
 * into Selenium-native {@link By} locators. This is the default engine for VOID
 * and preserves all existing Selenium behavior.</p>
 *
 * <h3>Thread safety</h3>
 * <p>One {@code SeleniumEngine} instance per VOID session (per thread).
 * Not shared across threads.</p>
 */
public final class SeleniumEngine implements UIEngine {

    /** Engine identifier registered in {@link core.engine.UIEngineFactory}. */
    public static final String ID = "selenium";

    private final DriverFactory.Profile profile;  // null on legacy path
    private WebDriver driver;                      // null until initialize() on primary path
    private EngineConfig config;
    private Duration defaultTimeout;

    /**
     * Primary constructor: engine manages its own driver lifecycle.
     * Driver is created during {@link #initialize(EngineConfig)}.
     *
     * @param profile driver configuration profile
     */
    public SeleniumEngine(DriverFactory.Profile profile) {
        this.profile = profile;
        this.driver = null;
    }

    /**
     * Compatibility bridge for {@code Interactions(WebDriver)}.
     * Kept until {@code Interactions(WebDriver)} is removed.
     *
     * @param driver active WebDriver instance (already created by caller)
     * @deprecated use {@link #SeleniumEngine(DriverFactory.Profile)} via the factory
     */
    @Deprecated(forRemoval = true)
    public SeleniumEngine(WebDriver driver) {
        this.profile = null;
        this.driver = driver;
        this.defaultTimeout = Duration.ofSeconds(10);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(EngineConfig config) {
        this.config = config;
        this.defaultTimeout = config.getDefaultTimeout();

        configureLogging();

        if (this.driver == null) {
            // Primary path: engine creates and registers its own driver.
            this.driver = DriverFactory.fromProfile(profile).build();
            DriverContext.setPrimaryDriver(this.driver);
            debug.log("[SeleniumEngine] Driver created and registered via profile: " + profile);
        } else {
            // Legacy path: driver was provided by caller (Interactions bridge).
            debug.log("[SeleniumEngine] Driver provided externally (legacy path).");
        }

        debug.log("[SeleniumEngine] Initialized with timeout=" + defaultTimeout.toSeconds() + "s");
    }

    @Override
    public void shutdown() {
        if (driver != null) {
            try {
                driver.quit();
                debug.log("[SeleniumEngine] Driver shut down.");
            } catch (Exception e) {
                warn.log("[SeleniumEngine] Error during shutdown: " + e.getMessage());
            } finally {
                if (DriverContext.hasPrimary()) {
                    DriverContext.removePrimary();
                    debug.log("[SeleniumEngine] Driver removed from DriverContext.");
                }
            }
        }
    }

    @Override
    public void navigateTo(String url) {
        driver.get(url);
        info.navigate(url);
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public void refresh() {
        driver.navigate().refresh();
        info.navigate("Page refreshed.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESOLUTION
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public LocatorDescriptor resolve(Element element, ElementRole role, Object... args) {
        return LocatorResolvers.strict().resolveDescriptor(element, role, args);
    }

    @Override
    public LocatorDescriptor resolve(String fileName, String key, Object... args) {
        return LocatorResolvers.strict().resolveDescriptor(fileName, key, args);
    }

    // ─────────────────────────────────────────────────────────────────────
    // CORE ACTIONS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Robust click: wait-visible → wait-clickable → scroll → highlight → click → JS fallback → stale retry.
     */
    @Override
    public void click(LocatorDescriptor locator) {
        By by = toBy(locator);
        String currentUrl = driver.getCurrentUrl();

        // Phase 1: Wait visible + clickable + scroll + highlight
        try {
            WebElement element = new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(by));
            scrollToElement(element);
            highlightElement(element, "red");
            new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.elementToBeClickable(by));
        } catch (Exception e) {
            debug.log("[SeleniumEngine] Pre-click wait failed for: " + locator + " — " + e.getMessage());
        }

        // Phase 2: Standard click
        try {
            WebElement element = driver.findElement(by);
            String text = safeText(element);
            element.click();
            info.click(clickLabel(text, locator));
            debug.click("Clicked using Selenium click(). Locator: " + locator);
            return;
        } catch (StaleElementReferenceException staleEx) {
            if (urlChanged(currentUrl)) {
                debug.log("[SeleniumEngine] Page navigated after stale — treating as success.");
                return;
            }
            debug.error("[SeleniumEngine] Stale element on standard click, will retry...");
        } catch (Exception e) {
            debug.log("[SeleniumEngine] Standard click failed for: " + locator + " — " + e.getMessage());
        }

        // Phase 3: JS click fallback
        warn.fallback("[SeleniumEngine] Retrying with JavaScript click for: " + locator);
        try {
            WebElement element = driver.findElement(by);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            info.click(clickLabel(safeText(element), locator));
            debug.success("Clicked using JavaScriptExecutor.");
            return;
        } catch (StaleElementReferenceException staleEx) {
            if (urlChanged(currentUrl)) {
                debug.log("[SeleniumEngine] Page navigated after stale (JS) — treating as success.");
                return;
            }
            debug.error("[SeleniumEngine] Stale on JS click, will re-locate and retry...");
        } catch (Exception e) {
            debug.log("[SeleniumEngine] JS click also failed: " + e.getMessage());
        }

        // Phase 4: Re-locate and final attempt
        try {
            WebElement freshElement = new WebDriverWait(driver, defaultTimeout)
                    .until(ExpectedConditions.elementToBeClickable(by));
            freshElement.click();
            info.click(clickLabel(safeText(freshElement), locator));
        } catch (Exception retryEx) {
            error.failed("[SeleniumEngine] click() exhausted all strategies for: " + locator);
            throw new RuntimeException("click failed for: " + locator, retryEx);
        }
    }

    @Override
    public void type(LocatorDescriptor locator, String text) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.visibilityOfElementLocated(by));
        scrollToElement(element);
        element.clear();
        element.sendKeys(text);
        if (isPasswordLocator(locator)) {
            info.password(text, labelFor(locator));
        } else {
            info.input("Typed: " + text + " | " + labelFor(locator));
        }
    }

    @Override
    public void appendType(LocatorDescriptor locator, String text) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.visibilityOfElementLocated(by));
        scrollToElement(element);
        element.sendKeys(text);
        info.input("Appended: " + text + " | " + labelFor(locator));
    }

    @Override
    public void clear(LocatorDescriptor locator) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.visibilityOfElementLocated(by));
        element.clear();
        info.clear("Cleared: " + labelFor(locator));
    }

    @Override
    public void sendKey(LocatorDescriptor locator, String key) {
        By by = toBy(locator);
        WebElement element = driver.findElement(by);
        Keys seleniumKey = mapKey(key);
        element.sendKeys(seleniumKey);
        info.key(key + " | " + labelFor(locator));
    }

    @Override
    public void selectByVisibleText(LocatorDescriptor locator, String text) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.visibilityOfElementLocated(by));
        new org.openqa.selenium.support.ui.Select(element).selectByVisibleText(text);
        info.dropdown("Selected: " + text + " | " + labelFor(locator));
    }

    @Override
    public void selectByValue(LocatorDescriptor locator, String value) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.visibilityOfElementLocated(by));
        new org.openqa.selenium.support.ui.Select(element).selectByValue(value);
        info.dropdown("Selected: " + value + " | " + labelFor(locator));
    }


    // ─────────────────────────────────────────────────────────────────────
    // RETRIEVAL
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public String getText(LocatorDescriptor locator) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.presenceOfElementLocated(by));
        return element.getText().trim();
    }

    @Override
    public String getAttribute(LocatorDescriptor locator, String attribute) {
        By by = toBy(locator);
        WebElement element = driver.findElement(by);
        return element.getAttribute(attribute);
    }

    @Override
    public boolean isVisible(LocatorDescriptor locator) {
        try {
            By by = toBy(locator);
            List<WebElement> elements = driver.findElements(by);
            return !elements.isEmpty() && elements.get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEnabled(LocatorDescriptor locator) {
        try {
            By by = toBy(locator);
            WebElement element = driver.findElement(by);
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isSelected(LocatorDescriptor locator) {
        try {
            By by = toBy(locator);
            WebElement element = driver.findElement(by);
            return element.isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getElementCount(LocatorDescriptor locator) {
        By by = toBy(locator);
        return driver.findElements(by).size();
    }

    @Override
    public String getTextWithAttributeFallback(LocatorDescriptor locator, String endsWith, String... attributes) {
        By by = toBy(locator);
        WebElement element = waitFor(by).until(ExpectedConditions.presenceOfElementLocated(by));
        scrollToElement(element);
        String text = element.getText().trim();

        // If text is non-empty and not truncated, return it
        if (!text.isEmpty() && (endsWith == null || !text.endsWith(endsWith))) {
            return text;
        }

        // Fallback: try each attribute in order
        for (String attr : attributes) {
            String value = element.getAttribute(attr);
            if (value != null && !value.trim().isEmpty()) {
                debug.log("[SeleniumEngine] Text fallback to attribute '" + attr + "': " + value.trim());
                return value.trim();
            }
        }

        return text; // return whatever we have
    }

    @Override
    public boolean getCheckboxState(LocatorDescriptor locator) {
        By by = toBy(locator);
        WebElement cb = driver.findElement(by);
        try {
            String aria = cb.getAttribute("aria-checked");
            if (aria != null && !aria.isBlank()) {
                return aria.equalsIgnoreCase("true");
            }
            String checkedAttr = cb.getAttribute("checked");
            if (checkedAttr != null) {
                return true;
            }
            return cb.isSelected();
        } catch (Exception ignored) {
            return cb.isSelected();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // WAITS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void waitForVisible(LocatorDescriptor locator, Duration timeout) {
        By by = toBy(locator);
        new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    @Override
    public void waitForClickable(LocatorDescriptor locator, Duration timeout) {
        By by = toBy(locator);
        new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.elementToBeClickable(by));
    }

    @Override
    public void waitForAbsence(LocatorDescriptor locator, Duration timeout) {
        By by = toBy(locator);
        new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.invisibilityOfElementLocated(by));
    }

    @Override
    public void waitForPresence(LocatorDescriptor locator, Duration timeout) {
        By by = toBy(locator);
        new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.presenceOfElementLocated(by));
    }

    @Override
    public void waitForOverlay(Duration timeout) {
        By overlayPane = By.cssSelector("div.cdk-overlay-pane");
        try {
            new FluentWait<>(driver)
                    .withTimeout(timeout)
                    .pollingEvery(Duration.ofMillis(100))
                    .ignoring(NoSuchElementException.class)
                    .until(drv -> !drv.findElements(overlayPane).isEmpty());
            debug.log("[SeleniumEngine] CDK overlay appeared.");
        } catch (Exception e) {
            debug.log("[SeleniumEngine] Overlay wait timed out — continuing.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADVANCED
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    @Override
    public void scrollTo(LocatorDescriptor locator) {
        try {
            By by = toBy(locator);
            WebElement element = driver.findElement(by);
            scrollToElement(element);
        } catch (Exception e) {
            // Best-effort — element may be gone if the action caused a navigation.
            debug.log("[SeleniumEngine] scrollTo() skipped — element no longer present: " + e.getMessage());
        }
    }

    @Override
    public void uploadFile(LocatorDescriptor locator, String filePath) {
        By by = toBy(locator);
        WebElement element = driver.findElement(by);
        element.sendKeys(filePath);
        info.upload("Uploaded: " + filePath + " | " + labelFor(locator));
    }

    @Override
    public byte[] takeScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    public void highlight(LocatorDescriptor locator, String color) {
        try {
            By by = toBy(locator);
            WebElement element = driver.findElement(by);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='6px solid " + color + "';", element);
        } catch (Exception e) {
            // Best-effort — element may be gone if the action caused a navigation.
            debug.log("[SeleniumEngine] highlight() skipped — element no longer present: " + e.getMessage());
        }
    }

    @Override
    public void hover(LocatorDescriptor locator) {
        By by = toBy(locator);
        WebElement element = driver.findElement(by);
        new Actions(driver).moveToElement(element).perform();
        info.hover("Hovered: " + labelFor(locator));
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONTEXT / ESCAPE HATCH
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public Object getNativeDriver() {
        warn.log("[SeleniumEngine] getNativeDriver() called — breaks engine portability.");
        return driver;
    }

    @Override
    public String getEngineName() {
        return "selenium";
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC HELPERS (Selenium-specific, used by legacy compatibility)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the underlying WebDriver.
     * <p><b>For internal/legacy use only</b> — prefer UIEngine methods.</p>
     */
    public WebDriver getWebDriver() {
        return driver;
    }

    /**
     * Translates a {@link LocatorDescriptor} to a Selenium {@link By}.
     * Exposed for backward compatibility with hooks and utilities that
     * still operate on {@code By} during the transition period.
     *
     * @param descriptor locator descriptor
     * @return Selenium By
     */
    public static By toBy(LocatorDescriptor descriptor) {
        if (descriptor == null || descriptor.value() == null) {
            throw new IllegalArgumentException("LocatorDescriptor or value cannot be null");
        }
        return switch (descriptor.strategy()) {
            case XPATH -> By.xpath(descriptor.value());
            case CSS -> By.cssSelector(descriptor.value());
            case ID -> By.id(descriptor.value());
            case NAME -> By.name(descriptor.value());
        };
    }

    /**
     * Creates a {@link LocatorDescriptor} from a Selenium {@link By}.
     * Used during the transition period for backward compatibility.
     *
     * @param by Selenium By locator
     * @return equivalent LocatorDescriptor
     * @deprecated Use {@link core.bridge.selenium.SeleniumLocatorBridge#fromBy(By)} instead.
     *             Will be removed with the {@code By}/{@code WebElement} deprecated API workstream.
     */
    @Deprecated(forRemoval = true)
    public static LocatorDescriptor fromBy(By by) {
        String byString = by.toString();
        if (byString.startsWith("By.xpath:")) {
            return LocatorDescriptor.of(byString.substring("By.xpath: ".length()), LocatorStrategy.XPATH);
        } else if (byString.startsWith("By.cssSelector:")) {
            return LocatorDescriptor.of(byString.substring("By.cssSelector: ".length()), LocatorStrategy.CSS);
        } else if (byString.startsWith("By.id:")) {
            return LocatorDescriptor.of(byString.substring("By.id: ".length()), LocatorStrategy.ID);
        } else if (byString.startsWith("By.name:")) {
            return LocatorDescriptor.of(byString.substring("By.name: ".length()), LocatorStrategy.NAME);
        }
        // Fallback: treat the entire toString as CSS
        return LocatorDescriptor.of(byString, LocatorStrategy.CSS);
    }

    // ─────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Finds an element, respecting scoped (parent→child) descriptors.
     * If the descriptor has a parent, finds within the parent scope.
     */
    private WebElement findElement(LocatorDescriptor locator) {
        if (locator.isScoped()) {
            WebElement parent = findElement(locator.parent());
            return parent.findElement(toBy(locator));
        }
        return driver.findElement(toBy(locator));
    }

    private WebDriverWait waitFor(By by) {
        return new WebDriverWait(driver, defaultTimeout);
    }

    private void scrollToElement(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", element);
        } catch (Exception ignored) {
            // scroll is best-effort
        }
    }

    private void highlightElement(WebElement element, String color) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='6px solid " + color + "';", element);
        } catch (Exception ignored) {}
    }

    private boolean urlChanged(String originalUrl) {
        try {
            return !java.util.Objects.equals(originalUrl, driver.getCurrentUrl());
        } catch (Exception e) {
            return false;
        }
    }

    private static String safeText(WebElement el) {
        try { return el.getText().trim(); } catch (Exception ignored) { return ""; }
    }

    private static String labelFor(LocatorDescriptor locator) {
        if (locator.label() != null) return locator.label();
        Object[] args = locator.args();
        if (args != null && args.length > 0) return String.valueOf(args[0]);
        return locator.value();
    }

    private static String clickLabel(String visibleText, LocatorDescriptor locator) {
        String label = labelFor(locator);
        return (visibleText == null || visibleText.isBlank())
                ? "Clicked: " + label
                : "Clicked: " + visibleText + " | " + label;
    }

    private static boolean isPasswordLocator(LocatorDescriptor locator) {
        String value = locator.value() != null ? locator.value().toLowerCase() : "";
        if (value.contains("password")) return true;
        Object[] args = locator.args();
        if (args != null) {
            for (Object arg : args) {
                if (String.valueOf(arg).toLowerCase().contains("password")) return true;
            }
        }
        return false;
    }

    private static void configureLogging() {
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
    }

    private static Keys mapKey(String key) {
        if (key == null) return Keys.ENTER;
        return switch (key.toUpperCase()) {
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            case "ESCAPE", "ESC" -> Keys.ESCAPE;
            case "BACKSPACE", "BACK_SPACE" -> Keys.BACK_SPACE;
            case "DELETE" -> Keys.DELETE;
            case "SPACE" -> Keys.SPACE;
            case "ARROW_DOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROW_UP", "UP" -> Keys.ARROW_UP;
            case "ARROW_LEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            case "HOME" -> Keys.HOME;
            case "END" -> Keys.END;
            case "PAGE_UP" -> Keys.PAGE_UP;
            case "PAGE_DOWN" -> Keys.PAGE_DOWN;
            default -> Keys.valueOf(key.toUpperCase());
        };
    }
}

