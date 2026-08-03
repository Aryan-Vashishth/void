package domain.automation.web.selenium.driver;

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
 *   <li>Create a {@link WebDriver} via {@link SeleniumDriverFactory}</li>
 *   <li>Register it in {@link SeleniumDriverContext} for thread-local access</li>
 *   <li>Quit and clean up all drivers for a session</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>No static driver storage of its own — delegates to {@link SeleniumDriverContext}.</li>
 *   <li>All methods are static utilities; no instance state.</li>
 * </ul>
 */
public final class SeleniumDriverManager {

    private SeleniumDriverManager() {}

    /**
     * Creates a new {@link WebDriver} from the given profile, registers it as
     * the primary driver in {@link SeleniumDriverContext}, and returns it.
     *
     * @param profile the driver configuration profile
     * @return the newly created and registered WebDriver
     */
    public static WebDriver createDriver(SeleniumDriverFactory.Profile profile) {
        CustomLogger.debug.log("SeleniumDriverManager: creating driver with profile " + profile);
        WebDriver driver = SeleniumDriverFactory.fromProfile(profile).build();
        SeleniumDriverContext.setPrimaryDriver(driver);
        CustomLogger.debug.log("SeleniumDriverManager: driver created and registered as primary.");
        return driver;
    }

    /**
     * Quits all drivers registered in {@link SeleniumDriverContext} for the current thread.
     */
    public static void quitAll() {
        CustomLogger.debug.log("SeleniumDriverManager: quitting all drivers.");
        SeleniumDriverContext.quitAllDrivers();
    }

    /**
     * Quits only the primary driver.
     */
    public static void quitPrimary() {
        CustomLogger.debug.log("SeleniumDriverManager: quitting primary driver.");
        SeleniumDriverContext.quitPrimaryDriver();
    }
}

