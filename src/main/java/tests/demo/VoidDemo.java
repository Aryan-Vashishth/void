package tests.demo;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.flow.Flow;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.executor.FlowExecutor;
import core.runtime.VOID;
import elements.meta.ElementRole;
import tests.demo.pages.DemoLoginPage;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

import static core.logging.CustomLogger.*;

/**
 * VOID Framework — Quick Start Demo (TestNG).
 *
 * <p>Demonstrates both the plain Action/Flow/FlowExecutor pattern and the fluent
 * {@code .withHooks(before, after)} pipeline with before/after hooks.
 * Targets the public demo site:
 * <a href="https://the-internet.herokuapp.com/login">the-internet.herokuapp.com</a></p>
 *
 * <p>Run via TestNG or: {@code mvn test -Dtest=tests.demo.VoidDemo}</p>
 */
public class VoidDemo {

    private static final String TARGET_URL = "https://the-internet.herokuapp.com/login";
    private static final String VALID_USERNAME = "tomsmith";
    private static final String VALID_PASSWORD = "SuperSecretPassword!";

    private VOID app;
    private UIEngine engine;
    private FlowExecutor executor;

    @BeforeClass
    public void setUp() {
        CustomLogger.initialize(VoidDemo.class);
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);

        info.log("==================================================");
        info.log("|       VOID Framework -- Quick Start Demo        |");
        info.log("==================================================");

        // Start VOID (bootstraps framework + creates browser via DriverFactory)
        info.log("[SETUP] Starting VOID framework...");
        app = VOID.start();
        engine = app.getEngine();
        executor = new FlowExecutor(engine);
        info.success("VOID started, engine: " + engine.getEngineName());
    }

    /**
     * Plain login — no hooks. Shows the basic Action / Flow / FlowExecutor pattern.
     */
    @Test
    public void loginWithValidCredentials() {
        // Navigate to the login page
        info.log("[1/3] Navigating to: " + TARGET_URL);
        engine.navigateTo(TARGET_URL);
        info.success("Page loaded. Current URL: " + engine.getCurrentUrl());

        // Execute login flow using Action / Flow / FlowExecutor pattern
        info.log("[2/3] Executing login flow...");
        debug.log("--> Typing username: " + VALID_USERNAME);
        debug.log("--> Typing password: ********");
        debug.log("--> Clicking Login button");

        executor.run(Flow.of(
                DemoLoginPage.Credentials.USERNAME_INPUT.type(VALID_USERNAME),
                DemoLoginPage.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD),
                DemoLoginPage.Button.LOGIN_BUTTON.click()
        ));

        info.success("Flow executed successfully.");

        // Wait for navigation, then verify we landed on the secure page
        info.log("[3/3] Verifying result...");
        LocatorDescriptor successMsg = engine.resolve(
                DemoLoginPage.Labels.SUCCESS_MESSAGE, ElementRole.TEXT);
        engine.waitForVisible(successMsg, Duration.ofSeconds(5));
        String currentUrl = engine.getCurrentUrl();
        debug.log("Current URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("LOGIN PASSED — Redirected to secure area.");
    }

    /**
     * Hooked login — demonstrates the fluent {@code .withHooks(before, after)} API
     * with before/after hook chains.
     *
     * <p>Each action is composed with hooks that receive the element's
     * {@code LocatorDescriptor} explicitly — no global state involved.</p>
     *
     * <h3>Hook pipeline per action</h3>
     * <pre>
     *   before hooks  →  action  →  after hooks
     *       ↓                           ↓
     *   (engine, descriptor)       (engine, descriptor)
     * </pre>
     */
    @Test(dependsOnMethods = "loginWithValidCredentials")
    public void loginWithHookedActions() {
        // Navigate back to login
        info.log("[HOOKED 1/3] Navigating to: " + TARGET_URL);
        engine.navigateTo(TARGET_URL);

        // Build a login flow with hooks around every action:
        //   - Before type: clear field + highlight (red)
        //   - After type:  highlight (green = success)
        //   - Before click: wait for clickable + highlight
        //   - After click:  custom hook waits for success message (demonstrates inline lambdas)
        info.log("[HOOKED 2/3] Executing hooked login flow...");

        executor.run(Flow.of(
                // Type username — with clear + highlight hooks
                DemoLoginPage.Credentials.USERNAME_INPUT.type(VALID_USERNAME)
                        .withHooks(
                                List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
                                List.of(After.HIGHLIGHT_ELEMENT)),

                // Type password — with clear + highlight hooks
                DemoLoginPage.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD)
                        .withHooks(
                                List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
                                List.of(After.HIGHLIGHT_ELEMENT)),

                // Click login — wait for clickable, then wait for success message after
                DemoLoginPage.Button.LOGIN_BUTTON.click()
                        .withHooks(
                                List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                                List.of(
                                        // Custom inline hook: wait for the success message after login click.
                                        // Demonstrates: hooks can resolve other elements via the engine,
                                        // not just the descriptor they receive.
                                        (eng, desc) -> {
                                            LocatorDescriptor successMsg = eng.resolve(
                                                    DemoLoginPage.Labels.SUCCESS_MESSAGE, ElementRole.TEXT);
                                            eng.waitForVisible(successMsg, Duration.ofSeconds(5));
                                            debug.log("[HOOK] Success message visible after login click.");
                                        }
                                ))
        ));

        info.success("Hooked flow executed successfully.");

        // Verify result
        info.log("[HOOKED 3/3] Verifying result...");
        String currentUrl = engine.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("HOOKED LOGIN PASSED — Redirected to secure area.");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        info.log("[TEARDOWN] Shutting down VOID...");
        if (app != null) {
            app.shutdown();
        }
        info.complete("Browser closed. Demo complete.");
        info.log("==================================================");
        info.log("  Demo finished. See Quick Start Guide for details.");
        info.log("==================================================");
    }
}
