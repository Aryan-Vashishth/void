package domain.automation.web.selenium.driver;

import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Provides on-demand {@link WebDriverWait} instances backed by the current thread's
 * active {@link SeleniumDriverContext} driver.
 *
 * <p>Replaces the {@code protected static WebDriverWait wait} field that lived in
 * {@code BaseUtils}, which was shared (and overwritten) across all subclasses —
 * a hidden thread-safety hazard in parallel test suites.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   Waiter.get().until(ExpectedConditions.visibilityOf(element));
 *   Waiter.get(30).until(ExpectedConditions.titleContains("Dashboard"));
 * }</pre>
 * </p>
 */
public final class Waiter {

    /** Default wait timeout used when no explicit timeout is supplied. */
    public static final int DEFAULT_TIMEOUT_SEC = 10;

    private Waiter() { /* static utility — no instances */ }

    /**
     * Returns a new {@link WebDriverWait} with the {@link #DEFAULT_TIMEOUT_SEC default timeout}
     * bound to the driver on the current thread.
     */
    public static WebDriverWait get() {
        return new WebDriverWait(SeleniumDriverContext.getDriver(), Duration.ofSeconds(DEFAULT_TIMEOUT_SEC));
    }

    /**
     * Returns a new {@link WebDriverWait} with an explicit timeout in seconds,
     * bound to the driver on the current thread.
     *
     * @param timeoutSeconds wait timeout (seconds)
     */
    public static WebDriverWait get(int timeoutSeconds) {
        return new WebDriverWait(SeleniumDriverContext.getDriver(), Duration.ofSeconds(timeoutSeconds));
    }
}
