package tests.demo;

import core.engine.LocatorDescriptor;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runtime.VOID;
import elements.meta.ElementRole;
import tests.demo.pages.DemoLoginPage;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

import static core.logging.CustomLogger.*;

/**
 * VOID Framework — Quick Start Demo (TestNG).
 *
 * <p>Demonstrates the session-façade pattern: all interactions go through
 * the {@code VOID} session object — no direct engine or executor access.</p>
 *
 * <p>Targets the public demo site:
 * <a href="https://the-internet.herokuapp.com/login">the-internet.herokuapp.com</a></p>
 *
 * <p>Run via TestNG or: {@code mvn test -Dtest=tests.demo.VoidDemo}</p>
 */
public class VoidDemo {

    private static final String TARGET_URL = "https://the-internet.herokuapp.com/login";
    private static final String VALID_USERNAME = "tomsmith";
    private static final String VALID_PASSWORD = "SuperSecretPassword!";

    private VOID app;

    @BeforeClass
    public void setUp() {
        CustomLogger.initialize(VoidDemo.class);
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);

        info.log("==================================================");
        info.log("|       VOID Framework -- Quick Start Demo        |");
        info.log("==================================================");

        info.log("[SETUP] Starting VOID session...");
        app = VOID.start();
        info.success("VOID session started — engine: " + app.getEngine().getEngineName());
    }

    /**
     * Plain login — no hooks. Shows the session-façade pattern:
     * {@code app.navigateTo()}, {@code app.run(flow)}, {@code app.getCurrentUrl()}.
     */
    @Test
    public void loginWithValidCredentials() {
        // Navigate via session façade
        info.log("[1/3] Navigating to: " + TARGET_URL);
        app.navigateTo(TARGET_URL);
        info.success("Page loaded. Current URL: " + app.getCurrentUrl());

        // Execute login flow via session façade
        info.log("[2/3] Executing login flow...");
        debug.log("--> Typing username: " + VALID_USERNAME);
        debug.log("--> Typing password: ********");
        debug.log("--> Clicking Login button");

        app.run(Flow.of(
                DemoLoginPage.Credentials.USERNAME_INPUT.type(VALID_USERNAME),
                DemoLoginPage.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD),
                DemoLoginPage.Button.LOGIN_BUTTON.click()
        ));

        info.success("Flow executed successfully.");

        // Verify via session façade — engine resolves/waits internally
        info.log("[3/3] Verifying result...");
        String currentUrl = app.getCurrentUrl();
        debug.log("Current URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("LOGIN PASSED — Redirected to secure area.");
    }

    /**
     * Profiled login — demonstrates {@code .safely()} as the primary hook pattern.
     *
     * <p>{@code safely()} applies a capability-aware {@code SAFE} profile:
     * correct before/after hooks are chosen automatically based on whether the
     * action is a click, type, or select — no manual hook wiring needed.</p>
     *
     * <h3>Profile expansion</h3>
     * <pre>
     *   action.safely()  →  SAFE profile resolves hooks by capability
     *       Typeable  : before [CLEAR_FIELD, WAIT_FOR_ELEMENT_VISIBLE], after [HIGHLIGHT_ELEMENT]
     *       Clickable : before [WAIT_FOR_ELEMENT_CLICKABLE],            after [WAIT_FOR_ANGULAR_LOADER, HIGHLIGHT_ELEMENT]
     * </pre>
     *
     * <p>For full manual control (advanced / power-user), use {@code withHooks(List, List)} directly:</p>
     * <pre>
     *   element.type("text")
     *       .withHooks(
     *           List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE),
     *           List.of(After.HIGHLIGHT_ELEMENT));
     * </pre>
     */
    @Test(dependsOnMethods = "loginWithValidCredentials")
    public void loginWithHookedActions() {
        info.log("[HOOKED 1/3] Navigating to: " + TARGET_URL);
        app.navigateTo(TARGET_URL);

        info.log("[HOOKED 2/3] Executing profiled login flow...");

        app.run(Flow.of(
                // safely() applies the SAFE profile — capability-aware hooks, no manual wiring
                DemoLoginPage.Credentials.USERNAME_INPUT.type(VALID_USERNAME).safely(),

                DemoLoginPage.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD).safely(),

                // safely() + extra inline after-hook for app-specific wait
                DemoLoginPage.Button.LOGIN_BUTTON.click()
                        .safely()
                        .after(
                                // Custom inline hook: wait for success message after login click.
                                // Hooks receive the engine as a parameter — the intended way for
                                // hooks to perform engine-level operations without exposing the
                                // engine to test code.
                                (eng, desc) -> {
                                    LocatorDescriptor successMsg = eng.resolve(
                                            DemoLoginPage.Labels.SUCCESS_MESSAGE, ElementRole.TEXT);
                                    eng.waitForVisible(successMsg, Duration.ofSeconds(5));
                                    debug.log("[HOOK] Success message visible after login click.");
                                }
                        )
        ));

        info.success("Profiled flow executed successfully.");

        info.log("[HOOKED 3/3] Verifying result...");
        String currentUrl = app.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("HOOKED LOGIN PASSED — Redirected to secure area.");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        info.log("[TEARDOWN] Shutting down VOID session...");
        if (app != null) {
            app.shutdown();
        }
        info.complete("Browser closed. Demo complete.");
        info.log("==================================================");
        info.log("  Demo finished. See Quick Start Guide for details.");
        info.log("==================================================");
    }
}
