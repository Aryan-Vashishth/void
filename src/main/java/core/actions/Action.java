package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single execution contract — a UI operation deferred for Flow-based execution.
 *
 * <p>An Action is produced by element capability interfaces (e.g., {@code element.click()},
 * {@code element.type("text")}) and executed later by a {@link core.executor.FlowExecutor}.
 * Both locator resolution and browser execution are <b>deferred</b> until
 * {@link #perform(UIEngine)} is called.</p>
 *
 * <h3>Single execution path</h3>
 * <pre>
 *   Element → Action → Flow → FlowExecutor → UIEngine
 * </pre>
 *
 * <h3>Hook support</h3>
 * <p>Actions created via {@link ElementActions} support {@link #withHooks(List, List)}
 * for fluent before/after hook composition:</p>
 * <pre>
 *   LoginPage.USERNAME.type("user")
 *       .withHooks(List.of(Before.CLEAR_FIELD), List.of(After.HIGHLIGHT_ELEMENT));
 * </pre>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Resolve locators <b>inside</b> perform (deferred, not eager).</li>
 *   <li>Never reference {@code WebDriver}, {@code WebElement}, or {@code By}.</li>
 *   <li>Action = deferred execution intent. Engine = smart executor.</li>
 * </ul>
 */
@Beta(since = "2.0", note = "Action/Flow/FlowExecutor pipeline is evolving — API may change")
@FunctionalInterface
public interface Action {

    /**
     * Executes this action against the given engine.
     *
     * @param engine the UI engine that performs the actual browser interaction
     */
    void perform(UIEngine engine);

    /**
     * Resolves the {@link LocatorDescriptor} for this action's target element.
     *
     * <p>Override in element-bound actions (via {@link ElementActions}) to enable
     * {@link #withHooks(List, List)}.  The default throws for raw lambda actions
     * that don't target a specific element.</p>
     *
     * @param engine the engine to resolve against
     * @return resolved descriptor
     * @throws UnsupportedOperationException if this action doesn't support resolution
     */
    default LocatorDescriptor resolve(UIEngine engine) {
        throw new UnsupportedOperationException(
                "This action does not support descriptor resolution. " +
                "Use ElementActions.of() to create resolvable actions.");
    }

    /**
     * Wraps this action with before/after hooks, returning a new {@link Action}.
     *
     * <p>The descriptor is resolved once at execution time via {@link #resolve(UIEngine)}
     * and shared across all hooks and the delegate action.</p>
     *
     * <pre>
     *   LoginPage.USERNAME.type("user")
     *       .withHooks(
     *           List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
     *           List.of(After.HIGHLIGHT_ELEMENT));
     * </pre>
     *
     * @param before before-hooks (null = none)
     * @param after  after-hooks (null = none)
     * @return a new action that runs: before hooks → this → after hooks
     */
    default Action withHooks(@Nullable List<ActionHandler> before,
                             @Nullable List<ActionHandler> after) {
        return engine -> {
            LocatorDescriptor d = resolve(engine);
            new HookedAction(this, d, before, after).perform(engine);
        };
    }
}
