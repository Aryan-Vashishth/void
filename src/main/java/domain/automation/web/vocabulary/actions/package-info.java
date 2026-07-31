/**
 * {@code domain.automation.web.vocabulary.actions} -- Deferred action implementations.
 *
 * <p>Concrete {@link core.actions.Action} implementations emitted by the Web-domain
 * capability interfaces. Each class captures the element and any runtime arguments;
 * the actual WebDriver interaction is deferred until {@code action.perform(engine)} is
 * called by the {@link core.executor.FlowExecutor}.</p>
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Action classes never execute directly -- they are invoked via
 *       {@code FlowExecutor}.</li>
 *   <li>Locator resolution happens inside {@code perform()}, not at construction time.</li>
 *   <li>No direct {@code WebDriver} import -- interactions go through
 *       {@link domain.automation.web.engine.UIEngine}.</li>
 * </ul>
 */
package domain.automation.web.vocabulary.actions;
