package examples.demo.hooks;

import core.actions.hooks.AfterActionHandler;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import examples.demo.pages.DemoLoginPage;
import domain.automation.web.vocabulary.role.ElementRole;

import java.time.Duration;

import static core.logging.CustomLogger.debug;

/**
 * Project-specific after-action hook constants for the VOID demo.
 *
 * <p>This class illustrates the recommended pattern for building a custom hook
 * library in a real test project. The framework ships a standard library in
 * {@link core.interactions.hooks.After}; teams extend it here — same shape,
 * project-specific logic.</p>
 *
 * <h3>How to add a new hook</h3>
 * <ol>
 *   <li>Declare a {@code static final AfterActionHandler} constant.</li>
 *   <li>Implement the two-arg lambda: {@code (engine, descriptor) -> ...}</li>
 *   <li>Use it anywhere {@code .after(...)} is accepted — inline or composed.</li>
 * </ol>
 *
 * <p>This class is a pure constants holder — never instantiate it.</p>
 */
public final class DemoHooks {

    private static final Duration LOGIN_SUCCESS_TIMEOUT = Duration.ofSeconds(5);

    private DemoHooks() {}

    /**
     * Waits for the login-success flash message to appear after the login button is clicked.
     *
     * <p>Resolves the success label via the engine — the hook never holds a direct
     * {@code WebElement} reference, keeping it engine-agnostic and reusable across sessions.</p>
     */
    public static final AfterActionHandler WAIT_FOR_LOGIN_SUCCESS = (executor, descriptor) -> {
        UIEngine engine = (UIEngine) executor;
        LocatorDescriptor successMsg = engine.resolve(
                DemoLoginPage.Labels.SUCCESS_MESSAGE, ElementRole.TEXT);
        engine.waitForVisible(successMsg, LOGIN_SUCCESS_TIMEOUT);
        debug.log("[HOOK] Login success message visible.");
    };
}
