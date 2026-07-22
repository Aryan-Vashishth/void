package core.interactions;

import core.engine.UIEngine;
import core.engine.LocatorDescriptor;
import core.bridge.selenium.SeleniumLocatorBridge;
import core.engine.selenium.SeleniumEngine;
import elements.meta.ElementRole;
import elements.api.Element;
import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Hoverable;
import elements.api.capability.Selectable;
import elements.api.capability.Checkable;
import elements.api.capability.Searchable;
import elements.api.capability.MultiSelectable;
import core.interactions.hooks.ActionHandler;
import core.interactions.hooks.Before;
import core.interactions.hooks.After;
import core.resolvers.locator.api.LocatorResolver;
import core.resolvers.locator.api.LocatorResolvers;
import core.utils.*;
import org.openqa.selenium.*;
import java.time.Duration;
import java.util.*;
import com.beust.jcommander.internal.Nullable;
import static core.logging.CustomLogger.*;

/**
 * Interactions — Legacy Orchestrator (Frozen)
 * ————————————————————————————————————————————
 * <b>This class is frozen.</b> No new features should be added here.
 * For new code, use the Action-based pipeline: {@code Element → Action → Flow → FlowExecutor → UIEngine}.
 *
 * <p>This class is preserved for backward compatibility with existing step definitions
 * and page objects. All execution is delegated to {@link UIEngine}.</p>
 *
 * @apiNote <b>Stable (frozen).</b> This API will not receive new features.
 * Existing behavior will remain unchanged, but this API will not evolve further.
 * Prefer Action/Flow/FlowExecutor pipeline for new development.
 *
 * @deprecated Prefer capability methods like {@code Clickable.click()}, {@code Typeable.type()}, etc. which return
 *             {@link core.actions.Action} objects composed via {@link core.flow.Flow}.
 * @see core.actions.Action
 * @see core.flow.Flow
 * @see core.executor.FlowExecutor
 * @see UIEngine
 */
@Deprecated(since = "0.1", forRemoval = false)
public class Interactions {

    private static final Duration OVERLAY_TIMEOUT = Duration.ofSeconds(5);

    /** Strict locator resolver. */
    private static final LocatorResolver LOCATORS = LocatorResolvers.strict();

    /** The active execution engine â€” single authority for all browser operations. */
    protected final UIEngine engine;

    /**
     * Standard constructor using the engine abstraction.
     *
     * @param engine The UIEngine instance to use for all actions.
     */
    public Interactions(UIEngine engine) {
        this.engine = engine;
    }

    /**
     * Legacy constructor, wraps WebDriver in a SeleniumEngine.
     *
     * @param driver The WebDriver instance to use for all actions.
     * @deprecated Use {@link #Interactions(UIEngine)} instead. This constructor
     *             exists for backward compatibility and will be removed.
     */
    @Deprecated(forRemoval = true)
    public Interactions(WebDriver driver) {
        this(new SeleniumEngine(driver));
    }


    /** Helper for collecting hooks in a single call. */
    public static List<ActionHandler> of(ActionHandler... handlers) {
        return (handlers == null || handlers.length == 0) ? null : List.of(handlers);
    }

    // ====== RESOLUTION HELPERS (to LocatorDescriptor, not By) ======

    /**
     * Resolves a LocatorDescriptor for an Element and tracks it in UIContext.
     */
    private LocatorDescriptor resolveAndTrack(Element element) {
        LocatorDescriptor descriptor = LOCATORS.resolveDescriptor(element);
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        return descriptor;
    }

    /** Overload: resolve by role with args and track descriptor. */
    private LocatorDescriptor resolveAndTrack(Element element, ElementRole role, Object... args) {
        LocatorDescriptor descriptor = LOCATORS.resolveDescriptor(element, role, args);
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        return descriptor;
    }

