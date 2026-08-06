package tests.listeners;

import core.logging.CustomLogger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Launches {@code mvn allure:serve} in a background process after the suite finishes,
 * generating the HTML report and opening it in the default browser automatically.
 *
 * <p>Skipped in CI and headless environments so it only fires on local runs.</p>
 */
public class AllureServeListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        if (isCiOrHeadless()) return;
        try {
            String mvn = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "mvn.cmd" : "mvn";
            new ProcessBuilder(mvn, "allure:serve")
                    .directory(new File(System.getProperty("user.dir")))
                    .start();
        } catch (Exception e) {
            CustomLogger.warn.log("[AllureServeListener] Could not launch allure:serve: " + e.getMessage());
        }
    }

    private boolean isCiOrHeadless() {
        return System.getenv("CI") != null
                || System.getenv("GITHUB_ACTIONS") != null
                || GraphicsEnvironment.isHeadless();
    }
}
