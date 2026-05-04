package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Decorator that applies before/after hooks around a delegate {@link Action}.
 *
 * <p>Execution order:</p>
 * <pre>
 *   before hooks → delegate.perform(engine) → after hooks
 * </pre>
 *
 * <p>Hooks receive the resolved {@link LocatorDescriptor} for this action.
 * Descriptor is guaranteed non-null in the Action/Flow/Runner pipeline.
 * Null only occurs when bridging from legacy {@link core.interactions.Interactions}.</p>
 *
 * <h3>Failure behavior</h3>
 * <ul>
 *   <li>If a before hook throws → the action is <b>not</b> executed.</li>
 *   <li>If an after hook throws → propagates (caller decides recovery).</li>
 * </ul>
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Descriptor is <b>passed in</b>, not resolved here — Action owns resolution.</li>
 *   <li>HookedAction contains <b>no engine logic</b> — only orchestrates.</li>
 *   <li>Runner stays dumb — HookedAction is just another {@link Action}.</li>
 * </ul>
 *
 * @see Action
 * @see ActionHandler
 */
public class HookedAction implements Action {

    private final Action delegate;
    private final LocatorDescriptor descriptor;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;

    /**
     * @param delegate   the core action to execute (must not be null)
     * @param descriptor the locator descriptor for the target element;
     *                   non-null in Action/Flow/Runner pipeline,
     *                   may be null only in legacy bridging
     * @param before     before-hooks to run (null = none)
     * @param after      after-hooks to run (null = none)
     */
    public HookedAction(Action delegate,
                        LocatorDescriptor descriptor,
                        @Nullable List<ActionHandler> before,
                        @Nullable List<ActionHandler> after) {
        this.delegate   = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.descriptor = descriptor; // may be null only in legacy bridging
        this.before     = before == null ? List.of() : before;
        this.after      = after  == null ? List.of() : after;
    }

    @Override
    public void perform(UIEngine engine) {
        executeHooks(before, engine, descriptor);
        delegate.perform(engine);
        executeHooks(after, engine, descriptor);
    }

    private void executeHooks(List<ActionHandler> hooks,
                              UIEngine engine,
                              LocatorDescriptor descriptor) {
        for (ActionHandler hook : hooks) {
            if (hook != null) {
                hook.execute(engine, descriptor);
            }
        }
    }

    // ── Deferred-resolution factory ─────────────────────────────────────────

    /**
     * Creates an {@link Action} that resolves the descriptor at execution time,
     * then runs before → delegate → after with that descriptor.
     *
     * <p>Keeps resolution fully deferred (consistent with Action/Flow/Runner).</p>
     * <pre>
     *   Flow.of(
     *       HookedAction.wrap(USERNAME.type("user"), USERNAME, ElementRole.INPUT,
     *           List.of(Before.CLEAR_FIELD), List.of(After.HIGHLIGHT_ELEMENT)),
     *       HookedAction.wrap(LOGIN.click(), LOGIN, ElementRole.TRIGGER,
     *           List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE), null)
     *   );
     * </pre>
     *
     * @param delegate the core action to wrap
     * @param element  the element whose locator will be resolved
     * @param role     the locator role to resolve (INPUT, TRIGGER, TEXT, etc.)
     * @param before   before-hooks (null = none)
     * @param after    after-hooks (null = none)
     * @return a deferred Action that resolves the descriptor and executes hooks
     */
    public static Action wrap(Action delegate,
                              elements.api.Element element,
                              elements.meta.ElementRole role,
                              @Nullable List<ActionHandler> before,
                              @Nullable List<ActionHandler> after) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(element, "element must not be null");
        Objects.requireNonNull(role, "role must not be null");
        return engine -> {
            LocatorDescriptor descriptor = engine.resolve(element, role);
            new HookedAction(delegate, descriptor, before, after).perform(engine);
        };
    }
}
