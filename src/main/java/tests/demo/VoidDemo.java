package tests.demo;

import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runtime.VOID;
import tests.demo.hooks.DemoHooks;
import tests.demo.pages.DemoLoginPage;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static core.logging.CustomLogger.*;

/**
 * VOID — Quick Start Demo (TestNG).
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
        info.log("|            VOID -- Quick Start Demo           |");
        info.log("==================================================");

        info.log("[SETUP] Starting VOID session...");
        app = VOID.builder().start();
        info.success("VOID session started -- engine: " + app.debug().engine().getEngineName());
    }

    /**
     * Plain login -- no profile. Shows the session-façade pattern:
     * {@code app.navigateTo()}, {@code app.run(flow)}, {@code app.getCurrentUrl()}.
     *
     * <p>{@link DemoHooks#WAIT_FOR_LOGIN_SUCCESS} is attached to the click to guard
     * the redirect assertion. Post-submit navigation waits are a practical necessity
     * even without a {@code .safely()} profile -- the browser needs time to complete
     * the form-submission redirect before the URL can be read.</p>
     */
    @Test
    public void loginWithValidCredentials() {
        app.browser().navigateTo(TARGET_URL);

        info.log("Creating login flow...");
        Flow loginFlow = Flow.of(
                DemoLoginPage.Credentials.USERNAME.type(VALID_USERNAME),
                DemoLoginPage.Credentials.PASSWORD.type(VALID_PASSWORD),
                DemoLoginPage.Button.LOGIN_BUTTON.click()
                        .after(DemoHooks.WAIT_FOR_LOGIN_SUCCESS)
        );

        info.log("Executing login flow...");
        app.run(loginFlow);
        info.success("Flow executed successfully.");

        info.verifying("Verifying redirect to /secure...");
        String currentUrl = app.browser().url();
        Assert.assertTrue(currentUrl.contains("/secure"),
                "Expected URL to contain '/secure' but was: " + currentUrl);
        info.success("LOGIN PASSED — Redirected to secure area.");
    }

    /**
     * Profiled login — demonstrates {@code .safely()} and custom project-specific after-hooks.
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
     * <h3>Building custom hooks</h3>
     * <p>Project teams compose their own hook libraries by declaring
     * {@code static final AfterActionHandler} constants — the same pattern used by
     * {@link core.interactions.hooks.After}.  See {@link tests.demo.hooks.DemoHooks} for the
     * canonical example.  Any named constant drops in wherever a lambda would work:</p>
     * <pre>
     *   element.click()
     *       .safely()
     *       .after(DemoHooks.WAIT_FOR_LOGIN_SUCCESS);
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
        app.browser().navigateTo(TARGET_URL);

        info.log("Executing profiled login flow...");
        app.run(Flow.of(
                // safely() applies the SAFE profile — capability-aware hooks, no manual wiring
                DemoLoginPage.Credentials.USERNAME.type(VALID_USERNAME).safely(),

                DemoLoginPage.Credentials.PASSWORD.type(VALID_PASSWORD).safely(),

                // safely() + custom named after-hook — see DemoHooks for the implementation.
                // Named hooks are reusable, testable, and searchable; prefer them over
                // inline lambdas for anything beyond a one-off throwaway.
                DemoLoginPage.Button.LOGIN_BUTTON.click()
                        .safely()
                        .after(DemoHooks.WAIT_FOR_LOGIN_SUCCESS)
        ));
        info.success("Profiled flow executed successfully.");

        info.verifying("Verifying redirect to /secure...");
        String currentUrl = app.browser().url();
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
