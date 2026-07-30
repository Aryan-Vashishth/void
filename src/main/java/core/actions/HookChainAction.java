package core.actions;

import core.actions.hooks.ActionHandler;
import core.actions.trace.ActionTrace;
import core.actions.trace.ActionTraceLogger;
import core.actions.trace.TraceStatus;
import core.engine.Executor;
import elements.locator.LocatorDescriptor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal action wrapper that stores composable before/after hooks.
 *
 * <p>Owns the hook pipeline: resolves the descriptor once, runs before hooks,
 * executes the delegate, then runs after hooks. Emits an {@link ActionTrace}
 * for each execution.</p>
 */
final class HookChainAction implements Action {

    private static final ThreadLocal<ActionTrace> LAST_TRACE = new ThreadLocal<>();

    private final Action delegate;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;
    @Nullable private final String profileName;

    HookChainAction(Action delegate,
                    @Nullable List<? extends ActionHandler> before,
                    @Nullable List<? extends ActionHandler> after) {
        this(delegate, before, after, null);
    }

    private HookChainAction(Action delegate,
                            @Nullable List<? extends ActionHandler> before,
                            @Nullable List<? extends ActionHandler> after,
                            @Nullable String profileName) {
        this.delegate    = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.before      = normalize(before);
        this.after       = normalize(after);
        this.profileName = profileName;
    }

    @Override
    public Action mergeHooks(List<? extends ActionHandler> additionalBefore,
                             List<? extends ActionHandler> additionalAfter) {
        return new HookChainAction(
                delegate,
                concat(before, additionalBefore),
                concat(after, additionalAfter),
                profileName
        );
    }

    @Override
    public Action withProfile(ActionProfile profile) {
        return withProfileName(profile.name());
    }

    HookChainAction withProfileName(String name) {
        return new HookChainAction(delegate, before, after, name);
    }

    @Override
    public void perform(Executor executor) {
        performAndTrace(executor);
    }

    /**
     * Executes the full hook pipeline and returns a trace of the execution.
     * Package-private -- exposed for unit testing only.
     */
    ActionTrace performAndTrace(Executor executor) {
        LocatorDescriptor descriptor = delegate.resolve(executor);

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
                    hook.execute(executor, descriptor);
                } catch (RuntimeException | Error t) {
                    status  = TraceStatus.HOOK_FAILED;
                    failure = t;
                }
            }
        }

        if (failure == null) {
            try {
                delegate.perform(executor);
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
                        hook.execute(executor, descriptor);
                    } catch (RuntimeException | Error t) {
                        status  = TraceStatus.HOOK_FAILED;
                        failure = t;
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        ActionTrace trace = new ActionTrace(
                elementLabel(), operationLabel(),
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

    /**
     * Test factory -- creates a HookChainAction with a pinned descriptor.
     *
     * <p>Wraps {@code delegate} so that {@code resolve(engine)} returns the supplied
     * {@code descriptor} instead of querying the engine. Use in unit tests that need
     * to control the descriptor without a real element binding.</p>
     *
     * @deprecated Internal testing API. Do not use in production code.
     */
    @Deprecated(since = "0.3", forRemoval = true)
    static HookChainAction forTesting(Action delegate,
                                      @Nullable LocatorDescriptor descriptor,
                                      @Nullable List<ActionHandler> before,
                                      @Nullable List<ActionHandler> after) {
        Objects.requireNonNull(delegate, "delegate action must not be null");
        Action pinned = new Action() {
            @Override public void perform(Executor executor)          { delegate.perform(executor); }
            @Override public LocatorDescriptor resolve(Executor e)    { return descriptor; }
            @Override public ActionCapability capability()            { return delegate.capability(); }
            @Override public String elementLabel()                    { return delegate.elementLabel(); }
            @Override public String operationLabel()                  { return delegate.operationLabel(); }
        };
        return new HookChainAction(pinned, before, after);
    }

    @Override
    public LocatorDescriptor resolve(Executor executor) {
        return delegate.resolve(executor);
    }

    @Override
    public ActionCapability capability() {
        return delegate.capability();
    }

    @Override
    public String elementLabel() {
        return delegate.elementLabel();
    }

    @Override
    public String operationLabel() {
        return delegate.operationLabel();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    private static List<ActionHandler> normalize(@Nullable List<? extends ActionHandler> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            return List.of();
        }

        List<ActionHandler> normalized = new ArrayList<>(hooks.size());
        for (ActionHandler hook : hooks) {
            if (hook != null) {
                normalized.add(hook);
            }
        }

        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static List<ActionHandler> concat(List<ActionHandler> existing,
                                               @Nullable List<? extends ActionHandler> additional) {
        if (additional == null || additional.isEmpty()) {
            return existing;
        }

        List<ActionHandler> merged = new ArrayList<>(existing.size() + additional.size());
        merged.addAll(existing);
        for (ActionHandler hook : additional) {
            if (hook != null) {
                merged.add(hook);
            }
        }

        return merged.isEmpty() ? List.of() : List.copyOf(merged);
    }
}
