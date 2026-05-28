/**
 * {@code core.context} — Per-session execution context holders.
 *
 * <p>Provides immutable, explicitly-passed context objects that hold a session's
 * resolved configuration and execution resources. Eliminates hidden global state
 * by making dependencies visible at construction time.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.context.ExecutionContext} — holds resolved configuration and a
 *       raw {@link org.openqa.selenium.WebDriver} instance. Used by the legacy
 *       path and internal framework code.</li>
 *   <li>{@link core.context.SessionContext} — engine-agnostic replacement for
 *       {@code ExecutionContext}. Holds configuration and a {@link core.engine.UIEngine},
 *       enabling Playwright or any future engine without code changes.</li>
 * </ul>
 *
 * <h3>Design benefits</h3>
 * <ul>
 *   <li>No global mutable singletons — each thread/test gets its own context.</li>
 *   <li>Enables safe parallel execution.</li>
 *   <li>Makes all dependencies visible and testable at construction time.</li>
 * </ul>
 *
 * @see core.context.ExecutionContext
 * @see core.context.SessionContext
 * @see core.engine.UIEngine
 */
package core.context;
