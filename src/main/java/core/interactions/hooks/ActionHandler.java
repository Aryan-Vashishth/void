package core.interactions.hooks;

import core.engine.UIEngine;

/**
 * Functional interface for before/after action hooks applied around UI interactions.
 * <p>
 * Implementations are composable: collect any number into a {@code List<ActionHandler>}
 * and pass them to the relevant {@code Interactions} overload.  Pre-built constants live
 * in {@code core.interactions.hooks.Before} and {@code core.interactions.hooks.After};
 * custom lambdas are always valid:
 * <pre>
 *   interactions.clickOn(List.of(Before.WAIT_FOR_ANGULAR_LOADER, e -> myCustomSetup(e)), element);
 * </pre>
 *
 * @apiNote <b>Stable.</b> Hook execution semantics will not change.
 * Compatible with both Interactions and Action/Flow/Runner pipelines.
 */
@FunctionalInterface
public interface ActionHandler {
    /** Execute this hook given the active {@link UIEngine}. */
    void execute(UIEngine engine);
}

