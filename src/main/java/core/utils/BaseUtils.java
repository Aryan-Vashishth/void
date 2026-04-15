package core.utils;

import core.driver.DriverContext;
import core.logging.CustomLogger;
import WebApplication.VOID;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * @deprecated God-object / inheritance-for-convenience anti-pattern.
 * <ul>
 *   <li>For logging: {@code import static core.logging.CustomLogger.debug;} (and info/warn/error)</li>
 *   <li>For {@code wait}: use {@link core.driver.Waiter#get()} — creates a per-call
 *       {@link WebDriverWait} from {@link core.driver.DriverContext}, eliminating the shared-static
 *       race condition in parallel tests.</li>
 *   <li>For {@code VOID}: use {@code new VOID()} directly (lightweight façade).</li>
 * </ul>
 * No new classes should extend this.
 */
@Deprecated(since = "2.0", forRemoval = true)
public abstract class BaseUtils {
    protected static CustomLogger.Debug debug = new CustomLogger.Debug();
    protected static CustomLogger.Info info = new CustomLogger.Info();
    protected static CustomLogger.Warn warn = new CustomLogger.Warn();
    protected static CustomLogger.Error error = new CustomLogger.Error();
    protected static VOID VOID;
    protected static WebDriverWait wait;

    /**
     * @deprecated Use {@link core.driver.Waiter#get()} instead.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void initializer(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("WebDriver instance must not be null. Initialization failed.");
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        CustomLogger.initialize(this.getClass());
        debug.log("[" + getClass().getSimpleName() + "] initialized with WebDriver: " + driver);
        VOID = new VOID();
    }

    public void initializer() {
        WebDriver driver = DriverContext.getActiveDriver();
        if (driver == null) {
            throw new IllegalStateException("WebDriver is not set in DriverContext.");
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        CustomLogger.initialize(this.getClass());
        debug.log("[" + BaseUtils.class.getSimpleName() + "] initialized with WebDriver: " + driver);
        VOID = new VOID();
    }
}
