package core.engine;

import java.time.Duration;
import java.util.List;

/**
 * UIEngine — VOID's Execution Contract
 * ─────────────────────────────────────
 * Defines the minimal set of browser operations that VOID's {@code Interactions}
 * layer requires. Each engine implementation (Selenium, Playwright, etc.) provides
 * a concrete class that fulfills this contract using its own native APIs.
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>Mirrors VOID's actual interaction vocabulary — not Selenium's API surface.</li>
 *   <li>Every method added here must be implemented by <em>every</em> engine.</li>
 *   <li>Edge cases use {@link #getNativeDriver()} as an explicit escape hatch.</li>
 *   <li>Locators are received as {@link LocatorDescriptor} — engine translates internally.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   engine.initialize(config);   // once at startup
 *   engine.navigateTo(url);
 *   engine.click(locator);
 *   engine.type(locator, text);
 *   engine.shutdown();           // once at teardown
 * </pre>
 *
 * @see LocatorDescriptor
 * @see EngineConfig
 */
public interface UIEngine {

    // ─────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Initializes the engine with the given configuration.
     * Called once before any interaction methods.
     *
     * @param config engine configuration
     */
    void initialize(EngineConfig config);

    /**
     * Shuts down the engine, releasing all resources (browser, driver, connections).
     */
    void shutdown();

    /**
     * Navigates the active browser context to the given URL.
     *
     * @param url target URL
     */
    void navigateTo(String url);

    /**
     * Returns the current page URL.
     *
     * @return current URL string
     */
    String getCurrentUrl();

    // ─────────────────────────────────────────────────────────────────────
    // CORE ACTIONS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Clicks the element identified by the locator.
     *
     * @param locator target element descriptor
     */
    void click(LocatorDescriptor locator);

    /**
     * Clicks using JavaScript execution (bypasses native event pipeline).
     *
     * @param locator target element descriptor
     */
    void jsClick(LocatorDescriptor locator);

    /**
     * Clears and types text into the element identified by the locator.
     *
     * @param locator target input element descriptor
     * @param text    text to type
     */
    void type(LocatorDescriptor locator, String text);

    /**
     * Types text into the element <em>without</em> clearing first (append).
     *
     * @param locator target input element descriptor
     * @param text    text to append
     */
    void appendType(LocatorDescriptor locator, String text);

    /**
     * Clears the content of the element identified by the locator.
     *
     * @param locator target input element descriptor
     */
    void clear(LocatorDescriptor locator);

    /**
     * Sends a keyboard key to the element (e.g., ENTER, TAB).
     *
     * @param locator target element descriptor
     * @param key     key name (engine normalizes)
     */
    void sendKey(LocatorDescriptor locator, String key);

    /**
     * Selects a dropdown option by visible text.
     *
     * @param locator target select/dropdown element
     * @param text    visible option text
     */
    void selectByVisibleText(LocatorDescriptor locator, String text);

    /**
     * Selects a dropdown option by value attribute.
     *
     * @param locator target select/dropdown element
     * @param value   option value
     */
    void selectByValue(LocatorDescriptor locator, String value);

    // ─────────────────────────────────────────────────────────────────────
    // RETRIEVAL
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the visible text content of the element.
     *
     * @param locator target element descriptor
     * @return trimmed text content
     */
    String getText(LocatorDescriptor locator);

    /**
     * Returns the value of the specified attribute on the element.
     *
     * @param locator   target element descriptor
     * @param attribute attribute name
     * @return attribute value or null
     */
    String getAttribute(LocatorDescriptor locator, String attribute);

    /**
     * Checks whether the element is currently visible/displayed.
     *
     * @param locator target element descriptor
     * @return true if visible
     */
    boolean isVisible(LocatorDescriptor locator);

    /**
     * Checks whether the element is currently enabled (interactable).
     *
     * @param locator target element descriptor
     * @return true if enabled
     */
    boolean isEnabled(LocatorDescriptor locator);

    /**
     * Checks whether the element is selected (for checkboxes/radios).
     *
     * @param locator target element descriptor
     * @return true if selected
     */
    boolean isSelected(LocatorDescriptor locator);

    /**
     * Returns the count of elements matching the locator.
     *
     * @param locator target descriptor
     * @return number of matching elements (0 if none)
     */
    int getElementCount(LocatorDescriptor locator);

    // ─────────────────────────────────────────────────────────────────────
    // WAITS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Waits for the element to become visible within the given timeout.
     *
     * @param locator target element descriptor
     * @param timeout maximum wait duration
     */
    void waitForVisible(LocatorDescriptor locator, Duration timeout);

    /**
     * Waits for the element to become clickable within the given timeout.
     *
     * @param locator target element descriptor
     * @param timeout maximum wait duration
     */
    void waitForClickable(LocatorDescriptor locator, Duration timeout);

    /**
     * Waits for all elements matching the locator to disappear.
     *
     * @param locator target element descriptor
     * @param timeout maximum wait duration
     */
    void waitForAbsence(LocatorDescriptor locator, Duration timeout);

    /**
     * Waits for the element to be present in DOM (not necessarily visible).
     *
     * @param locator target element descriptor
     * @param timeout maximum wait duration
     */
    void waitForPresence(LocatorDescriptor locator, Duration timeout);

    // ─────────────────────────────────────────────────────────────────────
    // ADVANCED
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Executes JavaScript in the browser context.
     *
     * @param script script body
     * @param args   arguments passed to the script
     * @return script return value (may be null)
     */
    Object executeScript(String script, Object... args);

    /**
     * Scrolls the element into view.
     *
     * @param locator target element descriptor
     */
    void scrollTo(LocatorDescriptor locator);

    /**
     * Uploads a file via the element (file input).
     *
     * @param locator  target file input element
     * @param filePath absolute path to the file
     */
    void uploadFile(LocatorDescriptor locator, String filePath);

    /**
     * Takes a full-page screenshot.
     *
     * @return screenshot bytes (PNG)
     */
    byte[] takeScreenshot();

    /**
     * Highlights the element with a visible border (debug aid).
     *
     * @param locator target element descriptor
     * @param color   CSS color (e.g., "red", "#ff0000")
     */
    void highlight(LocatorDescriptor locator, String color);

    /**
     * Hovers over the element.
     *
     * @param locator target element descriptor
     */
    void hover(LocatorDescriptor locator);

    // ─────────────────────────────────────────────────────────────────────
    // CONTEXT / ESCAPE HATCH
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns the native driver/page object for engine-specific escape hatches.
     * <p><b>Warning:</b> Using this breaks engine portability. Use only when
     * absolutely necessary and log a warning when invoked.</p>
     *
     * @return WebDriver (Selenium) or Page (Playwright) or equivalent
     */
    Object getNativeDriver();

    /**
     * Returns an identifier for this engine (e.g., "selenium", "playwright").
     *
     * @return engine name
     */
    String getEngineName();
}

