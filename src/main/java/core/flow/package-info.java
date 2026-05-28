/**
 * {@code core.flow} — Declarative action composition.
 *
 * <p>Contains {@link core.flow.Flow}, an immutable sequence of {@link core.actions.Action}s
 * that represents a complete UI workflow. Flows are created declaratively and executed
 * by a {@link core.executor.FlowExecutor}.</p>
 *
 * <h3>Key type</h3>
 * <ul>
 *   <li>{@link core.flow.Flow} — immutable, ordered list of Actions created via the
 *       static factory {@link core.flow.Flow#of(core.actions.Action...)}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   // Compose actions into a flow
 *   Flow loginFlow = Flow.of(
 *       LoginPage.USERNAME.type("admin@example.com"),
 *       LoginPage.PASSWORD.type("secret"),
 *       LoginPage.SUBMIT.click()
 *   );
 *
 *   // Execute later
 *   executor.run(loginFlow);
 * </pre>
 *
 * <h3>Design philosophy</h3>
 * <ul>
 *   <li>Flows are <b>pure data</b> — they describe what to do, not how to do it.</li>
 *   <li>Flows are immutable and reusable — the same flow can be executed multiple times.</li>
 *   <li>Flows compose naturally — you can build flows from other flows' actions.</li>
 *   <li>Execution is always deferred — creating a Flow does not trigger any browser
 *       interaction.</li>
 * </ul>
 *
 * <h3>Stability</h3>
 * <p><b>@Beta</b> — this API may change without notice between releases.</p>
 *
 * @see core.actions.Action
 * @see core.executor.FlowExecutor
 */
package core.flow;

