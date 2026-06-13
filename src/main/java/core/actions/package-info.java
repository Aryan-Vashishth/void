/**
 * {@code core.actions} — Deferred execution model for UI interactions.
 *
 * <p>This package defines the <b>Action/Flow/FlowExecutor</b> pipeline that forms VOID's
 * primary execution path. Actions represent deferred UI operations (intent) that are
 * composed into flows and executed later by a {@link core.executor.FlowExecutor}.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.actions.Action} — functional interface representing a single deferred
 *       UI operation. Produced by capability interfaces (e.g., {@code element.click()},
 *       {@code element.type("text")}). Supports directional hook composition via
 *       {@link core.actions.Action#before(core.interactions.hooks.BeforeActionHandler...)} and
 *       {@link core.actions.Action#after(core.interactions.hooks.AfterActionHandler...)}.</li>
 *   <li>{@link core.actions.ElementActions} — internal factory that creates element-bound
 *       Actions supporting descriptor resolution. Not part of the public DSL — capability
 *       interfaces use it internally to emit actions.</li>
 *   <li>{@link core.actions.HookedAction} — decorator that wraps an Action with
 *       before/after {@link core.interactions.hooks.ActionHandler} hooks. Execution order:
 *       before hooks → delegate action → after hooks.</li>
 * </ul>
 *
 * <h3>Execution path</h3>
 * <pre>
 *   Element (capability interface)
 *     → Action (deferred intent)
 *       → Flow (composition)
 *         → FlowExecutor (iteration)
 *           → UIEngine (physical execution)
 * </pre>
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Actions are <b>deferred</b> — locator resolution happens inside
 *       {@link core.actions.Action#perform}, never eagerly.</li>
 *   <li>Actions never reference {@code WebDriver}, {@code WebElement}, or {@code By} directly.</li>
 *   <li>Hook composition is optional and fluent:
 *       {@code element.click().before(...).after(...)}</li>
 * </ul>
 *
 * <h3>Stability</h3>
 * <p>This package is <b>@Beta</b> — the API may change between releases. Do not use
 * inside stable modules. External consumers interact with Actions opaquely by passing
 * them to {@link core.flow.Flow#of} and {@link core.executor.FlowExecutor#run}.</p>
 *
 * @see core.flow.Flow
 * @see core.executor.FlowExecutor
 * @see core.engine.UIEngine
 * @see core.interactions.hooks.ActionHandler
 */
package core.actions;

