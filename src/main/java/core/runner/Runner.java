package core.runner;

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
 *   Runner runner = new Runner(engine);
 *   runner.run(Flow.of(page.USERNAME.type("user"), page.LOGIN.click()));
 *   runner.execute(page.SUBMIT.click());
 * </pre>
 */
@Beta(since = "2.0", note = "Action/Flow/Runner pipeline is evolving — API may change")
public class Runner {

    private final UIEngine engine;

    public Runner(UIEngine engine) {
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
    public void execute(Action action) {
        action.perform(engine);
    }
}

