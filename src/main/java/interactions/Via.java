package interactions;

import core.driver.DriverContext;
import core.resolvers.locator.LocatorResolverV1;
import elements.api.*;
import elements.meta.ElementRole;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Via — Static Element Interface Helper
 * ─────────────────────────────────────
 * Provides a fluent, discoverable set of factory/cast helpers so that callers can
 * quickly resolve the correct interface type or locate a {@link WebElement} from
 * different input sources.
 *
 * <h3>Usage patterns</h3>
 * <pre>
 * // Cast an Element descriptor to a specific role interface
 * Clickable  btn   = Via.clickable(MyPageElements.SAVE_BUTTON);
 * TextInputField tf = Via.textInput(MyPageElements.EMAIL_FIELD);
 * Dropdown   ddl   = Via.dropdown(MyPageElements.STATUS_DROPDOWN);
 *
 * // Resolve a By locator from any Element descriptor or explicit file/key
 * By locator = Via.locator(MyPageElements.SAVE_BUTTON);
 * By roleLocator = Via.locator(MyPageElements.STATUS_DROPDOWN, ElementRole.LIST, "Active");
 * By rawLocator  = Via.locator("common-elements.json", "searchInput");
 *
 * // Find a live WebElement from a locator or descriptor
 * WebElement el = Via.webElement(driver, locator);
 * WebElement el = Via.webElement(driver, MyPageElements.EMAIL_FIELD);
 * </pre>
 *
 * <h3>Cast-safety</h3>
 * <p>All {@code as*()} / named helpers throw a descriptive {@link ClassCastException}
 * if the supplied element does not implement the requested interface.</p>
 */
public final class Via {

    private Via() { /* static utility */ }

