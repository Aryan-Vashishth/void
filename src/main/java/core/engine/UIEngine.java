package core.engine;

import elements.api.Element;
import elements.meta.ElementRole;
import java.time.Duration;

/**
 * UIEngine — VOID's Execution Contract
 * ─────────────────────────────────────
 * Single authority for all browser execution. Each engine implementation
 * (Selenium, Playwright, etc.) provides a concrete class.
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>One method per logical action — engine handles retry, fallback, waits internally.</li>
 *   <li>Every method added here must be implemented by <em>every</em> engine.</li>
 *   <li>{@code click()} is robust by default — includes wait, scroll, retry, JS fallback.</li>
 *   <li>Locators are received as {@link LocatorDescriptor} — engine translates internally.</li>
 * </ul>
 *
 * <h3>Engine responsibilities</h3>
 * <p>The engine owns ALL execution concerns:</p>
 * <ul>
 *   <li>Resolving {@link LocatorDescriptor} to native locators</li>
 *   <li>Scrolling elements into view (if needed)</li>
 *   <li>Waiting for visibility, clickability, or presence</li>
 *   <li>Retrying on stale element or intercept exceptions</li>
 *   <li>Fallback strategies (e.g., JS click when native fails)</li>
 * </ul>
 *
 * <p><b>Callers must NOT perform scroll, waits, or direct execution.
 * All such behavior is encapsulated within the engine.</b></p>
 *
 * <h3>Single execution path</h3>
 * <pre>
 *   Element → Action → Flow → FlowExecutor → UIEngine
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

    /**
     * Returns the title of the current page.
     *
     * @return page title string
     */
    String getTitle();

    /**
     * Reloads the current page.
     */
    void refresh();

    // ─────────────────────────────────────────────────────────────────────
    // RESOLUTION — engine owns locator resolution
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Resolves a {@link LocatorDescriptor} for an element using its role mapping.
     * This is the single resolution authority — elements call this
     * rather than importing resolver infrastructure directly.
     *
     * @param element the element descriptor (enum implementing Element)
     * @param role    the locator role to resolve (e.g., TRIGGER, INPUT)
     * @param args    optional format arguments for parameterized locators
     * @return resolved locator descriptor
     */
    LocatorDescriptor resolve(Element element, ElementRole role, Object... args);

    /**
     * Resolves a {@link LocatorDescriptor} from a raw file name, key, and args.
     * Used for locators that don't map to an element's role system.
     *
     * @param fileName properties or JSON file
     * @param key      locator key
     * @param args     optional format arguments
     * @return resolved locator descriptor
     */
    LocatorDescriptor resolve(String fileName, String key, Object... args);

    // ─────────────────────────────────────────────────────────────────────
    // CORE ACTIONS — each method is robust (retry + fallback built in)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Clicks the element identified by the locator.
     *
     * @param locator target element descriptor
     */
    void click(LocatorDescriptor locator);


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

    /**
     * Reads visible text, falling back to specified attributes if text is empty or truncated.
     * Useful for ellipsized cells or tooltip-bearing elements.
     *
     * @param locator    target element descriptor
     * @param endsWith   if the visible text ends with this suffix, treat as truncated (nullable)
     * @param attributes attribute names to try as fallbacks (in order), e.g., "title", "aria-label"
     * @return resolved text (trimmed)
     */
    String getTextWithAttributeFallback(LocatorDescriptor locator, String endsWith, String... attributes);

    /**
     * Returns the checked/selected state of a checkbox element.
     * Checks {@code aria-checked}, {@code checked} attribute, then {@code isSelected()} in order.
     *
     * @param locator target checkbox element descriptor
     * @return true if currently checked/selected
     */
    boolean getCheckboxState(LocatorDescriptor locator);

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

    /**
     * Waits for a CDK/Material overlay pane to appear in the DOM.
     * Used after triggering Angular Material dropdowns/menus.
     *
     * @param timeout maximum wait duration
     */
    void waitForOverlay(Duration timeout);

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
