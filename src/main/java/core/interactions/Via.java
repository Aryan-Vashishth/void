package core.interactions;

import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolvers;

import core.driver.DriverContext;
import elements.api.Element;
import elements.api.capability.*;
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
 * // Cast an Element descriptor to a specific capability interface
 * Clickable  btn   = Via.clickable(MyPageElements.SAVE_BUTTON);
 * Typeable   tf    = Via.typeable(MyPageElements.EMAIL_FIELD);
 * Selectable ddl   = Via.selectable(MyPageElements.STATUS_DROPDOWN);
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
 * <p>All cast helpers throw a descriptive {@link ClassCastException}
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
        if (element instanceof Clickable c) return c;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Clickable. "
                + "Ensure the enum implements the Clickable interface.");
    }

    /**
     * Casts {@code element} to {@link Typeable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Typeable}
     * @throws ClassCastException if the element does not implement {@link Typeable}
     */
    public static Typeable typeable(Element element) {
        if (element instanceof Typeable t) return t;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Typeable. "
                + "Ensure the enum implements the Typeable interface.");
    }

    /**
     * Casts {@code element} to {@link Selectable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Selectable}
     * @throws ClassCastException if the element does not implement {@link Selectable}
     */
    public static Selectable selectable(Element element) {
        if (element instanceof Selectable s) return s;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Selectable. "
                + "Ensure the enum implements the Selectable interface.");
    }

    /**
     * Casts {@code element} to {@link ReadOnly}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link ReadOnly}
     * @throws ClassCastException if the element does not implement {@link ReadOnly}
     */
    public static ReadOnly readOnly(Element element) {
        if (element instanceof ReadOnly r) return r;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement ReadOnly. "
                + "Ensure the enum implements the ReadOnly interface.");
    }

    /**
     * Casts {@code element} to {@link Searchable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Searchable}
     * @throws ClassCastException if the element does not implement {@link Searchable}
     */
    public static Searchable searchable(Element element) {
        if (element instanceof Searchable s) return s;
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
        if (element instanceof SearchableDropdown sd) return sd;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement SearchableDropdown.");
    }

    /**
     * Casts {@code element} to {@link MultiSelectable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link MultiSelectable}
     * @throws ClassCastException if the element does not implement {@link MultiSelectable}
     */
    public static MultiSelectable multiSelectable(Element element) {
        if (element instanceof MultiSelectable ms) return ms;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement MultiSelectable.");
    }

    /**
     * Casts {@code element} to {@link Checkable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Checkable}
     * @throws ClassCastException if the element does not implement {@link Checkable}
     */
    public static Checkable checkable(Element element) {
        if (element instanceof Checkable c) return c;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Checkable.");
    }

    /**
     * Casts {@code element} to {@link Hoverable}.
     *
     * @param element any {@link Element} enum descriptor
     * @return the same instance as {@link Hoverable}
     * @throws ClassCastException if the element does not implement {@link Hoverable}
     */
    public static Hoverable hoverable(Element element) {
        if (element instanceof Hoverable h) return h;
        throw new ClassCastException(element.getClass().getSimpleName()
                + " does not implement Hoverable.");
    }

    // ─────────────────────────────────────────────────────────────────────
    // TYPE-CHECK PREDICATES
    // ─────────────────────────────────────────────────────────────────────

    /** @return true if {@code element} implements {@link Clickable} */
    public static boolean isClickable(Element element) { return element instanceof Clickable; }

    /** @return true if {@code element} implements {@link Typeable} */
    public static boolean isTypeable(Element element) { return element instanceof Typeable; }

    /** @return true if {@code element} implements {@link Selectable} */
    public static boolean isSelectable(Element element) { return element instanceof Selectable; }

    /** @return true if {@code element} implements {@link ReadOnly} */
    public static boolean isReadOnly(Element element) { return element instanceof ReadOnly; }

    /** @return true if {@code element} implements {@link Searchable} */
    public static boolean isSearchable(Element element) { return element instanceof Searchable; }

    /** @return true if {@code element} implements {@link Checkable} */
    public static boolean isCheckable(Element element) { return element instanceof Checkable; }

    // ─────────────────────────────────────────────────────────────────────
    // LOCATOR DESCRIPTOR RESOLVERS (Engine-Agnostic — preferred)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Resolves the best-available {@link core.engine.LocatorDescriptor} for any {@link Element}.
     * <p>This is the preferred replacement for {@link #locator(Element)} which returns a Selenium {@code By}.</p>
     *
     * @param element any {@link Element} enum descriptor
     * @return resolved {@link core.engine.LocatorDescriptor}
     */
    public static core.engine.LocatorDescriptor descriptor(Element element) {
        return LocatorResolvers.strict().resolveDescriptor(element);
    }

    /**
     * Resolves a {@link core.engine.LocatorDescriptor} for a specific {@link ElementRole}.
     *
     * @param element      any {@link Element} enum descriptor
     * @param role         the specific role to resolve
     * @param overrideArgs optional arguments to override the enum's own args
     * @return resolved {@link core.engine.LocatorDescriptor}
     */
    public static core.engine.LocatorDescriptor descriptor(Element element, ElementRole role, Object... overrideArgs) {
        return LocatorResolvers.strict().resolveDescriptor(element, role, overrideArgs);
    }

    /**
     * Resolves a {@link core.engine.LocatorDescriptor} from an explicit file name + key + args.
     *
     * @param fileName properties or JSON file
     * @param key      locator key
     * @param args     optional format arguments
     * @return resolved {@link core.engine.LocatorDescriptor}
     */
    public static core.engine.LocatorDescriptor descriptor(String fileName, String key, Object... args) {
        return LocatorResolvers.strict().resolveDescriptor(fileName, key, args);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LOCATOR RESOLVERS (Selenium By — deprecated, use descriptor() instead)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * @deprecated Use {@link #descriptor(Element)} instead. Returns engine-agnostic descriptor
     *             rather than Selenium-specific {@code By}.
     */
    @Deprecated(forRemoval = true)
    public static By locator(Element element) {
        return LocatorResolvers.strict().resolve(element);
    }

    /**
     * @deprecated Use {@link #descriptor(Element, ElementRole, Object...)} instead.
     */
    @Deprecated(forRemoval = true)
    public static By locator(Element element, ElementRole role, Object... overrideArgs) {
        return LocatorResolvers.strict().resolve(element, role, overrideArgs);
    }

    /**
     * @deprecated Use {@link #descriptor(String, String, Object...)} instead.
     */
    @Deprecated(forRemoval = true)
    public static By locator(String fileName, String key, Object... args) {
        return LocatorResolvers.strict().resolve(LocatorRequest.of(fileName, key, args));
    }

    // ─────────────────────────────────────────────────────────────────────
    // WEB ELEMENT FINDERS (Selenium-specific — deprecated)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * @deprecated WebElement is engine-specific. Use descriptor-based resolution
     *             and pass descriptors to UIEngine methods.
     */
    @Deprecated(forRemoval = true)
    public static WebElement webElement(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** @deprecated Use engine-based methods instead. */
    @Deprecated(forRemoval = true)
    public static WebElement webElement(WebDriver driver, Element element) {
        return webElement(driver, LocatorResolvers.strict().resolve(element));
    }

    /** @deprecated Use engine-based methods instead. */
    @Deprecated(forRemoval = true)
    public static WebElement webElement(Element element) {
        return webElement(DriverContext.getActiveDriver(), element);
    }

    /** @deprecated Use engine-based methods instead. */
    @Deprecated(forRemoval = true)
    public static WebElement webElement(By locator) {
        return webElement(DriverContext.getActiveDriver(), locator);
    }

    /** @deprecated Use engine-based methods instead. */
    @Deprecated(forRemoval = true)
    public static WebElement webElement(WebDriver driver, Element element, ElementRole role, Object... overrideArgs) {
        return webElement(driver, LocatorResolvers.strict().resolve(element, role, overrideArgs));
    }
}
