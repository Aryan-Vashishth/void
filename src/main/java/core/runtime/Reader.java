package core.runtime;

import core.executor.FlowExecutor;
import domain.automation.web.vocabulary.actions.ReadTextAction;

/**
 * Session service for reading element text via the action DSL.
 *
 * <p>Obtained via {@link VOID#reader()}. Executes a {@link ReadTextAction} through
 * the full executor pipeline (hooks, waits, logging) and returns the captured result.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   String error = app.reader().query(LoginPage.ErrorMessage.Labels.ERROR_BANNER.getText());
 *   String badge = app.reader().query(ProductsPage.Header.Labels.CART_BADGE.getText());
 * </pre>
 *
 * <p>The element emits the action ({@code element.getText()}); the reader executes it
 * and returns the result. No direct engine access required.</p>
 */
public final class Reader {

    private final FlowExecutor executor;

    Reader(FlowExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes the action, then returns the text it captured.
     *
     * @param action a {@link ReadTextAction} emitted by a {@link domain.automation.web.vocabulary.capability.ReadOnly} element
     * @return the visible text of the element at execution time
     */
    public String query(ReadTextAction action) {
        executor.run(action);
        return action.result();
    }
}
