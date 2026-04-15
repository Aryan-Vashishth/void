package WebApplication;

import core.driver.DriverContext;
import interactions.Interactions;
import interactions.StepDefInteractions;
import org.openqa.selenium.WebDriver;

/**
 * Façade / entry point for the VOID framework.
 * <p>
 * Obtain an instance via your base test class (it is stored in {@code BaseUtils.VOID})
 * then chain interaction calls:
 * <pre>
 *   VOID.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 *   VOID.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Dashboard");
 * </pre>
 * Interaction instances are lazily created and cached per {@code VOID} instance —
 * no new objects are allocated on every call.
 */
public class VOID {

    private final WebDriver driver;

    /** Lazily-initialised, cached interaction helpers. */
    private Interactions          interactions;
    private StepDefInteractions   stepDefInteractions;

    public VOID() {
        driver = DriverContext.getActiveDriver();
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

    /** Returns the (cached) step-definition interaction helper. */
    public StepDefInteractions stepDefInteraction() {
        if (stepDefInteractions == null) {
            stepDefInteractions = new StepDefInteractions(driver);
        }
        return stepDefInteractions;
    }
}
