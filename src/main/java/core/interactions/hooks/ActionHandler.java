package core.interactions.hooks;

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
 * {@link Before} and {@link After}; custom lambdas are always valid:</p>
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
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/Runner pipelines.
 */
@FunctionalInterface
public interface ActionHandler {

    /**
     * Execute this hook given the active engine and the current action's locator descriptor.
     *
     * @param engine     the UI engine that performs browser interactions
     * @param descriptor the locator descriptor for the element being acted upon;
     *                   may be {@code null} in legacy code paths
     *
     * @implNote Descriptor is non-null in the Action/Flow/Runner pipeline
     * ({@link core.actions.HookedAction}).  Null only occurs in legacy
     * {@link core.interactions.Interactions} usage where hooks are invoked
     * without an explicit descriptor.  New code must always supply a
     * non-null descriptor.
     */
    void execute(UIEngine engine, @Nullable LocatorDescriptor descriptor);

    // ── Legacy adapter ──────────────────────────────────────────────────────

    /**
     * Bridge for single-arg hooks that do not need a descriptor.
     * <p>Use during migration to wrap old-style {@code engine -> ...} lambdas:</p>
     * <pre>
     *   ActionHandler wrapped = ActionHandler.legacy(engine -&gt; doSomething(engine));
     * </pre>
     *
     * @param handler single-arg hook (receives only the engine)
     * @return two-arg {@link ActionHandler} that ignores the descriptor
     * @deprecated Use descriptor-aware {@code (engine, descriptor) -> ...} hooks instead.
     *             This adapter exists only for migration and will be removed.
     */
    @Deprecated(forRemoval = true)
    static ActionHandler legacy(java.util.function.Consumer<UIEngine> handler) {
        return (engine, descriptor) -> handler.accept(engine);
    }
}
