package core.interactions.hooks;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;

import java.time.Duration;

import static core.logging.CustomLogger.debug;

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
 * <p>Element-dependent hooks receive the {@link LocatorDescriptor} of the element being
 * acted upon via the second parameter.  When invoked from legacy code paths the descriptor
 * may be {@code null}; each hook logs a warning and returns early in that case.</p>
 *
 * <h3>Hook ordering guarantee</h3>
 * <p>Before hooks execute in list order, then the action, then after hooks <b>in list order</b>.</p>
 *
 * <p>This class is a pure constants holder — never instantiate it.</p>
 *
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/Runner pipelines.
 */
public final class After {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private After() {}

    // ── No-ops ────────────────────────────────────────────────────────────
    public static final ActionHandler DO_NOTHING = (engine, descriptor) -> {};

    // ── Loader waits ──────────────────────────────────────────────────────
    public static final ActionHandler WAIT_FOR_ANGULAR_LOADER = (engine, descriptor) -> {
        LocatorDescriptor loader = LocatorDescriptor.of("app-loader", core.engine.LocatorStrategy.CSS);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    public static final ActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = (engine, descriptor) -> {
        LocatorDescriptor loader = LocatorDescriptor.of(
                "//span[contains(@class, 'spin spinner')]", core.engine.LocatorStrategy.XPATH);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    // ── Element-state waits ────────────────────────────────────────────────

    /** Waits for the element to be visible after the action. */
    public static final ActionHandler WAIT_FOR_ELEMENT_VISIBLE = (engine, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to After.WAIT_FOR_ELEMENT_VISIBLE");
            return;
        }
        engine.waitForVisible(descriptor, DEFAULT_TIMEOUT);
    };

    // ── Element manipulation ───────────────────────────────────────────────

    /** Highlights the element with a green border (success indicator). */
    public static final ActionHandler HIGHLIGHT_ELEMENT = (engine, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to After.HIGHLIGHT_ELEMENT");
            return;
        }
        engine.highlight(descriptor, "green");
    };

    /** Scrolls the element into view after the action. */
    public static final ActionHandler SCROLL_TO_ELEMENT = (engine, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to After.SCROLL_TO_ELEMENT");
            return;
        }
        engine.scrollTo(descriptor);
    };
}

