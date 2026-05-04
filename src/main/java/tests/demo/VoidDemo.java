package tests.demo;

import core.actions.HookedAction;
import core.engine.UIEngine;
import core.flow.Flow;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runner.Runner;
import core.runtime.VOID;
import elements.meta.ElementRole;
import tests.demo.pages.DemoLoginElements;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static core.logging.CustomLogger.*;

/**
 * VOID Framework — Quick Start Demo (TestNG).
 *
 * <p>Demonstrates both the plain Action/Flow/Runner pattern and the new
 * descriptor-based {@link HookedAction} pipeline with before/after hooks.
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
    private Runner runner;

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
        runner = new Runner(engine);
        info.success("VOID started, engine: " + engine.getEngineName());
    }

    /**
     * Plain login — no hooks. Shows the basic Action / Flow / Runner pattern.
     */
    @Test
    public void loginWithValidCredentials() {
        // Navigate to the login page
        info.log("[1/3] Navigating to: " + TARGET_URL);
        engine.navigateTo(TARGET_URL);
        info.success("Page loaded. Current URL: " + engine.getCurrentUrl());

        // Execute login flow using Action / Flow / Runner pattern
        info.log("[2/3] Executing login flow...");
        debug.log("--> Typing username: " + VALID_USERNAME);
        debug.log("--> Typing password: ********");
        debug.log("--> Clicking Login button");

        runner.run(Flow.of(
                DemoLoginElements.Credentials.USERNAME_INPUT.type(VALID_USERNAME),
                DemoLoginElements.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD),
                DemoLoginElements.Actions.LOGIN_BUTTON.click()
        ));

        info.success("Flow executed successfully.");

        // Verify we landed on the secure page
        info.log("[3/3] Verifying result...");
        String currentUrl = engine.getCurrentUrl();
        debug.log("Current URL: " + currentUrl);

        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("LOGIN PASSED — Redirected to secure area.");
    }

    /**
     * Hooked login — demonstrates descriptor-based {@link HookedAction} with
     * before/after hook chains.
     *
     * <p>Each action is wrapped with hooks that receive the element's
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
        //   - After click:  wait for element visible (success message)
        info.log("[HOOKED 2/3] Executing hooked login flow...");

        runner.run(Flow.of(
                // Type username — with clear + highlight hooks
                HookedAction.wrap(
                        DemoLoginElements.Credentials.USERNAME_INPUT.type(VALID_USERNAME),
                        DemoLoginElements.Credentials.USERNAME_INPUT, ElementRole.INPUT,
                        List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
                        List.of(After.HIGHLIGHT_ELEMENT)
                ),

                // Type password — with clear + highlight hooks
                HookedAction.wrap(
                        DemoLoginElements.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD),
                        DemoLoginElements.Credentials.PASSWORD_INPUT, ElementRole.INPUT,
                        List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
                        List.of(After.HIGHLIGHT_ELEMENT)
                ),

                // Click login — wait for clickable, highlight, then wait for result
                HookedAction.wrap(
                        DemoLoginElements.Actions.LOGIN_BUTTON.click(),
                        DemoLoginElements.Actions.LOGIN_BUTTON, ElementRole.TRIGGER,
                        List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE, Before.HIGHLIGHT_ELEMENT),
                        null
                )
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
