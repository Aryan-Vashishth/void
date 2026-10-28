package examples.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.file.Path;
import java.util.Base64;

/**
 * Writes one Extent Spark HTML report per suite to {@code logs/extent-report/<suite>.html}.
 *
 * <p>Thread-safe for {@code parallel="methods"}: each test thread holds its own {@link ExtentTest}.
 * Test classes implementing {@link ScreenshotCapable} get a screenshot embedded on failure.</p>
 */
public class ExtentReportListener implements ITestListener {

    private static final Path REPORT_DIR = Path.of("logs", "extent-report");

    private final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();
    private ExtentReports extent;

    @Override
    public synchronized void onStart(ITestContext context) {
        if (extent != null) return;
        String suiteName = context.getSuite().getName();

        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_DIR.resolve(suiteName + ".html").toString());
        spark.config().setDocumentTitle(suiteName + " -- Extent Report");
        spark.config().setReportName(suiteName);
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
        extent.setSystemInfo("Headless", System.getProperty("headless", "false"));
        extent.setSystemInfo("Java", System.getProperty("java.version"));
        extent.setSystemInfo("OS", System.getProperty("os.name"));
    }

    @Override
    public void onTestStart(ITestResult result) {
        String description = result.getMethod().getDescription();
        ExtentTest test = extent.createTest(
                description != null ? description : result.getMethod().getMethodName(),
                result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName());
        String[] groups = result.getMethod().getGroups();
        if (groups.length > 0) test.assignCategory(groups);
        currentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        currentTest.get().pass("Passed in " + durationMs(result) + " ms");
        currentTest.remove();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = currentTest.get();
        test.fail(result.getThrowable());
        attachScreenshot(test, result);
        currentTest.remove();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // A skip from a failed @BeforeMethod never reaches onTestStart.
        ExtentTest test = currentTest.get();
        if (test == null) {
            onTestStart(result);
            test = currentTest.get();
        }
        if (result.getThrowable() != null) test.skip(result.getThrowable());
        else test.skip("Skipped");
        currentTest.remove();
    }

    @Override
    public synchronized void onFinish(ITestContext context) {
        if (extent != null) extent.flush();
    }

    private static void attachScreenshot(ExtentTest test, ITestResult result) {
        if (!(result.getInstance() instanceof ScreenshotCapable capable)) return;
        try {
            byte[] png = capable.captureScreenshot();
            if (png != null && png.length > 0) {
                test.fail("Screenshot on failure", MediaEntityBuilder
                        .createScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(png))
                        .build());
            }
        } catch (Exception ignored) {
            // Never mask the original test failure.
        }
    }

    private static long durationMs(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
