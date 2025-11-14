package interactions;

import Elements.ElementRole;
import Elements.InfoElements;
import Elements.interfacesv1.*; // v1 interfaces only
import core.driver.DriverContext;
import core.resolvers.locator.LocatorResolverV1;
import core.utils.*;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.*;
import com.beust.jcommander.internal.Nullable;
import static core.logging.CustomLogger.*;

/**
 * Interactions
 * ------------
 * Central place for all UI actions in the framework. This class is intentionally
 * enum/interface-driven so that new UI elements can be added simply by defining
 * enums that implement the appropriate *Element interface, without touching
 * the core interaction logic.
 *
 * <h3>Key ideas</h3>
 * <ul>
 *   <li>Resolve everything from enums → locators via properties/json resolver,
 *       keeping all CSS/XPath selectors in properties files.</li>
 *   <li>Provide before/after action hooks so callers can compose behaviors
 *       (e.g., waits, scrolls, highlights).</li>
 *   <li>Log the arguments and final locators for each action so test traces
 *       remain accurate and debuggable.</li>
 * </ul>
 *
 * <h3>Angular Material Menus (Three Dots)</h3>
 * <ul>
 *   <li>Trigger (row index) and List Item (label) must be treated separately:</li>
 *   <ul>
 *     <li><b>Trigger property</b> (example): <code>(//td[@role='cell']/button)[%s]</code>
 *         → expects one argument: the row index.</li>
 *     <li><b>List property</b> (example):<br/>
 *         <code>(//div[contains(@class,'cdk-overlay-pane')]//div[@role='menu']//
 *         button[@role='menuitem' and normalize-space()='%s'])[last()]</code><br/>
 *         → expects one argument: the visible label (e.g., "View Registration").</li>
 *   </ul>
 *   <li>After clicking the trigger, Angular CDK renders the menu into a detached
 *       overlay. Always wait for the overlay to appear using
 *       {@code waitForOverlayToAppear()} before trying to click an item.</li>
 * </ul>
 *
 * <h3>Logging correctness</h3>
 * <ul>
 *   <li>Arguments for TRIGGER vs LIST are logged separately, along with the final
 *       resolved {@link By} locators. This avoids confusion such as substituting
 *       "1" (index) into the label placeholder.</li>
 * </ul>
 *
 * <h3>Robustness</h3>
 * <ul>
 *   <li>Click flow includes JS fallback and stale-element retry.</li>
 *   <li>Autocomplete and search flows scroll into view and/or use Actions to
 *       handle overlay edge cases.</li>
 * </ul>
 */

public class Interactions {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected final Logger log = Logger.getLogger(Interactions.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Standard constructor, initializes driver context and logging.
     *
     * @param driver The WebDriver instance to use for all actions.
     */
    public Interactions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        initialize(this.getClass());
        DriverContext.setDriver(driver);
    }

    public String getText(InfoElements elements) {
        By locator = LocatorResolverV1.getLocator(elements);
        WebElement webElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        String text = webElement.getText().trim();
        info.text("Retrieved from → " + elements.getDisplayText() + ": " + text);
        return text;
    }

    /** Functional interface for before/after action hooks. */
    @FunctionalInterface
    public interface ActionHandler { void execute(WebDriver driver); }

    /**
     * Helper for collecting hooks in a single call.
     *
     * @param handlers optional list of handlers
     * @return list or null if empty
     */
    public static List<ActionHandler> of(ActionHandler... handlers) {
        return (handlers == null || handlers.length == 0) ? null : List.of(handlers);
    }

    // ====== COMMON BEFORE/ACTION HOOKS ======
    public static class Before {
        public static final ActionHandler DO_NOTHING = driver -> {};
        public static final ActionHandler LOG_INTENT = driver -> debug.log("[DEBUG] Performing UI action...");
        public static final ActionHandler WAIT_FOR_ANGULAR_LOADER = driver -> WaitUtils.resolveAngularLoader();
        public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = driver -> WaitUtils.resolveLoader(WaitUtils.SPIN_SPINNER_LOADER);

