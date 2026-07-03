package core.actions.trace;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Immutable record of a single action execution — element, hooks, timing, and outcome.
 *
 * <p>Produced by the instrumented execution path in {@code HookedAction} and emitted
 * via {@code ActionTraceLogger} at DEBUG level. Not stored in memory.</p>
 */
public final class ActionTrace {

    private final String elementName;
    private final String operation;
    private final String profileName;
    private final List<String> beforeHooks;
    private final List<String> afterHooks;
    private final long durationMs;
    private final TraceStatus status;
    @Nullable private final Throwable failure;

    public ActionTrace(String elementName,
                String operation,
                String profileName,
                List<String> beforeHooks,
                List<String> afterHooks,
                long durationMs,
                TraceStatus status,
                @Nullable Throwable failure) {
        this.elementName = Objects.requireNonNull(elementName);
        this.operation   = Objects.requireNonNull(operation);
        this.profileName = Objects.requireNonNull(profileName);
        this.beforeHooks = List.copyOf(beforeHooks);
        this.afterHooks  = List.copyOf(afterHooks);
        this.durationMs  = durationMs;
        this.status      = Objects.requireNonNull(status);
        this.failure     = failure;
    }

    public String elementName()       { return elementName; }
    public String operation()         { return operation; }
    public String profileName()       { return profileName; }
    public List<String> beforeHooks() { return beforeHooks; }
    public List<String> afterHooks()  { return afterHooks; }
    public long durationMs()          { return durationMs; }
    public TraceStatus status()       { return status; }
    @Nullable public Throwable failure() { return failure; }
}
