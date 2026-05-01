package WebApplication;

import core.driver.DriverContext;
import core.driver.DriverFactory;
import core.logging.CustomLogger;
import core.utils.ConfigLoader;
import core.utils.ConfigPaths;
import interactions.Interactions;
import org.openqa.selenium.WebDriver;

import java.util.Properties;

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

    /** Tracks whether the framework has been bootstrapped (once per JVM). */
    private static volatile boolean bootstrapped = false;


    public VOID() {
        bootstrap();
        DriverContext.setPrimaryDriver(
                DriverFactory.fromProfile(DriverFactory.Profile.DEFAULT).build()
        );
        driver = DriverContext.getActiveDriver();
        CustomLogger.info.log("VOID initialised — driver ready.");
    }

    // ===========================
    //      Framework Bootstrap
    // ===========================

    /**
     * One-time framework initialisation. Ensures:
     * <ol>
     *   <li>CustomLogger is wired to Log4j (via this class)</li>
     *   <li>driver.properties exists on classpath</li>
     *   <li>test.properties (utils config) is loaded into ConfigLoader's active store</li>
     * </ol>
     * Safe to call multiple times — only the first invocation performs work.
     */
    private static synchronized void bootstrap() {
        if (bootstrapped) return;

        // 1. Initialise CustomLogger with this class's Log4j context
        CustomLogger.initialize(VOID.class);

        // 2. Verify driver.properties is on the classpath
        Properties driverProps = ConfigLoader.loadFromClasspath(ConfigPaths.DRIVER_DEFAULT);
        if (driverProps.isEmpty()) {
            throw new IllegalStateException(
                    "VOID bootstrap failed: driver.properties not found on classpath at '"
                            + ConfigPaths.DRIVER_DEFAULT + "'. "
                            + "Ensure the file exists at src/main/resources/core/driver/config/driver.properties");
        }
        CustomLogger.debug.log("VOID bootstrap: driver.properties loaded (" + driverProps.size() + " keys)");

        // 3. Load utils/test config into the active ConfigLoader store
        Properties utilsProps = ConfigLoader.loadFromClasspath(ConfigPaths.UTILS_TEST);
        if (!utilsProps.isEmpty()) {
            ConfigLoader.setActive(utilsProps);
            CustomLogger.debug.log("VOID bootstrap: utils config activated (" + utilsProps.size() + " keys)");
        }

        bootstrapped = true;
        CustomLogger.info.log("VOID framework bootstrapped successfully.");
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
