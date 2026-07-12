package core.actions.trace;

/**
 * Outcome classification for an instrumented action execution.
 */
public enum TraceStatus {
    /** All before-hooks, the action, and all after-hooks completed normally. */
    SUCCESS,
    /** The core action itself threw — before-hooks all passed. */
    FAILED,
    /** A before- or after-hook threw before the pipeline could complete. */
    HOOK_FAILED
}
