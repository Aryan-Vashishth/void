package automation.WebApplication;

import automation.interactions.StepDefInteractions;
import core.bootstrap.FrameworkBootstrap;
import core.context.ExecutionContext;
import core.driver.DriverFactory;
import core.driver.DriverManager;
import core.logging.CustomLogger;
import WebApplication.VOID;
import org.openqa.selenium.WebDriver;

/**
 * AutomationVOID — Automation-Layer Façade
 * -----------------------------------------
 * Extends the pure-framework {@link VOID} with BDD / Cucumber-specific helpers.
 *
 * <h3>Layer model</h3>
 * <pre>
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  automation layer  (this class + automation.* packages)      │
 *  │                                                              │
 *  │  AutomationVOID                                              │
 *  │    └── stepDefInteraction()  →  StepDefInteractions          │
 *  │                                 (context-based BDD actions)  │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  framework layer   (WebApplication / interactions / core)    │
 *  │                                                              │
 *  │  VOID                                                        │
 *  │    └── interaction()         →  Interactions                 │
 *  │                                 (raw UI actions)             │
 *  └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Usage</h3>
 * In your base test / step-definition base class, declare:
 * <pre>
 *   protected AutomationVOID app = AutomationVOID.start();
 *
 *   // raw framework action
 *   app.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 *
 *   // BDD context-aware action
 *   app.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Dashboard", After.DO_NOTHING);
 *
 *   // cleanup
 *   app.shutdown();
 * </pre>
 */
public class AutomationVOID extends VOID {

    /** Lazily-initialised, cached step-definition interaction helper. */
    private StepDefInteractions stepDefInteractions;

    /**
     * Protected constructor — use {@link #start()} or {@link #start(DriverFactory.Profile)}.
     */
    protected AutomationVOID(ExecutionContext context) {
        super(context);
    }

    // ===========================
    //      Static Factories
    // ===========================

    /**
     * Starts a new AutomationVOID session with the DEFAULT driver profile.
     *
     * @return a ready-to-use AutomationVOID instance
     */
    public static AutomationVOID start() {
        return start(DriverFactory.Profile.DEFAULT);
    }

    /**
     * Starts a new AutomationVOID session with the specified driver profile.
     *
     * @param profile the driver configuration profile
     * @return a ready-to-use AutomationVOID instance
     */
    public static AutomationVOID start(DriverFactory.Profile profile) {
        FrameworkBootstrap.init();

        WebDriver driver = DriverManager.createDriver(profile);
        ExecutionContext ctx = new ExecutionContext(
                FrameworkBootstrap.getUtilsConfig(),
                driver
        );

        CustomLogger.info.log("AutomationVOID initialised — driver ready.");
        return new AutomationVOID(ctx);
    }

    /**
     * Returns the (cached) step-definition interaction helper.
     * Creates a new instance on first call using the active WebDriver.
     */
    public StepDefInteractions stepDefInteraction() {
        if (stepDefInteractions == null) {
            stepDefInteractions = new StepDefInteractions(getDriver());
        }
        return stepDefInteractions;
    }
}
