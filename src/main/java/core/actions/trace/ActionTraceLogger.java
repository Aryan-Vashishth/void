package core.actions.trace;

import core.actions.hooks.ActionHandler;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static core.logging.CustomLogger.debug;

/**
 * Formats and emits {@link ActionTrace} output at DEBUG level.
 *
 * <p>Output is suppressed automatically when the DEBUG log level is inactive.
 * Traces are never retained in memory — emit and forget.</p>
 */
public final class ActionTraceLogger {

    private ActionTraceLogger() {}

    /**
     * Constant-holder classes searched by {@link #nameOf(ActionHandler)} for a matching
     * field name. Populated via {@link #registerNameSource(Class)} rather than a hardcoded
     * reference, so this kernel-owned class (ADR-021, runtime-redesign I2.1) does not need
     * to import domain-specific hook payload libraries (e.g. {@code core.interactions.hooks
     * .Before}/{@code .After}) to name their constants.
     */
    private static final List<Class<?>> NAME_SOURCES = new CopyOnWriteArrayList<>();

    /**
     * Registers a constant-holder class whose {@code public static final} fields should be
     * searched by {@link #nameOf(ActionHandler)}.
     *
     * <p>Domain-specific hook libraries call this (typically from a static initializer) to
     * make their constants resolve to a readable name in trace output instead of falling
     * back to {@code "lambda"}.</p>
     *
     * @param holder a class whose public static fields hold {@link ActionHandler} constants
     */
    public static void registerNameSource(Class<?> holder) {
        NAME_SOURCES.add(holder);
    }

    /**
     * Emit a formatted trace block via the DEBUG logger.
     *
     * @param trace the completed action trace
     */
    public static void emit(ActionTrace trace) {
        debug.log(format(trace));
    }

    /**
     * Resolves a human-readable name for a hook instance.
     *
     * <p>Searches classes registered via {@link #registerNameSource(Class)} for a matching
     * constant, returning its field name (e.g. {@code "HIGHLIGHT_ELEMENT"}). For lambdas or
     * unregistered constants, returns {@code "lambda"}.</p>
     *
     * @param handler the hook to name
     * @return best-effort display name
     */
    public static String nameOf(ActionHandler handler) {
        for (Class<?> cls : NAME_SOURCES) {
            for (Field field : cls.getFields()) {
                try {
                    if (field.get(null) == handler) return field.getName();
                } catch (Exception ignored) {}
            }
        }
        String simple = handler.getClass().getSimpleName();
        return simple.contains("$$Lambda") || simple.isEmpty() ? "lambda" : simple;
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private static String format(ActionTrace trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══ ACTION TRACE ════════════════════════════════╗\n");
        line(sb, "Element  ", trace.elementName());
        line(sb, "Operation", trace.operation());
        line(sb, "Profile  ", trace.profileName());

        if (!trace.beforeHooks().isEmpty()) {
            String beforeLine = trace.beforeHooks().toString();
            boolean beforeFailed = trace.status() == TraceStatus.HOOK_FAILED
                    && trace.afterHooks().isEmpty();
            line(sb, "Before   ", beforeFailed ? beforeLine + "  ← FAILED HERE" : beforeLine);
        }

        if (trace.status() == TraceStatus.SUCCESS || trace.status() == TraceStatus.FAILED) {
            line(sb, "Execute  ", trace.operation() + "()");
        }

        if (!trace.afterHooks().isEmpty()) {
            String afterLine = trace.afterHooks().toString();
            boolean afterFailed = trace.status() == TraceStatus.HOOK_FAILED
                    && !trace.afterHooks().isEmpty();
            line(sb, "After    ", afterFailed ? afterLine + "  ← FAILED HERE" : afterLine);
        }

        line(sb, "Duration ", trace.durationMs() + "ms");
        line(sb, "Status   ", trace.status().name());

        if (trace.failure() != null) {
            String msg = trace.failure().getClass().getSimpleName()
                    + ": " + trace.failure().getMessage();
            line(sb, "Error    ", msg);
        }

        sb.append("╚════════════════════════════════════════════════╝");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String label, String value) {
        sb.append("║  ").append(label).append(": ").append(value).append("\n");
    }
}
