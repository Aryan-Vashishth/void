/**
 * Reusable UI element abstractions for the VOID framework.
 *
 * <p>{@link elements.api.UIElement} is the web-domain root contract, extending
 * the domain-neutral {@link core.target.Target}. Capability interfaces
 * in {@code elements.api.capability} ({@code Clickable}, {@code Typeable},
 * {@code Selectable}, {@code Table}, etc.) extend it to model specific
 * element types and emit deferred {@link core.actions.Action} objects.</p>
 *
 * <p>Elements NEVER execute -- they emit intent only. Execution is handled
 * by the web domain's execution contract at runtime.</p>
 *
 * <h3>Execution path</h3>
 * <pre>
 *   UIElement -&gt; Action (intent) -&gt; Flow -&gt; FlowExecutor -&gt; UIEngine (execution)
 * </pre>
 *
 * <h3>Domain ownership (ADR-021 addendum, runtime-redesign I6.2)</h3>
 * <p>All types in this package are <strong>Web-domain vocabulary</strong> (logical
 * ownership layer). They define what the Web domain's elements ARE, not how
 * they are executed. Physical relocation to {@code domain.automation.web.vocabulary.element}
 * is gated on the I6.4 Class Migration Matrix execution. ADR-021 addendum.</p>
 */
package elements.api;

