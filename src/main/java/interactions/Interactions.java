package interactions;

import Elements.Interfaces.*;
import core.locators.LocatorResolver; // ← centralized AUTO resolver (JSON→.properties)
import core.utils.*;
import core.driver.DriverContext;
import com.beust.jcommander.internal.Nullable;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

import static core.logging.CustomLogger.*;

/**
 * Interactions (V2, centralized locators)
 * ---------------------------------------
 * Now resolves all locators through LocatorResolver which prefers JSON and
 * falls back to .properties, using BaseElement.getPrimaryLocator()/getSecondaryLocator().
 *
 * - For generic elements: PRIMARY = main locator, SECONDARY = auxiliary locator.
 * - For DropdownElement:   PRIMARY = trigger, SECONDARY = list-item.
 * - For SearchableElement: PRIMARY = input field, SECONDARY = result locator (format args supplied at call-site).
 * - For MultipleIdenticalDropdownElements: use getTriggerKey()/getListLocatorKey() with distinct args (index vs label).
 */
public class Interactions {

    // ====== STATIC HELPERS ======
    public static class Via {
        public static WebElement WebElement(WebElement element) { return element; }
        public static BaseElement BaseElement(BaseElement element) { return element; }
        public static CheckboxElement CheckboxElement(CheckboxElement element) { return element; }
        public static ClickableElement ClickableElement(ClickableElement element) { return element; }
        public static DropdownElement DropdownElement(DropdownElement element) { return element; }
        public static FileInputElement FileInputElement(FileInputElement element) { return element; }
        public static Form Form(Form element) { return element; }
        public static ListElement ListElement(ListElement element) { return element; }
        public static MultipleIdenticalDropdownElements MultipleIdenticalDropdownElements(MultipleIdenticalDropdownElements element) { return element; }
        public static ReadOnlyElement ReadOnlyElement(ReadOnlyElement element) { return element; }
        public static ResolvableEnum ResolvableEnum(ResolvableEnum element) { return element; }
        public static SearchableElementInput SearchableElement(SearchableElementInput element) { return element; }
        public static TableElement TableElement(TableElement element) { return element; }
        public static TextInputFieldElement TextFieldElement(TextInputFieldElement element) { return element; }
        public static ToolTipElement ToolTipElement(ToolTipElement element) { return element; }
        public static WritableTableElement WritableTableElement(WritableTableElement element) { return element; }
    }

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected final Logger log = Logger.getLogger(Interactions.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public Interactions(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        initialize(this.getClass());
        DriverContext.setDriver(driver);
    }

    @FunctionalInterface
    public interface ActionHandler { void execute(WebDriver driver); }

    public static List<ActionHandler> of(ActionHandler... handlers) {
        return (handlers == null || handlers.length == 0) ? null : List.of(handlers);
    }

    // ====== COMMON BEFORE/ACTION HOOKS ======
    public static class Before {
        public static final ActionHandler DO_NOTHING = driver -> {};
        public static final ActionHandler LOG_INTENT = driver -> debug.log("[DEBUG] Performing UI action...");
        public static final ActionHandler WAIT_FOR_ANGULAR_LOADER = driver -> WaitUtils.resolveAngularLoader();
        public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = driver -> WaitUtils.resolveLoader(WaitUtils.SPIN_SPINNER_LOADER);

        public static final ActionHandler WAIT_FOR_ELEMENT_CLICKABLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.elementToBeClickable(element));
            }
        };

        public static final ActionHandler CLEAR_FIELD = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                element.clear();
            } else {
                throw new IllegalStateException("[BEFORE] Cannot clear field – UIContext.getLastElement() is null.");
            }
        };

        public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOf(element));
            }
        };

        public static final ActionHandler SCROLL_TO_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                DOMUtils.scrollToElement(element);
            }
        };

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

        public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.visibilityOf(element));
            }
        };

        public static final ActionHandler HIGHLIGHT_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.border='6px solid green';", element);
            }
        };

        public static final ActionHandler SCROLL_TO_ELEMENT = driver -> {
            WebElement element = UIContext.getLastElement();
            if (element != null) {
                DOMUtils.scrollToElement(element);
            }
        };
    }

    // ======================= GENERIC TEXT RETRIEVAL =======================

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

    public String getText(@Nullable List<ActionHandler> beforeActions,
                          ReadOnlyElement element,
                          @Nullable List<ActionHandler> afterActions) {
        if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
        By locator = LocatorResolver.primary(element); // PRIMARY for ReadOnlyElement
        if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        return getTextByWebElement(locator);
    }

    public String getText(ReadOnlyElement element) { return getText(null, element, null); }

    public String getTextViaToolTip(@Nullable List<ActionHandler> beforeActions,
                                    ToolTipElement element,
                                    @Nullable List<ActionHandler> afterActions,
                                    boolean enableResolveTooltip) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            By locator = LocatorResolver.primary(element); // PRIMARY for main text element
            WebElement targetElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String unresolvedText = getTextByWebElement(locator);
            info.text("Retrieved from → " + element.getDisplayText() + " Unresolved Text: " + unresolvedText);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);

            if (enableResolveTooltip && !unresolvedText.isEmpty() && unresolvedText.endsWith(element.getEndsWith())) {
                debug.log("[DEBUG] Unresolved text ends with '" + element.getEndsWith() + "', resolving via hover.");
                return ToolTipsResolver.resolveTooltipByHover(element);
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

    public <T extends Enum<T> & ClickableElement & ResolvableEnum> void clickOn(Class<T> enumClass, String label) {
        try {
            T element = EnumResolver.stringToEnum(enumClass, label);
            clickOn(null, element, null);
        } catch (Exception e) {
            log.error("Failed to click on element from " + enumClass.getSimpleName() + ": " + label + " " + e);
            throw new RuntimeException("Failed to click on element: " + label, e);
        }
    }

    public void clickOn(@Nullable List<ActionHandler> beforeActions,
                        ClickableElement element,
                        @Nullable List<ActionHandler> afterActions) {
        try {
            if (beforeActions != null) for (ActionHandler action : beforeActions) if (action != null) action.execute(driver);
            By locator = LocatorResolver.primary(element); // PRIMARY for a generic click target
            WebElement clickable = driver.findElement(locator);
            UIContext.setLastElement(clickable);
            clickOn(clickable);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        } catch (Exception e) {
            error.failed("Failed to click element: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click action failed for: " + element.getDisplayText(), e);
        }
    }

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

    public void clickOn(WebElement element) {
        UIContext.setLastElement(element);
        boolean clickedSuccessfully = tryClickWithHooks(
                element,
                List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                false,
                List.of(After.DO_NOTHING),
                true
        );
        if (!clickedSuccessfully) {
            warn.fallback("Selenium click failed, retrying with JavaScript click...");
            clickedSuccessfully = tryClickWithHooks(
                    element,
                    List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                    true,
                    List.of(After.DO_NOTHING),
                    true
            );
            if (!clickedSuccessfully) warn.error("JS click failed...");
        }
    }

    private boolean tryClickWithHooks(WebElement element,
                                      List<ActionHandler> beforeActions,
                                      boolean useJSExecutor,
                                      List<ActionHandler> afterActions,
                                      boolean skipRetryIfNavigated) {
        String originalUrl = DriverContext.getDriver().getCurrentUrl();
        try {
            beforeActions.forEach(action -> action.execute(DriverContext.getDriver()));
            performClick(element, useJSExecutor);
            if (afterActions != null) afterActions.forEach(action -> action.execute(DriverContext.getDriver()));
            return true;
        } catch (StaleElementReferenceException staleEx) {
            debug.error("Stale element encountered during click.");
            // We do not re-resolve via file/key here anymore to keep resolution centralized.
            String currentUrl = DriverContext.getDriver().getCurrentUrl();
            if (skipRetryIfNavigated && !originalUrl.equals(currentUrl)) {
                debug.log("Page navigation detected (URL changed). Treating click as success.");
                return true;
            }
            return false;
        } catch (Exception e) {
            error.failed("Click failed: " + e.getMessage());
            return false;
        }
    }

    private void performClick(WebElement element, boolean useJSExecutor) {
        if (useJSExecutor) {
            ((JavascriptExecutor) DriverContext.getDriver()).executeScript("arguments[0].click();", element);
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

    // Shortcuts
    public void clickOn(ClickableElement element) { clickOn(null, element, null); }
    public void clickOn(List<ActionHandler> beforeActions, ClickableElement element) { clickOn(beforeActions, element, null); }
    public void clickOn(ClickableElement element, List<ActionHandler> afterActions) { clickOn(null, element, afterActions); }
    public void clickOn(ActionHandler before, ClickableElement element) { clickOn(before != null ? List.of(before) : null, element, null); }
    public void clickOn(ClickableElement element, ActionHandler after) { clickOn(null, element, after != null ? List.of(after) : null); }

    public <T extends Enum<T> & ClickableElement & ResolvableEnum> void clickOn(
            @Nullable ActionHandler beforeAction,
            Class<T> enumClass,
            String label,
            @Nullable ActionHandler afterAction
    ) {
        try {
            T element = EnumResolver.stringToEnum(enumClass, label);
            clickOn(of(beforeAction), element, of(afterAction));
        } catch (Exception e) {
            error.failed("Failed to click on element from " + enumClass.getSimpleName() + ": " + label + " " + e);
            throw new RuntimeException("Failed to click on element: " + label, e);
        }
    }
    public <T extends Enum<T> & ClickableElement & ResolvableEnum> void clickOn(ActionHandler beforeAction, Class<T> enumClass, String label) { clickOn(beforeAction, enumClass, label, null); }
    public <T extends Enum<T> & ClickableElement & ResolvableEnum> void clickOn(Class<T> enumClass, String label, ActionHandler afterAction) { clickOn(null, enumClass, label, afterAction); }

    public void clickOnWithin(WebElement scope, ClickableElement element) {
        try {
            By locator = LocatorResolver.primary(element);
            WebElement target = scope.findElement(locator);
            DOMUtils.scrollToElement(target);
            wait.until(ExpectedConditions.elementToBeClickable(target)).click();
            info.click(element.getDisplayText() + " (within scope)");
        } catch (Exception e) {
            error.failed("Failed to click within scope: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click in scope failed for: " + element.getDisplayText(), e);
        }
    }

    // ======================= SELECT FROM DROPDOWN =======================

    private void waitForOverlayToAppear() {
        By overlayPane = By.cssSelector("div.cdk-overlay-pane");
        WaitUtils.waitForCondition(
                DriverContext.getDriver(),
                drv -> drv.findElements(overlayPane).size() > 0,
                overlayPane,
                5,
                100,
                true,
                "cdk overlay to appear"
        );
    }

    public void selectFromDropdown(By triggerLocator, By optionLocator) {
        try {
            WebElement trigger = driver.findElement(triggerLocator);
            debug.dropdown("[TRIGGER] " + triggerLocator);
            clickOn(trigger);

            waitForOverlayToAppear();

            debug.dropdown("[OPTION ] " + optionLocator);
            WebElement option = driver.findElement(optionLocator);
            clickOn(option);

            debug.dropdown("Selected from dropdown. Trigger: " + triggerLocator + ", Option: " + optionLocator);
        } catch (Exception e) {
            error.failed("Dropdown selection failed for option: " + optionLocator + " via trigger: " + triggerLocator + ". " + e.getMessage());
            throw new RuntimeException("Dropdown selection failed: " + optionLocator, e);
        }
    }

    /** Single-value dropdown: PRIMARY=trigger, SECONDARY=list-item */
    public void selectFromDropdown(DropdownElement option) {
        try {
            By trigger = LocatorResolver.primary(option);
            By listOption = LocatorResolver.secondary(option);
            selectFromDropdown(trigger, listOption);
            debug.dropdown("Selected → " + option.getDisplayText());
        } catch (Exception e) {
            debug.error("Dropdown selection failed: " + option.getDisplayText());
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText() + " ", e);
        }
    }

    public void selectFromDropdown(
            DropdownElement option,
            @Nullable List<ActionHandler> beforeDropdownActions,
            @Nullable List<ActionHandler> afterDropdownActions,
            @Nullable List<ActionHandler> beforeOptionActions,
            @Nullable List<ActionHandler> afterOptionActions,
            boolean useJSExecutor
    ) {
        try {
            By trigger = LocatorResolver.primary(option);
            By listOption = LocatorResolver.secondary(option);
            WebElement triggerElement = driver.findElement(trigger);

            if (beforeDropdownActions != null) for (ActionHandler a : beforeDropdownActions) if (a != null) a.execute(driver);
            clickOn(null, triggerElement, useJSExecutor, null);
            if (afterDropdownActions != null) for (ActionHandler a : afterDropdownActions) if (a != null) a.execute(driver);

            waitForOverlayToAppear();

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
    public void selectFromDropdown(DropdownElement option, boolean useJSExecutor) {
        selectFromDropdown(option, null, null, null, null, useJSExecutor);
    }

    /** Multiple dropdown (three-dots): trigger index only, list label only. */
    public void triggerDropdown(MultipleIdenticalDropdownElements dropdown, @Nullable Integer dropdownIndex) {
        try {
            // Trigger uses its specific key and INDEX as arg
            By triggerLocator = LocatorResolver.key(dropdown, dropdown.getTriggerKey(),
                    (dropdownIndex == null) ? dropdown.getArgs() : new Object[]{ dropdownIndex });
            debug.dropdown("[TRIGGER args] index=" + (dropdownIndex == null ? "null" : dropdownIndex));
            debug.dropdown("[TRIGGER key ] " + dropdown.getTriggerKey());
            WebElement triggerElement = driver.findElement(triggerLocator);
            clickOn(triggerElement);
        } catch (Exception e) {
            error.failed("Failed to trigger dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    public void selectFromDropdown(@Nullable Integer dropdownIndex, MultipleIdenticalDropdownElements option) {
        try {
            triggerDropdown(option, dropdownIndex);
            waitForOverlayToAppear();

            // List uses its specific key and LABEL (from enum args)
            By optionLocator = LocatorResolver.key(option, option.getListLocatorKey(), option.getArgs());
            debug.dropdown("[LIST args] label=" + option.getDisplayText());
            debug.dropdown("[LIST key ] " + option.getListLocatorKey());
            debug.dropdown("[LIST loc ] " + optionLocator);

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

    public void selectFromDropdown(MultipleIdenticalDropdownElements option) { selectFromDropdown(null, option); }

    public void triggerDropdown(DropdownElement dropdown) {
        try {
            By triggerLocator = LocatorResolver.primary(dropdown);
            WebElement triggerElement = driver.findElement(triggerLocator);
            clickOn(triggerElement);
        } catch (Exception e) {
            error.failed("Failed to trigger dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    public void selectFromDropdownByContext(String dropdownLabel, String optionLabel) {
        switch (dropdownLabel.trim().toLowerCase()) {
            case "import records":
                AccountMappingElements.importRecordsDropdown selectedOption =
                        EnumResolver.stringToEnum("importRecordsDropdown", optionLabel);
                selectFromDropdown(selectedOption);
                debug.dropdown("'" + optionLabel + "' selected from 'Import Records' Dropdown");
                break;
            default:
                throw new UnsupportedOperationException("Dropdown not supported: " + dropdownLabel);
        }
    }

    // ======================= INPUT TEXT HANDLING =======================

    public void inputText(TextInputFieldElement element, String text) {
        try {
            By locator = LocatorResolver.primary(element); // PRIMARY for input field
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            DOMUtils.scrollToElement(inputField);
            UIContext.setLastElement(inputField);
            try {
                inputField.clear();
                inputField.sendKeys(text);
                debug.input("Entered " + text + " into " + element.getDisplayText());
                info.success("Entered " + text + " into " + element.getDisplayText());
            } catch (Exception e) {
                warn.text("sendKeys() failed. Retrying with JS → " + element.getDisplayText() + " " + e);
                ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", inputField, text);
                info.input("[JS] → " + text + " set in " + element.getDisplayText());
            }
        } catch (Exception e) {
            error.failed("Input failed for field: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Input failed for: " + element.getDisplayText(), e);
        }
    }

    // ======================= AUTOCOMPLETE SELECT HANDLING =======================

    public void selectAutocompleteOption(String optionText, @Nullable List<ActionHandler> beforeClickActions, @Nullable List<ActionHandler> afterClickActions) {
        try {
            By matOptionLocator = By.xpath("//mat-option[normalize-space() and not(@aria-disabled='true')]");
            wait.until(ExpectedConditions.visibilityOfElementLocated(matOptionLocator));
            List<WebElement> options = driver.findElements(matOptionLocator);

            WebElement match = null;
            for (WebElement option : options) {
                String optionTxt = option.getText().trim();
                if (optionTxt.toLowerCase().contains(optionText.toLowerCase())) {
                    match = option;
                    break;
                }
            }
            if (match == null) throw new RuntimeException("No mat-option matching '" + optionText + "' found.");
            UIContext.setLastElement(match);

            if (beforeClickActions != null) for (ActionHandler action : beforeClickActions) if (action != null) action.execute(driver);

            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", match);
            try {
                new org.openqa.selenium.interactions.Actions(driver)
                        .moveToElement(match)
                        .pause(Duration.ofMillis(200))
                        .click(match)
                        .perform();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", match);
            }

            if (afterClickActions != null) for (ActionHandler action : afterClickActions) if (action != null) action.execute(driver);
            info.log("[selectAutocompleteOption] Selected: " + optionText);
        } catch (Exception e) {
            error.failed("Failed to select autocomplete option '" + optionText + "': " + e.getMessage());
            throw new RuntimeException("Failed to select autocomplete option: " + optionText, e);
        }
    }

    // ======================= TABLE SEARCH HANDLING =======================

    public List<Map<String, String>> searchThisTable(
            @Nullable List<ActionHandler> beforeActions,
            @Nullable WebElement searchField,
            TableElement table,
            String searchColumn,
            String searchTerm,
            @Nullable WebElement searchButton,
            @Nullable Set<String> desiredColumns,
            @Nullable List<ActionHandler> afterActions
    ) {
        try {
            performSearch(beforeActions, searchField, searchTerm, searchButton, afterActions);
            Map<String, Object> columnData = new HashMap<>();
            columnData.put(searchColumn, searchTerm);
            List<Map<String, String>> rows = TableHandler.getRow(
                    table,
                    null,
                    columnData,
                    true
            );
            info.result("[SearchThisTable] Found " + rows.size() + " matching row(s) for filter → " +
                    searchColumn + " = '" + searchTerm + "'");
            return rows;
        } catch (Exception e) {
            error.failed("[SearchThisTable] Failed for table: " + table.getDisplayText() +
                    " using filter → " + searchColumn + " = '" + searchTerm + "': " + e.getMessage());
            throw new RuntimeException("SearchThisTable failed for: " + table.getDisplayText(), e);
        }
    }
    public List<Map<String, String>> searchThisTable(TableElement table, String searchColumn, String searchTerm) {
        return searchThisTable(null, null, table, searchColumn, searchTerm, null, null, null);
    }

    // ======================= GENERIC SEARCH AND RESULT HANDLING =======================

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
            if (searchButton != null) clickOn(searchButton);
            else searchField.sendKeys(Keys.ENTER);
            if (afterActions != null) for (ActionHandler action : afterActions) if (action != null) action.execute(driver);
        } catch (Exception e) {
            error.failed("[SEARCH] Failed to perform search for: '" + searchTerm + "' " + e);
            throw new RuntimeException("Search failed for term: " + searchTerm, e);
        }
    }

    public WebElement getSearchedElement(SearchableElementInput field, String searchTerm) {
        try {
            // PRIMARY = input field
            By inputLocator = LocatorResolver.primary(field);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);

            // SECONDARY key but with caller-supplied arg(s) (searchTerm)
            By resultLocator = LocatorResolver.key(field, field.getSecondaryLocator(), searchTerm);
            DOMUtils.scrollToElement(driver.findElement(resultLocator));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(resultLocator));
        } catch (Exception e) {
            debug.error("Search failed for term: " + searchTerm + " using field: " + field.getDisplayText());
            error.log(e.getMessage());
            throw new RuntimeException("Search failed for: " + searchTerm, e);
        }
    }

    public String getSearchResultText(SearchableElementInput field, String searchTerm) {
        WebElement result = getSearchedElement(field, searchTerm);
        return result != null ? result.getText().trim() : "";
    }

    public List<WebElement> searchAndGetResults(SearchableElementInput field, String searchTerm) {
        try {
            By inputLocator = LocatorResolver.primary(field);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);

            // Use supplier for multi-arg result patterns
            By resultLocator = LocatorResolver.key(field, field.getSecondaryLocator(), field.getResultArgsSupplier().get());
            return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(resultLocator));
        } catch (TimeoutException e) {
            warn.log("[SEARCH] No results found for term: '" + searchTerm + "' in " + field.getDisplayText());
            return List.of();
        } catch (Exception e) {
            error.failed("Failed to search and get results for: " + searchTerm + " " + e);
            throw new RuntimeException("SearchAndGetResults failed", e);
        }
    }

    public List<WebElement> searchThisList(SearchableElementInput field, String searchTerm) {
        try {
            By inputLocator = LocatorResolver.primary(field);
            WebElement inputField = wait.until(ExpectedConditions.visibilityOfElementLocated(inputLocator));
            UIContext.setLastElement(inputField);
            performSearch(null, inputField, searchTerm, null, null);
            By resultLocator = LocatorResolver.key(field, field.getSecondaryLocator(), field.getResultArgsSupplier().get());
            return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(resultLocator));
        } catch (TimeoutException e) {
            warn.log("[SearchThisList] No results found for → '" + searchTerm + "'");
            return List.of();
        } catch (Exception e) {
            error.failed("[SearchThisList] Error for → " + field.getDisplayText() + ": " + e.getMessage());
            throw new RuntimeException("SearchThisList failed for: " + field.getDisplayText(), e);
        }
    }

    public WebElement searchFor(
            SearchableElementInput field,
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
    public WebElement searchFor(SearchableElementInput field, String searchTerm) { return searchFor(field, true, null, null, null, null, searchTerm); }
    public WebElement searchForWithoutClick(SearchableElementInput field, String searchTerm) { return searchFor(field, false, null, null, null, null, searchTerm); }
    public WebElement searchFor(SearchableElementInput field, @Nullable List<ActionHandler> beforeSearch, @Nullable List<ActionHandler> afterSearch, String searchTerm) {
        return searchFor(field, true, beforeSearch, afterSearch, null, null, searchTerm);
    }
    public WebElement searchFor(SearchableElementInput field, String searchTerm, @Nullable List<ActionHandler> beforeClick, @Nullable List<ActionHandler> afterClick) {
        return searchFor(field, true, null, null, beforeClick, afterClick, searchTerm);
    }
    public WebElement searchFor(SearchableElementInput field, boolean clickResult, Object... resultArgs) {
        return searchFor(field, clickResult, null, null, null, null, resultArgs);
    }
    public WebElement searchFor(SearchableElementInput field, ActionHandler beforeSearch, String searchTerm) {
        return searchFor(field, true, of(beforeSearch), null, null, null, searchTerm);
    }
    public WebElement searchFor(SearchableElementInput field, String searchTerm, ActionHandler afterSearch) {
        return searchFor(field, true, null, of(afterSearch), null, null, searchTerm);
    }

    // ======================= CHECKBOX HANDLING =======================

    public void setCheckbox(
            CheckboxElement element,
            Boolean checkFlag,
            @Nullable List<ActionHandler> beforeActions,
            @Nullable List<ActionHandler> afterActions
    ) {
        try {
            By locator = LocatorResolver.primary(element); // PRIMARY for checkbox element
            WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            UIContext.setLastElement(checkbox);

            boolean currentlyChecked = element.isChecked(driver);
            boolean shouldClick = false;
            String actionMsg;

            if (checkFlag == null) {
                shouldClick = true;
                actionMsg = "Toggling (clicking) checkbox regardless of current state.";
            } else if (checkFlag && !currentlyChecked) {
                shouldClick = true;
                actionMsg = "Checking checkbox (was unchecked).";
            } else if (!checkFlag && currentlyChecked) {
                shouldClick = true;
                actionMsg = "Unchecking checkbox (was checked).";
            } else {
                actionMsg = "Checkbox already in desired state (" + (currentlyChecked ? "checked" : "unchecked") + "). No click needed.";
            }

            info.checkbox(element.getDisplayText() + ": " + actionMsg + " State: " + checkFlag);

            if (shouldClick) {
                if (beforeActions != null) for (ActionHandler before : beforeActions) if (before != null) before.execute(driver);
                clickOn(checkbox);

                if (checkFlag != null) {
                    boolean afterChecked = element.isChecked(driver);
                    if (checkFlag != afterChecked) {
                        warn.failed("Checkbox '" + element.getDisplayText() + "' did not reach desired state after click.");
                    } else {
                        info.success("Checkbox '" + element.getDisplayText() + "' is now " + (afterChecked ? "checked" : "unchecked") + ".");
                    }
                }
                if (afterActions != null) for (ActionHandler after : afterActions) if (after != null) after.execute(driver);
            }
        } catch (Exception e) {
            error.failed("Failed to set checkbox: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Checkbox action failed for: " + element.getDisplayText(), e);
        }
    }

    public void setCheckbox(CheckboxElement element, boolean check) { setCheckbox(element, check, null, null); }

    public boolean setCheckbox(
            WebElement checkbox,
            Boolean checkFlag,
            @Nullable List<ActionHandler> beforeActions,
            @Nullable List<ActionHandler> afterActions
    ) {
        try {
            if (beforeActions != null) for (ActionHandler before : beforeActions) if (before != null) before.execute(driver);
            String ariaChecked = checkbox.getAttribute("aria-checked");
            boolean currentlyChecked = ariaChecked != null ? "true".equalsIgnoreCase(ariaChecked) : checkbox.isSelected();
            boolean shouldClick = false;
            String actionMsg;

            if (checkFlag == null) {
                shouldClick = true;
                actionMsg = "Toggling (clicking) checkbox regardless of current state.";
            } else if (checkFlag && !currentlyChecked) {
                shouldClick = true;
                actionMsg = "Checking checkbox (was unchecked).";
            } else if (!checkFlag && currentlyChecked) {
                shouldClick = true;
                actionMsg = "Unchecking checkbox (was checked).";
            } else {
                actionMsg = "Checkbox already in desired state (" + (currentlyChecked ? "checked" : "unchecked") + "). No click needed.";
            }

            info.checkbox("setCheckboxByWebElement: " + actionMsg);

            if (shouldClick) {
                clickOn(checkbox);
                if (checkFlag != null) {
                    ariaChecked = checkbox.getAttribute("aria-checked");
                    boolean afterChecked = ariaChecked != null ? "true".equalsIgnoreCase(ariaChecked) : checkbox.isSelected();
                    if (checkFlag != afterChecked) {
                        warn.failed("Checkbox did not reach desired state after click.");
                    } else {
                        info.success("Checkbox is now " + (afterChecked ? "checked" : "unchecked") + ".");
                    }
                    currentlyChecked = afterChecked;
                }
            }
            if (afterActions != null) for (ActionHandler after : afterActions) if (after != null) after.execute(driver);
            return currentlyChecked;
        } catch (Exception e) {
            error.failed("Failed to set checkbox by WebElement: " + e);
            throw new RuntimeException("Checkbox action by WebElement failed.", e);
        }
    }

    public boolean setCheckbox(WebElement checkbox, Boolean checkFlag) { return setCheckbox(checkbox, checkFlag, null, null); }
    public boolean clickCheckbox(WebElement checkbox) { return setCheckbox(checkbox, null, null, null); }
    public void clickOn(CheckboxElement element) { setCheckbox(element, null, null, null); }
}
