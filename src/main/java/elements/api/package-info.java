/**
 * Reusable UI element abstractions for the VOID framework.
 *
 * <p>{@link elements.api.Element} is the root contract. Capability interfaces
 * in {@code elements.api.capability} ({@code Clickable}, {@code Typeable},
 * {@code Selectable}, {@code Table}, etc.) extend it to model specific
 * element types and emit deferred {@link core.actions.Action} objects.</p>
 *
 * <p>Elements NEVER execute — they emit intent only. Execution is handled
 * by {@link core.engine.UIEngine} at runtime.</p>
 *
 * <h3>Execution path</h3>
 * <pre>
 *   Element → Action (intent) → Flow → FlowExecutor → UIEngine (execution)
 * </pre>
 */
package elements.api;

