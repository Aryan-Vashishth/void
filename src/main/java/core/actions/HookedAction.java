package core.actions;

import core.actions.trace.ActionTrace;
import core.actions.trace.ActionTraceLogger;
import core.actions.trace.TraceStatus;
import core.annotations.Internal;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
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
 * Descriptor is guaranteed non-null in the Action/Flow/FlowExecutor pipeline.
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
 *   <li>FlowExecutor stays dumb — HookedAction is just another {@link Action}.</li> * </ul>
 *
 * @see Action
 * @see ActionHandler
 */
@Internal
@Deprecated(forRemoval = true, since = "0.2")
public class HookedAction implements Action {

    private static final ThreadLocal<ActionTrace> LAST_TRACE = new ThreadLocal<>();

    private final Action delegate;
    private final LocatorDescriptor descriptor;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;
    @Nullable private final String profileName;

    /**
     * Package-private constructor used by {@link HookChainAction} — includes profile name
     * for trace output.
     */
    HookedAction(Action delegate,
                 LocatorDescriptor descriptor,
                 @Nullable List<ActionHandler> before,
                 @Nullable List<ActionHandler> after,
                 @Nullable String profileName) {
        this.delegate    = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.descriptor  = descriptor;
        this.before      = before == null ? List.of() : before;
        this.after       = after  == null ? List.of() : after;
        this.profileName = profileName;
    }

    /**
     * Test factory — creates a HookedAction for unit testing.
     * Internal testing API.
     *
     * @deprecated Internal testing API. Do not use in production code.
     */
    @Deprecated(since = "0.2", forRemoval = true)
    public static HookedAction forTesting(Action delegate,
                                          LocatorDescriptor descriptor,
                                          @Nullable List<ActionHandler> before,
                                          @Nullable List<ActionHandler> after) {
        return new HookedAction(delegate, descriptor, before, after, null);
    }



    public void perform(UIEngine engine) {
        performAndTrace(engine);
    }

    /**
     * Executes the full hook pipeline and returns a trace of the execution.
     * Package-private — exposed for unit testing only.
     */
    ActionTrace performAndTrace(UIEngine engine) {
        long start = System.currentTimeMillis();
        List<String> ranBefore = new ArrayList<>();
        List<String> ranAfter  = new ArrayList<>();
        TraceStatus  status    = TraceStatus.SUCCESS;
        Throwable    failure   = null;

        for (ActionHandler hook : before) {
            if (hook == null) continue;
            ranBefore.add(ActionTraceLogger.nameOf(hook));
            if (failure == null) {
                try {
                    hook.execute(engine, descriptor);
                } catch (RuntimeException | Error t) {
                    status  = TraceStatus.HOOK_FAILED;
                    failure = t;
                }
            }
        }

        if (failure == null) {
            try {
                delegate.perform(engine);
            } catch (RuntimeException | Error t) {
                status  = TraceStatus.FAILED;
                failure = t;
            }
        }

        if (failure == null) {
            for (ActionHandler hook : after) {
                if (hook == null) continue;
                ranAfter.add(ActionTraceLogger.nameOf(hook));
                if (failure == null) {
                    try {
                        hook.execute(engine, descriptor);
                    } catch (RuntimeException | Error t) {
                        status  = TraceStatus.HOOK_FAILED;
                        failure = t;
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        ActionTrace trace = new ActionTrace(
                resolveElementLabel(), resolveOperationLabel(),
                profileName != null ? profileName : "custom",
                ranBefore, ranAfter,
                elapsed, status, failure);

        LAST_TRACE.set(trace);
        ActionTraceLogger.emit(trace);

        if (failure != null) {
            sneakyThrow(failure);
        }
        return trace;
    }

    /** Returns the most-recently emitted trace on this thread (for testing). */
    static ActionTrace lastTrace() {
        return LAST_TRACE.get();
    }

    /** Clears the thread-local trace (call in @BeforeMethod / @AfterMethod). */
    static void clearLastTrace() {
        LAST_TRACE.remove();
    }

    private String resolveElementLabel() {
        return delegate.elementLabel();
    }

    private String resolveOperationLabel() {
        return delegate.operationLabel();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