    /** Overload: resolve from file/key/args and track descriptor. */
    private LocatorDescriptor resolveAndTrack(String fileName, String key, Object... args) {
        LocatorDescriptor descriptor = LOCATORS.resolveDescriptor(fileName, key, args);
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        return descriptor;
    }

    /**
     * Execute hooks safely.
     * <p>Legacy path: passes {@code null} as the descriptor. Element-dependent hooks
     * will log a warning and return early.  For descriptor-aware hook execution,
     * use {@link core.actions.HookedAction} instead.</p>
     */
    private void executeHooks(@Nullable List<ActionHandler> hooks) {
        if (hooks != null) {
            for (ActionHandler action : hooks) {
                // ⚠️ Legacy: descriptor is null — hooks that need it will log a warning.
                if (action != null) action.execute(engine, null);
            }
        }
    }

    // ======================= GENERIC TEXT RETRIEVAL =======================

    /**
     * Reads the trimmed text from a locator descriptor via the engine.
     *
     * @param descriptor target locator descriptor
     * @return element text (trimmed)
     */
    public String getTextByDescriptor(LocatorDescriptor descriptor) {
        try {
            engine.scrollTo(descriptor);
            String text = engine.getText(descriptor);
            debug.text("Retrieved from â†’ " + descriptor + ": " + text);
            return text;
        } catch (Exception e) {
            error.log("Failed to get text from element: " + descriptor + " " + e);
            throw new RuntimeException("Failed to get text from: " + descriptor, e);
        }
    }

    /**
     * Reads the trimmed text from a By locator (legacy bridge).
     *
     * @param locator target locator
     * @return element text (trimmed)
     * @deprecated Use descriptor-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public String getTextByWebElement(By locator) {
        LocatorDescriptor descriptor = SeleniumLocatorBridge.fromBy(locator);
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        return getTextByDescriptor(descriptor);
    }

    /**
     * Gets text from a read-only element with optional before/after actions.
     */
    public String getText(@Nullable List<ActionHandler> beforeActions,
                          ReadOnly element,
                          @Nullable List<ActionHandler> afterActions) {
        executeHooks(beforeActions);
        LocatorDescriptor descriptor = resolveAndTrack(element);
        executeHooks(afterActions);
        return getTextByDescriptor(descriptor);
    }

    /** Simple overload: no hooks. */
    public String getText(ReadOnly element) { return getText(null, element, null); }

