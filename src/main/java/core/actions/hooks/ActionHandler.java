package core.actions.hooks;

import core.engine.Executor;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;

import javax.annotation.Nullable;

/**
 * Functional interface for before/after action hooks applied around UI interactions.
 *
 * <p>Each hook receives the active {@link UIEngine} <b>and</b> the {@link LocatorDescriptor}
 * of the element being acted upon.  The descriptor may be {@code null} in legacy call paths
 * (e.g.&nbsp;{@link core.interactions.Interactions}); hooks must guard accordingly.</p>
 *
 * <p>Implementations are composable: collect any number into a {@code List<ActionHandler>}
 * and pass them to the relevant overload.  Pre-built constants live in
 * {@link core.interactions.hooks.Before} and {@link core.interactions.hooks.After}
 * (domain-specific hook payload libraries); custom lambdas are always valid:</p>
 * <pre>
 *   interactions.clickOn(
 *       List.of(Before.WAIT_FOR_ANGULAR_LOADER, (e, d) -&gt; myCustomSetup(e)),
 *       element);
 * </pre>
 *
 * <h3>Hook ordering guarantee</h3>
 * <ul>
 *   <li>Before hooks execute in list order.</li>
 *   <li>Then the action executes.</li>
 *   <li>After hooks execute in list order.</li>
 * </ul>
 *
 * <h3>Failure behavior</h3>
 * <ul>
 *   <li>If a <b>before</b> hook throws, the action is <b>not</b> executed.</li>
 *   <li>If an <b>after</b> hook throws, propagates (caller decides recovery).</li>
 * </ul>
 *
 * <h3>Kernel ownership (ADR-021, runtime-redesign I2.1)</h3>
 * <p>This is the kernel-owned hook contract. It moved here from
 * {@code core.interactions.hooks} because that package is the frozen legacy orchestrator
 * zone (audit D4) and the kernel must not import through it.
 * {@code core.interactions.hooks.ActionHandler} remains as a deprecated bridge (old
 * extends new) until I9.3. The contract still references {@link UIEngine} and
 * {@link LocatorDescriptor} directly -- both domain-side types today; retyping this
 * signature against the neutral {@code Executor} contract is I4's job, not this move's.</p>
 *
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/FlowExecutor pipelines.
 */
@FunctionalInterface
public interface ActionHandler {

    /**
     * Execute this hook given the active executor and the current action's locator descriptor.
     *
     * @param executor   the execution context that performs interactions
     * @param descriptor the locator descriptor for the element being acted upon;
     *                   may be {@code null} in legacy code paths
     *
     * @implNote Descriptor is non-null in the Action/Flow/FlowExecutor pipeline
     * ({@link core.actions.HookChainAction}).  Null only occurs in legacy
     * {@link core.interactions.Interactions} usage where hooks are invoked
     * without an explicit descriptor.  New code must always supply a
     * non-null descriptor.
     */
    void execute(Executor executor, @Nullable LocatorDescriptor descriptor);

    /**
     * @deprecated Use {@link #execute(Executor, LocatorDescriptor)} instead.
     *             Bridge overload; delegates to the primary. Scheduled for deletion in I9.4.
     */
    @Deprecated(since = "0.5", forRemoval = true)
    default void execute(UIEngine engine, @Nullable LocatorDescriptor descriptor) {
        execute((Executor) engine, descriptor);
    }

    // ── Legacy adapter ──────────────────────────────────────────────────────

    /**
     * Bridge for single-arg hooks that do not need a descriptor.
     * <p>Use during migration to wrap old-style {@code engine -> ...} lambdas:</p>
     * <pre>
     *   ActionHandler wrapped = ActionHandler.legacy(engine -&gt; doSomething(engine));
     * </pre>
     *
     * @param handler single-arg hook (receives only the UIEngine)
     * @return two-arg {@link ActionHandler} that ignores the descriptor
     * @deprecated Use descriptor-aware {@code (executor, descriptor) -> ...} hooks instead.
     *             This adapter exists only for migration and will be removed.
     */
    @Deprecated(forRemoval = true)
    static ActionHandler legacy(java.util.function.Consumer<UIEngine> handler) {
        return (executor, descriptor) -> handler.accept((UIEngine) executor);
    }
}
