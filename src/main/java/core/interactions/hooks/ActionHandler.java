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
     */
    static ActionHandler legacy(java.util.function.Consumer<UIEngine> handler) {
        return (engine, descriptor) -> handler.accept(engine);
    }
}

