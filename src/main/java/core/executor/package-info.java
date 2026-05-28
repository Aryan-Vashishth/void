/**
 * {@code core.executor} — Flow execution engine.
 *
 * <p>Contains {@link core.executor.FlowExecutor}, the terminal component in the
 * Action/Flow/FlowExecutor pipeline. The executor iterates through a
 * {@link core.flow.Flow}'s actions and calls {@link core.actions.Action#perform}
 * on each, delegating all browser interaction to the {@link core.engine.UIEngine}.</p>
 *
 * <h3>Key type</h3>
 * <ul>
 *   <li>{@link core.executor.FlowExecutor} — accepts a {@link core.engine.UIEngine}
 *       at construction time and provides {@code run(Flow)} and {@code run(Action)}
 *       methods for sequential execution.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   UIEngine engine = ...;
 *   FlowExecutor executor = new FlowExecutor(engine);
 *
 *   // Execute a composed flow
 *   executor.run(Flow.of(
 *       LoginPage.USERNAME.type("admin"),
 *       LoginPage.PASSWORD.type("secret"),
 *       LoginPage.SUBMIT.click()
 *   ));
 *
 *   // Execute a single action
 *   executor.run(DashboardPage.LOGOUT.click());
 * </pre>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>The executor is intentionally "dumb" — it only iterates and delegates.
 *       All smart execution logic (scroll, waits, retries, fallback) lives in
 *       {@link core.engine.UIEngine}.</li>
 *   <li>Hook orchestration is handled by {@link core.actions.HookedAction}, not
 *       by the executor — keeping the executor's responsibilities minimal.</li>
 * </ul>
 *
 * <h3>Stability</h3>
 * <p><b>@Beta</b> — this API may change without notice between releases.</p>
 *
 * @see core.actions.Action
 * @see core.flow.Flow
 * @see core.engine.UIEngine
 */
package core.executor;