    /**
     * Reads visible text, with attribute fallback if truncated (common for ellipsized cells).
     */
    public String getTextViaToolTip(@Nullable List<ActionHandler> beforeActions,
                                    Hoverable element,
                                    @Nullable List<ActionHandler> afterActions,
                                    boolean enableResolveTooltip) {
        try {
            executeHooks(beforeActions);
            LocatorDescriptor descriptor = resolveAndTrack(element);

            String unresolvedText = engine.getText(descriptor);
            info.text("Retrieved from â†’ " + element.getDisplayText() + " Unresolved Text: " + unresolvedText);
            executeHooks(afterActions);

            // If truncated and resolver is enabled, attempt to skip (disabled scenario)
            if (enableResolveTooltip && !unresolvedText.isEmpty() && unresolvedText.endsWith(element.getEndsWith())) {
                debug.log("[DEBUG] Unresolved text ends with '" + element.getEndsWith() + "'. Skipping hover resolution (disabled).");
                return unresolvedText;
            }

            // If empty or truncated, try attribute fallbacks via engine
            if (unresolvedText.isEmpty() || unresolvedText.endsWith(element.getEndsWith())) {
                String fallback = engine.getTextWithAttributeFallback(descriptor, element.getEndsWith(), "title", "aria-label");
                if (fallback != null && !fallback.trim().isEmpty() && !fallback.equals(unresolvedText)) {
                    info.fallback("Fallback to attribute for â†’ " + element.getDisplayText() + ": " + fallback);
                    return fallback;
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
     * Clicks a Clickable using optional before/after hooks.
     * All execution delegated to {@link UIEngine#clickWithRetry(LocatorDescriptor)}.
     */
    public void clickOn(@Nullable List<ActionHandler> beforeActions,
                        Clickable element,
                        @Nullable List<ActionHandler> afterActions) {
        try {
            executeHooks(beforeActions);
            LocatorDescriptor descriptor = resolveAndTrack(element);
            engine.click(descriptor);
            executeHooks(afterActions);
        } catch (Exception e) {
            error.failed("Failed to click element: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click action failed for: " + element.getDisplayText(), e);
        }
    }

    /**
     * Clicks a LocatorDescriptor via the engine's robust click pipeline.
     */
    public void clickOn(LocatorDescriptor descriptor) {
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        engine.click(descriptor);
    }

    /**
     * Clicks a By locator (legacy bridge).
     *
     * @deprecated Use descriptor-based or element-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public void clickOn(By locator) {
        LocatorDescriptor descriptor = SeleniumLocatorBridge.fromBy(locator);
        clickOn(descriptor);
    }

    /**
     * Core click with optional JS fallback (legacy bridge).
     *
     * @deprecated Use engine-based click methods instead.
     */
    @Deprecated(forRemoval = true)
    public void clickOn(@Nullable List<ActionHandler> beforeActions, WebElement element,
                        @Nullable Boolean useJSExecutor, @Nullable List<ActionHandler> afterActions) {
        try {
            executeHooks(beforeActions);
            if (useJSExecutor != null && useJSExecutor) {
                ((JavascriptExecutor) engine.getNativeDriver()).executeScript("arguments[0].click();", element);
                debug.click("Clicked using JavaScriptExecutor (legacy).");
            } else {
                element.click();
                debug.click("Clicked using Selenium click() (legacy).");
            }
            executeHooks(afterActions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to click on the element (legacy). Details: " + e.getMessage(), e);
        }
    }

    /**
     * Clicks a raw WebElement (legacy bridge).
     *
     * @deprecated Prefer descriptor-based or element-based methods.
     */
    @Deprecated(forRemoval = true)
    public void clickOn(WebElement element) {
        element.click();
    }

    // --- Shortcuts for clickOn with different signatures ---
    public void clickOn(Clickable element) { clickOn(null, element, null); }
    public void clickOn(List<ActionHandler> beforeActions, Clickable element) { clickOn(beforeActions, element, null); }
    public void clickOn(Clickable element, List<ActionHandler> afterActions) { clickOn(null, element, afterActions); }
    public void clickOn(ActionHandler before, Clickable element) { clickOn(before != null ? List.of(before) : null, element, null); }
    public void clickOn(Clickable element, ActionHandler after) { clickOn(null, element, after != null ? List.of(after) : null); }

    /**
     * Click an enum-based Clickable with both before and after hooks.
     */
    public <T extends Enum<T> & Clickable> void clickOn(
            @Nullable ActionHandler beforeAction,
            Class<T> enumClass,
            String label,
            @Nullable ActionHandler afterAction
    ) {
        try {
            @SuppressWarnings({"rawtypes","unchecked"})
            T element = (T) EnumResolver.stringToEnum((Class) enumClass, label);
            clickOn(of(beforeAction), element, of(afterAction));
        } catch (Exception e) {
            error.failed("Failed to click on element from " + enumClass.getSimpleName() + ": " + label + " " + e);
            throw new RuntimeException("Failed to click on element: " + label, e);
        }
    }
    public <T extends Enum<T> & Clickable> void clickOn(ActionHandler beforeAction, Class<T> enumClass, String label) { clickOn(beforeAction, enumClass, label, null); }
    public <T extends Enum<T> & Clickable> void clickOn(Class<T> enumClass, String label, ActionHandler afterAction) { clickOn(null, enumClass, label, afterAction); }

    /**
     * Clicks a target within a specific scope (parentâ†’child).
     * Uses {@link LocatorDescriptor#withParent(LocatorDescriptor)} for composable scoping.
     *
     * @param scopeDescriptor parent element descriptor
     * @param element         enum mapping to a child locator
     */
    public void clickOnWithin(LocatorDescriptor scopeDescriptor, Clickable element) {
        try {
            LocatorDescriptor childDescriptor = resolveAndTrack(element);
            LocatorDescriptor scoped = childDescriptor.withParent(scopeDescriptor);
            engine.scrollTo(scoped);
            engine.click(scoped);
            info.click(element.getDisplayText() + " (within scope)");
        } catch (Exception e) {
            error.failed("Failed to click within scope: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click in scope failed for: " + element.getDisplayText(), e);
        }
    }

    /**
     * Clicks a target within a WebElement scope (legacy bridge).
     *
     * @deprecated Use {@link #clickOnWithin(LocatorDescriptor, Clickable)} instead.
     */
    @Deprecated(forRemoval = true)
    public void clickOnWithin(WebElement scope, Clickable element) {
        try {
            By locator = LOCATORS.resolve(element);
            WebElement target = scope.findElement(locator);
            target.click();
            info.click(element.getDisplayText() + " (within scope, legacy)");
        } catch (Exception e) {
            error.failed("Failed to click within scope: " + element.getDisplayText() + " " + e);
            throw new RuntimeException("Click in scope failed for: " + element.getDisplayText(), e);
        }
    }

    // ======================= SELECT FROM Selectable (ALL OVERLOADS) =======================

    /**
     * Selects an option from a Selectable using descriptor-based locators.
     * All execution goes through the engine.
     */
    public void selectFromDropdown(LocatorDescriptor triggerDescriptor, LocatorDescriptor optionDescriptor) {
        try {
            debug.dropdown("[TRIGGER] " + triggerDescriptor);
            engine.click(triggerDescriptor);
            engine.waitForOverlay(OVERLAY_TIMEOUT);
            debug.dropdown("[OPTION ] " + optionDescriptor);
            engine.click(optionDescriptor);
            debug.dropdown("Selected from Dropdown. Trigger: " + triggerDescriptor + ", Option: " + optionDescriptor);
        } catch (Exception e) {
            error.failed("Dropdown selection failed. " + e.getMessage());
            throw new RuntimeException("Dropdown selection failed: " + optionDescriptor, e);
        }
    }

    /**
     * Selects from dropdown using By locators (legacy bridge).
     *
     * @deprecated Use descriptor-based overload instead.
     */
    @Deprecated(forRemoval = true)
    public void selectFromDropdown(By triggerLocator, By optionLocator) {
        selectFromDropdown(SeleniumLocatorBridge.fromBy(triggerLocator), SeleniumLocatorBridge.fromBy(optionLocator));
    }

    /**
     * Select a single-value {@link Selectable} (trigger+list come from the enum).
     */
    public void selectFromDropdown(Selectable option) {
        try {
            LocatorDescriptor trigger = LOCATORS.resolveDescriptor(option, ElementRole.TRIGGER);
            LocatorDescriptor listOption = LOCATORS.resolveDescriptor(option, ElementRole.LIST, option.getArgs());
            selectFromDropdown(trigger, listOption);
            debug.dropdown("Selected â†’ " + option.getDisplayText());
        } catch (Exception e) {
            debug.error("Dropdown selection failed: " + option.getDisplayText());
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText() + " ", e);
        }
    }

    /**
     * Advanced select for {@link Selectable} with hooks and optional JS click.
     */
    public void selectFromDropdown(
            Selectable option,
            @Nullable List<ActionHandler> beforeDropdownActions,
            @Nullable List<ActionHandler> afterDropdownActions,
            @Nullable List<ActionHandler> beforeOptionActions,
            @Nullable List<ActionHandler> afterOptionActions,
            boolean useJSExecutor
    ) {
        try {
            LocatorDescriptor trigger = LOCATORS.resolveDescriptor(option, ElementRole.TRIGGER);
            LocatorDescriptor listOption = LOCATORS.resolveDescriptor(option, ElementRole.LIST, option.getArgs());

            // Open
            executeHooks(beforeDropdownActions);
            if (useJSExecutor) {
                engine.click(trigger);
            } else {
                engine.click(trigger);
            }
            executeHooks(afterDropdownActions);

            // Wait overlay
            engine.waitForOverlay(OVERLAY_TIMEOUT);

            // Select
            executeHooks(beforeOptionActions);
            engine.waitForVisible(listOption, Duration.ofSeconds(10));
            if (useJSExecutor) {
                engine.click(listOption);
            } else {
                engine.click(listOption);
            }
            executeHooks(afterOptionActions);

            debug.dropdown("Selected â†’ " + option.getDisplayText());
        } catch (Exception e) {
            debug.error("Dropdown selection failed: " + option.getDisplayText());
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText(), e);
        }
    }

    /** Overload: allow simple JS fallback, no hooks. */
    public void selectFromDropdown(Selectable option, boolean useJSExecutor) {
        selectFromDropdown(option, null, null, null, null, useJSExecutor);
    }

    /**
     * Triggers (opens) a {@link MultiSelectable} by optional index.
     */
    public void triggerDropdown(MultiSelectable dropdown, @Nullable Integer dropdownIndex) {
        try {
            LocatorDescriptor triggerDescriptor = (dropdownIndex == null)
                    ? LOCATORS.resolveDescriptor(dropdown, ElementRole.MULTI_TRIGGER)
                    : LOCATORS.resolveDescriptor(dropdown, ElementRole.MULTI_TRIGGER, dropdownIndex);
            engine.click(triggerDescriptor);
        } catch (Exception e) {
            error.failed("Failed to trigger Dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger Dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    /**
     * Selects an option from a {@link MultiSelectable} using optional index.
     */
    public void selectFromDropdown(@Nullable Integer dropdownIndex, MultiSelectable option) {
        try {
            triggerDropdown(option, dropdownIndex);
            engine.waitForOverlay(OVERLAY_TIMEOUT);
            LocatorDescriptor optionDescriptor = LOCATORS.resolveDescriptor(
                    option.getExternalFileName(), option.getListLocator(), option.getArgs());
            engine.click(optionDescriptor);
            debug.dropdown("Selected option '" + option.getDisplayText()
                    + "' in Dropdown " + (dropdownIndex == null ? "[default]" : "#" + dropdownIndex));
        } catch (Exception e) {
            debug.error("Dropdown selection failed for option '"
                    + option.getDisplayText() + "' in Dropdown " + (dropdownIndex == null ? "[default]" : "#" + dropdownIndex));
            throw new RuntimeException("Dropdown selection failed: " + option.getDisplayText(), e);
        }
    }

    /** Overload for MultiSelectable with default index (null). */
    public void selectFromDropdown(MultiSelectable option) { selectFromDropdown(null, option); }

    /**
     * Clicks the trigger element for any {@link Selectable}.
     */
    public void triggerDropdown(Selectable dropdown) {
        try {
            LocatorDescriptor triggerDescriptor = LOCATORS.resolveDescriptor(dropdown, ElementRole.TRIGGER);
            engine.click(triggerDescriptor);
        } catch (Exception e) {
            error.failed("Failed to trigger Dropdown for: " + dropdown.getDisplayText() + " - " + e.getMessage());
            throw new RuntimeException("Failed to trigger Dropdown for: " + dropdown.getDisplayText(), e);
        }
    }

    // ======================= SEARCH HANDLING =======================

    /**
     * Performs a search using a Searchable field and clicks the result.
     * All execution through the engine.
     */
    public void searchAndSelect(Searchable field, String searchTerm) {
        try {
            LocatorDescriptor inputDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_INPUT);
            engine.waitForVisible(inputDescriptor, Duration.ofSeconds(10));
            engine.type(inputDescriptor, searchTerm);

            LocatorDescriptor resultDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_RESULT, searchTerm);
            engine.waitForVisible(resultDescriptor, Duration.ofSeconds(10));
            engine.scrollTo(resultDescriptor);
            engine.click(resultDescriptor);
        } catch (Exception e) {
            debug.error("Search failed for term: " + searchTerm + " using field: " + field.getDisplayText());
            error.log(e.getMessage());
            throw new RuntimeException("Search failed for: " + searchTerm, e);
        }
    }

    /**
     * Performs a search and returns the result text (without clicking).
     */
    public String getSearchResultText(Searchable field, String searchTerm) {
        try {
            LocatorDescriptor inputDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_INPUT);
            engine.waitForVisible(inputDescriptor, Duration.ofSeconds(10));
            engine.type(inputDescriptor, searchTerm);

            LocatorDescriptor resultDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_RESULT, searchTerm);
            engine.waitForVisible(resultDescriptor, Duration.ofSeconds(10));
            engine.scrollTo(resultDescriptor);
            return engine.getText(resultDescriptor);
        } catch (Exception e) {
            debug.error("Search failed for term: " + searchTerm + " using field: " + field.getDisplayText());
            return "";
        }
    }

    /**
     * Performs a search, optionally clicking the result.
     */
    public void searchFor(
            Searchable field,
            String searchTerm,
            boolean clickResult,
            @Nullable List<ActionHandler> beforeSearch,
            @Nullable List<ActionHandler> afterSearch,
            @Nullable List<ActionHandler> beforeClick,
            @Nullable List<ActionHandler> afterClick
    ) {
        try {
            executeHooks(beforeSearch);
            LocatorDescriptor inputDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_INPUT);
            engine.waitForVisible(inputDescriptor, Duration.ofSeconds(10));
            engine.type(inputDescriptor, searchTerm);

            LocatorDescriptor resultDescriptor = LOCATORS.resolveDescriptor(field, ElementRole.SEARCH_RESULT, searchTerm);
            engine.waitForVisible(resultDescriptor, Duration.ofSeconds(10));
            engine.scrollTo(resultDescriptor);
            executeHooks(afterSearch);

            if (clickResult) {
                executeHooks(beforeClick);
                engine.click(resultDescriptor);
                executeHooks(afterClick);
            }
        } catch (Exception e) {
            warn.log("[searchFor] Failed for '" + searchTerm + "' in " + field.getDisplayText() + ": " + e);
            throw new RuntimeException("searchFor failed for: " + searchTerm, e);
        }
    }

    /** Simple search + click. */
    public void searchFor(Searchable field, String searchTerm) {
        searchFor(field, searchTerm, true, null, null, null, null);
    }

    /** Search without clicking result. */
    public void searchForWithoutClick(Searchable field, String searchTerm) {
        searchFor(field, searchTerm, false, null, null, null, null);
    }

    /**
     * Legacy search with raw WebElement (deprecated).
     *
     * @deprecated Use {@link #searchFor(Searchable, String)} instead.
     */
    @Deprecated(forRemoval = true)
    public void performSearch(
            @Nullable List<ActionHandler> beforeActions,
            WebElement searchField,
            String searchTerm,
            @Nullable WebElement searchButton,
            @Nullable List<ActionHandler> afterActions
    ) {
        try {
            executeHooks(beforeActions);
            if (searchField == null) throw new IllegalArgumentException("searchField cannot be null.");
            searchField.clear();
            searchField.sendKeys(searchTerm);
            if (searchButton != null) searchButton.click(); else searchField.sendKeys(Keys.ENTER);
            executeHooks(afterActions);
        } catch (Exception e) {
            error.failed("[SEARCH] Failed to perform search for: '" + searchTerm + "' " + e);
            throw new RuntimeException("Search failed for term: " + searchTerm, e);
        }
    }

    // ======================= TEXT INPUT HANDLING =======================

    /**
     * Clears and types text into a By locator via the engine.
     *
     * @deprecated Use descriptor-based or element-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public void typeInto(By locator, String text) {
        LocatorDescriptor descriptor = SeleniumLocatorBridge.fromBy(locator);
        UIContext.setLastLocatorDescriptor(descriptor);
        UIContext.setLastActionTarget(descriptor);
        engine.type(descriptor, text);
    }

    /**
     * Clears and types text into a raw WebElement (legacy bridge).
     *
     * @deprecated Use descriptor-based or element-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public void typeInto(WebElement element, String text) {
        element.clear();
        element.sendKeys(text);
        debug.text("Typed into WebElement (legacy): '" + text + "'");
    }

    /**
     * Full-pipeline type into a {@link Typeable} with optional before/after hooks.
     * All execution delegated to the engine.
     */
    public void typeInto(@Nullable List<ActionHandler> beforeActions,
                         Typeable field,
                         String text,
                         @Nullable List<ActionHandler> afterActions) {
        try {
            executeHooks(beforeActions);
            LocatorDescriptor descriptor = resolveAndTrack(field);
            engine.type(descriptor, text);
            info.text("Typed into '" + field.getDisplayText() + "': " + text);
            executeHooks(afterActions);
        } catch (Exception e) {
            error.failed("Failed to type into field '" + field.getDisplayText() + "': " + e.getMessage());
            throw new RuntimeException("typeInto(Typeable) failed for: " + field.getDisplayText(), e);
        }
    }

    /** Simple overload â€” no hooks. */
    public void typeInto(Typeable field, String text) { typeInto(null, field, text, null); }

    /** Overload â€” before hook only. */
    public void typeInto(ActionHandler before, Typeable field, String text) {
        typeInto(before != null ? List.of(before) : null, field, text, null);
    }

    /** Overload â€” after hook only. */
    public void typeInto(Typeable field, String text, ActionHandler after) {
        typeInto(null, field, text, after != null ? List.of(after) : null);
    }

    /**
     * Appends text to an existing field value without clearing.
     */
    public void appendTo(Typeable field, String text) {
        try {
            LocatorDescriptor descriptor = resolveAndTrack(field);
            engine.appendType(descriptor, text);
            debug.text("Appended to '" + field.getDisplayText() + "': " + text);
        } catch (Exception e) {
            error.failed("appendTo failed for '" + field.getDisplayText() + "': " + e.getMessage());
            throw new RuntimeException("appendTo failed for: " + field.getDisplayText(), e);
        }
    }

    /**
     * Appends to a raw WebElement (legacy bridge).
     *
     * @deprecated Use descriptor-based or element-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public void appendTo(WebElement element, String text) {
        element.sendKeys(text);
        debug.text("Appended to WebElement (legacy): '" + text + "'");
    }

    /**
     * Clears the content of a {@link Typeable}.
     */
    public void clearField(Typeable field) {
        try {
            LocatorDescriptor descriptor = resolveAndTrack(field);
            engine.clear(descriptor);
            debug.text("Cleared field: '" + field.getDisplayText() + "'");
        } catch (Exception e) {
            error.failed("clearField failed for '" + field.getDisplayText() + "': " + e.getMessage());
            throw new RuntimeException("clearField failed for: " + field.getDisplayText(), e);
        }
    }

    /**
     * Clears a raw WebElement (legacy bridge).
     *
     * @deprecated Use descriptor-based or element-based methods instead.
     */
    @Deprecated(forRemoval = true)
    public void clearField(WebElement element) {
        element.clear();
        debug.text("Cleared WebElement (legacy).");
    }

    /**
     * Types into a field and presses a key immediately after (e.g., ENTER, TAB).
     * Engine handles the type+key atomically.
     */
    public void typeIntoAndPress(Typeable field, String text, String key) {
        try {
            LocatorDescriptor descriptor = resolveAndTrack(field);
            engine.type(descriptor, text); engine.sendKey(descriptor, key);
            debug.text("Typed '" + text + "' and pressed " + key + " in '" + field.getDisplayText() + "'");
        } catch (Exception e) {
            error.failed("typeIntoAndPress failed for '" + field.getDisplayText() + "': " + e.getMessage());
            throw new RuntimeException("typeIntoAndPress failed for: " + field.getDisplayText(), e);
        }
    }

    /**
     * Types into a field and presses a Selenium Keys chord (legacy bridge).
     *
     * @deprecated Use {@link #typeIntoAndPress(Typeable, String, String)} with key name instead.
     */
    @Deprecated(forRemoval = true)
    public void typeIntoAndPress(Typeable field, String text, Keys key) {
        typeIntoAndPress(field, text, key.name());
    }

    /**
     * Sends a key to the element resolved from a Typeable.
     */
    public void pressKey(Typeable field, String key) {
        try {
            LocatorDescriptor descriptor = resolveAndTrack(field);
            engine.sendKey(descriptor, key);
            debug.text("Pressed key " + key + " on '" + field.getDisplayText() + "'");
        } catch (Exception e) {
            error.failed("pressKey failed: " + e.getMessage());
            throw new RuntimeException("pressKey failed", e);
        }
    }

    /**
     * Presses a key on a raw WebElement (legacy bridge).
     *
     * @deprecated Use descriptor-based pressKey instead.
     */
    @Deprecated(forRemoval = true)
    public void pressKey(WebElement element, Keys key) {
        element.sendKeys(key);
        debug.text("Pressed key " + key + " on element (legacy).");
    }

    // ======================= CHECKBOX HANDLING =======================

    /**
     * Sets a {@link Checkable} to the desired state (checked / unchecked).
     * Reads state via the engine, clicks only if toggle needed.
     */
    public void setCheckbox(Checkable checkbox, boolean desiredState) {
        try {
            LocatorDescriptor descriptor = resolveAndTrack(checkbox);
            boolean current = engine.getCheckboxState(descriptor);
            if (current != desiredState) {
                engine.click(descriptor);
                info.success("Checkbox toggled â†’ " + checkbox.getDisplayText() + " to " + desiredState);
            } else {
                info.validation("Checkbox already in desired state â†’ " + checkbox.getDisplayText() + " = " + desiredState);
            }
        } catch (Exception e) {
            error.failed("Failed to set Checkbox: " + checkbox.getDisplayText() + " " + e.getMessage());
            throw new RuntimeException("Failed to set Checkbox state for: " + checkbox.getDisplayText(), e);
        }
    }

    // ======================= VISIBILITY HELPERS =======================

    /**
     * Checks whether any element matching the descriptor is currently displayed.
     */
    public boolean isAnyDisplayed(LocatorDescriptor descriptor, Duration timeout) {
        try {
            engine.waitForVisible(descriptor, timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Convenience overload with 5s timeout. */
    public boolean isAnyDisplayed(LocatorDescriptor descriptor) {
        return isAnyDisplayed(descriptor, Duration.ofSeconds(5));
    }

    /**
     * Checks visibility using a By locator (legacy bridge).
     *
     * @deprecated Use descriptor-based overload instead.
     */
    @Deprecated(forRemoval = true)
    public boolean isAnyDisplayed(By locator, Duration timeout, Duration poll) {
        LocatorDescriptor descriptor = SeleniumLocatorBridge.fromBy(locator);
        return isAnyDisplayed(descriptor, timeout);
    }

    /**
     * @deprecated Use descriptor-based overload instead.
     */
    @Deprecated(forRemoval = true)
    public boolean isAnyDisplayed(By locator) {
        return isAnyDisplayed(SeleniumLocatorBridge.fromBy(locator), Duration.ofSeconds(5));
    }
}

