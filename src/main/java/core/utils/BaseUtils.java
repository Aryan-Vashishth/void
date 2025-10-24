package core.utils;

import core.driver.DriverContext;
import core.logging.CustomLogger;
import WebApplication.VOID;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public abstract class BaseUtils {
    protected static CustomLogger.Debug debug = new CustomLogger.Debug();
    protected static CustomLogger.Info info = new CustomLogger.Info();
    protected static CustomLogger.Warn warn = new CustomLogger.Warn();
    protected static CustomLogger.Error error = new CustomLogger.Error();
    protected static VOID VOID;
    protected static WebDriverWait wait;

    /**
     * Initializes the WebDriver context, wait, and logger.
     * Call this ONCE per thread/test!
     */
    public void initializer(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver instance must not be null. Initialization failed.");
        }
        DriverContext.setDriver(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        CustomLogger.initialize(this.getClass());
        debug.log("[" + getClass().getSimpleName() + "] initialized with WebDriver: " + driver);
        VOID = new VOID();
    }

    public void initializer() {
        WebDriver driver = DriverContext.getDriver();
        if (driver == null) {
            throw new IllegalStateException("WebDriver is not set in DriverContext.");
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        CustomLogger.initialize(this.getClass());
        debug.log("[" + getClass().getSimpleName() + "] initialized with WebDriver: " + driver);
        VOID = new VOID();
    }
}
