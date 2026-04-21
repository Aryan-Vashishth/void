package automation.WebApplication;

import automation.interactions.StepDefInteractions;
import WebApplication.VOID;

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
 *   protected AutomationVOID app = new AutomationVOID();
 *
 *   // raw framework action
 *   app.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 *
 *   // BDD context-aware action
 *   app.stepDefInteraction().clickOnFrom("tiles", "admin_home", "Dashboard", After.DO_NOTHING);
 * </pre>
 */
public class AutomationVOID extends VOID {

    /** Lazily-initialised, cached step-definition interaction helper. */
    private StepDefInteractions stepDefInteractions;

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

