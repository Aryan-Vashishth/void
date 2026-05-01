package core.driver;

import core.logging.CustomLogger;
import org.openqa.selenium.WebDriver;

/**
 * Isolated driver lifecycle manager.
 *
 * <p>Encapsulates WebDriver creation and registration, keeping driver
 * lifecycle concerns out of the VOID façade and the Interactions layer.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Create a {@link WebDriver} via {@link DriverFactory}</li>
 *   <li>Register it in {@link DriverContext} for thread-local access</li>
 *   <li>Quit and clean up all drivers for a session</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>No static driver storage of its own — delegates to {@link DriverContext}.</li>
 *   <li>All methods are static utilities; no instance state.</li>
 * </ul>
 */
public final class DriverManager {

    private DriverManager() {}

    /**
     * Creates a new {@link WebDriver} from the given profile, registers it as
     * the primary driver in {@link DriverContext}, and returns it.
     *
     * @param profile the driver configuration profile
     * @return the newly created and registered WebDriver
     */
    public static WebDriver createDriver(DriverFactory.Profile profile) {
        CustomLogger.debug.log("DriverManager: creating driver with profile " + profile);
        WebDriver driver = DriverFactory.fromProfile(profile).build();
        DriverContext.setPrimaryDriver(driver);
        CustomLogger.debug.log("DriverManager: driver created and registered as primary.");
        return driver;
    }

    /**
     * Quits all drivers registered in {@link DriverContext} for the current thread.
     */
    public static void quitAll() {
        CustomLogger.debug.log("DriverManager: quitting all drivers.");
        DriverContext.quitAllDrivers();
    }

    /**
     * Quits only the primary driver.
     */
    public static void quitPrimary() {
        CustomLogger.debug.log("DriverManager: quitting primary driver.");
        DriverContext.quitPrimaryDriver();
    }
}


