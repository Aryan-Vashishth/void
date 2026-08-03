package core.interactions.hooks;

import core.actions.hooks.BeforeActionHandler;
import core.engine.Executor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.locator.LocatorStrategy;

import java.time.Duration;

import static core.logging.CustomLogger.debug;

/**
 * Standard library of <b>before-action</b> {@link BeforeActionHandler} constants.
 * <p>
 * Combine freely at call sites:
 * <pre>
 *   interactions.clickOn(List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.HIGHLIGHT_ELEMENT), element);
 * </pre>
 *
 * <p>All hooks are <b>engine-agnostic</b> — they delegate to {@link UIEngine} methods
 * rather than calling Selenium APIs directly. This ensures portability across engines.</p>
 *
 * <p>Element-dependent hooks receive the {@link LocatorDescriptor} of the element being
 * acted upon via the second parameter.  When invoked from legacy code paths (e.g.
 * {@link core.interactions.Interactions}) the descriptor may be {@code null}; each hook
 * logs a warning and returns early in that case.</p>
 *
 * <h3>Hook ordering guarantee</h3>
 * <p>Before hooks execute <b>in list order</b>, then the action, then after hooks.</p>
 *
 * <p>This class is a pure constants holder — never instantiate it.</p>
 *
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/FlowExecutor pipelines.
 */
public final class Before {

    static {
        core.actions.trace.ActionTraceLogger.registerNameSource(Before.class);
    }

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private Before() {}

    // ── No-ops / logging ──────────────────────────────────────────────────
    public static final BeforeActionHandler DO_NOTHING  = (executor, descriptor) -> {};
    public static final BeforeActionHandler LOG_INTENT  = (executor, descriptor) -> debug.log("[DEBUG] Performing UI action...");

    // ── Loader waits ──────────────────────────────────────────────────────
    public static final BeforeActionHandler WAIT_FOR_ANGULAR_LOADER = (executor, descriptor) -> {
        UIEngine engine = (UIEngine) executor;
        LocatorDescriptor loader = LocatorDescriptor.of("app-loader", LocatorStrategy.CSS);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    public static final BeforeActionHandler WAIT_FOR_SPIN_SPINNER_LOADER = (executor, descriptor) -> {
        UIEngine engine = (UIEngine) executor;
        LocatorDescriptor loader = LocatorDescriptor.of(
                "//span[contains(@class, 'spin spinner')]", LocatorStrategy.XPATH);
        try { engine.waitForAbsence(loader, DEFAULT_TIMEOUT); }
        catch (Exception ignored) { /* loader not present — continue */ }
    };

    // ── Element-state waits (use descriptor passed by HookedAction / caller) ─────

    /** Waits for the element to become clickable. */
    public static final BeforeActionHandler WAIT_FOR_ELEMENT_CLICKABLE = (executor, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to WAIT_FOR_ELEMENT_CLICKABLE");
            return;
        }
        ((UIEngine) executor).waitForClickable(descriptor, DEFAULT_TIMEOUT);
    };

    /** Waits for the element to be visible. */
    public static final BeforeActionHandler WAIT_FOR_ELEMENT_VISIBLE = (executor, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to WAIT_FOR_ELEMENT_VISIBLE");
            return;
        }
        ((UIEngine) executor).waitForVisible(descriptor, DEFAULT_TIMEOUT);
    };

    // ── Element manipulation ───────────────────────────────────────────────

    /** Clears the input element. Throws if no descriptor provided. */
    public static final BeforeActionHandler CLEAR_FIELD = (executor, descriptor) -> {
        if (descriptor == null) {
            throw new IllegalStateException(
                    "[Before.CLEAR_FIELD] No descriptor provided – resolve the element first.");
        }
        ((UIEngine) executor).clear(descriptor);
    };

    /** Scrolls the element into view. */
    public static final BeforeActionHandler SCROLL_TO_ELEMENT = (executor, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to SCROLL_TO_ELEMENT");
            return;
        }
        ((UIEngine) executor).scrollTo(descriptor);
    };

    /** Highlights the element with a red border (debug aid). */
    public static final BeforeActionHandler HIGHLIGHT_ELEMENT = (executor, descriptor) -> {
        if (descriptor == null) {
            debug.log("[HOOK WARNING] No locator descriptor provided to HIGHLIGHT_ELEMENT");
            return;
        }
        ((UIEngine) executor).highlight(descriptor, "red");
    };
}

