package Configurations;

import CommonUtilMethods.ApplicationCommonElements;
import HelperClasses.HelperMethods;
import PageObjects.UserListing;
import Pages.AdminHome;
import Pages.Common.CommonMethods;
import ObjectsFactory.ObjectFactory;
import WebApplication.VOID;
import core.driver.DriverContext;
import core.logging.CustomLogger;
import core.utils.ExcelReader.ReadProperties;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;
import com.github.javafaker.Faker;
import java.time.Duration;


public class InitialiseBaseTest {
    public BrowserConfigurations browserConfigurations;
    public EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration();
    protected static ObjectFactory objectFactory;
    UserListing userListing;
    public static WebDriver driver;
    public static Logger log = Logger.getLogger(InitialiseBaseTest.class);
    private static String sprintnum = null;
    private URLS urls;
    protected SoftAssert softAssert = WebDrivercommonUtils.getSoftAssert();;
    protected CommonMethods commonMethods;
    public static ReadProperties properties = ReadProperties.getInstance();
    public Faker faker = new Faker();
    public static WebDriverWait wait;
    public VOID VOID;
    public CustomLogger.Debug debug = new CustomLogger.Debug();
    public CustomLogger.Info info = new CustomLogger.Info();
    public CustomLogger.Warn warn = new CustomLogger.Warn();
    public CustomLogger.Error error = new CustomLogger.Error();





    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_REDD = "\u001B[101m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_BLACK_BG_WHITE_TEXT = "\u001B[40m\u001B[37m";



    public void initialSetup() {
        CustomLogger.initialize(this.getClass());
        debug = new CustomLogger.Debug();
        info = new CustomLogger.Info();
        warn = new CustomLogger.Warn();
        error = new CustomLogger.Error();
        objectFactory = new ObjectFactory();
        driver = launchBrowser();
        DriverContext.setDriver(driver);
        WebDrivercommonUtils.setActions(driver);
        WebDrivercommonUtils.setJSExecutor(driver);
//        softAssert = WebDrivercommonUtils.getSoftAssert();
//        softAssert = new SoftAssert();
        WebDrivercommonUtils.resetInstance();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        VOID = new VOID();
    }

    //BeforeClass(alwaysRun = true)
    public void launchBaseCase() {

        browserConfigurations = new BrowserConfigurations();
        EnvironmentConfiguration.intislizeEnvironment();
        userListing=new UserListing();
        urls =new URLS();
    }

    public void setSprintVersion(){
        By sprintVersionLocator = By.xpath("//*[@id='maincontainer']/footer/div[1]/p");
        HelperMethods.waitUntilElementIsvisiblity(driver,sprintVersionLocator);
        WebElement SprintVersionPath=driver.findElement(sprintVersionLocator);
        String str = SprintVersionPath.getText();
        String[] splited = str.split(" ");
        try {
            sprintnum = splited[4];
            info.log("Return Sprint Number = '" + sprintnum + "'");
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }catch (TimeoutException ex) {
            log.error("Timed out waiting for sprint version element to be visible", ex);
        }
    }

    public static String getSprintnum() {
        return sprintnum;
    }

    public WebDriver launchBrowser() {
        launchBaseCase(); // Launch base configurations
        driver = browserConfigurations.browserName(EnvironmentConfiguration.getBrowserName());
        info.log("Storing driver value into webdriver variable");

        // Initialize core page objects
        new AdminHome(driver);
        new ApplicationCommonElements(driver);
        return driver;
    }

    public static WebDriver getDriver() {
        return driver;
    }

//    public void tearItDown(Scenario scenario) {
//        if (softAssert != null) {
//            try {
//                softAssert.assertAll();
//            } catch (AssertionError e) {
//                log.error("Soft assertions failed: " + e.getMessage());
//            }
//        }
//    }
}