    // ─────────────────────────────────────────────────────────────────────
    // INTERFACE CAST HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Casts {@code element} to {@link Clickable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Clickable}
     * @throws ClassCastException if the element does not implement {@link Clickable}
     */
    public static Clickable clickable(Element element) {
        if (element instanceof Clickable) return (Clickable) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Clickable. "
                + "Ensure the enum implements the Clickable interface.");
    }

    /**
     * Casts {@code element} to {@link TextInputField}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link TextInputField}
     * @throws ClassCastException if the element does not implement {@link TextInputField}
     */
    public static TextInputField textInput(Element element) {
        if (element instanceof TextInputField) return (TextInputField) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement TextInputField. "
                + "Ensure the enum implements the TextInputField interface.");
    }

    /**
     * Casts {@code element} to {@link Dropdown}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Dropdown}
     * @throws ClassCastException if the element does not implement {@link Dropdown}
     */
    public static Dropdown dropdown(Element element) {
        if (element instanceof Dropdown) return (Dropdown) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Dropdown. "
                + "Ensure the enum implements the Dropdown interface.");
    }

    /**
     * Casts {@code element} to {@link ReadOnlyElement}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link ReadOnlyElement}
     * @throws ClassCastException if the element does not implement {@link ReadOnlyElement}
     */
    public static ReadOnlyElement readOnly(Element element) {
        if (element instanceof ReadOnlyElement) return (ReadOnlyElement) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement ReadOnlyElement. "
                + "Ensure the enum implements the ReadOnlyElement interface.");
    }

    /**
     * Casts {@code element} to {@link Searchable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Searchable}
     * @throws ClassCastException if the element does not implement {@link Searchable}
     */
    public static Searchable searchable(Element element) {
        if (element instanceof Searchable) return (Searchable) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Searchable. "
                + "Ensure the enum implements the Searchable interface.");
    }

    /**
     * Casts {@code element} to {@link SearchableDropdown}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link SearchableDropdown}
     * @throws ClassCastException if the element does not implement {@link SearchableDropdown}
     */
    public static SearchableDropdown searchableDropdown(Element element) {
        if (element instanceof SearchableDropdown) return (SearchableDropdown) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement SearchableDropdown.");
    }

    /**
     * Casts {@code element} to {@link MultipleIdenticalDropdowns}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link MultipleIdenticalDropdowns}
     * @throws ClassCastException if the element does not implement {@link MultipleIdenticalDropdowns}
     */
    public static MultipleIdenticalDropdowns multiDropdown(Element element) {
        if (element instanceof MultipleIdenticalDropdowns) return (MultipleIdenticalDropdowns) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement MultipleIdenticalDropdowns.");
    }

    /**
     * Casts {@code element} to {@link Checkbox}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Checkbox}
     * @throws ClassCastException if the element does not implement {@link Checkbox}
     */
    public static Checkbox checkbox(Element element) {
        if (element instanceof Checkbox) return (Checkbox) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Checkbox.");
    }

    /**
     * Casts {@code element} to {@link ToolTipElement}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link ToolTipElement}
     * @throws ClassCastException if the element does not implement {@link ToolTipElement}
     */
    public static ToolTipElement tooltip(Element element) {
        if (element instanceof ToolTipElement) return (ToolTipElement) element;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement ToolTipElement.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // TYPE-CHECK PREDICATES
    // ─────────────────────────────────────────────────────────────────────

    /** @return true if {@code element} implements {@link Clickable} */
    public static boolean isClickable(Element element) { return element instanceof Clickable; }

    /** @return true if {@code element} implements {@link TextInputField} */
    public static boolean isTextInput(Element element) { return element instanceof TextInputField; }

    /** @return true if {@code element} implements {@link Dropdown} */
    public static boolean isDropdown(Element element) { return element instanceof Dropdown; }

    /** @return true if {@code element} implements {@link ReadOnlyElement} */
    public static boolean isReadOnly(Element element) { return element instanceof ReadOnlyElement; }

    /** @return true if {@code element} implements {@link Searchable} */
    public static boolean isSearchable(Element element) { return element instanceof Searchable; }

    /** @return true if {@code element} implements {@link Checkbox} */
    public static boolean isCheckbox(Element element) { return element instanceof Checkbox; }

    // ─────────────────────────────────────────────────────────────────────
    // LOCATOR RESOLVERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Resolves the best-available {@link By} locator for any {@link Element} descriptor
     * using {@link LocatorResolverV1#getLocator(Element)}.
     *
     * @param element any {@link Element} enum descriptor
     * @return resolved {@link By}
     */
    public static By locator(Element element) {
        return LocatorResolverV1.getLocator(element);
    }

    /**
     * Resolves the {@link By} locator for a specific {@link ElementRole} on an {@link Element}.
     *
     * @param element      any {@link Element} enum descriptor
     * @param role         the specific role to resolve
     * @param overrideArgs optional arguments to override the enum's own args
     * @return resolved {@link By}
     */
    public static By locator(Element element, ElementRole role, Object... overrideArgs) {
        return LocatorResolverV1.getLocator(element, role, overrideArgs);
    }

    /**
     * Resolves a {@link By} locator from an explicit file name + property key + args.
     * Delegates directly to {@link LocatorResolverV1#getLocator(String, String, Object...)}.
     *
     * @param fileName properties or JSON file (e.g. "common-elements.json") — may be null for hardcoded
     * @param key      locator key / template string
     * @param args     optional format arguments
     * @return resolved {@link By}
     */
    public static By locator(String fileName, String key, Object... args) {
        return LocatorResolverV1.getLocator(fileName, key, args);
    }

    // ─────────────────────────────────────────────────────────────────────
    // WEB ELEMENT FINDERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Finds and returns a visible {@link WebElement} using a pre-resolved {@link By} locator.
     * Uses a 10-second default wait from the active {@link DriverContext}.
     *
     * @param driver  active {@link WebDriver}
     * @param locator {@link By} locator
     * @return visible {@link WebElement}
     * @throws org.openqa.selenium.TimeoutException if element is not visible within timeout
     */
    public static WebElement webElement(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Finds and returns a visible {@link WebElement} resolved from an {@link Element} descriptor.
     *
     * @param driver  active {@link WebDriver}
     * @param element any {@link Element} enum descriptor
     * @return visible {@link WebElement}
     */
    public static WebElement webElement(WebDriver driver, Element element) {
        return webElement(driver, locator(element));
    }

    /**
     * Finds and returns a visible {@link WebElement} using the active driver from
     * {@link DriverContext} — no need to pass a driver reference.
     *
     * @param element any {@link Element} enum descriptor
     * @return visible {@link WebElement}
     */
    public static WebElement webElement(Element element) {
        return webElement(DriverContext.getActiveDriver(), element);
    }

    /**
     * Finds and returns a visible {@link WebElement} using the active driver from
     * {@link DriverContext} — no need to pass a driver reference.
     *
     * @param locator {@link By} locator
     * @return visible {@link WebElement}
     */
    public static WebElement webElement(By locator) {
        return webElement(DriverContext.getActiveDriver(), locator);
    }

    /**
     * Finds and returns a visible {@link WebElement} for a specific {@link ElementRole}
     * on the given {@link Element} descriptor.
     *
     * @param driver       active {@link WebDriver}
     * @param element      any {@link Element} enum descriptor
     * @param role         the role whose locator to use
     * @param overrideArgs optional override arguments
     * @return visible {@link WebElement}
     */
    public static WebElement webElement(WebDriver driver, Element element, ElementRole role, Object... overrideArgs) {
        return webElement(driver, locator(element, role, overrideArgs));
    }
}

