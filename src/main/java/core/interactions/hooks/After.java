package core.interactions.hooks;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.utils.UIContext;

import java.time.Duration;

/**
 * Standard library of <b>after-action</b> {@link ActionHandler} constants.
 * <p>
 * Use alongside {@link Before} to compose full hook chains:
 * <pre>
 *   interactions.clickOn(
 *       List.of(Before.WAIT_FOR_ANGULAR_LOADER),
 *       element,
 *       List.of(After.WAIT_FOR_ELEMENT_VISIBLE));
 * </pre>
 *
 * <p>All hooks are <b>engine-agnostic</b> — they delegate to {@link UIEngine} methods
 * rather than calling Selenium APIs directly.</p>
 *
 * This class is a pure constants holder — never instantiate it.
 */
public final class After {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private After() {}

    // ── No-ops ────────────────────────────────────────────────────────────
    public static final ActionHandler DO_NOTHING = engine -> {};

    // ── Loader waits ──────────────────────────────────────────────────────
    public static final ActionHandler WAIT_FOR_ANGULAR_LOADER = engine -> {
        LocatorDescriptor loader = LocatorDescriptor.of("app-loader", core.engine.LocatorStrategy.CSS);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = engine -> {
        LocatorDescriptor loader = LocatorDescriptor.of(
                "//span[contains(@class, 'spin spinner')]", core.engine.LocatorStrategy.XPATH);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    // ── Element-state waits ────────────────────────────────────────────────
    /** Waits for the last resolved element to be visible after the action. */
    public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) {
            engine.waitForVisible(locator, DEFAULT_TIMEOUT);
        }
    };

    // ── Element manipulation ───────────────────────────────────────────────
    /** Highlights the last resolved element with a green border (success indicator). */
    public static final ActionHandler HIGHLIGHT_ELEMENT = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) engine.highlight(locator, "green");
    };

    /** Scrolls the last resolved element into view after the action. */
    public static final ActionHandler SCROLL_TO_ELEMENT = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) engine.scrollTo(locator);
    };
}

