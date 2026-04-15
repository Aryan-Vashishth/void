package interactions.hooks;

import org.openqa.selenium.WebDriver;

/**
 * Functional interface for before/after action hooks applied around UI interactions.
 * <p>
 * Implementations are composable: collect any number into a {@code List<ActionHandler>}
 * and pass them to the relevant {@code Interactions} overload.  Pre-built constants live
 * in {@code interactions.hooks.Before} and {@code interactions.hooks.After};
 * custom lambdas are always valid:
 * <pre>
 *   interactions.clickOn(List.of(Before.WAIT_FOR_ANGULAR_LOADER, d -> myCustomSetup(d)), element);
 * </pre>
 */
@FunctionalInterface
public interface ActionHandler {
    /** Execute this hook given the active {@link WebDriver}. */
    void execute(WebDriver driver);
}

