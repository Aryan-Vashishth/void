package core.interactions.hooks;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.utils.UIContext;
import core.utils.web.WaitUtils;

import java.time.Duration;

import static core.logging.CustomLogger.debug;

/**
 * Standard library of <b>before-action</b> {@link ActionHandler} constants.
 * <p>
 * Combine freely at call sites:
 * <pre>
 *   interactions.clickOn(List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.HIGHLIGHT_ELEMENT), element);
 * </pre>
 *
 * <p>All hooks are <b>engine-agnostic</b> — they delegate to {@link UIEngine} methods
 * rather than calling Selenium APIs directly. This ensures portability across engines.</p>
 *
 * <p>This class is a pure constants holder — never instantiate it.</p>
 *
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/Runner pipelines.
 */
public final class Before {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private Before() {}

    // ── No-ops / logging ──────────────────────────────────────────────────
    public static final ActionHandler DO_NOTHING  = engine -> {};
    public static final ActionHandler LOG_INTENT  = engine -> debug.log("[DEBUG] Performing UI action...");

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

    // ── Element-state waits (use UIContext.getLastLocatorDescriptor()) ─────
    /** Waits for the last resolved element to become clickable. */
    public static final ActionHandler WAIT_FOR_ELEMENT_CLICKABLE = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) {
            engine.waitForClickable(locator, DEFAULT_TIMEOUT);
        }
    };

    /** Waits for the last resolved element to be visible. */
    public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) {
            engine.waitForVisible(locator, DEFAULT_TIMEOUT);
        }
    };

    // ── Element manipulation ───────────────────────────────────────────────
    /** Clears the last resolved input element. Throws if no locator tracked. */
    public static final ActionHandler CLEAR_FIELD = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) {
            engine.clear(locator);
        } else {
            throw new IllegalStateException(
                    "[Before.CLEAR_FIELD] UIContext.getLastLocatorDescriptor() is null – resolve the element first.");
        }
    };

    /** Scrolls the last resolved element into view. */
    public static final ActionHandler SCROLL_TO_ELEMENT = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) engine.scrollTo(locator);
    };

    /** Highlights the last resolved element with a red border (debug aid). */
    public static final ActionHandler HIGHLIGHT_ELEMENT = engine -> {
        LocatorDescriptor locator = UIContext.getLastLocatorDescriptor();
        if (locator != null) engine.highlight(locator, "red");
    };
}

