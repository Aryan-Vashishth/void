package core.executor;

import core.actions.Action;
import core.annotations.Beta;
import core.engine.Executor;
import core.flow.Flow;

/**
 * Executes {@link Action}s and {@link Flow}s against an {@link Executor}.
 *
 * @apiNote <b>Beta.</b> This API may change without notice. Do not use inside stable modules.
 *
 * <p><b>Preferred usage — let the VOID session manage execution:</b></p>
 * <pre>
 *   VOID app = VOID.start();
 *   app.run(Flow.of(page.USERNAME.type("user"), page.LOGIN.click()));
 *   app.run(page.SUBMIT.click());
 * </pre>
 *
 * <p>Direct construction of {@code FlowExecutor} is an advanced pattern reserved for
 * infrastructure code or framework internals. Test authors should use
 * {@link core.runtime.VOID#run(Flow)} and {@link core.runtime.VOID#run(Action)} instead,
 * which delegate to the session's internal executor.</p>
 */
@Beta(since = "0.1", note = "Action/Flow/FlowExecutor pipeline is evolving — API may change")
public class FlowExecutor {

    private final Executor executor;

    public FlowExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Executes all actions in the given flow sequentially.
     *
     * @param flow the flow to execute
     */
    public void run(Flow flow) {
        for (Action action : flow.getActions()) {
            action.perform(executor);
        }
    }

    /**
     * Executes a single action.
     *
     * @param action the action to execute
     */
    public void run(Action action) {
        action.perform(executor);
    }
}
