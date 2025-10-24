package Configurations;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.log4j.Logger;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BrowserConfigurations {
    WebDriver driver;
    private final Logger log = Logger.getLogger(BrowserConfigurations.class);

    public WebDriver browserName(String browser) {
        if (!Objects.equals(browser, "null") && !Objects.equals(browser, "")) {
            switch (browser.toLowerCase()) {
                case "chrome":
                    chrome();
                    log.info("Opening Chrome Browser");
                    break;
                case "firefox":
                    firefox();
                    log.info("Opening Firefox Browser");
                    break;
                case "IE":
                    IE();
                    log.info("Opening IE Browser");
                    break;
                case "edge":
                    edge();
                    log.info("Opening Edge Browser");
                    break;
                default:
                    System.out.println("Something wrong with browser name");
            }
        } else {
            System.out.println("Browser name is empty");
        }
        return driver;
    }

    public void chrome() {
        String path = System.getProperty("user.dir");
        WebDriverManager.chromedriver().clearDriverCache().setup();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", path+"\\src\\main\\resources\\DownloadedReports\\");
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    public void firefox() {
        String path = System.getProperty("user.dir");
        WebDriverManager.firefoxdriver().clearDriverCache().setup();
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", path + "\\src\\main\\resources\\DownloadedReports\\");
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/pdf");
        FirefoxOptions options = new FirefoxOptions();
        options.setProfile(profile);
        driver = new FirefoxDriver(options);
        driver.manage().window().maximize();
    }

    public void IE() {
        String path = System.getProperty("user.dir");
        WebDriverManager.iedriver().setup();
        InternetExplorerOptions options = new InternetExplorerOptions();
        options.setCapability(InternetExplorerDriver.IE_ENSURE_CLEAN_SESSION, true);
        options.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
        options.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
//        options.setCapability(InternetExplorerDriver.NATIVE_EVENTS, false);
//        options.setCapability(InternetExplorerDriver.UNEXPECTED_ALERT_BEHAVIOR, UnexpectedAlertBehaviour.ACCEPT);
        options.setCapability("download.default_directory", path + "\\src\\main\\resources\\DownloadedReports\\");
        options.setCapability("requireWindowFocus", true);
        driver = new InternetExplorerDriver(options);
        driver.manage().window().maximize();
    }

    public void edge(){
        String path = System.getProperty("user.dir");
        WebDriverManager.edgedriver().clearDriverCache().setup();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("download.default_directory", path+"\\src\\main\\resources\\DownloadedReports\\");
        EdgeOptions options = new EdgeOptions();
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--remote-allow-origins=*");
        driver = new EdgeDriver(options);
        driver.manage().window().maximize();
    }

}