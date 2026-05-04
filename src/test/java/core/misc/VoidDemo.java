package core.misc;

import core.engine.UIEngine;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runner.Runner;
import core.runtime.VOID;

import static core.logging.CustomLogger.*;

/**
 * VOID Framework -- Quick Start Demo (not a unit test).
 *
 * <p>Demonstrates the modern Action / Flow / Runner pattern from the Quick Start Guide.
 * Targets the public demo site: https://the-internet.herokuapp.com/login</p>
 *
 * <p>Run directly via: {@code java -cp <classpath> core.misc.VoidDemo}</p>
 */
public class VoidDemo {

    private static final String TARGET_URL = "https://the-internet.herokuapp.com/login";
    private static final String VALID_USERNAME = "tomsmith";
    private static final String VALID_PASSWORD = "SuperSecretPassword!";

    public static void main(String[] args) {
        CustomLogger.initialize(VoidDemo.class);
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);
        info.log("==================================================");
        info.log("|       VOID Framework -- Quick Start Demo        |");
        info.log("==================================================");

        // Step 1: Start VOID (bootstraps framework + creates browser via DriverFactory)
        info.log("[1/5] Starting VOID framework...");
        VOID app = VOID.start();
        info.success("VOID started, engine: " + app.getEngine().getEngineName());

        // Step 2: Get engine + create runner
        UIEngine engine = app.getEngine();
        Runner runner = new Runner(engine);

        try {
            // Step 3: Navigate to the login page
            info.log("[2/5] Navigating to: " + TARGET_URL);
            engine.navigateTo(TARGET_URL);
            info.success("Page loaded. Current URL: " + engine.getCurrentUrl());

            // Step 4: Execute login flow using Action / Flow / Runner pattern
            info.log("[3/5] Executing login flow...");
            debug.log("--> Typing username: " + VALID_USERNAME);
            debug.log("--> Typing password: ********");
            debug.log("--> Clicking Login button");

            runner.run(Flow.of(
                    DemoLoginElements.Credentials.USERNAME_INPUT.type(VALID_USERNAME),
                    DemoLoginElements.Credentials.PASSWORD_INPUT.type(VALID_PASSWORD),
                    DemoLoginElements.Actions.LOGIN_BUTTON.click()
            ));

            info.success("Flow executed successfully.");

            // Step 5: Verify we landed on the secure page
            info.log("[4/5] Verifying result...");
            String currentUrl = engine.getCurrentUrl();
            debug.log("Current URL: " + currentUrl);

            if (currentUrl.contains("/secure")) {
                info.success("LOGIN PASSED -- Redirected to secure area.");
            } else {
                error.failed("UNEXPECTED -- URL does not contain '/secure'. Check locators.");
            }

        } catch (Exception e) {
            error.failed("ERROR during demo execution: " + e.getMessage());
            error.log(e.toString());
        } finally {
            // Step 6: Shutdown
            info.log("[5/5] Shutting down VOID...");
            app.shutdown();
            info.complete("Browser closed. Demo complete.");
        }

        info.log("==================================================");
        info.log("  Demo finished. See Quick Start Guide for details.");
        info.log("==================================================");
    }
}
