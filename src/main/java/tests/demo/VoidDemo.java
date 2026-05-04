package tests.demo;

import core.engine.UIEngine;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runner.Runner;
import core.runtime.VOID;
import tests.demo.pages.DemoLoginElements;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static core.logging.CustomLogger.*;

/**
 * VOID Framework -- Quick Start Demo (TestNG).
 *
 * <p>Demonstrates the modern Action / Flow / Runner pattern from the Quick Start Guide.
 * Targets the public demo site: <a href="https://the-internet.herokuapp.com/login">the-internet.herokuapp.com</a></p>
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
        info.success("LOGIN PASSED -- Redirected to secure area.");
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
