package examples.listeners;

import io.qameta.allure.Allure;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

/**
 * TestNG listener that attaches a PNG screenshot to the Allure report when a test fails.
 * Relies on {@link ScreenshotCapable} -- any test class that implements it gets automatic
 * failure screenshots with no further configuration.
 *
 * <p>Register via the TestNG suite XML:
 * <pre>{@code
 * <listeners>
 *   <listener class-name="tests.listeners.ScreenshotListener"/>
 * </listeners>
 * }</pre>
 * </p>
 */
public class ScreenshotListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        Object instance = result.getInstance();
        if (!(instance instanceof ScreenshotCapable capable)) return;
        try {
            byte[] png = capable.captureScreenshot();
            if (png != null && png.length > 0) {
                Allure.addAttachment("Screenshot on failure", "image/png",
                        new ByteArrayInputStream(png), "png");
            }
        } catch (Exception ignored) {
            // Never mask the original test failure.
        }
    }
}