        /** Waits for the last resolved element to become clickable (uses UIContext). */
        public static final ActionHandler WAIT_FOR_ELEMENT_CLICKABLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.elementToBeClickable(element));
            }
        };

        /** Clears the last resolved element if it is an input. */
        public static final ActionHandler CLEAR_FIELD = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                element.clear();
            } else {
                throw new IllegalStateException("[BEFORE] Cannot clear field – UIContext.getLastElement() is null.");
            }
        };

        /** Waits for the last element to be visible. */
        public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOf(element));
            }
        };

        /** Scrolls the last element into view. */
        public static final ActionHandler SCROLL_TO_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                DOMUtils.scrollToElement(element);
            }
        };

        /** Highlights the last element for debug. */
        public static final ActionHandler HIGHLIGHT_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='6px solid red';", element);
            }
        };
    }

    public static class After {
        public static final ActionHandler DO_NOTHING = driver -> {};
        public static final ActionHandler WAIT_FOR_ANGULAR_LOADER = driver -> WaitUtils.resolveAngularLoader();
        public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = driver -> WaitUtils.resolveLoader(WaitUtils.SPIN_SPINNER_LOADER);

        /** Waits for last element to be visible after action. */
        public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOf(element));
            }
        };

        /** Highlights last element with green border for visual validation. */
        public static final ActionHandler HIGHLIGHT_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='6px solid green';", element);
            }
        };

        /** Scrolls the last element into view post-action. */
        public static final ActionHandler SCROLL_TO_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                DOMUtils.scrollToElement(element);
            }
        };
    }

    // ======================= GENERIC TEXT RETRIEVAL =======================

    /**
     * Reads the trimmed text from a locator, scrolling into view and updating UIContext for hooks.
     *
     * @param locator target locator
     * @return element text (trimmed)
     * @throws RuntimeException on failure to locate/read
     */
    public String getTextByWebElement(By locator) {
        try {
            WebElement targetText = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            try { DOMUtils.scrollToElement(targetText); } catch (Exception ignored) {}
            UIContext.setLastElement(targetText);
            String text = targetText.getText().trim();
            debug.text("Retrieved from → " + locator + ": " + text);
            return text;
        } catch (Exception e) {
            error.log("Failed to get text from element: " + locator + " " + e);
            throw new RuntimeException("Failed to get text from: " + locator, e);
        }
    }

    /**
     * Gets text from a read-only element with optional before/after actions.
     *
     * @param beforeActions optional hooks
     * @param element       read-only element
     * @param afterActions  optional hooks
     * @return trimmed text
     */
    public String getText(@Nullable List<ActionHandler> beforeActions,
                          ReadOnlyElement element,
                          @Nullable List<ActionHandler> afterActions) {
        if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
        By locator = LocatorResolverV1.getLocator(element); // role-based best available
        if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        return getTextByWebElement(locator);
    }

    /** Simple overload: no hooks. */
    public String getText(ReadOnlyElement element) { return getText(null, element, null); }

    /**
     * Reads visible text, with a tooltip fallback if truncated (common for ellipsized cells).
     *
     * @param beforeActions        optional hooks before
     * @param element              tooltip-capable element
     * @param afterActions         optional hooks after
     * @param enableResolveTooltip if true, will try hover-based resolution
     * @return resolved text
     * @throws RuntimeException on failure
     */
    public String getTextViaToolTip(@Nullable List<ActionHandler> beforeActions,
                                    ToolTipElement element,
                                    @Nullable List<ActionHandler> afterActions,
                                    boolean enableResolveTooltip) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            By locator = LocatorResolverV1.getLocator(element); // role-based best available
            WebElement targetElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String unresolvedText = getTextByWebElement(locator);
            info.text("Retrieved from → " + element.getDisplayText() + " Unresolved Text: " + unresolvedText);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);

            // If truncated, try hover-based resolver or title/aria-label fallbacks
            if (enableResolveTooltip && !unresolvedText.isEmpty() && unresolvedText.endsWith(element.getEndsWith())) {
                debug.log("[DEBUG] Unresolved text ends with '" + element.getEndsWith() + "'. Skipping hover resolution (disabled).");
                return unresolvedText; // Keep truncated if resolver disabled
            }
            if (unresolvedText.isEmpty() || unresolvedText.endsWith(element.getEndsWith())) {
                String tooltipAttr = targetElement.getAttribute("title");
                String ariaLabelAttr = targetElement.getAttribute("aria-label");
                String tooltipFallback = tooltipAttr != null && !tooltipAttr.trim().isEmpty() ? tooltipAttr.trim() : ariaLabelAttr;
                if (tooltipFallback != null && !tooltipFallback.trim().isEmpty()) {
                    info.fallback("Fallback to tooltip attribute for → " + element.getDisplayText() + ": " + tooltipFallback);
                    return tooltipFallback;
                }
            }
            return unresolvedText;
        } catch (Exception e) {
            error.text("Failed to get text with tooltip fallback: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Failed to get text with tooltip fallback for: " + element.getDisplayText(), e);
        }
    }

    // ======================= CLICK HANDLING (ALL OVERLOADS) =======================

    /**
     * Clicks a ClickableElement using optional before/after hooks.
     *
     * @param beforeActions optional hooks
     * @param element       target enum element
     * @param afterActions  optional hooks
     * @throws RuntimeException if click fails
     */
    public void clickOn(@Nullable List<ActionHandler> beforeActions,
                        Clickable element,
                        @Nullable List<ActionHandler> afterActions) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            By locator = LocatorResolverV1.getLocator(element); // role-based best available
            WebElement clickable = driver.findElement(locator);
            UIContext.setLastElement(clickable);
            clickOn(clickable);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        } catch (Exception e) {
            error.failed("Failed to click element: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click action failed for: " + element.getDisplayText(), e);
        }
    }

    /**
     * Core click with optional JS fallback and stale retry.
     *
     * @param beforeActions optional hooks before
     * @param element       target element
     * @param useJSExecutor if true, use JS click
     * @param afterActions  optional hooks after
     * @throws RuntimeException on failure
     */
    public void clickOn(@Nullable List<ActionHandler> beforeActions, WebElement element, @Nullable Boolean useJSExecutor, @Nullable List<ActionHandler> afterActions) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            if (useJSExecutor != null && useJSExecutor) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                debug.click("Clicked using JavaScriptExecutor.");
            } else {
                element.click();
                debug.click("Clicked using standard Selenium click().");
            }
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        } catch (Exception e) {
            throw new RuntimeException("Failed to click on the element. Details: " + e.getMessage(), e);
        }
    }

    /**
     * High-level click pipeline:
     * 1) Wait visible + clickable + highlight (for human debugging)
     * 2) Try Selenium click
     * 3) If that fails → JS click as a fallback
     * 4) If stale → try re-resolve via UIContext meta and click again
     *
     * @param element target element
     */
    public void clickOn(WebElement element) {
        UIContext.setLastElement(element);
        boolean clickedSuccessfully = tryClickWithHooks(
                element,
                List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                false,  // prefer standard click first
                List.of(After.DO_NOTHING),
                true    // skip retry if URL changes (navigation)
        );
        if (!clickedSuccessfully) {
            warn.fallback("Selenium click failed, retrying with JavaScript click...");
            clickedSuccessfully = tryClickWithHooks(
                    element,
                    List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                    true,   // JS fallback
                    List.of(After.DO_NOTHING),
                    true
            );
            if (!clickedSuccessfully) warn.error("JS click failed...");
        }
    }

    public void clickOn(By element){
        WebElement webElement = driver.findElement(element);
        clickOn(webElement);
    }

    /**
     * Tries a click with hooks and optional JS, with stale retry if page didn't navigate.
     *
     * @param element              target
     * @param beforeActions        hooks before
     * @param useJSExecutor        JS click?
     * @param afterActions         hooks after
     * @param skipRetryIfNavigated if URL changed, assume click succeeded
     * @return true if click path completed without fatal error
     */
    private boolean tryClickWithHooks(WebElement element,
                                      List<ActionHandler> beforeActions,
                                      boolean useJSExecutor,
                                      List<ActionHandler> afterActions,
                                      boolean skipRetryIfNavigated) {
        WebDriver activeDriver = DriverContext.getActiveDriver();
        String originalUrl = activeDriver.getCurrentUrl();
        try {
            // Only log intent once per click attempt
            if (beforeActions != null) {
                beforeActions.forEach(action -> {
                    if (action != Before.LOG_INTENT) {
                        action.execute(activeDriver);
                    }
                });
                // Execute LOG_INTENT only once
                if (beforeActions.contains(Before.LOG_INTENT)) {
                    Before.LOG_INTENT.execute(activeDriver);
                }
            }
            performClick(element, useJSExecutor, activeDriver);
            if (afterActions != null) afterActions.forEach(action -> action.execute(activeDriver));
            return true;
        } catch (StaleElementReferenceException staleEx) {
            debug.error("Stale element, retrying...");
            String currentUrl = activeDriver.getCurrentUrl();
            if (skipRetryIfNavigated && !originalUrl.equals(currentUrl)) {
                debug.log("Page navigation detected (URL changed). Skipping stale element retry.");
                return true; // treat as success — action likely took effect
            }
            try {
                By retryLocator = LocatorResolverV1.getLocator(
                        UIContext.getLastElementMeta().getPropertyFile(),
                        UIContext.getLastElementMeta().getKey(),
                        UIContext.getLastElementMeta().getArgs()
                );
                WebElement freshElement = activeDriver.findElement(retryLocator);
                UIContext.setLastElement(freshElement);
                // On retry, skip beforeActions to avoid duplicate logs
                performClick(freshElement, useJSExecutor, activeDriver);
                if (afterActions != null) afterActions.forEach(action -> action.execute(activeDriver));
                return true;
            } catch (Exception retryEx) {
                error.failed("Retry after stale element failed: " + retryEx.getMessage());
                return false;
            }
        } catch (Exception e) {
            error.failed("Click failed: " + e.getMessage());
            return false;
        }
    }

    private void performClick(WebElement element, boolean useJSExecutor) {
        performClick(element, useJSExecutor, DriverContext.getActiveDriver());
    }

    private void performClick(WebElement element, boolean useJSExecutor, WebDriver activeDriver) {
        if (useJSExecutor) {
            ((JavascriptExecutor) activeDriver).executeScript("arguments[0].click();", element);
            info.success("Clicked on: " + safeText(element));
            debug.success("Clicked using JavaScriptExecutor.");
        } else {
            String buttonName = safeText(element);
            element.click();
            info.success("Clicked on: " + (buttonName.isBlank() ? element.toString() : buttonName));
            debug.click("Clicked using Selenium click(). Element text: " + buttonName + " | Locator: " + element);
        }
    }

    private static String safeText(WebElement el) {
        try { return (el.getText() == null) ? "" : el.getText().trim(); } catch (Exception ignored) { return ""; }
    }

    // --- Shortcuts for clickOn with different signatures ---
    public void clickOn(Clickable element) { clickOn(null, element, null); }
    public void clickOn(List<ActionHandler> beforeActions, Clickable element) { clickOn(beforeActions, element, null); }
    public void clickOn(Clickable element, List<ActionHandler> afterActions) { clickOn(null, element, afterActions); }
    public void clickOn(ActionHandler before, Clickable element) { clickOn(before != null ? List.of(before) : null, element, null); }
    public void clickOn(Clickable element, ActionHandler after) { clickOn(null, element, after != null ? List.of(after) : null); }

    /**
     * Click an enum-based ClickableElement with both before and after hooks.
     *
     * @param beforeAction hook before click (nullable)
     * @param enumClass    enum class
     * @param label        label to resolve
     * @param afterAction  hook after click (nullable)
     * @param <T>          Clickable + Resolvable enum
     * @throws RuntimeException if click fails
     */
    public <T extends Enum<T> & Clickable> void clickOn(
            @Nullable ActionHandler beforeAction,
            Class<T> enumClass,
            String label,
            @Nullable ActionHandler afterAction
    ) {
        try {
            @SuppressWarnings({"rawtypes","unchecked"})
            T element = (T) EnumResolver.stringToEnum((Class) enumClass, label); // v1 ResolvableEnum only
            clickOn(of(beforeAction), element, of(afterAction));
        } catch (Exception e) {
            error.failed("Failed to click on element from " + enumClass.getSimpleName() + ": " + label + " " + e);
            throw new RuntimeException("Failed to click on element: " + label, e);
        }
    }
    public <T extends Enum<T> & Clickable> void clickOn(ActionHandler beforeAction, Class<T> enumClass, String label) { clickOn(beforeAction, enumClass, label, null); }
    public <T extends Enum<T> & Clickable> void clickOn(Class<T> enumClass, String label, ActionHandler afterAction) { clickOn(null, enumClass, label, afterAction); }

    /**
     * Clicks a target within a specific scope node (useful for complex tables/cards).
     *
     * @param scope   parent WebElement
     * @param element enum mapping to a child locator
     * @throws RuntimeException if click fails
     */
    public void clickOnWithin(WebElement scope, Clickable element) {
        try {
            By locator = LocatorResolverV1.getLocator(element); // role-based best available
            WebElement target = scope.findElement(locator);
            DOMUtils.scrollToElement(target);
            wait.until(ExpectedConditions.elementToBeClickable(target)).click();
            info.click(element.getDisplayText() + " (within scope)");
        } catch (Exception e) {
            error.failed("Failed to click within scope: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click in scope failed for: " + element.getDisplayText(), e);
        }
    }

    // ======================= SELECT FROM DROPDOWN (ALL OVERLOADS) =======================

    /**
     * Waits briefly for Angular Material CDK overlay to appear.
     *
     * <p>Why: mat-menu renders into an overlay pane detached from the main DOM tree.
     * If we try to find a menu item before the overlay exists, lookup fails.</p>
     *
     * @implNote Uses a lightweight presence check on <code>div.cdk-overlay-pane</code>
     *           via {@link WaitUtils#waitForCondition(WebDriver, org.openqa.selenium.support.ui.ExpectedCondition, By, Integer, Integer, Boolean, String)}.
     */
    private void waitForOverlayToAppear() {
        WebDriver activeDriver = DriverContext.getActiveDriver();
        By overlayPane = By.cssSelector("div.cdk-overlay-pane");
        WaitUtils.waitForCondition(
                activeDriver,
                drv -> {
                    assert drv != null;
                    return !drv.findElements(overlayPane).isEmpty();
                },
                overlayPane,
                5,   // seconds
                100, // polling ms
                true,
                "cdk overlay to appear"
        );
    }

    /**
     * Selects an option from a dropdown using low-level locators.
     * <ol>
     *   <li>Clicks trigger (opens dropdown)</li>
     *   <li>Waits for Angular CDK overlay (if applicable)</li>
     *   <li>Clicks option</li>
     * </ol>
     *
     * @param triggerLocator the locator for the dropdown trigger (button/icon to open the menu)
     * @param optionLocator  the locator for the option (menu item) to be clicked
     * @throws RuntimeException if trigger or option cannot be clicked or located
     */
    public void selectFromDropdown(By triggerLocator, By optionLocator) {
        try {
            // --- Trigger (open) ---
            WebElement trigger = driver.findElement(triggerLocator);
            debug.dropdown("[TRIGGER] " + triggerLocator);
            clickOn(trigger);

            // --- Overlay appear (Angular menus) ---
            waitForOverlayToAppear();

            // --- Option (select) ---
            debug.dropdown("[OPTION ] " + optionLocator);
            WebElement option = driver.findElement(optionLocator);
            clickOn(option);

            debug.dropdown("Selected from dropdown. Trigger: " + triggerLocator + ", Option: " + optionLocator);
        } catch (Exception e) {
            error.failed("Dropdown selection failed for option: " + optionLocator + " via trigger: " + triggerLocator + ". " + e.getMessage());
            throw new RuntimeException("Dropdown selection failed: " + optionLocator, e);
        }
    }

    /**
     * Select a single-value {@link Dropdown} (trigger+list come from the enum).
     *
     * @param option enum representing both the trigger and list item locators
     * @throws RuntimeException if selection fails
     */
    public void selectFromDropdown(Dropdown option) {
        try {
            By trigger = LocatorResolverV1.getLocator(option, ElementRole.TRIGGER);
            By listOption = LocatorResolverV1.getLocator(option, ElementRole.LIST, option.getArgs());
            selectFromDropdown(trigger, listOption);
            debug.dropdown("Selected → " + option.getDisplayText());
        } catch (Exception e) {
            debug.error("Dropdown selection failed: " + option.getDisplayText());
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText() + " ", e);
        }
    }

    /**
     * Advanced select for {@link Dropdown} with hooks and optional JS click.
     *
     * @param option                dropdown element representing trigger and list item
     * @param beforeDropdownActions actions before clicking trigger (may be null)
     * @param afterDropdownActions  actions after clicking trigger (may be null)
     * @param beforeOptionActions   actions before clicking option (may be null)
     * @param afterOptionActions    actions after clicking option (may be null)
     * @param useJSExecutor         if true, click via JavaScript as a fallback/primary
     * @throws RuntimeException if selection fails at any stage
     */
    public void selectFromDropdown(
            Dropdown option,
            @Nullable List<ActionHandler> beforeDropdownActions,
            @Nullable List<ActionHandler> afterDropdownActions,
            @Nullable List<ActionHandler> beforeOptionActions,
            @Nullable List<ActionHandler> afterOptionActions,
            boolean useJSExecutor
    ) {
        try {
            By trigger = LocatorResolverV1.getLocator(option, ElementRole.TRIGGER);
            By listOption = LocatorResolverV1.getLocator(option, ElementRole.LIST, option.getArgs());
            WebElement triggerElement = driver.findElement(trigger);

            // Open
            if (beforeDropdownActions != null) for (ActionHandler a : beforeDropdownActions) if (a != null) a.execute(driver);
            clickOn(null, triggerElement, useJSExecutor, null);
            if (afterDropdownActions != null) for (ActionHandler a : afterDropdownActions) if (a != null) a.execute(driver);

            // Wait overlay (if Angular)
            waitForOverlayToAppear();

            // Select
            if (beforeOptionActions != null) for (ActionHandler a : beforeOptionActions) if (a != null) a.execute(driver);
            WebElement optionElement = wait.until(ExpectedConditions.visibilityOfElementLocated(listOption));
            clickOn(null, optionElement, useJSExecutor, null);
            if (afterOptionActions != null) for (ActionHandler a : afterOptionActions) if (a != null) a.execute(driver);

            debug.dropdown("Selected → " + option.getDisplayText());
        } catch (Exception e) {
            debug.error("Dropdown selection failed: " + option.getDisplayText());
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText(), e);
        }
    }
    /** Overload for Dropdown: allow simple JS fallback, no hooks. */
    public void selectFromDropdown(Dropdown option, boolean useJSExecutor) {
        selectFromDropdown(option, null, null, null, null, useJSExecutor);
    }

    /**
     * Triggers (opens) a {@link MultipleIdenticalDropdowns} by optional index.
     * Important: Trigger uses the row index only (e.g., "(.../button)[%s]").
     *
     * @param dropdown      the three-dots (multi) dropdown enum
     * @param dropdownIndex 1-based index of the row whose three-dots to open; null uses enum default
     * @throws RuntimeException if trigger cannot be clicked
     */
    public void triggerDropdown(MultipleIdenticalDropdowns dropdown, @Nullable Integer dropdownIndex) {
        try {
            By triggerLocator = (dropdownIndex == null)
                    ? LocatorResolverV1.getLocator(dropdown, ElementRole.MULTI_TRIGGER)
                    : LocatorResolverV1.getLocator(dropdown, ElementRole.MULTI_TRIGGER, dropdownIndex);
            WebElement triggerElement = driver.findElement(triggerLocator);
            clickOn(triggerElement);
        } catch (Exception e) {
            error.failed("Failed to trigger dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    /**
     * Selects an option from a {@link MultipleIdenticalDropdowns} (three-dots) using optional index.
     *
     * Argument separation:
     * - Trigger: index only
     * - List item: label only (single %s is the human label)
     *
     * @param dropdownIndex the 1-based index of the Nth three-dots on the page (may be null)
     * @param option        the enum constant representing the specific menu option to click
     * @throws RuntimeException if selection fails
     */
    public void selectFromDropdown(@Nullable Integer dropdownIndex, MultipleIdenticalDropdowns option) {
        try {
            // 1) Trigger with INDEX ONLY
            triggerDropdown(option, dropdownIndex);

            // 2) Wait overlay appear (Angular menu)
            waitForOverlayToAppear();

            // 3) Resolve list with LABEL ONLY — ignore dropdownIndex here
            By optionLocator = LocatorResolverV1.getLocator(option.getExternalFileName(), option.getListLocator(), option.getArgs()); // list uses label only
            WebElement optionElement = driver.findElement(optionLocator);
            clickOn(optionElement);

            debug.dropdown("Selected option '" + option.getDisplayText()
                    + "' in dropdown " + (dropdownIndex == null ? "[default]" : "#" + dropdownIndex));
        } catch (Exception e) {
            debug.error("Dropdown selection failed for option '"
                    + option.getDisplayText() + "' in dropdown " + (dropdownIndex == null ? "[default]" : "#" + dropdownIndex));
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText(), e);
        }
    }

    /** Overload for MultipleDropdownElement with default index (null). */
    public void selectFromDropdown(MultipleIdenticalDropdowns option) { selectFromDropdown(null, option); }

    /**
     * Clicks the trigger element for any {@link Dropdown}.
     *
     * @param dropdown dropdown enum
     * @throws RuntimeException on failure
     */
    public void triggerDropdown(Dropdown dropdown) {
        try {
            By triggerLocator = LocatorResolverV1.getLocator(dropdown, ElementRole.TRIGGER);
            WebElement triggerElement = driver.findElement(triggerLocator);
            clickOn(triggerElement);
        } catch (Exception e) {
            error.failed("Failed to trigger dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    // ======================= GENERIC SEARCH AND RESULT HANDLING (RESTORED) =======================

    /**
     * Performs a simple search flow:
     *  - scroll to field
     *  - type term
     *  - click button OR press ENTER
     *
     * @param beforeActions optional hooks before
     * @param searchField   the input element (WebElement already located)
     * @param searchTerm    term to search
     * @param searchButton  optional button to click (nullable)
     * @param afterActions  optional hooks after
     */
    public void performSearch(
            @Nullable List<ActionHandler> beforeActions,
            WebElement searchField,
            String searchTerm,
            @Nullable WebElement searchButton,
            @Nullable List<ActionHandler> afterActions
    ) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            if (searchField == null) throw new IllegalArgumentException("searchField cannot be null.");
            DOMUtils.scrollToElement(searchField);
            UIContext.setLastElement(searchField);
            searchField.clear();
            searchField.sendKeys(searchTerm);
            if (searchButton != null) clickOn(searchButton); else searchField.sendKeys(Keys.ENTER);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        } catch (Exception e) {
            error.failed("[SEARCH] Failed to perform search for: '" + searchTerm + "' " + e);
            throw new RuntimeException("Search failed for term: " + searchTerm, e);
        }
    }

    /**
     * Performs a search using a Searchable field and returns the first result WebElement after scrolling.
     * Uses SEARCH_INPUT then SEARCH_RESULT roles.
     */
    public WebElement getSearchedElement(Searchable field, String searchTerm) {
        try {
            By inputLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_INPUT);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);
            By resultLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_RESULT, searchTerm);
            DOMUtils.scrollToElement(driver.findElement(resultLocator));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(resultLocator));
        } catch (Exception e) {
            debug.error("Search failed for term: " + searchTerm + " using field: " + field.getDisplayText());
            error.log(e.getMessage());
            throw new RuntimeException("Search failed for: " + searchTerm, e);
        }
    }

    /** Returns the display text for a searched/selected value after performing search. */
    public String getSearchResultText(Searchable field, String searchTerm) {
        WebElement result = getSearchedElement(field, searchTerm);
        return result != null ? result.getText().trim() : "";
    }

    /** Performs a search and returns all result WebElements (list may be empty). */
    public List<WebElement> searchAndGetResults(Searchable field, String searchTerm) {
        try {
            By inputLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_INPUT);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);
            By resultLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_RESULT, field.getArgs());
            return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(resultLocator));
        } catch (TimeoutException e) {
            warn.log("[SEARCH] No results found for term: '" + searchTerm + "' in " + field.getDisplayText());
            return List.of();
        } catch (Exception e) {
            error.failed("Failed to search and get results for: " + searchTerm + " " + e);
            throw new RuntimeException("searchAndGetResults failed", e);
        }
    }

    /** Specialized search for shared list context. */
    public List<WebElement> searchThisList(Searchable field, String searchTerm) {
        try {
            By inputLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_INPUT);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);
            By resultLocator = LocatorResolverV1.getLocator(field, ElementRole.SEARCH_RESULT, field.getArgs());
            return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(resultLocator));
        } catch (TimeoutException e) {
            warn.log("[SearchThisList] No results found for → '" + searchTerm + "'");
            return List.of();
        } catch (Exception e) {
            error.failed("[SearchThisList] Error for → " + field.getDisplayText() + ": " + e.getMessage());
            throw new RuntimeException("SearchThisList failed for: " + field.getDisplayText(), e);
        }
    }

    /** Generic search + optional click first result. */
    public WebElement searchFor(
            Searchable field,
            boolean clickResult,
            @Nullable List<ActionHandler> beforeSearch,
            @Nullable List<ActionHandler> afterSearch,
            @Nullable List<ActionHandler> beforeClick,
            @Nullable List<ActionHandler> afterClick,
            Object... resultArgs
    ) {
        try {
            String searchTerm = (resultArgs != null && resultArgs.length > 0 && resultArgs[0] != null)
                    ? resultArgs[0].toString() : "";
            if (beforeSearch != null) for (ActionHandler action : beforeSearch) if (action != null) action.execute(driver);
            WebElement resultElement = getSearchedElement(field, searchTerm);
            if (afterSearch != null) for (ActionHandler action : afterSearch) if (action != null) action.execute(driver);
            if (clickResult && resultElement != null) {
                if (beforeClick != null) for (ActionHandler action : beforeClick) if (action != null) action.execute(driver);
                clickOn(resultElement);
                if (afterClick != null) for (ActionHandler action : afterClick) if (action != null) action.execute(driver);
            }
            return resultElement;
        } catch (TimeoutException e) {
            warn.log("[searchFor] No results found for '" + Arrays.toString(resultArgs) + "' in " + field.getDisplayText());
            return null;
        } catch (Exception e) {
            error.failed("[searchFor] Failed for '" + Arrays.toString(resultArgs) + "' in " + field.getDisplayText() + ": " + e);
            throw new RuntimeException("searchFor failed for: " + Arrays.toString(resultArgs), e);
        }
    }
    public WebElement searchFor(Searchable field, String searchTerm) { return searchFor(field, true, null, null, null, null, searchTerm); }
    public WebElement searchForWithoutClick(Searchable field, String searchTerm) { return searchFor(field, false, null, null, null, null, searchTerm); }

    // ======================= END SEARCH SECTION =======================
}
