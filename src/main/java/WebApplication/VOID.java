package WebApplication;

import core.driver.DriverContext;
import core.driver.DriverFactory;
import interactions.Interactions;
import org.openqa.selenium.WebDriver;

/**
 * Façade / entry point for the core VOID framework.
 *
 * <p>This class is intentionally <b>framework-only</b> — it carries no
 * BDD / Cucumber dependencies. For step-definition helpers such as
 * {@code stepDefInteraction()}, use
 * {@code automation.WebApplication.AutomationVOID} which extends this class.</p>
 *
 * <h3>Framework layer usage</h3>
 * <pre>
 *   VOID app = new VOID();
 *   app.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 * </pre>
 *
 * <h3>Automation layer usage</h3>
 * <pre>
 *   AutomationVOID app = new AutomationVOID();
 *   app.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 *   app.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Dashboard", After.DO_NOTHING);
 * </pre>
 *
 * <h3>Layer model</h3>
 * <pre>
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  automation layer  (automation.*)                            │
 *  │    AutomationVOID  →  StepDefInteractions  (BDD actions)     │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  framework layer   (this class + interactions / core)        │
 *  │    VOID            →  Interactions  (raw UI actions)         │
 *  └──────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class VOID {

    private final WebDriver driver;

    /** Lazily-initialised, cached interaction helper. */
    private Interactions interactions;


    public VOID() {
        DriverContext.setPrimaryDriver(
                DriverFactory.fromProfile(DriverFactory.Profile.DEFAULT).build()
        );
        driver = DriverContext.getActiveDriver();
    }

    // ===========================
    //   Accessible to subclasses
    // ===========================

    /**
     * Returns the underlying {@link WebDriver} so subclasses (e.g. {@code AutomationVOID})
     * can pass it to their own interaction helpers without re-fetching it from the context.
     */
    protected WebDriver getDriver() {
        return driver;
    }

    // ===========================
    //         Interactions
    // ===========================

    /** Returns the (cached) general-purpose interaction helper. */
    public Interactions interaction() {
        if (interactions == null) {
            interactions = new Interactions(driver);
        }
        return interactions;
    }
}
