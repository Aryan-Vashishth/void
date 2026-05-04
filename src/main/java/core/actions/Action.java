package core.actions;

import core.annotations.Beta;
import core.engine.UIEngine;

/**
 * Minimal execution contract — a single UI operation deferred for Flow-based execution.
 *
 * @apiNote <b>Beta.</b> This API may change without notice. Do not use inside stable modules.
 *
 * <p>An Action is produced by element capability interfaces (e.g., {@code element.click()},
 * {@code element.type("text")}) and executed later by a {@link core.runner.Runner}.
 * Both locator resolution and browser execution are <b>deferred</b> until
 * {@link #perform(UIEngine)} is called.</p>
 *
 * <h3>Single execution path</h3>
 * <pre>
 *   Element → Action → Flow → Runner → UIEngine
 * </pre>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Resolve locators <b>inside</b> the lambda (deferred, not eager).</li>
 *   <li>Never reference {@code WebDriver}, {@code WebElement}, or {@code By}.</li>
 *   <li>Action = deferred execution intent. Engine = smart executor.</li>
 * </ul>
 */
@Beta(since = "2.0", note = "Action/Flow/Runner pipeline is evolving — API may change")
@FunctionalInterface
public interface Action {
    /**
     * Executes this action against the given engine.
     *
     * @param engine the UI engine that performs the actual browser interaction
     */
    void perform(UIEngine engine);
}
