package core.flow;

import core.actions.Action;
import core.annotations.Beta;

import java.util.List;

/**
 * Immutable sequence of {@link Action}s representing a declarative UI workflow.
 *
 * @apiNote <b>Beta.</b> This API may change without notice. Do not use inside stable modules.
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow loginFlow = Flow.of(
 *       LoginPage.USERNAME.type("user"),
 *       LoginPage.PASSWORD.type("pass"),
 *       LoginPage.LOGIN_BUTTON.click()
 *   );
 *   executor.run(loginFlow);
 * </pre>
 */
@Beta(since = "2.0", note = "Action/Flow/FlowExecutor pipeline is evolving — API may change")
public class Flow {

    private final List<Action> actions;

    private Flow(List<Action> actions) {
        this.actions = actions;
    }

    /**
     * Creates a Flow from one or more Actions.
     *
     * @param actions ordered actions to execute
     * @return immutable Flow
     */
    public static Flow of(Action... actions) {
        return new Flow(List.of(actions));
    }

    /**
     * Returns the ordered list of actions in this flow.
     *
     * @return unmodifiable action list
     */
    public List<Action> getActions() {
        return actions;
    }
}

