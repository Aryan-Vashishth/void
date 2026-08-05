package core.runtime;

import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;
import domain.automation.web.vocabulary.role.ElementRole;

import java.time.Duration;
import java.util.List;

/**
 * Session service for element-level queries.
 *
 * <p>Obtained via {@link VOID#elements()}. Provides capability-typed query methods
 * so callers never supply an {@link ElementRole} explicitly -- the role is encoded
 * in the capability interface ({@code Clickable} → {@code TRIGGER},
 * {@code Typeable} → {@code INPUT}, {@code ReadOnly} → {@code TEXT}).</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   boolean visible = app.elements().isVisible(ProductsPage.Header.Labels.CART_BADGE);
 *   String  href    = app.elements().attribute(ProductsPage.ProductItem.Buttons.ITEM_TITLE_LINK, "href");
 *   int     count   = app.elements().count(ProductsPage.Header.Labels.CART_BADGE);
 *   List&lt;String&gt; names = app.elements().allTexts(ProductsPage.ProductItem.Labels.ITEM_NAME);
 * </pre>
 */
public final class Elements {

    private final UIEngine engine;

    Elements(UIEngine engine) {
        this.engine = engine;
    }

    // ── Visibility ─────────────────────────────────────────────────────────

    /** Returns {@code true} if the {@link ReadOnly} element is currently visible. */
    public boolean isVisible(ReadOnly element) {
        return engine.isVisible(engine.resolve(element, ElementRole.TEXT));
    }

    /** Returns {@code true} if the {@link Clickable} element is currently visible. */
    public boolean isVisible(Clickable element) {
        return engine.isVisible(engine.resolve(element, ElementRole.TRIGGER));
    }

    /**
     * Returns {@code true} if the parameterized {@link Clickable} element is visible.
     *
     * @param element the clickable element whose locator template requires args
     * @param args    format arguments substituted into the locator template
     */
    public boolean isVisible(Clickable element, Object... args) {
        return engine.isVisible(engine.resolve(element, ElementRole.TRIGGER, args));
    }

    /**
     * Waits up to 10 seconds for the {@link Clickable} element to become visible.
     *
     * <p>Use after triggering an animation or transition before asserting visibility.</p>
     */
    public void waitForVisible(Clickable element) {
        engine.waitForVisible(engine.resolve(element, ElementRole.TRIGGER), Duration.ofSeconds(10));
    }

    /**
     * Waits up to 10 seconds for the {@link Clickable} element to become hidden or absent.
     *
     * <p>Use after triggering a close/dismiss animation before asserting invisibility.</p>
     */
    public void waitForHidden(Clickable element) {
        engine.waitForAbsence(engine.resolve(element, ElementRole.TRIGGER), Duration.ofSeconds(10));
    }

    // ── Attributes ─────────────────────────────────────────────────────────

    /** Returns the named attribute value for a {@link Clickable} element (TRIGGER role). */
    public String attribute(Clickable element, String name) {
        return engine.getAttribute(engine.resolve(element, ElementRole.TRIGGER), name);
    }

    /** Returns the named attribute value for a {@link Typeable} element (INPUT role). */
    public String attribute(Typeable element, String name) {
        return engine.getAttribute(engine.resolve(element, ElementRole.INPUT), name);
    }

    // ── Count / bulk text ──────────────────────────────────────────────────

    /** Returns the number of elements matching the {@link ReadOnly} locator. */
    public int count(ReadOnly element) {
        return engine.getElementCount(engine.resolve(element, ElementRole.TEXT));
    }

    /** Returns the visible text of every element matching the {@link ReadOnly} locator. */
    public List<String> allTexts(ReadOnly element) {
        return engine.getAllTexts(engine.resolve(element, ElementRole.TEXT));
    }
}
