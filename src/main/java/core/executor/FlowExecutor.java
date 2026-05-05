package core.executor;

import core.actions.Action;
import core.annotations.Beta;
import core.engine.UIEngine;
import core.flow.Flow;

/**
 * Executes {@link Action}s and {@link Flow}s against a {@link UIEngine}.
 *
 * @apiNote <b>Beta.</b> This API may change without notice. Do not use inside stable modules.
 *
 * <p>Usage:</p>
 * <pre>
 *   FlowExecutor executor = new FlowExecutor(engine);
 *   executor.run(Flow.of(page.USERNAME.type("user"), page.LOGIN.click()));
 *   executor.run(page.SUBMIT.click());
 * </pre>
 */
@Beta(since = "2.0", note = "Action/Flow/FlowExecutor pipeline is evolving — API may change")
public class FlowExecutor {

    private final UIEngine engine;

    public FlowExecutor(UIEngine engine) {
        this.engine = engine;
    }

    /**
     * Executes all actions in the given flow sequentially.
     *
     * @param flow the flow to execute
     */
    public void run(Flow flow) {
        for (Action action : flow.getActions()) {
            action.perform(engine);
        }
    }

    /**
     * Executes a single action.
     *
     * @param action the action to execute
     */
    public void run(Action action) {
        action.perform(engine);
    }
}

