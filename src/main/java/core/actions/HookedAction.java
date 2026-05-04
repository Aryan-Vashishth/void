package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Wraps an {@link Action} with before/after {@link ActionHandler} hooks and an explicit
 * {@link LocatorDescriptor}, eliminating the need for global state ({@code UIContext}).
 *
 * <h3>Execution contract</h3>
 * <ol>
 *   <li><b>Before hooks</b> execute in list order with {@code (engine, descriptor)}.</li>
 *   <li>The <b>delegate action</b> executes via {@code delegate.perform(engine)}.</li>
 *   <li><b>After hooks</b> execute in list order with {@code (engine, descriptor)}.</li>
 * </ol>
 *
 * <h3>Descriptor ownership</h3>
 * <p>The descriptor is supplied at construction time by the element capability that creates
 * the action, <em>not</em> looked up from UIContext.  This makes HookedAction deterministic
 * and free of hidden global state.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   Action raw = engine -&gt; engine.click(descriptor);
 *   Action hooked = new HookedAction(raw, descriptor,
 *       List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE),
 *       List.of(After.HIGHLIGHT_ELEMENT));
 *   runner.execute(hooked);   // Runner stays dumb — HookedAction is just an Action
 * </pre>
 *
 * @apiNote <b>Beta.</b> This API may change without notice.
 * @see Action
 * @see ActionHandler
 */
@Beta(since = "2.0", note = "Hook evolution — descriptor-based hooks (no UIContext)")
public class HookedAction implements Action {

    private final Action delegate;
    private final LocatorDescriptor descriptor;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;

    /**
     * Creates a hooked action wrapping the given delegate.
     *
     * @param delegate   the core action to execute (must not be null)
     * @param descriptor the locator descriptor for the target element (may be null for
     *                   actions that don't target a specific element)
     * @param before     before-hooks to run (null or empty = none)
     * @param after      after-hooks to run (null or empty = none)
     */
    public HookedAction(Action delegate,
                        @Nullable LocatorDescriptor descriptor,
                        @Nullable List<ActionHandler> before,
                        @Nullable List<ActionHandler> after) {
        this.delegate   = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.descriptor = descriptor;
        this.before     = before == null ? List.of() : List.copyOf(before);
        this.after      = after  == null ? List.of() : List.copyOf(after);
    }

    /**
     * Executes the full hook pipeline: before → action → after.
     *
     * @param engine the UI engine that performs the actual browser interaction
     */
    @Override
    public void perform(UIEngine engine) {
        runHooks(before, engine);
        delegate.perform(engine);
        runHooks(after, engine);
    }

    /** Runs a list of hooks sequentially, passing the engine and descriptor. */
    private void runHooks(List<ActionHandler> hooks, UIEngine engine) {
        for (ActionHandler hook : hooks) {
            if (hook != null) {
                hook.execute(engine, descriptor);
            }
        }
    }

    /** Returns the descriptor associated with this hooked action (may be null). */
    @Nullable
    public LocatorDescriptor getDescriptor() {
        return descriptor;
    }
}

